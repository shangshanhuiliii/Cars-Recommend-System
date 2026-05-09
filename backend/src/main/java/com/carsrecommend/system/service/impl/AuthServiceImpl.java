package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.auth.AuthPrincipal;
import com.carsrecommend.system.auth.JwtTokenService;
import com.carsrecommend.system.auth.PasswordHasher;
import com.carsrecommend.system.auth.PrincipalType;
import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.dto.LoginRequest;
import com.carsrecommend.system.dto.UserRegisterRequest;
import com.carsrecommend.system.entity.Admin;
import com.carsrecommend.system.entity.AppUser;
import com.carsrecommend.system.mapper.AdminMapper;
import com.carsrecommend.system.mapper.AppUserMapper;
import com.carsrecommend.system.service.AuthService;
import com.carsrecommend.system.vo.AuthMenuVO;
import com.carsrecommend.system.vo.AuthPrincipalVO;
import com.carsrecommend.system.vo.AuthTokenVO;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class AuthServiceImpl implements AuthService {

    private static final String USER_ROLE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_STATUS_ACTIVE = "ACTIVE";
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

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
            "admin:users",
            "admin:favorites",
            "admin:feedbacks",
            "admin:recommend-records",
            "admin:dashboard",
            "admin:health",
            "admin:algorithm-demo");

    private static final List<AuthMenuVO> USER_MENUS = List.of(
            new AuthMenuVO("home", "首页", "/"),
            new AuthMenuVO("recommend", "购车推荐", "/recommend"),
            new AuthMenuVO("history", "推荐历史", "/history"),
            new AuthMenuVO("favorites", "我的收藏", "/favorites"),
            new AuthMenuVO("compare", "车型对比", "/compare"));

    private static final List<AuthMenuVO> ADMIN_MENUS = List.of(
            new AuthMenuVO("admin-cars", "车型管理", "/admin/cars"),
            new AuthMenuVO("admin-users", "用户管理", "/admin/users"),
            new AuthMenuVO("admin-favorites", "收藏车型", "/admin/favorites"),
            new AuthMenuVO("admin-feedbacks", "反馈记录", "/admin/feedbacks"),
            new AuthMenuVO("admin-recommend-records", "推荐记录", "/admin/recommend-records"),
            new AuthMenuVO("admin-dashboard", "运营概览", "/admin/dashboard"),
            new AuthMenuVO("admin-health", "健康检查", "/admin/health"),
            new AuthMenuVO("algorithm-demo", "算法可视化", "/algorithm-demo"));

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
    public AuthTokenVO login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        String password = normalizePassword(request.getPassword());
        AppUser user = appUserMapper.findByUsername(username).orElse(null);
        Admin admin = adminMapper.findActiveByUsername(username).orElse(null);
        boolean userMatched = user != null && passwordHasher.matches(password, user.getPassword());
        boolean adminMatched = admin != null && passwordHasher.matches(password, admin.getPassword());
        if (userMatched && adminMatched) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "账号同时存在普通用户和管理员，请联系管理员处理");
        }
        if (userMatched) {
            if (!USER_STATUS_ACTIVE.equals(user.getStatus())) {
                throw invalidCredentials();
            }
            return toTokenVO(toUserPrincipal(user));
        }
        if (adminMatched) {
            return toTokenVO(toAdminPrincipal(admin));
        }
        throw invalidCredentials();
    }

    @Override
    public AuthTokenVO loginUser(LoginRequest request) {
        AppUser user = appUserMapper.findActiveByUsername(request.getUsername().trim())
                .orElseThrow(this::invalidCredentials);
        if (!passwordHasher.matches(request.getPassword(), user.getPassword())) {
            throw invalidCredentials();
        }
        return toTokenVO(toUserPrincipal(user));
    }

    @Override
    public AuthTokenVO loginAdmin(LoginRequest request) {
        Admin admin = adminMapper.findActiveByUsername(request.getUsername().trim())
                .orElseThrow(this::invalidCredentials);
        if (!passwordHasher.matches(request.getPassword(), admin.getPassword())) {
            throw invalidCredentials();
        }
        return toTokenVO(toAdminPrincipal(admin));
    }

    private AuthPrincipal toAdminPrincipal(Admin admin) {
        String role = StringUtils.hasText(admin.getRole()) ? admin.getRole().trim() : ADMIN_ROLE;
        if (!ADMIN_ROLE.equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin role is not allowed");
        }
        return new AuthPrincipal(
                admin.getId(),
                PrincipalType.ADMIN,
                admin.getUsername(),
                role,
                admin.getUsername());
    }

    private AuthPrincipal toUserPrincipal(AppUser user) {
        return new AuthPrincipal(
                user.getId(),
                PrincipalType.USER,
                user.getUsername(),
                USER_ROLE,
                StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
    }

    @Override
    public AuthTokenVO registerUser(UserRegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        String password = normalizePassword(request.getPassword());
        String email = normalizeEmail(request.getEmail());
        validateRegisterRequest(request, username, password, email);
        if (appUserMapper.existsByUsername(username) || adminMapper.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在");
        }
        if (appUserMapper.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱已存在");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordHasher.hash(password));
        user.setNickname(normalizeNickname(request.getNickname(), username));
        user.setEmail(email);
        user.setPhone(normalizePhone(request.getPhone()));
        user.setStatus(USER_STATUS_ACTIVE);
        try {
            appUserMapper.insert(user);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名或邮箱已存在");
        }

        return toTokenVO(toUserPrincipal(user));
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

    private void validateRegisterRequest(UserRegisterRequest request, String username, String password, String email) {
        if (username.length() < 4 || username.length() > 32 || !username.matches("[A-Za-z0-9_]+")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名必须为4-32位字母、数字或下划线");
        }
        if (password.length() < 8 || password.length() > 32
                || password.chars().noneMatch(Character::isLetter)
                || password.chars().noneMatch(Character::isDigit)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码必须为8-32位且至少包含字母和数字");
        }
        if (!password.equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "两次输入的密码不一致");
        }
        String nickname = request.getNickname();
        if (StringUtils.hasText(nickname) && nickname.trim().length() > 32) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "昵称最多32个字符");
        }
        if (!StringUtils.hasText(email) || email.length() > 128 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱格式不正确");
        }
        String phone = normalizePhone(request.getPhone());
        if (phone != null && !phone.matches("1\\d{10}") && !phone.matches("\\d{11,}")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号格式不正确");
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private String normalizePassword(String password) {
        return password == null ? "" : password;
    }

    private String normalizeNickname(String nickname, String username) {
        return StringUtils.hasText(nickname) ? nickname.trim() : username;
    }

    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : "";
    }

    private String normalizePhone(String phone) {
        return StringUtils.hasText(phone) ? phone.trim() : null;
    }
}
