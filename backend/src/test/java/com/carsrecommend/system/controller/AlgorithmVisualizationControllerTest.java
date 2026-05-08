package com.carsrecommend.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.auth.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:cars_algorithm_visualization;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
@Sql(
        scripts = {"/db/schema.sql", "/db/seed-data.sql"},
        config = @SqlConfig(encoding = "UTF-8")
)
class AlgorithmVisualizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void returnsReadonlyVisualizationFromRecommendationSnapshot() throws Exception {
        mockMvc.perform(post("/api/admin/cars/scores/recalculate"))
                .andExpect(status().isOk());

        JsonNode demand = postDemand("""
                {
                  "budgetMin": 100000,
                  "budgetMax": 150000,
                  "bodyTypes": ["SUV"],
                  "energyTypes": ["插混"],
                  "minSeats": 5,
                  "scenes": ["家庭出行"],
                  "factorWeights": {
                    "space": 8,
                    "safety": 8,
                    "comfort": 6
                  }
                }
                """);
        JsonNode recommend = createRecommendation(demand.path("id").asLong());
        long recordId = recommend.path("recordId").asLong();
        long recordCountBefore = count("SELECT COUNT(*) FROM recommend_record");
        long itemCountBefore = count("SELECT COUNT(*) FROM recommend_item");

        JsonNode visualization = getJson("/api/recommend/" + recordId + "/algorithm-visualization");

        assertEquals(recordId, visualization.path("recordId").asLong());
        assertEquals(demand.path("id").asLong(), visualization.path("demandId").asLong());
        assertEquals("pareto-topsis-v1", visualization.path("algorithmVersion").asText());
        assertEquals(0, new BigDecimal("0.75").compareTo(visualization.path("alpha").decimalValue()));
        assertTrue(visualization.path("weights").path("subjectiveWeight").path("space").isNumber());
        assertTrue(visualization.path("weights").path("objectiveWeight").path("space").isNumber());
        assertTrue(visualization.path("weights").path("finalWeight").path("space").isNumber());
        assertEquals("SUV", visualization.path("demand").path("bodyTypes").get(0).asText());
        assertEquals("插混", visualization.path("demand").path("energyTypes").get(0).asText());
        assertEquals(5, visualization.path("demand").path("minSeats").asInt());
        assertTrue(visualization.path("constraints").size() >= 6);
        assertEquals(15, visualization.path("pipeline").size());
        assertPipelineDetailed(visualization.path("pipeline"));
        assertEquals(5, visualization.path("stageStats").size());
        assertTrue(visualization.path("featureScoreRules").size() >= 9);
        assertTrue(visualization.path("snapshotNote").asText().contains("不会重新生成推荐"));

