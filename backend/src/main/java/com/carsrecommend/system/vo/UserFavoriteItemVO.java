package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class UserFavoriteItemVO {

    private Long favoriteId;
    private Long carId;
    private String brand;
    private String series;
    private String modelName;
    private BigDecimal guidePrice;
    private String bodyType;
    private String energyType;
    private Integer seats;
    private String imageUrl;
    private Map<String, BigDecimal> scoreSummary;
    private LocalDateTime favoriteTime;

    public Long getFavoriteId() {
        return favoriteId;
    }

    public void setFavoriteId(Long favoriteId) {
        this.favoriteId = favoriteId;
    }

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

    public Integer getSeats() {
        return seats;
    }

    public void setSeats(Integer seats) {
        this.seats = seats;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Map<String, BigDecimal> getScoreSummary() {
        return scoreSummary;
    }

    public void setScoreSummary(Map<String, BigDecimal> scoreSummary) {
        this.scoreSummary = scoreSummary;
    }

    public LocalDateTime getFavoriteTime() {
        return favoriteTime;
    }

    public void setFavoriteTime(LocalDateTime favoriteTime) {
        this.favoriteTime = favoriteTime;
    }
}
