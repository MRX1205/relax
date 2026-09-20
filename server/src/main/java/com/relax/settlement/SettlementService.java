package com.relax.settlement;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.common.api.BusinessException;

@Service
public class SettlementService {

    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.20"); // 20%

    private final SettlementMapper settlementMapper;

    SettlementService(SettlementMapper settlementMapper) {
        this.settlementMapper = settlementMapper;
    }

    @Transactional
    public void createIncome(long orderId, long technicianId, BigDecimal projectAmount) {
        BigDecimal fee = projectAmount.multiply(PLATFORM_FEE_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal payable = projectAmount.subtract(fee);
        long id = IdWorker.getId();
        settlementMapper.insertIncome(id, orderId, technicianId, projectAmount, fee, payable);
    }

    @Transactional
    public SettlementMapper.SettlementView createSettlement(long operatorId, long technicianId, List<Long> incomeIds) {
        if (incomeIds == null || incomeIds.isEmpty()) {
            throw new BusinessException("NO_INCOME_SELECTED", "请选择收入明细");
        }
        
        BigDecimal total = BigDecimal.ZERO;
        long settleId = IdWorker.getId();
        String settleNo = "ST" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));

        // 先计算总金额
        for (Long incomeId : incomeIds) {
            SettlementMapper.IncomeView income = settlementMapper.findIncomeById(incomeId);
            if (income == null) {
                throw new BusinessException("INCOME_NOT_FOUND", "收入明细不存在");
            }
            if (!"PENDING".equals(income.status())) {
                throw new BusinessException("INCOME_NOT_PENDING", "收入明细状态不正确");
            }
            total = total.add(income.payableAmount());
        }

        settlementMapper.insertSettlement(settleId, settleNo, technicianId, total, operatorId);

        for (Long incomeId : incomeIds) {
            SettlementMapper.IncomeView income = settlementMapper.findIncomeById(incomeId);
            settlementMapper.updateIncomeStatus(incomeId, "SETTLED");
            long itemId = IdWorker.getId();
            settlementMapper.insertItem(itemId, settleId, incomeId, income.orderId(), income.payableAmount());
        }

        return settlementMapper.findSettlementById(settleId).orElseThrow();
    }

    @Transactional
    public void markPaid(long operatorId, long settlementId, String referenceNo, Long proofFileId) {
        if (settlementMapper.markPaid(settlementId, referenceNo, proofFileId, operatorId) == 0) {
            throw new BusinessException("SETTLEMENT_NOT_PENDING", "该结算单不在待付款状态");
        }
    }

    @Transactional
    public void markVoid(long settlementId) {
        if (settlementMapper.markVoid(settlementId) == 0) {
            throw new BusinessException("SETTLEMENT_NOT_PENDING", "该结算单不在待付款状态");
        }
    }

    public List<SettlementMapper.IncomeView> listIncome(long technicianId, int page, int size) {
        return settlementMapper.findIncomeByTech(technicianId, size, page * size);
    }

    public List<SettlementMapper.SettlementView> listSettlementsByTech(long technicianId, int page, int size) {
        return settlementMapper.findSettlementsByTech(technicianId, size, page * size);
    }

    public List<SettlementMapper.SettlementView> listAllSettlements(int page, int size) {
        return settlementMapper.findAllSettlements(size, page * size);
    }
}
