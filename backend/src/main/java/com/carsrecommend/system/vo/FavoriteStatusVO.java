package com.carsrecommend.system.vo;

public class FavoriteStatusVO {

    private Long carId;
    private Boolean favorited;

    public FavoriteStatusVO() {
    }

    public FavoriteStatusVO(Long carId, Boolean favorited) {
        this.carId = carId;
        this.favorited = favorited;
    }

    public Long getCarId() {
        return carId;
    }

    public void setCarId(Long carId) {
        this.carId = carId;
    }

    public Boolean getFavorited() {
        return favorited;
    }

    public void setFavorited(Boolean favorited) {
        this.favorited = favorited;
    }
}
