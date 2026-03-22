package com.example.bankingpaymentservice.service;

import com.example.bankingpaymentservice.util.SensitiveDataMasker;
import io.micrometer.core.annotation.Timed;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Timed(value = "payment.service.execution", histogram = true)
public class SanctionsCheckService {

    private static final Logger log = LoggerFactory.getLogger(SanctionsCheckService.class);

    @Async("transactionProcessingExecutor")
    public CompletableFuture<Boolean> checkSanctions(String accountNumber) {
        String threadName = Thread.currentThread().getName();
        String maskedAccountNumber = SensitiveDataMasker.maskAccountNumber(accountNumber);
        log.debug(
                "Starting sanctions check account={} thread={}",
                maskedAccountNumber,
                threadName
        );

        sleepOneSecond();

        boolean isSanctioned = accountNumber.startsWith("SAN")
                || accountNumber.endsWith("000");

        log.debug(
                "Completed sanctions check account={} result={} thread={}",
                maskedAccountNumber,
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
