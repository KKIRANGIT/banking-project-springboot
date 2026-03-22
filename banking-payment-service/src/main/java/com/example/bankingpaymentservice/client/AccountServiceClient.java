package com.example.bankingpaymentservice.client;

import com.example.bankingpaymentservice.dto.AccountBalanceUpdateRequest;
import com.example.bankingpaymentservice.dto.AccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "banking-account-service", configuration = AccountServiceClientConfig.class)
public interface AccountServiceClient {

    @GetMapping("/api/accounts/{accountNumber}")
    AccountResponse getAccount(@PathVariable("accountNumber") String accountNumber);

    @PutMapping("/api/accounts/{accountNumber}/balance")
    AccountResponse updateBalance(
            @PathVariable("accountNumber") String accountNumber,
            @RequestBody AccountBalanceUpdateRequest request
    );
}
