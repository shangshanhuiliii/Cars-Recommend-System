package com.carsrecommend.system.vo;

import java.time.LocalDateTime;

public class AuthTokenVO {

    private String token;
    private String tokenType;
    private LocalDateTime expiresAt;
    private AuthPrincipalVO principal;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public AuthPrincipalVO getPrincipal() {
        return principal;
    }

    public void setPrincipal(AuthPrincipalVO principal) {
        this.principal = principal;
    }
}
