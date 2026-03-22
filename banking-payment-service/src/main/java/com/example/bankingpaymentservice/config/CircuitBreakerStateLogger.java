package com.example.bankingpaymentservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CircuitBreakerStateLogger {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerStateLogger.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerStateLogger(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @PostConstruct
    public void registerStateTransitionLogger() {
        circuitBreakerRegistry.circuitBreaker("accountService")
                .getEventPublisher()
                .onStateTransition(event -> log.info(
                        "Circuit breaker name={} fromState={} toState={}",
                        event.getCircuitBreakerName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()
                ));
    }
}
