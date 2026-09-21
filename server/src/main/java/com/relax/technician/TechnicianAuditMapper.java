package com.relax.technician;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TechnicianAuditMapper {

    @Select("SELECT t.id, t.user_id AS userId, u.nickname, t.service_name AS serviceName, t.real_name AS realName, "
            + "t.phone, t.status, t.online_status AS onlineStatus, t.created_at AS createdAt "
            + "FROM technician t JOIN platform_user u ON u.id = t.user_id ORDER BY t.id DESC")
    List<TechnicianBrief> findAllTechnicians();

    @Select("SELECT t.id, t.user_id AS userId, u.nickname, t.service_name AS serviceName, t.real_name AS realName, "
            + "t.phone, t.status, t.online_status AS onlineStatus, t.created_at AS createdAt "
            + "FROM technician t JOIN platform_user u ON u.id = t.user_id WHERE t.status = #{status} ORDER BY t.id DESC")
    List<TechnicianBrief> findTechniciansByStatus(@Param("status") String status);

    @Select("SELECT t.id, t.user_id AS userId, u.nickname, t.service_name AS serviceName, t.real_name AS realName, "
            + "t.phone, t.intro, t.experience_years AS experienceYears, t.status, t.online_status AS onlineStatus, "
            + "COALESCE(t.avatar_url, u.avatar_url) AS avatarUrl, t.age, t.age_tag AS ageTag, t.height, t.weight, "
            + "t.latitude, t.longitude, t.base_address AS baseAddress, t.certifications_json AS certificationsJson, "
            + "t.created_at AS createdAt "
            + "FROM technician t JOIN platform_user u ON u.id = t.user_id WHERE t.user_id = #{userId}")
    Optional<TechnicianFullProfile> findFullProfileByUserId(@Param("userId") long userId);

    @Select("SELECT t.id, t.user_id AS userId, u.nickname, t.service_name AS serviceName, t.real_name AS realName, "
            + "t.phone, t.intro, t.experience_years AS experienceYears, t.status, t.online_status AS onlineStatus, "
            + "COALESCE(t.avatar_url, u.avatar_url) AS avatarUrl, t.age, t.age_tag AS ageTag, t.height, t.weight, "
            + "t.latitude, t.longitude, t.base_address AS baseAddress, t.certifications_json AS certificationsJson, "
            + "t.created_at AS createdAt "
            + "FROM technician t JOIN platform_user u ON u.id = t.user_id WHERE t.id = #{id}")
    Optional<TechnicianFullProfile> findFullProfileById(@Param("id") long id);

    @Select("SELECT id, user_id AS userId, service_name AS serviceName, real_name AS realName, "
            + "phone, intro, experience_years AS experienceYears, status, created_at AS createdAt "
            + "FROM technician_application WHERE id = #{id}")
    Optional<TechnicianApplicationView> findApplicationById(@Param("id") long id);

    @Select("SELECT id, user_id AS userId, service_name AS serviceName, real_name AS realName, "
            + "phone, intro, experience_years AS experienceYears, status, reject_reason AS rejectReason, created_at AS createdAt "
            + "FROM technician_application WHERE status = 'PENDING' ORDER BY id DESC")
    List<TechnicianApplicationView> findPendingApplications();

    @Update("UPDATE technician_application SET status = #{status}, reject_reason = #{rejectReason}, "
            + "reviewer_id = #{reviewerId}, reviewed_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int reviewApplication(@Param("id") long id, @Param("status") String status,
            @Param("rejectReason") String rejectReason, @Param("reviewerId") long reviewerId);

    @Insert("INSERT INTO technician (id, user_id, service_name, real_name, phone, intro, experience_years, status) "
            + "VALUES (#{id}, #{userId}, #{serviceName}, #{realName}, #{phone}, #{intro}, #{experienceYears}, 'ACTIVE')")
    void insertTechnician(@Param("id") long id, @Param("userId") long userId,
            @Param("serviceName") String serviceName, @Param("realName") String realName,
            @Param("phone") String phone, @Param("intro") String intro, @Param("experienceYears") int experienceYears);

    @Update("UPDATE technician SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateTechnicianStatus(@Param("id") long id, @Param("status") String status);

    @Update("UPDATE technician SET online_status = #{onlineStatus}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateOnlineStatus(@Param("id") long id, @Param("onlineStatus") String onlineStatus);

    @Update("UPDATE technician SET service_name = #{serviceName}, phone = #{phone}, intro = #{intro}, "
            + "experience_years = #{experienceYears}, avatar_url = #{avatarUrl}, age = #{age}, age_tag = #{ageTag}, "
            + "height = #{height}, weight = #{weight}, latitude = #{latitude}, longitude = #{longitude}, "
            + "base_address = #{baseAddress}, certifications_json = #{certificationsJson}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id}")
    int updateFullProfile(@Param("id") long id, @Param("serviceName") String serviceName,
            @Param("phone") String phone, @Param("intro") String intro,
            @Param("experienceYears") int experienceYears, @Param("avatarUrl") String avatarUrl,
            @Param("age") Integer age, @Param("ageTag") String ageTag,
            @Param("height") Integer height, @Param("weight") Integer weight,
            @Param("latitude") BigDecimal latitude, @Param("longitude") BigDecimal longitude,
            @Param("baseAddress") String baseAddress, @Param("certificationsJson") String certificationsJson);

    record TechnicianBrief(long id, long userId, String nickname, String serviceName, String realName,
            String phone, String status, String onlineStatus, LocalDateTime createdAt) {
    }

    record TechnicianFullProfile(long id, long userId, String nickname, String serviceName, String realName,
            String phone, String intro, int experienceYears, String status, String onlineStatus,
            String avatarUrl, Integer age, String ageTag, Integer height, Integer weight,
            BigDecimal latitude, BigDecimal longitude, String baseAddress, String certificationsJson,
            LocalDateTime createdAt) {
    }

    record TechnicianApplicationView(long id, long userId, String serviceName, String realName,
            String phone, String intro, int experienceYears, String status, String rejectReason, LocalDateTime createdAt) {
    }
}
