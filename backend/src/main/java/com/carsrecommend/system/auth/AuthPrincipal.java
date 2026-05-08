package com.carsrecommend.system.auth;

public class AuthPrincipal {

    private final Long id;
    private final PrincipalType principalType;
    private final String username;
    private final String role;
    private final String displayName;

    public AuthPrincipal(Long id, PrincipalType principalType, String username, String role, String displayName) {
        this.id = id;
        this.principalType = principalType;
        this.username = username;
        this.role = role;
        this.displayName = displayName;
    }

    public Long id() {
        return id;
    }

    public PrincipalType principalType() {
        return principalType;
    }

    public String username() {
        return username;
    }

    public String role() {
        return role;
    }

    public String displayName() {
        return displayName;
    }
}
