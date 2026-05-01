package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.entity.CarFeatureScore;
import com.carsrecommend.system.entity.CarModel;
import com.carsrecommend.system.entity.CarParam;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class CarFeatureScoreCalculator {

    public static final String SCORE_VERSION = "feature-score-v1";

    public CarFeatureScore calculate(CarModel carModel, CarParam param, int maxSalesVolume) {
        BigDecimal spaceScore = score(calculateSpaceScore(carModel, param));
        BigDecimal safetyScore = score(calculateSafetyScore(param));
        BigDecimal energyScore = score(calculateEnergyScore(carModel, param));
        BigDecimal intelligenceScore = score(calculateIntelligenceScore(param));
        BigDecimal powerScore = score(calculatePowerScore(param));
        BigDecimal reputationScore = score(calculateReputationScore(carModel));
        BigDecimal popularityScore = score(calculatePopularityScore(carModel, maxSalesVolume));
        BigDecimal comfortScore = score(
                spaceScore.doubleValue() * 0.5
                        + intelligenceScore.doubleValue() * 0.2
                        + reputationScore.doubleValue() * 0.3);

        CarFeatureScore score = new CarFeatureScore();
        score.setCarId(carModel.getId());
        score.setSpaceScore(spaceScore);
        score.setSafetyScore(safetyScore);
        score.setEnergyScore(energyScore);
        score.setIntelligenceScore(intelligenceScore);
        score.setComfortScore(comfortScore);
        score.setPowerScore(powerScore);
        score.setReputationScore(reputationScore);
        score.setPopularityScore(popularityScore);
        score.setScoreVersion(SCORE_VERSION);
        score.setCalculatedTime(LocalDateTime.now());
        return score;
    }

    double calculateSpaceScore(CarModel carModel, CarParam param) {
        double score = 60;
        Integer wheelbase = param == null ? null : param.getWheelbaseMm();
        if (wheelbase != null) {
            if (wheelbase < 2600) {
                score = 50;
            } else if (wheelbase < 2700) {
                score = 65;
            } else if (wheelbase < 2800) {
                score = 80;
            } else if (wheelbase < 2900) {
                score = 90;
            } else {
                score = 95;
            }
        }
        if ("SUV".equals(carModel.getBodyType())) {
            score += 3;
        } else if ("MPV".equals(carModel.getBodyType())) {
            score += 8;
        }
        if (carModel.getSeats() != null && carModel.getSeats() >= 7) {
            score += 5;
        }
        Integer length = param == null ? null : param.getLengthMm();
        if (length != null && length > 4800) {
            score += 5;
        }
        return clamp(score);
    }

    double calculateSafetyScore(CarParam param) {
        double score = 30;
        if (param == null) {
            return score;
        }
        if (Boolean.TRUE.equals(param.getHasAbs())) {
            score += 10;
        }
        if (Boolean.TRUE.equals(param.getHasEsp())) {
            score += 15;
        }
        if (param.getAirbagCount() != null && param.getAirbagCount() >= 6) {
            score += 20;
        }
        if (Boolean.TRUE.equals(param.getHasActiveBrake())) {
            score += 15;
        }
        if (Boolean.TRUE.equals(param.getHasLaneKeep())) {
            score += 10;
        }
        if (Boolean.TRUE.equals(param.getHasAdaptiveCruise())) {
            score += 10;
        }
        if (Boolean.TRUE.equals(param.getHasBlindSpot())) {
            score += 5;
        }
        return clamp(score);
    }

    double calculateEnergyScore(CarModel carModel, CarParam param) {
        if (param == null) {
            return 60;
        }
        String energyType = carModel.getEnergyType();
        if ("燃油".equals(energyType)) {
            BigDecimal consumption = param.getFuelConsumption();
            if (consumption == null) {
                return 60;
            }
            double value = consumption.doubleValue();
            if (value <= 5) {
                return 95;
            } else if (value <= 6) {
                return 85;
            } else if (value <= 7) {
                return 75;
            } else if (value <= 8) {
                return 65;
            } else if (value <= 10) {
                return 55;
            }
            return 45;
        }
        if ("纯电".equals(energyType)) {
            Integer range = param.getElectricRangeKm();
            if (range == null) {
                return 60;
            }
            if (range >= 700) {
                return 95;
            } else if (range >= 600) {
                return 90;
            } else if (range >= 500) {
                return 80;
            } else if (range >= 400) {
                return 70;
            } else if (range >= 300) {
                return 60;
            }
            return 50;
        }
        if ("插混".equals(energyType) || "增程".equals(energyType)) {
            Integer range = param.getTotalRangeKm();
            if (range == null) {
                return 60;
            }
            if (range >= 1000) {
                return 95;
            } else if (range >= 800) {
                return 85;
            } else if (range >= 600) {
                return 75;
            }
            return 65;
        }
        return 60;
    }

    double calculateIntelligenceScore(CarParam param) {
        if (param == null) {
            return 50;
        }
        double score = 0;
        if (Boolean.TRUE.equals(param.getHasVoiceControl())) {
            score += 10;
        }
        if (Boolean.TRUE.equals(param.getHasOta())) {
            score += 10;
        }
        if (param.getScreenSize() != null && param.getScreenSize().doubleValue() >= 12) {
            score += 10;
        }
        if (Boolean.TRUE.equals(param.getHasReverseCamera())) {
            score += 8;
        }
        if (Boolean.TRUE.equals(param.getHas360Camera())) {
            score += 12;
        }
        if ("L2".equalsIgnoreCase(param.getAssistDriveLevel())) {
            score += 20;
        }
        if (Boolean.TRUE.equals(param.getHasAutoParking())) {
            score += 10;
        }
        return clamp(score);
    }

    double calculatePowerScore(CarParam param) {
        if (param == null || param.getAcceleration100() == null) {
            return 60;
        }
        double acceleration = param.getAcceleration100().doubleValue();
        if (acceleration <= 4) {
            return 100;
        } else if (acceleration <= 6) {
            return 90;
        } else if (acceleration <= 8) {
            return 80;
        } else if (acceleration <= 10) {
            return 70;
        } else if (acceleration <= 12) {
            return 60;
        }
        return 50;
    }

    double calculateReputationScore(CarModel carModel) {
        if (carModel.getUserRating() == null) {
            return 60;
        }
        return clamp(carModel.getUserRating().doubleValue() / 5.0 * 100);
    }

    double calculatePopularityScore(CarModel carModel, int maxSalesVolume) {
        if (maxSalesVolume <= 0 || carModel.getSalesVolume() == null) {
            return 0;
        }
        return clamp(carModel.getSalesVolume() * 100.0 / maxSalesVolume);
    }

    private BigDecimal score(double value) {
        return BigDecimal.valueOf(clamp(value)).setScale(2, RoundingMode.HALF_UP);
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }
}
