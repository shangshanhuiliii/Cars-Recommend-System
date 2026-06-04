package com.carsrecommend.system.controller;

import com.carsrecommend.system.auth.AuthContext;
import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.dto.LoginRequest;
import com.carsrecommend.system.dto.UserProfileUpdateRequest;
import com.carsrecommend.system.dto.UserRegisterRequest;
import com.carsrecommend.system.service.AuthService;
import com.carsrecommend.system.vo.AuthPrincipalVO;
import com.carsrecommend.system.vo.AuthTokenVO;
import com.carsrecommend.system.vo.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/user/login")
    public ApiResponse<AuthTokenVO> userLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.loginUser(request));
    }

    @PostMapping("/user/register")
    public ApiResponse<AuthTokenVO> userRegister(@Valid @RequestBody UserRegisterRequest request) {
        return ApiResponse.success(authService.registerUser(request));
    }

    @PostMapping("/admin/login")
    public ApiResponse<AuthTokenVO> adminLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.loginAdmin(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthPrincipalVO> me() {
        return ApiResponse.success(authService.current(AuthContext.current()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录后再继续操作"))));
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileVO> profile() {
        return ApiResponse.success(authService.currentUserProfile(AuthContext.current()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "please login first"))));
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfileVO> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.success(authService.updateCurrentUserProfile(AuthContext.current()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "please login first")), request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }
}
