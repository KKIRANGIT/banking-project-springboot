package com.example.bankingpaymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableKafka
@EnableCaching
@EnableAsync
@EnableScheduling
public class BankingPaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingPaymentServiceApplication.class, args);
    }
}
