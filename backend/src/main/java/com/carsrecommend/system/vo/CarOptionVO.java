package com.carsrecommend.system.vo;

public class CarOptionVO {

    private Long id;
    private String brand;
    private String modelName;
    private String displayName;

    public CarOptionVO() {
    }

    public CarOptionVO(Long id, String brand, String modelName, String displayName) {
        this.id = id;
        this.brand = brand;
        this.modelName = modelName;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
