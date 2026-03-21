package com.example.bankingpaymentservice.aspect;

import com.example.bankingpaymentservice.dto.TransactionResponse;
import com.example.bankingpaymentservice.exception.InvalidTransactionException;
import com.example.bankingpaymentservice.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class TransactionIdempotencyAspect {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final IdempotencyService idempotencyService;

    public TransactionIdempotencyAspect(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Around("execution(* com.example.bankingpaymentservice.controller.TransactionController.createTransaction(..))")
    public Object applyIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);

        if (!StringUtils.hasText(idempotencyKey)) {
            return joinPoint.proceed();
        }

        validateKey(idempotencyKey);

        Optional<TransactionResponse> storedResponse = idempotencyService.getStoredResponse(idempotencyKey);
        if (storedResponse.isPresent()) {
            return storedResponse.get();
        }

        Object result = joinPoint.proceed();
        if (result instanceof TransactionResponse response) {
            idempotencyService.storeResponse(idempotencyKey, response);
        }
        return result;
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            throw new IllegalStateException("No current HTTP request available for idempotency handling");
        }
        return servletAttributes.getRequest();
    }

    private void validateKey(String idempotencyKey) {
        try {
            UUID.fromString(idempotencyKey);
        } catch (IllegalArgumentException exception) {
            throw new InvalidTransactionException("Idempotency-Key must be a valid UUID");
        }
    }
}
