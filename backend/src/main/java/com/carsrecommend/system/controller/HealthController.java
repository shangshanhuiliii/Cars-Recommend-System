package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("backend", "running");
        status.put("database", "not_configured");
        return ApiResponse.success(status);
    }
}
