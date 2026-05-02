package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.entity.UserDemand;
import java.math.BigDecimal;
import java.util.List;

enum RecommendationDimension {
    PRICE("price") {
        @Override
        BigDecimal subjectiveWeight(UserDemand demand) {
            return demand.getWeightPrice();
        }

        @Override
        BigDecimal score(RecommendationScoreVector vector) {
            return vector.price();
        }
    },
    SPACE("space") {
        @Override
        BigDecimal subjectiveWeight(UserDemand demand) {
            return demand.getWeightSpace();
        }

        @Override
        BigDecimal score(RecommendationScoreVector vector) {
            return vector.space();
        }
    },
    SAFETY("safety") {
        @Override
        BigDecimal subjectiveWeight(UserDemand demand) {
            return demand.getWeightSafety();
        }

        @Override
        BigDecimal score(RecommendationScoreVector vector) {
            return vector.safety();
        }
    },
    ENERGY("energy") {
        @Override
        BigDecimal subjectiveWeight(UserDemand demand) {
            return demand.getWeightEnergy();
        }

        @Override
        BigDecimal score(RecommendationScoreVector vector) {
            return vector.energy();
        }
    },
    INTELLIGENCE("intelligence") {
        @Override
        BigDecimal subjectiveWeight(UserDemand demand) {
            return demand.getWeightIntelligence();
        }

        @Override
        BigDecimal score(RecommendationScoreVector vector) {
            return vector.intelligence();
        }
    },
    COMFORT("comfort") {
        @Override
        BigDecimal subjectiveWeight(UserDemand demand) {
            return demand.getWeightComfort();
        }

        @Override
        BigDecimal score(RecommendationScoreVector vector) {
            return vector.comfort();
        }
    },
    POWER("power") {
        @Override
        BigDecimal subjectiveWeight(UserDemand demand) {
            return demand.getWeightPower();
        }

        @Override
        BigDecimal score(RecommendationScoreVector vector) {
            return vector.power();
        }
    },
    REPUTATION("reputation") {
        @Override
        BigDecimal subjectiveWeight(UserDemand demand) {
            return demand.getWeightReputation();
        }

        @Override
        BigDecimal score(RecommendationScoreVector vector) {
            return vector.reputation();
        }
    },
    POPULARITY("popularity") {
        @Override
        BigDecimal subjectiveWeight(UserDemand demand) {
            return demand.getWeightPopularity();
        }

        @Override
        BigDecimal score(RecommendationScoreVector vector) {
            return vector.popularity();
        }
    };

    static final List<RecommendationDimension> ORDERED = List.of(values());

    private final String key;

    RecommendationDimension(String key) {
        this.key = key;
    }

    String key() {
        return key;
    }

    abstract BigDecimal subjectiveWeight(UserDemand demand);

    abstract BigDecimal score(RecommendationScoreVector vector);
}
