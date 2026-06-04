package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.dto.AdminUserQuery;
import com.carsrecommend.system.dto.AdminUserStatusUpdateRequest;
import com.carsrecommend.system.entity.AppUser;
import com.carsrecommend.system.entity.RecommendFeedback;
import com.carsrecommend.system.entity.UserDemand;
import com.carsrecommend.system.mapper.AppUserMapper;
import com.carsrecommend.system.mapper.RecommendFeedbackMapper;
import com.carsrecommend.system.mapper.RecommendRecordMapper;
import com.carsrecommend.system.mapper.UserDemandMapper;
import com.carsrecommend.system.mapper.UserFavoriteMapper;
import com.carsrecommend.system.service.AdminUserService;
import com.carsrecommend.system.service.RecommendationRecordService;
import com.carsrecommend.system.vo.AdminUserBaseVO;
import com.carsrecommend.system.vo.AdminUserDemandVO;
import com.carsrecommend.system.vo.AdminUserDetailVO;
import com.carsrecommend.system.vo.AdminUserListItemVO;
import com.carsrecommend.system.vo.AdminUserSummaryVO;
import com.carsrecommend.system.vo.RecommendationFeedbackVO;
import com.carsrecommend.system.vo.RecommendationHistoryItemVO;
import com.carsrecommend.system.vo.UserFavoriteItemVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AdminUserServiceImpl implements AdminUserService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int DETAIL_PREVIEW_SIZE = 5;
    private static final int MAX_SIZE = 100;

    private final AppUserMapper appUserMapper;
    private final UserDemandMapper userDemandMapper;
    private final RecommendRecordMapper recommendRecordMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final RecommendFeedbackMapper recommendFeedbackMapper;
    private final RecommendationRecordService recommendationRecordService;
    private final ObjectMapper objectMapper;

    public AdminUserServiceImpl(
            AppUserMapper appUserMapper,
            UserDemandMapper userDemandMapper,
            RecommendRecordMapper recommendRecordMapper,
            UserFavoriteMapper userFavoriteMapper,
            RecommendFeedbackMapper recommendFeedbackMapper,
            RecommendationRecordService recommendationRecordService,
            ObjectMapper objectMapper) {
        this.appUserMapper = appUserMapper;
        this.userDemandMapper = userDemandMapper;
        this.recommendRecordMapper = recommendRecordMapper;
        this.userFavoriteMapper = userFavoriteMapper;
        this.recommendFeedbackMapper = recommendFeedbackMapper;
        this.recommendationRecordService = recommendationRecordService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminUserListItemVO> list(AdminUserQuery query) {
        int page = normalizePage(query == null ? null : query.getPage());
        int size = normalizeSize(query == null ? null : query.getSize());
        String keyword = query == null ? null : trimToNull(query.getKeyword());
        String status = query == null ? null : normalizeStatusFilter(query.getStatus());
        long total = appUserMapper.countForAdmin(keyword, status);
        long offset = (long) (page - 1) * size;
        List<AdminUserListItemVO> records = appUserMapper.findPageForAdmin(keyword, status, size, offset)
                .stream()
                .map(this::toListItemVO)
                .toList();
        return PageResult.of(records, total, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailVO detail(Long userId) {
        AppUser user = getExistingUser(userId);
        AdminUserDetailVO vo = new AdminUserDetailVO();
        vo.setUser(toBaseVO(user));
        vo.setSummary(buildSummary(userId));
        vo.setLatestDemand(userDemandMapper.findLatestByUserId(userId).map(this::toDemandVO).orElse(null));
        vo.setRecentRecommendRecords(recommendationRecordService
                .adminHistory(userId, DEFAULT_PAGE, DETAIL_PREVIEW_SIZE)
                .getRecords());
        vo.setFavorites(List.of());
        vo.setFeedbacks(List.of());
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RecommendationHistoryItemVO> recommendRecords(Long userId, Integer page, Integer size) {
        getExistingUser(userId);
        return recommendationRecordService.adminHistory(userId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserFavoriteItemVO> favorites(Long userId, Integer page, Integer size) {
        getExistingUser(userId);
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        long total = userFavoriteMapper.countActiveByUserId(userId);
        long offset = (long) (pageNo - 1) * pageSize;
        return PageResult.of(userFavoriteMapper.findPageByUserId(userId, pageSize, offset), total, pageNo, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RecommendationFeedbackVO> feedbacks(Long userId, Integer page, Integer size) {
        getExistingUser(userId);
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        long total = recommendFeedbackMapper.countByUserId(userId);
        long offset = (long) (pageNo - 1) * pageSize;
        List<RecommendationFeedbackVO> records = recommendFeedbackMapper.findPageByUserId(userId, pageSize, offset)
                .stream()
                .map(this::toFeedbackVO)
                .toList();
        return PageResult.of(records, total, pageNo, pageSize);
    }

    @Override
    @Transactional
    public AdminUserBaseVO updateStatus(Long userId, AdminUserStatusUpdateRequest request) {
        getExistingUser(userId);
        String status = normalizeStatus(request == null ? null : request.getStatus());
        int updated = appUserMapper.updateStatus(userId, status);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return toBaseVO(getExistingUser(userId));
    }

    private AppUser getExistingUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return appUserMapper.findByIdForAdmin(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    private AdminUserSummaryVO buildSummary(Long userId) {
        AdminUserSummaryVO summary = new AdminUserSummaryVO();
        summary.setDemandCount(userDemandMapper.countByUserId(userId));
        summary.setRecommendRecordCount(recommendRecordMapper.countByUserId(userId));
        summary.setFavoriteCount(userFavoriteMapper.countActiveByUserId(userId));
        summary.setFeedbackCount(recommendFeedbackMapper.countByUserId(userId));
        return summary;
    }

    private AdminUserListItemVO toListItemVO(AppUser user) {
        AdminUserListItemVO vo = new AdminUserListItemVO();
        copyBase(user, vo);
        Long userId = user.getId();
        vo.setRecommendRecordCount(recommendRecordMapper.countByUserId(userId));
        vo.setFavoriteCount(userFavoriteMapper.countActiveByUserId(userId));
        vo.setFeedbackCount(recommendFeedbackMapper.countByUserId(userId));
        return vo;
    }

    private AdminUserBaseVO toBaseVO(AppUser user) {
        AdminUserBaseVO vo = new AdminUserBaseVO();
        copyBase(user, vo);
        return vo;
    }

    private void copyBase(AppUser user, AdminUserBaseVO vo) {
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setDeleted(user.getDeleted());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
    }

    private AdminUserDemandVO toDemandVO(UserDemand demand) {
        AdminUserDemandVO vo = new AdminUserDemandVO();
        vo.setId(demand.getId());
        vo.setUserId(demand.getUserId());
        vo.setRawText(demand.getRawText());
        vo.setBudgetMin(demand.getBudgetMin());
        vo.setBudgetMax(demand.getBudgetMax());
        vo.setBrands(readStringList(demand.getBrands()));
        vo.setBodyTypes(readStringList(demand.getBodyTypes()));
        vo.setEnergyTypes(readStringList(demand.getEnergyTypes()));
        vo.setSeatOptions(readStringList(demand.getSeatOptions()));
        vo.setScenes(readStringList(demand.getScenes()));
        vo.setProfileText(demand.getProfileText());
        vo.setCreateTime(demand.getCreateTime());
        vo.setUpdateTime(demand.getUpdateTime());
        return vo;
    }

    private RecommendationFeedbackVO toFeedbackVO(RecommendFeedback feedback) {
        RecommendationFeedbackVO vo = new RecommendationFeedbackVO();
        vo.setId(feedback.getId());
        vo.setUserId(feedback.getUserId());
        vo.setRecordId(feedback.getRecordId());
        vo.setSatisfactionScore(feedback.getSatisfactionScore());
        vo.setSatisfactionLevel(feedback.getSatisfactionLevel());
        vo.setReasonTags(readStringList(feedback.getReasonTags()));
        vo.setComment(feedback.getComment());
        vo.setCreateTime(feedback.getCreateTime());
        vo.setUpdateTime(feedback.getUpdateTime());
        return vo;
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 1) {
            throw new BusinessException("页码必须大于或等于 1");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException("每页数量必须在 1 到 100 之间");
        }
        return size;
    }

    private String normalizeStatusFilter(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return normalizeStatus(status);
    }

    private String normalizeStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户状态不能为空");
        }
        normalized = normalized.toUpperCase();
        if (!STATUS_ACTIVE.equals(normalized) && !STATUS_DISABLED.equals(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户状态只能为启用或禁用");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isTextual()) {
                node = objectMapper.readTree(node.asText());
            }
            if (!node.isArray()) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            for (JsonNode item : node) {
                values.add(item.asText());
            }
            return values;
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }
}
