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
import com.carsrecommend.system.vo.CarDetailVO;
import com.carsrecommend.system.vo.CarFeatureScoreVO;
import com.carsrecommend.system.vo.CarModelVO;
import com.carsrecommend.system.vo.CarParamVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class CarDetailServiceImpl implements CarDetailService {

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
