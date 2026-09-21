package com.relax.technician;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TechnicianPhotoMapper {

    @Select("SELECT id, technician_id AS technicianId, file_id AS fileId, file_url AS fileUrl, "
            + "photo_type AS photoType, sort, audit_status AS auditStatus, created_at AS createdAt "
            + "FROM technician_photo WHERE technician_id = #{technicianId} ORDER BY sort, id")
    List<PhotoView> findByTechnician(@Param("technicianId") long technicianId);

    @Insert("INSERT INTO technician_photo (id, technician_id, file_id, file_url, photo_type, sort, audit_status) "
            + "VALUES (#{id}, #{technicianId}, #{fileId}, #{fileUrl}, #{photoType}, #{sort}, 'APPROVED')")
    void insert(@Param("id") long id, @Param("technicianId") long technicianId,
            @Param("fileId") long fileId, @Param("fileUrl") String fileUrl,
            @Param("photoType") String photoType, @Param("sort") int sort);

    @Delete("DELETE FROM technician_photo WHERE id = #{id} AND technician_id = #{technicianId}")
    int delete(@Param("id") long id, @Param("technicianId") long technicianId);

    record PhotoView(long id, long technicianId, long fileId, String fileUrl,
            String photoType, int sort, String auditStatus, LocalDateTime createdAt) {
    }
}
