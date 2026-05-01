package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.service.CarFeatureScoreService;
import com.carsrecommend.system.vo.CarFeatureScoreBatchVO;
import com.carsrecommend.system.vo.CarFeatureScoreVO;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/cars")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AdminCarScoreController {

    private final CarFeatureScoreService carFeatureScoreService;

    public AdminCarScoreController(CarFeatureScoreService carFeatureScoreService) {
        this.carFeatureScoreService = carFeatureScoreService;
    }

    @PostMapping("/{id}/score/recalculate")
    public ApiResponse<CarFeatureScoreVO> recalculate(@PathVariable @Positive Long id) {
        return ApiResponse.success(carFeatureScoreService.recalculate(id));
    }

    @PostMapping("/scores/recalculate")
    public ApiResponse<CarFeatureScoreBatchVO> recalculateAll() {
        return ApiResponse.success(carFeatureScoreService.recalculateAll());
    }

    @GetMapping("/{id}/score")
    public ApiResponse<CarFeatureScoreVO> getScore(@PathVariable @Positive Long id) {
        return ApiResponse.success(carFeatureScoreService.getByCarId(id));
    }
}
