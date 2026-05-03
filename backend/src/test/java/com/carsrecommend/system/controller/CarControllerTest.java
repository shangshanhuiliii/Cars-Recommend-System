package com.carsrecommend.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
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

    @Test
    void userCarDetailReturnsModelParamScoreAndDoesNotRecalculate() throws Exception {
        mockMvc.perform(post("/api/admin/cars/{id}/score/recalculate", 2))
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

        compareCarsReturnsModelParamScoreAndDoesNotWriteRecommendationData();
    }

    private void compareCarsReturnsModelParamScoreAndDoesNotWriteRecommendationData() throws Exception {
        mockMvc.perform(post("/api/admin/cars/{id}/score/recalculate", 1))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/cars/{id}/score/recalculate", 2))
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
}
