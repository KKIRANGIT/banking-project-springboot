package com.example.bankingpaymentservice.service;

import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SanctionsCheckService {

    private static final Logger log = LoggerFactory.getLogger(SanctionsCheckService.class);

    @Async("transactionProcessingExecutor")
    public CompletableFuture<Boolean> checkSanctions(String accountNumber) {
        String threadName = Thread.currentThread().getName();
        log.info(
                "Starting sanctions check for account {} on thread {}",
                accountNumber,
                threadName
        );

        sleepOneSecond();

        boolean isSanctioned = accountNumber.startsWith("SAN")
                || accountNumber.endsWith("000");

        log.info(
                "Completed sanctions check for account {} with result {} on thread {}",
                accountNumber,
                isSanctioned,
                threadName
        );
        return CompletableFuture.completedFuture(isSanctioned);
    }

    private void sleepOneSecond() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Sanctions check interrupted", exception);
        }
    }
}
