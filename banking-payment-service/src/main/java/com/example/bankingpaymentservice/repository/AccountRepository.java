package com.example.bankingpaymentservice.repository;

import com.example.bankingpaymentservice.model.Account;
import com.example.bankingpaymentservice.model.AccountStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByStatus(AccountStatus status);

    @Query(value = "SELECT * FROM accounts ORDER BY balance DESC LIMIT 1", nativeQuery = true)
    Optional<Account> findAccountWithHighestBalance();
}
