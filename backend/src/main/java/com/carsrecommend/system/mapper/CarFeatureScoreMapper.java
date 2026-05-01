package com.carsrecommend.system.mapper;

import com.carsrecommend.system.entity.CarFeatureScore;
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
public class CarFeatureScoreMapper {

    private static final String SELECT_COLUMNS = """
            id, car_id, space_score, safety_score, energy_score, intelligence_score,
            comfort_score, power_score, reputation_score, popularity_score,
            score_version, calculated_time, deleted, create_time, update_time
            """;

    private static final RowMapper<CarFeatureScore> ROW_MAPPER = (resultSet, rowNum) -> {
        CarFeatureScore score = new CarFeatureScore();
        score.setId(resultSet.getLong("id"));
        score.setCarId(resultSet.getLong("car_id"));
        score.setSpaceScore(resultSet.getBigDecimal("space_score"));
        score.setSafetyScore(resultSet.getBigDecimal("safety_score"));
        score.setEnergyScore(resultSet.getBigDecimal("energy_score"));
        score.setIntelligenceScore(resultSet.getBigDecimal("intelligence_score"));
        score.setComfortScore(resultSet.getBigDecimal("comfort_score"));
        score.setPowerScore(resultSet.getBigDecimal("power_score"));
        score.setReputationScore(resultSet.getBigDecimal("reputation_score"));
        score.setPopularityScore(resultSet.getBigDecimal("popularity_score"));
        score.setScoreVersion(resultSet.getString("score_version"));
        score.setCalculatedTime(readLocalDateTime(resultSet, "calculated_time"));
        score.setDeleted(resultSet.getBoolean("deleted"));
        score.setCreateTime(readLocalDateTime(resultSet, "create_time"));
        score.setUpdateTime(readLocalDateTime(resultSet, "update_time"));
        return score;
    };

    private final JdbcTemplate jdbcTemplate;

    public CarFeatureScoreMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CarFeatureScore> findByCarId(Long carId) {
        List<CarFeatureScore> scores = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM car_feature_score WHERE car_id = ? AND deleted = FALSE",
                ROW_MAPPER,
                carId);
        return scores.stream().findFirst();
    }

    public int upsertByCarId(CarFeatureScore score) {
        int updated = jdbcTemplate.update(
                """
                        UPDATE car_feature_score
                        SET space_score = ?, safety_score = ?, energy_score = ?, intelligence_score = ?,
                            comfort_score = ?, power_score = ?, reputation_score = ?, popularity_score = ?,
                            score_version = ?, calculated_time = ?, deleted = FALSE, update_time = CURRENT_TIMESTAMP
                        WHERE car_id = ?
                        """,
                score.getSpaceScore(),
                score.getSafetyScore(),
                score.getEnergyScore(),
                score.getIntelligenceScore(),
                score.getComfortScore(),
                score.getPowerScore(),
                score.getReputationScore(),
                score.getPopularityScore(),
                score.getScoreVersion(),
                Timestamp.valueOf(score.getCalculatedTime()),
                score.getCarId());
        if (updated > 0) {
            return updated;
        }
        return jdbcTemplate.update(
                """
                        INSERT INTO car_feature_score (
                            car_id, space_score, safety_score, energy_score, intelligence_score,
                            comfort_score, power_score, reputation_score, popularity_score,
                            score_version, calculated_time
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                score.getCarId(),
                score.getSpaceScore(),
                score.getSafetyScore(),
                score.getEnergyScore(),
                score.getIntelligenceScore(),
                score.getComfortScore(),
                score.getPowerScore(),
                score.getReputationScore(),
                score.getPopularityScore(),
                score.getScoreVersion(),
                Timestamp.valueOf(score.getCalculatedTime()));
    }

    private static LocalDateTime readLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
