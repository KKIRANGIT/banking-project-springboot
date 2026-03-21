package com.example.bankingpaymentservice.exception;

public class InvalidTransactionException extends RuntimeException {

    public InvalidTransactionException(String message) {
        super(message);
    }
}
