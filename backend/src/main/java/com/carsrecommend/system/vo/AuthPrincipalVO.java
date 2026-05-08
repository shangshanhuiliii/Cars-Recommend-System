package com.carsrecommend.system.vo;

import java.util.List;

public class AuthPrincipalVO {

    private Long id;
    private String username;
    private String displayName;
    private String principalType;
    private String role;
    private List<String> permissions;
    private List<AuthMenuVO> menus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPrincipalType() {
        return principalType;
    }

    public void setPrincipalType(String principalType) {
        this.principalType = principalType;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<AuthMenuVO> getMenus() {
        return menus;
    }

    public void setMenus(List<AuthMenuVO> menus) {
        this.menus = menus;
    }
}
