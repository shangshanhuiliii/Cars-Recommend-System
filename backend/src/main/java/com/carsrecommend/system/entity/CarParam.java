package com.carsrecommend.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("car_param")
public class CarParam extends BaseEntity {

    @TableField("car_id")
    private Long carId;

    @TableField("length_mm")
    private Integer lengthMm;

    @TableField("width_mm")
    private Integer widthMm;

    @TableField("height_mm")
    private Integer heightMm;

    @TableField("wheelbase_mm")
    private Integer wheelbaseMm;

    @TableField("fuel_consumption")
    private BigDecimal fuelConsumption;

    @TableField("electric_consumption")
    private BigDecimal electricConsumption;

    @TableField("electric_range_km")
    private Integer electricRangeKm;

    @TableField("total_range_km")
    private Integer totalRangeKm;

    @TableField("acceleration_100")
    private BigDecimal acceleration100;

    @TableField("airbag_count")
    private Integer airbagCount;

    @TableField("has_abs")
    private Boolean hasAbs;

    @TableField("has_esp")
    private Boolean hasEsp;

    @TableField("has_active_brake")
    private Boolean hasActiveBrake;

    @TableField("has_lane_keep")
    private Boolean hasLaneKeep;

    @TableField("has_adaptive_cruise")
    private Boolean hasAdaptiveCruise;

    @TableField("has_blind_spot")
    private Boolean hasBlindSpot;

    @TableField("has_reverse_camera")
    private Boolean hasReverseCamera;

    @TableField("has_360_camera")
    private Boolean has360Camera;

    @TableField("has_ota")
    private Boolean hasOta;

    @TableField("has_voice_control")
    private Boolean hasVoiceControl;

    @TableField("has_auto_parking")
    private Boolean hasAutoParking;

    @TableField("screen_size")
    private BigDecimal screenSize;

    @TableField("assist_drive_level")
    private String assistDriveLevel;

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
}
