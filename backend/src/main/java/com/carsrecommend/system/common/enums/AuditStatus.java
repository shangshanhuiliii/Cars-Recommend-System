package com.carsrecommend.system.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AuditStatus {

    APPROVED("APPROVED"),
    PENDING("PENDING"),
    REJECTED("REJECTED");

    private final String code;

    AuditStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static AuditStatus fromCode(String code) {
        for (AuditStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("不支持的审核状态：" + code);
    }
}
