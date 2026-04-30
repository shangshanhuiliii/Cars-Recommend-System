package com.carsrecommend.system.service;

import com.carsrecommend.system.dto.CarParamSaveRequest;
import com.carsrecommend.system.vo.CarParamVO;

public interface CarParamService {

    CarParamVO getByCarId(Long carId);

    CarParamVO saveByCarId(Long carId, CarParamSaveRequest request);
}
