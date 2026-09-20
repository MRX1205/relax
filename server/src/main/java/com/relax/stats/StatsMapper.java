package com.relax.stats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StatsMapper {

    // === 总览统计 ===
    @Select("SELECT COUNT(*) FROM service_order WHERE status NOT IN ('CANCELLED','EXPIRED')")
    long totalOrders();

    @Select("SELECT COALESCE(SUM(payable_amount), 0) FROM order_amount oa "
            + "JOIN service_order o ON o.id = oa.order_id WHERE o.status IN ('PAID','COMPLETED','IN_SERVICE','ACCEPTED')")
    BigDecimal totalRevenue();

    @Select("SELECT COUNT(*) FROM platform_user")
    long totalUsers();

    @Select("SELECT COUNT(*) FROM technician WHERE status = 'ACTIVE'")
    long totalTechnicians();

    // === 今日统计 ===
    @Select("SELECT COUNT(*) FROM service_order WHERE DATE(created_at) = CURRENT_DATE AND status NOT IN ('CANCELLED','EXPIRED')")
    long todayOrders();

    @Select("SELECT COALESCE(SUM(payable_amount), 0) FROM order_amount oa "
            + "JOIN service_order o ON o.id = oa.order_id WHERE DATE(o.created_at) = CURRENT_DATE "
            + "AND o.status IN ('PAID','COMPLETED','IN_SERVICE','ACCEPTED')")
    BigDecimal todayRevenue();

    // === 订单状态统计 ===
    @Select("SELECT status, COUNT(*) as count FROM service_order GROUP BY status")
    List<StatusCount> orderStatusCounts();

    // === 技师排行 ===
    @Select("SELECT t.id AS technicianId, t.name, COUNT(o.id) AS orderCount, "
            + "COALESCE(SUM(oa.payable_amount), 0) AS revenue "
            + "FROM technician t LEFT JOIN service_order o ON o.technician_id = t.id AND o.status = 'COMPLETED' "
            + "LEFT JOIN order_amount oa ON oa.order_id = o.id "
            + "WHERE t.status = 'ACTIVE' "
            + "GROUP BY t.id, t.name ORDER BY orderCount DESC LIMIT #{limit}")
    List<TechRanking> techRanking(@Param("limit") int limit);

    // === 每日趋势 ===
    @Select("SELECT DATE(created_at) AS date, COUNT(*) AS orderCount, "
            + "COALESCE(SUM(CASE WHEN status NOT IN ('CANCELLED','EXPIRED') THEN 1 ELSE 0 END), 0) AS validOrders "
            + "FROM service_order WHERE created_at >= DATE_SUB(CURRENT_DATE, INTERVAL #{days} DAY) "
            + "GROUP BY DATE(created_at) ORDER BY date")
    List<DailyTrend> dailyTrend(@Param("days") int days);

    record StatusCount(String status, long count) {}

    record TechRanking(long technicianId, String name, long orderCount, BigDecimal revenue) {}

    record DailyTrend(LocalDate date, long orderCount, long validOrders) {}
}
