package com.example.bankingpaymentservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bankingpaymentservice.model.Account;
import com.example.bankingpaymentservice.model.AccountStatus;
import com.example.bankingpaymentservice.model.Transaction;
import com.example.bankingpaymentservice.model.TransactionStatus;
import com.example.bankingpaymentservice.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void testFindByAccountNumber() {
        Account accountOne = persistAccount("ACC1001");
        Account accountTwo = persistAccount("ACC2002");
        persistTransaction(accountOne, "100.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 1);
        persistTransaction(accountOne, "250.00", TransactionType.DEBIT, TransactionStatus.PENDING, 2);
        persistTransaction(accountTwo, "75.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 3);

        List<Transaction> transactions = transactionRepository.findByAccountAccountNumber("ACC1001");

        assertThat(transactions).hasSize(2);
        assertThat(transactions).extracting(Transaction::getAccountNumber).containsOnly("ACC1001");
    }

    @Test
    void testFindTop5ByOrderByAmountDesc() {
        persistTransaction(persistAccount("ACC1001"), "100.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 1);
        persistTransaction(persistAccount("ACC1002"), "600.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 2);
        persistTransaction(persistAccount("ACC1003"), "300.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 3);
        persistTransaction(persistAccount("ACC1004"), "500.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 4);
        persistTransaction(persistAccount("ACC1005"), "200.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 5);
        persistTransaction(persistAccount("ACC1006"), "400.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 6);

        List<Transaction> transactions = transactionRepository.findTop5ByOrderByAmountDesc();

        assertThat(transactions).hasSize(5);
        assertThat(transactions)
                .extracting(Transaction::getAmount)
                .containsExactly(
                        new BigDecimal("600.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("400.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("200.00")
                );
    }

    @Test
    void testCustomJpqlQuery() {
        persistTransaction(persistAccount("ACC3001"), "99.99", TransactionType.DEBIT, TransactionStatus.SUCCESS, 1);
        persistTransaction(persistAccount("ACC3002"), "100.01", TransactionType.DEBIT, TransactionStatus.SUCCESS, 2);
        persistTransaction(persistAccount("ACC3003"), "450.00", TransactionType.CREDIT, TransactionStatus.FAILED, 3);

        List<Transaction> transactions = transactionRepository.findTransactionsAboveAmount(new BigDecimal("100.00"));

        assertThat(transactions).hasSize(2);
        assertThat(transactions)
                .extracting(Transaction::getAmount)
                .containsExactlyInAnyOrder(new BigDecimal("100.01"), new BigDecimal("450.00"));
    }

    private Account persistAccount(String accountNumber) {
        Account account = new Account(
                accountNumber,
                "Holder-" + accountNumber,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE
        );
        entityManager.persist(account);
        return account;
    }

    private void persistTransaction(
            Account account,
            String amount,
            TransactionType type,
            TransactionStatus status,
            int sequence
    ) {
        entityManager.persist(new Transaction(
                account,
                new BigDecimal(amount),
                "USD",
                type,
                status,
                LocalDateTime.of(2026, 3, 22, 10, sequence)
        ));
        entityManager.flush();
        entityManager.clear();
    }
}
