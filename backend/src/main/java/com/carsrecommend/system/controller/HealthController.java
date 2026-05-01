package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public HealthController(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("backend", "running");
        status.put("database", databaseStatus());
        return ApiResponse.success(status);
    }

    private String databaseStatus() {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return "not_configured";
        }
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "connected";
        } catch (RuntimeException exception) {
            return "disconnected";
        }
    }
}
