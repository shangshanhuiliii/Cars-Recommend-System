package com.carsrecommend.system.controller;

import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.dto.CarModelSaveRequest;
import com.carsrecommend.system.dto.CarPageQuery;
import com.carsrecommend.system.dto.CarParamSaveRequest;
import com.carsrecommend.system.service.CarDataSourceImportService;
import com.carsrecommend.system.service.CarModelService;
import com.carsrecommend.system.service.CarParamService;
import com.carsrecommend.system.vo.CarDataSourceImportResultVO;
import com.carsrecommend.system.vo.CarModelVO;
import com.carsrecommend.system.vo.CarParamVO;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/admin/cars")
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AdminCarController {

    private final CarModelService carModelService;
    private final CarParamService carParamService;
    private final CarDataSourceImportService carDataSourceImportService;

    public AdminCarController(
            CarModelService carModelService,
            CarParamService carParamService,
            CarDataSourceImportService carDataSourceImportService) {
        this.carModelService = carModelService;
        this.carParamService = carParamService;
        this.carDataSourceImportService = carDataSourceImportService;
    }

    @GetMapping
    public ApiResponse<PageResult<CarModelVO>> page(@Valid @ModelAttribute CarPageQuery query) {
        return ApiResponse.success(carModelService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<CarModelVO> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(carModelService.getById(id));
    }

    @PostMapping
    public ApiResponse<CarModelVO> create(@Valid @RequestBody CarModelSaveRequest request) {
        return ApiResponse.success(carModelService.create(request));
    }

    @PostMapping(value = "/data-source/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CarDataSourceImportResultVO> importDataSource(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(carDataSourceImportService.importJson(file));
    }

    @PutMapping("/{id}")
    public ApiResponse<CarModelVO> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CarModelSaveRequest request) {
        return ApiResponse.success(carModelService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        carModelService.softDelete(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/param")
    public ApiResponse<CarParamVO> getParam(@PathVariable @Positive Long id) {
        return ApiResponse.success(carParamService.getByCarId(id));
    }

    @PutMapping("/{id}/param")
    public ApiResponse<CarParamVO> saveParam(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CarParamSaveRequest request) {
        return ApiResponse.success(carParamService.saveByCarId(id, request));
    }
}
