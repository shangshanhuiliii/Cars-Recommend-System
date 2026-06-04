package com.carsrecommend.system.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
        "app.auth.jwt-secret=auth-controller-test-secret-keep-at-least-32-bytes",
        "app.auth.token-expire-seconds=7200",
        "spring.datasource.url=jdbc:h2:mem:cars_auth;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
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
class AuthControllerTest {

    private static final String JWT_SECRET = "auth-controller-test-secret-keep-at-least-32-bytes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void userLoginReturnsTokenPrincipalMenusAndPermissions() throws Exception {
        JsonNode data = login("/api/auth/user/login", "user", "user123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresAt").isString())
                .andExpect(jsonPath("$.data.principal.principalType").value("USER"))
                .andExpect(jsonPath("$.data.principal.role").value("USER"))
                .andExpect(jsonPath("$.data.principal.permissions[0]").value("user:demand"))
                .andExpect(jsonPath("$.data.principal.menus[0].code").value("home"))
                .andReturnData();

        String token = data.path("token").asText();
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.principalType").value("USER"))
                .andExpect(jsonPath("$.data.menus[1].code").value("recommend"));
    }

    @Test
    void adminLoginReturnsTokenPrincipalMenusAndPermissions() throws Exception {
        login("/api/auth/admin/login", "admin", "admin123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.principal.principalType").value("ADMIN"))
                .andExpect(jsonPath("$.data.principal.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.principal.permissions[0]").value("admin:cars"))
                .andExpect(jsonPath("$.data.principal.permissions[?(@=='admin:favorites')]").exists())
                .andExpect(jsonPath("$.data.principal.permissions[?(@=='admin:feedbacks')]").exists())
                .andExpect(jsonPath("$.data.principal.menus[0].code").value("admin-cars"))
                .andExpect(jsonPath("$.data.principal.menus[1].code").value("admin-users"))
                .andExpect(jsonPath("$.data.principal.menus[2].code").value("admin-favorites"))
                .andExpect(jsonPath("$.data.principal.menus[3].code").value("admin-feedbacks"))
                .andExpect(jsonPath("$.data.principal.menus[?(@.code=='home')]").doesNotExist());
    }

    @Test
    void unifiedLoginReturnsUserPrincipal() throws Exception {
        login("/api/auth/login", "user", "user123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.principal.principalType").value("USER"))
                .andExpect(jsonPath("$.data.principal.role").value("USER"))
                .andExpect(jsonPath("$.data.principal.menus[0].code").value("home"));
    }

    @Test
    void unifiedLoginReturnsAdminPrincipal() throws Exception {
        login("/api/auth/login", "admin", "admin123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.principal.principalType").value("ADMIN"))
                .andExpect(jsonPath("$.data.principal.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.principal.menus[0].code").value("admin-cars"))
                .andExpect(jsonPath("$.data.principal.menus[?(@.code=='home')]").doesNotExist());
    }

    @Test
    void registerUserReturnsTokenAndUserPrincipal() throws Exception {
        register("register_success_user", "User123456", "User123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.principal.principalType").value("USER"))
                .andExpect(jsonPath("$.data.principal.role").value("USER"))
                .andExpect(jsonPath("$.data.principal.menus[1].code").value("recommend"));
        String email = jdbcTemplate.queryForObject(
                "SELECT email FROM app_user WHERE username = ?",
                String.class,
                "register_success_user");
        org.junit.jupiter.api.Assertions.assertEquals("register_success_user@example.test", email);
    }

    @Test
    void userCanReadAndUpdateOwnProfile() throws Exception {
        String token = register("profile_update_user", "User123456", "User123456", "profile_update_user@example.test")
                .andReturnData()
                .path("token")
                .asText();

        mockMvc.perform(get("/api/auth/profile").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("profile_update_user"))
                .andExpect(jsonPath("$.data.email").isString());

        String payload = """
                {
                  "nickname": "Profile User",
                  "email": "profile_user@example.test",
                  "phone": "13900000000"
                }
                """;
        mockMvc.perform(put("/api/auth/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("Profile User"))
                .andExpect(jsonPath("$.data.email").value("profile_user@example.test"))
                .andExpect(jsonPath("$.data.phone").value("13900000000"));

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Profile User"));
    }

    @Test
    void updateProfileValidatesEmailAndRole() throws Exception {
        String userToken = register("profile_invalid_user", "User123456", "User123456", "profile_invalid_user@example.test")
                .andReturnData()
                .path("token")
                .asText();
        String adminToken = login("/api/auth/admin/login", "admin", "admin123456")
                .andReturnData()
                .path("token")
                .asText();

        mockMvc.perform(put("/api/auth/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Bad Email\",\"email\":\"bad-email\",\"phone\":\"13900000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/api/auth/profile").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void registeredUserCanLogin() throws Exception {
        register("register_login_user", "User123456", "User123456")
                .andExpect(status().isOk());

        login("/api/auth/user/login", "register_login_user", "User123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.principal.username").value("register_login_user"));
    }

    @Test
    void duplicateUsernameRegisterReturnsBadRequest() throws Exception {
        register("user", "User123456", "User123456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void registerCannotUseExistingAdminUsername() throws Exception {
        register("admin", "User123456", "User123456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void invalidRegisterFieldsReturnBadRequest() throws Exception {
        register("short_password_user", "User12", "User12")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        register("mismatch_password_user", "User123456", "User654321")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        register("bad-name", "User123456", "User123456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void invalidRegisterEmailReturnsBadRequest() throws Exception {
        register("bad_email_user", "User123456", "User123456", "not-an-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void duplicateRegisterEmailReturnsBadRequest() throws Exception {
        register("email_one_user", "User123456", "User123456", "shared_email@example.test")
                .andExpect(status().isOk());

        register("email_two_user", "User123456", "User123456", "shared_email@example.test")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        register("disabled_login_user", "User123456", "User123456")
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE app_user SET status = 'DISABLED' WHERE username = ?", "disabled_login_user");

        login("/api/auth/user/login", "disabled_login_user", "User123456")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        login("/api/auth/login", "disabled_login_user", "User123456")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {
        login("/api/auth/user/login", "user", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        login("/api/auth/login", "user", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void protectedEndpointsEnforceAuthenticationAndRoles() throws Exception {
        mockMvc.perform(get("/api/user/demand/latest"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        String userToken = login("/api/auth/user/login", "user", "user123456")
                .andReturnData()
                .path("token")
                .asText();
        String adminToken = login("/api/auth/admin/login", "admin", "admin123456")
                .andReturnData()
                .path("token")
                .asText();

        mockMvc.perform(get("/api/admin/stat/overview").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/user/demand")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void unmatchedApiEndpointsFailClosed() throws Exception {
        mockMvc.perform(get("/api/not-exist-with-auth-rule"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        String userToken = login("/api/auth/user/login", "user", "user123456")
                .andReturnData()
                .path("token")
                .asText();
        String adminToken = login("/api/auth/admin/login", "admin", "admin123456")
                .andReturnData()
                .path("token")
                .asText();

        mockMvc.perform(get("/api/not-exist-with-auth-rule").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/api/not-exist-with-auth-rule").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void invalidOrExpiredTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/demand/latest").header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/user/demand/latest").header(HttpHeaders.AUTHORIZATION, bearer(expiredToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void publicEndpointsAllowAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/car/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.carModel.id").value(1));
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

    private ResultActionsWithData register(String username, String password, String confirmPassword) throws Exception {
        return register(username, password, confirmPassword, username + "@example.test");
    }

    private ResultActionsWithData register(
            String username,
            String password,
            String confirmPassword,
            String email) throws Exception {
        String payload = """
                {
                  "username": "%s",
                  "password": "%s",
                  "confirmPassword": "%s",
                  "nickname": "%s",
                  "email": "%s",
                  "phone": "13800000000"
                }
                """.formatted(username, password, confirmPassword, username, email);
        MvcResult result = mockMvc.perform(post("/api/auth/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(payload))
                .andReturn();
        return new ResultActionsWithData(result);
    }

    private String expiredToken() throws Exception {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", "1");
        payload.put("id", 1);
        payload.put("principalType", "USER");
        payload.put("username", "user");
        payload.put("role", "USER");
        payload.put("displayName", "user");
        payload.put("iat", now - 7200);
        payload.put("exp", now - 60);
        String headerPart = base64Url(objectMapper.writeValueAsBytes(header));
        String payloadPart = base64Url(objectMapper.writeValueAsBytes(payload));
        String signature = sign(headerPart + "." + payloadPart);
        return headerPart + "." + payloadPart + "." + signature;
    }

    private String sign(String signingInput) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
