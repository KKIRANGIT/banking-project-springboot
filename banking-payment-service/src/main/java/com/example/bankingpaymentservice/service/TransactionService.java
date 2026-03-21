package com.example.bankingpaymentservice.service;

import com.example.bankingpaymentservice.dto.AccountResponse;
import com.example.bankingpaymentservice.dto.TransactionRequest;
import com.example.bankingpaymentservice.dto.TransactionResponse;
import com.example.bankingpaymentservice.exception.AccountClientNotFoundException;
import com.example.bankingpaymentservice.exception.InsufficientFundsException;
import com.example.bankingpaymentservice.exception.InvalidTransactionException;
import com.example.bankingpaymentservice.exception.TransactionNotFoundException;
import com.example.bankingpaymentservice.exception.TransactionProcessingException;
import com.example.bankingpaymentservice.model.Account;
import com.example.bankingpaymentservice.model.AccountStatus;
import com.example.bankingpaymentservice.model.Transaction;
import com.example.bankingpaymentservice.model.TransactionStatus;
import com.example.bankingpaymentservice.model.TransactionType;
import com.example.bankingpaymentservice.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final String DEFAULT_CURRENCY = "USD";

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final RemoteAccountService remoteAccountService;
    private final FraudCheckService fraudCheckService;
    private final SanctionsCheckService sanctionsCheckService;
    private final TransactionEventProducer transactionEventProducer;
    private final Clock clock;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountService accountService,
            RemoteAccountService remoteAccountService,
            FraudCheckService fraudCheckService,
            SanctionsCheckService sanctionsCheckService,
            TransactionEventProducer transactionEventProducer,
            Clock clock
    ) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.remoteAccountService = remoteAccountService;
        this.fraudCheckService = fraudCheckService;
        this.sanctionsCheckService = sanctionsCheckService;
        this.transactionEventProducer = transactionEventProducer;
        this.clock = clock;
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        validateBusinessRules(request);

        String accountNumber = request.getAccountNumber().trim();
        AccountResponse remoteAccount = fetchVerifiedActiveAccount(accountNumber);
        Account account = accountService.syncAccountSnapshot(remoteAccount);

        BigDecimal amount = request.getAmount();
        if (request.getType() == TransactionType.DEBIT && remoteAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds for account " + accountNumber);
        }

        Transaction transaction = new Transaction(
                account,
                amount,
                DEFAULT_CURRENCY,
                request.getType(),
                TransactionStatus.PENDING,
                LocalDateTime.now(clock)
        );
        transaction = transactionRepository.saveAndFlush(transaction);
        transactionEventProducer.publishTransactionInitiated(transaction);

        try {
            CompletableFuture<Boolean> fraudFuture = fraudCheckService.checkFraud(transaction);
            CompletableFuture<Boolean> sanctionsFuture = sanctionsCheckService.checkSanctions(accountNumber);

            CompletableFuture.allOf(fraudFuture, sanctionsFuture)
                    .orTimeout(3, TimeUnit.SECONDS)
                    .join();

            boolean isFraudulent = fraudFuture.join();
            boolean isSanctioned = sanctionsFuture.join();

            if (isFraudulent || isSanctioned) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction = transactionRepository.saveAndFlush(transaction);
                transactionEventProducer.publishTransactionFailed(transaction);
                log.info(
                        "Transaction for account {} marked FAILED on thread {}",
                        accountNumber,
                        Thread.currentThread().getName()
                );
            } else {
                try {
                    AccountResponse updatedRemoteAccount = remoteAccountService.updateBalance(
                            accountNumber,
                            calculateBalanceDelta(amount, request.getType())
                    );
                    ensureAccountServiceAvailable(updatedRemoteAccount, accountNumber);
                    account = accountService.syncAccountSnapshot(updatedRemoteAccount);
                    transaction.setAccount(account);
                    transaction.setStatus(TransactionStatus.SUCCESS);
                    transaction = transactionRepository.saveAndFlush(transaction);
                    transactionEventProducer.publishTransactionCompleted(transaction);
                    log.info(
                            "Transaction for account {} marked SUCCESS on thread {}",
                            accountNumber,
                            Thread.currentThread().getName()
                    );
                } catch (TransactionProcessingException exception) {
                    transaction.setStatus(TransactionStatus.FAILED);
                    transaction = transactionRepository.saveAndFlush(transaction);
                    transactionEventProducer.publishTransactionFailed(transaction);
                    throw exception;
                }
            }
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof TimeoutException) {
                transaction.setStatus(TransactionStatus.PENDING);
                transaction = transactionRepository.saveAndFlush(transaction);
                log.warn(
                        "Transaction checks timed out for account {}. Saving as PENDING on thread {}",
                        accountNumber,
                        Thread.currentThread().getName()
                );
            } else {
                throw exception;
            }
        }

        return toResponse(transaction);
    }

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAllWithAccount().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TransactionResponse getTransactionById(Long id) {
        return transactionRepository.findByIdWithAccount(id)
                .map(this::toResponse)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for id: " + id));
    }

    public List<TransactionResponse> getTransactionsByAccount(String accountNumber) {
        List<TransactionResponse> accountTransactions = transactionRepository
                .findByAccountNumberWithAccount(accountNumber)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        if (accountTransactions.isEmpty()) {
            throw new TransactionNotFoundException(
                    "No transactions found for account number: " + accountNumber
            );
        }

        return accountTransactions;
    }

    @Transactional
    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findByIdWithAccount(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for id: " + id));

        if (transaction.getStatus() == TransactionStatus.SUCCESS) {
            String accountNumber = transaction.getAccountNumber();
            AccountResponse updatedRemoteAccount = remoteAccountService.updateBalance(
                    accountNumber,
                    reverseBalanceDelta(transaction.getAmount(), transaction.getType())
            );
            ensureAccountServiceAvailable(updatedRemoteAccount, accountNumber);
            Account syncedAccount = accountService.syncAccountSnapshot(updatedRemoteAccount);
            transaction.setAccount(syncedAccount);
        }

        transactionRepository.delete(transaction);
    }

    public void demonstrateNPlusOneProblem() {
        log.info("N+1 demo: loading transactions without JOIN FETCH");
        List<Transaction> transactions = transactionRepository.findAll();
        transactions.forEach(transaction -> log.info(
                "Transaction {} belongs to account {}",
                transaction.getId(),
                transaction.getAccount().getAccountNumber()
        ));
    }

    public void demonstrateJoinFetchFix() {
        log.info("JOIN FETCH demo: loading transactions with account in one query");
        List<Transaction> transactions = transactionRepository.findAllWithAccount();
        transactions.forEach(transaction -> log.info(
                "Transaction {} belongs to account {}",
                transaction.getId(),
                transaction.getAccount().getAccountNumber()
        ));
    }

    public long countPendingTransactions() {
        return transactionRepository.countByStatus(TransactionStatus.PENDING);
    }

    public DailyTransactionSummary getTodaySummary() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (Transaction transaction : transactionRepository.findByCreatedAtBetween(startOfDay, startOfNextDay)) {
            if (transaction.getType() == TransactionType.DEBIT) {
                totalDebit = totalDebit.add(transaction.getAmount());
            } else if (transaction.getType() == TransactionType.CREDIT) {
                totalCredit = totalCredit.add(transaction.getAmount());
            }
        }

        return new DailyTransactionSummary(totalDebit, totalCredit);
    }

    private void validateBusinessRules(TransactionRequest request) {
        String normalizedAccountNumber = request.getAccountNumber() == null ? "" : request.getAccountNumber().trim();
        if (normalizedAccountNumber.length() < 4) {
            throw new InvalidTransactionException("Account number must contain at least 4 characters");
        }
        if (request.getType() != TransactionType.DEBIT && request.getType() != TransactionType.CREDIT) {
            throw new InvalidTransactionException("Transaction type must be DEBIT or CREDIT");
        }
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .accountNumber(transaction.getAccountNumber())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private AccountResponse fetchVerifiedActiveAccount(String accountNumber) {
        try {
            AccountResponse account = remoteAccountService.getAccount(accountNumber);
            ensureAccountServiceAvailable(account, accountNumber);
            if (account.getStatus() != AccountStatus.ACTIVE) {
                throw new InvalidTransactionException("Transactions are allowed only for ACTIVE accounts");
            }
            return account;
        } catch (AccountClientNotFoundException exception) {
            throw new InvalidTransactionException(exception.getMessage());
        }
    }

    private void ensureAccountServiceAvailable(AccountResponse account, String accountNumber) {
        if (account.getStatus() == AccountStatus.UNKNOWN) {
            throw new TransactionProcessingException(
                    "Account service is unavailable for account number: " + accountNumber
            );
        }
    }

    private BigDecimal calculateBalanceDelta(BigDecimal amount, TransactionType type) {
        return type == TransactionType.DEBIT ? amount.negate() : amount;
    }

    private BigDecimal reverseBalanceDelta(BigDecimal amount, TransactionType type) {
        return type == TransactionType.DEBIT ? amount : amount.negate();
    }

    public record DailyTransactionSummary(BigDecimal totalDebit, BigDecimal totalCredit) {
    }
}
