package com.carsrecommend.system.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BodyType {

    SUV("SUV"),
    SEDAN("轿车"),
    MPV("MPV"),
    SPORTS("跑车"),
    TRUCK("卡车");

    private final String code;

    BodyType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static BodyType fromCode(String code) {
        for (BodyType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported bodyType: " + code);
    }
}
