package com.carsrecommend.system.dto;

import com.carsrecommend.system.common.enums.AuditStatus;
import com.carsrecommend.system.common.enums.BodyType;
import com.carsrecommend.system.common.enums.EnergyType;
import java.math.BigDecimal;

public class CarDataSourceImportItem {

    private String brand;
    private String series;
    private String modelName;
    private BigDecimal guidePrice;
    private BodyType bodyType;
    private EnergyType energyType;
    private Integer seats;
    private Integer launchYear;
    private String imageUrl;
    private Integer salesVolume;
    private BigDecimal userRating;
    private AuditStatus auditStatus;
    private CarParamSaveRequest param;

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

    public CarParamSaveRequest getParam() {
        return param;
    }

    public void setParam(CarParamSaveRequest param) {
        this.param = param;
    }
}
