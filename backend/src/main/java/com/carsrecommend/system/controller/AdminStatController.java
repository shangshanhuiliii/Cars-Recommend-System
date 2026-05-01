package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.service.AdminStatService;
import com.carsrecommend.system.vo.AdminStatOverviewVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stat")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AdminStatController {

    private final AdminStatService adminStatService;

    public AdminStatController(AdminStatService adminStatService) {
        this.adminStatService = adminStatService;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminStatOverviewVO> overview() {
        return ApiResponse.success(adminStatService.overview());
    }
}
