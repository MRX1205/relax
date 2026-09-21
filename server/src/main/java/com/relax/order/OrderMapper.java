package com.relax.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper {

    // === 订单 ===
    @Insert("INSERT INTO service_order (id, order_no, user_id, technician_id, project_id, status, service_date, start_time, end_time, note) "
            + "VALUES (#{id}, #{orderNo}, #{userId}, #{technicianId}, #{projectId}, 'PENDING_PAYMENT', #{serviceDate}, #{startTime}, #{endTime}, #{note})")
    void insertOrder(@Param("id") long id, @Param("orderNo") String orderNo,
            @Param("userId") long userId, @Param("technicianId") long technicianId,
            @Param("projectId") long projectId, @Param("serviceDate") LocalDate serviceDate,
            @Param("startTime") String startTime, @Param("endTime") String endTime,
            @Param("note") String note);

    @Select("SELECT id, order_no AS orderNo, user_id AS userId, technician_id AS technicianId, "
            + "project_id AS projectId, status, service_date AS serviceDate, start_time AS startTime, "
            + "end_time AS endTime, version, note, cancel_reason AS cancelReason, "
            + "created_at AS createdAt, updated_at AS updatedAt "
            + "FROM service_order WHERE order_no = #{orderNo}")
    Optional<OrderView> findByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT id, order_no AS orderNo, user_id AS userId, technician_id AS technicianId, "
            + "project_id AS projectId, status, service_date AS serviceDate, start_time AS startTime, "
            + "end_time AS endTime, version, note, cancel_reason AS cancelReason, "
            + "created_at AS createdAt, updated_at AS updatedAt "
            + "FROM service_order WHERE user_id = #{userId} ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<OrderView> findByUser(@Param("userId") long userId,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT id, order_no AS orderNo, user_id AS userId, technician_id AS technicianId, "
            + "project_id AS projectId, status, service_date AS serviceDate, start_time AS startTime, "
            + "end_time AS endTime, version, note, cancel_reason AS cancelReason, "
            + "created_at AS createdAt, updated_at AS updatedAt "
            + "FROM service_order WHERE technician_id = #{technicianId} ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<OrderView> findByTechnician(@Param("technicianId") long technicianId,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT id, order_no AS orderNo, user_id AS userId, technician_id AS technicianId, "
            + "project_id AS projectId, status, service_date AS serviceDate, start_time AS startTime, "
            + "end_time AS endTime, version, note, cancel_reason AS cancelReason, "
            + "created_at AS createdAt, updated_at AS updatedAt "
            + "FROM service_order ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<OrderView> findAll(@Param("limit") int limit, @Param("offset") int offset);

    // === 丰富信息列表查询 ===
    @Select("SELECT o.id, o.order_no AS orderNo, o.user_id AS userId, o.technician_id AS technicianId, "
            + "COALESCE(t.service_name, '专业技师') AS technicianName, "
            + "COALESCE(t.phone, '') AS technicianPhone, "
            + "COALESCE(t.avatar_url, '') AS technicianAvatarUrl, "
            + "o.project_id AS projectId, "
            + "COALESCE(ops.project_name, p.name, '理疗推拿服务') AS projectName, "
            + "COALESCE(ops.duration_minutes, p.duration_minutes, 60) AS durationMinutes, "
            + "COALESCE(oas.contact_name, u.nickname, '顾客') AS customerName, "
            + "COALESCE(oas.contact_phone, u.phone, '') AS customerPhone, "
            + "CONCAT(COALESCE(oas.region_name, ''), ' ', COALESCE(oas.detail, '')) AS serviceAddress, "
            + "oas.longitude, oas.latitude, "
            + "o.status, o.service_date AS serviceDate, o.start_time AS startTime, o.end_time AS endTime, "
            + "COALESCE(oa.payable_amount, 0) AS payableAmount, "
            + "COALESCE(oa.paid_amount, 0) AS paidAmount, "
            + "o.note, o.cancel_reason AS cancelReason, "
            + "o.created_at AS createdAt, o.updated_at AS updatedAt "
            + "FROM service_order o "
            + "LEFT JOIN technician t ON t.id = o.technician_id "
            + "LEFT JOIN service_project p ON p.id = o.project_id "
            + "LEFT JOIN platform_user u ON u.id = o.user_id "
            + "LEFT JOIN order_project_snapshot ops ON ops.order_id = o.id "
            + "LEFT JOIN order_address_snapshot oas ON oas.order_id = o.id "
            + "LEFT JOIN order_amount oa ON oa.order_id = o.id "
            + "WHERE o.user_id = #{userId} "
            + "ORDER BY o.id DESC LIMIT #{limit} OFFSET #{offset}")
    List<OrderListItem> findEnrichedByUser(@Param("userId") long userId,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT o.id, o.order_no AS orderNo, o.user_id AS userId, o.technician_id AS technicianId, "
            + "COALESCE(t.service_name, '专业技师') AS technicianName, "
            + "COALESCE(t.phone, '') AS technicianPhone, "
            + "COALESCE(t.avatar_url, '') AS technicianAvatarUrl, "
            + "o.project_id AS projectId, "
            + "COALESCE(ops.project_name, p.name, '理疗推拿服务') AS projectName, "
            + "COALESCE(ops.duration_minutes, p.duration_minutes, 60) AS durationMinutes, "
            + "COALESCE(oas.contact_name, u.nickname, '顾客') AS customerName, "
            + "COALESCE(oas.contact_phone, u.phone, '') AS customerPhone, "
            + "CONCAT(COALESCE(oas.region_name, ''), ' ', COALESCE(oas.detail, '')) AS serviceAddress, "
            + "oas.longitude, oas.latitude, "
            + "o.status, o.service_date AS serviceDate, o.start_time AS startTime, o.end_time AS endTime, "
            + "COALESCE(oa.payable_amount, 0) AS payableAmount, "
            + "COALESCE(oa.paid_amount, 0) AS paidAmount, "
            + "o.note, o.cancel_reason AS cancelReason, "
            + "o.created_at AS createdAt, o.updated_at AS updatedAt "
            + "FROM service_order o "
            + "LEFT JOIN technician t ON t.id = o.technician_id "
            + "LEFT JOIN service_project p ON p.id = o.project_id "
            + "LEFT JOIN platform_user u ON u.id = o.user_id "
            + "LEFT JOIN order_project_snapshot ops ON ops.order_id = o.id "
            + "LEFT JOIN order_address_snapshot oas ON oas.order_id = o.id "
            + "LEFT JOIN order_amount oa ON oa.order_id = o.id "
            + "WHERE o.technician_id = #{technicianId} "
            + "ORDER BY o.id DESC LIMIT #{limit} OFFSET #{offset}")
    List<OrderListItem> findEnrichedByTechnician(@Param("technicianId") long technicianId,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT o.id, o.order_no AS orderNo, o.user_id AS userId, o.technician_id AS technicianId, "
            + "COALESCE(t.service_name, '专业技师') AS technicianName, "
            + "COALESCE(t.phone, '') AS technicianPhone, "
            + "COALESCE(t.avatar_url, '') AS technicianAvatarUrl, "
            + "o.project_id AS projectId, "
            + "COALESCE(ops.project_name, p.name, '理疗推拿服务') AS projectName, "
            + "COALESCE(ops.duration_minutes, p.duration_minutes, 60) AS durationMinutes, "
            + "COALESCE(oas.contact_name, u.nickname, '顾客') AS customerName, "
            + "COALESCE(oas.contact_phone, u.phone, '') AS customerPhone, "
            + "CONCAT(COALESCE(oas.region_name, ''), ' ', COALESCE(oas.detail, '')) AS serviceAddress, "
            + "oas.longitude, oas.latitude, "
            + "o.status, o.service_date AS serviceDate, o.start_time AS startTime, o.end_time AS endTime, "
            + "COALESCE(oa.payable_amount, 0) AS payableAmount, "
            + "COALESCE(oa.paid_amount, 0) AS paidAmount, "
            + "o.note, o.cancel_reason AS cancelReason, "
            + "o.created_at AS createdAt, o.updated_at AS updatedAt "
            + "FROM service_order o "
            + "LEFT JOIN technician t ON t.id = o.technician_id "
            + "LEFT JOIN service_project p ON p.id = o.project_id "
            + "LEFT JOIN platform_user u ON u.id = o.user_id "
            + "LEFT JOIN order_project_snapshot ops ON ops.order_id = o.id "
            + "LEFT JOIN order_address_snapshot oas ON oas.order_id = o.id "
            + "LEFT JOIN order_amount oa ON oa.order_id = o.id "
            + "ORDER BY o.id DESC LIMIT #{limit} OFFSET #{offset}")
    List<OrderListItem> findEnrichedAll(@Param("limit") int limit, @Param("offset") int offset);

    @Update("UPDATE service_order SET status = #{toStatus}, version = version + 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE order_no = #{orderNo} AND status = #{fromStatus} AND version = #{version}")
    int transitionStatus(@Param("orderNo") String orderNo, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("version") int version);

    @Update("UPDATE service_order SET status = #{toStatus}, cancel_reason = #{reason}, version = version + 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE order_no = #{orderNo} AND status = #{fromStatus} AND version = #{version}")
    int transitionWithReason(@Param("orderNo") String orderNo, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("reason") String reason, @Param("version") int version);

    @Update("UPDATE service_order SET note = #{note}, updated_at = CURRENT_TIMESTAMP WHERE order_no = #{orderNo}")
    void updateNote(@Param("orderNo") String orderNo, @Param("note") String note);

    // === 项目快照 ===
    @Insert("INSERT INTO order_project_snapshot (order_id, project_name, duration_minutes, base_price, override_price, actual_price) "
            + "VALUES (#{orderId}, #{projectName}, #{durationMinutes}, #{basePrice}, #{overridePrice}, #{actualPrice})")
    void insertProjectSnapshot(@Param("orderId") long orderId, @Param("projectName") String projectName,
            @Param("durationMinutes") int durationMinutes, @Param("basePrice") BigDecimal basePrice,
            @Param("overridePrice") BigDecimal overridePrice, @Param("actualPrice") BigDecimal actualPrice);

    @Select("SELECT project_name AS projectName, duration_minutes AS durationMinutes, "
            + "base_price AS basePrice, override_price AS overridePrice, actual_price AS actualPrice "
            + "FROM order_project_snapshot WHERE order_id = #{orderId}")
    Optional<ProjectSnapshot> findProjectSnapshot(@Param("orderId") long orderId);

    // === 地址快照 ===
    @Insert("INSERT INTO order_address_snapshot (order_id, contact_name, contact_phone, region_name, detail, longitude, latitude) "
            + "VALUES (#{orderId}, #{contactName}, #{contactPhone}, #{regionName}, #{detail}, #{longitude}, #{latitude})")
    void insertAddressSnapshot(@Param("orderId") long orderId, @Param("contactName") String contactName,
            @Param("contactPhone") String contactPhone, @Param("regionName") String regionName,
            @Param("detail") String detail, @Param("longitude") BigDecimal longitude,
            @Param("latitude") BigDecimal latitude);

    @Select("SELECT contact_name AS contactName, contact_phone AS contactPhone, "
            + "region_name AS regionName, detail, longitude, latitude "
            + "FROM order_address_snapshot WHERE order_id = #{orderId}")
    Optional<AddressSnapshot> findAddressSnapshot(@Param("orderId") long orderId);

    // === 金额 ===
    @Insert("INSERT INTO order_amount (order_id, project_amount, travel_fee, discount_amount, payable_amount) "
            + "VALUES (#{orderId}, #{projectAmount}, #{travelFee}, #{discountAmount}, #{payableAmount})")
    void insertAmount(@Param("orderId") long orderId, @Param("projectAmount") BigDecimal projectAmount,
            @Param("travelFee") BigDecimal travelFee, @Param("discountAmount") BigDecimal discountAmount,
            @Param("payableAmount") BigDecimal payableAmount);

    @Select("SELECT project_amount AS projectAmount, travel_fee AS travelFee, "
            + "discount_amount AS discountAmount, payable_amount AS payableAmount, "
            + "paid_amount AS paidAmount, refunded_amount AS refundedAmount "
            + "FROM order_amount WHERE order_id = #{orderId}")
    Optional<AmountView> findAmount(@Param("orderId") long orderId);

    @Update("UPDATE order_amount SET paid_amount = #{amount}, updated_at = CURRENT_TIMESTAMP WHERE order_id = #{orderId}")
    void updatePaidAmount(@Param("orderId") long orderId, @Param("amount") BigDecimal amount);

    @Update("UPDATE order_amount SET refunded_amount = refunded_amount + #{amount}, updated_at = CURRENT_TIMESTAMP WHERE order_id = #{orderId}")
    void addRefundedAmount(@Param("orderId") long orderId, @Param("amount") BigDecimal amount);

    // === 状态日志 ===
    @Insert("INSERT INTO order_status_log (id, order_id, from_status, to_status, operator_type, operator_id, reason) "
            + "VALUES (#{id}, #{orderId}, #{fromStatus}, #{toStatus}, #{operatorType}, #{operatorId}, #{reason})")
    void insertStatusLog(@Param("id") long id, @Param("orderId") long orderId,
            @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus,
            @Param("operatorType") String operatorType, @Param("operatorId") Long operatorId,
            @Param("reason") String reason);

    @Select("SELECT id, from_status AS fromStatus, to_status AS toStatus, "
            + "operator_type AS operatorType, operator_id AS operatorId, reason, created_at AS createdAt "
            + "FROM order_status_log WHERE order_id = #{orderId} ORDER BY id")
    List<StatusLogView> findStatusLogs(@Param("orderId") long orderId);

    // === 时段锁 ===
    @Insert("INSERT INTO schedule_lock (id, technician_id, lock_date, start_time, end_time, order_no, expire_at) "
            + "VALUES (#{id}, #{technicianId}, #{lockDate}, #{startTime}, #{endTime}, #{orderNo}, #{expireAt})")
    void insertLock(@Param("id") long id, @Param("technicianId") long technicianId,
            @Param("lockDate") LocalDate lockDate, @Param("startTime") String startTime,
            @Param("endTime") String endTime, @Param("orderNo") String orderNo,
            @Param("expireAt") LocalDateTime expireAt);

    @Update("UPDATE schedule_lock SET status = 'RELEASED' WHERE order_no = #{orderNo}")
    void releaseLock(@Param("orderNo") String orderNo);

    @Select("SELECT COUNT(*) FROM schedule_lock "
            + "WHERE technician_id = #{technicianId} AND lock_date = #{lockDate} "
            + "AND start_time < #{endTime} AND end_time > #{startTime} "
            + "AND status = 'LOCKED' AND expire_at > #{now}")
    int countActiveLock(@Param("technicianId") long technicianId, @Param("lockDate") LocalDate lockDate,
            @Param("startTime") String startTime, @Param("endTime") String endTime,
            @Param("now") LocalDateTime now);

    // === 超时处理 ===
    @Select("SELECT order_no FROM service_order WHERE status = 'PENDING_PAYMENT' AND created_at < #{cutoff}")
    List<String> findExpiredPendingOrders(@Param("cutoff") LocalDateTime cutoff);

    @Update("UPDATE service_order SET status = 'EXPIRED', version = version + 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE order_no = #{orderNo} AND status = 'PENDING_PAYMENT'")
    int expireOrder(@Param("orderNo") String orderNo);

    // === 技师接单 ===
    @Update("UPDATE service_order SET status = #{toStatus}, version = version + 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE order_no = #{orderNo} AND status = #{fromStatus} AND version = #{version}")
    int acceptOrder(@Param("orderNo") String orderNo, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("version") int version);

    @Update("UPDATE service_order SET status = #{toStatus}, version = version + 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE order_no = #{orderNo} AND status = #{fromStatus} AND version = #{version}")
    int startService(@Param("orderNo") String orderNo, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("version") int version);

    @Update("UPDATE service_order SET status = #{toStatus}, version = version + 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE order_no = #{orderNo} AND status = #{fromStatus} AND version = #{version}")
    int completeService(@Param("orderNo") String orderNo, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("version") int version);

    // === 改派 ===
    @Update("UPDATE service_order SET technician_id = #{newTechnicianId}, version = version + 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE order_no = #{orderNo} AND version = #{version}")
    int reassignTechnician(@Param("orderNo") String orderNo, @Param("newTechnicianId") long newTechnicianId,
            @Param("version") int version);

    @Insert("INSERT INTO order_reassignment (id, order_id, from_technician_id, new_technician_id, reason, operator_id, is_latest) "
            + "VALUES (#{id}, #{orderId}, #{fromTechnicianId}, #{newTechnicianId}, #{reason}, #{operatorId}, #{isLatest})")
    void insertReassignment(@Param("id") long id, @Param("orderId") long orderId,
            @Param("fromTechnicianId") long fromTechnicianId, @Param("newTechnicianId") long newTechnicianId,
            @Param("reason") String reason, @Param("operatorId") long operatorId, @Param("isLatest") boolean isLatest);

    // === 视图记录 ===
    record OrderView(long id, String orderNo, long userId, long technicianId, long projectId,
            String status, LocalDate serviceDate, String startTime, String endTime,
            int version, String note, String cancelReason, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    record OrderListItem(long id, String orderNo, long userId, long technicianId,
            String technicianName, String technicianPhone, String technicianAvatarUrl,
            long projectId, String projectName, int durationMinutes,
            String customerName, String customerPhone, String serviceAddress,
            BigDecimal longitude, BigDecimal latitude,
            String status, LocalDate serviceDate, String startTime, String endTime,
            BigDecimal payableAmount, BigDecimal paidAmount,
            String note, String cancelReason, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    record ProjectSnapshot(String projectName, int durationMinutes, BigDecimal basePrice,
            BigDecimal overridePrice, BigDecimal actualPrice) {}

    record AddressSnapshot(String contactName, String contactPhone, String regionName,
            String detail, BigDecimal longitude, BigDecimal latitude) {}

    record AmountView(BigDecimal projectAmount, BigDecimal travelFee, BigDecimal discountAmount,
            BigDecimal payableAmount, BigDecimal paidAmount, BigDecimal refundedAmount) {}

    record StatusLogView(long id, String fromStatus, String toStatus, String operatorType,
            Long operatorId, String reason, LocalDateTime createdAt) {}

    // === 根据ID查找订单 ===
    @Select("SELECT id, order_no AS orderNo, user_id AS userId, technician_id AS technicianId, "
            + "project_id AS projectId, status, service_date AS serviceDate, start_time AS startTime, "
            + "end_time AS endTime, version, note, cancel_reason AS cancelReason, "
            + "created_at AS createdAt, updated_at AS updatedAt "
            + "FROM service_order WHERE id = #{id}")
    OrderView findByOrderId(@Param("id") long id);

    // === 技师统计 ===
    @Select("SELECT COUNT(*) FROM service_order WHERE technician_id = #{technicianId} AND status = 'PAID'")
    int countPendingOrders(@Param("technicianId") long technicianId);

    @Select("SELECT COUNT(*) FROM service_order WHERE technician_id = #{technicianId} AND service_date = #{today}")
    int countTodayOrders(@Param("technicianId") long technicianId, @Param("today") LocalDate today);
}