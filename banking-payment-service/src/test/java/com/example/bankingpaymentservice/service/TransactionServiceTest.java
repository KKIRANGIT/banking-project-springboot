package com.example.bankingpaymentservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bankingpaymentservice.dto.AccountResponse;
import com.example.bankingpaymentservice.dto.TransactionRequest;
import com.example.bankingpaymentservice.dto.TransactionResponse;
import com.example.bankingpaymentservice.exception.AccountClientNotFoundException;
import com.example.bankingpaymentservice.exception.InsufficientFundsException;
import com.example.bankingpaymentservice.exception.InvalidTransactionException;
import com.example.bankingpaymentservice.exception.TransactionNotFoundException;
import com.example.bankingpaymentservice.exception.TransactionProcessingException;
import com.example.bankingpaymentservice.metrics.TransactionMetricsService;
import com.example.bankingpaymentservice.model.Account;
import com.example.bankingpaymentservice.model.AccountStatus;
import com.example.bankingpaymentservice.model.Transaction;
import com.example.bankingpaymentservice.model.TransactionStatus;
import com.example.bankingpaymentservice.model.TransactionType;
import com.example.bankingpaymentservice.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-22T10:15:30Z"), ZoneOffset.UTC);

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private RemoteAccountService remoteAccountService;

    @Mock
    private FraudCheckService fraudCheckService;

    @Mock
    private SanctionsCheckService sanctionsCheckService;

    @Mock
    private TransactionEventProducer transactionEventProducer;

    @Mock
    private TransactionMetricsService transactionMetricsService;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository,
                accountService,
                remoteAccountService,
                fraudCheckService,
                sanctionsCheckService,
                transactionEventProducer,
                transactionMetricsService,
                FIXED_CLOCK
        );
    }

    @Test
    void testCreateTransaction_Success() {
        stubSaveAndFlush();

        TransactionRequest request = new TransactionRequest("ACC1001", new BigDecimal("200.00"), TransactionType.DEBIT);
        AccountResponse existingAccount = accountResponse("ACC1001", "1000.00", AccountStatus.ACTIVE);
        AccountResponse updatedAccount = accountResponse("ACC1001", "800.00", AccountStatus.ACTIVE);
        Account accountSnapshot = account("ACC1001", "1000.00");
        Account updatedSnapshot = account("ACC1001", "800.00");

        when(remoteAccountService.getAccount("ACC1001")).thenReturn(existingAccount);
        when(accountService.syncAccountSnapshot(existingAccount)).thenReturn(accountSnapshot);
        when(fraudCheckService.checkFraud(any(Transaction.class))).thenReturn(CompletableFuture.completedFuture(false));
        when(sanctionsCheckService.checkSanctions("ACC1001")).thenReturn(CompletableFuture.completedFuture(false));
        when(remoteAccountService.updateBalance("ACC1001", new BigDecimal("-200.00"))).thenReturn(updatedAccount);
        when(accountService.syncAccountSnapshot(updatedAccount)).thenReturn(updatedSnapshot);

        TransactionResponse response = transactionService.createTransaction(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAccountNumber()).isEqualTo("ACC1001");
        assertThat(response.getAmount()).isEqualByComparingTo("200.00");
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 22, 10, 15, 30));

        verify(transactionMetricsService).incrementCreated();
        verify(transactionEventProducer).publishTransactionInitiated(any(Transaction.class));
        verify(transactionEventProducer).publishTransactionCompleted(any(Transaction.class));
        verify(transactionEventProducer, never()).publishTransactionFailed(any(Transaction.class));
    }

    @Test
    void testCreateTransaction_InsufficientFunds() {
        TransactionRequest request = new TransactionRequest("ACC1001", new BigDecimal("200.00"), TransactionType.DEBIT);
        AccountResponse account = accountResponse("ACC1001", "100.00", AccountStatus.ACTIVE);

        when(remoteAccountService.getAccount("ACC1001")).thenReturn(account);
        when(accountService.syncAccountSnapshot(account)).thenReturn(account("ACC1001", "100.00"));

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds for account ACC1001");

        verify(transactionRepository, never()).saveAndFlush(any(Transaction.class));
        verify(transactionEventProducer, never()).publishTransactionInitiated(any(Transaction.class));
    }

    @Test
    void testCreateTransaction_AccountNotFound() {
        TransactionRequest request = new TransactionRequest("ACC404", new BigDecimal("50.00"), TransactionType.CREDIT);

        when(remoteAccountService.getAccount("ACC404"))
                .thenThrow(new AccountClientNotFoundException("Account not found for account number: ACC404"));

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Account not found for account number: ACC404");

        verify(transactionRepository, never()).saveAndFlush(any(Transaction.class));
    }

    @Test
    void testCreateTransaction_FraudDetected() {
        stubSaveAndFlush();

        TransactionRequest request = new TransactionRequest("ACC1001", new BigDecimal("200.00"), TransactionType.CREDIT);
        AccountResponse existingAccount = accountResponse("ACC1001", "1000.00", AccountStatus.ACTIVE);
        Account accountSnapshot = account("ACC1001", "1000.00");

        when(remoteAccountService.getAccount("ACC1001")).thenReturn(existingAccount);
        when(accountService.syncAccountSnapshot(existingAccount)).thenReturn(accountSnapshot);
        when(fraudCheckService.checkFraud(any(Transaction.class))).thenReturn(CompletableFuture.completedFuture(true));
        when(sanctionsCheckService.checkSanctions("ACC1001")).thenReturn(CompletableFuture.completedFuture(false));

        TransactionResponse response = transactionService.createTransaction(request);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);
        verify(transactionMetricsService).incrementCreated();
        verify(transactionMetricsService).incrementFailed();
        verify(remoteAccountService, never()).updateBalance(any(), any());
        verify(transactionEventProducer).publishTransactionInitiated(any(Transaction.class));
        verify(transactionEventProducer).publishTransactionFailed(any(Transaction.class));
        verify(transactionEventProducer, never()).publishTransactionCompleted(any(Transaction.class));
    }

    @Test
    void testCreateTransaction_TimeoutMarksPending() {
        stubSaveAndFlush();

        TransactionRequest request = new TransactionRequest("ACC1001", new BigDecimal("200.00"), TransactionType.CREDIT);
        AccountResponse existingAccount = accountResponse("ACC1001", "1000.00", AccountStatus.ACTIVE);

        when(remoteAccountService.getAccount("ACC1001")).thenReturn(existingAccount);
        when(accountService.syncAccountSnapshot(existingAccount)).thenReturn(account("ACC1001", "1000.00"));
        when(fraudCheckService.checkFraud(any(Transaction.class)))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("fraud timed out")));
        when(sanctionsCheckService.checkSanctions("ACC1001")).thenReturn(CompletableFuture.completedFuture(false));

        TransactionResponse response = transactionService.createTransaction(request);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.PENDING);
        verify(transactionMetricsService).incrementCreated();
        verify(transactionEventProducer).publishTransactionInitiated(any(Transaction.class));
        verify(transactionEventProducer, never()).publishTransactionCompleted(any(Transaction.class));
        verify(transactionEventProducer, never()).publishTransactionFailed(any(Transaction.class));
    }

    @Test
    void testCreateTransaction_AccountServiceUnavailableDuringBalanceUpdate() {
        stubSaveAndFlush();

        TransactionRequest request = new TransactionRequest("ACC1001", new BigDecimal("200.00"), TransactionType.CREDIT);
        AccountResponse existingAccount = accountResponse("ACC1001", "1000.00", AccountStatus.ACTIVE);
        AccountResponse unknownAccount = accountResponse("ACC1001", "0.00", AccountStatus.UNKNOWN);

        when(remoteAccountService.getAccount("ACC1001")).thenReturn(existingAccount);
        when(accountService.syncAccountSnapshot(existingAccount)).thenReturn(account("ACC1001", "1000.00"));
        when(fraudCheckService.checkFraud(any(Transaction.class))).thenReturn(CompletableFuture.completedFuture(false));
        when(sanctionsCheckService.checkSanctions("ACC1001")).thenReturn(CompletableFuture.completedFuture(false));
        when(remoteAccountService.updateBalance("ACC1001", new BigDecimal("200.00"))).thenReturn(unknownAccount);

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(TransactionProcessingException.class)
                .hasMessageContaining("Account service is unavailable for account number: ACC1001");

        verify(transactionMetricsService).incrementCreated();
        verify(transactionMetricsService).incrementFailed();
        verify(transactionEventProducer).publishTransactionFailed(any(Transaction.class));
        verify(transactionEventProducer, never()).publishTransactionCompleted(any(Transaction.class));
    }

    @Test
    void testCreateTransaction_InactiveAccount() {
        TransactionRequest request = new TransactionRequest("ACC1001", new BigDecimal("25.00"), TransactionType.CREDIT);
        AccountResponse suspendedAccount = accountResponse("ACC1001", "1000.00", AccountStatus.SUSPENDED);

        when(remoteAccountService.getAccount("ACC1001")).thenReturn(suspendedAccount);

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transactions are allowed only for ACTIVE accounts");
    }

    @Test
    void testCreateTransaction_AccountServiceUnavailableOnLookup() {
        TransactionRequest request = new TransactionRequest("ACC1001", new BigDecimal("25.00"), TransactionType.CREDIT);
        AccountResponse unknownAccount = accountResponse("ACC1001", "0.00", AccountStatus.UNKNOWN);

        when(remoteAccountService.getAccount("ACC1001")).thenReturn(unknownAccount);

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(TransactionProcessingException.class)
                .hasMessageContaining("Account service is unavailable for account number: ACC1001");
    }

    @Test
    void testCreateTransaction_InvalidRequest() {
        TransactionRequest request = new TransactionRequest("ABC", new BigDecimal("25.00"), null);

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Account number must contain at least 4 characters");
    }

    @Test
    void testGetAllTransactions_Success() {
        when(transactionRepository.findAllWithAccount()).thenReturn(List.of(
                transaction(account("ACC1001", "1000.00"), "100.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 1L),
                transaction(account("ACC1002", "500.00"), "50.00", TransactionType.DEBIT, TransactionStatus.PENDING, 2L)
        ));

        List<TransactionResponse> transactions = transactionService.getAllTransactions();

        assertThat(transactions).hasSize(2);
        assertThat(transactions).extracting(TransactionResponse::getAccountNumber).containsExactly("ACC1001", "ACC1002");
    }

    @Test
    void testGetTransactionById_NotFound() {
        when(transactionRepository.findByIdWithAccount(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(99L))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("Transaction not found for id: 99");
    }

    @Test
    void testGetTransactionsByAccount_Success() {
        when(transactionRepository.findByAccountNumberWithAccount("ACC1001")).thenReturn(List.of(
                transaction(account("ACC1001", "1000.00"), "100.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 1L),
                transaction(account("ACC1001", "1000.00"), "25.00", TransactionType.DEBIT, TransactionStatus.FAILED, 2L)
        ));

        List<TransactionResponse> transactions = transactionService.getTransactionsByAccount("ACC1001");

        assertThat(transactions).hasSize(2);
        assertThat(transactions).extracting(TransactionResponse::getStatus)
                .containsExactly(TransactionStatus.SUCCESS, TransactionStatus.FAILED);
    }

    @Test
    void testGetTransactionsByAccount_NotFound() {
        when(transactionRepository.findByAccountNumberWithAccount("ACC9999")).thenReturn(List.of());

        assertThatThrownBy(() -> transactionService.getTransactionsByAccount("ACC9999"))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("No transactions found for account number: ACC9999");
    }

    @Test
    void testDeleteTransaction_Success() {
        Transaction existingTransaction = transaction(
                account("ACC1001", "1000.00"),
                "200.00",
                TransactionType.DEBIT,
                TransactionStatus.SUCCESS,
                1L
        );
        AccountResponse updatedAccount = accountResponse("ACC1001", "1200.00", AccountStatus.ACTIVE);
        Account syncedAccount = account("ACC1001", "1200.00");

        when(transactionRepository.findByIdWithAccount(1L)).thenReturn(Optional.of(existingTransaction));
        when(remoteAccountService.updateBalance("ACC1001", new BigDecimal("200.00"))).thenReturn(updatedAccount);
        when(accountService.syncAccountSnapshot(updatedAccount)).thenReturn(syncedAccount);

        transactionService.deleteTransaction(1L);

        verify(transactionRepository).delete(existingTransaction);
    }

    @Test
    void testDeleteTransaction_NotFound() {
        when(transactionRepository.findByIdWithAccount(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(55L))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("Transaction not found for id: 55");
    }

    @Test
    void testCountPendingTransactions() {
        when(transactionRepository.countByStatus(TransactionStatus.PENDING)).thenReturn(4L);

        assertThat(transactionService.countPendingTransactions()).isEqualTo(4L);
    }

    @Test
    void testGetTodaySummary() {
        when(transactionRepository.findByCreatedAtBetween(
                LocalDateTime.of(2026, 3, 22, 0, 0),
                LocalDateTime.of(2026, 3, 23, 0, 0)
        )).thenReturn(List.of(
                transaction(account("ACC1001", "1000.00"), "100.00", TransactionType.DEBIT, TransactionStatus.SUCCESS, 1L),
                transaction(account("ACC1002", "1000.00"), "75.00", TransactionType.DEBIT, TransactionStatus.SUCCESS, 2L),
                transaction(account("ACC1003", "1000.00"), "50.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 3L)
        ));

        TransactionService.DailyTransactionSummary summary = transactionService.getTodaySummary();

        assertThat(summary.totalDebit()).isEqualByComparingTo("175.00");
        assertThat(summary.totalCredit()).isEqualByComparingTo("50.00");
    }

    @Test
    void testDemonstrateNPlusOneProblem() {
        when(transactionRepository.findAll()).thenReturn(List.of(
                transaction(account("ACC1001", "1000.00"), "100.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 1L)
        ));

        transactionService.demonstrateNPlusOneProblem();

        verify(transactionRepository).findAll();
    }

    @Test
    void testDemonstrateJoinFetchFix() {
        when(transactionRepository.findAllWithAccount()).thenReturn(List.of(
                transaction(account("ACC1001", "1000.00"), "100.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 1L)
        ));

        transactionService.demonstrateJoinFetchFix();

        verify(transactionRepository).findAllWithAccount();
    }

    private void stubSaveAndFlush() {
        when(transactionRepository.saveAndFlush(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            if (transaction.getId() == null) {
                transaction.setId(1L);
            }
            return transaction;
        });
    }

    private AccountResponse accountResponse(String accountNumber, String balance, AccountStatus status) {
        return AccountResponse.builder()
                .id(1L)
                .accountNumber(accountNumber)
                .accountHolderName("Alice")
                .balance(new BigDecimal(balance))
                .status(status)
                .version(0L)
                .build();
    }

    private Account account(String accountNumber, String balance) {
        return new Account(accountNumber, "Alice", new BigDecimal(balance), AccountStatus.ACTIVE);
    }

    private Transaction transaction(
            Account account,
            String amount,
            TransactionType type,
            TransactionStatus status,
            Long id
    ) {
        Transaction transaction = new Transaction(
                account,
                new BigDecimal(amount),
                "USD",
                type,
                status,
                LocalDateTime.of(2026, 3, 22, 10, 15)
        );
        transaction.setId(id);
        return transaction;
    }
}
