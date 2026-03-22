package com.example.bankingpaymentservice.service;

import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Timed(value = "payment.component.execution", histogram = true)
public class TransactionScheduler {

    private static final Logger log = LoggerFactory.getLogger(TransactionScheduler.class);

    private final TransactionService transactionService;

    public TransactionScheduler(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Scheduled(fixedRate = 30000)
    public void printPendingTransactionCount() {
        log.info(
                "Pending transactions count={} thread={}",
                transactionService.countPendingTransactions(),
                Thread.currentThread().getName()
        );
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void printDailySummary() {
        TransactionService.DailyTransactionSummary summary = transactionService.getTodaySummary();
        log.info(
                "Daily summary thread={} totalDebit={} totalCredit={}",
                Thread.currentThread().getName(),
                summary.totalDebit(),
                summary.totalCredit()
        );
    }
}
