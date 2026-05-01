package com.carsrecommend.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        "spring.datasource.url=jdbc:h2:mem:cars_stage4;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
@Sql(
        scripts = {"/db/schema.sql", "/db/seed-data.sql"},
        config = @SqlConfig(encoding = "UTF-8")
)
class UserDemandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void demandEndpointsPersistProfileWeightsAndReturnArrayFields() throws Exception {
        JsonNode family = postDemand("""
                {
                  "rawText": "家庭出行，想要空间和安全都好一些",
                  "budgetMin": 100000,
                  "budgetMax": 150000,
                  "bodyType": "SUV",
                  "energyType": "插混",
                  "seats": 5,
                  "scene": "家庭出行",
                  "focusFactors": ["空间", "安全"],
                  "excludedBrands": ["特斯拉"],
                  "excludedCarIds": [4, 9]
                }
                """);
        long familyId = family.path("id").asLong();
        assertEquals(1L, family.path("userId").asLong());
        assertEquals("家庭实用型用户，预算10-15万，偏好插混SUV，关注空间和安全。",
                family.path("profileText").asText());
        assertEquals("空间", family.path("focusFactors").get(0).asText());
        assertEquals(4L, family.path("excludedCarIds").get(0).asLong());
        assertWeightSumIsOne(family);
        assertTrue(weight(family, "space").compareTo(weight(family, "price")) > 0);
        assertTrue(weight(family, "safety").compareTo(weight(family, "energy")) > 0);
        assertTrue(weight(family, "comfort").compareTo(weight(family, "intelligence")) > 0);

        JsonNode city = postDemand("""
                {
                  "userId": 1,
                  "budgetMin": 80000,
                  "budgetMax": 120000,
                  "bodyType": "轿车",
                  "energyType": "燃油",
                  "seats": 5,
                  "scene": "城市通勤",
                  "focusFactors": ["价格", "能耗", "智能"],
                  "excludedBrands": [],
                  "excludedCarIds": []
                }
                """);
        assertWeightSumIsOne(city);
        assertTrue(weight(city, "price").compareTo(weight(city, "safety")) > 0);
        assertTrue(weight(city, "energy").compareTo(weight(city, "space")) > 0);
        assertTrue(weight(city, "intelligence").compareTo(weight(city, "comfort")) > 0);

        JsonNode sameDimensionCap = postDemand("""
                {
                  "scene": "综合需求",
                  "focusFactors": ["能耗", "省油", "续航"],
                  "excludedBrands": [],
                  "excludedCarIds": []
                }
                """);
        assertWeightSumIsOne(sameDimensionCap);
        assertDecimalEquals("0.2500", weight(sameDimensionCap, "energy"));

        JsonNode rawWeightCap = postDemand("""
                {
                  "scene": "城市通勤",
                  "focusFactors": ["价格", "性价比", "不贵"],
                  "excludedBrands": [],
                  "excludedCarIds": []
                }
                """);
        assertWeightSumIsOne(rawWeightCap);
        assertDecimalEquals("0.3182", weight(rawWeightCap, "price"));

        JsonNode newEnergy = postDemand("""
                {
                  "budgetMax": 180000,
                  "bodyType": "SUV",
                  "energyType": "新能源",
                  "seats": 5,
                  "scene": "综合需求",
                  "focusFactors": ["热度"],
                  "excludedBrands": ["丰田"],
                  "excludedCarIds": [2, 3]
                }
                """);
        long latestId = newEnergy.path("id").asLong();
        assertEquals("新能源", newEnergy.path("energyType").asText());
        assertEquals(2L, newEnergy.path("excludedCarIds").get(0).asLong());
        assertEquals(1, count("SELECT COUNT(*) FROM user_demand WHERE id = " + latestId
                + " AND user_id = 1 AND energy_type = '新能源'"));

        mockMvc.perform(get("/api/user/demand/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(latestId))
                .andExpect(jsonPath("$.data.energyType").value("新能源"))
                .andExpect(jsonPath("$.data.excludedCarIds[1]").value(3));

        mockMvc.perform(get("/api/user/demand/{id}", familyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.profileText").value("家庭实用型用户，预算10-15万，偏好插混SUV，关注空间和安全。"))
                .andExpect(jsonPath("$.data.weights.space").value(0.2845))
                .andExpect(jsonPath("$.data.focusFactors[1]").value("安全"));
    }

    private JsonNode postDemand(String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/demand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.profileText").isString())
                .andExpect(jsonPath("$.data.weights.price").isNumber())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }

    private BigDecimal weight(JsonNode demand, String name) {
        return demand.path("weights").path(name).decimalValue();
    }

    private void assertWeightSumIsOne(JsonNode demand) {
        BigDecimal sum = weight(demand, "price")
                .add(weight(demand, "space"))
                .add(weight(demand, "safety"))
                .add(weight(demand, "energy"))
                .add(weight(demand, "intelligence"))
                .add(weight(demand, "comfort"))
                .add(weight(demand, "power"))
                .add(weight(demand, "reputation"))
                .add(weight(demand, "popularity"));
        assertEquals(0, new BigDecimal("1.0000").compareTo(sum));
    }

    private void assertDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
