package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.service.CarDetailService;
import com.carsrecommend.system.vo.CarCompareVO;
import com.carsrecommend.system.vo.CarDetailVO;
import com.carsrecommend.system.vo.CarOptionVO;
import com.carsrecommend.system.vo.HomeCarouselCarVO;
import jakarta.validation.constraints.Positive;
import java.util.Arrays;
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

    @GetMapping("/compare")
    public ApiResponse<CarCompareVO> compare(@RequestParam String carIds) {
        return ApiResponse.success(carDetailService.compareCars(parseCarIds(carIds)));
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

    @GetMapping("/home-carousel")
    public ApiResponse<List<HomeCarouselCarVO>> homeCarousel(@RequestParam(required = false) Integer limit) {
        return ApiResponse.success(carDetailService.getHomeCarouselCars(limit));
    }

    private List<Long> parseCarIds(String carIds) {
        try {
            return Arrays.stream(carIds.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(Long::valueOf)
                    .toList();
        } catch (NumberFormatException exception) {
            throw new BusinessException("carIds must be comma separated numbers");
        }
    }
}
