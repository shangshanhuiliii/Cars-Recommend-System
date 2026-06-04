package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.auth.AuthContext;
import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.mapper.AppUserMapper;
import com.carsrecommend.system.mapper.CarModelMapper;
import com.carsrecommend.system.mapper.UserCompareCarMapper;
import com.carsrecommend.system.service.CarDetailService;
import com.carsrecommend.system.service.UserCompareService;
import com.carsrecommend.system.vo.CarCompareDimensionVO;
import com.carsrecommend.system.vo.CarCompareVO;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class UserCompareServiceImpl implements UserCompareService {

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

    private final UserCompareCarMapper userCompareCarMapper;
    private final CarModelMapper carModelMapper;
    private final AppUserMapper appUserMapper;
    private final CarDetailService carDetailService;

    public UserCompareServiceImpl(
            UserCompareCarMapper userCompareCarMapper,
            CarModelMapper carModelMapper,
            AppUserMapper appUserMapper,
            CarDetailService carDetailService) {
        this.userCompareCarMapper = userCompareCarMapper;
        this.carModelMapper = carModelMapper;
        this.appUserMapper = appUserMapper;
        this.carDetailService = carDetailService;
    }

    @Override
    @Transactional(readOnly = true)
    public CarCompareVO current() {
        return currentForUser(currentUserId());
    }

    @Override
    @Transactional
    public CarCompareVO add(Long carId) {
        Long userId = currentUserId();
        assertActiveCar(carId);
        if (!userCompareCarMapper.existsActive(userId, carId)
                && userCompareCarMapper.countActiveByUserId(userId) >= MAX_COMPARE_COUNT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "最多选择 3 款车型进行对比。");
        }
        if (!userCompareCarMapper.existsActive(userId, carId)) {
            Integer sortNo = userCompareCarMapper.nextSortNo(userId);
            if (userCompareCarMapper.findAnyByUserIdAndCarId(userId, carId).isPresent()) {
                userCompareCarMapper.activate(userId, carId, sortNo);
            } else {
                userCompareCarMapper.insert(userId, carId, sortNo);
            }
        }
        return currentForUser(userId);
    }

    @Override
    @Transactional
    public CarCompareVO remove(Long carId) {
        Long userId = currentUserId();
        userCompareCarMapper.softDelete(userId, carId);
        return currentForUser(userId);
    }

    @Override
    @Transactional
    public CarCompareVO clear() {
        Long userId = currentUserId();
        userCompareCarMapper.clear(userId);
        return currentForUser(userId);
    }

    private CarCompareVO currentForUser(Long userId) {
        List<Long> carIds = userCompareCarMapper.findActiveCarIdsByUserId(userId);
        if (carIds.isEmpty()) {
            CarCompareVO vo = new CarCompareVO();
            vo.setCarIds(List.of());
            vo.setCars(List.of());
            vo.setDimensions(COMPARE_DIMENSIONS);
            return vo;
        }
        return carDetailService.compareCars(carIds);
    }

    private Long currentUserId() {
        Long userId = AuthContext.currentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录后再继续操作");
        }
        if (appUserMapper.findByIdForAdmin(userId).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return userId;
    }

    private void assertActiveCar(Long carId) {
        if (carId == null || carId <= 0 || !carModelMapper.existsActiveById(carId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "车型不存在");
        }
    }
}
