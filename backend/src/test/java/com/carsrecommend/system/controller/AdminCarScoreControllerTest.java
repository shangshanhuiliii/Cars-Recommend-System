package com.carsrecommend.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.auth.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:cars_stage3;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
@Sql(
        scripts = {"/db/schema.sql", "/db/seed-data.sql"},
        config = @SqlConfig(encoding = "UTF-8")
)
class AdminCarScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void scoreEndpointsCalculatePersistAndReuseFeatureScores() throws Exception {
        mockMvc.perform(post("/api/admin/cars/{id}/score/recalculate", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.carId").value(2))
                .andExpect(jsonPath("$.data.spaceScore").value(83.00))
                .andExpect(jsonPath("$.data.safetyScore").value(100.00))
                .andExpect(jsonPath("$.data.energyScore").value(95.00))
                .andExpect(jsonPath("$.data.intelligenceScore").value(70.00))
                .andExpect(jsonPath("$.data.powerScore").value(80.00))
                .andExpect(jsonPath("$.data.reputationScore").value(92.00))
                .andExpect(jsonPath("$.data.popularityScore").value(80.00))
                .andExpect(jsonPath("$.data.comfortScore").value(83.10))
                .andExpect(jsonPath("$.data.scoreVersion").value("feature-score-v1"));

        mockMvc.perform(get("/api/admin/cars/{id}/score", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carId").value(2))
                .andExpect(jsonPath("$.data.comfortScore").value(83.10));

        mockMvc.perform(post("/api/admin/cars/{id}/score/recalculate", 2))
                .andExpect(status().isOk());
        assertEquals(1, count("SELECT COUNT(*) FROM car_feature_score WHERE car_id = 2"));

        jdbcTemplate.update("""
                INSERT INTO car_model (
                    id, brand, series, model_name, guide_price, body_type, energy_type,
                    seats, launch_year, image_url, sales_volume, user_rating, audit_status
                ) VALUES (1001, 'Test', 'MissingParam', 'Missing Param SUV', 120000, 'SUV', '纯电',
                    5, 2026, '', 1000, 4.0, 'APPROVED')
                """);

        mockMvc.perform(post("/api/admin/cars/{id}/score/recalculate", 1001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carId").value(1001))
                .andExpect(jsonPath("$.data.spaceScore").value(63.00))
                .andExpect(jsonPath("$.data.safetyScore").value(30.00))
                .andExpect(jsonPath("$.data.energyScore").value(60.00))
                .andExpect(jsonPath("$.data.intelligenceScore").value(50.00))
                .andExpect(jsonPath("$.data.powerScore").value(60.00));

        mockMvc.perform(post("/api/admin/cars/scores/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recalculatedCount").value(121))
                .andExpect(jsonPath("$.data.records.length()").value(121));
        assertEquals(121, count("SELECT COUNT(*) FROM car_feature_score"));

        BigDecimal topPopularity = jdbcTemplate.queryForObject(
                "SELECT popularity_score FROM car_feature_score WHERE car_id = 1",
                BigDecimal.class);
        assertEquals(0, new BigDecimal("100.00").compareTo(topPopularity));

        mockMvc.perform(post("/api/admin/cars/{id}/score/recalculate", 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        assertThrows(BadSqlGrammarException.class,
                () -> jdbcTemplate.queryForObject("SELECT price_score FROM car_feature_score", BigDecimal.class));
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
