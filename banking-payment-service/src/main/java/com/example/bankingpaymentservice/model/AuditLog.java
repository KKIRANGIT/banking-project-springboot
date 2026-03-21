package com.example.bankingpaymentservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_name", nullable = false, length = 100)
    private String topicName;

    @Column(name = "event_key", nullable = false, length = 100)
    private String eventKey;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
