package com.example.bankingaccountservice.service;

import com.example.bankingaccountservice.dto.AccountRequest;
import com.example.bankingaccountservice.dto.AccountResponse;
import com.example.bankingaccountservice.exception.AccountNotFoundException;
import com.example.bankingaccountservice.exception.InvalidAccountException;
import com.example.bankingaccountservice.model.Account;
import com.example.bankingaccountservice.model.AccountStatus;
import com.example.bankingaccountservice.repository.AccountRepository;
import com.example.bankingaccountservice.util.SensitiveDataMasker;
import io.micrometer.core.annotation.Timed;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Timed(value = "account.service.execution", histogram = true)
@Transactional(readOnly = true)
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = findAccountEntityByNumber(accountNumber);
        log.info("Fetched account={}", SensitiveDataMasker.maskAccountNumber(account.getAccountNumber()));
        return mapToResponse(account);
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
        Account savedAccount = accountRepository.save(account);
        log.info(
                "Created account={} status={}",
                SensitiveDataMasker.maskAccountNumber(savedAccount.getAccountNumber()),
                savedAccount.getStatus()
        );
        return mapToResponse(savedAccount);
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
        Account savedAccount = accountRepository.save(account);
        log.info(
                "Updated balance account={} delta={} newBalance={}",
                SensitiveDataMasker.maskAccountNumber(savedAccount.getAccountNumber()),
                amount,
                savedAccount.getBalance()
        );
        return mapToResponse(savedAccount);
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
