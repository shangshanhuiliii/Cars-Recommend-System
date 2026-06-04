package com.carsrecommend.system.dto;

import jakarta.validation.constraints.Size;

public class UserProfileUpdateRequest {

    @Size(max = 32)
    private String nickname;

    @Size(max = 128)
    private String email;

    @Size(max = 32)
    private String phone;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
