package com.example.bankingpaymentservice.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component("redisHealthIndicator")
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Health health() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String ping = connection.ping();
            return Health.up()
                    .withDetail("redis", "available")
                    .withDetail("ping", ping)
                    .build();
        } catch (Exception exception) {
            return Health.down(exception)
                    .withDetail("redis", "unavailable")
                    .build();
        }
    }
}
