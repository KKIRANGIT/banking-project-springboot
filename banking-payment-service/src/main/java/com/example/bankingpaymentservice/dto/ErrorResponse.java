package com.example.bankingpaymentservice.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private final String code;
    private final String message;
    private final LocalDateTime timestamp;
}
