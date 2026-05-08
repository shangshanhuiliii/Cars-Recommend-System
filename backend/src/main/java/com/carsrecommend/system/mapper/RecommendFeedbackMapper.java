package com.carsrecommend.system.mapper;

import com.carsrecommend.system.entity.RecommendFeedback;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class RecommendFeedbackMapper {

    private static final String SELECT_COLUMNS = """
            id, user_id, record_id, satisfaction_score, satisfaction_level, reason_tags,
            comment, deleted, create_time, update_time
            """;

    private static final RowMapper<RecommendFeedback> ROW_MAPPER = (resultSet, rowNum) -> {
        RecommendFeedback feedback = new RecommendFeedback();
        feedback.setId(resultSet.getLong("id"));
        feedback.setUserId(resultSet.getLong("user_id"));
        feedback.setRecordId(resultSet.getLong("record_id"));
        feedback.setSatisfactionScore(resultSet.getInt("satisfaction_score"));
        feedback.setSatisfactionLevel(resultSet.getString("satisfaction_level"));
        feedback.setReasonTags(resultSet.getString("reason_tags"));
        feedback.setComment(resultSet.getString("comment"));
        feedback.setDeleted(resultSet.getBoolean("deleted"));
        feedback.setCreateTime(readLocalDateTime(resultSet, "create_time"));
        feedback.setUpdateTime(readLocalDateTime(resultSet, "update_time"));
        return feedback;
    };

    private final JdbcTemplate jdbcTemplate;

    public RecommendFeedbackMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<RecommendFeedback> findActiveByUserIdAndRecordId(Long userId, Long recordId) {
        List<RecommendFeedback> feedback = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS
                        + " FROM recommend_feedback WHERE user_id = ? AND record_id = ? AND deleted = FALSE",
                ROW_MAPPER,
                userId,
                recordId);
        return feedback.stream().findFirst();
    }

    public Optional<RecommendFeedback> findAnyByUserIdAndRecordId(Long userId, Long recordId) {
        List<RecommendFeedback> feedback = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS
                        + " FROM recommend_feedback WHERE user_id = ? AND record_id = ?",
                ROW_MAPPER,
                userId,
                recordId);
        return feedback.stream().findFirst();
    }

    public int insert(RecommendFeedback feedback) {
        return jdbcTemplate.update(
                """
                        INSERT INTO recommend_feedback (
                            user_id, record_id, satisfaction_score, satisfaction_level, reason_tags, comment
                        ) VALUES (?, ?, ?, ?, ?, ?)
                        """,
                feedback.getUserId(),
                feedback.getRecordId(),
                feedback.getSatisfactionScore(),
                feedback.getSatisfactionLevel(),
                feedback.getReasonTags(),
                feedback.getComment());
    }

    public int updateByUserIdAndRecordId(RecommendFeedback feedback) {
        return jdbcTemplate.update(
                """
                        UPDATE recommend_feedback
                        SET satisfaction_score = ?, satisfaction_level = ?, reason_tags = ?,
                            comment = ?, deleted = FALSE, update_time = CURRENT_TIMESTAMP
                        WHERE user_id = ? AND record_id = ?
                        """,
                feedback.getSatisfactionScore(),
                feedback.getSatisfactionLevel(),
                feedback.getReasonTags(),
                feedback.getComment(),
                feedback.getUserId(),
                feedback.getRecordId());
    }

    public long countByUserId(Long userId) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommend_feedback WHERE user_id = ? AND deleted = FALSE",
                Long.class,
                userId);
        return total == null ? 0 : total;
    }

    public List<RecommendFeedback> findPageByUserId(Long userId, long limit, long offset) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS
                        + " FROM recommend_feedback WHERE user_id = ? AND deleted = FALSE"
                        + " ORDER BY update_time DESC, id DESC LIMIT ? OFFSET ?",
                ROW_MAPPER,
                userId,
                limit,
                offset);
    }

    private static LocalDateTime readLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
