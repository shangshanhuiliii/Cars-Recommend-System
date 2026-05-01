package com.carsrecommend.system.vo;

import java.time.LocalDateTime;
import java.util.List;

public class RecommendationHistoryItemVO {

    private Long recordId;
    private LocalDateTime createTime;
    private String profileText;
    private String recommendStatus;
    private String fallbackMessage;
    private List<String> topCarNames;
    private long itemCount;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getProfileText() {
        return profileText;
    }

    public void setProfileText(String profileText) {
        this.profileText = profileText;
    }

    public String getRecommendStatus() {
        return recommendStatus;
    }

    public void setRecommendStatus(String recommendStatus) {
        this.recommendStatus = recommendStatus;
    }

    public String getFallbackMessage() {
        return fallbackMessage;
    }

    public void setFallbackMessage(String fallbackMessage) {
        this.fallbackMessage = fallbackMessage;
    }

    public List<String> getTopCarNames() {
        return topCarNames;
    }

    public void setTopCarNames(List<String> topCarNames) {
        this.topCarNames = topCarNames;
    }

    public long getItemCount() {
        return itemCount;
    }

    public void setItemCount(long itemCount) {
        this.itemCount = itemCount;
    }
}
