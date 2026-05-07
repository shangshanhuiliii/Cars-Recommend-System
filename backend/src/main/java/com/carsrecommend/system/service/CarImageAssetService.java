package com.carsrecommend.system.service;

import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.dto.CarImageAssetQuery;
import com.carsrecommend.system.dto.CarImageAuditRequest;
import com.carsrecommend.system.dto.CarImageUploadRequest;
import com.carsrecommend.system.vo.CarImageAssetVO;

public interface CarImageAssetService {

    CarImageAssetVO upload(CarImageUploadRequest request);

    PageResult<CarImageAssetVO> page(CarImageAssetQuery query);

    CarImageAssetVO audit(Long id, CarImageAuditRequest request);

    void softDelete(Long id);
}
