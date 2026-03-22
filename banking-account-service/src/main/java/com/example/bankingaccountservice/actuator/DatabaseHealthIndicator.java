package com.example.bankingaccountservice.actuator;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("database")
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            boolean valid = connection.isValid(2);
            if (!valid) {
                return Health.down()
                        .withDetail("database", metadata.getDatabaseProductName())
                        .withDetail("validation", "Connection is invalid")
                        .build();
            }
            return Health.up()
                    .withDetail("database", metadata.getDatabaseProductName())
                    .withDetail("url", metadata.getURL())
                    .withDetail("validationQuery", "Connection#isValid")
                    .build();
        } catch (Exception exception) {
            return Health.down(exception)
                    .withDetail("database", "unavailable")
                    .build();
        }
    }
}
