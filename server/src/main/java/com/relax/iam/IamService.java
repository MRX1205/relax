package com.relax.iam;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.audit.AuditService;
import com.relax.auth.AuthMapper;
import com.relax.auth.UserAccount;
import com.relax.common.api.BusinessException;

@Service
public class IamService {

    private final IamMapper iamMapper;
    private final AuditService auditService;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;

    IamService(IamMapper iamMapper, AuditService auditService, AuthMapper authMapper, PasswordEncoder passwordEncoder) {
        this.iamMapper = iamMapper;
        this.auditService = auditService;
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AccessUserView> findAdmins() {
        return iamMapper.listAdmins().stream().map(this::toView).toList();
    }

    public List<AccessUserView> findUsers(String keyword) {
        List<IamMapper.AccessUser> users = keyword == null || keyword.isBlank()
                ? iamMapper.listAccessUsers()
                : iamMapper.searchAccessUsers(keyword.strip());
        return users.stream().map(this::toView).toList();
    }

    public List<GroupView> findGroups() {
        return iamMapper.findPermissionGroups().stream()
                .map(group -> new GroupView(group.id(), group.code(), group.name(),
                        iamMapper.findGroupPermissions(group.id()).stream().map(IamMapper.PermissionSummary::code).toList()))
                .toList();
    }

    @Transactional
    public AccessUserView createAdmin(long operatorId, String phone, String password, String nickname,
            Set<String> groupCodes, String ipAddress) {
        if (phone == null || !phone.matches("1\\d{10}")) {
            throw new BusinessException("PHONE_INVALID", "手机号格式不正确，请输入11位手机号");
        }
        if (password == null || password.strip().length() < 6) {
            throw new BusinessException("PASSWORD_INVALID", "密码不能少于6位");
        }
        String cleanPhone = phone.strip();
        String passwordHash = passwordEncoder.encode(password.strip());
        long adminRoleId = iamMapper.findRoleId("ADMIN")
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "ROLE_NOT_FOUND", "ADMIN角色未定义"));

        Optional<UserAccount> userOpt = authMapper.findUserByPhone(cleanPhone);
        long userId;
        if (userOpt.isPresent()) {
            userId = userOpt.get().id();
            authMapper.updatePassword(userId, passwordHash);
            if (nickname != null && !nickname.isBlank()) {
                authMapper.updateProfile(userId, nickname.strip(), userOpt.get().avatarUrl());
            }
            if (iamMapper.countRoleAssignment(userId, "ADMIN") == 0) {
                iamMapper.insertRole(userId, adminRoleId, operatorId);
            } else {
                iamMapper.updateRoleStatus(userId, adminRoleId, "ENABLED");
            }
        } else {
            userId = IdWorker.getId();
            String name = (nickname != null && !nickname.isBlank()) ? nickname.strip() : "管理员" + cleanPhone.substring(7);
            authMapper.insertUser(userId, "admin:phone:" + cleanPhone, null);
            authMapper.updatePhone(userId, cleanPhone);
            authMapper.updateProfile(userId, name, null);
            authMapper.updatePassword(userId, passwordHash);
            authMapper.insertRole(userId, "USER", operatorId);
            iamMapper.insertRole(userId, adminRoleId, operatorId);
        }

        Set<String> normalizedGroups = groupCodes == null ? Set.of() : groupCodes.stream()
                .map(String::strip).filter(value -> !value.isBlank()).collect(Collectors.toSet());
        iamMapper.deleteUserGroups(userId);
        for (String groupCode : normalizedGroups) {
            iamMapper.findGroupId(groupCode).ifPresent(groupId -> {
                iamMapper.insertUserGroup(userId, groupId, operatorId);
            });
        }

