package com.example.bankingaccountservice.dto;

import com.example.bankingaccountservice.model.AccountStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class AccountRequest {

    @NotBlank(message = "Account number must not be blank")
    private String accountNumber;

    @NotBlank(message = "Account holder name must not be blank")
    private String accountHolderName;

    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Balance must not be negative")
    private BigDecimal balance;

    @NotNull(message = "Account status is required")
    private AccountStatus status;
}
