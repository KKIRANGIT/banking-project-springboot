package com.example.bankingpaymentservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "banking-payment-service-jwt-secret-key-change-me-1234567890";

    @Test
    void testGenerateToken_Success() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000L);

        String token = provider.generateToken("alice", "TELLER");

        assertThat(token).isNotBlank();
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("alice");
        assertThat(provider.getRoleFromToken(token)).isEqualTo("TELLER");
    }

    @Test
    void testValidateToken_ValidToken() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000L);
        String token = provider.generateToken("alice", "ADMIN");

        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void testValidateToken_ExpiredToken() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, -1_000L);
        String token = provider.generateToken("alice", "ADMIN");

        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    void testGetUsernameFromToken() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000L);
        String token = provider.generateToken("bob", "CUSTOMER");

        assertThat(provider.getUsernameFromToken(token)).isEqualTo("bob");
    }
}
