package com.carsrecommend.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RecommendationGenerateRequest {

    @Positive
    private Long userId;

    @NotNull
    @Positive
    private Long demandId;

    @Min(1)
    @Max(50)
    private Integer topK = 10;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDemandId() {
        return demandId;
    }

    public void setDemandId(Long demandId) {
        this.demandId = demandId;
    }

    public Integer getTopK() {
        return topK == null ? 10 : topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }
}
