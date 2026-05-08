package com.carsrecommend.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("user_demand")
public class UserDemand extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("raw_text")
    private String rawText;

    @TableField("budget_min")
    private BigDecimal budgetMin;

    @TableField("budget_max")
    private BigDecimal budgetMax;

    @TableField("brands")
    private String brands;

    @TableField("body_types")
    private String bodyTypes;

    @TableField("energy_types")
    private String energyTypes;

    @TableField("seat_options")
    private String seatOptions;

    @TableField("min_seats")
    private Integer minSeats;

    @TableField("scenes")
    private String scenes;

    @TableField("factor_weights")
    private String factorWeights;

    @TableField("excluded_brands")
    private String excludedBrands;

    @TableField("excluded_car_ids")
    private String excludedCarIds;

    @TableField("profile_text")
    private String profileText;

    @TableField("weight_price")
    private BigDecimal weightPrice;

    @TableField("weight_space")
    private BigDecimal weightSpace;

    @TableField("weight_safety")
    private BigDecimal weightSafety;

    @TableField("weight_energy")
    private BigDecimal weightEnergy;

    @TableField("weight_intelligence")
    private BigDecimal weightIntelligence;

    @TableField("weight_comfort")
    private BigDecimal weightComfort;

    @TableField("weight_power")
    private BigDecimal weightPower;

    @TableField("weight_reputation")
    private BigDecimal weightReputation;

    @TableField("weight_popularity")
    private BigDecimal weightPopularity;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public BigDecimal getBudgetMin() {
        return budgetMin;
    }

    public void setBudgetMin(BigDecimal budgetMin) {
        this.budgetMin = budgetMin;
    }

    public BigDecimal getBudgetMax() {
        return budgetMax;
    }

    public void setBudgetMax(BigDecimal budgetMax) {
        this.budgetMax = budgetMax;
    }

    public String getBrands() {
        return brands;
    }

    public void setBrands(String brands) {
        this.brands = brands;
    }

    public String getBodyTypes() {
        return bodyTypes;
    }

    public void setBodyTypes(String bodyTypes) {
        this.bodyTypes = bodyTypes;
    }

    public String getEnergyTypes() {
        return energyTypes;
    }

    public void setEnergyTypes(String energyTypes) {
        this.energyTypes = energyTypes;
    }

    public String getSeatOptions() {
        return seatOptions;
    }

    public void setSeatOptions(String seatOptions) {
        this.seatOptions = seatOptions;
    }

    public Integer getMinSeats() {
        return minSeats;
    }

    public void setMinSeats(Integer minSeats) {
        this.minSeats = minSeats;
    }

    public String getScenes() {
        return scenes;
    }

    public void setScenes(String scenes) {
        this.scenes = scenes;
    }

    public String getFactorWeights() {
        return factorWeights;
    }

    public void setFactorWeights(String factorWeights) {
        this.factorWeights = factorWeights;
    }

    public String getExcludedBrands() {
        return excludedBrands;
    }

    public void setExcludedBrands(String excludedBrands) {
        this.excludedBrands = excludedBrands;
    }

    public String getExcludedCarIds() {
        return excludedCarIds;
    }

    public void setExcludedCarIds(String excludedCarIds) {
        this.excludedCarIds = excludedCarIds;
    }

    public String getProfileText() {
        return profileText;
    }

    public void setProfileText(String profileText) {
        this.profileText = profileText;
    }

    public BigDecimal getWeightPrice() {
        return weightPrice;
    }

    public void setWeightPrice(BigDecimal weightPrice) {
        this.weightPrice = weightPrice;
    }

    public BigDecimal getWeightSpace() {
        return weightSpace;
    }

    public void setWeightSpace(BigDecimal weightSpace) {
        this.weightSpace = weightSpace;
    }

    public BigDecimal getWeightSafety() {
        return weightSafety;
    }

    public void setWeightSafety(BigDecimal weightSafety) {
        this.weightSafety = weightSafety;
    }

    public BigDecimal getWeightEnergy() {
        return weightEnergy;
    }

    public void setWeightEnergy(BigDecimal weightEnergy) {
        this.weightEnergy = weightEnergy;
    }

    public BigDecimal getWeightIntelligence() {
        return weightIntelligence;
    }

    public void setWeightIntelligence(BigDecimal weightIntelligence) {
        this.weightIntelligence = weightIntelligence;
    }

    public BigDecimal getWeightComfort() {
        return weightComfort;
    }

    public void setWeightComfort(BigDecimal weightComfort) {
        this.weightComfort = weightComfort;
    }

    public BigDecimal getWeightPower() {
        return weightPower;
    }

    public void setWeightPower(BigDecimal weightPower) {
        this.weightPower = weightPower;
    }

    public BigDecimal getWeightReputation() {
        return weightReputation;
    }

    public void setWeightReputation(BigDecimal weightReputation) {
        this.weightReputation = weightReputation;
    }

    public BigDecimal getWeightPopularity() {
        return weightPopularity;
    }

    public void setWeightPopularity(BigDecimal weightPopularity) {
        this.weightPopularity = weightPopularity;
    }
}