        auditService.record(operatorId, "ADMIN_CREATED", "USER", Long.toString(userId),
                "phone=" + cleanPhone + ",groups=" + String.join(",", normalizedGroups), ipAddress);
        return findUser(userId);
    }

    @Transactional
    public void deleteAdmin(long operatorId, long targetUserId, String ipAddress) {
        if (operatorId == targetUserId || iamMapper.findRoles(targetUserId).contains("SUPER_ADMIN")) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "SUPER_ADMIN_PROTECTED", "不能删除超级管理员或当前操作人");
        }
        long adminRoleId = iamMapper.findRoleId("ADMIN")
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "ROLE_NOT_FOUND", "ADMIN角色未定义"));
        iamMapper.updateRoleStatus(targetUserId, adminRoleId, "DISABLED");
        iamMapper.deleteUserGroups(targetUserId);
        auditService.record(operatorId, "ADMIN_REVOKED", "USER", Long.toString(targetUserId), "revoked", ipAddress);
    }

    @Transactional
    public void resetAdminPassword(long operatorId, long targetUserId, String newPassword, String ipAddress) {
        if (newPassword == null || newPassword.strip().length() < 6) {
            throw new BusinessException("PASSWORD_INVALID", "密码不能少于6位");
        }
        if (operatorId != targetUserId && iamMapper.findRoles(targetUserId).contains("SUPER_ADMIN")) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "SUPER_ADMIN_PROTECTED", "不能重置超级管理员密码");
        }
        authMapper.updatePassword(targetUserId, passwordEncoder.encode(newPassword.strip()));
        auditService.record(operatorId, "ADMIN_PASSWORD_RESET", "USER", Long.toString(targetUserId), "reset", ipAddress);
    }

    @Transactional
    public AccessUserView updateAdminAccess(long operatorId, long targetUserId, boolean enabled,
            Set<String> groupCodes, String ipAddress) {
        IamMapper.AccessUser target = iamMapper.findAccessUser(targetUserId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "账号不存在"));
        if (operatorId == targetUserId || iamMapper.findRoles(targetUserId).contains("SUPER_ADMIN")) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "SUPER_ADMIN_PROTECTED", "不能修改超级管理员权限");
        }
        Set<String> normalizedGroups = groupCodes == null ? Set.of() : groupCodes.stream()
                .map(String::strip).filter(value -> !value.isBlank()).collect(java.util.stream.Collectors.toSet());
        List<IamMapper.PermissionGroup> available = iamMapper.findPermissionGroups();
        Set<String> availableCodes = available.stream().map(IamMapper.PermissionGroup::code).collect(java.util.stream.Collectors.toSet());
        if (!availableCodes.containsAll(normalizedGroups)) {
            throw new BusinessException("PERMISSION_GROUP_INVALID", "包含无效的权限组");
        }
        iamMapper.deleteUserGroups(targetUserId);
        for (String groupCode : normalizedGroups) {
            long groupId = iamMapper.findGroupId(groupCode).orElseThrow();
            iamMapper.insertUserGroup(targetUserId, groupId, operatorId);
        }
        long adminRoleId = iamMapper.findRoleId("ADMIN").orElseThrow();
        if (enabled) {
            if (iamMapper.countRoleAssignment(targetUserId, "ADMIN") == 0) {
                iamMapper.insertRole(targetUserId, adminRoleId, operatorId);
            } else {
                iamMapper.updateRoleStatus(targetUserId, adminRoleId, "ENABLED");
            }
        } else {
            iamMapper.updateRoleStatus(targetUserId, adminRoleId, "DISABLED");
        }
        auditService.record(operatorId, "ADMIN_ACCESS_UPDATED", "USER", Long.toString(targetUserId),
                "enabled=" + enabled + ",groups=" + String.join(",", normalizedGroups), ipAddress);
        return findUser(targetUserId);
    }

    @Transactional
    public AccessUserView updateStatus(long operatorId, long targetUserId, String status, String ipAddress) {
        IamMapper.AccessUser target = iamMapper.findAccessUser(targetUserId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "账号不存在"));
        if (operatorId == targetUserId || iamMapper.findRoles(targetUserId).contains("SUPER_ADMIN")) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "SUPER_ADMIN_PROTECTED", "不能修改超级管理员状态");
        }
        if (!Set.of("ACTIVE", "DISABLED").contains(status)) {
            throw new BusinessException("ACCOUNT_STATUS_INVALID", "账号状态无效");
        }
        iamMapper.updateUserStatus(targetUserId, status);
        auditService.record(operatorId, "ACCOUNT_STATUS_UPDATED", "USER", Long.toString(targetUserId),
                "status=" + status, ipAddress);
        return findUser(targetUserId);
    }

    public AccessUserView findUser(long userId) {
        return iamMapper.findAccessUser(userId).map(this::toView)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "账号不存在"));
    }

    private AccessUserView toView(IamMapper.AccessUser user) {
        return new AccessUserView(user.id(), user.nickname(), user.phone(), user.status(),
                iamMapper.findRoles(user.id()), iamMapper.findGroups(user.id()));
    }

    public record GroupView(long id, String code, String name, List<String> permissions) {
    }

    public record AccessUserView(long id, String nickname, String phone, String status,
            List<String> roles, List<String> permissionGroups) {
    }

    public record AdminAccessRequest(boolean enabled, Set<String> groupCodes) {
    }

    public record AccountStatusRequest(String status) {
    }
}
