package com.relax.coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.common.api.BusinessException;

import com.relax.iam.IamMapper;

@Service
public class CouponService {

    private final CouponMapper couponMapper;
    private final IamMapper iamMapper;

    CouponService(CouponMapper couponMapper, IamMapper iamMapper) {
        this.couponMapper = couponMapper;
        this.iamMapper = iamMapper;
    }

    // === User endpoints ===

    public List<CouponMapper.UserCoupon> getMyCoupons(long userId, String status) {
        if (status != null && !status.isBlank()) {
            return couponMapper.findUserCouponsByStatus(userId, status);
        }
        return couponMapper.findAllUserCoupons(userId);
    }

    @Transactional
    public void claimCoupon(long userId, long templateId) {
        CouponMapper.CouponTemplate template = couponMapper.findTemplateById(templateId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COUPON_NOT_FOUND", "优惠券不存在"));

        if (!"ACTIVE".equals(template.status())) {
            throw new BusinessException("COUPON_NOT_ACTIVE", "优惠券不可领取");
        }
        if (template.issuedCount() >= template.totalCount()) {
            throw new BusinessException("COUPON_EXHAUSTED", "优惠券已领完");
        }
        if (template.endAt() != null && template.endAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("COUPON_EXPIRED", "优惠券已过期");
        }
        if (couponMapper.countUserCoupon(userId, templateId) > 0) {
            throw new BusinessException("COUPON_ALREADY_CLAIMED", "已领取过该优惠券");
        }

        long id = IdWorker.getId();
        couponMapper.insertUserCoupon(id, userId, templateId);
        couponMapper.incrementIssuedCount(templateId);
    }

    // === Order integration ===

    @Transactional
    public BigDecimal lockCoupon(long userId, long couponId, long orderId, BigDecimal orderAmount) {
        CouponMapper.UserCoupon coupon = couponMapper.findAllUserCoupons(userId).stream()
                .filter(c -> c.id() == couponId)
                .findFirst()
                .orElseThrow(() -> new BusinessException("COUPON_NOT_FOUND", "优惠券不存在"));

        if (!"AVAILABLE".equals(coupon.status())) {
            throw new BusinessException("COUPON_NOT_AVAILABLE", "优惠券不可用");
        }
        if (coupon.endAt() != null && coupon.endAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("COUPON_EXPIRED", "优惠券已过期");
        }
        if (orderAmount.compareTo(coupon.minSpend()) < 0) {
            throw new BusinessException("COUPON_MIN_SPEND_NOT_MET", "未达到最低消费金额");
        }

        if (couponMapper.lockCoupon(couponId, orderId) == 0) {
            throw new BusinessException("COUPON_LOCK_FAILED", "优惠券锁定失败");
        }

        return coupon.amount();
    }

    @Transactional
    public void useCoupon(long couponId) {
        if (couponMapper.useCoupon(couponId) == 0) {
            throw new BusinessException("COUPON_USE_FAILED", "优惠券核销失败");
        }
    }

    @Transactional
    public void releaseCoupon(long couponId) {
        if (couponMapper.releaseCoupon(couponId) == 0) {
            throw new BusinessException("COUPON_RELEASE_FAILED", "优惠券释放失败");
        }
    }

    // === Admin endpoints ===

    public List<CouponMapper.CouponTemplate> listTemplates() {
        return couponMapper.findAllTemplates();
    }

    @Transactional
    public CouponMapper.CouponTemplate createTemplate(CreateTemplateRequest request) {
        long id = IdWorker.getId();
        couponMapper.insertTemplate(id, request.name(), request.amount(), request.minSpend(),
                request.totalCount(), request.startAt(), request.endAt());
        return couponMapper.findTemplateById(id).orElseThrow();
    }

    @Transactional
    public void updateTemplateStatus(long id, String status) {
        if (couponMapper.updateTemplateStatus(id, status) == 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "COUPON_NOT_FOUND", "优惠券模板不存在");
        }
    }

    @Transactional
    public int grantCoupon(long templateId, Long targetUserId) {
        couponMapper.findTemplateById(templateId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COUPON_NOT_FOUND", "优惠券不存在"));

        if (targetUserId != null && targetUserId > 0) {
            if (couponMapper.countUserCoupon(targetUserId, templateId) == 0) {
                long id = IdWorker.getId();
                couponMapper.insertUserCoupon(id, targetUserId, templateId);
                couponMapper.incrementIssuedCount(templateId);
                return 1;
            }
            return 0;
        }

        List<Long> allUserIds = iamMapper.findAllActiveUserIds();
        int count = 0;
        for (Long uid : allUserIds) {
            if (couponMapper.countUserCoupon(uid, templateId) == 0) {
                long id = IdWorker.getId();
                couponMapper.insertUserCoupon(id, uid, templateId);
                couponMapper.incrementIssuedCount(templateId);
                count++;
            }
        }
        return count;
    }

    public record CreateTemplateRequest(String name, BigDecimal amount, BigDecimal minSpend,
            int totalCount, LocalDateTime startAt, LocalDateTime endAt) {}
}
