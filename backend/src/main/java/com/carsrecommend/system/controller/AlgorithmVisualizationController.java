package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.service.AlgorithmVisualizationService;
import com.carsrecommend.system.vo.AlgorithmVisualizationVO;
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
@RequestMapping("/api/recommend")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AlgorithmVisualizationController {

    private final AlgorithmVisualizationService algorithmVisualizationService;

    public AlgorithmVisualizationController(AlgorithmVisualizationService algorithmVisualizationService) {
        this.algorithmVisualizationService = algorithmVisualizationService;
    }

    @GetMapping("/{recordId}/algorithm-visualization")
    public ApiResponse<AlgorithmVisualizationVO> detail(@PathVariable @Positive Long recordId) {
        return ApiResponse.success(algorithmVisualizationService.getVisualizationForAdmin(recordId));
    }
}
