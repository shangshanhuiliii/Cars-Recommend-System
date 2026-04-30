package com.carsrecommend.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("car_feature_score")
public class CarFeatureScore extends BaseEntity {

    @TableField("car_id")
    private Long carId;

    @TableField("space_score")
    private BigDecimal spaceScore;

    @TableField("safety_score")
    private BigDecimal safetyScore;

    @TableField("energy_score")
    private BigDecimal energyScore;

    @TableField("intelligence_score")
    private BigDecimal intelligenceScore;

    @TableField("comfort_score")
    private BigDecimal comfortScore;

    @TableField("power_score")
    private BigDecimal powerScore;

    @TableField("reputation_score")
    private BigDecimal reputationScore;

    @TableField("popularity_score")
    private BigDecimal popularityScore;

    @TableField("score_version")
    private String scoreVersion;

    @TableField("calculated_time")
    private LocalDateTime calculatedTime;

    public Long getCarId() {
        return carId;
    }

    public void setCarId(Long carId) {
        this.carId = carId;
    }

    public BigDecimal getSpaceScore() {
        return spaceScore;
    }

    public void setSpaceScore(BigDecimal spaceScore) {
        this.spaceScore = spaceScore;
    }

    public BigDecimal getSafetyScore() {
        return safetyScore;
    }

    public void setSafetyScore(BigDecimal safetyScore) {
        this.safetyScore = safetyScore;
    }

    public BigDecimal getEnergyScore() {
        return energyScore;
    }

    public void setEnergyScore(BigDecimal energyScore) {
        this.energyScore = energyScore;
    }

    public BigDecimal getIntelligenceScore() {
        return intelligenceScore;
    }

    public void setIntelligenceScore(BigDecimal intelligenceScore) {
        this.intelligenceScore = intelligenceScore;
    }

    public BigDecimal getComfortScore() {
        return comfortScore;
    }

    public void setComfortScore(BigDecimal comfortScore) {
        this.comfortScore = comfortScore;
    }

    public BigDecimal getPowerScore() {
        return powerScore;
    }

    public void setPowerScore(BigDecimal powerScore) {
        this.powerScore = powerScore;
    }

    public BigDecimal getReputationScore() {
        return reputationScore;
    }

    public void setReputationScore(BigDecimal reputationScore) {
        this.reputationScore = reputationScore;
    }

    public BigDecimal getPopularityScore() {
        return popularityScore;
    }

    public void setPopularityScore(BigDecimal popularityScore) {
        this.popularityScore = popularityScore;
    }

    public String getScoreVersion() {
        return scoreVersion;
    }

    public void setScoreVersion(String scoreVersion) {
        this.scoreVersion = scoreVersion;
    }

    public LocalDateTime getCalculatedTime() {
        return calculatedTime;
    }

    public void setCalculatedTime(LocalDateTime calculatedTime) {
        this.calculatedTime = calculatedTime;
    }
}
