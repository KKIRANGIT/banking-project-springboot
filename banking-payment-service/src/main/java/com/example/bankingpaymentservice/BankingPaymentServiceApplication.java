package com.example.bankingpaymentservice;

import com.example.bankingpaymentservice.model.Account;
import com.example.bankingpaymentservice.model.AccountStatus;
import com.example.bankingpaymentservice.model.Transaction;
import com.example.bankingpaymentservice.model.TransactionStatus;
import com.example.bankingpaymentservice.model.TransactionType;
import com.example.bankingpaymentservice.service.TransactionAnalyzer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableKafka
@EnableCaching
@EnableAsync
@EnableScheduling
public class BankingPaymentServiceApplication {

    public static void main(String[] args) {
        List<Transaction> transactions = createDummyTransactions();

        System.out.println("Top five transactions by amount:");
        TransactionAnalyzer.findTopFiveByAmount(transactions).forEach(System.out::println);

        System.out.println("\nTransactions grouped by status:");
        System.out.println(TransactionAnalyzer.groupByStatus(transactions));

        System.out.println("\nTotal amount by transaction type:");
        System.out.println(TransactionAnalyzer.getTotalAmountByType(transactions));

        System.out.println("\nDuplicate account numbers:");
        System.out.println(TransactionAnalyzer.findDuplicateAccountNumbers(transactions));

        SpringApplication.run(BankingPaymentServiceApplication.class, args);
    }

    private static List<Transaction> createDummyTransactions() {
        Account accountOne = buildAccount("ACC1001", "Alice Johnson", "5000.00");
        Account accountTwo = buildAccount("ACC1002", "Brian Smith", "2500.00");
        Account accountThree = buildAccount("ACC1003", "Carla Davis", "1200.00");
        Account accountFour = buildAccount("ACC1004", "David Wilson", "900.00");
        Account accountFive = buildAccount("ACC1005", "Eva Brown", "300.00");
        Account accountSix = buildAccount("ACC1006", "Frank Miller", "1800.00");
        Account accountSeven = buildAccount("ACC1007", "Grace Taylor", "760.00");
        Account accountEight = buildAccount("ACC1008", "Henry Moore", "50.00");

        return List.of(
                buildTransaction(accountOne, "1250.75", TransactionType.CREDIT, TransactionStatus.SUCCESS, 10),
                buildTransaction(accountTwo, "320.00", TransactionType.DEBIT, TransactionStatus.PENDING, 9),
                buildTransaction(accountThree, "875.20", TransactionType.CREDIT, TransactionStatus.SUCCESS, 8),
                buildTransaction(accountFour, "1450.00", TransactionType.DEBIT, TransactionStatus.FAILED, 7),
                buildTransaction(accountOne, "2200.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 6),
                buildTransaction(accountFive, "99.99", TransactionType.DEBIT, TransactionStatus.PENDING, 5),
                buildTransaction(accountSix, "1800.10", TransactionType.CREDIT, TransactionStatus.SUCCESS, 4),
                buildTransaction(accountTwo, "430.45", TransactionType.DEBIT, TransactionStatus.FAILED, 3),
                buildTransaction(accountSeven, "760.00", TransactionType.CREDIT, TransactionStatus.SUCCESS, 2),
                buildTransaction(accountEight, "50.00", TransactionType.DEBIT, TransactionStatus.PENDING, 1)
        );
    }

    private static Account buildAccount(String accountNumber, String holderName, String balance) {
        return new Account(accountNumber, holderName, new BigDecimal(balance), AccountStatus.ACTIVE);
    }

    private static Transaction buildTransaction(
            Account account,
            String amount,
            TransactionType type,
            TransactionStatus status,
            int hoursAgo
    ) {
        return new Transaction(
                account,
                new BigDecimal(amount),
                "USD",
                type,
                status,
                LocalDateTime.now().minusHours(hoursAgo)
        );
    }
}
