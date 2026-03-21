package com.example.bankingpaymentservice.service;

import com.example.bankingpaymentservice.model.Transaction;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class FraudCheckService {

    private static final Logger log = LoggerFactory.getLogger(FraudCheckService.class);

    @Async("transactionProcessingExecutor")
    public CompletableFuture<Boolean> checkFraud(Transaction transaction) {
        String threadName = Thread.currentThread().getName();
        log.info(
                "Starting fraud check for account {} on thread {}",
                transaction.getAccountNumber(),
                threadName
        );

        sleepOneSecond();

        boolean isFraudulent = transaction.getAmount().doubleValue() >= 10000
                || transaction.getAccountNumber().endsWith("999");

        log.info(
                "Completed fraud check for account {} with result {} on thread {}",
                transaction.getAccountNumber(),
                isFraudulent,
                threadName
        );
        return CompletableFuture.completedFuture(isFraudulent);
    }

    private void sleepOneSecond() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Fraud check interrupted", exception);
        }
    }
}
