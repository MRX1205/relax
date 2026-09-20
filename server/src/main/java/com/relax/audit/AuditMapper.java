package com.relax.audit;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuditMapper {

    @Insert("INSERT INTO audit_log (id, operator_id, action, resource_type, resource_id, detail, ip_address) "
            + "VALUES (#{id}, #{operatorId}, #{action}, #{resourceType}, #{resourceId}, #{detail}, #{ipAddress})")
    void insert(@Param("id") long id, @Param("operatorId") Long operatorId, @Param("action") String action,
            @Param("resourceType") String resourceType, @Param("resourceId") String resourceId,
            @Param("detail") String detail, @Param("ipAddress") String ipAddress);

    @Select("SELECT id, operator_id AS operatorId, action, resource_type AS resourceType, resource_id AS resourceId, "
            + "detail, ip_address AS ipAddress, created_at AS createdAt FROM audit_log ORDER BY id DESC LIMIT 100")
    List<AuditEntry> findRecent();

    record AuditEntry(long id, Long operatorId, String action, String resourceType, String resourceId,
            String detail, String ipAddress, LocalDateTime createdAt) {
    }
}
