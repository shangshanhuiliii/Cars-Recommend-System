package com.carsrecommend.system.service.impl;

import java.math.BigDecimal;

record RecommendationScoreVector(
        BigDecimal price,
        BigDecimal space,
        BigDecimal safety,
        BigDecimal energy,
        BigDecimal intelligence,
        BigDecimal comfort,
        BigDecimal power,
        BigDecimal reputation,
        BigDecimal popularity) {
}
