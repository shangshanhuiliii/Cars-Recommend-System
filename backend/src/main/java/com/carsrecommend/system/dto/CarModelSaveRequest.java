package com.carsrecommend.system.dto;

import com.carsrecommend.system.common.enums.AuditStatus;
import com.carsrecommend.system.common.enums.BodyType;
import com.carsrecommend.system.common.enums.EnergyType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class CarModelSaveRequest {

    @NotBlank
    @Size(max = 64)
    private String brand;

    @NotBlank
    @Size(max = 64)
    private String series;

    @NotBlank
    @Size(max = 128)
    private String modelName;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal guidePrice;

    @NotNull
    private BodyType bodyType;

    @NotNull
    private EnergyType energyType;

    @NotNull
    @Min(2)
    @Max(9)
    private Integer seats;

    @Min(1990)
    @Max(2100)
    private Integer launchYear;

    @Size(max = 512)
    private String imageUrl;

    @NotNull
    @Min(0)
    private Integer salesVolume;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("5")
    private BigDecimal userRating;

    @NotNull
    private AuditStatus auditStatus;

    @AssertTrue(message = "car model energyType must not be NEW_ENERGY")
    public boolean isCarModelEnergyTypeValid() {
        return energyType == null || energyType.isCarModelType();
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

    public BodyType getBodyType() {
        return bodyType;
    }

    public void setBodyType(BodyType bodyType) {
        this.bodyType = bodyType;
    }

    public EnergyType getEnergyType() {
        return energyType;
    }

    public void setEnergyType(EnergyType energyType) {
        this.energyType = energyType;
    }

    public Integer getSeats() {
        return seats;
    }

    public void setSeats(Integer seats) {
        this.seats = seats;
    }

    public Integer getLaunchYear() {
        return launchYear;
    }

    public void setLaunchYear(Integer launchYear) {
        this.launchYear = launchYear;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getSalesVolume() {
        return salesVolume;
    }

    public void setSalesVolume(Integer salesVolume) {
        this.salesVolume = salesVolume;
    }

    public BigDecimal getUserRating() {
        return userRating;
    }

    public void setUserRating(BigDecimal userRating) {
        this.userRating = userRating;
    }

    public AuditStatus getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(AuditStatus auditStatus) {
        this.auditStatus = auditStatus;
    }
}
