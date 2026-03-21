package com.example.bankingpaymentservice.config;

import com.example.bankingpaymentservice.kafka.TransactionEvent;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.apache.kafka.clients.admin.NewTopic;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_TRANSACTION_INITIATED = "payment.transaction.initiated";
    public static final String TOPIC_TRANSACTION_COMPLETED = "payment.transaction.completed";
    public static final String TOPIC_TRANSACTION_FAILED = "payment.transaction.failed";
    public static final String TOPIC_AUDIT_EVENTS = "payment.audit.events";
    public static final String TOPIC_AUDIT_EVENTS_DLT = "payment.audit.events.DLT";

    @Bean
    public NewTopic paymentTransactionInitiatedTopic() {
        return TopicBuilder.name(TOPIC_TRANSACTION_INITIATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentTransactionCompletedTopic() {
        return TopicBuilder.name(TOPIC_TRANSACTION_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentTransactionFailedTopic() {
        return TopicBuilder.name(TOPIC_TRANSACTION_FAILED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentAuditEventsTopic() {
        return TopicBuilder.name(TOPIC_AUDIT_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentAuditEventsDltTopic() {
        return TopicBuilder.name(TOPIC_AUDIT_EVENTS_DLT).partitions(3).replicas(1).build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionEvent> auditKafkaListenerContainerFactory(
            ConsumerFactory<String, TransactionEvent> consumerFactory,
            KafkaOperations<String, TransactionEvent> kafkaOperations
    ) {
        ConcurrentKafkaListenerContainerFactory<String, TransactionEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) -> new TopicPartition(TOPIC_AUDIT_EVENTS_DLT, record.partition())
        );
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
