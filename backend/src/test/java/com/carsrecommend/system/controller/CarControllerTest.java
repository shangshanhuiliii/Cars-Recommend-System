package com.carsrecommend.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.auth.enabled=true",
        "app.auth.jwt-secret=car-controller-test-secret-keep-at-least-32-bytes",
        "app.auth.token-expire-seconds=7200",
        "spring.datasource.url=jdbc:h2:mem:cars_stage8_car_detail;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
@Sql(
        scripts = {"/db/schema.sql", "/db/seed-data.sql"},
        config = @SqlConfig(encoding = "UTF-8")
)
class CarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userCarDetailReturnsModelParamScoreAndDoesNotRecalculate() throws Exception {
        verifyHomeCarouselIsPublicReadOnlyAndFiltersCars();

        String adminToken = adminToken();
        mockMvc.perform(post("/api/admin/cars/{id}/score/recalculate", 2)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/car/{id}", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.carModel.id").value(2))
                .andExpect(jsonPath("$.data.carModel.modelName").value("宋PLUS DM-i 110KM 旗舰型"))
                .andExpect(jsonPath("$.data.carParam.carId").value(2))
                .andExpect(jsonPath("$.data.carParam.wheelbaseMm").value(2765))
                .andExpect(jsonPath("$.data.carFeatureScore.carId").value(2))
                .andExpect(jsonPath("$.data.carFeatureScore.spaceScore").value(83.00));

        assertEquals(1, count("SELECT COUNT(*) FROM car_feature_score WHERE car_id = 2"));
        assertEquals(0, count("SELECT COUNT(*) FROM car_feature_score WHERE car_id = 1"));
        mockMvc.perform(get("/api/car/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carModel.id").value(1))
                .andExpect(jsonPath("$.data.carParam.carId").value(1))
                .andExpect(jsonPath("$.data.carFeatureScore").doesNotExist());
        assertEquals(0, count("SELECT COUNT(*) FROM car_feature_score WHERE car_id = 1"));

        jdbcTemplate.update("""
                INSERT INTO car_model (
                    id, brand, series, model_name, guide_price, body_type, energy_type,
                    seats, launch_year, image_url, sales_volume, user_rating, audit_status
                ) VALUES (501, '详情测试', '无参数车系', '无参数评分测试车', 99900, 'SUV', '燃油',
                    5, 2026, '', 300, ?, 'APPROVED')
                """, new BigDecimal("4.2"));
        mockMvc.perform(get("/api/car/{id}", 501))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carModel.id").value(501))
                .andExpect(jsonPath("$.data.carParam").doesNotExist())
                .andExpect(jsonPath("$.data.carFeatureScore").doesNotExist());

        jdbcTemplate.update("UPDATE car_model SET deleted = TRUE WHERE id = 501");
        mockMvc.perform(get("/api/car/{id}", 501))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/api/car/{id}", 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/api/car/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@=='比亚迪')]").exists())
                .andExpect(jsonPath("$.data[?(@=='详情测试')]").doesNotExist());

        mockMvc.perform(get("/api/car/options")
                        .param("keyword", "宋")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].brand").value("比亚迪"))
                .andExpect(jsonPath("$.data[0].modelName").value("宋PLUS DM-i 110KM 旗舰型"))
                .andExpect(jsonPath("$.data[0].displayName").value("比亚迪 宋PLUS DM-i 110KM 旗舰型"));

        mockMvc.perform(get("/api/car/options")
                        .param("keyword", "无参数评分测试车"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        compareCarsReturnsModelParamScoreAndDoesNotWriteRecommendationData(adminToken);
    }

    private void verifyHomeCarouselIsPublicReadOnlyAndFiltersCars() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO car_model (
                    id, brand, series, model_name, guide_price, body_type, energy_type,
                    seats, launch_year, image_url, sales_volume, user_rating, audit_status, deleted
                ) VALUES
                    (601, '轮播测试', 'Aero', 'Aero One', 189900, 'SUV', '纯电',
                        5, 2026, '/uploads/car-images/aero-one.jpg', 1200, ?, 'APPROVED', FALSE),
                    (602, '轮播测试', 'Aero', 'Aero Two', 209900, '轿车', '插混',
                        5, 2026, '/uploads/car-images/aero-two.jpg', 1100, ?, 'APPROVED', FALSE),
                    (603, '轮播测试', 'Aero', 'Aero Three', 229900, 'MPV', '增程',
                        6, 2026, '/uploads/car-images/aero-three.jpg', 900, ?, 'APPROVED', FALSE),
                    (604, '轮播测试', 'Pending', 'Pending Car', 199900, 'SUV', '纯电',
                        5, 2026, '/uploads/car-images/pending.jpg', 800, ?, 'PENDING', FALSE),
                    (605, '轮播测试', 'Deleted', 'Deleted Car', 199900, 'SUV', '纯电',
                        5, 2026, '/uploads/car-images/deleted.jpg', 800, ?, 'APPROVED', TRUE)
                """,
                new BigDecimal("4.6"),
                new BigDecimal("4.5"),
                new BigDecimal("4.4"),
                new BigDecimal("4.3"),
                new BigDecimal("4.2"));

        int recordCount = count("SELECT COUNT(*) FROM recommend_record");
        int itemCount = count("SELECT COUNT(*) FROM recommend_item");
        int demandCount = count("SELECT COUNT(*) FROM user_demand");
        int scoreCount = count("SELECT COUNT(*) FROM car_feature_score");

        mockMvc.perform(get("/api/car/home-carousel").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[?(@.id==601)]").exists())
                .andExpect(jsonPath("$.data[?(@.id==602)]").exists())
                .andExpect(jsonPath("$.data[?(@.id==603)]").exists())
                .andExpect(jsonPath("$.data[?(@.id==604)]").doesNotExist())
                .andExpect(jsonPath("$.data[?(@.id==605)]").doesNotExist())
                .andExpect(jsonPath("$.data[0].brand").exists())
                .andExpect(jsonPath("$.data[0].series").exists())
                .andExpect(jsonPath("$.data[0].modelName").exists())
                .andExpect(jsonPath("$.data[0].guidePrice").exists())
                .andExpect(jsonPath("$.data[0].bodyType").exists())
                .andExpect(jsonPath("$.data[0].energyType").exists())
                .andExpect(jsonPath("$.data[0].seats").exists())
                .andExpect(jsonPath("$.data[0].imageUrl").exists());

        mockMvc.perform(get("/api/car/home-carousel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6));

        mockMvc.perform(get("/api/car/home-carousel").param("limit", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/api/car/home-carousel").param("limit", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        assertEquals(recordCount, count("SELECT COUNT(*) FROM recommend_record"));
        assertEquals(itemCount, count("SELECT COUNT(*) FROM recommend_item"));
        assertEquals(demandCount, count("SELECT COUNT(*) FROM user_demand"));
        assertEquals(scoreCount, count("SELECT COUNT(*) FROM car_feature_score"));
    }

    private void compareCarsReturnsModelParamScoreAndDoesNotWriteRecommendationData(String adminToken) throws Exception {
        mockMvc.perform(post("/api/admin/cars/{id}/score/recalculate", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/cars/{id}/score/recalculate", 2)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        assertEquals(2, count("SELECT COUNT(*) FROM car_feature_score"));
        assertEquals(0, count("SELECT COUNT(*) FROM recommend_record"));
        assertEquals(0, count("SELECT COUNT(*) FROM recommend_item"));
        assertEquals(0, count("SELECT COUNT(*) FROM user_demand"));

        mockMvc.perform(get("/api/car/compare").param("carIds", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.carIds.length()").value(1))
                .andExpect(jsonPath("$.data.dimensions.length()").value(8))
                .andExpect(jsonPath("$.data.cars.length()").value(1))
                .andExpect(jsonPath("$.data.cars[0].carId").value(1))
                .andExpect(jsonPath("$.data.cars[0].brand").value("比亚迪"))
                .andExpect(jsonPath("$.data.cars[0].param.carId").value(1))
                .andExpect(jsonPath("$.data.cars[0].scores.space").isNumber());

        mockMvc.perform(get("/api/car/compare").param("carIds", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.carIds.length()").value(2))
                .andExpect(jsonPath("$.data.dimensions.length()").value(8))
                .andExpect(jsonPath("$.data.dimensions[0].key").value("space"))
                .andExpect(jsonPath("$.data.cars.length()").value(2))
                .andExpect(jsonPath("$.data.cars[0].carId").value(1))
                .andExpect(jsonPath("$.data.cars[0].brand").value("比亚迪"))
                .andExpect(jsonPath("$.data.cars[0].param.carId").value(1))
                .andExpect(jsonPath("$.data.cars[0].scores.space").isNumber())
                .andExpect(jsonPath("$.data.cars[1].carId").value(2))
                .andExpect(jsonPath("$.data.cars[1].param.wheelbaseMm").value(2765))
                .andExpect(jsonPath("$.data.cars[1].scores.safety").isNumber());

        mockMvc.perform(get("/api/car/compare").param("carIds", "1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cars.length()").value(3))
                .andExpect(jsonPath("$.data.cars[2].carId").value(3))
                .andExpect(jsonPath("$.data.cars[2].scores").doesNotExist());

        assertEquals(2, count("SELECT COUNT(*) FROM car_feature_score"));
        assertEquals(0, count("SELECT COUNT(*) FROM recommend_record"));
        assertEquals(0, count("SELECT COUNT(*) FROM recommend_item"));
        assertEquals(0, count("SELECT COUNT(*) FROM user_demand"));

        mockMvc.perform(get("/api/car/compare").param("carIds", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(get("/api/car/compare").param("carIds", "1,2,3,4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(get("/api/car/compare").param("carIds", "1,1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carIds.length()").value(1))
                .andExpect(jsonPath("$.data.cars.length()").value(1));
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private String adminToken() throws Exception {
        return login("/api/auth/admin/login", "admin", "admin123456").andReturnData().path("token").asText();
    }

    private ResultActionsWithData login(String url, String username, String password) throws Exception {
        String payload = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(payload))
                .andReturn();
        return new ResultActionsWithData(result);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private class ResultActionsWithData {

        private final MvcResult result;

        ResultActionsWithData(MvcResult result) {
            this.result = result;
        }

        JsonNode andReturnData() throws Exception {
            return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
        }
    }
}
