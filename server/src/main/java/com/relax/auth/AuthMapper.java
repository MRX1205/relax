package com.relax.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AuthMapper {

    String USER_COLUMNS = "id, wechat_open_id AS wechatOpenId, union_id AS unionId, nickname, "
            + "avatar_url AS avatarUrl, phone, status, last_role AS lastRole, password_hash AS passwordHash, created_at AS createdAt";

    @Select("SELECT " + USER_COLUMNS + " FROM platform_user WHERE wechat_open_id = #{openId}")
    Optional<UserAccount> findUserByOpenId(@Param("openId") String openId);

    @Select("SELECT " + USER_COLUMNS + " FROM platform_user WHERE id = #{id}")
    Optional<UserAccount> findUserById(@Param("id") long id);

    @Select("SELECT " + USER_COLUMNS + " FROM platform_user WHERE phone = #{phone}")
    Optional<UserAccount> findUserByPhone(@Param("phone") String phone);

    @Insert("INSERT INTO platform_user (id, wechat_open_id, union_id) VALUES (#{id}, #{openId}, #{unionId})")
    void insertUser(@Param("id") long id, @Param("openId") String openId, @Param("unionId") String unionId);

    @Insert("INSERT INTO auth_access_token (id, user_id, token_digest, expires_at) "
            + "VALUES (#{id}, #{userId}, #{digest}, #{expiresAt})")
    void insertToken(@Param("id") long id, @Param("userId") long userId,
            @Param("digest") String digest, @Param("expiresAt") LocalDateTime expiresAt);

    @Select("SELECT u.id, u.wechat_open_id AS wechatOpenId, u.union_id AS unionId, u.nickname, "
            + "u.avatar_url AS avatarUrl, u.phone, u.status, u.last_role AS lastRole, u.password_hash AS passwordHash, u.created_at AS createdAt "
            + "FROM auth_access_token t JOIN platform_user u ON u.id = t.user_id "
            + "WHERE t.token_digest = #{digest} AND t.revoked_at IS NULL AND t.expires_at > #{now}")
    Optional<UserAccount> findUserByValidToken(@Param("digest") String digest, @Param("now") LocalDateTime now);

    @Select("SELECT r.code FROM iam_user_role ur JOIN iam_role r ON r.id = ur.role_id "
            + "WHERE ur.user_id = #{userId} AND ur.status = 'ENABLED' AND r.status = 'ENABLED' ORDER BY r.id")
    List<String> findRoleCodes(@Param("userId") long userId);

    @Select("SELECT DISTINCT code FROM ("
            + "SELECT p.code FROM iam_user_role ur JOIN iam_role_permission rp ON rp.role_id = ur.role_id "
            + "JOIN iam_permission p ON p.id = rp.permission_id WHERE ur.user_id = #{userId} AND ur.status = 'ENABLED' "
            + "UNION ALL SELECT p.code FROM iam_user_permission_group ug "
            + "JOIN iam_permission_group g ON g.id = ug.group_id AND g.status = 'ENABLED' "
            + "JOIN iam_group_permission gp ON gp.group_id = ug.group_id "
            + "JOIN iam_permission p ON p.id = gp.permission_id WHERE ug.user_id = #{userId}"
            + ") permission_codes ORDER BY code")
    List<String> findPermissionCodes(@Param("userId") long userId);

    @Select("SELECT g.code FROM iam_user_permission_group ug JOIN iam_permission_group g ON g.id = ug.group_id "
            + "WHERE ug.user_id = #{userId} AND g.status = 'ENABLED' ORDER BY g.id")
    List<String> findGroupCodes(@Param("userId") long userId);

    @Select("SELECT COUNT(*) FROM iam_user_role ur JOIN iam_role r ON r.id = ur.role_id "
            + "WHERE ur.user_id = #{userId} AND r.code = #{roleCode} AND ur.status = 'ENABLED'")
    int countRole(@Param("userId") long userId, @Param("roleCode") String roleCode);

    @Insert("INSERT INTO iam_user_role (user_id, role_id, status, granted_by) "
            + "SELECT #{userId}, id, 'ENABLED', #{grantedBy} FROM iam_role WHERE code = #{roleCode}")
    void insertRole(@Param("userId") long userId, @Param("roleCode") String roleCode,
            @Param("grantedBy") Long grantedBy);

    @Update("UPDATE platform_user SET phone = #{phone}, updated_at = CURRENT_TIMESTAMP WHERE id = #{userId}")
    void updatePhone(@Param("userId") long userId, @Param("phone") String phone);

    @Update("UPDATE platform_user SET nickname = #{nickname}, avatar_url = #{avatarUrl}, "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = #{userId}")
    void updateProfile(@Param("userId") long userId, @Param("nickname") String nickname,
            @Param("avatarUrl") String avatarUrl);

    @Update("UPDATE platform_user SET last_role = #{role}, updated_at = CURRENT_TIMESTAMP WHERE id = #{userId}")
    void updateLastRole(@Param("userId") long userId, @Param("role") String role);

    @Update("UPDATE platform_user SET password_hash = #{passwordHash}, updated_at = CURRENT_TIMESTAMP WHERE id = #{userId}")
    void updatePassword(@Param("userId") long userId, @Param("passwordHash") String passwordHash);

    @Update("UPDATE platform_user SET wechat_open_id = #{openId}, union_id = #{unionId}, "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = #{userId}")
    void updateWechatOpenId(@Param("userId") long userId, @Param("openId") String openId,
            @Param("unionId") String unionId);

    @Update("UPDATE platform_user SET wechat_open_id = NULL, union_id = NULL, "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = #{userId}")
    void clearOpenId(@Param("userId") long userId);

    @Update("UPDATE auth_access_token SET revoked_at = CURRENT_TIMESTAMP "
            + "WHERE token_digest = #{digest} AND revoked_at IS NULL")
    void revokeToken(@Param("digest") String digest);
}
