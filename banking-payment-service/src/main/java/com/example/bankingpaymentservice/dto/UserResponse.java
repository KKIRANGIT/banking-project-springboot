package com.example.bankingpaymentservice.dto;

import com.example.bankingpaymentservice.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserResponse {

    private final Long id;
    private final String username;
    private final Role role;
}
