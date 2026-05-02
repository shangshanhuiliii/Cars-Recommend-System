package com.carsrecommend.system.service;

import com.carsrecommend.system.vo.CarDetailVO;
import com.carsrecommend.system.vo.CarCompareVO;
import com.carsrecommend.system.vo.CarOptionVO;
import java.util.List;

public interface CarDetailService {

    CarDetailVO getUserCarDetail(Long carId);

    CarCompareVO compareCars(List<Long> carIds);

    List<String> getActiveBrands();

    List<CarOptionVO> getCarOptions(String keyword, Integer limit);
}
