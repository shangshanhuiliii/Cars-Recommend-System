package com.carsrecommend.system.service;

import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.vo.FavoriteStatusVO;
import com.carsrecommend.system.vo.UserFavoriteItemVO;
import java.util.List;

public interface UserFavoriteService {

    void favorite(Long userId, Long carId);

    void cancel(Long userId, Long carId);

    PageResult<UserFavoriteItemVO> list(Long userId, Integer page, Integer size);

    List<FavoriteStatusVO> status(Long userId, List<Long> carIds);
}
