package com.relax.order;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderScheduler {

    private final OrderService orderService;

    OrderScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 60000)
    public void expirePendingOrders() {
        orderService.expirePendingOrders();
    }
}
