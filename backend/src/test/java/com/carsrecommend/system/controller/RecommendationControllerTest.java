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
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
    void generateUsesRealScoresFallbackExplanationsAndPersistsTraceRecords() throws Exception {
        mockMvc.perform(post("/api/admin/cars/scores/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recalculatedCount").value(20));

        JsonNode familyDemand = postDemand("""
                {
                  "budgetMin": 100000,
                  "budgetMax": 150000,
                  "bodyType": "SUV",
                  "energyType": "插混",
                  "seats": 5,
                  "scene": "家庭出行",
                  "focusFactors": ["空间", "安全"],
                  "excludedBrands": [],
                  "excludedCarIds": []
                }
                """);
        JsonNode familyRecommend = generate(familyDemand.path("id").asLong(), 1);
        assertEquals("SUCCESS", familyRecommend.path("recommendStatus").asText());
        assertEquals("已为您找到完全匹配车型", familyRecommend.path("fallbackMessage").asText());
        assertEquals(1, familyRecommend.path("items").size());
        JsonNode familyItem = familyRecommend.path("items").get(0);
        assertEquals(8L, familyItem.path("carId").asLong());
        assertEquals("STRICT", familyItem.path("matchLevel").asText());
        assertNoTag(familyItem, "完全匹配");
        assertEquals("SUV", familyItem.path("bodyType").asText());
        assertEquals("插混", familyItem.path("energyType").asText());
        assertTrue(familyItem.path("guidePrice").decimalValue().compareTo(new BigDecimal("150000")) <= 0);
        assertEquals(0, count("SELECT COUNT(*) FROM recommend_item WHERE record_id = ? AND car_id = 2",
                familyRecommend.path("recordId").asLong()));
        assertTotalScoreMatchesFormula(familyDemand, familyItem);
        assertTextPresent(familyItem.path("reasonText").asText());
        assertTextPresent(familyItem.path("weaknessText").asText());
        assertRecordAndItemSnapshotsSaved(familyRecommend, familyItem, "SUCCESS");

        JsonNode partialFallbackRecommend = generate(familyDemand.path("id").asLong(), 5);
        assertEquals("FALLBACK", partialFallbackRecommend.path("recommendStatus").asText());
        assertTrue(partialFallbackRecommend.path("fallbackMessage").asText()
                .contains("完全匹配车型数量不足"));
        assertFalse(partialFallbackRecommend.path("fallbackMessage").asText()
                .contains("未找到完全匹配车型"));
        assertTrue(countMatchLevel(partialFallbackRecommend.path("items"), "STRICT") > 0);
        assertTrue(containsNonStrictMatchLevel(partialFallbackRecommend.path("items")));
        assertNoTag(partialFallbackRecommend.path("items"), "完全匹配");

        JsonNode cityDemand = postDemand("""
                {
                  "budgetMin": 80000,
                  "budgetMax": 120000,
                  "scene": "城市通勤",
                  "focusFactors": ["价格", "能耗"],
                  "excludedBrands": [],
                  "excludedCarIds": []
                }
                """);
        JsonNode cityRecommend = generate(cityDemand.path("id").asLong(), 5);
        assertEquals("SUCCESS", cityRecommend.path("recommendStatus").asText());
        assertTrue(cityRecommend.path("items").size() > 1);
        assertEquals(1L, cityRecommend.path("items").get(0).path("carId").asLong());
        assertSorted(cityRecommend.path("items"));
        assertTotalScoreMatchesFormula(cityDemand, cityRecommend.path("items").get(0));
        assertTrue(containsTag(cityRecommend.path("items").get(0), "价格匹配度高"));
        assertTrue(cityRecommend.path("items").get(0).path("priceScore").decimalValue()
                .compareTo(new BigDecimal("90")) >= 0);
        assertTrue(cityRecommend.path("items").get(0).path("energyScore").decimalValue()
                .compareTo(new BigDecimal("85")) >= 0);
        assertTrue(cityRecommend.path("items").get(0).path("tags").size() >= 2);
        assertTrue(cityRecommend.path("items").get(0).path("tags").size() <= 3);
        assertAllMatchLevel(cityRecommend.path("items"), "STRICT");

        JsonNode minOnlyDemand = postDemand("""
                {
                  "budgetMin": 200000,
                  "bodyType": "SUV",
                  "scene": "综合需求",
                  "focusFactors": ["价格"],
                  "excludedBrands": [],
                  "excludedCarIds": []
                }
                """);
        JsonNode minOnlyRecommend = generate(minOnlyDemand.path("id").asLong(), 20);
        assertTrue(containsCarBelowPrice(minOnlyRecommend.path("items"), new BigDecimal("200000")));
        assertTrue(containsBelowBudgetPriceScore(minOnlyRecommend.path("items"), new BigDecimal("200000")));

        JsonNode newEnergyDemand = postDemand("""
                {
                  "budgetMax": 400000,
                  "energyType": "新能源",
                  "scene": "综合需求",
                  "focusFactors": ["能耗"],
                  "excludedBrands": ["比亚迪"],
                  "excludedCarIds": []
                }
                """);
        JsonNode newEnergyRecommend = generate(newEnergyDemand.path("id").asLong(), 20);
        assertFalse(newEnergyRecommend.path("items").isEmpty());
        for (JsonNode item : newEnergyRecommend.path("items")) {
            assertTrue("纯电".equals(item.path("energyType").asText())
                    || "插混".equals(item.path("energyType").asText())
                    || "增程".equals(item.path("energyType").asText()));
            assertFalse("比亚迪".equals(item.path("brand").asText()));
        }

        JsonNode topKThresholdDemand = postDemand("""
                {
                  "budgetMax": 140000,
                  "bodyType": "SUV",
                  "scene": "综合需求",
                  "focusFactors": ["空间"],
                  "excludedBrands": [],
                  "excludedCarIds": []
                }
                """);
        JsonNode topKThresholdRecommend = generate(topKThresholdDemand.path("id").asLong(), 3);
        assertEquals("SUCCESS", topKThresholdRecommend.path("recommendStatus").asText());
        assertEquals(3, topKThresholdRecommend.path("items").size());
        assertAllMatchLevel(topKThresholdRecommend.path("items"), "STRICT");

        JsonNode budgetRelaxDemand = postDemand("""
                {
                  "budgetMax": 150000,
                  "bodyType": "SUV",
                  "energyType": "插混",
                  "seats": 5,
                  "scene": "家庭出行",
                  "focusFactors": ["空间", "安全"],
                  "excludedBrands": [],
                  "excludedCarIds": [8]
                }
                """);
        JsonNode budgetRelaxRecommend = generate(budgetRelaxDemand.path("id").asLong(), 1);
        assertEquals("FALLBACK", budgetRelaxRecommend.path("recommendStatus").asText());
        assertTrue(budgetRelaxRecommend.path("fallbackMessage").asText()
                .contains("未找到完全匹配车型"));
        assertEquals(0, countMatchLevel(budgetRelaxRecommend.path("items"), "STRICT"));
        assertEquals("RELAX_BUDGET", budgetRelaxRecommend.path("items").get(0).path("matchLevel").asText());
        assertEquals(2L, budgetRelaxRecommend.path("items").get(0).path("carId").asLong());

        JsonNode bodyRelaxDemand = postDemand("""
                {
                  "budgetMax": 200000,
                  "bodyType": "SUV",
                  "energyType": "燃油",
                  "seats": 5,
                  "scene": "综合需求",
                  "focusFactors": ["空间"],
                  "excludedBrands": [],
                  "excludedCarIds": [6]
                }
                """);
        JsonNode bodyRelaxRecommend = generate(bodyRelaxDemand.path("id").asLong(), 1);
        assertEquals("FALLBACK", bodyRelaxRecommend.path("recommendStatus").asText());
        assertTrue(bodyRelaxRecommend.path("fallbackMessage").asText()
                .contains("未找到完全匹配车型"));
        assertEquals(0, countMatchLevel(bodyRelaxRecommend.path("items"), "STRICT"));
        assertEquals("RELAX_BODY_TYPE", bodyRelaxRecommend.path("items").get(0).path("matchLevel").asText());
        assertEquals("MPV", bodyRelaxRecommend.path("items").get(0).path("bodyType").asText());

        JsonNode energyRelaxDemand = postDemand("""
                {
                  "budgetMax": 150000,
                  "bodyType": "SUV",
                  "energyType": "燃油",
                  "seats": 5,
                  "scene": "综合需求",
                  "focusFactors": ["能耗"],
                  "excludedBrands": [],
                  "excludedCarIds": [6]
                }
                """);
        JsonNode energyRelaxRecommend = generate(energyRelaxDemand.path("id").asLong(), 1);
        assertEquals("FALLBACK", energyRelaxRecommend.path("recommendStatus").asText());
        assertTrue(energyRelaxRecommend.path("fallbackMessage").asText()
                .contains("未找到完全匹配车型"));
        assertEquals(0, countMatchLevel(energyRelaxRecommend.path("items"), "STRICT"));
        assertEquals("RELAX_ENERGY_TYPE", energyRelaxRecommend.path("items").get(0).path("matchLevel").asText());
        assertEquals("插混", energyRelaxRecommend.path("items").get(0).path("energyType").asText());

        JsonNode similarDemand = postDemand("""
                {
                  "budgetMax": 50000,
                  "bodyType": "MPV",
                  "energyType": "纯电",
                  "seats": 7,
                  "scene": "综合需求",
                  "focusFactors": ["空间"],
                  "excludedBrands": ["别克"],
                  "excludedCarIds": [20]
                }
                """);
        JsonNode similarRecommend = generate(similarDemand.path("id").asLong(), 1);
        assertEquals("FALLBACK", similarRecommend.path("recommendStatus").asText());
        assertTrue(similarRecommend.path("fallbackMessage").asText()
                .contains("未找到完全匹配车型"));
        assertEquals(0, countMatchLevel(similarRecommend.path("items"), "STRICT"));
        JsonNode similarItem = similarRecommend.path("items").get(0);
        assertEquals("SIMILAR_RECOMMEND", similarItem.path("matchLevel").asText());
        assertTrue(similarItem.path("seats").asInt() >= 7);
        assertFalse("别克".equals(similarItem.path("brand").asText()));
        assertFalse(20L == similarItem.path("carId").asLong());

        JsonNode fallbackChainRecommend = generate(budgetRelaxDemand.path("id").asLong(), 20);
        assertEquals("FALLBACK", fallbackChainRecommend.path("recommendStatus").asText());
        assertEquals(1, countCarId(fallbackChainRecommend.path("items"), 2L));
        assertEquals("RELAX_BUDGET", findItemByCarId(fallbackChainRecommend.path("items"), 2L)
                .path("matchLevel").asText());
        assertSavedRecommendationItemTexts(fallbackChainRecommend);

        insertBalancedCar();
        JsonNode balancedDemand = postDemand("""
                {
                  "bodyType": "MPV",
                  "energyType": "纯电",
                  "seats": 7,
                  "scene": "综合需求",
                  "focusFactors": ["空间", "安全"],
                  "excludedBrands": [],
                  "excludedCarIds": []
                }
                """);
        JsonNode balancedRecommend = generate(balancedDemand.path("id").asLong(), 1);
        assertEquals(301L, balancedRecommend.path("items").get(0).path("carId").asLong());
        assertEquals("该车型整体匹配较均衡，暂无明显短板。",
                balancedRecommend.path("items").get(0).path("weaknessText").asText());
        assertTextPresent(balancedRecommend.path("items").get(0).path("reasonText").asText());

        JsonNode emptyDemand = postDemand("""
                {
                  "budgetMax": 50000,
                  "bodyType": "SUV",
                  "energyType": "纯电",
                  "seats": 9,
                  "scene": "综合需求",
                  "focusFactors": ["空间"],
                  "excludedBrands": [],
                  "excludedCarIds": []
                }
                """);
        JsonNode emptyRecommend = generate(emptyDemand.path("id").asLong(), 10);
        assertEquals("EMPTY", emptyRecommend.path("recommendStatus").asText());
        assertEquals("暂未找到合适车型，请调整预算、车型类型或动力类型后重试。",
                emptyRecommend.path("fallbackMessage").asText());
        assertEquals(0, emptyRecommend.path("items").size());
        long emptyRecordId = emptyRecommend.path("recordId").asLong();
        assertEquals(1, count("SELECT COUNT(*) FROM recommend_record WHERE id = ? AND recommend_status = 'EMPTY'",
                emptyRecordId));
        assertEquals(0, count("SELECT COUNT(*) FROM recommend_item WHERE record_id = ?", emptyRecordId));

        insertSortingCars();
        JsonNode sortingDemand = postDemand("""
                {
                  "bodyType": "MPV",
                  "energyType": "纯电",
                  "scene": "综合需求",
                  "focusFactors": [],
                  "excludedBrands": [],
                  "excludedCarIds": [301]
                }
                """);
        JsonNode sortingRecommend = generate(sortingDemand.path("id").asLong(), 4);
        JsonNode sortingItems = sortingRecommend.path("items");
        assertEquals(4, sortingItems.size());
        assertEquals(203L, sortingItems.get(0).path("carId").asLong());
        assertEquals(204L, sortingItems.get(1).path("carId").asLong());
        assertEquals(201L, sortingItems.get(2).path("carId").asLong());
        assertEquals(202L, sortingItems.get(3).path("carId").asLong());
        assertSorted(sortingItems);
        assertAllRecommendationTextsPresent(sortingItems);

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
        assertTrue(firstHistoryRecord.path("topCarNames").size() > 0);
        assertTrue(firstHistoryRecord.path("itemCount").asLong() > 0);

        JsonNode historyPageTwo = getJson("/api/recommend/history?page=2&size=1");
        assertEquals(1, historyPageTwo.path("records").size());
        assertFalse(firstHistoryRecord.path("recordId").asLong()
                == historyPageTwo.path("records").get(0).path("recordId").asLong());

        JsonNode fullHistory = getJson("/api/recommend/history?page=1&size=100");
        assertTrue(containsRecord(fullHistory.path("records"), familyRecordId));

        JsonNode detail = getJson("/api/recommend/" + familyRecordId);
        assertEquals(familyRecordId, detail.path("recordId").asLong());
        assertEquals(1L, detail.path("userId").asLong());
        assertEquals(familyDemand.path("id").asLong(), detail.path("demandId").asLong());
        assertTextPresent(detail.path("profileText").asText());
        assertTextPresent(detail.path("fallbackMessage").asText());
        assertEquals("SUCCESS", detail.path("recommendStatus").asText());
        assertTrue(detail.path("createTime").isTextual());
        assertTrue(detail.path("weights").path("space").isNumber());
        assertEquals(familyDemand.path("id").asLong(), detail.path("demand").path("id").asLong());
        assertEquals(familyDemand.path("bodyType").asText(), detail.path("demand").path("bodyType").asText());
        JsonNode detailItem = detail.path("items").get(0);
        assertEquals(1, detailItem.path("rankNo").asInt());
        assertEquals(0, new BigDecimal("12.34").compareTo(detailItem.path("totalScore").decimalValue()));
        assertScoreFieldsPresent(detailItem);
        assertEquals("snapshot-tag", detailItem.path("tags").get(0).asText());
        assertEquals("snapshot reason", detailItem.path("reasonText").asText());
        assertEquals("snapshot weakness", detailItem.path("weaknessText").asText());
        assertEquals("STRICT", detailItem.path("matchLevel").asText());

        JsonNode sortingDetail = getJson("/api/recommend/" + sortingRecommend.path("recordId").asLong());
        assertRankNoAscending(sortingDetail.path("items"));

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

    private JsonNode generate(long demandId, int topK) throws Exception {
        String payload = """
                {
                  "demandId": %d,
                  "topK": %d
                }
                """.formatted(demandId, topK);
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

    private void assertRecordAndItemSnapshotsSaved(JsonNode recommend, JsonNode item, String recommendStatus) throws Exception {
        long recordId = recommend.path("recordId").asLong();
        assertEquals(1, count("SELECT COUNT(*) FROM recommend_record WHERE id = ? AND recommend_status = ?",
                recordId, recommendStatus));
        assertEquals(1, count("""
                SELECT COUNT(*) FROM recommend_item
                WHERE record_id = ?
                  AND car_id = ?
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
                  AND match_level = ?
                  AND reason_text IS NOT NULL
                  AND reason_text <> ''
                  AND weakness_text IS NOT NULL
                  AND weakness_text <> ''
                """, recordId, item.path("carId").asLong(), item.path("matchLevel").asText()));

        String tagsJson = jdbcTemplate.queryForObject(
                "SELECT tags FROM recommend_item WHERE record_id = ? AND rank_no = 1",
                String.class,
                recordId);
        JsonNode savedTags = readJsonArray(tagsJson);
        assertTrue(savedTags.isArray());
        assertEquals(item.path("tags").size(), savedTags.size());
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

    private JsonNode readJsonArray(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        if (node.isTextual()) {
            node = objectMapper.readTree(node.asText());
        }
        return node;
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

    private void assertAllRecommendationTextsPresent(JsonNode items) {
        for (JsonNode item : items) {
            assertTextPresent(item.path("reasonText").asText());
            assertTextPresent(item.path("weaknessText").asText());
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

    private boolean containsRecord(JsonNode records, long recordId) {
        for (JsonNode record : records) {
            if (record.path("recordId").asLong() == recordId) {
                return true;
            }
        }
        return false;
    }

    private int countCarId(JsonNode items, long carId) {
        int total = 0;
        for (JsonNode item : items) {
            if (item.path("carId").asLong() == carId) {
                total++;
            }
        }
        return total;
    }

    private JsonNode findItemByCarId(JsonNode items, long carId) {
        for (JsonNode item : items) {
            if (item.path("carId").asLong() == carId) {
                return item;
            }
        }
        throw new AssertionError("carId not found: " + carId);
    }

    private boolean containsTag(JsonNode item, String tag) {
        for (JsonNode value : item.path("tags")) {
            if (tag.equals(value.asText())) {
                return true;
            }
        }
        return false;
    }

    private void assertNoTag(JsonNode itemsOrItem, String tag) {
        if (itemsOrItem.isArray()) {
            for (JsonNode item : itemsOrItem) {
                assertNoTag(item, tag);
            }
            return;
        }
        assertFalse(containsTag(itemsOrItem, tag));
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

    private void insertSortingCars() {
        insertCar(201, "排序测试", "同分排序", "口碑优先测试车", "MPV", "纯电", 150000, 1000, "4.5");
        insertCar(202, "排序测试", "同分排序", "口碑次级测试车", "MPV", "纯电", 150000, 1000, "4.0");
        insertCar(203, "排序测试", "同分排序", "热度优先测试车", "MPV", "纯电", 150000, 1000, "4.0");
        insertCar(204, "排序测试", "同分排序", "热度次级测试车", "MPV", "纯电", 150000, 1000, "4.0");
        insertScore(201, "70.00", "70.00", "70.00", "70.00", "70.00", "70.00", "90.00", "10.00");
        insertScore(202, "70.00", "70.00", "70.00", "70.00", "70.00", "70.00", "80.00", "24.00");
        insertScore(203, "70.00", "70.00", "70.00", "70.00", "70.00", "60.00", "80.00", "46.00");
        insertScore(204, "70.00", "70.00", "70.00", "70.00", "70.00", "70.00", "80.00", "30.00");
    }

    private void insertBalancedCar() {
        insertCar(301, "解释测试", "均衡车系", "均衡纯电MPV", "MPV", "纯电", 150000, 1000, "4.0");
        insertScore(301, "80.00", "80.00", "80.00", "80.00", "80.00", "80.00", "80.00", "80.00");
    }

    private void insertCar(
            long id,
            String brand,
            String series,
            String modelName,
            String bodyType,
            String energyType,
            int guidePrice,
            int salesVolume,
            String userRating) {
        jdbcTemplate.update("""
                INSERT INTO car_model (
                    id, brand, series, model_name, guide_price, body_type, energy_type,
                    seats, launch_year, image_url, sales_volume, user_rating, audit_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 7, 2026, '', ?, ?, 'APPROVED')
                """, id, brand, series, modelName, guidePrice, bodyType, energyType, salesVolume,
                new BigDecimal(userRating));
    }

    private void insertScore(
            long carId,
            String space,
            String safety,
            String energy,
            String intelligence,
            String comfort,
            String power,
            String reputation,
            String popularity) {
        jdbcTemplate.update("""
                INSERT INTO car_feature_score (
                    car_id, space_score, safety_score, energy_score, intelligence_score,
                    comfort_score, power_score, reputation_score, popularity_score,
                    score_version, calculated_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'stage5-sort-test', ?)
                """, carId, new BigDecimal(space), new BigDecimal(safety), new BigDecimal(energy),
                new BigDecimal(intelligence), new BigDecimal(comfort), new BigDecimal(power),
                new BigDecimal(reputation), new BigDecimal(popularity), Timestamp.valueOf(LocalDateTime.now()));
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
