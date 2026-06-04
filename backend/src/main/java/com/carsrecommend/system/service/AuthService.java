package com.carsrecommend.system.service;

import com.carsrecommend.system.auth.AuthPrincipal;
import com.carsrecommend.system.dto.LoginRequest;
import com.carsrecommend.system.dto.UserProfileUpdateRequest;
import com.carsrecommend.system.dto.UserRegisterRequest;
import com.carsrecommend.system.vo.AuthPrincipalVO;
import com.carsrecommend.system.vo.AuthTokenVO;
import com.carsrecommend.system.vo.UserProfileVO;

public interface AuthService {

    AuthTokenVO login(LoginRequest request);

    AuthTokenVO loginUser(LoginRequest request);

    AuthTokenVO loginAdmin(LoginRequest request);

    AuthTokenVO registerUser(UserRegisterRequest request);

    AuthPrincipalVO current(AuthPrincipal principal);

    UserProfileVO currentUserProfile(AuthPrincipal principal);

    UserProfileVO updateCurrentUserProfile(AuthPrincipal principal, UserProfileUpdateRequest request);
}
