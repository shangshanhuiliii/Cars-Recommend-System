package com.carsrecommend.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("recommend_item")
public class RecommendItem extends BaseEntity {

    @TableField("record_id")
    private Long recordId;

    @TableField("car_id")
    private Long carId;

    @TableField("rank_no")
    private Integer rankNo;

    @TableField("total_score")
    private BigDecimal totalScore;

    @TableField("price_score")
    private BigDecimal priceScore;

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

    @TableField("tags")
    private String tags;

    @TableField("match_level")
    private String matchLevel;

    @TableField("reason_text")
    private String reasonText;

    @TableField("weakness_text")
    private String weaknessText;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getCarId() {
        return carId;
    }

    public void setCarId(Long carId) {
        this.carId = carId;
    }

    public Integer getRankNo() {
        return rankNo;
    }

    public void setRankNo(Integer rankNo) {
        this.rankNo = rankNo;
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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getMatchLevel() {
        return matchLevel;
    }

    public void setMatchLevel(String matchLevel) {
        this.matchLevel = matchLevel;
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
