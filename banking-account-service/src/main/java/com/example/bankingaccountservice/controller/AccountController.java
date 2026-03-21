package com.example.bankingaccountservice.controller;

import com.example.bankingaccountservice.dto.AccountRequest;
import com.example.bankingaccountservice.dto.AccountResponse;
import com.example.bankingaccountservice.dto.BalanceUpdateRequest;
import com.example.bankingaccountservice.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse getAccount(@PathVariable String accountNumber) {
        return accountService.getAccountByNumber(accountNumber);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody AccountRequest request) {
        return accountService.createAccount(request);
    }

    @PutMapping("/{accountNumber}/balance")
    public AccountResponse updateBalance(
            @PathVariable String accountNumber,
            @Valid @RequestBody BalanceUpdateRequest request
    ) {
        return accountService.updateBalance(accountNumber, request.getAmount());
    }
}
