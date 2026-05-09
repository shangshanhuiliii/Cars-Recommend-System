package com.carsrecommend.system.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
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
        "app.auth.jwt-secret=admin-favorite-controller-test-secret-keep-at-least-32-bytes",
        "app.auth.token-expire-seconds=7200",
        "spring.datasource.url=jdbc:h2:mem:cars_admin_favorites;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
@Sql(
        scripts = {"/db/schema.sql", "/db/seed-data.sql"},
        config = @SqlConfig(encoding = "UTF-8"),
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
class AdminFavoriteControllerTest {

    private static final String DEMO_USER_PASSWORD_HASH =
            "pbkdf2$310000$ZGVtb191c2VyX3NhbHQxNg==$9w9/M2pOGlYqpRoEtjTJr5MwQ6UMyGM2/OSH577wGCY=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpFavoriteData() {
        jdbcTemplate.update("DELETE FROM user_favorite");
        jdbcTemplate.update("DELETE FROM user_compare_car");
        jdbcTemplate.update("DELETE FROM app_user WHERE id = 2");
        jdbcTemplate.update(
                """
                        INSERT INTO app_user (id, username, password, nickname, email, phone, status)
                        VALUES (2, 'favorite_user', ?, 'Favorite User', 'favorite_user@example.com', '13900000000', 'ACTIVE')
                        """,
                DEMO_USER_PASSWORD_HASH);
        jdbcTemplate.update("INSERT INTO user_favorite (user_id, car_id) VALUES (1, 1)");
        jdbcTemplate.update("INSERT INTO user_favorite (user_id, car_id) VALUES (2, 1)");
        jdbcTemplate.update("INSERT INTO user_favorite (user_id, car_id) VALUES (1, 2)");
    }

    @Test
    void adminCanQueryFavoriteCarRankingAndFavoriteUsersReadOnly() throws Exception {
        String adminToken = adminToken();

        mockMvc.perform(get("/api/admin/favorites/cars")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].carId").value(1))
                .andExpect(jsonPath("$.data.records[0].favoriteCount").value(2))
                .andExpect(jsonPath("$.data.records[1].carId").value(2))
                .andExpect(jsonPath("$.data.records[1].favoriteCount").value(1));

        mockMvc.perform(get("/api/admin/favorites/cars")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].carId").value(1))
                .andExpect(jsonPath("$.data.records[0].favoriteCount").value(2));

        mockMvc.perform(get("/api/admin/favorites/cars/{carId}/users", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[?(@.username=='demo_user')]").exists())
                .andExpect(jsonPath("$.data.records[?(@.username=='favorite_user')]").exists());

        mockMvc.perform(delete("/api/admin/favorites/cars/{carId}/users", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void adminFavoriteApisRequireAdminToken() throws Exception {
        mockMvc.perform(get("/api/admin/favorites/cars").header(HttpHeaders.AUTHORIZATION, bearer(userToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(get("/api/admin/favorites/cars"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    private String adminToken() throws Exception {
        return login("/api/auth/admin/login", "demo_admin", "admin123456").andReturnData().path("token").asText();
    }

    private String userToken() throws Exception {
        return login("/api/auth/user/login", "demo_user", "demo123456").andReturnData().path("token").asText();
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
