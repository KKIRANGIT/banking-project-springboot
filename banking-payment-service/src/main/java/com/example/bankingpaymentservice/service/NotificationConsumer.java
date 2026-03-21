package com.example.bankingpaymentservice.service;

import com.example.bankingpaymentservice.config.KafkaConfig;
import com.example.bankingpaymentservice.kafka.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @KafkaListener(topics = KafkaConfig.TOPIC_TRANSACTION_COMPLETED, groupId = "notification-service-group")
    public void consumeCompletedTransaction(TransactionEvent event) {
        log.info(
                "SMS sent to customer: {} \u2014 Transaction {} completed",
                event.getAccountNumber(),
                event.getTransactionId()
        );
    }
}
