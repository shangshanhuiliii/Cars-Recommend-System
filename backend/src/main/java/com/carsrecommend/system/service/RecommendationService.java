package com.carsrecommend.system.service;

import com.carsrecommend.system.dto.RecommendationGenerateRequest;
import com.carsrecommend.system.vo.RecommendationResponseVO;

public interface RecommendationService {

    RecommendationResponseVO generate(RecommendationGenerateRequest request);
}
