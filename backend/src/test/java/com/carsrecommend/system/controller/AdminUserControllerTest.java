package com.carsrecommend.system.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
        "app.auth.jwt-secret=admin-user-controller-test-secret-keep-at-least-32-bytes",
        "app.auth.token-expire-seconds=7200",
        "spring.datasource.url=jdbc:h2:mem:cars_admin_user;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
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
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpUserActivityData() {
        jdbcTemplate.update("DELETE FROM recommend_feedback");
        jdbcTemplate.update("DELETE FROM user_favorite");
        jdbcTemplate.update("DELETE FROM recommend_item");
        jdbcTemplate.update("DELETE FROM recommend_record");
        jdbcTemplate.update("DELETE FROM user_demand");
        jdbcTemplate.update("UPDATE app_user SET status = 'ACTIVE' WHERE username = 'demo_user'");

        jdbcTemplate.update(
                "INSERT INTO user_demand (user_id, raw_text, profile_text) VALUES (1, 'test demand', 'admin user test demand')");
        Long demandId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM user_demand", Long.class);
        jdbcTemplate.update(
                """
                        INSERT INTO recommend_record (
                            user_id, demand_id, profile_text, weight_snapshot, recommend_status
                        ) VALUES (1, ?, 'admin user test profile', '{"finalWeight":{}}', 'SUCCESS')
                        """,
                demandId);
        Long recordId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM recommend_record", Long.class);
        jdbcTemplate.update("INSERT INTO user_favorite (user_id, car_id) VALUES (1, 1)");
        jdbcTemplate.update(
                """
                        INSERT INTO recommend_feedback (
                            user_id, record_id, satisfaction_score, satisfaction_level, reason_tags, comment
                        ) VALUES (1, ?, 5, 'SATISFIED', '[]', 'good')
                        """,
                recordId);
    }

    @Test
    void adminCanPageUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].username").value("demo_user"))
                .andExpect(jsonPath("$.data.records[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void userCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(userToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void anonymousCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminCanViewUserDetail() throws Exception {
        mockMvc.perform(get("/api/admin/users/{userId}", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("demo_user"))
                .andExpect(jsonPath("$.data.summary.demandCount").value(1))
                .andExpect(jsonPath("$.data.latestDemand.id").isNumber())
                .andExpect(jsonPath("$.data.recentRecommendRecords[0].recordId").isNumber())
                .andExpect(jsonPath("$.data.favorites[0].carId").value(1))
                .andExpect(jsonPath("$.data.feedbacks[0].satisfactionScore").value(5));
    }

    @Test
    void adminCanViewUserRecommendRecords() throws Exception {
        mockMvc.perform(get("/api/admin/users/{userId}/recommend-records", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].recommendStatus").value("SUCCESS"));
    }

    @Test
    void adminCanViewUserFavorites() throws Exception {
        mockMvc.perform(get("/api/admin/users/{userId}/favorites", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].carId").value(1));
    }

    @Test
    void adminCanViewUserFeedbacks() throws Exception {
        mockMvc.perform(get("/api/admin/users/{userId}/feedbacks", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].satisfactionScore").value(5));
    }

    @Test
    void adminCanDisableAndEnableUser() throws Exception {
        String adminToken = adminToken();
        mockMvc.perform(put("/api/admin/users/{userId}/status", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        login("/api/auth/user/login", "demo_user", "demo123456")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(put("/api/admin/users/{userId}/status", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        login("/api/auth/user/login", "demo_user", "demo123456")
                .andExpect(status().isOk());
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

        ResultActionsWithData andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            matcher.match(result);
            return this;
        }

        JsonNode andReturnData() throws Exception {
            return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
        }
    }
}
