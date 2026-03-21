package com.example.bankingpaymentservice.controller;

import com.example.bankingpaymentservice.service.PaymentService;
import org.springframework.stereotype.Controller;

@Controller
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public String serviceStatus() {
        return paymentService.getServiceStatus();
    }
}
