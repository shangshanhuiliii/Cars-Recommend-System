package com.carsrecommend.system.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void userLoginReturnsTokenPrincipalMenusAndPermissions() throws Exception {
        JsonNode data = login("/api/auth/user/login", "demo_user", "demo123456")
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
        login("/api/auth/admin/login", "demo_admin", "admin123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.principal.principalType").value("ADMIN"))
                .andExpect(jsonPath("$.data.principal.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.principal.permissions[0]").value("admin:cars"))
                .andExpect(jsonPath("$.data.principal.menus[1].code").value("admin-cars"));
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {
        login("/api/auth/user/login", "demo_user", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void protectedEndpointsEnforceAuthenticationAndRoles() throws Exception {
        mockMvc.perform(get("/api/user/demand/latest"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        String userToken = login("/api/auth/user/login", "demo_user", "demo123456")
                .andReturnData()
                .path("token")
                .asText();
        String adminToken = login("/api/auth/admin/login", "demo_admin", "admin123456")
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

    private String expiredToken() throws Exception {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", "1");
        payload.put("id", 1);
        payload.put("principalType", "USER");
        payload.put("username", "demo_user");
        payload.put("role", "USER");
        payload.put("displayName", "demo_user");
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
