package com.example.bankingpaymentservice.exception;

public class TransactionProcessingException extends RuntimeException {

    public TransactionProcessingException(String message) {
        super(message);
    }
}
