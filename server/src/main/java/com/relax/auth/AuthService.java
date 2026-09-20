package com.relax.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.common.api.BusinessException;
import com.relax.technician.TechnicianAuditMapper;

@Service
public class AuthService {

    private final AuthMapper authMapper;
    private final AuthProperties properties;
    private final TokenService tokenService;
    private final WechatGateway wechatGateway;
    private final TechnicianAuditMapper technicianAuditMapper;

    AuthService(AuthMapper authMapper, AuthProperties properties, TokenService tokenService,
            WechatGateway wechatGateway, TechnicianAuditMapper technicianAuditMapper) {
        this.authMapper = authMapper;
        this.properties = properties;
        this.tokenService = tokenService;
        this.wechatGateway = wechatGateway;
        this.technicianAuditMapper = technicianAuditMapper;
    }

    @Transactional
    public LoginResult login(String code) {
        WechatGateway.WechatIdentity identity = wechatGateway.exchangeLoginCode(code);
        UserAccount user = findOrCreate(identity);
        grantRoleIfMissing(user.id(), "USER", null);
        if (identity.openId().equals(properties.bootstrapSuperAdminOpenId())) {
            // Bootstrap super admin gets ALL roles for development convenience
            grantRoleIfMissing(user.id(), "TECHNICIAN", user.id());
            grantRoleIfMissing(user.id(), "ADMIN", user.id());
            grantRoleIfMissing(user.id(), "SUPER_ADMIN", user.id());
            // Ensure technician profile exists for the bootstrap super admin
            ensureTechnicianProfile(user.id(), user.nickname(), user.phone());
        }
        requireActive(user);
        TokenService.IssuedToken token = tokenService.issue(user.id());
        return new LoginResult(token.value(), token.expiresAt(), account(user.id()));
    }

    public AccountView account(long userId) {
        UserAccount user = authMapper.findUserById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "ACCOUNT_NOT_FOUND", "账号不存在"));
        return toView(user);
    }

    @Transactional
    public AccountView bindPhone(long userId, String phoneCode) {
        String phone = wechatGateway.exchangePhoneCode(phoneCode);
        try {
            authMapper.updatePhone(userId, phone);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("PHONE_ALREADY_BOUND", "该手机号已绑定其他账号");
        }
        return account(userId);
    }

    @Transactional
    public AccountView updateProfile(long userId, String nickname, String avatarUrl) {
        authMapper.updateProfile(userId, nickname.strip(), normalizeUrl(avatarUrl));
        return account(userId);
    }

    @Transactional
    public AccountView switchRole(long userId, String role) {
        Set<String> roles = Set.copyOf(authMapper.findRoleCodes(userId));
        if (!roles.contains(role)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ROLE_NOT_GRANTED", "当前账号未获得该身份");
        }
        authMapper.updateLastRole(userId, role);
        return account(userId);
    }

    public void logout(String rawToken) {
        tokenService.revoke(rawToken);
    }

    private UserAccount findOrCreate(WechatGateway.WechatIdentity identity) {
        return authMapper.findUserByOpenId(identity.openId()).orElseGet(() -> {
            try {
                long id = IdWorker.getId();
                authMapper.insertUser(id, identity.openId(), identity.unionId());
                return authMapper.findUserById(id).orElseThrow();
            } catch (DuplicateKeyException exception) {
                return authMapper.findUserByOpenId(identity.openId()).orElseThrow();
            }
        });
    }

    private void grantRoleIfMissing(long userId, String role, Long grantedBy) {
        if (authMapper.countRole(userId, role) == 0) {
            authMapper.insertRole(userId, role, grantedBy);
        }
    }

    private void ensureTechnicianProfile(long userId, String nickname, String phone) {
        boolean hasProfile = technicianAuditMapper.findAllTechnicians().stream()
                .anyMatch(t -> t.userId() == userId);
        if (!hasProfile) {
            long techId = IdWorker.getId();
            String name = (nickname != null && !nickname.isBlank()) ? nickname : "管理员技师";
            String phoneNum = (phone != null && !phone.isBlank()) ? phone : "13800000000";
            technicianAuditMapper.insertTechnician(techId, userId, name, name,
                    phoneNum, "平台管理员技师账号", 5);
        }
    }

    private void requireActive(UserAccount user) {
        if (!"ACTIVE".equals(user.status())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "账号已被停用");
        }
    }

    private AccountView toView(UserAccount user) {
        List<String> roles = authMapper.findRoleCodes(user.id());
        String lastRole = roles.contains(user.lastRole()) ? user.lastRole() : "USER";
        return new AccountView(user.id(), user.nickname(), user.avatarUrl(), user.phone(), user.status(), lastRole,
                roles, authMapper.findPermissionCodes(user.id()), authMapper.findGroupCodes(user.id()));
    }

    private String normalizeUrl(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public record LoginResult(String accessToken, LocalDateTime expiresAt, AccountView account) {
    }

    public record AccountView(long id, String nickname, String avatarUrl, String phone, String status,
            String lastRole, List<String> roles, List<String> permissions, List<String> permissionGroups) {
    }
}
