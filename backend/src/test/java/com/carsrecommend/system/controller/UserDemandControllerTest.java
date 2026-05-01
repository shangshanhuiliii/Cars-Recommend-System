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
    void demandEndpointsPersistNewDemandModelAndProfileWeights() throws Exception {
        JsonNode family = postDemand("""
                {
                  "rawText": "家庭出行，想要空间和安全都好一些",
                  "budgetMin": 100000,
                  "budgetMax": 150000,
                  "bodyTypes": ["SUV", "MPV"],
                  "energyTypes": ["插混", "新能源"],
                  "minSeats": 5,
                  "scenes": ["家庭出行", "长途自驾"],
                  "factorWeights": {
                    "price": 5,
                    "space": 8,
                    "safety": 8,
                    "energy": 6,
                    "intelligence": 3,
                    "comfort": 7,
                    "power": 2,
                    "reputation": 4,
                    "popularity": 1
                  },
                  "excludedBrands": ["特斯拉"],
                  "excludedCarIds": [4, 9]
                }
                """);
        long familyId = family.path("id").asLong();
        assertEquals(1L, family.path("userId").asLong());
        assertEquals("SUV", family.path("bodyTypes").get(0).asText());
        assertEquals("MPV", family.path("bodyTypes").get(1).asText());
        assertEquals("插混", family.path("energyTypes").get(0).asText());
        assertEquals("新能源", family.path("energyTypes").get(1).asText());
        assertEquals(5, family.path("minSeats").asInt());
        assertEquals("家庭出行", family.path("scenes").get(0).asText());
        assertEquals(8, family.path("factorWeights").path("space").asInt());
        assertEquals(4L, family.path("excludedCarIds").get(0).asLong());
        assertTrue(family.path("profileText").asText().contains("可接受SUV和MPV"));
        assertTrue(family.path("profileText").asText().contains("可接受插混和新能源动力"));
        assertTrue(family.path("profileText").asText().contains("重点关注空间、安全、舒适和能耗"));
        assertWeightSumIsOne(family);
        assertDecimalEquals("0.1818", weight(family, "space"));
        assertDecimalEquals("0.1818", weight(family, "safety"));
        assertDecimalEquals("0.1591", weight(family, "comfort"));

        JsonNode sceneDefault = postDemand("""
                {
                  "userId": 1,
                  "budgetMin": 80000,
                  "budgetMax": 120000,
                  "bodyTypes": ["轿车", "SUV"],
                  "energyTypes": ["燃油"],
                  "minSeats": 5,
                  "scenes": ["城市通勤", "家庭出行"],
                  "factorWeights": {
                    "price": 0,
                    "space": 0,
                    "safety": 0,
                    "energy": 0,
                    "intelligence": 0,
                    "comfort": 0,
                    "power": 0,
                    "reputation": 0,
                    "popularity": 0
                  },
                  "excludedBrands": [],
                  "excludedCarIds": []
                }
                """);
        assertWeightSumIsOne(sceneDefault);
        assertDecimalEquals("0.1750", weight(sceneDefault, "price"));
        assertDecimalEquals("0.1850", weight(sceneDefault, "safety"));
        assertDecimalEquals("0.1750", weight(sceneDefault, "energy"));
        assertTrue(weight(sceneDefault, "safety").compareTo(weight(sceneDefault, "comfort")) > 0);

        JsonNode defaultScene = postDemand("""
                {
                  "factorWeights": {},
                  "excludedBrands": [],
                  "excludedCarIds": []
                }
                """);
        assertWeightSumIsOne(defaultScene);
        assertEquals("综合需求", defaultScene.path("scenes").get(0).asText());
        assertDecimalEquals("0.1500", weight(defaultScene, "price"));

        JsonNode newEnergy = postDemand("""
                {
                  "budgetMax": 180000,
                  "bodyTypes": ["SUV"],
                  "energyTypes": ["新能源"],
                  "minSeats": 5,
                  "scenes": ["综合需求"],
                  "factorWeights": { "popularity": 10 },
                  "excludedBrands": ["丰田"],
                  "excludedCarIds": [2, 3]
                }
                """);
        long latestId = newEnergy.path("id").asLong();
        assertEquals("新能源", newEnergy.path("energyTypes").get(0).asText());
        assertEquals(2L, newEnergy.path("excludedCarIds").get(0).asLong());
        assertEquals(1, count("SELECT COUNT(*) FROM user_demand WHERE id = " + latestId
                + " AND user_id = 1 AND energy_types LIKE '%新能源%'"));

        mockMvc.perform(get("/api/user/demand/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(latestId))
                .andExpect(jsonPath("$.data.energyTypes[0]").value("新能源"))
                .andExpect(jsonPath("$.data.excludedCarIds[1]").value(3));

        mockMvc.perform(get("/api/user/demand/{id}", familyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bodyTypes[1]").value("MPV"))
                .andExpect(jsonPath("$.data.energyTypes[1]").value("新能源"))
                .andExpect(jsonPath("$.data.minSeats").value(5))
                .andExpect(jsonPath("$.data.weights.space").value(0.1818))
                .andExpect(jsonPath("$.data.factorWeights.safety").value(8));
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
