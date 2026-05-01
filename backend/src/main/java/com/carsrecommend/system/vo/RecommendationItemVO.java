package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.util.List;

public class RecommendationItemVO {

    private Integer rankNo;
    private Long carId;
    private String brand;
    private String series;
    private String modelName;
    private BigDecimal guidePrice;
    private String bodyType;
    private String energyType;
    private Integer seats;
    private BigDecimal totalScore;
    private BigDecimal priceScore;
    private BigDecimal spaceScore;
    private BigDecimal safetyScore;
    private BigDecimal energyScore;
    private BigDecimal intelligenceScore;
    private BigDecimal comfortScore;
    private BigDecimal powerScore;
    private BigDecimal reputationScore;
    private BigDecimal popularityScore;
    private String matchLevel;
    private List<String> tags;
    private String reasonText;
    private String weaknessText;

    public Integer getRankNo() {
        return rankNo;
    }

    public void setRankNo(Integer rankNo) {
        this.rankNo = rankNo;
    }

    public Long getCarId() {
        return carId;
    }

    public void setCarId(Long carId) {
        this.carId = carId;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public BigDecimal getGuidePrice() {
        return guidePrice;
    }

    public void setGuidePrice(BigDecimal guidePrice) {
        this.guidePrice = guidePrice;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public String getEnergyType() {
        return energyType;
    }

    public void setEnergyType(String energyType) {
        this.energyType = energyType;
    }

    public Integer getSeats() {
        return seats;
    }

    public void setSeats(Integer seats) {
        this.seats = seats;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public BigDecimal getPriceScore() {
        return priceScore;
    }

    public void setPriceScore(BigDecimal priceScore) {
        this.priceScore = priceScore;
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

    public String getMatchLevel() {
        return matchLevel;
    }

    public void setMatchLevel(String matchLevel) {
        this.matchLevel = matchLevel;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getReasonText() {
        return reasonText;
    }

    public void setReasonText(String reasonText) {
        this.reasonText = reasonText;
    }

    public String getWeaknessText() {
        return weaknessText;
    }

    public void setWeaknessText(String weaknessText) {
        this.weaknessText = weaknessText;
    }
}
