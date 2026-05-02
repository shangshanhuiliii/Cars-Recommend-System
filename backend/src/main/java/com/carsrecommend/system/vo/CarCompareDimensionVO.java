package com.carsrecommend.system.vo;

public class CarCompareDimensionVO {

    private String key;
    private String label;

    public CarCompareDimensionVO() {
    }

    public CarCompareDimensionVO(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
