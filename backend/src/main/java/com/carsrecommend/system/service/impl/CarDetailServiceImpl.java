package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.entity.CarFeatureScore;
import com.carsrecommend.system.entity.CarModel;
import com.carsrecommend.system.entity.CarParam;
import com.carsrecommend.system.mapper.CarFeatureScoreMapper;
import com.carsrecommend.system.mapper.CarModelMapper;
import com.carsrecommend.system.mapper.CarParamMapper;
import com.carsrecommend.system.service.CarDetailService;
import com.carsrecommend.system.vo.CarCompareCarVO;
import com.carsrecommend.system.vo.CarCompareDimensionVO;
import com.carsrecommend.system.vo.CarCompareVO;
import com.carsrecommend.system.vo.CarDetailVO;
import com.carsrecommend.system.vo.CarFeatureScoreVO;
import com.carsrecommend.system.vo.CarModelVO;
import com.carsrecommend.system.vo.CarOptionVO;
import com.carsrecommend.system.vo.CarParamVO;
import com.carsrecommend.system.vo.HomeCarouselCarVO;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class CarDetailServiceImpl implements CarDetailService {

    private static final int DEFAULT_OPTION_LIMIT = 20;
    private static final int MAX_OPTION_LIMIT = 100;
    private static final int DEFAULT_HOME_CAROUSEL_LIMIT = 6;
    private static final int MIN_HOME_CAROUSEL_LIMIT = 3;
    private static final int MAX_HOME_CAROUSEL_LIMIT = 12;
    private static final int MIN_COMPARE_COUNT = 1;
    private static final int MAX_COMPARE_COUNT = 3;
    private static final List<CarCompareDimensionVO> COMPARE_DIMENSIONS = List.of(
            new CarCompareDimensionVO("space", "空间"),
            new CarCompareDimensionVO("safety", "安全"),
            new CarCompareDimensionVO("energy", "能耗"),
            new CarCompareDimensionVO("intelligence", "智能"),
            new CarCompareDimensionVO("comfort", "舒适"),
            new CarCompareDimensionVO("power", "动力"),
            new CarCompareDimensionVO("reputation", "口碑"),
            new CarCompareDimensionVO("popularity", "热度"));

    private final CarModelMapper carModelMapper;
    private final CarParamMapper carParamMapper;
    private final CarFeatureScoreMapper carFeatureScoreMapper;

    public CarDetailServiceImpl(
            CarModelMapper carModelMapper,
            CarParamMapper carParamMapper,
            CarFeatureScoreMapper carFeatureScoreMapper) {
        this.carModelMapper = carModelMapper;
        this.carParamMapper = carParamMapper;
        this.carFeatureScoreMapper = carFeatureScoreMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public CarDetailVO getUserCarDetail(Long carId) {
        CarModel carModel = carModelMapper.findById(carId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "car model not found"));
        CarDetailVO vo = new CarDetailVO();
        vo.setCarModel(toCarModelVO(carModel));
        vo.setCarParam(carParamMapper.findByCarId(carId).map(this::toCarParamVO).orElse(null));
        vo.setCarFeatureScore(carFeatureScoreMapper.findByCarId(carId).map(this::toCarFeatureScoreVO).orElse(null));
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public CarCompareVO compareCars(List<Long> carIds) {
        List<Long> normalizedIds = normalizeCompareIds(carIds);
        CarCompareVO vo = new CarCompareVO();
        vo.setCarIds(normalizedIds);
        vo.setDimensions(COMPARE_DIMENSIONS);
        vo.setCars(normalizedIds.stream().map(this::toCompareCarVO).toList());
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getActiveBrands() {
        return carModelMapper.findActiveBrands();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarOptionVO> getCarOptions(String keyword, Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        return carModelMapper.findActiveOptions(keyword, normalizedLimit).stream()
                .map(car -> new CarOptionVO(
                        car.getId(),
                        car.getBrand(),
                        car.getModelName(),
                        car.getBrand() + " " + car.getModelName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeCarouselCarVO> getHomeCarouselCars(Integer limit) {
        int normalizedLimit = normalizeHomeCarouselLimit(limit);
        return carModelMapper.findHomeCarouselCars(normalizedLimit).stream()
                .map(this::toHomeCarouselCarVO)
                .toList();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_OPTION_LIMIT;
        }
        if (limit < 1 || limit > MAX_OPTION_LIMIT) {
            throw new BusinessException("limit must be between 1 and 100");
        }
        return limit;
    }

    private int normalizeHomeCarouselLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_HOME_CAROUSEL_LIMIT;
        }
        if (limit < MIN_HOME_CAROUSEL_LIMIT || limit > MAX_HOME_CAROUSEL_LIMIT) {
            throw new BusinessException("limit must be between 3 and 12");
        }
        return limit;
    }

    private List<Long> normalizeCompareIds(List<Long> carIds) {
        if (carIds == null || carIds.isEmpty()) {
            throw new BusinessException("carIds must contain 1 to 3 cars");
        }
        List<Long> normalizedIds = carIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (normalizedIds.size() < MIN_COMPARE_COUNT) {
            throw new BusinessException("carIds must contain at least 1 car");
        }
        if (normalizedIds.size() > MAX_COMPARE_COUNT) {
            throw new BusinessException("carIds can contain at most 3 cars");
        }
        return normalizedIds;
    }

    private CarCompareCarVO toCompareCarVO(Long carId) {
        CarModel carModel = carModelMapper.findById(carId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "car model not found: " + carId));
        CarCompareCarVO vo = new CarCompareCarVO();
        vo.setCarId(carModel.getId());
        vo.setBrand(carModel.getBrand());
        vo.setSeries(carModel.getSeries());
        vo.setModelName(carModel.getModelName());
        vo.setGuidePrice(carModel.getGuidePrice());
        vo.setBodyType(carModel.getBodyType());
        vo.setEnergyType(carModel.getEnergyType());
        vo.setSeats(carModel.getSeats());
        vo.setLaunchYear(carModel.getLaunchYear());
        vo.setImageUrl(carModel.getImageUrl());
        vo.setParam(carParamMapper.findByCarId(carId).map(this::toCarParamVO).orElse(null));
        vo.setScores(carFeatureScoreMapper.findByCarId(carId).map(this::toScoreMap).orElse(null));
        return vo;
    }

    private Map<String, BigDecimal> toScoreMap(CarFeatureScore score) {
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        scores.put("space", score.getSpaceScore());
        scores.put("safety", score.getSafetyScore());
        scores.put("energy", score.getEnergyScore());
        scores.put("intelligence", score.getIntelligenceScore());
        scores.put("comfort", score.getComfortScore());
        scores.put("power", score.getPowerScore());
        scores.put("reputation", score.getReputationScore());
        scores.put("popularity", score.getPopularityScore());
        return scores;
    }

    private CarModelVO toCarModelVO(CarModel carModel) {
        CarModelVO vo = new CarModelVO();
        vo.setId(carModel.getId());
        vo.setBrand(carModel.getBrand());
        vo.setSeries(carModel.getSeries());
        vo.setModelName(carModel.getModelName());
        vo.setGuidePrice(carModel.getGuidePrice());
        vo.setBodyType(carModel.getBodyType());
        vo.setEnergyType(carModel.getEnergyType());
        vo.setSeats(carModel.getSeats());
        vo.setLaunchYear(carModel.getLaunchYear());
        vo.setImageUrl(carModel.getImageUrl());
        vo.setSalesVolume(carModel.getSalesVolume());
        vo.setUserRating(carModel.getUserRating());
        vo.setAuditStatus(carModel.getAuditStatus());
        vo.setCreateTime(carModel.getCreateTime());
        vo.setUpdateTime(carModel.getUpdateTime());
        return vo;
    }

    private HomeCarouselCarVO toHomeCarouselCarVO(CarModel carModel) {
        HomeCarouselCarVO vo = new HomeCarouselCarVO();
        vo.setId(carModel.getId());
        vo.setBrand(carModel.getBrand());
        vo.setSeries(carModel.getSeries());
        vo.setModelName(carModel.getModelName());
        vo.setGuidePrice(carModel.getGuidePrice());
        vo.setBodyType(carModel.getBodyType());
        vo.setEnergyType(carModel.getEnergyType());
        vo.setSeats(carModel.getSeats());
        vo.setImageUrl(carModel.getImageUrl());
        return vo;
    }

    private CarParamVO toCarParamVO(CarParam param) {
        CarParamVO vo = new CarParamVO();
        vo.setId(param.getId());
        vo.setCarId(param.getCarId());
        vo.setLengthMm(param.getLengthMm());
        vo.setWidthMm(param.getWidthMm());
        vo.setHeightMm(param.getHeightMm());
        vo.setWheelbaseMm(param.getWheelbaseMm());
        vo.setFuelConsumption(param.getFuelConsumption());
        vo.setElectricConsumption(param.getElectricConsumption());
        vo.setElectricRangeKm(param.getElectricRangeKm());
        vo.setTotalRangeKm(param.getTotalRangeKm());
        vo.setAcceleration100(param.getAcceleration100());
        vo.setAirbagCount(param.getAirbagCount());
        vo.setHasAbs(param.getHasAbs());
        vo.setHasEsp(param.getHasEsp());
        vo.setHasActiveBrake(param.getHasActiveBrake());
        vo.setHasLaneKeep(param.getHasLaneKeep());
        vo.setHasAdaptiveCruise(param.getHasAdaptiveCruise());
        vo.setHasBlindSpot(param.getHasBlindSpot());
        vo.setHasReverseCamera(param.getHasReverseCamera());
        vo.setHas360Camera(param.getHas360Camera());
        vo.setHasOta(param.getHasOta());
        vo.setHasVoiceControl(param.getHasVoiceControl());
        vo.setHasAutoParking(param.getHasAutoParking());
        vo.setScreenSize(param.getScreenSize());
        vo.setAssistDriveLevel(param.getAssistDriveLevel());
        vo.setCreateTime(param.getCreateTime());
        vo.setUpdateTime(param.getUpdateTime());
        return vo;
    }

    private CarFeatureScoreVO toCarFeatureScoreVO(CarFeatureScore score) {
        CarFeatureScoreVO vo = new CarFeatureScoreVO();
        vo.setId(score.getId());
        vo.setCarId(score.getCarId());
        vo.setSpaceScore(score.getSpaceScore());
        vo.setSafetyScore(score.getSafetyScore());
        vo.setEnergyScore(score.getEnergyScore());
        vo.setIntelligenceScore(score.getIntelligenceScore());
        vo.setComfortScore(score.getComfortScore());
        vo.setPowerScore(score.getPowerScore());
        vo.setReputationScore(score.getReputationScore());
        vo.setPopularityScore(score.getPopularityScore());
        vo.setScoreVersion(score.getScoreVersion());
        vo.setCalculatedTime(score.getCalculatedTime());
        vo.setCreateTime(score.getCreateTime());
        vo.setUpdateTime(score.getUpdateTime());
        return vo;
    }
}
