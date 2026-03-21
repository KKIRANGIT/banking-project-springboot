package com.example.bankingpaymentservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "account")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Transaction(
            Account account,
            BigDecimal amount,
            String currency,
            TransactionType type,
            TransactionStatus status,
            LocalDateTime createdAt
    ) {
        this.account = account;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getAccountNumber() {
        return account == null ? null : account.getAccountNumber();
    }
}
