package com.carsrecommend.system.mapper;

import com.carsrecommend.system.entity.RecommendRecord;
import java.sql.PreparedStatement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class RecommendRecordMapper {

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
}
