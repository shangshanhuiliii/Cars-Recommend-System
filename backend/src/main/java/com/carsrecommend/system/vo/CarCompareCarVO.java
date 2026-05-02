package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.util.Map;

public class CarCompareCarVO {

    private Long carId;
    private String brand;
    private String series;
    private String modelName;
    private BigDecimal guidePrice;
    private String bodyType;
    private String energyType;
    private Integer seats;
    private Integer launchYear;
    private String imageUrl;
    private CarParamVO param;
    private Map<String, BigDecimal> scores;

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

    public CarParamVO getParam() {
        return param;
    }

    public void setParam(CarParamVO param) {
        this.param = param;
    }

    public Map<String, BigDecimal> getScores() {
        return scores;
    }

    public void setScores(Map<String, BigDecimal> scores) {
        this.scores = scores;
    }
}
