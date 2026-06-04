package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.entity.CarFeatureScore;
import com.carsrecommend.system.entity.CarModel;
import com.carsrecommend.system.entity.CarParam;
import com.carsrecommend.system.mapper.CarFeatureScoreMapper;
import com.carsrecommend.system.mapper.CarModelMapper;
import com.carsrecommend.system.mapper.CarParamMapper;
import com.carsrecommend.system.service.CarFeatureScoreService;
import com.carsrecommend.system.vo.CarFeatureScoreBatchVO;
import com.carsrecommend.system.vo.CarFeatureScoreVO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class CarFeatureScoreServiceImpl implements CarFeatureScoreService {

    private final CarModelMapper carModelMapper;
    private final CarParamMapper carParamMapper;
    private final CarFeatureScoreMapper carFeatureScoreMapper;
    private final CarFeatureScoreCalculator calculator;

    public CarFeatureScoreServiceImpl(
            CarModelMapper carModelMapper,
            CarParamMapper carParamMapper,
            CarFeatureScoreMapper carFeatureScoreMapper,
            CarFeatureScoreCalculator calculator) {
        this.carModelMapper = carModelMapper;
        this.carParamMapper = carParamMapper;
        this.carFeatureScoreMapper = carFeatureScoreMapper;
        this.calculator = calculator;
    }

    @Override
    public CarFeatureScoreVO recalculate(Long carId) {
        CarModel carModel = loadActiveCar(carId);
        int maxSalesVolume = carModelMapper.findMaxSalesVolume();
        return recalculateOne(carModel, maxSalesVolume);
    }

    @Override
    public CarFeatureScoreBatchVO recalculateAll() {
        List<CarModel> cars = carModelMapper.findAllActive();
        int maxSalesVolume = carModelMapper.findMaxSalesVolume();
        List<CarFeatureScoreVO> records = new ArrayList<>();
        for (CarModel car : cars) {
            records.add(recalculateOne(car, maxSalesVolume));
        }
        return new CarFeatureScoreBatchVO(records.size(), records);
    }

    @Override
    public CarFeatureScoreVO getByCarId(Long carId) {
        loadActiveCar(carId);
        return carFeatureScoreMapper.findByCarId(carId)
                .map(this::toVO)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "车型评分不存在"));
    }

    private CarFeatureScoreVO recalculateOne(CarModel carModel, int maxSalesVolume) {
        CarParam param = carParamMapper.findByCarId(carModel.getId()).orElse(null);
        CarFeatureScore score = calculator.calculate(carModel, param, maxSalesVolume);
        carFeatureScoreMapper.upsertByCarId(score);
        return getByCarId(carModel.getId());
    }

    private CarModel loadActiveCar(Long carId) {
        return carModelMapper.findById(carId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "车型不存在"));
    }

    private CarFeatureScoreVO toVO(CarFeatureScore score) {
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
