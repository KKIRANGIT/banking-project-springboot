package com.example.bankingapigateway.filter;

import com.example.bankingapigateway.security.JwtTokenValidator;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtValidationGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtValidationGatewayFilter.class);
    private static final String MDC_KEY = "correlationId";
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/payments/auth/register",
            "/api/payments/auth/login"
    );

    private final JwtTokenValidator jwtTokenValidator;

    public JwtValidationGatewayFilter(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith("/api/")
                || PUBLIC_PATHS.contains(path)
                || HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing bearer token");
        }

        String token = authorizationHeader.substring(7);
        if (!jwtTokenValidator.isValid(token)) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationIdGatewayFilter.CORRELATION_ID_HEADER);
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put(MDC_KEY, correlationId);
        }
        try {
            LOGGER.warn("Gateway JWT validation failed message={} path={}", message, exchange.getRequest().getPath());
        } finally {
            MDC.remove(MDC_KEY);
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer body = exchange.getResponse().bufferFactory().wrap(
                ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8)
        );
        return exchange.getResponse().writeWith(Mono.just(body));
    }
}
