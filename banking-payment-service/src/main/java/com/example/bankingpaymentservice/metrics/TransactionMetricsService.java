package com.example.bankingpaymentservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class TransactionMetricsService {

    private final Counter createdCounter;
    private final Counter failedCounter;

    public TransactionMetricsService(MeterRegistry meterRegistry) {
        this.createdCounter = Counter.builder("transactions.created.total")
                .description("Total number of persisted transactions created")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("transactions.failed.total")
                .description("Total number of failed transactions")
                .register(meterRegistry);
    }

    public void incrementCreated() {
        createdCounter.increment();
    }

    public void incrementFailed() {
        failedCounter.increment();
    }
}
