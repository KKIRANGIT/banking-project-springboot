package com.example.bankingpaymentservice.actuator;

import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("kafka")
@ConditionalOnBean(AdminClient.class)
@ConditionalOnProperty(value = "monitoring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaHealthIndicator implements HealthIndicator {

    private final AdminClient adminClient;

    public KafkaHealthIndicator(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    @Override
    public Health health() {
        try {
            String clusterId = adminClient.describeCluster().clusterId().get(5, TimeUnit.SECONDS);
            int brokerCount = adminClient.describeCluster().nodes().get(5, TimeUnit.SECONDS).size();
            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("brokerCount", brokerCount)
                    .build();
        } catch (Exception exception) {
            return Health.down(exception)
                    .withDetail("kafka", "unavailable")
                    .build();
        }
    }
}
