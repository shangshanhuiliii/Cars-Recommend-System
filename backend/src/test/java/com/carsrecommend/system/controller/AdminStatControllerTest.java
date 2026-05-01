package com.carsrecommend.system.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
        "spring.datasource.url=jdbc:h2:mem:cars_stage9_stat;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
@Sql(
        scripts = {"/db/schema.sql", "/db/seed-data.sql"},
        config = @SqlConfig(encoding = "UTF-8")
)
class AdminStatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void overviewReturnsNameValueStatisticsFromRealTables() throws Exception {
        mockMvc.perform(get("/api/admin/stat/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.budgetDistribution.length()").value(0))
                .andExpect(jsonPath("$.data.sceneDistribution.length()").value(0))
                .andExpect(jsonPath("$.data.popularCars.length()").value(0));

        insertDemand(101, new BigDecimal("100000"), new BigDecimal("150000"),
                "[\"SUV\"]", "[\"插混\"]", "[\"家庭出行\"]",
                "{\"space\":8,\"safety\":8}", "家庭画像");
        insertDemand(102, new BigDecimal("250000"), new BigDecimal("320000"),
                "[\"MPV\"]", "[\"纯电\"]", "[\"商务接待\"]",
                "{\"safety\":8,\"comfort\":7,\"reputation\":6}", "商务画像");
        insertRecommendRecord(201, 101, "FALLBACK");
        insertRecommendRecord(202, 102, "SUCCESS");
        insertRecommendItem(201, 2, 1);
        insertRecommendItem(201, 8, 2);
        insertRecommendItem(202, 2, 1);

        mockMvc.perform(get("/api/admin/stat/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.budgetDistribution[?(@.name=='10-15万')].value", contains(1)))
                .andExpect(jsonPath("$.data.budgetDistribution[?(@.name=='25万以上')].value", contains(1)))
                .andExpect(jsonPath("$.data.sceneDistribution[?(@.name=='家庭出行')].value", contains(1)))
                .andExpect(jsonPath("$.data.sceneDistribution[?(@.name=='商务接待')].value", contains(1)))
                .andExpect(jsonPath("$.data.focusFactorDistribution[?(@.name=='安全')].value", contains(2)))
                .andExpect(jsonPath("$.data.focusFactorDistribution[?(@.name=='空间')].value", contains(1)))
                .andExpect(jsonPath("$.data.popularCars[0].name").value("比亚迪 宋PLUS DM-i 110KM 旗舰型"))
                .andExpect(jsonPath("$.data.popularCars[0].value").value(2))
                .andExpect(jsonPath("$.data.recommendStatusDistribution[?(@.name=='FALLBACK')].value", contains(1)))
                .andExpect(jsonPath("$.data.recommendStatusDistribution[?(@.name=='SUCCESS')].value", contains(1)))
                .andExpect(jsonPath("$.data.energyTypeDistribution[?(@.name=='插混')].value", contains(1)))
                .andExpect(jsonPath("$.data.energyTypeDistribution[?(@.name=='纯电')].value", contains(1)))
                .andExpect(jsonPath("$.data.bodyTypeDistribution[?(@.name=='SUV')].value", contains(1)))
                .andExpect(jsonPath("$.data.bodyTypeDistribution[?(@.name=='MPV')].value", contains(1)))
                .andExpect(jsonPath("$.data.satisfactionDistribution.length()").value(0))
                .andExpect(jsonPath("$.data.feedbackReasonDistribution.length()").value(0));
    }

    private void insertDemand(
            long id,
            BigDecimal budgetMin,
            BigDecimal budgetMax,
            String bodyTypes,
            String energyTypes,
            String scenes,
            String factorWeights,
            String profileText) {
        jdbcTemplate.update("""
                INSERT INTO user_demand (
                    id, user_id, raw_text, budget_min, budget_max, body_types, energy_types, min_seats,
                    scenes, factor_weights, excluded_brands, excluded_car_ids, profile_text,
                    weight_price, weight_space, weight_safety, weight_energy, weight_intelligence,
                    weight_comfort, weight_power, weight_reputation, weight_popularity
                ) VALUES (?, 1, '', ?, ?, ?, ?, 5, ?, ?, '[]', '[]', ?,
                    0.1000, 0.2000, 0.2000, 0.1000, 0.1000, 0.1000, 0.0500, 0.1000, 0.0500)
                """, id, budgetMin, budgetMax, bodyTypes, energyTypes, scenes, factorWeights, profileText);
    }

    private void insertRecommendRecord(long id, long demandId, String status) {
        jdbcTemplate.update("""
                INSERT INTO recommend_record (
                    id, user_id, demand_id, profile_text, weight_snapshot, fallback_message, recommend_status
                ) VALUES (?, 1, ?, 'profile snapshot',
                    '{"price":0.1,"space":0.2,"safety":0.2,"energy":0.1,"intelligence":0.1,"comfort":0.1,"power":0.05,"reputation":0.1,"popularity":0.05}',
                    '', ?)
                """, id, demandId, status);
    }

    private void insertRecommendItem(long recordId, long carId, int rankNo) {
        jdbcTemplate.update("""
                INSERT INTO recommend_item (
                    record_id, car_id, rank_no, total_score, price_score,
                    space_score, safety_score, energy_score, intelligence_score,
                    comfort_score, power_score, reputation_score, popularity_score,
                    tags, match_level, reason_text, weakness_text
                ) VALUES (?, ?, ?, 88.00, 90.00, 85.00, 86.00, 87.00, 80.00,
                    82.00, 78.00, 90.00, 70.00, '["安全配置高"]', 'STRICT',
                    'reason snapshot', 'weakness snapshot')
                """, recordId, carId, rankNo);
    }
}
