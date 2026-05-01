package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CarFeatureScoreVO {

    private Long id;
    private Long carId;
    private BigDecimal spaceScore;
    private BigDecimal safetyScore;
    private BigDecimal energyScore;
    private BigDecimal intelligenceScore;
    private BigDecimal comfortScore;
    private BigDecimal powerScore;
    private BigDecimal reputationScore;
    private BigDecimal popularityScore;
    private String scoreVersion;
    private LocalDateTime calculatedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
