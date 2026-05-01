package com.carsrecommend.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public class UserDemandSaveRequest {

    @Positive
    private Long userId;

    @Size(max = 1000)
    private String rawText;

    @DecimalMin("0")
    private BigDecimal budgetMin;

    @DecimalMin("0")
    private BigDecimal budgetMax;

    private String bodyType;

    private String energyType;

    @Min(2)
    @Max(9)
    private Integer seats;

    private String scene;

    @Size(max = 20)
    private List<@Size(max = 32) String> focusFactors;

    @Size(max = 20)
    private List<@Size(max = 64) String> excludedBrands;

    @Size(max = 50)
    private List<@Positive Long> excludedCarIds;

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
}
