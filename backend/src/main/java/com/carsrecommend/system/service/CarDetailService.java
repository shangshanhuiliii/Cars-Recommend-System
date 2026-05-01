package com.carsrecommend.system.service;

import com.carsrecommend.system.vo.CarDetailVO;
import com.carsrecommend.system.vo.CarOptionVO;
import java.util.List;

public interface CarDetailService {

    CarDetailVO getUserCarDetail(Long carId);

    List<String> getActiveBrands();

    List<CarOptionVO> getCarOptions(String keyword, Integer limit);
}
