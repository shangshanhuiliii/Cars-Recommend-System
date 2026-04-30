package com.carsrecommend.system.service;

import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.dto.CarModelSaveRequest;
import com.carsrecommend.system.dto.CarPageQuery;
import com.carsrecommend.system.vo.CarModelVO;
import java.util.List;

public interface CarModelService {

    PageResult<CarModelVO> page(CarPageQuery query);

    CarModelVO getById(Long id);

    CarModelVO create(CarModelSaveRequest request);

    CarModelVO update(Long id, CarModelSaveRequest request);

    void softDelete(Long id);

    List<CarModelVO> listApprovedRecommendationCandidates();
}
