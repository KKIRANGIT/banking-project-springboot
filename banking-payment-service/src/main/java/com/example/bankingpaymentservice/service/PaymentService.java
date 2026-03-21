package com.example.bankingpaymentservice.service;

import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    public String getServiceStatus() {
        return "banking-payment-service is ready";
    }
}
