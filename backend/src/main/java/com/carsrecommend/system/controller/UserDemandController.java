package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.dto.DemandTextParseRequest;
import com.carsrecommend.system.dto.UserDemandSaveRequest;
import com.carsrecommend.system.service.DemandTextParseService;
import com.carsrecommend.system.service.UserProfileService;
import com.carsrecommend.system.vo.DemandTextParseVO;
import com.carsrecommend.system.vo.UserDemandVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/user/demand")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class UserDemandController {

    private final UserProfileService userProfileService;
    private final DemandTextParseService demandTextParseService;

    public UserDemandController(UserProfileService userProfileService, DemandTextParseService demandTextParseService) {
        this.userProfileService = userProfileService;
        this.demandTextParseService = demandTextParseService;
    }

    @PostMapping
    public ApiResponse<UserDemandVO> save(@Valid @RequestBody UserDemandSaveRequest request) {
        return ApiResponse.success(userProfileService.saveDemand(request));
    }

    @PostMapping("/parse-text")
    public ApiResponse<DemandTextParseVO> parseText(@Valid @RequestBody DemandTextParseRequest request) {
        return ApiResponse.success(demandTextParseService.parse(request));
    }

    @GetMapping("/latest")
    public ApiResponse<UserDemandVO> latest(@RequestParam(required = false) @Positive Long userId) {
        return ApiResponse.success(userProfileService.getLatestDemand(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDemandVO> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(userProfileService.getDemandById(id));
    }
}
