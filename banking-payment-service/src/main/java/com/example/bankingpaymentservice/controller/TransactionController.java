package com.example.bankingpaymentservice.controller;

import com.example.bankingpaymentservice.dto.TransactionRequest;
import com.example.bankingpaymentservice.dto.TransactionResponse;
import com.example.bankingpaymentservice.service.TransactionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /*
    Example with TELLER or ADMIN token and idempotency key:
    curl -X POST http://localhost:8081/api/transactions ^
      -H "Authorization: Bearer <JWT_TOKEN>" ^
      -H "Idempotency-Key: 123e4567-e89b-12d3-a456-426614174000" ^
      -H "Content-Type: application/json" ^
      -d "{\"accountNumber\":\"ACC1001\",\"amount\":500.00,\"type\":\"CREDIT\"}"
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@Valid @RequestBody TransactionRequest request) {
        return transactionService.createTransaction(request);
    }

    /*
    Example with CUSTOMER, TELLER, or ADMIN token:
    curl http://localhost:8081/api/transactions ^
      -H "Authorization: Bearer <JWT_TOKEN>"
     */
    @GetMapping
    public List<TransactionResponse> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    /*
    Example with CUSTOMER, TELLER, or ADMIN token:
    curl http://localhost:8081/api/transactions/1 ^
      -H "Authorization: Bearer <JWT_TOKEN>"
     */
    @GetMapping("/{id}")
    public TransactionResponse getTransactionById(@PathVariable Long id) {
        return transactionService.getTransactionById(id);
    }

    /*
    Example with CUSTOMER, TELLER, or ADMIN token:
    curl http://localhost:8081/api/transactions/account/ACC1001 ^
      -H "Authorization: Bearer <JWT_TOKEN>"
     */
    @GetMapping("/account/{accountNumber}")
    public List<TransactionResponse> getTransactionsByAccount(@PathVariable String accountNumber) {
        return transactionService.getTransactionsByAccount(accountNumber);
    }

    /*
    Example with ADMIN token:
    curl -X DELETE http://localhost:8081/api/transactions/1 ^
      -H "Authorization: Bearer <JWT_TOKEN>"
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
    }
}
