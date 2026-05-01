package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.service.CarDetailService;
import com.carsrecommend.system.vo.CarDetailVO;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/car")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class CarController {

    private final CarDetailService carDetailService;

    public CarController(CarDetailService carDetailService) {
        this.carDetailService = carDetailService;
    }

    @GetMapping("/{id}")
    public ApiResponse<CarDetailVO> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(carDetailService.getUserCarDetail(id));
    }
}
