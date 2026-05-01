package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.dto.RecommendationGenerateRequest;
import com.carsrecommend.system.service.RecommendationRecordService;
import com.carsrecommend.system.service.RecommendationService;
import com.carsrecommend.system.vo.RecommendationHistoryDetailVO;
import com.carsrecommend.system.vo.RecommendationHistoryItemVO;
import com.carsrecommend.system.vo.RecommendationResponseVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/recommend")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final RecommendationRecordService recommendationRecordService;

    public RecommendationController(
            RecommendationService recommendationService,
            RecommendationRecordService recommendationRecordService) {
        this.recommendationService = recommendationService;
        this.recommendationRecordService = recommendationRecordService;
    }

    @PostMapping("/generate")
    public ApiResponse<RecommendationResponseVO> generate(@Valid @RequestBody RecommendationGenerateRequest request) {
        return ApiResponse.success(recommendationService.generate(request));
    }

    @GetMapping("/history")
    public ApiResponse<PageResult<RecommendationHistoryItemVO>> history(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.success(recommendationRecordService.history(userId, page, size));
    }

    @GetMapping("/{recordId}")
    public ApiResponse<RecommendationHistoryDetailVO> detail(
            @PathVariable @Positive Long recordId,
            @RequestParam(required = false) Long userId) {
        return ApiResponse.success(recommendationRecordService.detail(recordId, userId));
    }
}
