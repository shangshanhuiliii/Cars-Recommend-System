package com.carsrecommend.system.service;

import com.carsrecommend.system.dto.RecommendationFeedbackRequest;
import com.carsrecommend.system.vo.RecommendationFeedbackVO;

public interface RecommendationFeedbackService {

    RecommendationFeedbackVO submit(Long recordId, RecommendationFeedbackRequest request);

    RecommendationFeedbackVO get(Long recordId, Long userId);
}
