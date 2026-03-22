package com.example.bankingpaymentservice.service;

import com.example.bankingpaymentservice.model.Transaction;
import com.example.bankingpaymentservice.util.SensitiveDataMasker;
import io.micrometer.core.annotation.Timed;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Timed(value = "payment.service.execution", histogram = true)
public class FraudCheckService {

    private static final Logger log = LoggerFactory.getLogger(FraudCheckService.class);

    @Async("transactionProcessingExecutor")
    public CompletableFuture<Boolean> checkFraud(Transaction transaction) {
        String threadName = Thread.currentThread().getName();
        String maskedAccountNumber = SensitiveDataMasker.maskAccountNumber(transaction.getAccountNumber());
        log.debug(
                "Starting fraud check account={} thread={}",
                maskedAccountNumber,
                threadName
        );

        sleepOneSecond();

        boolean isFraudulent = transaction.getAmount().doubleValue() >= 10000
                || transaction.getAccountNumber().endsWith("999");

        log.debug(
                "Completed fraud check account={} result={} thread={}",
                maskedAccountNumber,
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
