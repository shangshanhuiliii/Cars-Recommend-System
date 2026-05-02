package com.carsrecommend.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carsrecommend.system.service.CarModelService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        "spring.datasource.url=jdbc:h2:mem:cars_stage2;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
@Sql(
        scripts = {"/db/schema.sql", "/db/seed-data.sql"},
        config = @SqlConfig(encoding = "UTF-8")
)
class AdminCarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CarModelService carModelService;

    @Test
    void carAndParamEndpointsCoverStageTwoRequirements() throws Exception {
        mockMvc.perform(get("/api/admin/cars")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(120))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.records.length()").value(5));

        mockMvc.perform(get("/api/admin/cars/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.modelName").value("秦PLUS DM-i 120KM 卓越型"));

        MvcResult createResult = mockMvc.perform(post("/api/admin/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(carPayload("阶段2参数维护测试款", 168800, "APPROVED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.brand").value("测试品牌"))
                .andExpect(jsonPath("$.data.energyType").value("插混"))
                .andReturn();
        long carId = readDataId(createResult);

        mockMvc.perform(put("/api/admin/cars/{id}", carId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(carPayload("阶段2参数维护测试款 改款", 158800, "PENDING")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(carId))
                .andExpect(jsonPath("$.data.modelName").value("阶段2参数维护测试款 改款"))
                .andExpect(jsonPath("$.data.auditStatus").value("PENDING"));
        assertFalse(carModelService.listApprovedRecommendationCandidates().stream()
                .anyMatch(car -> car.getId().equals(carId)));

        mockMvc.perform(put("/api/admin/cars/{id}/param", carId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(paramPayload(carId, 2790)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carId").value(carId))
                .andExpect(jsonPath("$.data.wheelbaseMm").value(2790))
                .andExpect(jsonPath("$.data.has360Camera").value(true));

        mockMvc.perform(get("/api/admin/cars/{id}/param", carId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carId").value(carId))
                .andExpect(jsonPath("$.data.lengthMm").value(4780));

        mockMvc.perform(put("/api/admin/cars/{id}/param", 99999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(paramPayload(99999, 2800)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(delete("/api/admin/cars/{id}", carId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        Integer deletedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_model WHERE id = ? AND deleted = TRUE",
                Integer.class,
                carId);
        assertEquals(1, deletedCount);

        mockMvc.perform(get("/api/admin/cars/{id}", carId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    private long readDataId(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return jsonNode.path("data").path("id").asLong();
    }

    private String carPayload(String modelName, int guidePrice, String auditStatus) {
        return """
                {
                  "brand": "测试品牌",
                  "series": "阶段2车系",
                  "modelName": "%s",
                  "guidePrice": %d,
                  "bodyType": "SUV",
                  "energyType": "插混",
                  "seats": 5,
                  "launchYear": 2026,
                  "imageUrl": "",
                  "salesVolume": 1200,
                  "userRating": 4.3,
                  "auditStatus": "%s"
                }
                """.formatted(modelName, guidePrice, auditStatus);
    }

    private String paramPayload(long carId, int wheelbaseMm) {
        return """
                {
                  "carId": %d,
                  "lengthMm": 4780,
                  "widthMm": 1890,
                  "heightMm": 1680,
                  "wheelbaseMm": %d,
                  "fuelConsumption": 4.8,
                  "electricConsumption": null,
                  "electricRangeKm": 120,
                  "totalRangeKm": 1050,
                  "acceleration100": 7.8,
                  "airbagCount": 6,
                  "hasAbs": true,
                  "hasEsp": true,
                  "hasActiveBrake": true,
                  "hasLaneKeep": true,
                  "hasAdaptiveCruise": true,
                  "hasBlindSpot": true,
                  "hasReverseCamera": true,
                  "has360Camera": true,
                  "hasOta": true,
                  "hasVoiceControl": true,
                  "hasAutoParking": false,
                  "screenSize": 15.6,
                  "assistDriveLevel": "L2"
                }
                """.formatted(carId, wheelbaseMm);
    }
}
