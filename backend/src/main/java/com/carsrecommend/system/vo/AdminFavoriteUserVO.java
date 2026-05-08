package com.carsrecommend.system.vo;

import java.time.LocalDateTime;

public class AdminFavoriteUserVO {

    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private String status;
    private LocalDateTime favoriteTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getFavoriteTime() {
        return favoriteTime;
    }

    public void setFavoriteTime(LocalDateTime favoriteTime) {
        this.favoriteTime = favoriteTime;
    }
}
