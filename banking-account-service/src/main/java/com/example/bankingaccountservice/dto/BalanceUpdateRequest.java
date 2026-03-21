package com.example.bankingaccountservice.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BalanceUpdateRequest {

    @NotNull(message = "Amount is required")
    private BigDecimal amount;
}
