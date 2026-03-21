package com.example.bankingpaymentservice.dto;

import com.example.bankingpaymentservice.model.AccountStatus;
import java.math.BigDecimal;
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
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private String accountHolderName;
    private BigDecimal balance;
    private AccountStatus status;
    private Long version;
}
