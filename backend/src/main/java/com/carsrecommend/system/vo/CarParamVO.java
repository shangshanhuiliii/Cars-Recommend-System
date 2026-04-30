package com.carsrecommend.system.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CarParamVO {

    private Long id;
    private Long carId;
    private Integer lengthMm;
    private Integer widthMm;
    private Integer heightMm;
    private Integer wheelbaseMm;
    private BigDecimal fuelConsumption;
    private BigDecimal electricConsumption;
    private Integer electricRangeKm;
    private Integer totalRangeKm;
    private BigDecimal acceleration100;
    private Integer airbagCount;
    private Boolean hasAbs;
    private Boolean hasEsp;
    private Boolean hasActiveBrake;
    private Boolean hasLaneKeep;
    private Boolean hasAdaptiveCruise;
    private Boolean hasBlindSpot;
    private Boolean hasReverseCamera;
    private Boolean has360Camera;
    private Boolean hasOta;
    private Boolean hasVoiceControl;
    private Boolean hasAutoParking;
    private BigDecimal screenSize;
    private String assistDriveLevel;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCarId() {
        return carId;
    }

    public void setCarId(Long carId) {
        this.carId = carId;
    }

    public Integer getLengthMm() {
        return lengthMm;
    }

    public void setLengthMm(Integer lengthMm) {
        this.lengthMm = lengthMm;
    }

    public Integer getWidthMm() {
        return widthMm;
    }

    public void setWidthMm(Integer widthMm) {
        this.widthMm = widthMm;
    }

    public Integer getHeightMm() {
        return heightMm;
    }

    public void setHeightMm(Integer heightMm) {
        this.heightMm = heightMm;
    }

    public Integer getWheelbaseMm() {
        return wheelbaseMm;
    }

    public void setWheelbaseMm(Integer wheelbaseMm) {
        this.wheelbaseMm = wheelbaseMm;
    }

    public BigDecimal getFuelConsumption() {
        return fuelConsumption;
    }

    public void setFuelConsumption(BigDecimal fuelConsumption) {
        this.fuelConsumption = fuelConsumption;
    }

    public BigDecimal getElectricConsumption() {
        return electricConsumption;
    }

    public void setElectricConsumption(BigDecimal electricConsumption) {
        this.electricConsumption = electricConsumption;
    }

    public Integer getElectricRangeKm() {
        return electricRangeKm;
    }

    public void setElectricRangeKm(Integer electricRangeKm) {
        this.electricRangeKm = electricRangeKm;
    }

    public Integer getTotalRangeKm() {
        return totalRangeKm;
    }

    public void setTotalRangeKm(Integer totalRangeKm) {
        this.totalRangeKm = totalRangeKm;
    }

    public BigDecimal getAcceleration100() {
        return acceleration100;
    }

    public void setAcceleration100(BigDecimal acceleration100) {
        this.acceleration100 = acceleration100;
    }

    public Integer getAirbagCount() {
        return airbagCount;
    }

    public void setAirbagCount(Integer airbagCount) {
        this.airbagCount = airbagCount;
    }

    public Boolean getHasAbs() {
        return hasAbs;
    }

    public void setHasAbs(Boolean hasAbs) {
        this.hasAbs = hasAbs;
    }

    public Boolean getHasEsp() {
        return hasEsp;
    }

    public void setHasEsp(Boolean hasEsp) {
        this.hasEsp = hasEsp;
    }

    public Boolean getHasActiveBrake() {
        return hasActiveBrake;
    }

    public void setHasActiveBrake(Boolean hasActiveBrake) {
        this.hasActiveBrake = hasActiveBrake;
    }

    public Boolean getHasLaneKeep() {
        return hasLaneKeep;
    }

    public void setHasLaneKeep(Boolean hasLaneKeep) {
        this.hasLaneKeep = hasLaneKeep;
    }

    public Boolean getHasAdaptiveCruise() {
        return hasAdaptiveCruise;
    }

    public void setHasAdaptiveCruise(Boolean hasAdaptiveCruise) {
        this.hasAdaptiveCruise = hasAdaptiveCruise;
    }

    public Boolean getHasBlindSpot() {
        return hasBlindSpot;
    }

    public void setHasBlindSpot(Boolean hasBlindSpot) {
        this.hasBlindSpot = hasBlindSpot;
    }

    public Boolean getHasReverseCamera() {
        return hasReverseCamera;
    }

    public void setHasReverseCamera(Boolean hasReverseCamera) {
        this.hasReverseCamera = hasReverseCamera;
    }

    public Boolean getHas360Camera() {
        return has360Camera;
    }

    public void setHas360Camera(Boolean has360Camera) {
        this.has360Camera = has360Camera;
    }

    public Boolean getHasOta() {
        return hasOta;
    }

    public void setHasOta(Boolean hasOta) {
        this.hasOta = hasOta;
    }

    public Boolean getHasVoiceControl() {
        return hasVoiceControl;
    }

    public void setHasVoiceControl(Boolean hasVoiceControl) {
        this.hasVoiceControl = hasVoiceControl;
    }

    public Boolean getHasAutoParking() {
        return hasAutoParking;
    }

    public void setHasAutoParking(Boolean hasAutoParking) {
        this.hasAutoParking = hasAutoParking;
    }

    public BigDecimal getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(BigDecimal screenSize) {
        this.screenSize = screenSize;
    }

    public String getAssistDriveLevel() {
        return assistDriveLevel;
    }

    public void setAssistDriveLevel(String assistDriveLevel) {
        this.assistDriveLevel = assistDriveLevel;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
