package com.example.bankingpaymentservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TransactionScheduler {

    private static final Logger log = LoggerFactory.getLogger(TransactionScheduler.class);

    private final TransactionService transactionService;

    public TransactionScheduler(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Scheduled(fixedRate = 30000)
    public void printPendingTransactionCount() {
        log.info(
                "Pending transactions count: {} on thread {}",
                transactionService.countPendingTransactions(),
                Thread.currentThread().getName()
        );
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void printDailySummary() {
        TransactionService.DailyTransactionSummary summary = transactionService.getTodaySummary();
        log.info(
                "Daily summary for today on thread {} -> total DEBIT: {}, total CREDIT: {}",
                Thread.currentThread().getName(),
                summary.totalDebit(),
                summary.totalCredit()
        );
    }
}
