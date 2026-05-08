package com.carsrecommend.system.service;

import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.vo.RecommendationHistoryDetailVO;
import com.carsrecommend.system.vo.RecommendationHistoryItemVO;

public interface RecommendationRecordService {

    PageResult<RecommendationHistoryItemVO> history(Long userId, Integer page, Integer size);

    RecommendationHistoryDetailVO detail(Long recordId, Long userId);

    PageResult<RecommendationHistoryItemVO> adminHistory(Long userId, Integer page, Integer size);

    RecommendationHistoryDetailVO adminDetail(Long recordId);
}
