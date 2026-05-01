package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.dto.RecommendationGenerateRequest;
import com.carsrecommend.system.service.RecommendationService;
import com.carsrecommend.system.vo.RecommendationResponseVO;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommend")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/generate")
    public ApiResponse<RecommendationResponseVO> generate(@Valid @RequestBody RecommendationGenerateRequest request) {
        return ApiResponse.success(recommendationService.generate(request));
    }
}
