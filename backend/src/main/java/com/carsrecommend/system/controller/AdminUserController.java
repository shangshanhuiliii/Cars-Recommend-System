package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.dto.AdminUserQuery;
import com.carsrecommend.system.dto.AdminUserStatusUpdateRequest;
import com.carsrecommend.system.service.AdminUserService;
import com.carsrecommend.system.vo.AdminUserBaseVO;
import com.carsrecommend.system.vo.AdminUserDetailVO;
import com.carsrecommend.system.vo.AdminUserListItemVO;
import com.carsrecommend.system.vo.RecommendationFeedbackVO;
import com.carsrecommend.system.vo.RecommendationHistoryItemVO;
import com.carsrecommend.system.vo.UserFavoriteItemVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/users")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminUserListItemVO>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        AdminUserQuery query = new AdminUserQuery();
        query.setPage(page);
        query.setSize(size);
        query.setKeyword(keyword);
        query.setStatus(status);
        return ApiResponse.success(adminUserService.list(query));
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserDetailVO> detail(@PathVariable @Positive Long userId) {
        return ApiResponse.success(adminUserService.detail(userId));
    }

    @GetMapping("/{userId}/recommend-records")
    public ApiResponse<PageResult<RecommendationHistoryItemVO>> recommendRecords(
            @PathVariable @Positive Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.success(adminUserService.recommendRecords(userId, page, size));
    }

    @GetMapping("/{userId}/favorites")
    public ApiResponse<PageResult<UserFavoriteItemVO>> favorites(
            @PathVariable @Positive Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.success(adminUserService.favorites(userId, page, size));
    }

    @GetMapping("/{userId}/feedbacks")
    public ApiResponse<PageResult<RecommendationFeedbackVO>> feedbacks(
            @PathVariable @Positive Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.success(adminUserService.feedbacks(userId, page, size));
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<AdminUserBaseVO> updateStatus(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest request) {
        return ApiResponse.success(adminUserService.updateStatus(userId, request));
    }
}
