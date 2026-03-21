package com.example.bankingpaymentservice.repository;

import com.example.bankingpaymentservice.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
