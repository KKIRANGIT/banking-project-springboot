package com.example.bankingpaymentservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bankingpaymentservice.model.Account;
import com.example.bankingpaymentservice.model.AccountStatus;
import com.example.bankingpaymentservice.model.Transaction;
import com.example.bankingpaymentservice.model.TransactionStatus;
import com.example.bankingpaymentservice.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TransactionAnalyzerTest {

    @Test
    void testFindTopFiveByAmount() {
        List<Transaction> transactions = List.of(
                transaction("ACC1", "100.00", TransactionType.CREDIT, TransactionStatus.SUCCESS),
                transaction("ACC2", "900.00", TransactionType.DEBIT, TransactionStatus.SUCCESS),
                transaction("ACC3", "300.00", TransactionType.CREDIT, TransactionStatus.PENDING),
                transaction("ACC4", "700.00", TransactionType.DEBIT, TransactionStatus.FAILED),
                transaction("ACC5", "500.00", TransactionType.CREDIT, TransactionStatus.SUCCESS),
                transaction("ACC6", "200.00", TransactionType.DEBIT, TransactionStatus.PENDING),
                transaction("ACC7", "50.00", TransactionType.CREDIT, TransactionStatus.FAILED)
        );

        List<Transaction> topFive = TransactionAnalyzer.findTopFiveByAmount(transactions);

        assertThat(topFive).hasSize(5);
        assertThat(topFive)
                .extracting(Transaction::getAmount)
                .containsExactly(
                        new BigDecimal("900.00"),
                        new BigDecimal("700.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("200.00")
                );
    }

    @Test
    void testGroupByStatus() {
        List<Transaction> transactions = List.of(
                transaction("ACC1", "100.00", TransactionType.CREDIT, TransactionStatus.SUCCESS),
                transaction("ACC2", "200.00", TransactionType.DEBIT, TransactionStatus.SUCCESS),
                transaction("ACC3", "300.00", TransactionType.CREDIT, TransactionStatus.FAILED),
                transaction("ACC4", "400.00", TransactionType.DEBIT, TransactionStatus.PENDING)
        );

        Map<TransactionStatus, List<Transaction>> grouped = TransactionAnalyzer.groupByStatus(transactions);

        assertThat(grouped).containsKeys(TransactionStatus.SUCCESS, TransactionStatus.FAILED, TransactionStatus.PENDING);
        assertThat(grouped.get(TransactionStatus.SUCCESS)).hasSize(2);
        assertThat(grouped.get(TransactionStatus.FAILED)).hasSize(1);
        assertThat(grouped.get(TransactionStatus.PENDING)).hasSize(1);
    }

    @Test
    void testGetTotalAmountByType() {
        List<Transaction> transactions = List.of(
                transaction("ACC1", "100.00", TransactionType.CREDIT, TransactionStatus.SUCCESS),
                transaction("ACC2", "250.00", TransactionType.CREDIT, TransactionStatus.SUCCESS),
                transaction("ACC3", "80.00", TransactionType.DEBIT, TransactionStatus.FAILED),
                transaction("ACC4", "20.00", TransactionType.DEBIT, TransactionStatus.PENDING)
        );

        Map<TransactionType, BigDecimal> totals = TransactionAnalyzer.getTotalAmountByType(transactions);

        assertThat(totals.get(TransactionType.CREDIT)).isEqualByComparingTo("350.00");
        assertThat(totals.get(TransactionType.DEBIT)).isEqualByComparingTo("100.00");
    }

    @Test
    void testFindDuplicateAccountNumbers() {
        List<Transaction> transactions = List.of(
                transaction("ACC1", "100.00", TransactionType.CREDIT, TransactionStatus.SUCCESS),
                transaction("ACC1", "200.00", TransactionType.DEBIT, TransactionStatus.SUCCESS),
                transaction("ACC2", "300.00", TransactionType.CREDIT, TransactionStatus.FAILED),
                transaction("ACC2", "400.00", TransactionType.DEBIT, TransactionStatus.PENDING),
                transaction("ACC3", "500.00", TransactionType.CREDIT, TransactionStatus.SUCCESS)
        );

        Set<String> duplicates = TransactionAnalyzer.findDuplicateAccountNumbers(transactions);

        assertThat(duplicates).containsExactlyInAnyOrder("ACC1", "ACC2");
    }

    private Transaction transaction(
            String accountNumber,
            String amount,
            TransactionType type,
            TransactionStatus status
    ) {
        Account account = new Account(accountNumber, "Holder-" + accountNumber, new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        return new Transaction(
                account,
                new BigDecimal(amount),
                "USD",
                type,
                status,
                LocalDateTime.of(2026, 3, 22, 10, 0)
        );
    }
}
