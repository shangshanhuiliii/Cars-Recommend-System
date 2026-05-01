package com.carsrecommend.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.carsrecommend.system.entity.CarFeatureScore;
import com.carsrecommend.system.entity.CarModel;
import com.carsrecommend.system.entity.CarParam;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CarFeatureScoreCalculatorTest {

    private final CarFeatureScoreCalculator calculator = new CarFeatureScoreCalculator();

    @Test
    void calculatorUsesDocumentedRulesForAllFeatureDimensions() {
        CarModel car = car(1L, "MPV", "燃油", 7, 10000, "4.5");
        CarParam param = new CarParam();
        param.setCarId(1L);
        param.setLengthMm(4850);
        param.setWheelbaseMm(2910);
        param.setFuelConsumption(new BigDecimal("5.5"));
        param.setAcceleration100(new BigDecimal("5.8"));
        param.setAirbagCount(6);
        param.setHasAbs(true);
        param.setHasEsp(true);
        param.setHasActiveBrake(true);
        param.setHasLaneKeep(true);
        param.setHasAdaptiveCruise(true);
        param.setHasBlindSpot(true);
        param.setHasReverseCamera(true);
        param.setHas360Camera(true);
        param.setHasOta(true);
        param.setHasVoiceControl(true);
        param.setHasAutoParking(true);
        param.setScreenSize(new BigDecimal("12.3"));
        param.setAssistDriveLevel("L2");

        CarFeatureScore score = calculator.calculate(car, param, 20000);

        assertEquals(new BigDecimal("100.00"), score.getSpaceScore());
        assertEquals(new BigDecimal("100.00"), score.getSafetyScore());
        assertEquals(new BigDecimal("85.00"), score.getEnergyScore());
        assertEquals(new BigDecimal("80.00"), score.getIntelligenceScore());
        assertEquals(new BigDecimal("90.00"), score.getPowerScore());
        assertEquals(new BigDecimal("90.00"), score.getReputationScore());
        assertEquals(new BigDecimal("50.00"), score.getPopularityScore());
        assertEquals(new BigDecimal("93.00"), score.getComfortScore());
    }

    @Test
    void calculatorUsesEnergyRulesByEnergyType() {
        assertEquals(new BigDecimal("95.00"), calculator.calculate(
                car(1L, "轿车", "纯电", 5, 1, "4.0"),
                electricParam(700),
                1).getEnergyScore());
        assertEquals(new BigDecimal("85.00"), calculator.calculate(
                car(2L, "SUV", "插混", 5, 1, "4.0"),
                rangeParam(900),
                1).getEnergyScore());
        assertEquals(new BigDecimal("75.00"), calculator.calculate(
                car(3L, "SUV", "增程", 5, 1, "4.0"),
                rangeParam(700),
                1).getEnergyScore());
    }

    @Test
    void calculatorUsesDefaultScoresWhenParamIsMissing() {
        CarFeatureScore score = calculator.calculate(car(1L, "SUV", "纯电", 5, 0, "4.0"), null, 0);

        assertEquals(new BigDecimal("63.00"), score.getSpaceScore());
        assertEquals(new BigDecimal("30.00"), score.getSafetyScore());
        assertEquals(new BigDecimal("60.00"), score.getEnergyScore());
        assertEquals(new BigDecimal("50.00"), score.getIntelligenceScore());
        assertEquals(new BigDecimal("60.00"), score.getPowerScore());
        assertEquals(new BigDecimal("80.00"), score.getReputationScore());
        assertEquals(new BigDecimal("0.00"), score.getPopularityScore());
        assertEquals(new BigDecimal("65.50"), score.getComfortScore());
    }

    private CarModel car(Long id, String bodyType, String energyType, int seats, int salesVolume, String userRating) {
        CarModel car = new CarModel();
        car.setId(id);
        car.setBodyType(bodyType);
        car.setEnergyType(energyType);
        car.setSeats(seats);
        car.setSalesVolume(salesVolume);
        car.setUserRating(new BigDecimal(userRating));
        return car;
    }

    private CarParam electricParam(int range) {
        CarParam param = new CarParam();
        param.setElectricRangeKm(range);
        return param;
    }

    private CarParam rangeParam(int range) {
        CarParam param = new CarParam();
        param.setTotalRangeKm(range);
        return param;
    }
}
