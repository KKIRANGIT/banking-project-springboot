package com.example.bankingaccountservice.service;

import com.example.bankingaccountservice.dto.AccountRequest;
import com.example.bankingaccountservice.dto.AccountResponse;
import com.example.bankingaccountservice.exception.AccountNotFoundException;
import com.example.bankingaccountservice.exception.InvalidAccountException;
import com.example.bankingaccountservice.model.Account;
import com.example.bankingaccountservice.model.AccountStatus;
import com.example.bankingaccountservice.repository.AccountRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse getAccountByNumber(String accountNumber) {
        return mapToResponse(findAccountEntityByNumber(accountNumber));
    }

    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        String accountNumber = normalizeAccountNumber(request.getAccountNumber());
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new InvalidAccountException("Account already exists for account number: " + accountNumber);
        }

        Account account = new Account(
                accountNumber,
                request.getAccountHolderName().trim(),
                request.getBalance(),
                request.getStatus()
        );
        return mapToResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse updateBalance(String accountNumber, BigDecimal amount) {
        Account account = findAccountEntityByNumber(accountNumber);

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountException("Balance updates are allowed only for ACTIVE accounts");
        }

        BigDecimal updatedBalance = account.getBalance().add(amount);
        if (updatedBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAccountException("Balance cannot become negative for account " + account.getAccountNumber());
        }

        account.applyBalanceDelta(amount);
        return mapToResponse(accountRepository.save(account));
    }

    private Account findAccountEntityByNumber(String accountNumber) {
        String normalizedAccountNumber = normalizeAccountNumber(accountNumber);
        return accountRepository.findByAccountNumber(normalizedAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found for account number: " + normalizedAccountNumber
                ));
    }

    private String normalizeAccountNumber(String accountNumber) {
        String normalizedAccountNumber = accountNumber == null ? "" : accountNumber.trim();
        if (normalizedAccountNumber.length() < 4) {
            throw new InvalidAccountException("Account number must contain at least 4 characters");
        }
        return normalizedAccountNumber;
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountHolderName(account.getAccountHolderName())
                .balance(account.getBalance())
                .status(account.getStatus())
                .version(account.getVersion())
                .build();
    }
}
