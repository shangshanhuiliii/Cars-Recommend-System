package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.auth.AuthContext;
import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.mapper.CarModelMapper;
import com.carsrecommend.system.mapper.UserDemandMapper;
import com.carsrecommend.system.mapper.UserFavoriteMapper;
import com.carsrecommend.system.service.UserFavoriteService;
import com.carsrecommend.system.vo.FavoriteStatusVO;
import com.carsrecommend.system.vo.UserFavoriteItemVO;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class UserFavoriteServiceImpl implements UserFavoriteService {

    private static final long DEFAULT_SEED_USER_ID = 1L;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final UserDemandMapper userDemandMapper;
    private final CarModelMapper carModelMapper;
    private final UserFavoriteMapper userFavoriteMapper;

    public UserFavoriteServiceImpl(
            UserDemandMapper userDemandMapper,
            CarModelMapper carModelMapper,
            UserFavoriteMapper userFavoriteMapper) {
        this.userDemandMapper = userDemandMapper;
        this.carModelMapper = carModelMapper;
        this.userFavoriteMapper = userFavoriteMapper;
    }

    @Override
    @Transactional
    public void favorite(Long userId, Long carId) {
        Long resolvedUserId = resolveUserId(userId);
        assertActiveCar(carId);
        if (userFavoriteMapper.findAnyByUserIdAndCarId(resolvedUserId, carId).isPresent()) {
            userFavoriteMapper.activate(resolvedUserId, carId);
            return;
        }
        userFavoriteMapper.insert(resolvedUserId, carId);
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long carId) {
        Long resolvedUserId = resolveUserId(userId);
        userFavoriteMapper.cancel(resolvedUserId, carId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserFavoriteItemVO> list(Long userId, Integer page, Integer size) {
        Long resolvedUserId = resolveUserId(userId);
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        long total = userFavoriteMapper.countActiveByUserId(resolvedUserId);
        long offset = (long) (pageNo - 1) * pageSize;
        return PageResult.of(
                userFavoriteMapper.findPageByUserId(resolvedUserId, pageSize, offset),
                total,
                pageNo,
                pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteStatusVO> status(Long userId, List<Long> carIds) {
        Long resolvedUserId = resolveUserId(userId);
        if (carIds == null || carIds.isEmpty()) {
            return List.of();
        }
        return carIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .map(carId -> new FavoriteStatusVO(carId, userFavoriteMapper.existsActive(resolvedUserId, carId)))
                .toList();
    }

    private Long resolveUserId(Long userId) {
        Long currentUserId = AuthContext.currentUserIdOrNull();
        Long resolvedUserId = currentUserId != null ? currentUserId : (userId == null ? DEFAULT_SEED_USER_ID : userId);
        if (!userDemandMapper.existsActiveUser(resolvedUserId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "app user not found");
        }
        return resolvedUserId;
    }

    private void assertActiveCar(Long carId) {
        if (carId == null || carId <= 0 || !carModelMapper.existsActiveById(carId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "car model not found");
        }
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 1) {
            throw new BusinessException("page must be greater than or equal to 1");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException("size must be between 1 and 100");
        }
        return size;
    }
}
