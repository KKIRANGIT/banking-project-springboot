package com.example.bankingpaymentservice.dto;

import com.example.bankingpaymentservice.model.TransactionStatus;
import com.example.bankingpaymentservice.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private String accountNumber;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private TransactionStatus status;
    private LocalDateTime createdAt;
}
