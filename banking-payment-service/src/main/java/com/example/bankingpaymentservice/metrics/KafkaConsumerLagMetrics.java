package com.example.bankingpaymentservice.metrics;

import com.example.bankingpaymentservice.config.KafkaMonitoringProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(AdminClient.class)
@ConditionalOnProperty(value = "monitoring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConsumerLagMetrics {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerLagMetrics.class);

    private final AdminClient adminClient;
    private final KafkaMonitoringProperties kafkaMonitoringProperties;
    private final AtomicLong consumerLag = new AtomicLong(0);

    public KafkaConsumerLagMetrics(
            AdminClient adminClient,
            KafkaMonitoringProperties kafkaMonitoringProperties,
            MeterRegistry meterRegistry
    ) {
        this.adminClient = adminClient;
        this.kafkaMonitoringProperties = kafkaMonitoringProperties;
        Gauge.builder("kafka.consumer.lag", consumerLag, AtomicLong::get)
                .description("Total lag across monitored Kafka consumer groups")
                .register(meterRegistry);
    }

    @PostConstruct
    public void initialize() {
        refreshConsumerLag();
    }

    @Scheduled(fixedDelayString = "${monitoring.kafka.lag-refresh-ms:15000}")
    public void refreshConsumerLag() {
        List<String> consumerGroups = kafkaMonitoringProperties.getConsumerGroups();
        if (consumerGroups == null || consumerGroups.isEmpty()) {
            consumerLag.set(0);
            return;
        }

        long totalLag = 0;
        try {
            for (String consumerGroup : consumerGroups) {
                Map<TopicPartition, OffsetAndMetadata> committedOffsets = adminClient
                        .listConsumerGroupOffsets(consumerGroup)
                        .partitionsToOffsetAndMetadata()
                        .get(5, TimeUnit.SECONDS);

                if (committedOffsets.isEmpty()) {
                    continue;
                }

                Map<TopicPartition, OffsetSpec> latestOffsetRequest = committedOffsets.keySet().stream()
                        .collect(java.util.stream.Collectors.toMap(topicPartition -> topicPartition, topicPartition -> OffsetSpec.latest()));

                Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets = adminClient
                        .listOffsets(latestOffsetRequest)
                        .all()
                        .get(5, TimeUnit.SECONDS);

                for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : committedOffsets.entrySet()) {
                    long latestOffset = latestOffsets.get(entry.getKey()).offset();
                    totalLag += Math.max(latestOffset - entry.getValue().offset(), 0);
                }
            }
            consumerLag.set(totalLag);
        } catch (Exception exception) {
            log.warn("Unable to refresh Kafka consumer lag metric: {}", exception.getMessage());
            consumerLag.set(0);
        }
    }
}

