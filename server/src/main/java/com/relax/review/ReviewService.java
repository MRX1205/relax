package com.relax.review;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.common.api.BusinessException;
import com.relax.order.OrderMapper;

@Service
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;

    ReviewService(ReviewMapper reviewMapper, OrderMapper orderMapper) {
        this.reviewMapper = reviewMapper;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public ReviewMapper.ReviewView createReview(long userId, String orderNo, int score, String content) {
        OrderMapper.OrderView order = orderMapper.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在"));
        if (order.userId() != userId) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ORDER_NOT_OWNED", "无权操作");
        }
        if (!"COMPLETED".equals(order.status())) {
            throw new BusinessException("ORDER_NOT_COMPLETED", "只能评价已完成的订单");
        }
        if (reviewMapper.findByOrderId(order.id()).isPresent()) {
            throw new BusinessException("REVIEW_EXISTS", "该订单已评价");
        }
        long id = IdWorker.getId();
        reviewMapper.insert(id, order.id(), userId, order.technicianId(), score, content);
        return reviewMapper.findByOrderId(order.id()).orElseThrow();
    }

    public Optional<ReviewMapper.ReviewView> getByOrder(long orderId) {
        return reviewMapper.findByOrderId(orderId);
    }

    public List<ReviewMapper.ReviewView> listByTechnician(long technicianId, int page, int size) {
        return reviewMapper.findByTechnician(technicianId, size, page * size);
    }

    public List<ReviewMapper.ReviewView> listAll(int page, int size) {
        return reviewMapper.findAll(size, page * size);
    }
}
