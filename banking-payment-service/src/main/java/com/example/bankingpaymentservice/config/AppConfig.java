package com.example.bankingpaymentservice.config;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }

    @Bean(name = "transactionProcessingExecutor", destroyMethod = "shutdown")
    public ExecutorService transactionProcessingExecutor() {
        AtomicInteger threadCounter = new AtomicInteger(1);
        return Executors.newFixedThreadPool(5, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("txn-async-" + threadCounter.getAndIncrement());
            return thread;
        });
    }
}
