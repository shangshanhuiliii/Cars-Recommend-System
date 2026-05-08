package com.carsrecommend.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.auth.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:cars_stage11_feedback;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
@Sql(
        scripts = {"/db/schema.sql", "/db/seed-data.sql"},
        config = @SqlConfig(encoding = "UTF-8")
)
class RecommendationFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void submitFeedbackValidatesOwnershipAndOverwritesExistingFeedbackOnly() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO app_user (id, username, password, nickname, phone) VALUES (2, 'feedback_user', 'pwd', 'Feedback', '')");
        insertDemand(301, 1);
        insertDemand(302, 2);
        insertRecommendRecord(401, 1, 301, "SUCCESS");
        insertRecommendRecord(402, 2, 302, "SUCCESS");
        insertRecommendItem(401, 2);

        mockMvc.perform(get("/api/recommend/{recordId}/feedback", 401))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(post("/api/recommend/{recordId}/feedback", 401)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content("""
                                {
                                  "satisfactionScore": 4,
                                  "reasonTags": ["推荐有帮助", "解释清楚"],
                                  "comment": "推荐结果比较符合家用需求"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.recordId").value(401))
                .andExpect(jsonPath("$.data.satisfactionScore").value(4))
                .andExpect(jsonPath("$.data.satisfactionLevel").value("SATISFIED"))
                .andExpect(jsonPath("$.data.reasonTags[0]").value("推荐有帮助"))
                .andExpect(jsonPath("$.data.comment").value("推荐结果比较符合家用需求"));

        assertEquals(1, count("SELECT COUNT(*) FROM recommend_feedback WHERE user_id = 1 AND record_id = 401"));
        assertRecommendationSnapshotUnchanged();

        mockMvc.perform(post("/api/recommend/{recordId}/feedback", 401)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content("""
                                {
                                  "satisfactionScore": 2,
                                  "reasonTags": ["推荐太贵", "车型不合适"],
                                  "comment": "预算不太合适"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.satisfactionScore").value(2))
                .andExpect(jsonPath("$.data.satisfactionLevel").value("DISSATISFIED"))
                .andExpect(jsonPath("$.data.reasonTags[0]").value("推荐太贵"))
                .andExpect(jsonPath("$.data.comment").value("预算不太合适"));

        assertEquals(1, count("SELECT COUNT(*) FROM recommend_feedback WHERE user_id = 1 AND record_id = 401"));
        assertEquals(2, count("SELECT satisfaction_score FROM recommend_feedback WHERE user_id = 1 AND record_id = 401"));
        assertRecommendationSnapshotUnchanged();

        mockMvc.perform(get("/api/recommend/{recordId}/feedback", 401))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.satisfactionScore").value(2))
                .andExpect(jsonPath("$.data.reasonTags[1]").value("车型不合适"));

        mockMvc.perform(post("/api/recommend/{recordId}/feedback", 401)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content("""
                                {
                                  "satisfactionScore": 6,
                                  "reasonTags": [],
                                  "comment": "score invalid"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/recommend/{recordId}/feedback", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content("""
                                {
                                  "satisfactionScore": 3
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(post("/api/recommend/{recordId}/feedback", 402)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content("""
                                {
                                  "satisfactionScore": 3,
                                  "reasonTags": ["推荐有帮助"]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        assertEquals(1, count("SELECT COUNT(*) FROM recommend_feedback"));
        assertRecommendationSnapshotUnchanged();
    }

    private void insertDemand(long id, long userId) {
        jdbcTemplate.update("""
                INSERT INTO user_demand (
                    id, user_id, raw_text, budget_min, budget_max, body_types, energy_types, min_seats,
                    scenes, factor_weights, excluded_brands, excluded_car_ids, profile_text,
                    weight_price, weight_space, weight_safety, weight_energy, weight_intelligence,
                    weight_comfort, weight_power, weight_reputation, weight_popularity
                ) VALUES (?, ?, '', 100000, 150000, '["SUV"]', '["插混"]', 5,
                    '["家庭出行"]', '{"space":8,"safety":8}', '[]', '[]', 'profile',
                    0.1000, 0.2000, 0.2000, 0.1000, 0.1000, 0.1000, 0.0500, 0.1000, 0.0500)
                """, id, userId);
    }

    private void insertRecommendRecord(long id, long userId, long demandId, String status) {
        jdbcTemplate.update("""
                INSERT INTO recommend_record (
                    id, user_id, demand_id, profile_text, weight_snapshot, fallback_message, recommend_status
                ) VALUES (?, ?, ?, 'profile snapshot',
                    '{"price":0.1,"space":0.2,"safety":0.2,"energy":0.1,"intelligence":0.1,"comfort":0.1,"power":0.05,"reputation":0.1,"popularity":0.05}',
                    '', ?)
                """, id, userId, demandId, status);
    }

    private void insertRecommendItem(long recordId, long carId) {
        jdbcTemplate.update("""
                INSERT INTO recommend_item (
                    record_id, car_id, rank_no, total_score, price_score,
                    space_score, safety_score, energy_score, intelligence_score,
                    comfort_score, power_score, reputation_score, popularity_score,
                    tags, match_level, reason_text, weakness_text
                ) VALUES (?, ?, 1, 88.00, 90.00, 85.00, 86.00, 87.00, 80.00,
                    82.00, 78.00, 90.00, 70.00, '["安全配置高"]', 'STRICT',
                    'reason snapshot', 'weakness snapshot')
                """, recordId, carId);
    }

    private void assertRecommendationSnapshotUnchanged() {
        assertEquals(1, count("SELECT COUNT(*) FROM recommend_record WHERE id = 401 AND recommend_status = 'SUCCESS'"));
        assertEquals(1, count("""
                SELECT COUNT(*)
                FROM recommend_item
                WHERE record_id = 401
                  AND total_score = 88.00
                  AND reason_text = 'reason snapshot'
                  AND weakness_text = 'weakness snapshot'
                """));
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
