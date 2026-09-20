package com.relax.aftersale;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.common.api.BusinessException;
import com.relax.order.OrderMapper;

@Service
public class AfterSaleService {

    private final AfterSaleMapper afterSaleMapper;
    private final OrderMapper orderMapper;

    AfterSaleService(AfterSaleMapper afterSaleMapper, OrderMapper orderMapper) {
        this.afterSaleMapper = afterSaleMapper;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public AfterSaleMapper.CaseView createCase(long userId, String orderNo, String type, String content) {
        OrderMapper.OrderView order = orderMapper.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在"));
        if (order.userId() != userId) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ORDER_NOT_OWNED", "无权操作");
        }
        long id = IdWorker.getId();
        String caseNo = "AS" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
        afterSaleMapper.insertCase(id, caseNo, order.id(), userId, type, content);
        return afterSaleMapper.findByCaseNo(caseNo).orElseThrow();
    }

    @Transactional
    public void addRecord(long operatorId, String caseNo, String action, String content) {
        AfterSaleMapper.CaseView caseView = afterSaleMapper.findByCaseNo(caseNo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CASE_NOT_FOUND", "工单不存在"));
        long recordId = IdWorker.getId();
        afterSaleMapper.insertRecord(recordId, caseView.id(), operatorId, action, content);
    }

    @Transactional
    public void updateStatus(long operatorId, String caseNo, String status) {
        afterSaleMapper.updateStatus(caseNo, status, operatorId);
        addRecord(operatorId, caseNo, "STATUS_CHANGE", "状态变更为 " + status);
    }

    public AfterSaleMapper.CaseView getCase(String caseNo) {
        return afterSaleMapper.findByCaseNo(caseNo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CASE_NOT_FOUND", "工单不存在"));
    }

    public List<AfterSaleMapper.CaseView> listAll(int page, int size) {
        return afterSaleMapper.findAll(size, page * size);
    }

    public List<AfterSaleMapper.RecordView> listRecords(long caseId) {
        return afterSaleMapper.findRecords(caseId);
    }
}
