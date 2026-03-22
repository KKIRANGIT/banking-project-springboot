package com.example.bankingapigateway.filter;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdGatewayFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdGatewayFilter.class);
    private static final String MDC_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
        mutatedExchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);

        MDC.put(MDC_KEY, correlationId);
        try {
            LOGGER.info("Incoming gateway request method={} path={}", request.getMethod(), request.getURI().getPath());
        } finally {
            MDC.remove(MDC_KEY);
        }

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
