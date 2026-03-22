package com.example.bankingpaymentservice.service;

import com.example.bankingpaymentservice.client.AccountServiceClient;
import com.example.bankingpaymentservice.dto.AccountBalanceUpdateRequest;
import com.example.bankingpaymentservice.dto.AccountResponse;
import com.example.bankingpaymentservice.exception.AccountClientNotFoundException;
import com.example.bankingpaymentservice.model.AccountStatus;
import com.example.bankingpaymentservice.util.SensitiveDataMasker;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@Timed(value = "payment.service.execution", histogram = true)
public class RemoteAccountService {

    private static final Logger log = LoggerFactory.getLogger(RemoteAccountService.class);

    private final AccountServiceClient accountServiceClient;

    public RemoteAccountService(AccountServiceClient accountServiceClient) {
        this.accountServiceClient = accountServiceClient;
    }

    @Retry(name = "accountService")
    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountFallback")
    public AccountResponse getAccount(String accountNumber) {
        try {
            return accountServiceClient.getAccount(accountNumber);
        } catch (FeignException.NotFound exception) {
            throw new AccountClientNotFoundException("Account not found for account number: " + accountNumber);
        }
    }

    @Retry(name = "accountService")
    @CircuitBreaker(name = "accountService", fallbackMethod = "updateBalanceFallback")
    public AccountResponse updateBalance(String accountNumber, BigDecimal amount) {
        try {
            return accountServiceClient.updateBalance(accountNumber, new AccountBalanceUpdateRequest(amount));
        } catch (FeignException.NotFound exception) {
            throw new AccountClientNotFoundException("Account not found for account number: " + accountNumber);
        }
    }

    @SuppressWarnings("unused")
    private AccountResponse getAccountFallback(String accountNumber, Throwable throwable) {
        log.warn(
                "Account lookup fallback account={} reason={}",
                SensitiveDataMasker.maskAccountNumber(accountNumber),
                throwable.toString()
        );
        return unknownAccount(accountNumber);
    }

    @SuppressWarnings("unused")
    private AccountResponse updateBalanceFallback(String accountNumber, BigDecimal amount, Throwable throwable) {
        log.warn(
                "Account balance fallback account={} amount={} reason={}",
                SensitiveDataMasker.maskAccountNumber(accountNumber),
                amount,
                throwable.toString()
        );
        return unknownAccount(accountNumber);
    }

    private AccountResponse unknownAccount(String accountNumber) {
        return AccountResponse.builder()
                .accountNumber(accountNumber)
                .accountHolderName("UNKNOWN")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.UNKNOWN)
                .version(-1L)
                .build();
    }
}
