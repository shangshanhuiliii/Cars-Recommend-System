package com.carsrecommend.system.auth;

import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import java.util.Optional;

public final class AuthContext {

    private static final ThreadLocal<AuthPrincipal> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthPrincipal principal) {
        CURRENT.set(principal);
    }

    public static Optional<AuthPrincipal> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static Long currentUserIdOrNull() {
        AuthPrincipal principal = CURRENT.get();
        return principal != null && principal.principalType() == PrincipalType.USER ? principal.id() : null;
    }

    public static Long currentAdminIdOrNull() {
        AuthPrincipal principal = CURRENT.get();
        return principal != null && principal.principalType() == PrincipalType.ADMIN ? principal.id() : null;
    }

    public static Long requireUserId() {
        AuthPrincipal principal = CURRENT.get();
        if (principal == null || principal.principalType() != PrincipalType.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要普通用户身份");
        }
        return principal.id();
    }

    public static Long requireAdminId() {
        AuthPrincipal principal = CURRENT.get();
        if (principal == null || principal.principalType() != PrincipalType.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要管理员身份");
        }
        return principal.id();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
