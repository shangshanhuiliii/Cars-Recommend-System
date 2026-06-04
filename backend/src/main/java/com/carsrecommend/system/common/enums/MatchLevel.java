package com.carsrecommend.system.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MatchLevel {

    STRICT("STRICT"),
    RELAX_BUDGET("RELAX_BUDGET"),
    RELAX_BODY_TYPE("RELAX_BODY_TYPE"),
    RELAX_ENERGY_TYPE("RELAX_ENERGY_TYPE"),
    SIMILAR_RECOMMEND("SIMILAR_RECOMMEND");

    private final String code;

    MatchLevel(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static MatchLevel fromCode(String code) {
        for (MatchLevel value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("不支持的匹配层级：" + code);
    }
}
