package com.carsrecommend.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cars_stage11_favorites;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
@Sql(
        scripts = {"/db/schema.sql", "/db/seed-data.sql"},
        config = @SqlConfig(encoding = "UTF-8")
)
class UserFavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void favoriteCancelListAndStatusAreIdempotentForDemoUser() throws Exception {
        mockMvc.perform(post("/api/admin/cars/{id}/score/recalculate", 2))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/user/favorites/{carId}", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/api/user/favorites/{carId}", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertEquals(1, count("SELECT COUNT(*) FROM user_favorite WHERE user_id = 1 AND car_id = 2"));
        assertEquals(1, count("SELECT COUNT(*) FROM user_favorite WHERE user_id = 1 AND car_id = 2 AND deleted = FALSE"));

        mockMvc.perform(get("/api/user/favorites/status").param("carIds", "2,8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].carId").value(2))
                .andExpect(jsonPath("$.data[0].favorited").value(true))
                .andExpect(jsonPath("$.data[1].carId").value(8))
                .andExpect(jsonPath("$.data[1].favorited").value(false));

        mockMvc.perform(get("/api/user/favorites").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].carId").value(2))
                .andExpect(jsonPath("$.data.records[0].brand").value("比亚迪"))
                .andExpect(jsonPath("$.data.records[0].modelName").value("宋PLUS DM-i 110KM 旗舰型"))
                .andExpect(jsonPath("$.data.records[0].scoreSummary.space").isNumber());

        mockMvc.perform(delete("/api/user/favorites/{carId}", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(delete("/api/user/favorites/{carId}", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertEquals(0, count("SELECT COUNT(*) FROM user_favorite WHERE user_id = 1 AND car_id = 2 AND deleted = FALSE"));
        mockMvc.perform(get("/api/user/favorites/status").param("carIds", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].favorited").value(false));

        mockMvc.perform(get("/api/user/favorites").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records.length()").value(0));

        mockMvc.perform(delete("/api/user/favorites/{carId}", 8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/user/favorites/{carId}", 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        jdbcTemplate.update("UPDATE car_model SET deleted = TRUE WHERE id = 3");
        mockMvc.perform(post("/api/user/favorites/{carId}", 3))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
