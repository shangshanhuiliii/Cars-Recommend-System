package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.service.UserFavoriteService;
import com.carsrecommend.system.vo.FavoriteStatusVO;
import com.carsrecommend.system.vo.UserFavoriteItemVO;
import jakarta.validation.constraints.Positive;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/user/favorites")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class UserFavoriteController {

    private final UserFavoriteService userFavoriteService;

    public UserFavoriteController(UserFavoriteService userFavoriteService) {
        this.userFavoriteService = userFavoriteService;
    }

    @PostMapping("/{carId}")
    public ApiResponse<Void> favorite(
            @PathVariable @Positive Long carId,
            @RequestParam(required = false) Long userId) {
        userFavoriteService.favorite(userId, carId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{carId}")
    public ApiResponse<Void> cancel(
            @PathVariable @Positive Long carId,
            @RequestParam(required = false) Long userId) {
        userFavoriteService.cancel(userId, carId);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<PageResult<UserFavoriteItemVO>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.success(userFavoriteService.list(userId, page, size));
    }

    @GetMapping("/status")
    public ApiResponse<List<FavoriteStatusVO>> status(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String carIds) {
        return ApiResponse.success(userFavoriteService.status(userId, parseCarIds(carIds)));
    }

    private List<Long> parseCarIds(String carIds) {
        if (carIds == null || carIds.isBlank()) {
            return List.of();
        }
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
