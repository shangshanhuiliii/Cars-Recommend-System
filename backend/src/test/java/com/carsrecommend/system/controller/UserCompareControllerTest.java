package com.carsrecommend.system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        "app.auth.jwt-secret=user-compare-controller-test-secret-keep-at-least-32-bytes",
        "app.auth.token-expire-seconds=7200",
        "spring.datasource.url=jdbc:h2:mem:cars_user_compare;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
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
class UserCompareControllerTest {

    private static final String DEMO_USER_PASSWORD_HASH =
            "pbkdf2$310000$ZGVtb191c2VyX3NhbHQxNg==$9w9/M2pOGlYqpRoEtjTJr5MwQ6UMyGM2/OSH577wGCY=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanCompareData() {
        jdbcTemplate.update("DELETE FROM user_compare_car");
        jdbcTemplate.update("DELETE FROM app_user WHERE id = 2");
    }

    @Test
    void userCompareListAddRepeatLimitRemoveAndClearUseCurrentUser() throws Exception {
        String userToken = userToken();

        mockMvc.perform(get("/api/user/compare").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carIds.length()").value(0))
                .andExpect(jsonPath("$.data.dimensions.length()").value(8))
                .andExpect(jsonPath("$.data.cars.length()").value(0));

        mockMvc.perform(post("/api/user/compare/{carId}", 1).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carIds[0]").value(1))
                .andExpect(jsonPath("$.data.cars[0].carId").value(1));
        mockMvc.perform(post("/api/user/compare/{carId}", 1).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carIds.length()").value(1));
        assertEquals(1, count("SELECT COUNT(*) FROM user_compare_car WHERE user_id = 1 AND car_id = 1"));

        mockMvc.perform(post("/api/user/compare/{carId}", 2).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carIds.length()").value(2));
        mockMvc.perform(post("/api/user/compare/{carId}", 3).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carIds.length()").value(3));
        mockMvc.perform(post("/api/user/compare/{carId}", 4).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(delete("/api/user/compare/{carId}", 2).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carIds.length()").value(2))
                .andExpect(jsonPath("$.data.carIds[?(@==2)]").doesNotExist());
        mockMvc.perform(delete("/api/user/compare/{carId}", 2).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carIds.length()").value(2));

        mockMvc.perform(delete("/api/user/compare").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carIds.length()").value(0));
        assertEquals(0, count("SELECT COUNT(*) FROM user_compare_car WHERE user_id = 1 AND deleted = FALSE"));
    }

    @Test
    void compareSelectionsAreIsolatedByUserAndStaticCompareStaysPublic() throws Exception {
        jdbcTemplate.update(
                """
                        INSERT INTO app_user (id, username, password, nickname, phone, status)
                        VALUES (2, 'compare_user', ?, 'Compare User', '', 'ACTIVE')
                        """,
                DEMO_USER_PASSWORD_HASH);
        String userOneToken = userToken();
        String userTwoToken = login("/api/auth/user/login", "compare_user", "demo123456")
                .andReturnData()
                .path("token")
                .asText();

        mockMvc.perform(post("/api/user/compare/{carId}", 1).header(HttpHeaders.AUTHORIZATION, bearer(userOneToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/user/compare/{carId}", 2).header(HttpHeaders.AUTHORIZATION, bearer(userTwoToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/compare").header(HttpHeaders.AUTHORIZATION, bearer(userOneToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carIds[0]").value(1))
                .andExpect(jsonPath("$.data.carIds[?(@==2)]").doesNotExist());
        mockMvc.perform(get("/api/user/compare").header(HttpHeaders.AUTHORIZATION, bearer(userTwoToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carIds[0]").value(2))
                .andExpect(jsonPath("$.data.carIds[?(@==1)]").doesNotExist());

        mockMvc.perform(get("/api/car/compare").param("carIds", "1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cars.length()").value(3));
    }

    @Test
    void userCompareRequiresUserTokenOnly() throws Exception {
        mockMvc.perform(get("/api/user/compare"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/user/compare").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    private String userToken() throws Exception {
        return login("/api/auth/user/login", "demo_user", "demo123456").andReturnData().path("token").asText();
    }

    private String adminToken() throws Exception {
        return login("/api/auth/admin/login", "demo_admin", "admin123456").andReturnData().path("token").asText();
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

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
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
