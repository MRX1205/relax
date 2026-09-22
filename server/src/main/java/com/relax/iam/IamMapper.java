package com.relax.iam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IamMapper {

    @Select("SELECT id, code, name FROM iam_permission_group WHERE status = 'ENABLED' ORDER BY id")
    List<PermissionGroup> findPermissionGroups();

    @Select("SELECT id, code, name FROM iam_permission WHERE id IN "
            + "(SELECT permission_id FROM iam_group_permission WHERE group_id = #{groupId}) ORDER BY id")
    List<PermissionSummary> findGroupPermissions(@Param("groupId") long groupId);

    @Select("SELECT id, wechat_open_id AS wechatOpenId, nickname, phone, status "
            + "FROM platform_user WHERE id = #{userId}")
    Optional<AccessUser> findAccessUser(@Param("userId") long userId);

    @Select("SELECT id, wechat_open_id AS wechatOpenId, nickname, phone, status FROM platform_user "
            + "WHERE phone LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%') "
            + "ORDER BY id DESC LIMIT 20")
    List<AccessUser> searchAccessUsers(@Param("keyword") String keyword);

    @Select("SELECT id, wechat_open_id AS wechatOpenId, nickname, phone, status FROM platform_user "
            + "ORDER BY id DESC LIMIT 20")
    List<AccessUser> listAccessUsers();

    @Select("SELECT u.id, u.wechat_open_id AS wechatOpenId, u.nickname, u.phone, u.status "
            + "FROM platform_user u JOIN iam_user_role ur ON ur.user_id = u.id JOIN iam_role r ON r.id = ur.role_id "
            + "WHERE r.code IN ('ADMIN', 'SUPER_ADMIN') AND ur.status = 'ENABLED' "
            + "GROUP BY u.id, u.wechat_open_id, u.nickname, u.phone, u.status ORDER BY u.id DESC")
    List<AccessUser> listAdmins();

    @Select("SELECT code FROM iam_role r JOIN iam_user_role ur ON ur.role_id = r.id "
            + "WHERE ur.user_id = #{userId} AND ur.status = 'ENABLED' ORDER BY r.id")
    List<String> findRoles(@Param("userId") long userId);

    @Select("SELECT code FROM iam_permission_group g JOIN iam_user_permission_group ug ON ug.group_id = g.id "
            + "WHERE ug.user_id = #{userId} ORDER BY g.id")
    List<String> findGroups(@Param("userId") long userId);

    @Select("SELECT id FROM iam_permission_group WHERE code = #{code} AND status = 'ENABLED'")
    Optional<Long> findGroupId(@Param("code") String code);

    @Select("SELECT id FROM iam_role WHERE code = #{code} AND status = 'ENABLED'")
    Optional<Long> findRoleId(@Param("code") String code);

    @Select("SELECT COUNT(*) FROM iam_user_role ur JOIN iam_role r ON r.id = ur.role_id "
            + "WHERE ur.user_id = #{userId} AND r.code = #{roleCode} AND ur.status = 'ENABLED'")
    int countRole(@Param("userId") long userId, @Param("roleCode") String roleCode);

    @Select("SELECT COUNT(*) FROM iam_user_role ur JOIN iam_role r ON r.id = ur.role_id "
            + "WHERE ur.user_id = #{userId} AND r.code = #{roleCode}")
    int countRoleAssignment(@Param("userId") long userId, @Param("roleCode") String roleCode);

    @Insert("INSERT INTO iam_user_role (user_id, role_id, status, granted_by) "
            + "VALUES (#{userId}, #{roleId}, 'ENABLED', #{grantedBy})")
    void insertRole(@Param("userId") long userId, @Param("roleId") long roleId, @Param("grantedBy") long grantedBy);

    @Update("UPDATE iam_user_role SET status = #{status} WHERE user_id = #{userId} AND role_id = #{roleId}")
    void updateRoleStatus(@Param("userId") long userId, @Param("roleId") long roleId, @Param("status") String status);

    @Delete("DELETE FROM iam_user_permission_group WHERE user_id = #{userId}")
    void deleteUserGroups(@Param("userId") long userId);

    @Insert("INSERT INTO iam_user_permission_group (user_id, group_id, granted_by) "
            + "VALUES (#{userId}, #{groupId}, #{grantedBy})")
    void insertUserGroup(@Param("userId") long userId, @Param("groupId") long groupId, @Param("grantedBy") long grantedBy);

    @Update("UPDATE platform_user SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{userId}")
    void updateUserStatus(@Param("userId") long userId, @Param("status") String status);

    record PermissionGroup(long id, String code, String name) {
    }

    record PermissionSummary(long id, String code, String name) {
    }

    record AccessUser(long id, String wechatOpenId, String nickname, String phone, String status) {
    }
}
