package com.example.bankingpaymentservice.service;

import com.example.bankingpaymentservice.config.KafkaConfig;
import com.example.bankingpaymentservice.kafka.TransactionEvent;
import com.example.bankingpaymentservice.model.AuditLog;
import com.example.bankingpaymentservice.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditEventConsumer(AuditLogRepository auditLogRepository, ObjectMapper objectMapper, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @KafkaListener(
            topics = KafkaConfig.TOPIC_AUDIT_EVENTS,
            groupId = "audit-service-group",
            containerFactory = "auditKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeAuditEvent(
            TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setTopicName(topic);
        auditLog.setEventKey(key == null ? "" : key);
        auditLog.setTransactionId(event.getTransactionId());
        auditLog.setAccountNumber(event.getAccountNumber());
        auditLog.setEventType(event.getEventType().name());
        auditLog.setPayload(toPayload(event));
        auditLog.setCreatedAt(LocalDateTime.now(clock));
        auditLogRepository.save(auditLog);

        log.info(
                "Audit event consumed for account {} on topic {}",
                event.getAccountNumber(),
                topic
        );
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_AUDIT_EVENTS_DLT, groupId = "audit-service-group-dlt")
    public void consumeDltEvent(
            TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        log.error(
                "Audit event moved to DLT topic {} for account {} and transaction {}",
                topic,
                event.getAccountNumber(),
                event.getTransactionId()
        );
    }

    private String toPayload(TransactionEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize audit event", exception);
        }
    }
}
