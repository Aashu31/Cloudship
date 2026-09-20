package com.cloudship.service;

import com.cloudship.dto.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);
    private final DataSource dataSource;

    public HealthService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public HealthResponse checkHealth() {
        String dbStatus = "UNKNOWN";
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(2)) {
                dbStatus = "CONNECTED";
            }
        } catch (SQLException e) {
            log.warn("Database health probe check failed: {}", e.getMessage());
            dbStatus = "DISCONNECTED";
        }

        return new HealthResponse("UP", "cloudship", dbStatus);
    }
}
