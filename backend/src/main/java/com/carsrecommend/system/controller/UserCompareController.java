package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.service.UserCompareService;
import com.carsrecommend.system.vo.CarCompareVO;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/user/compare")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class UserCompareController {

    private final UserCompareService userCompareService;

    public UserCompareController(UserCompareService userCompareService) {
        this.userCompareService = userCompareService;
    }

    @GetMapping
    public ApiResponse<CarCompareVO> current() {
        return ApiResponse.success(userCompareService.current());
    }

    @PostMapping("/{carId}")
    public ApiResponse<CarCompareVO> add(@PathVariable @Positive Long carId) {
        return ApiResponse.success(userCompareService.add(carId));
    }

    @DeleteMapping("/{carId}")
    public ApiResponse<CarCompareVO> remove(@PathVariable @Positive Long carId) {
        return ApiResponse.success(userCompareService.remove(carId));
    }

    @DeleteMapping
    public ApiResponse<CarCompareVO> clear() {
        return ApiResponse.success(userCompareService.clear());
    }
}
