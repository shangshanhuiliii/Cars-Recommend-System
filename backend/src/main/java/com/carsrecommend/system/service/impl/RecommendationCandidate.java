package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.common.enums.MatchLevel;
import com.carsrecommend.system.entity.CarFeatureScore;
import com.carsrecommend.system.entity.CarModel;

record RecommendationCandidate(
        CarModel car,
        CarFeatureScore featureScore,
        MatchLevel matchLevel) {
}
