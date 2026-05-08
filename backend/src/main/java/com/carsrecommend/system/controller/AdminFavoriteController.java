package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.service.AdminFavoriteService;
import com.carsrecommend.system.vo.AdminFavoriteCarVO;
import com.carsrecommend.system.vo.AdminFavoriteUserVO;
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
@RequestMapping("/api/admin/favorites")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AdminFavoriteController {

    private final AdminFavoriteService adminFavoriteService;

    public AdminFavoriteController(AdminFavoriteService adminFavoriteService) {
        this.adminFavoriteService = adminFavoriteService;
    }

    @GetMapping("/cars")
    public ApiResponse<PageResult<AdminFavoriteCarVO>> cars(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId) {
        return ApiResponse.success(adminFavoriteService.cars(page, size, keyword, userId));
    }

    @GetMapping("/cars/{carId}/users")
    public ApiResponse<PageResult<AdminFavoriteUserVO>> users(
            @PathVariable @Positive Long carId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.success(adminFavoriteService.users(carId, page, size));
    }
}
