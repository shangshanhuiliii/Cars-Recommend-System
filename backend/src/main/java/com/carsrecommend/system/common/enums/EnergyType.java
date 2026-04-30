package com.carsrecommend.system.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnergyType {

    FUEL("燃油", true),
    ELECTRIC("纯电", true),
    PLUG_IN_HYBRID("插混", true),
    EXTENDED_RANGE("增程", true),
    NEW_ENERGY("新能源", false);

    private final String code;
    private final boolean carModelType;

    EnergyType(String code, boolean carModelType) {
        this.code = code;
        this.carModelType = carModelType;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public boolean isCarModelType() {
        return carModelType;
    }

    public boolean isDemandType() {
        return true;
    }

    @JsonCreator
    public static EnergyType fromCode(String code) {
        for (EnergyType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported energyType: " + code);
    }
}
