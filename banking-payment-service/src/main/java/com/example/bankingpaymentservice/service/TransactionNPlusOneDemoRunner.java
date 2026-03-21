package com.example.bankingpaymentservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class TransactionNPlusOneDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TransactionNPlusOneDemoRunner.class);

    private final TransactionService transactionService;

    public TransactionNPlusOneDemoRunner(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    public void run(String... args) {
        log.info("Demonstrating N+1 queries with lazy account loading");
        transactionService.demonstrateNPlusOneProblem();

        log.info("Demonstrating JOIN FETCH optimization");
        transactionService.demonstrateJoinFetchFix();
    }
}
