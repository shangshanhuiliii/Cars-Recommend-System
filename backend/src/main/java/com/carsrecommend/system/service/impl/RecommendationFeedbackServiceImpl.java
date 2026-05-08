package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.auth.AuthContext;
import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.dto.RecommendationFeedbackRequest;
import com.carsrecommend.system.entity.RecommendFeedback;
import com.carsrecommend.system.mapper.RecommendFeedbackMapper;
import com.carsrecommend.system.mapper.RecommendRecordMapper;
import com.carsrecommend.system.mapper.UserDemandMapper;
import com.carsrecommend.system.service.RecommendationFeedbackService;
import com.carsrecommend.system.vo.RecommendationFeedbackVO;
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
public class RecommendationFeedbackServiceImpl implements RecommendationFeedbackService {

    private static final long DEFAULT_DEMO_USER_ID = 1L;

    private final UserDemandMapper userDemandMapper;
    private final RecommendRecordMapper recommendRecordMapper;
    private final RecommendFeedbackMapper recommendFeedbackMapper;
    private final ObjectMapper objectMapper;

    public RecommendationFeedbackServiceImpl(
            UserDemandMapper userDemandMapper,
            RecommendRecordMapper recommendRecordMapper,
            RecommendFeedbackMapper recommendFeedbackMapper,
            ObjectMapper objectMapper) {
        this.userDemandMapper = userDemandMapper;
        this.recommendRecordMapper = recommendRecordMapper;
        this.recommendFeedbackMapper = recommendFeedbackMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public RecommendationFeedbackVO submit(Long recordId, RecommendationFeedbackRequest request) {
        Long resolvedUserId = resolveUserId(request.getUserId());
        assertOwnRecord(recordId, resolvedUserId);
        RecommendFeedback feedback = new RecommendFeedback();
        feedback.setUserId(resolvedUserId);
        feedback.setRecordId(recordId);
        feedback.setSatisfactionScore(request.getSatisfactionScore());
        feedback.setSatisfactionLevel(satisfactionLevel(request.getSatisfactionScore()));
        feedback.setReasonTags(writeReasonTags(request.getReasonTags()));
        feedback.setComment(normalizeComment(request.getComment()));
        if (recommendFeedbackMapper.findAnyByUserIdAndRecordId(resolvedUserId, recordId).isPresent()) {
            recommendFeedbackMapper.updateByUserIdAndRecordId(feedback);
        } else {
            recommendFeedbackMapper.insert(feedback);
        }
        return get(recordId, resolvedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationFeedbackVO get(Long recordId, Long userId) {
        Long resolvedUserId = resolveUserId(userId);
        assertOwnRecord(recordId, resolvedUserId);
        return recommendFeedbackMapper.findActiveByUserIdAndRecordId(resolvedUserId, recordId)
                .map(this::toVO)
                .orElse(null);
    }

    private Long resolveUserId(Long userId) {
        Long currentUserId = AuthContext.currentUserIdOrNull();
        Long resolvedUserId = currentUserId != null ? currentUserId : (userId == null ? DEFAULT_DEMO_USER_ID : userId);
        if (!userDemandMapper.existsActiveUser(resolvedUserId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "app user not found");
        }
        return resolvedUserId;
    }

    private void assertOwnRecord(Long recordId, Long userId) {
        recommendRecordMapper.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "recommend record not found"));
    }

    private String satisfactionLevel(Integer score) {
        if (score >= 4) {
            return "SATISFIED";
        }
        if (score == 3) {
            return "NEUTRAL";
        }
        return "DISSATISFIED";
    }

    private String writeReasonTags(List<String> reasonTags) {
        List<String> normalizedTags = reasonTags == null ? List.of() : reasonTags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        try {
            return objectMapper.writeValueAsString(normalizedTags);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize feedback reason tags", exception);
        }
    }

    private String normalizeComment(String comment) {
        if (!StringUtils.hasText(comment)) {
            return "";
        }
        return comment.trim();
    }

    private RecommendationFeedbackVO toVO(RecommendFeedback feedback) {
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
