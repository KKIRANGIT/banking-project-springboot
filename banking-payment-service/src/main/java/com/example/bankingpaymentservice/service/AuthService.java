package com.example.bankingpaymentservice.service;

import com.example.bankingpaymentservice.dto.AuthResponse;
import com.example.bankingpaymentservice.dto.LoginRequest;
import com.example.bankingpaymentservice.dto.RegisterRequest;
import com.example.bankingpaymentservice.dto.UserResponse;
import com.example.bankingpaymentservice.exception.InvalidCredentialsException;
import com.example.bankingpaymentservice.exception.UserAlreadyExistsException;
import com.example.bankingpaymentservice.model.User;
import com.example.bankingpaymentservice.repository.UserRepository;
import com.example.bankingpaymentservice.security.JwtTokenProvider;
import io.micrometer.core.annotation.Timed;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Timed(value = "payment.service.execution", histogram = true)
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username already exists: " + username);
        }

        User user = new User(
                username,
                passwordEncoder.encode(request.getPassword()),
                request.getRole()
        );
        User savedUser = userRepository.save(user);

        return UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .role(savedUser.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }
}
