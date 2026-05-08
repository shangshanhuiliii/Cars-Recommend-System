package com.carsrecommend.system.auth;

import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.config.AuthProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;

    public JwtTokenService(AuthProperties authProperties, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void validateSecret() {
        if (authProperties.isEnabled() && secretBytes().length < 32) {
            throw new IllegalStateException("app.auth.jwt-secret must be at least 32 bytes");
        }
    }

    public TokenIssueResult issue(AuthPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(authProperties.getTokenExpireSeconds());
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(principal.id()));
        payload.put("id", principal.id());
        payload.put("principalType", principal.principalType().name());
        payload.put("username", principal.username());
        payload.put("role", principal.role());
        payload.put("displayName", principal.displayName());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String signaturePart = sign(headerPart + "." + payloadPart);
        return new TokenIssueResult(
                headerPart + "." + payloadPart + "." + signaturePart,
                LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
    }

    public AuthPrincipal parse(String token) {
        if (!StringUtils.hasText(token)) {
            throw unauthorized("missing token");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw unauthorized("invalid token");
        }
        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw unauthorized("invalid token signature");
        }
        try {
            JsonNode header = objectMapper.readTree(URL_DECODER.decode(parts[0]));
            if (!"HS256".equals(header.path("alg").asText())) {
                throw unauthorized("unsupported token algorithm");
            }
            JsonNode payload = objectMapper.readTree(URL_DECODER.decode(parts[1]));
            long exp = payload.path("exp").asLong(0L);
            if (exp <= Instant.now().getEpochSecond()) {
                throw unauthorized("token expired");
            }
            Long id = payload.path("id").isNumber()
                    ? payload.path("id").asLong()
                    : Long.valueOf(payload.path("sub").asText());
            PrincipalType principalType = PrincipalType.valueOf(payload.path("principalType").asText());
            return new AuthPrincipal(
                    id,
                    principalType,
                    payload.path("username").asText(),
                    payload.path("role").asText(),
                    payload.path("displayName").asText());
        } catch (IllegalArgumentException | IOException exception) {
            throw unauthorized("invalid token");
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "failed to create token");
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes(), HMAC_ALGORITHM));
            return URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "failed to sign token");
        }
    }

    private byte[] secretBytes() {
        return authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
    }

    private BusinessException unauthorized(String message) {
        return new BusinessException(ErrorCode.UNAUTHORIZED, message);
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int diff = 0;
        for (int index = 0; index < leftBytes.length; index++) {
            diff |= leftBytes[index] ^ rightBytes[index];
        }
        return diff == 0;
    }

    public record TokenIssueResult(String token, LocalDateTime expiresAt) {
    }
}
