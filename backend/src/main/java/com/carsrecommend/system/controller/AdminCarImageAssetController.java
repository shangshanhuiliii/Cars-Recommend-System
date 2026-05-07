package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.dto.CarImageAssetQuery;
import com.carsrecommend.system.dto.CarImageAuditRequest;
import com.carsrecommend.system.dto.CarImageUploadRequest;
import com.carsrecommend.system.service.CarImageAssetService;
import com.carsrecommend.system.vo.CarImageAssetVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/car-images")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AdminCarImageAssetController {

    private final CarImageAssetService carImageAssetService;

    public AdminCarImageAssetController(CarImageAssetService carImageAssetService) {
        this.carImageAssetService = carImageAssetService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CarImageAssetVO> upload(@Valid @ModelAttribute CarImageUploadRequest request) {
        return ApiResponse.success(carImageAssetService.upload(request));
    }

    @GetMapping
    public ApiResponse<PageResult<CarImageAssetVO>> page(@Valid @ModelAttribute CarImageAssetQuery query) {
        return ApiResponse.success(carImageAssetService.page(query));
    }

    @PutMapping("/{id}/audit")
    public ApiResponse<CarImageAssetVO> audit(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CarImageAuditRequest request) {
        return ApiResponse.success(carImageAssetService.audit(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        carImageAssetService.softDelete(id);
        return ApiResponse.success();
    }
}