        JsonNode items = visualization.path("items");
        assertFalse(items.isEmpty());
        assertRankNoAscending(items);
        JsonNode firstItem = items.get(0);
        assertTrue(firstItem.path("rankNo").isInt());
        assertTrue(firstItem.path("totalScore").isNumber());
        assertTrue(firstItem.path("scores").path("price").isNumber());
        assertTrue(firstItem.path("scores").path("space").isNumber());
        assertTrue(firstItem.path("reasonText").asText().length() > 0);
        assertTrue(firstItem.path("weaknessText").asText().length() > 0);
        assertTrue(firstItem.path("paretoDominated").isBoolean());
        assertTrue(firstItem.path("topsis").path("closeness").decimalValue().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(firstItem.path("topsis").path("closeness").decimalValue().compareTo(BigDecimal.ONE) <= 0);
        assertTrue(firstItem.path("topsis").path("positiveDistance").decimalValue().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(firstItem.path("topsis").path("negativeDistance").decimalValue().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(firstItem.path("contribution").path("space").isNumber());
        assertTrue(firstItem.path("gap").path("space").isNumber());
        JsonNode featureScoreExample = visualization.path("featureScoreExample");
        assertEquals(firstItem.path("carId").asLong(), featureScoreExample.path("carId").asLong());
        assertTrue(featureScoreExample.path("brand").asText().length() > 0);
        assertTrue(featureScoreExample.path("params").has("wheelbaseMm"));
        assertTrue(featureScoreExample.path("scores").path("space").isNumber());
        assertTrue(featureScoreExample.path("scores").path("safety").isNumber());
        assertTrue(featureScoreExample.path("scoreBreakdown").isArray());
        assertScoreBreakdownContains(featureScoreExample.path("scoreBreakdown"), "space");
        assertScoreBreakdownContains(featureScoreExample.path("scoreBreakdown"), "safety");
        assertScoreBreakdownContains(featureScoreExample.path("scoreBreakdown"), "energy");
        assertScoreBreakdownContains(featureScoreExample.path("scoreBreakdown"), "comfort");
        assertNoTechnicalTags(items);
        assertEquals(recordCountBefore, count("SELECT COUNT(*) FROM recommend_record"));
        assertEquals(itemCountBefore, count("SELECT COUNT(*) FROM recommend_item"));

        jdbcTemplate.update(
                "UPDATE recommend_record SET weight_snapshot = ? WHERE id = ?",
                objectMapper.writeValueAsString(demand.path("weights")),
                recordId);
        JsonNode legacyVisualization = getJson("/api/recommend/" + recordId + "/algorithm-visualization");
        assertEquals("weighted-sum-v1", legacyVisualization.path("algorithmVersion").asText());
        assertTrue(legacyVisualization.path("compatibilityNote").asText().contains("旧推荐记录"));
        assertEquals(0, demand.path("weights").path("space").decimalValue()
                .compareTo(legacyVisualization.path("weights").path("finalWeight").path("space").decimalValue()));
        assertEquals(recordCountBefore, count("SELECT COUNT(*) FROM recommend_record"));
        assertEquals(itemCountBefore, count("SELECT COUNT(*) FROM recommend_item"));

        mockMvc.perform(get("/api/recommend/999999/algorithm-visualization"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    private JsonNode postDemand(String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/demand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }

    private JsonNode createRecommendation(long demandId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recommend/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content("""
                                {
                                  "demandId": %d
                                }
                                """.formatted(demandId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }

    private JsonNode getJson(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }

    private void assertRankNoAscending(JsonNode items) {
        int previous = 0;
        for (JsonNode item : items) {
            int rankNo = item.path("rankNo").asInt();
            assertTrue(rankNo > previous);
            previous = rankNo;
        }
    }

    private void assertPipelineDetailed(JsonNode pipeline) {
        for (JsonNode step : pipeline) {
            assertTrue(step.path("description").asText().length() > 0);
            assertTrue(step.path("inputSummary").asText().length() > 0);
            assertTrue(step.path("outputSummary").asText().length() > 0);
            assertTrue(step.path("recordResult").asText().length() > 0);
            assertTrue(step.path("codeModule").asText().length() > 0);
        }
    }

    private void assertScoreBreakdownContains(JsonNode breakdowns, String dimension) {
        for (JsonNode breakdown : breakdowns) {
            if (dimension.equals(breakdown.path("dimension").asText())) {
                assertTrue(breakdown.path("finalScore").isNumber());
                assertTrue(breakdown.path("formulaText").asText().length() > 0);
                assertTrue(breakdown.path("matchedRules").isArray());
                assertFalse(breakdown.path("matchedRules").isEmpty());
                assertTrue(breakdown.path("matchedRules").get(0).path("delta").isNumber());
                assertTrue(breakdown.path("explanation").asText().length() > 0);
                return;
            }
        }
        throw new AssertionError("missing score breakdown: " + dimension);
    }

    private void assertNoTechnicalTags(JsonNode items) {
        for (JsonNode item : items) {
            assertFalse(containsTag(item, "完全匹配"));
            assertFalse(containsTag(item, "降级推荐"));
            assertFalse(containsTag(item, "放宽预算"));
            assertFalse(containsTag(item, "放宽车型"));
            assertFalse(containsTag(item, "放宽动力"));
            assertFalse(containsTag(item, "相似推荐"));
            assertFalse(containsTag(item, "STRICT"));
            assertFalse(containsTag(item, "RELAX_BUDGET"));
            assertFalse(containsTag(item, "RELAX_BODY_TYPE"));
            assertFalse(containsTag(item, "RELAX_ENERGY_TYPE"));
            assertFalse(containsTag(item, "SIMILAR_RECOMMEND"));
            assertFalse(containsTag(item, "TOPSIS"));
            assertFalse(containsTag(item, "Pareto"));
        }
    }

    private boolean containsTag(JsonNode item, String tag) {
        for (JsonNode value : item.path("tags")) {
            if (tag.equals(value.asText())) {
                return true;
            }
        }
        return false;
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
