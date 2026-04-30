package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.common.enums.AuditStatus;
import com.carsrecommend.system.common.enums.BodyType;
import com.carsrecommend.system.common.enums.EnergyType;
import com.carsrecommend.system.dto.CarModelSaveRequest;
import com.carsrecommend.system.dto.CarPageQuery;
import com.carsrecommend.system.entity.CarModel;
import com.carsrecommend.system.mapper.CarModelMapper;
import com.carsrecommend.system.service.CarModelService;
import com.carsrecommend.system.vo.CarModelVO;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class CarModelServiceImpl implements CarModelService {

    private final CarModelMapper carModelMapper;

    public CarModelServiceImpl(CarModelMapper carModelMapper) {
        this.carModelMapper = carModelMapper;
    }

    @Override
    public PageResult<CarModelVO> page(CarPageQuery query) {
        validatePageQuery(query);
        long total = carModelMapper.count(query);
        List<CarModelVO> records = carModelMapper.page(query).stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(records, total, query.getPage(), query.getSize());
    }

    @Override
    public CarModelVO getById(Long id) {
        return carModelMapper.findById(id)
                .map(this::toVO)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "car model not found"));
    }

    @Override
    public CarModelVO create(CarModelSaveRequest request) {
        CarModel created = carModelMapper.insert(toEntity(request));
        return getById(created.getId());
    }

    @Override
    public CarModelVO update(Long id, CarModelSaveRequest request) {
        ensureActiveCarExists(id);
        CarModel carModel = toEntity(request);
        carModel.setId(id);
        if (carModelMapper.update(carModel) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "car model not found");
        }
        return getById(id);
    }

    @Override
    public void softDelete(Long id) {
        ensureActiveCarExists(id);
        if (carModelMapper.softDelete(id) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "car model not found");
        }
    }

    @Override
    public List<CarModelVO> listApprovedRecommendationCandidates() {
        return carModelMapper.findApprovedRecommendationCandidates().stream()
                .map(this::toVO)
                .toList();
    }

    private void ensureActiveCarExists(Long id) {
        if (!carModelMapper.existsActiveById(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "car model not found");
        }
    }

    private void validatePageQuery(CarPageQuery query) {
        if (query.getMinPrice() != null && query.getMaxPrice() != null
                && query.getMinPrice().compareTo(query.getMaxPrice()) > 0) {
            throw new BusinessException("minPrice must not be greater than maxPrice");
        }
        if (StringUtils.hasText(query.getBodyType())) {
            BodyType.fromCode(query.getBodyType().trim());
        }
        if (StringUtils.hasText(query.getEnergyType())) {
            EnergyType energyType = EnergyType.fromCode(query.getEnergyType().trim());
            if (!energyType.isCarModelType()) {
                throw new BusinessException("car model energyType must not be NEW_ENERGY");
            }
        }
        if (StringUtils.hasText(query.getAuditStatus())) {
            AuditStatus.fromCode(query.getAuditStatus().trim());
        }
    }

    private CarModel toEntity(CarModelSaveRequest request) {
        CarModel carModel = new CarModel();
        carModel.setBrand(trim(request.getBrand()));
        carModel.setSeries(trim(request.getSeries()));
        carModel.setModelName(trim(request.getModelName()));
        carModel.setGuidePrice(request.getGuidePrice());
        carModel.setBodyType(request.getBodyType().getCode());
        carModel.setEnergyType(request.getEnergyType().getCode());
        carModel.setSeats(request.getSeats());
        carModel.setLaunchYear(request.getLaunchYear());
        carModel.setImageUrl(trimToNull(request.getImageUrl()));
        carModel.setSalesVolume(request.getSalesVolume());
        carModel.setUserRating(request.getUserRating());
        carModel.setAuditStatus(request.getAuditStatus().getCode());
        return carModel;
    }

    private CarModelVO toVO(CarModel carModel) {
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

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
