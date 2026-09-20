package com.relax.iam;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.relax.audit.AuditService;
import com.relax.common.api.BusinessException;

@Service
public class IamService {

    private final IamMapper iamMapper;
    private final AuditService auditService;

    IamService(IamMapper iamMapper, AuditService auditService) {
        this.iamMapper = iamMapper;
        this.auditService = auditService;
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
