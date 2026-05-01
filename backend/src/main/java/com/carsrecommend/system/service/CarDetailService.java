package com.carsrecommend.system.service;

import com.carsrecommend.system.vo.CarDetailVO;

public interface CarDetailService {

    CarDetailVO getUserCarDetail(Long carId);
}
