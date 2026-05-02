package com.carsrecommend.system.vo;

import java.util.List;

public class CarCompareVO {

    private List<Long> carIds;
    private List<CarCompareDimensionVO> dimensions;
    private List<CarCompareCarVO> cars;

    public List<Long> getCarIds() {
        return carIds;
    }

    public void setCarIds(List<Long> carIds) {
        this.carIds = carIds;
    }

    public List<CarCompareDimensionVO> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<CarCompareDimensionVO> dimensions) {
        this.dimensions = dimensions;
    }

    public List<CarCompareCarVO> getCars() {
        return cars;
    }

    public void setCars(List<CarCompareCarVO> cars) {
        this.cars = cars;
    }
}
