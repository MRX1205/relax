package com.relax.system;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class MockDataService {

    private final SystemSettingMapper settingMapper;
    private final JdbcTemplate jdbcTemplate;

    MockDataService(SystemSettingMapper settingMapper, JdbcTemplate jdbcTemplate) {
        this.settingMapper = settingMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public MockStatus getStatus() {
        String enabledStr = settingMapper.getSetting("mock.data.enabled");
        boolean enabled = "true".equalsIgnoreCase(enabledStr);
        String lastReset = settingMapper.getSetting("mock.data.last.reset");

        Long mockOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_order WHERE id BETWEEN 6000 AND 6999 OR order_no LIKE 'ORD2026%'",
                Long.class);
        Long realOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_order WHERE NOT (id BETWEEN 6000 AND 6999 OR order_no LIKE 'ORD2026%')",
                Long.class);

        return new MockStatus(
                enabled,
                mockOrders != null ? mockOrders : 0,
                realOrders != null ? realOrders : 0,
                lastReset != null ? lastReset : "",
                "13926700205"
        );
    }

    @Transactional
    public MockStatus toggle() {
        MockStatus current = getStatus();
        if (current.enabled()) {
            cleanMockData();
        } else {
            seedMockData();
        }
        return getStatus();
    }

    public boolean isEnabled() {
        String enabled = settingMapper.getSetting("mock.data.enabled");
        return "true".equalsIgnoreCase(enabled);
    }

    @Transactional
    public void cleanMockData() {
        jdbcTemplate.update("DELETE FROM order_status_log WHERE order_id BETWEEN 6000 AND 6999");
        jdbcTemplate.update("DELETE FROM order_amount WHERE order_id BETWEEN 6000 AND 6999");
        jdbcTemplate.update("DELETE FROM order_project_snapshot WHERE order_id BETWEEN 6000 AND 6999");
        jdbcTemplate.update("DELETE FROM order_address_snapshot WHERE order_id BETWEEN 6000 AND 6999");
        jdbcTemplate.update("DELETE FROM review WHERE id BETWEEN 8000 AND 8999");
        jdbcTemplate.update("DELETE FROM service_order WHERE id BETWEEN 6000 AND 6999 OR order_no LIKE 'ORD2026%'");

        settingMapper.upsertSetting("mock.data.enabled", "false", "Mock数据开关");
        settingMapper.upsertSetting("mock.data.last.reset", LocalDateTime.now().toString().replace('T', ' '), "最后清理时间");
    }

    @Transactional
    public void seedMockData() {
        cleanMockData();

        LocalDate today = LocalDate.now();

        // 插入模拟订单
        jdbcTemplate.update("""
            INSERT INTO service_order (id, order_no, user_id, technician_id, project_id, status, service_date, start_time, end_time, version, note) VALUES
            (6001, 'ORD20260909001', 9001, 3001, 201, 'COMPLETED', ?, '10:00', '11:00', 5, '第一次体验'),
            (6002, 'ORD20260909002', 9002, 3002, 203, 'COMPLETED', ?, '14:00', '15:30', 5, NULL),
            (6003, 'ORD20260909003', 9001, 3003, 207, 'IN_SERVICE', ?, '09:30', '10:45', 3, '运动后需要放松'),
            (6004, 'ORD20260909004', 9002, 3001, 202, 'PAID', ?, '10:00', '10:45', 1, NULL),
            (6005, 'ORD20260909005', 9001, 3002, 204, 'ACCEPTED', ?, '14:00', '15:00', 1, '肩颈不太好'),
            (6006, 'ORD20260909006', 9002, 3001, 205, 'PENDING_PAYMENT', ?, '09:00', '10:00', 0, NULL),
            (6007, 'ORD20260909007', 9001, 3003, 201, 'CANCELLED', ?, '15:00', '16:00', 2, '临时有事')
        """, today.minusDays(3), today.minusDays(2), today, today.plusDays(1), today.plusDays(1), today.plusDays(2), today.minusDays(1));

        // 订单快照
        jdbcTemplate.update("""
            INSERT INTO order_project_snapshot (order_id, project_name, duration_minutes, base_price, override_price, actual_price) VALUES
            (6001, '全身推拿60分钟', 60, 198.00, 188.00, 188.00),
            (6002, '全身精油SPA90分钟', 90, 388.00, 368.00, 368.00),
            (6003, '运动恢复75分钟', 75, 298.00, 288.00, 288.00),
            (6004, '肩颈专项45分钟', 45, 158.00, 148.00, 148.00),
            (6005, '背部精油60分钟', 60, 268.00, 258.00, 258.00),
            (6006, '经典足疗60分钟', 60, 168.00, 158.00, 158.00),
            (6007, '全身推拿60分钟', 60, 198.00, 198.00, 198.00)
        """);

        jdbcTemplate.update("""
            INSERT INTO order_address_snapshot (order_id, contact_name, contact_phone, region_name, detail, longitude, latitude) VALUES
            (6001, '张小明', '13800001111', '南城街道', '鸿福路100号A座1201', 113.750000, 23.020000),
            (6002, '李美丽', '13800002222', '东城街道', '东城中路88号碧桂园3栋502', 113.780000, 23.030000),
            (6003, '张小明', '13800001111', '南城街道', '鸿福路100号A座1201', 113.750000, 23.020000),
            (6004, '李美丽', '13800002222', '东城街道', '东城中路88号碧桂园3栋502', 113.780000, 23.030000),
            (6005, '张小明', '13800001111', '南城街道', '鸿福路100号A座1201', 113.750000, 23.020000),
            (6006, '李美丽', '13800002222', '东城街道', '东城中路88号碧桂园3栋502', 113.780000, 23.030000),
            (6007, '张小明', '13800001111', '南城街道', '鸿福路100号A座1201', 113.750000, 23.020000)
        """);

        jdbcTemplate.update("""
            INSERT INTO order_amount (order_id, project_amount, travel_fee, discount_amount, payable_amount, paid_amount, refunded_amount) VALUES
            (6001, 188.00, 20.00, 0.00, 208.00, 208.00, 0.00),
            (6002, 368.00, 20.00, 0.00, 388.00, 388.00, 0.00),
            (6003, 288.00, 20.00, 0.00, 308.00, 308.00, 0.00),
            (6004, 148.00, 20.00, 0.00, 168.00, 168.00, 0.00),
            (6005, 258.00, 20.00, 0.00, 278.00, 278.00, 0.00),
            (6006, 158.00, 20.00, 0.00, 178.00, 0.00, 0.00),
            (6007, 198.00, 20.00, 0.00, 218.00, 0.00, 0.00)
        """);

        jdbcTemplate.update("""
            INSERT INTO review (id, order_id, user_id, technician_id, score, content, status) VALUES
            (8001, 6001, 9001, 3001, 5, '王师傅手法很好，按完肩颈舒服多了！下次还会预约。', 'VISIBLE'),
            (8002, 6002, 9002, 3002, 4, '精油味道很好，全身放松。环境也很干净。', 'VISIBLE')
        """);

        settingMapper.upsertSetting("mock.data.enabled", "true", "Mock数据开关");
        settingMapper.upsertSetting("mock.data.last.reset", LocalDateTime.now().toString().replace('T', ' '), "最后重置时间");
    }

    @Transactional
    public void resetMockData() {
        seedMockData();
    }

    public record MockStatus(
            boolean enabled,
            long mockOrdersCount,
            long realOrdersCount,
            String lastReset,
            String adminPhone) {}
}
