package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.auth.AuthPrincipal;
import com.carsrecommend.system.auth.JwtTokenService;
import com.carsrecommend.system.auth.PasswordHasher;
import com.carsrecommend.system.auth.PrincipalType;
import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.dto.LoginRequest;
import com.carsrecommend.system.entity.Admin;
import com.carsrecommend.system.entity.AppUser;
import com.carsrecommend.system.mapper.AdminMapper;
import com.carsrecommend.system.mapper.AppUserMapper;
import com.carsrecommend.system.service.AuthService;
import com.carsrecommend.system.vo.AuthMenuVO;
import com.carsrecommend.system.vo.AuthPrincipalVO;
import com.carsrecommend.system.vo.AuthTokenVO;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AuthServiceImpl implements AuthService {

    private static final String USER_ROLE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";

    private static final List<String> USER_PERMISSIONS = List.of(
            "user:demand",
            "user:recommend",
            "user:history",
            "user:favorites",
            "user:feedback",
            "user:compare");

    private static final List<String> ADMIN_PERMISSIONS = List.of(
            "admin:cars",
            "admin:car-images",
            "admin:recommend-records",
            "admin:dashboard",
            "admin:health",
            "admin:algorithm-demo");

    private static final List<AuthMenuVO> USER_MENUS = List.of(
            new AuthMenuVO("home", "Home", "/"),
            new AuthMenuVO("recommend", "Recommendation", "/recommend"),
            new AuthMenuVO("history", "History", "/history"),
            new AuthMenuVO("favorites", "Favorites", "/favorites"),
            new AuthMenuVO("compare", "Compare", "/compare"));

    private static final List<AuthMenuVO> ADMIN_MENUS = List.of(
            new AuthMenuVO("home", "Home", "/"),
            new AuthMenuVO("admin-cars", "Cars", "/admin/cars"),
            new AuthMenuVO("admin-recommend-records", "Recommendation Records", "/admin/recommend-records"),
            new AuthMenuVO("admin-dashboard", "Dashboard", "/admin/dashboard"),
            new AuthMenuVO("admin-health", "Health", "/admin/health"),
            new AuthMenuVO("algorithm-demo", "Algorithm Demo", "/algorithm-demo"));

    private final AppUserMapper appUserMapper;
    private final AdminMapper adminMapper;
    private final PasswordHasher passwordHasher;
    private final JwtTokenService jwtTokenService;

    public AuthServiceImpl(
            AppUserMapper appUserMapper,
            AdminMapper adminMapper,
            PasswordHasher passwordHasher,
            JwtTokenService jwtTokenService) {
        this.appUserMapper = appUserMapper;
        this.adminMapper = adminMapper;
        this.passwordHasher = passwordHasher;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public AuthTokenVO loginUser(LoginRequest request) {
        AppUser user = appUserMapper.findActiveByUsername(request.getUsername().trim())
                .orElseThrow(this::invalidCredentials);
        if (!passwordHasher.matches(request.getPassword(), user.getPassword())) {
            throw invalidCredentials();
        }
        AuthPrincipal principal = new AuthPrincipal(
                user.getId(),
                PrincipalType.USER,
                user.getUsername(),
                USER_ROLE,
                StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        return toTokenVO(principal);
    }

    @Override
    public AuthTokenVO loginAdmin(LoginRequest request) {
        Admin admin = adminMapper.findActiveByUsername(request.getUsername().trim())
                .orElseThrow(this::invalidCredentials);
        if (!passwordHasher.matches(request.getPassword(), admin.getPassword())) {
            throw invalidCredentials();
        }
        String role = StringUtils.hasText(admin.getRole()) ? admin.getRole().trim() : ADMIN_ROLE;
        if (!ADMIN_ROLE.equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin role is not allowed");
        }
        AuthPrincipal principal = new AuthPrincipal(
                admin.getId(),
                PrincipalType.ADMIN,
                admin.getUsername(),
                role,
                admin.getUsername());
        return toTokenVO(principal);
    }

    @Override
    public AuthPrincipalVO current(AuthPrincipal principal) {
        return toPrincipalVO(principal);
    }

    private AuthTokenVO toTokenVO(AuthPrincipal principal) {
        JwtTokenService.TokenIssueResult token = jwtTokenService.issue(principal);
        AuthTokenVO vo = new AuthTokenVO();
        vo.setToken(token.token());
        vo.setTokenType("Bearer");
        vo.setExpiresAt(token.expiresAt());
        vo.setPrincipal(toPrincipalVO(principal));
        return vo;
    }

    private AuthPrincipalVO toPrincipalVO(AuthPrincipal principal) {
        AuthPrincipalVO vo = new AuthPrincipalVO();
        vo.setId(principal.id());
        vo.setUsername(principal.username());
        vo.setDisplayName(principal.displayName());
        vo.setPrincipalType(principal.principalType().name());
        vo.setRole(principal.role());
        if (principal.principalType() == PrincipalType.ADMIN) {
            vo.setPermissions(ADMIN_PERMISSIONS);
            vo.setMenus(ADMIN_MENUS);
        } else {
            vo.setPermissions(USER_PERMISSIONS);
            vo.setMenus(USER_MENUS);
        }
        return vo;
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(ErrorCode.UNAUTHORIZED, "username or password is incorrect");
    }
}
