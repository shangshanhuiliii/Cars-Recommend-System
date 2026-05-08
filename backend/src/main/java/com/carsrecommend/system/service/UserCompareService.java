package com.carsrecommend.system.service;

import com.carsrecommend.system.vo.CarCompareVO;

public interface UserCompareService {

    CarCompareVO current();

    CarCompareVO add(Long carId);

    CarCompareVO remove(Long carId);

    CarCompareVO clear();
}
