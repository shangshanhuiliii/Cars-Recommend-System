package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.mapper.AppUserMapper;
import com.carsrecommend.system.service.AdminFeedbackService;
import com.carsrecommend.system.vo.AdminFeedbackItemVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AdminFeedbackServiceImpl implements AdminFeedbackService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;
    private final AppUserMapper appUserMapper;
    private final ObjectMapper objectMapper;

    public AdminFeedbackServiceImpl(
            JdbcTemplate jdbcTemplate,
            AppUserMapper appUserMapper,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.appUserMapper = appUserMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminFeedbackItemVO> list(
            Integer page,
            Integer size,
            String keyword,
            Long userId,
            Integer satisfactionScore) {
        if (userId != null && appUserMapper.findByIdForAdmin(userId).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        QueryParts queryParts = buildFilter(keyword, userId, satisfactionScore);
        Long total = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM recommend_feedback rf
                        JOIN app_user au ON au.id = rf.user_id AND au.deleted = FALSE
                        """
                        + queryParts.whereClause(),
                Long.class,
                queryParts.args().toArray());
        List<Object> args = new ArrayList<>(queryParts.args());
        args.add(pageSize);
        args.add((long) (pageNo - 1) * pageSize);
        List<AdminFeedbackItemVO> records = jdbcTemplate.query(
                """
                        SELECT rf.id AS feedback_id, rf.user_id, au.username, au.nickname,
                               rf.record_id, rf.satisfaction_score, rf.satisfaction_level,
                               rf.reason_tags, rf.comment, rf.create_time, rf.update_time
                        FROM recommend_feedback rf
                        JOIN app_user au ON au.id = rf.user_id AND au.deleted = FALSE
                        """
                        + queryParts.whereClause()
                        + """
                        ORDER BY rf.update_time DESC, rf.id DESC
                        LIMIT ? OFFSET ?
                        """,
                (resultSet, rowNum) -> toFeedbackItemVO(resultSet),
                args.toArray());
        return PageResult.of(records, total == null ? 0 : total, pageNo, pageSize);
    }

    private QueryParts buildFilter(String keyword, Long userId, Integer satisfactionScore) {
        StringBuilder where = new StringBuilder(" WHERE rf.deleted = FALSE ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            where.append("""
                    AND (
                        LOWER(au.username) LIKE ?
                        OR LOWER(COALESCE(au.nickname, '')) LIKE ?
                        OR LOWER(COALESCE(rf.comment, '')) LIKE ?
                    )
                    """);
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        if (userId != null) {
            where.append(" AND rf.user_id = ? ");
            args.add(userId);
        }
        if (satisfactionScore != null) {
            if (satisfactionScore < 1 || satisfactionScore > 5) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "满意度评分必须在 1 到 5 之间");
            }
            where.append(" AND rf.satisfaction_score = ? ");
            args.add(satisfactionScore);
        }
        return new QueryParts(where.toString(), args);
    }

    private AdminFeedbackItemVO toFeedbackItemVO(ResultSet resultSet) throws SQLException {
        AdminFeedbackItemVO vo = new AdminFeedbackItemVO();
        vo.setFeedbackId(resultSet.getLong("feedback_id"));
        vo.setUserId(resultSet.getLong("user_id"));
        vo.setUsername(resultSet.getString("username"));
        vo.setNickname(resultSet.getString("nickname"));
        vo.setRecordId(resultSet.getLong("record_id"));
        vo.setSatisfactionScore(resultSet.getInt("satisfaction_score"));
        vo.setSatisfactionLevel(resultSet.getString("satisfaction_level"));
        vo.setReasonTags(readStringList(resultSet.getString("reason_tags")));
        vo.setComment(resultSet.getString("comment"));
        vo.setCreateTime(readLocalDateTime(resultSet, "create_time"));
        vo.setUpdateTime(readLocalDateTime(resultSet, "update_time"));
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

    private static LocalDateTime readLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record QueryParts(String whereClause, List<Object> args) {
    }
}
