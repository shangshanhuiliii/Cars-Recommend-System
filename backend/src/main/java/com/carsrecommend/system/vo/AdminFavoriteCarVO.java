package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminFavoriteCarVO {

    private Long carId;
    private String brand;
    private String series;
    private String modelName;
    private BigDecimal guidePrice;
    private String bodyType;
    private String energyType;
    private String imageUrl;
    private Long favoriteCount;
    private LocalDateTime latestFavoriteTime;

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(Long favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public LocalDateTime getLatestFavoriteTime() {
        return latestFavoriteTime;
    }

    public void setLatestFavoriteTime(LocalDateTime latestFavoriteTime) {
        this.latestFavoriteTime = latestFavoriteTime;
    }
}
