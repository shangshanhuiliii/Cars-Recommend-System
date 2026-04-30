package com.carsrecommend.system.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RecommendStatus {

    SUCCESS("SUCCESS"),
    FALLBACK("FALLBACK"),
    EMPTY("EMPTY");

    private final String code;

    RecommendStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static RecommendStatus fromCode(String code) {
        for (RecommendStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported recommendStatus: " + code);
    }
}
