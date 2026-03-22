package com.example.bankingpaymentservice.service;

import com.example.bankingpaymentservice.config.KafkaConfig;
import com.example.bankingpaymentservice.kafka.TransactionEvent;
import com.example.bankingpaymentservice.kafka.TransactionEventType;
import com.example.bankingpaymentservice.model.Transaction;
import com.example.bankingpaymentservice.util.SensitiveDataMasker;
import io.micrometer.core.annotation.Timed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Timed(value = "payment.service.execution", histogram = true)
public class TransactionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventProducer.class);

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final Clock clock;

    public TransactionEventProducer(KafkaTemplate<String, TransactionEvent> kafkaTemplate, Clock clock) {
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    public void publishTransactionInitiated(Transaction transaction) {
        TransactionEvent event = toEvent(transaction, TransactionEventType.INITIATED, "Transaction initiated");
        publish(KafkaConfig.TOPIC_TRANSACTION_INITIATED, event);
        publish(KafkaConfig.TOPIC_AUDIT_EVENTS, event);
    }

    public void publishTransactionCompleted(Transaction transaction) {
        TransactionEvent event = toEvent(transaction, TransactionEventType.COMPLETED, "Transaction completed");
        publish(KafkaConfig.TOPIC_TRANSACTION_COMPLETED, event);
        publish(KafkaConfig.TOPIC_AUDIT_EVENTS, event);
    }

    public void publishTransactionFailed(Transaction transaction) {
        TransactionEvent event = toEvent(transaction, TransactionEventType.FAILED, "Transaction failed");
        publish(KafkaConfig.TOPIC_TRANSACTION_FAILED, event);
        publish(KafkaConfig.TOPIC_AUDIT_EVENTS, event);
    }

    private void publish(String topic, TransactionEvent event) {
        kafkaTemplate.send(topic, event.getAccountNumber(), event);
        String maskedAccountNumber = SensitiveDataMasker.maskAccountNumber(event.getAccountNumber());
        log.info(
                "Published eventType={} account={} topic={} partitionKey={}",
                event.getEventType(),
                maskedAccountNumber,
                topic,
                maskedAccountNumber
        );
    }

    private TransactionEvent toEvent(Transaction transaction, TransactionEventType eventType, String message) {
        return TransactionEvent.builder()
                .transactionId(transaction.getId())
                .accountNumber(transaction.getAccountNumber())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .eventType(eventType)
                .occurredAt(LocalDateTime.now(clock))
                .message(message)
                .build();
    }
}
