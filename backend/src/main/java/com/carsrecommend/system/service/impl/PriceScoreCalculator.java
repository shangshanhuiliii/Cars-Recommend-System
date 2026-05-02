package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.entity.UserDemand;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class PriceScoreCalculator {

    public BigDecimal calculate(BigDecimal price, UserDemand demand) {
        BigDecimal budgetMin = demand.getBudgetMin();
        BigDecimal budgetMax = demand.getBudgetMax();
        if (budgetMin == null && budgetMax == null) {
            return score(75);
        }
        if (budgetMax == null) {
            if (price.compareTo(budgetMin) < 0) {
                return calculateBelowBudgetMinScore(price, budgetMin);
            }
            return score(90);
        }
        if (price.compareTo(budgetMax) > 0) {
            return calculateAboveBudgetMaxScore(price, budgetMax);
        }
        if (budgetMin == null) {
            budgetMin = BigDecimal.ZERO;
        }
        if (price.compareTo(budgetMin) < 0) {
            return calculateBelowBudgetMinScore(price, budgetMin);
        }
        BigDecimal budgetMid = budgetMin.add(budgetMax).divide(new BigDecimal("2"), 8, RoundingMode.HALF_UP);
        BigDecimal budgetRange = budgetMax.subtract(budgetMin);
        BigDecimal halfRange = budgetRange.divide(new BigDecimal("2"), 8, RoundingMode.HALF_UP)
                .max(BigDecimal.ONE);
        BigDecimal distanceRatio = price.subtract(budgetMid).abs()
                .divide(halfRange, 8, RoundingMode.HALF_UP);
        BigDecimal value = new BigDecimal("100").subtract(distanceRatio.multiply(new BigDecimal("10")));
        return score(value.max(new BigDecimal("90")));
    }

    private BigDecimal calculateBelowBudgetMinScore(BigDecimal price, BigDecimal budgetMin) {
        BigDecimal denominator = budgetMin.max(BigDecimal.ONE);
        BigDecimal lowerRatio = budgetMin.subtract(price).divide(denominator, 8, RoundingMode.HALF_UP);
        BigDecimal value = new BigDecimal("90").subtract(lowerRatio.multiply(new BigDecimal("50")));
        return score(value.max(new BigDecimal("75")));
    }

    private BigDecimal calculateAboveBudgetMaxScore(BigDecimal price, BigDecimal budgetMax) {
        BigDecimal denominator = budgetMax.max(BigDecimal.ONE);
        BigDecimal overRatio = price.subtract(budgetMax).divide(denominator, 8, RoundingMode.HALF_UP);
        BigDecimal value = new BigDecimal("80").subtract(overRatio.multiply(new BigDecimal("100")));
        return score(value.max(new BigDecimal("50")));
    }

    private BigDecimal score(double value) {
        return score(BigDecimal.valueOf(value));
    }

    private BigDecimal score(BigDecimal value) {
        return value.max(BigDecimal.ZERO)
                .min(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
