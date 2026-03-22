package com.example.bankingpaymentservice.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan(basePackages = "com.example.bankingpaymentservice")
public class ObservabilityConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(value = "monitoring.kafka.enabled", havingValue = "true", matchIfMissing = true)
    public AdminClient monitoringKafkaAdminClient(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        return AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
    }
}
