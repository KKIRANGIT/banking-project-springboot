package com.example.bankingpaymentservice.service;

import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;

@Service
@Timed(value = "payment.service.execution", histogram = true)
public class PaymentService {

    public String getServiceStatus() {
        return "banking-payment-service is ready";
    }
}
