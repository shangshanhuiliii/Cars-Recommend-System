package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserDemandVO {

    private Long id;
    private Long userId;
    private String rawText;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private List<String> brands;
    private List<String> bodyTypes;
    private List<String> energyTypes;
    private List<String> seatOptions;
    private Integer minSeats;
    private List<String> scenes;
    private Map<String, Integer> factorWeights = new LinkedHashMap<>();
    private List<String> excludedBrands;
    private List<Long> excludedCarIds;
    private String profileText;
    private DemandWeightsVO weights;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public List<String> getBrands() {
        return brands;
    }

    public void setBrands(List<String> brands) {
        this.brands = brands;
    }

    public List<String> getBodyTypes() {
        return bodyTypes;
    }

    public void setBodyTypes(List<String> bodyTypes) {
        this.bodyTypes = bodyTypes;
    }

    public List<String> getEnergyTypes() {
        return energyTypes;
    }

    public void setEnergyTypes(List<String> energyTypes) {
        this.energyTypes = energyTypes;
    }

    public List<String> getSeatOptions() {
        return seatOptions;
    }

    public void setSeatOptions(List<String> seatOptions) {
        this.seatOptions = seatOptions;
    }

    public Integer getMinSeats() {
        return minSeats;
    }

    public void setMinSeats(Integer minSeats) {
        this.minSeats = minSeats;
    }

    public List<String> getScenes() {
        return scenes;
    }

    public void setScenes(List<String> scenes) {
        this.scenes = scenes;
    }

    public Map<String, Integer> getFactorWeights() {
        return factorWeights;
    }

    public void setFactorWeights(Map<String, Integer> factorWeights) {
        this.factorWeights = factorWeights;
    }

    public List<String> getExcludedBrands() {
        return excludedBrands;
    }

    public void setExcludedBrands(List<String> excludedBrands) {
        this.excludedBrands = excludedBrands;
    }

    public List<Long> getExcludedCarIds() {
        return excludedCarIds;
    }

    public void setExcludedCarIds(List<Long> excludedCarIds) {
        this.excludedCarIds = excludedCarIds;
    }

    public String getProfileText() {
        return profileText;
    }

    public void setProfileText(String profileText) {
        this.profileText = profileText;
    }

    public DemandWeightsVO getWeights() {
        return weights;
    }

    public void setWeights(DemandWeightsVO weights) {
        this.weights = weights;
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
