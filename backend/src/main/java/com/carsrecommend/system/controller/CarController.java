package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.service.CarDetailService;
import com.carsrecommend.system.vo.CarDetailVO;
import com.carsrecommend.system.vo.CarOptionVO;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/brands")
    public ApiResponse<List<String>> brands() {
        return ApiResponse.success(carDetailService.getActiveBrands());
    }

    @GetMapping("/options")
    public ApiResponse<List<CarOptionVO>> options(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(carDetailService.getCarOptions(keyword, limit));
    }
}
