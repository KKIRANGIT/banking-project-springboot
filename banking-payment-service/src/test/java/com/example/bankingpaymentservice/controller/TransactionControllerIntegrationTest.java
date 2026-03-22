package com.example.bankingpaymentservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bankingpaymentservice.dto.TransactionResponse;
import com.example.bankingpaymentservice.model.TransactionStatus;
import com.example.bankingpaymentservice.model.TransactionType;
import com.example.bankingpaymentservice.security.JwtTokenProvider;
import com.example.bankingpaymentservice.service.IdempotencyService;
import com.example.bankingpaymentservice.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

    private static final String IDEMPOTENCY_KEY = "123e4567-e89b-12d3-a456-426614174000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        when(idempotencyService.getStoredResponse(anyString())).thenReturn(Optional.empty());
        doNothing().when(idempotencyService).storeResponse(anyString(), any(TransactionResponse.class));
    }

    @Test
    void testCreateTransactionEndpoint_WithValidJWT() throws Exception {
        TransactionResponse response = TransactionResponse.builder()
                .id(10L)
                .accountNumber("ACC1001")
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.of(2026, 3, 22, 10, 30))
                .build();

        when(transactionService.createTransaction(any())).thenReturn(response);

        mockMvc.perform(post("/api/transactions")
                        .header(AUTHORIZATION, bearer("teller-user", "TELLER"))
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"accountNumber\": \"ACC1001\",
                                  \"amount\": 250.00,
                                  \"type\": \"CREDIT\"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.accountNumber").value("ACC1001"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testCreateTransactionEndpoint_WithoutJWT() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"accountNumber\": \"ACC1001\",
                                  \"amount\": 250.00,
                                  \"type\": \"CREDIT\"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verify(transactionService, never()).createTransaction(any());
    }

    @Test
    void testCreateTransactionEndpoint_CustomerRole() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header(AUTHORIZATION, bearer("customer-user", "CUSTOMER"))
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"accountNumber\": \"ACC1001\",
                                  \"amount\": 250.00,
                                  \"type\": \"CREDIT\"
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(transactionService, never()).createTransaction(any());
    }

    @Test
    void testGetTransactions_Success() throws Exception {
        when(transactionService.getAllTransactions()).thenReturn(List.of(
                TransactionResponse.builder()
                        .id(1L)
                        .accountNumber("ACC1001")
                        .amount(new BigDecimal("100.00"))
                        .currency("USD")
                        .type(TransactionType.CREDIT)
                        .status(TransactionStatus.SUCCESS)
                        .createdAt(LocalDateTime.of(2026, 3, 22, 9, 0))
                        .build(),
                TransactionResponse.builder()
                        .id(2L)
                        .accountNumber("ACC1002")
                        .amount(new BigDecimal("50.00"))
                        .currency("USD")
                        .type(TransactionType.DEBIT)
                        .status(TransactionStatus.PENDING)
                        .createdAt(LocalDateTime.of(2026, 3, 22, 9, 15))
                        .build()
        ));

        mockMvc.perform(get("/api/transactions")
                        .header(AUTHORIZATION, bearer("viewer-user", "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$[0].accountNumber").value("ACC1001"))
                .andExpect(jsonPath("$[1].status").value("PENDING"));
    }

    private String bearer(String username, String role) {
        return "Bearer " + jwtTokenProvider.generateToken(username, role);
    }
}
