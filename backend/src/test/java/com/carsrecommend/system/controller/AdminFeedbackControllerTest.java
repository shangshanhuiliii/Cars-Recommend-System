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
        "app.auth.jwt-secret=admin-feedback-controller-test-secret-keep-at-least-32-bytes",
        "app.auth.token-expire-seconds=7200",
        "spring.datasource.url=jdbc:h2:mem:cars_admin_feedbacks;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
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
class AdminFeedbackControllerTest {

    private static final String DEMO_USER_PASSWORD_HASH =
            "pbkdf2$310000$ZGVtb191c2VyX3NhbHQxNg==$9w9/M2pOGlYqpRoEtjTJr5MwQ6UMyGM2/OSH577wGCY=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpFeedbackData() {
        jdbcTemplate.update("DELETE FROM recommend_feedback");
        jdbcTemplate.update("DELETE FROM recommend_item");
        jdbcTemplate.update("DELETE FROM recommend_record");
        jdbcTemplate.update("DELETE FROM user_demand");
        jdbcTemplate.update("DELETE FROM user_favorite");
        jdbcTemplate.update("DELETE FROM user_compare_car");
        jdbcTemplate.update("DELETE FROM app_user WHERE id = 2");
        jdbcTemplate.update(
                """
                        INSERT INTO app_user (id, username, password, nickname, email, phone, status)
                        VALUES (2, 'feedback_admin_user', ?, 'Feedback User', 'feedback_admin_user@example.com', '', 'ACTIVE')
                        """,
                DEMO_USER_PASSWORD_HASH);
        jdbcTemplate.update("INSERT INTO user_demand (id, user_id, raw_text, profile_text) VALUES (301, 1, '', 'profile one')");
        jdbcTemplate.update("INSERT INTO user_demand (id, user_id, raw_text, profile_text) VALUES (302, 2, '', 'profile two')");
        jdbcTemplate.update("""
                INSERT INTO recommend_record (id, user_id, demand_id, profile_text, weight_snapshot, recommend_status)
                VALUES (401, 1, 301, 'profile one', '{"finalWeight":{}}', 'SUCCESS')
                """);
        jdbcTemplate.update("""
                INSERT INTO recommend_record (id, user_id, demand_id, profile_text, weight_snapshot, recommend_status)
                VALUES (402, 2, 302, 'profile two', '{"finalWeight":{}}', 'FALLBACK')
                """);
        jdbcTemplate.update("""
                INSERT INTO recommend_feedback (
                    id, user_id, record_id, satisfaction_score, satisfaction_level, reason_tags, comment
                ) VALUES (501, 1, 401, 5, 'SATISFIED', '["推荐有帮助"]', 'explain clear')
                """);
        jdbcTemplate.update("""
                INSERT INTO recommend_feedback (
                    id, user_id, record_id, satisfaction_score, satisfaction_level, reason_tags, comment
                ) VALUES (502, 2, 402, 2, 'DISSATISFIED', '["推荐太贵"]', 'too expensive')
                """);
    }

    @Test
    void adminCanQueryFeedbackRecordsWithFiltersReadOnly() throws Exception {
        String adminToken = adminToken();

        mockMvc.perform(get("/api/admin/feedbacks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[?(@.feedbackId==501)].username").exists())
                .andExpect(jsonPath("$.data.records[?(@.feedbackId==502)].username").exists());

        mockMvc.perform(get("/api/admin/feedbacks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].userId").value(2))
                .andExpect(jsonPath("$.data.records[0].recordId").value(402));

        mockMvc.perform(get("/api/admin/feedbacks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("satisfactionScore", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].satisfactionScore").value(5));

        mockMvc.perform(get("/api/admin/feedbacks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("keyword", "expensive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].feedbackId").value(502));

        mockMvc.perform(delete("/api/admin/feedbacks/{feedbackId}", 501)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminFeedbackApisRequireAdminToken() throws Exception {
        mockMvc.perform(get("/api/admin/feedbacks").header(HttpHeaders.AUTHORIZATION, bearer(userToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(get("/api/admin/feedbacks"))
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
