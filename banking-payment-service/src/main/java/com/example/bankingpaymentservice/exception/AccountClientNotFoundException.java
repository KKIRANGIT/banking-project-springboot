package com.example.bankingpaymentservice.exception;

public class AccountClientNotFoundException extends RuntimeException {

    public AccountClientNotFoundException(String message) {
        super(message);
    }
}
