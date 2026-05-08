package com.carsrecommend.system.service;

import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.vo.AdminFavoriteCarVO;
import com.carsrecommend.system.vo.AdminFavoriteUserVO;

public interface AdminFavoriteService {

    PageResult<AdminFavoriteCarVO> cars(Integer page, Integer size, String keyword, Long userId);

    PageResult<AdminFavoriteUserVO> users(Long carId, Integer page, Integer size);
}
