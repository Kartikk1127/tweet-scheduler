package org.kartikey.tweet_scheduler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/health/db")
    public Map<String, String> checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return Map.of(
                    "status", "UP",
                    "database", connection.getMetaData().getDatabaseProductName(),
                    "url", connection.getMetaData().getURL()
            );
        } catch (SQLException e) {
            return Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
            );
        }
    }
}