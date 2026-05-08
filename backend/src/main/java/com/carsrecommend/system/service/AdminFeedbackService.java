package com.carsrecommend.system.service;

import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.vo.AdminFeedbackItemVO;

public interface AdminFeedbackService {

    PageResult<AdminFeedbackItemVO> list(
            Integer page,
            Integer size,
            String keyword,
            Long userId,
            Integer satisfactionScore);
}
