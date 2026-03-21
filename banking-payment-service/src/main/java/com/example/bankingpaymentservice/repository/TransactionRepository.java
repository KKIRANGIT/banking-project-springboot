package com.example.bankingpaymentservice.repository;

import com.example.bankingpaymentservice.model.Transaction;
import com.example.bankingpaymentservice.model.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findTop5ByOrderByAmountDesc();

    List<Transaction> findByAccountAccountNumber(String accountNumber);

    long countByStatus(TransactionStatus status);

    List<Transaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("select t from Transaction t where t.amount > :amount")
    List<Transaction> findTransactionsAboveAmount(@Param("amount") BigDecimal amount);

    @Query("select t from Transaction t join fetch t.account order by t.createdAt desc")
    List<Transaction> findAllWithAccount();

    @Query("select t from Transaction t join fetch t.account where t.id = :id")
    Optional<Transaction> findByIdWithAccount(@Param("id") Long id);

    @Query("select t from Transaction t join fetch t.account where t.account.accountNumber = :accountNumber order by t.createdAt desc")
    List<Transaction> findByAccountNumberWithAccount(@Param("accountNumber") String accountNumber);
}
