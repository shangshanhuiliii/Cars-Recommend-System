package com.carsrecommend.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserDemandSaveRequest {

    @Positive
    private Long userId;

    @Size(max = 1000)
    private String rawText;

    @DecimalMin("0")
    private BigDecimal budgetMin;

    @DecimalMin("0")
    private BigDecimal budgetMax;

    @Size(max = 30)
    private List<@Size(max = 64) String> brands;

    @Size(max = 10)
    private List<@Size(max = 16) String> bodyTypes;

    @Size(max = 10)
    private List<@Size(max = 16) String> energyTypes;

    @Size(max = 10)
    private List<@Size(max = 16) String> seatOptions;

    @Min(2)
    @Max(9)
    private Integer minSeats;

    @Size(max = 10)
    private List<@Size(max = 32) String> scenes;

    private Map<@Size(max = 32) String, @Min(0) @Max(10) Integer> factorWeights = new LinkedHashMap<>();

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
}
