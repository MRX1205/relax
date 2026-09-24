package com.relax.stats;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.relax.common.api.ApiResponse;
import com.relax.order.OrderMapper;

@RestController
@RequestMapping("/api/v1/admin/stats")
@PreAuthorize("hasAuthority('order:read') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class StatsController {

    private final StatsMapper statsMapper;
    private final OrderMapper orderMapper;

    StatsController(StatsMapper statsMapper, OrderMapper orderMapper) {
        this.statsMapper = statsMapper;
        this.orderMapper = orderMapper;
    }

    @GetMapping
    ApiResponse<OverviewStats> overview(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String month) {
        boolean hasFilter = (date != null && !date.isBlank()) || (month != null && !month.isBlank());
        long totalOrders = hasFilter ? statsMapper.filteredOrdersCount(date, month) : statsMapper.totalOrders();
        BigDecimal totalRevenue = hasFilter ? statsMapper.filteredRevenue(date, month) : statsMapper.totalRevenue();
        return ApiResponse.success(new OverviewStats(
                totalOrders,
                totalRevenue,
                statsMapper.totalUsers(),
                statsMapper.totalTechnicians(),
                statsMapper.todayOrders(),
                statsMapper.todayRevenue()));
    }

    @GetMapping("/order-status")
    ApiResponse<List<StatsMapper.StatusCount>> orderStatus(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String month) {
        boolean hasFilter = (date != null && !date.isBlank()) || (month != null && !month.isBlank());
        return ApiResponse.success(hasFilter ? statsMapper.filteredOrderStatusCounts(date, month) : statsMapper.orderStatusCounts());
    }

    @GetMapping("/tech-ranking")
    ApiResponse<List<StatsMapper.TechRanking>> techRanking(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "10") int limit) {
        boolean hasFilter = (date != null && !date.isBlank()) || (month != null && !month.isBlank());
        return ApiResponse.success(hasFilter ? statsMapper.filteredTechRanking(date, month, limit) : statsMapper.techRanking(limit));
    }

    @GetMapping("/daily-trend")
    ApiResponse<List<StatsMapper.DailyTrend>> dailyTrend(
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(statsMapper.dailyTrend(days));
    }

    @GetMapping("/technician-orders")
    ApiResponse<List<OrderMapper.OrderListItem>> technicianOrders(
            @RequestParam long technicianId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String month) {
        return ApiResponse.success(orderMapper.findEnrichedFiltered(null, technicianId, date, month, 100, 0));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportStats(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String month) {
        boolean hasFilter = (date != null && !date.isBlank()) || (month != null && !month.isBlank());
        long orders = hasFilter ? statsMapper.filteredOrdersCount(date, month) : statsMapper.totalOrders();
        BigDecimal revenue = hasFilter ? statsMapper.filteredRevenue(date, month) : statsMapper.totalRevenue();
        long users = statsMapper.totalUsers();
        long techs = statsMapper.totalTechnicians();
        List<StatsMapper.StatusCount> statusCounts = hasFilter ? statsMapper.filteredOrderStatusCounts(date, month) : statsMapper.orderStatusCounts();
        List<StatsMapper.TechRanking> ranking = hasFilter ? statsMapper.filteredTechRanking(date, month, 50) : statsMapper.techRanking(50);

        List<List<Object>> rows = new java.util.ArrayList<>();
        String label = (date != null && !date.isBlank()) ? ("指定日期: " + date) : ((month != null && !month.isBlank()) ? ("指定月份: " + month) : "历史累计");
        rows.add(List.of("报表类型", "经营数据统计报表"));
        rows.add(List.of("统计区间", label));
        rows.add(List.of("导出时间", java.time.LocalDateTime.now().toString().replace('T', ' ')));
        rows.add(List.of());

        rows.add(List.of("【核心经营指标】"));
        rows.add(List.of("指标名称", "数值"));
        rows.add(List.of("有效订单总量", orders + " 单"));
        rows.add(List.of("经营营业总额", revenue != null ? revenue.toString() : "0.00"));
        rows.add(List.of("注册顾客总数", users + " 人"));
        rows.add(List.of("在册活跃技师", techs + " 人"));
        rows.add(List.of());

        rows.add(List.of("【订单状态分布】"));
        rows.add(List.of("状态代码", "状态名称", "订单量"));
        for (StatsMapper.StatusCount sc : statusCounts) {
            rows.add(List.of(sc.status(), formatStatusName(sc.status()), sc.count()));
        }
        rows.add(List.of());

        rows.add(List.of("【技师业绩排行 TOP 50】"));
        rows.add(List.of("排名", "技师ID", "技师姓名", "完成单量", "产出营业额(元)"));
        int rank = 1;
        for (StatsMapper.TechRanking tr : ranking) {
            rows.add(List.of(
                    rank++,
                    tr.technicianId(),
                    tr.name() != null ? tr.name() : "",
                    tr.orderCount(),
                    tr.revenue() != null ? tr.revenue() : BigDecimal.ZERO
            ));
        }

        byte[] xlsx;
        try {
            xlsx = com.relax.common.util.SimpleExcelWriter.writeWorkbook("经营统计报表", rows);
        } catch (java.io.IOException e) {
            throw new RuntimeException("生成 Excel 报表失败", e);
        }

        String fileLabel = (date != null && !date.isBlank()) ? date : ((month != null && !month.isBlank()) ? month : "all");
        String filename = "business-stats-" + fileLabel + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private String formatStatusName(String status) {
        if (status == null) return "";
        return switch (status) {
            case "PENDING_PAYMENT" -> "待支付";
            case "PAID" -> "已支付";
            case "ACCEPTED" -> "已接单";
            case "DEPARTED" -> "已出发";
            case "ARRIVED" -> "已到达";
            case "IN_SERVICE" -> "服务中";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "EXPIRED" -> "已过期";
            case "REFUNDING" -> "退款中";
            case "REFUNDED" -> "已退款";
            default -> status;
        };
    }

    public record OverviewStats(
            long totalOrders,
            BigDecimal totalRevenue,
            long totalUsers,
            long totalTechnicians,
            long todayOrders,
            BigDecimal todayRevenue) {}
}
