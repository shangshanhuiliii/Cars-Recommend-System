package com.carsrecommend.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("recommend_record")
public class RecommendRecord extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("demand_id")
    private Long demandId;

    @TableField("profile_text")
    private String profileText;

    @TableField("weight_snapshot")
    private String weightSnapshot;

    @TableField("fallback_message")
    private String fallbackMessage;

    @TableField("recommend_status")
    private String recommendStatus;

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

    public String getProfileText() {
        return profileText;
    }

    public void setProfileText(String profileText) {
        this.profileText = profileText;
    }

    public String getWeightSnapshot() {
        return weightSnapshot;
    }

    public void setWeightSnapshot(String weightSnapshot) {
        this.weightSnapshot = weightSnapshot;
    }

    public String getFallbackMessage() {
        return fallbackMessage;
    }

    public void setFallbackMessage(String fallbackMessage) {
        this.fallbackMessage = fallbackMessage;
    }

    public String getRecommendStatus() {
        return recommendStatus;
    }

    public void setRecommendStatus(String recommendStatus) {
        this.recommendStatus = recommendStatus;
    }
}
