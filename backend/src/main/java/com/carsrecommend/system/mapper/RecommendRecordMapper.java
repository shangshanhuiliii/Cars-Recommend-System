package com.carsrecommend.system.mapper;

import com.carsrecommend.system.entity.RecommendRecord;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class RecommendRecordMapper {

    private static final String SELECT_COLUMNS = """
            id, user_id, demand_id, profile_text, weight_snapshot, fallback_message,
            recommend_status, deleted, create_time, update_time
            """;

    private static final RowMapper<RecommendRecord> ROW_MAPPER = (resultSet, rowNum) -> {
        RecommendRecord record = new RecommendRecord();
        record.setId(resultSet.getLong("id"));
        record.setUserId(resultSet.getLong("user_id"));
        record.setDemandId(resultSet.getLong("demand_id"));
        record.setProfileText(resultSet.getString("profile_text"));
        record.setWeightSnapshot(resultSet.getString("weight_snapshot"));
        record.setFallbackMessage(resultSet.getString("fallback_message"));
        record.setRecommendStatus(resultSet.getString("recommend_status"));
        record.setDeleted(resultSet.getBoolean("deleted"));
        record.setCreateTime(readLocalDateTime(resultSet, "create_time"));
        record.setUpdateTime(readLocalDateTime(resultSet, "update_time"));
        return record;
    };

    private final JdbcTemplate jdbcTemplate;

    public RecommendRecordMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RecommendRecord insert(RecommendRecord record) {
        String sql = """
                INSERT INTO recommend_record (
                    user_id, demand_id, profile_text, weight_snapshot, fallback_message, recommend_status
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setLong(1, record.getUserId());
            statement.setLong(2, record.getDemandId());
            statement.setString(3, record.getProfileText());
            statement.setString(4, record.getWeightSnapshot());
            statement.setString(5, record.getFallbackMessage());
            statement.setString(6, record.getRecommendStatus());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            record.setId(key.longValue());
        }
        return record;
    }

    public long countByUserId(Long userId) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommend_record WHERE user_id = ? AND deleted = FALSE",
                Long.class,
                userId);
        return total == null ? 0 : total;
    }

    public List<RecommendRecord> findPageByUserId(Long userId, long limit, long offset) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS
                        + " FROM recommend_record WHERE user_id = ? AND deleted = FALSE"
                        + " ORDER BY create_time DESC, id DESC LIMIT ? OFFSET ?",
                ROW_MAPPER,
                userId,
                limit,
                offset);
    }

    public Optional<RecommendRecord> findByIdAndUserId(Long recordId, Long userId) {
        List<RecommendRecord> records = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS
                        + " FROM recommend_record WHERE id = ? AND user_id = ? AND deleted = FALSE",
                ROW_MAPPER,
                recordId,
                userId);
        return records.stream().findFirst();
    }

    private static LocalDateTime readLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
