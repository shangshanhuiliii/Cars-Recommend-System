package com.carsrecommend.system.dto;

import com.carsrecommend.system.common.enums.AuditStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CarImageAuditRequest {

    @NotNull
    private AuditStatus auditStatus;

    @Size(max = 500)
    private String rejectReason;

    public AuditStatus getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(AuditStatus auditStatus) {
        this.auditStatus = auditStatus;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
