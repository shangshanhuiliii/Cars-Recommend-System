package com.carsrecommend.system.service;

import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.dto.CarPageQuery;
import com.carsrecommend.system.vo.CarDetailVO;
import com.carsrecommend.system.vo.CarCompareVO;
import com.carsrecommend.system.vo.CarListItemVO;
import com.carsrecommend.system.vo.CarOptionVO;
import com.carsrecommend.system.vo.HomeCarouselCarVO;
import java.util.List;

public interface CarDetailService {

    PageResult<CarListItemVO> pageUserCars(CarPageQuery query);

    CarDetailVO getUserCarDetail(Long carId);

    CarCompareVO compareCars(List<Long> carIds);

    List<String> getActiveBrands();

    List<CarOptionVO> getCarOptions(String keyword, Integer limit);

    List<HomeCarouselCarVO> getHomeCarouselCars(Integer limit);
}
