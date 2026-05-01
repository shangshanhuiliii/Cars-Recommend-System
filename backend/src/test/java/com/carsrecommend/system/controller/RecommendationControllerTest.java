package com.carsrecommend.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cars_stage5;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
@Sql(
        scripts = {"/db/schema.sql", "/db/seed-data.sql"},
        config = @SqlConfig(encoding = "UTF-8")
)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void generateUsesNewDemandModelGroupingAndSnapshotPersistence() throws Exception {
        mockMvc.perform(post("/api/admin/cars/scores/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recalculatedCount").value(20));

        JsonNode broadDemand = postDemand("""
                {
                  "scenes": ["综合需求"],
                  "factorWeights": {}
                }
                """);
        JsonNode broadRecommend = generate(broadDemand.path("id").asLong());
        assertEquals("SUCCESS", broadRecommend.path("recommendStatus").asText());
        assertEquals("已为您找到完全匹配车型", broadRecommend.path("fallbackMessage").asText());
        assertEquals(20, broadRecommend.path("items").size());
        assertAllMatchLevel(broadRecommend.path("items"), "STRICT");
        assertGroupedAndSorted(broadRecommend.path("items"));
        assertRankNoAscending(broadRecommend.path("items"));

        JsonNode familyDemand = postDemand("""
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
                  },
                  "excludedBrands": [],
                  "excludedCarIds": []
                }
                """);
        JsonNode familyRecommend = generate(familyDemand.path("id").asLong());
        assertEquals("FALLBACK", familyRecommend.path("recommendStatus").asText());
        assertTrue(familyRecommend.path("fallbackMessage").asText().contains("完全匹配车型数量不足"));
        assertFalse(familyRecommend.path("fallbackMessage").asText().contains("未找到完全匹配车型"));
        assertTrue(countMatchLevel(familyRecommend.path("items"), "STRICT") > 0);
        assertTrue(containsNonStrictMatchLevel(familyRecommend.path("items")));
        assertGroupedAndSorted(familyRecommend.path("items"));
        assertNoTechnicalTags(familyRecommend.path("items"));
        assertTotalScoreMatchesFormula(familyDemand, familyRecommend.path("items").get(0));
        assertRecordAndItemSnapshotsSaved(familyRecommend, "FALLBACK");
        assertSavedRecommendationItemTexts(familyRecommend);

        JsonNode budgetMinDemand = postDemand("""
                {
                  "budgetMin": 200000,
                  "bodyTypes": ["SUV"],
                  "scenes": ["综合需求"],
                  "factorWeights": { "price": 10 }
                }
                """);
        JsonNode budgetMinRecommend = generate(budgetMinDemand.path("id").asLong());
        assertTrue(containsCarBelowPrice(budgetMinRecommend.path("items"), new BigDecimal("200000")));
        assertTrue(containsBelowBudgetPriceScore(budgetMinRecommend.path("items"), new BigDecimal("200000")));

        JsonNode newEnergyDemand = postDemand("""
                {
                  "budgetMax": 400000,
                  "energyTypes": ["新能源"],
                  "scenes": ["综合需求"],
                  "factorWeights": { "energy": 10 },
                  "excludedBrands": ["比亚迪"],
                  "excludedCarIds": []
                }
                """);
        JsonNode newEnergyRecommend = generate(newEnergyDemand.path("id").asLong());
        assertFalse(newEnergyRecommend.path("items").isEmpty());
        for (JsonNode item : newEnergyRecommend.path("items")) {
            assertFalse("比亚迪".equals(item.path("brand").asText()));
            if ("STRICT".equals(item.path("matchLevel").asText())) {
                assertTrue("纯电".equals(item.path("energyType").asText())
                        || "插混".equals(item.path("energyType").asText())
                        || "增程".equals(item.path("energyType").asText()));
            }
        }

        JsonNode noStrictDemand = postDemand("""
                {
                  "budgetMax": 50000,
                  "bodyTypes": ["MPV"],
                  "energyTypes": ["纯电"],
                  "minSeats": 7,
                  "scenes": ["综合需求"],
                  "factorWeights": { "space": 10 },
                  "excludedBrands": ["别克"],
                  "excludedCarIds": [20]
                }
                """);
        JsonNode noStrictRecommend = generate(noStrictDemand.path("id").asLong());
        assertEquals("FALLBACK", noStrictRecommend.path("recommendStatus").asText());
        assertTrue(noStrictRecommend.path("fallbackMessage").asText().contains("未找到完全匹配车型"));
        assertEquals(0, countMatchLevel(noStrictRecommend.path("items"), "STRICT"));
        assertTrue(containsNonStrictMatchLevel(noStrictRecommend.path("items")));
        for (JsonNode item : noStrictRecommend.path("items")) {
            assertTrue(item.path("seats").asInt() >= 7);
            assertFalse("别克".equals(item.path("brand").asText()));
            assertFalse(20L == item.path("carId").asLong());
        }

        JsonNode emptyDemand = postDemand("""
                {
                  "budgetMax": 50000,
                  "bodyTypes": ["SUV"],
                  "energyTypes": ["纯电"],
                  "minSeats": 9,
                  "scenes": ["综合需求"],
                  "factorWeights": { "space": 10 }
                }
                """);
        JsonNode emptyRecommend = generate(emptyDemand.path("id").asLong());
        assertEquals("EMPTY", emptyRecommend.path("recommendStatus").asText());
        assertEquals("暂未找到合适车型，请调整预算、车型类型或动力类型后重试。",
                emptyRecommend.path("fallbackMessage").asText());
        assertEquals(0, emptyRecommend.path("items").size());
        long emptyRecordId = emptyRecommend.path("recordId").asLong();
        assertEquals(1, count("SELECT COUNT(*) FROM recommend_record WHERE id = ? AND recommend_status = 'EMPTY'",
                emptyRecordId));
        assertEquals(0, count("SELECT COUNT(*) FROM recommend_item WHERE record_id = ?", emptyRecordId));

        long familyRecordId = familyRecommend.path("recordId").asLong();
        jdbcTemplate.update("""
                UPDATE recommend_item
                SET total_score = ?, tags = ?, reason_text = ?, weakness_text = ?
                WHERE record_id = ? AND rank_no = 1
                """, new BigDecimal("12.34"), "[\"snapshot-tag\"]", "snapshot reason",
                "snapshot weakness", familyRecordId);

        JsonNode historyPageOne = getJson("/api/recommend/history?page=1&size=1");
        assertTrue(historyPageOne.path("total").asLong() >= 2);
        assertEquals(1, historyPageOne.path("records").size());
        JsonNode firstHistoryRecord = historyPageOne.path("records").get(0);
        assertTrue(firstHistoryRecord.path("recordId").asLong() > 0);
        assertTrue(firstHistoryRecord.path("topCarNames").isArray());
        assertTrue(firstHistoryRecord.path("itemCount").asLong() >= 0);
        JsonNode fullHistory = getJson("/api/recommend/history?page=1&size=100");
        JsonNode familyHistoryRecord = findHistoryRecord(fullHistory.path("records"), familyRecordId);
        assertTrue(familyHistoryRecord.path("topCarNames").isArray());
        assertTrue(familyHistoryRecord.path("itemCount").asLong() > 0);

        JsonNode detail = getJson("/api/recommend/" + familyRecordId);
        assertEquals(familyRecordId, detail.path("recordId").asLong());
        assertEquals(1L, detail.path("userId").asLong());
        assertEquals(familyDemand.path("id").asLong(), detail.path("demandId").asLong());
        assertTextPresent(detail.path("profileText").asText());
        assertTrue(detail.path("fallbackMessage").asText().contains("完全匹配车型数量不足"));
        assertEquals("FALLBACK", detail.path("recommendStatus").asText());
        assertTrue(detail.path("createTime").isTextual());
        assertTrue(detail.path("weights").path("space").isNumber());
        assertEquals(familyDemand.path("id").asLong(), detail.path("demand").path("id").asLong());
        assertEquals("SUV", detail.path("demand").path("bodyTypes").get(0).asText());
        assertEquals("插混", detail.path("demand").path("energyTypes").get(0).asText());
        assertEquals(5, detail.path("demand").path("minSeats").asInt());
        JsonNode detailItem = detail.path("items").get(0);
        assertEquals(1, detailItem.path("rankNo").asInt());
        assertEquals(0, new BigDecimal("12.34").compareTo(detailItem.path("totalScore").decimalValue()));
        assertScoreFieldsPresent(detailItem);
        assertEquals("snapshot-tag", detailItem.path("tags").get(0).asText());
        assertEquals("snapshot reason", detailItem.path("reasonText").asText());
        assertEquals("snapshot weakness", detailItem.path("weaknessText").asText());
        assertEquals("STRICT", detailItem.path("matchLevel").asText());
        assertRankNoAscending(detail.path("items"));

        jdbcTemplate.update(
                "INSERT INTO app_user (id, username, password, nickname, phone) VALUES (2, 'other_user', 'pwd', 'Other', '')");
        JsonNode otherUserHistory = getJson("/api/recommend/history?userId=2&page=1&size=10");
        assertEquals(0, otherUserHistory.path("total").asLong());
        assertEquals(0, otherUserHistory.path("records").size());
        mockMvc.perform(get("/api/recommend/{recordId}?userId=2", familyRecordId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        assertThrows(BadSqlGrammarException.class,
                () -> jdbcTemplate.queryForObject("SELECT price_score FROM car_feature_score", BigDecimal.class));
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

    private JsonNode generate(long demandId) throws Exception {
        String payload = """
                {
                  "demandId": %d
                }
                """.formatted(demandId);
        MvcResult result = mockMvc.perform(post("/api/recommend/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.recordId").isNumber())
                .andExpect(jsonPath("$.data.userId").value(1))
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

    private void assertRecordAndItemSnapshotsSaved(JsonNode recommend, String recommendStatus) {
        long recordId = recommend.path("recordId").asLong();
        assertEquals(1, count("SELECT COUNT(*) FROM recommend_record WHERE id = ? AND recommend_status = ?",
                recordId, recommendStatus));
        assertEquals(recommend.path("items").size(), count("""
                SELECT COUNT(*) FROM recommend_item
                WHERE record_id = ?
                  AND total_score IS NOT NULL
                  AND price_score IS NOT NULL
                  AND space_score IS NOT NULL
                  AND safety_score IS NOT NULL
                  AND energy_score IS NOT NULL
                  AND intelligence_score IS NOT NULL
                  AND comfort_score IS NOT NULL
                  AND power_score IS NOT NULL
                  AND reputation_score IS NOT NULL
                  AND popularity_score IS NOT NULL
                  AND match_level IS NOT NULL
                  AND reason_text IS NOT NULL
                  AND reason_text <> ''
                  AND weakness_text IS NOT NULL
                  AND weakness_text <> ''
                  AND tags IS NOT NULL
                  AND tags <> ''
                """, recordId));
    }

    private void assertSavedRecommendationItemTexts(JsonNode recommend) {
        long recordId = recommend.path("recordId").asLong();
        int itemCount = recommend.path("items").size();
        assertEquals(itemCount, count("""
                SELECT COUNT(*) FROM recommend_item
                WHERE record_id = ?
                  AND match_level IS NOT NULL
                  AND match_level <> ''
                  AND reason_text IS NOT NULL
                  AND reason_text <> ''
                  AND weakness_text IS NOT NULL
                  AND weakness_text <> ''
                """, recordId));
    }

    private void assertTotalScoreMatchesFormula(JsonNode demand, JsonNode item) {
        JsonNode weights = demand.path("weights");
        BigDecimal expected = BigDecimal.ZERO
                .add(item.path("priceScore").decimalValue().multiply(weights.path("price").decimalValue()))
                .add(item.path("spaceScore").decimalValue().multiply(weights.path("space").decimalValue()))
                .add(item.path("safetyScore").decimalValue().multiply(weights.path("safety").decimalValue()))
                .add(item.path("energyScore").decimalValue().multiply(weights.path("energy").decimalValue()))
                .add(item.path("intelligenceScore").decimalValue().multiply(weights.path("intelligence").decimalValue()))
                .add(item.path("comfortScore").decimalValue().multiply(weights.path("comfort").decimalValue()))
                .add(item.path("powerScore").decimalValue().multiply(weights.path("power").decimalValue()))
                .add(item.path("reputationScore").decimalValue().multiply(weights.path("reputation").decimalValue()))
                .add(item.path("popularityScore").decimalValue().multiply(weights.path("popularity").decimalValue()))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(item.path("totalScore").decimalValue()));
    }

    private void assertGroupedAndSorted(JsonNode items) {
        boolean seenNonStrict = false;
        for (JsonNode item : items) {
            if ("STRICT".equals(item.path("matchLevel").asText())) {
                assertFalse(seenNonStrict, "STRICT item must stay before recommendation group");
            } else {
                seenNonStrict = true;
            }
        }
        assertSorted(filterByStrict(items, true));
        assertSorted(filterByStrict(items, false));
    }

    private JsonNode filterByStrict(JsonNode items, boolean strict) {
        return objectMapper.valueToTree(stream(items).stream()
                .filter(item -> strict == "STRICT".equals(item.path("matchLevel").asText()))
                .toList());
    }

    private java.util.List<JsonNode> stream(JsonNode items) {
        java.util.List<JsonNode> result = new java.util.ArrayList<>();
        for (JsonNode item : items) {
            result.add(item);
        }
        return result;
    }

    private void assertSorted(JsonNode items) {
        for (int i = 1; i < items.size(); i++) {
            JsonNode previous = items.get(i - 1);
            JsonNode current = items.get(i);
            int totalCompare = previous.path("totalScore").decimalValue()
                    .compareTo(current.path("totalScore").decimalValue());
            assertTrue(totalCompare >= 0);
            if (totalCompare == 0) {
                int reputationCompare = previous.path("reputationScore").decimalValue()
                        .compareTo(current.path("reputationScore").decimalValue());
                assertTrue(reputationCompare >= 0);
                if (reputationCompare == 0) {
                    assertTrue(previous.path("popularityScore").decimalValue()
                            .compareTo(current.path("popularityScore").decimalValue()) >= 0);
                }
            }
        }
    }

    private void assertAllMatchLevel(JsonNode items, String matchLevel) {
        for (JsonNode item : items) {
            assertEquals(matchLevel, item.path("matchLevel").asText());
        }
    }

    private void assertTextPresent(String value) {
        assertTrue(value != null && !value.isBlank());
    }

    private void assertScoreFieldsPresent(JsonNode item) {
        assertTrue(item.path("priceScore").isNumber());
        assertTrue(item.path("spaceScore").isNumber());
        assertTrue(item.path("safetyScore").isNumber());
        assertTrue(item.path("energyScore").isNumber());
        assertTrue(item.path("intelligenceScore").isNumber());
        assertTrue(item.path("comfortScore").isNumber());
        assertTrue(item.path("powerScore").isNumber());
        assertTrue(item.path("reputationScore").isNumber());
        assertTrue(item.path("popularityScore").isNumber());
    }

    private void assertRankNoAscending(JsonNode items) {
        for (int i = 0; i < items.size(); i++) {
            assertEquals(i + 1, items.get(i).path("rankNo").asInt());
        }
    }

    private JsonNode findHistoryRecord(JsonNode records, long recordId) {
        for (JsonNode record : records) {
            if (record.path("recordId").asLong() == recordId) {
                return record;
            }
        }
        throw new AssertionError("history record not found: " + recordId);
    }

    private void assertNoTechnicalTags(JsonNode items) {
        for (JsonNode item : items) {
            assertFalse(containsTag(item, "完全匹配"));
            assertFalse(containsTag(item, "降级推荐"));
            assertFalse(containsTag(item, "放宽预算"));
            assertFalse(containsTag(item, "放宽车型"));
            assertFalse(containsTag(item, "放宽动力"));
            assertFalse(containsTag(item, "相似推荐"));
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

    private int countMatchLevel(JsonNode items, String matchLevel) {
        int total = 0;
        for (JsonNode item : items) {
            if (matchLevel.equals(item.path("matchLevel").asText())) {
                total++;
            }
        }
        return total;
    }

    private boolean containsNonStrictMatchLevel(JsonNode items) {
        for (JsonNode item : items) {
            if (!"STRICT".equals(item.path("matchLevel").asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsCarBelowPrice(JsonNode items, BigDecimal price) {
        for (JsonNode item : items) {
            if (item.path("guidePrice").decimalValue().compareTo(price) < 0) {
                return true;
            }
        }
        return false;
    }

    private boolean containsBelowBudgetPriceScore(JsonNode items, BigDecimal budgetMin) {
        for (JsonNode item : items) {
            if (item.path("guidePrice").decimalValue().compareTo(budgetMin) < 0
                    && item.path("priceScore").decimalValue().compareTo(new BigDecimal("90")) < 0) {
                return true;
            }
        }
        return false;
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
