package com.carsrecommend.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class RecommendationFeedbackRequest {

    private Long userId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer satisfactionScore;

    private List<String> reasonTags;

    @Size(max = 500)
    private String comment;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getSatisfactionScore() {
        return satisfactionScore;
    }

    public void setSatisfactionScore(Integer satisfactionScore) {
        this.satisfactionScore = satisfactionScore;
    }

    public List<String> getReasonTags() {
        return reasonTags;
    }

    public void setReasonTags(List<String> reasonTags) {
        this.reasonTags = reasonTags;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
