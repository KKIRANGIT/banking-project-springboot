package com.example.bankingpaymentservice.service;

import com.example.bankingpaymentservice.dto.AccountResponse;
import com.example.bankingpaymentservice.model.Account;
import com.example.bankingpaymentservice.repository.AccountRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Cacheable(cacheNames = "accounts", key = "#accountNumber")
    public AccountResponse getAccountSnapshotByNumber(String accountNumber) {
        Account account = findAccountSnapshotByNumber(accountNumber);
        return account == null ? null : mapToResponse(account);
    }

    @Transactional
    public Account syncAccountSnapshot(AccountResponse remoteAccount) {
        Account account = accountRepository.findByAccountNumber(remoteAccount.getAccountNumber())
                .orElseGet(Account::new);

        account.setAccountNumber(remoteAccount.getAccountNumber());
        account.setAccountHolderName(remoteAccount.getAccountHolderName());
        account.setBalance(remoteAccount.getBalance());
        account.setStatus(remoteAccount.getStatus());

        return accountRepository.save(account);
    }

    @Transactional
    public void updateAccount(Account account) {
        accountRepository.save(account);
    }

    public Account findAccountSnapshotByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElse(null);
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
