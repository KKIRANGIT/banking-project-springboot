package com.example.bankingapigateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

@Component
public class RateLimitingGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitingGatewayFilter.class);
    private static final int MAX_REQUESTS_PER_SECOND = 10;
    private final ConcurrentHashMap<String, RequestWindow> requestWindows = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = resolveClientIp(exchange.getRequest());
        RequestWindow window = requestWindows.computeIfAbsent(clientIp, ignored -> new RequestWindow());

        if (window.allowRequest()) {
            return chain.filter(exchange);
        }

        LOGGER.warn("Rate limit exceeded for ip={}", clientIp);
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer body = exchange.getResponse().bufferFactory().wrap(
                "{\"error\":\"Too Many Requests\"}".getBytes(StandardCharsets.UTF_8)
        );
        return exchange.getResponse().writeWith(Mono.just(body));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private static final class RequestWindow {

        private long windowStart = System.currentTimeMillis();
        private int requestCount;

        private synchronized boolean allowRequest() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - windowStart >= 1000) {
                windowStart = currentTime;
                requestCount = 0;
            }

            if (requestCount >= MAX_REQUESTS_PER_SECOND) {
                return false;
            }

            requestCount++;
            return true;
        }
    }
}
