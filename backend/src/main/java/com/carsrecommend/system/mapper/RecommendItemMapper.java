package com.carsrecommend.system.mapper;

import com.carsrecommend.system.entity.RecommendItem;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class RecommendItemMapper {

    private static final RowMapper<RecommendItemSnapshot> SNAPSHOT_ROW_MAPPER = (resultSet, rowNum) ->
            new RecommendItemSnapshot(
                    resultSet.getInt("rank_no"),
                    resultSet.getLong("car_id"),
                    resultSet.getString("brand"),
                    resultSet.getString("series"),
                    resultSet.getString("model_name"),
                    resultSet.getBigDecimal("guide_price"),
                    resultSet.getString("body_type"),
                    resultSet.getString("energy_type"),
                    resultSet.getInt("seats"),
                    resultSet.getString("image_url"),
                    resultSet.getBigDecimal("total_score"),
                    resultSet.getBigDecimal("price_score"),
                    resultSet.getBigDecimal("space_score"),
                    resultSet.getBigDecimal("safety_score"),
                    resultSet.getBigDecimal("energy_score"),
                    resultSet.getBigDecimal("intelligence_score"),
                    resultSet.getBigDecimal("comfort_score"),
                    resultSet.getBigDecimal("power_score"),
                    resultSet.getBigDecimal("reputation_score"),
                    resultSet.getBigDecimal("popularity_score"),
                    resultSet.getString("tags"),
                    resultSet.getString("match_level"),
                    resultSet.getString("reason_text"),
                    resultSet.getString("weakness_text"));

    private final JdbcTemplate jdbcTemplate;

    public RecommendItemMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insert(RecommendItem item) {
        return jdbcTemplate.update(
                """
                        INSERT INTO recommend_item (
                            record_id, car_id, rank_no, total_score, price_score,
                            space_score, safety_score, energy_score, intelligence_score,
                            comfort_score, power_score, reputation_score, popularity_score,
                            tags, match_level, reason_text, weakness_text
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                item.getRecordId(),
                item.getCarId(),
                item.getRankNo(),
                item.getTotalScore(),
                item.getPriceScore(),
                item.getSpaceScore(),
                item.getSafetyScore(),
                item.getEnergyScore(),
                item.getIntelligenceScore(),
                item.getComfortScore(),
                item.getPowerScore(),
                item.getReputationScore(),
                item.getPopularityScore(),
                item.getTags(),
                item.getMatchLevel(),
                item.getReasonText(),
                item.getWeaknessText());
    }

    public long countByRecordId(Long recordId) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommend_item WHERE record_id = ? AND deleted = FALSE",
                Long.class,
                recordId);
        return total == null ? 0 : total;
    }

    public List<String> findTopCarNamesByRecordId(Long recordId, int limit) {
        return jdbcTemplate.queryForList(
                """
                        SELECT cm.model_name
                        FROM recommend_item ri
                        JOIN car_model cm ON cm.id = ri.car_id
                        WHERE ri.record_id = ? AND ri.deleted = FALSE
                        ORDER BY ri.rank_no ASC
                        LIMIT ?
                        """,
                String.class,
                recordId,
                limit);
    }

    public List<RecommendItemSnapshot> findSnapshotsByRecordId(Long recordId) {
        return jdbcTemplate.query(
                """
                        SELECT
                            ri.rank_no, ri.car_id,
                            cm.brand, cm.series, cm.model_name, cm.guide_price,
                            cm.body_type, cm.energy_type, cm.seats, cm.image_url,
                            ri.total_score, ri.price_score,
                            ri.space_score, ri.safety_score, ri.energy_score, ri.intelligence_score,
                            ri.comfort_score, ri.power_score, ri.reputation_score, ri.popularity_score,
                            ri.tags, ri.match_level, ri.reason_text, ri.weakness_text
                        FROM recommend_item ri
                        JOIN car_model cm ON cm.id = ri.car_id
                        WHERE ri.record_id = ? AND ri.deleted = FALSE
                        ORDER BY ri.rank_no ASC
                        """,
                SNAPSHOT_ROW_MAPPER,
                recordId);
    }
}
