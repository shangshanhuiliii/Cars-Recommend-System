package com.carsrecommend.system.config;

import com.carsrecommend.system.auth.AuthContext;
import com.carsrecommend.system.auth.AuthPrincipal;
import com.carsrecommend.system.auth.JwtTokenService;
import com.carsrecommend.system.auth.PrincipalType;
import com.carsrecommend.system.common.ApiResponse;
import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@ConditionalOnBean(JwtTokenService.class)
public class AuthInterceptor implements HandlerInterceptor {

    private static final List<String> PUBLIC_GET_PATTERNS = List.of(
            "/api/health",
            "/api/car/**",
            "/uploads/**");
    private static final List<String> PUBLIC_POST_PATTERNS = List.of(
            "/api/auth/user/login",
            "/api/auth/admin/login");
    private static final List<String> USER_PATTERNS = List.of(
            "/api/user/demand",
            "/api/user/demand/**",
            "/api/user/favorites",
            "/api/user/favorites/**",
            "/api/recommend/generate",
            "/api/recommend/history",
            "/api/recommend/*",
            "/api/recommend/*/feedback");
    private static final List<String> ADMIN_PATTERNS = List.of(
            "/api/admin/**",
            "/api/recommend/*/algorithm-visualization");
    private static final List<String> AUTHENTICATED_PATTERNS = List.of(
            "/api/auth/me",
            "/api/auth/logout");

    private final AuthProperties authProperties;
    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthInterceptor(AuthProperties authProperties, JwtTokenService jwtTokenService, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        AuthContext.clear();
        if (!authProperties.isEnabled() || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (isPublic(request.getMethod(), path)) {
            return true;
        }

        PrincipalType requiredType = requiredPrincipalType(path);
        boolean requiresAuthentication = requiredType != null || matchesAny(path, AUTHENTICATED_PATTERNS);
        if (!requiresAuthentication) {
            return true;
        }

        AuthPrincipal principal;
        try {
            principal = jwtTokenService.parse(resolveBearerToken(request));
        } catch (BusinessException exception) {
            writeFailure(response, ErrorCode.UNAUTHORIZED, exception.getMessage());
            return false;
        }
        if (requiredType != null && principal.principalType() != requiredType) {
            writeFailure(response, ErrorCode.FORBIDDEN, "permission denied");
            return false;
        }
        AuthContext.set(principal);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private boolean isPublic(String method, String path) {
        if ("GET".equalsIgnoreCase(method) && matchesAny(path, PUBLIC_GET_PATTERNS)) {
            return true;
        }
        return "POST".equalsIgnoreCase(method) && matchesAny(path, PUBLIC_POST_PATTERNS);
    }

    private PrincipalType requiredPrincipalType(String path) {
        if (matchesAny(path, ADMIN_PATTERNS)) {
            return PrincipalType.ADMIN;
        }
        if (matchesAny(path, USER_PATTERNS)) {
            return PrincipalType.USER;
        }
        return null;
    }

    private boolean matchesAny(String path, List<String> patterns) {
        return patterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "missing bearer token");
        }
        return authorization.substring("Bearer ".length()).trim();
    }

    private void writeFailure(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(code, message));
    }
}
