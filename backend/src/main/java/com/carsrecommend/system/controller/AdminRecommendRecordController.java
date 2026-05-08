package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.service.AlgorithmVisualizationService;
import com.carsrecommend.system.service.RecommendationRecordService;
import com.carsrecommend.system.vo.AlgorithmVisualizationVO;
import com.carsrecommend.system.vo.RecommendationHistoryDetailVO;
import com.carsrecommend.system.vo.RecommendationHistoryItemVO;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/recommend-records")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AdminRecommendRecordController {

    private final RecommendationRecordService recommendationRecordService;
    private final AlgorithmVisualizationService algorithmVisualizationService;

    public AdminRecommendRecordController(
            RecommendationRecordService recommendationRecordService,
            AlgorithmVisualizationService algorithmVisualizationService) {
        this.recommendationRecordService = recommendationRecordService;
        this.algorithmVisualizationService = algorithmVisualizationService;
    }

    @GetMapping
    public ApiResponse<PageResult<RecommendationHistoryItemVO>> history(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.success(recommendationRecordService.adminHistory(userId, page, size));
    }

    @GetMapping("/{recordId}")
    public ApiResponse<RecommendationHistoryDetailVO> detail(@PathVariable @Positive Long recordId) {
        return ApiResponse.success(recommendationRecordService.adminDetail(recordId));
    }

    @GetMapping("/{recordId}/algorithm-visualization")
    public ApiResponse<AlgorithmVisualizationVO> algorithmVisualization(@PathVariable @Positive Long recordId) {
        return ApiResponse.success(algorithmVisualizationService.getVisualizationForAdmin(recordId));
    }
}
