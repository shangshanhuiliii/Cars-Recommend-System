package com.carsrecommend.system.service;

import com.carsrecommend.system.vo.CarFeatureScoreBatchVO;
import com.carsrecommend.system.vo.CarFeatureScoreVO;

public interface CarFeatureScoreService {

    CarFeatureScoreVO recalculate(Long carId);

    CarFeatureScoreBatchVO recalculateAll();

    CarFeatureScoreVO getByCarId(Long carId);
}
