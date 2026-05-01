package com.carsrecommend.system.mapper;

import com.carsrecommend.system.entity.RecommendItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class RecommendItemMapper {

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
}
