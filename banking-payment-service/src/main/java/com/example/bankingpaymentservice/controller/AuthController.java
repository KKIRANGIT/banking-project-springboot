package com.example.bankingpaymentservice.controller;

import com.example.bankingpaymentservice.dto.AuthResponse;
import com.example.bankingpaymentservice.dto.LoginRequest;
import com.example.bankingpaymentservice.dto.RegisterRequest;
import com.example.bankingpaymentservice.dto.UserResponse;
import com.example.bankingpaymentservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /*
    Example:
    curl -X POST http://localhost:8081/api/auth/register ^
      -H "Content-Type: application/json" ^
      -d "{\"username\":\"customer1\",\"password\":\"Password@123\",\"role\":\"CUSTOMER\"}"
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /*
    Example:
    curl -X POST http://localhost:8081/api/auth/login ^
      -H "Content-Type: application/json" ^
      -d "{\"username\":\"customer1\",\"password\":\"Password@123\"}"
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
