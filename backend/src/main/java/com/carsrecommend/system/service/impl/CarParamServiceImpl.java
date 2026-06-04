package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.dto.CarParamSaveRequest;
import com.carsrecommend.system.entity.CarParam;
import com.carsrecommend.system.mapper.CarModelMapper;
import com.carsrecommend.system.mapper.CarParamMapper;
import com.carsrecommend.system.service.CarParamService;
import com.carsrecommend.system.vo.CarParamVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class CarParamServiceImpl implements CarParamService {

    private final CarModelMapper carModelMapper;
    private final CarParamMapper carParamMapper;

    public CarParamServiceImpl(CarModelMapper carModelMapper, CarParamMapper carParamMapper) {
        this.carModelMapper = carModelMapper;
        this.carParamMapper = carParamMapper;
    }

    @Override
    public CarParamVO getByCarId(Long carId) {
        ensureActiveCarExists(carId);
        return carParamMapper.findByCarId(carId)
                .map(this::toVO)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "车型参数不存在"));
    }

    @Override
    public CarParamVO saveByCarId(Long carId, CarParamSaveRequest request) {
        if (request.getCarId() != null && !request.getCarId().equals(carId)) {
            throw new BusinessException("请求中的车型 ID 必须与路径中的车型 ID 一致");
        }
        ensureActiveCarExists(carId);
        CarParam param = toEntity(carId, request);
        if (carParamMapper.findByCarId(carId).isPresent()) {
            carParamMapper.updateByCarId(param);
        } else {
            carParamMapper.insert(param);
        }
        return getByCarId(carId);
    }

    private void ensureActiveCarExists(Long carId) {
        if (!carModelMapper.existsActiveById(carId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "车型不存在");
        }
    }

    private CarParam toEntity(Long carId, CarParamSaveRequest request) {
        CarParam param = new CarParam();
        param.setCarId(carId);
        param.setLengthMm(request.getLengthMm());
        param.setWidthMm(request.getWidthMm());
        param.setHeightMm(request.getHeightMm());
        param.setWheelbaseMm(request.getWheelbaseMm());
        param.setFuelConsumption(request.getFuelConsumption());
        param.setElectricConsumption(request.getElectricConsumption());
        param.setElectricRangeKm(request.getElectricRangeKm());
        param.setTotalRangeKm(request.getTotalRangeKm());
        param.setAcceleration100(request.getAcceleration100());
        param.setAirbagCount(request.getAirbagCount());
        param.setHasAbs(request.getHasAbs());
        param.setHasEsp(request.getHasEsp());
        param.setHasActiveBrake(request.getHasActiveBrake());
        param.setHasLaneKeep(request.getHasLaneKeep());
        param.setHasAdaptiveCruise(request.getHasAdaptiveCruise());
        param.setHasBlindSpot(request.getHasBlindSpot());
        param.setHasReverseCamera(request.getHasReverseCamera());
        param.setHas360Camera(request.getHas360Camera());
        param.setHasOta(request.getHasOta());
        param.setHasVoiceControl(request.getHasVoiceControl());
        param.setHasAutoParking(request.getHasAutoParking());
        param.setScreenSize(request.getScreenSize());
        param.setAssistDriveLevel(trimToNull(request.getAssistDriveLevel()));
        return param;
    }

    private CarParamVO toVO(CarParam param) {
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
