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

    @TableField("body_type")
    private String bodyType;

    @TableField("energy_type")
    private String energyType;

    @TableField("seats")
    private Integer seats;

    @TableField("scene")
    private String scene;

    @TableField("focus_factors")
    private String focusFactors;

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

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getFocusFactors() {
        return focusFactors;
    }

    public void setFocusFactors(String focusFactors) {
        this.focusFactors = focusFactors;
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
