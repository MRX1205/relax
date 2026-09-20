package com.relax.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.relax.catalog.TechnicianPublicMapper;
import com.relax.common.api.BusinessException;
import com.relax.coupon.CouponMapper;
import com.relax.region.RegionMapper;
import com.relax.schedule.ScheduleMapper;

@Service
public class OrderPreviewService {

    private static final BigDecimal TRAVEL_FEE = new BigDecimal("20.00");

    private final TechnicianPublicMapper techMapper;
    private final ScheduleMapper scheduleMapper;
    private final RegionMapper regionMapper;
    private final CouponMapper couponMapper;

    OrderPreviewService(TechnicianPublicMapper techMapper, ScheduleMapper scheduleMapper,
            RegionMapper regionMapper, CouponMapper couponMapper) {
        this.techMapper = techMapper;
        this.scheduleMapper = scheduleMapper;
        this.regionMapper = regionMapper;
        this.couponMapper = couponMapper;
    }

    public OrderPreview preview(long userId, PreviewRequest request) {
        // 1. 验证项目和技师
        TechnicianPublicMapper.TechnicianProjectItem projectItem = techMapper
                .findProjectsForTechnician(request.technicianId()).stream()
                .filter(p -> p.projectId() == request.projectId())
                .findFirst()
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_AVAILABLE", "该技师不提供此项目"));

        // 2. 验证地址
        Optional<RegionMapper.UserAddress> addressOpt = request.addressId() != null
                ? regionMapper.findAddress(userId, request.addressId())
                : Optional.empty();
        String addressSummary = addressOpt.map(a -> a.regionName() + " " + a.detail()).orElse("未选择地址");

        // 3. 验证时段可用
        LocalDate date = LocalDate.parse(request.serviceDate());
        String start = request.startTime();
        String end = calculateEndTime(start, projectItem.durationMinutes());

        List<ScheduleMapper.ScheduleView> schedules = scheduleMapper
                .findByTechnicianAndDate(request.technicianId(), date);
        boolean slotAvailable = schedules.stream().anyMatch(s ->
                s.startTime().compareTo(start) <= 0 && s.endTime().compareTo(end) >= 0);
        if (!slotAvailable) {
            throw new BusinessException("SLOT_NOT_AVAILABLE", "所选时段不可用，请重新选择");
        }

        // 4. 计算金额
        BigDecimal projectAmount = projectItem.price();
        BigDecimal travelFee = TRAVEL_FEE;
        BigDecimal subtotal = projectAmount.add(travelFee);
        BigDecimal discountAmount = BigDecimal.ZERO;

        // 优惠券折扣
        if (request.couponId() != null) {
            CouponMapper.UserCoupon coupon = couponMapper.findAllUserCoupons(userId).stream()
                    .filter(c -> c.id() == request.couponId())
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("COUPON_NOT_FOUND", "优惠券不存在"));
            if (!"AVAILABLE".equals(coupon.status())) {
                throw new BusinessException("COUPON_NOT_AVAILABLE", "优惠券不可用");
            }
            if (coupon.endAt() != null && coupon.endAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException("COUPON_EXPIRED", "优惠券已过期");
            }
            if (subtotal.compareTo(coupon.minSpend()) < 0) {
                throw new BusinessException("COUPON_MIN_SPEND_NOT_MET", "未达到最低消费金额");
            }
            discountAmount = coupon.amount();
        }

        BigDecimal payableAmount = subtotal.subtract(discountAmount);

        return new OrderPreview(
                request.projectId(),
                projectItem.projectName(),
                projectItem.durationMinutes(),
                request.technicianId(),
                request.serviceDate(),
                start,
                end,
                addressSummary,
                projectAmount,
                travelFee,
                discountAmount,
                payableAmount);
    }

    private String calculateEndTime(String startTime, int durationMinutes) {
        LocalTime start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime end = start.plusMinutes(durationMinutes);
        return end.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public record PreviewRequest(
            long projectId,
            long technicianId,
            Long addressId,
            String serviceDate,
            String startTime,
            Long couponId) {}

    public record OrderPreview(
            long projectId,
            String projectName,
            int durationMinutes,
            long technicianId,
            String serviceDate,
            String startTime,
            String endTime,
            String addressSummary,
            BigDecimal projectAmount,
            BigDecimal travelFee,
            BigDecimal discountAmount,
            BigDecimal payableAmount) {}
}
