package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class UserDemandVO {

    private Long id;
    private Long userId;
    private String rawText;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String bodyType;
    private String energyType;
    private Integer seats;
    private String scene;
    private List<String> focusFactors;
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

    public List<String> getFocusFactors() {
        return focusFactors;
    }

    public void setFocusFactors(List<String> focusFactors) {
        this.focusFactors = focusFactors;
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
