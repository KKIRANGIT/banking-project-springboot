package com.example.bankingpaymentservice.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "monitoring.kafka")
public class KafkaMonitoringProperties {

    private boolean enabled = true;
    private List<String> consumerGroups = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getConsumerGroups() {
        return consumerGroups;
    }

    public void setConsumerGroups(List<String> consumerGroups) {
        this.consumerGroups = consumerGroups;
    }
}
