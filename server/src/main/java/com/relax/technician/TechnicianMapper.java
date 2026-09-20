package com.relax.technician;

import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TechnicianMapper {

    @Select("SELECT id, user_id AS userId, service_name AS serviceName, real_name AS realName, "
            + "phone, intro, experience_years AS experienceYears, status, created_at AS createdAt "
            + "FROM technician_application WHERE user_id = #{userId} ORDER BY id DESC LIMIT 1")
    Optional<ApplicationView> findLatestApplication(@Param("userId") long userId);

    @Insert("INSERT INTO technician_application (id, user_id, service_name, real_name, phone, intro, "
            + "experience_years, service_area_codes, photo_file_id, certificate_file_id) "
            + "VALUES (#{id}, #{userId}, #{serviceName}, #{realName}, #{phone}, #{intro}, "
            + "#{experienceYears}, #{serviceAreaCodes}, #{photoFileId}, #{certificateFileId})")
    void insertApplication(@Param("id") long id, @Param("userId") long userId,
            @Param("serviceName") String serviceName, @Param("realName") String realName,
            @Param("phone") String phone, @Param("intro") String intro,
            @Param("experienceYears") int experienceYears, @Param("serviceAreaCodes") String serviceAreaCodes,
            @Param("photoFileId") Long photoFileId, @Param("certificateFileId") Long certificateFileId);

    record ApplicationView(long id, long userId, String serviceName, String realName,
            String phone, String intro, int experienceYears, String status, LocalDateTime createdAt) {
    }
}
