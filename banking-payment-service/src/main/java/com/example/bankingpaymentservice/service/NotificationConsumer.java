package com.example.bankingpaymentservice.service;

import com.example.bankingpaymentservice.config.KafkaConfig;
import com.example.bankingpaymentservice.kafka.TransactionEvent;
import com.example.bankingpaymentservice.util.SensitiveDataMasker;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Timed(value = "payment.component.execution", histogram = true)
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @KafkaListener(topics = KafkaConfig.TOPIC_TRANSACTION_COMPLETED, groupId = "notification-service-group")
    public void consumeCompletedTransaction(TransactionEvent event) {
        log.info(
                "Customer notification sent account={} transactionId={}",
                SensitiveDataMasker.maskAccountNumber(event.getAccountNumber()),
                event.getTransactionId()
        );
    }
}
