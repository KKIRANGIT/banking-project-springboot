package com.example.bankingpaymentservice.service;

import com.example.bankingpaymentservice.dto.TransactionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Timed(value = "payment.service.execution", histogram = true)
public class IdempotencyService {

    private static final String KEY_PREFIX = "idempotency:transactions:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<TransactionResponse> getStoredResponse(String idempotencyKey) {
        String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, TransactionResponse.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read stored idempotent response", exception);
        }
    }

    public void storeResponse(String idempotencyKey, TransactionResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + idempotencyKey, json, IDEMPOTENCY_TTL);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to store idempotent response", exception);
        }
    }
}
