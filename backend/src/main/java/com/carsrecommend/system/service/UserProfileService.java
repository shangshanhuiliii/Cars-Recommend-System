package com.carsrecommend.system.service;

import com.carsrecommend.system.dto.UserDemandSaveRequest;
import com.carsrecommend.system.vo.UserDemandVO;

public interface UserProfileService {

    UserDemandVO saveDemand(UserDemandSaveRequest request);

    UserDemandVO getLatestDemand(Long userId);

    UserDemandVO getDemandById(Long id);

    UserDemandVO getDemandById(Long id, Long userId);
}
