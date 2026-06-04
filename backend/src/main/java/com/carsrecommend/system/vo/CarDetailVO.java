package com.carsrecommend.system.vo;

public class CarDetailVO {

    private CarModelVO carModel;
    private CarParamVO carParam;
    private CarFeatureScoreVO carFeatureScore;

    public CarModelVO getCarModel() {
        return carModel;
    }

    public void setCarModel(CarModelVO carModel) {
        this.carModel = carModel;
    }

    public CarParamVO getCarParam() {
        return carParam;
    }

    public void setCarParam(CarParamVO carParam) {
        this.carParam = carParam;
    }

    public CarFeatureScoreVO getCarFeatureScore() {
        return carFeatureScore;
    }

    public void setCarFeatureScore(CarFeatureScoreVO carFeatureScore) {
        this.carFeatureScore = carFeatureScore;
    }

}
