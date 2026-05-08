package com.carsrecommend.system.service;

import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.dto.AdminUserQuery;
import com.carsrecommend.system.dto.AdminUserStatusUpdateRequest;
import com.carsrecommend.system.vo.AdminUserBaseVO;
import com.carsrecommend.system.vo.AdminUserDetailVO;
import com.carsrecommend.system.vo.AdminUserListItemVO;
import com.carsrecommend.system.vo.RecommendationFeedbackVO;
import com.carsrecommend.system.vo.RecommendationHistoryItemVO;
import com.carsrecommend.system.vo.UserFavoriteItemVO;

public interface AdminUserService {

    PageResult<AdminUserListItemVO> list(AdminUserQuery query);

    AdminUserDetailVO detail(Long userId);

    PageResult<RecommendationHistoryItemVO> recommendRecords(Long userId, Integer page, Integer size);

    PageResult<UserFavoriteItemVO> favorites(Long userId, Integer page, Integer size);

    PageResult<RecommendationFeedbackVO> feedbacks(Long userId, Integer page, Integer size);

    AdminUserBaseVO updateStatus(Long userId, AdminUserStatusUpdateRequest request);
}
