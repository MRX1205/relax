package com.relax.stats;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/admin/stats")
@PreAuthorize("hasAuthority('order:read') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class StatsController {

    private final StatsMapper statsMapper;

    StatsController(StatsMapper statsMapper) {
        this.statsMapper = statsMapper;
    }

    @GetMapping
    ApiResponse<OverviewStats> overview() {
        return ApiResponse.success(new OverviewStats(
                statsMapper.totalOrders(),
                statsMapper.totalRevenue(),
                statsMapper.totalUsers(),
                statsMapper.totalTechnicians(),
                statsMapper.todayOrders(),
                statsMapper.todayRevenue()));
    }

    @GetMapping("/order-status")
    ApiResponse<List<StatsMapper.StatusCount>> orderStatus() {
        return ApiResponse.success(statsMapper.orderStatusCounts());
    }

    @GetMapping("/tech-ranking")
    ApiResponse<List<StatsMapper.TechRanking>> techRanking(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(statsMapper.techRanking(limit));
    }

    @GetMapping("/daily-trend")
    ApiResponse<List<StatsMapper.DailyTrend>> dailyTrend(
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(statsMapper.dailyTrend(days));
    }

    public record OverviewStats(
            long totalOrders,
            java.math.BigDecimal totalRevenue,
            long totalUsers,
            long totalTechnicians,
            long todayOrders,
            java.math.BigDecimal todayRevenue) {}
}
