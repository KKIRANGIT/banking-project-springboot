package com.example.bankingpaymentservice.client;

import feign.Logger;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class AccountServiceClientConfig {

    @Bean
    public RequestInterceptor accountServiceAuthPropagationInterceptor() {
        return requestTemplate -> {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
                HttpServletRequest request = servletRequestAttributes.getRequest();
                String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                    requestTemplate.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
                }
            }
        };
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
