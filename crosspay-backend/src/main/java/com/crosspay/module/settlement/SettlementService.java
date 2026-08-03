package com.crosspay.module.settlement;

import com.crosspay.common.util.OrderNoGenerator;
import com.crosspay.module.merchant.MerchantRepository;
import com.crosspay.module.merchant.entity.Merchant;
import com.crosspay.module.payment.PaymentRepository;
import com.crosspay.module.payment.entity.PaymentOrder;
import com.crosspay.module.settlement.dto.SettlementResponse;
import com.crosspay.module.settlement.entity.Settlement;
import com.crosspay.module.settlement.entity.SettlementDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 结算服务
 *
 * === 结算的核心公式 ===
 *
 * netAmount = totalAmount - (totalAmount × feeRate)
 *
 * 例如: totalAmount = 1000 USD, feeRate = 3%
 *       feeAmount = 1000 × 0.03 = 30 USD
 *       netAmount = 1000 - 30 = 970 USD
 *
 * === 真实结算的复杂度 ===
 * - 不同支付方式的费率不同（信用卡 3.5%, 本地钱包 1.5%）
 * - 汇率波动（USD 结算但商户收 KES 肯尼亚先令）
 * - 拒付扣减（Chargeback 要从结算里扣）
 * - 最低结算金额（不满足最低金额则顺延到下个周期）
 * - 结算周期（T+1, T+3, 每周二等）
 */
@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final SettlementRepository settlementRepository;
    private final SettlementDetailRepository settlementDetailRepository;
    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;

    public SettlementService(SettlementRepository settlementRepository,
                             SettlementDetailRepository settlementDetailRepository,
                             PaymentRepository paymentRepository,
                             MerchantRepository merchantRepository) {
        this.settlementRepository = settlementRepository;
        this.settlementDetailRepository = settlementDetailRepository;
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
    }

    /**
     * 对指定商户执行日终结算
     *
     * 模拟逻辑：
     * 1. 查找该商户所有未结算的 SUCCESS 订单
     * 2. 汇总金额
     * 3. 按规定费率计算手续费
     * 4. 生成结算记录
     *
     * 真实系统这是一个定时任务（每日凌晨跑），不会手动触发。
     */
    @Transactional
    public SettlementResponse createSettlement(Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId).orElse(null);
        if (merchant == null) {
            throw new RuntimeException("商户不存在");
        }

        // 1. 查找该商户所有 SUCCESS 订单（简化：所有时间的，真实系统按日期过滤）
        List<PaymentOrder> orders = paymentRepository.findAll().stream()
                .filter(o -> o.getMerchantId().equals(merchantId))
                .filter(o -> "SUCCESS".equals(o.getStatus()))
                .toList();

        if (orders.isEmpty()) {
            log.info("No orders to settle for merchant: {}", merchantId);
            return null;
        }

        // 2. 汇总金额
        BigDecimal totalAmount = orders.stream()
                .map(PaymentOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal feeRate = merchant.getFeeRate();
        BigDecimal feeAmount = totalAmount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = totalAmount.subtract(feeAmount);

        log.info("Settlement for merchant {}: total={}, fee={}, net={}",
                merchant.getMerchantNo(), totalAmount, feeAmount, netAmount);

        // 3. 生成结算记录
        Settlement settlement = new Settlement();
        settlement.setSettlementNo(OrderNoGenerator.generateSettlementNo());
        settlement.setMerchantId(merchantId);
        settlement.setTotalAmount(totalAmount);
        settlement.setFeeAmount(feeAmount);
        settlement.setNetAmount(netAmount);
        settlement.setCurrency(merchant.getCurrency());
        settlement.setSettlementDate(LocalDate.now());
        settlement.setStatus("PENDING");

        settlement = settlementRepository.save(settlement);

        // 4. 记录结算明细（哪几笔订单被结算了）
        for (PaymentOrder order : orders) {
            settlementDetailRepository.save(
                    new SettlementDetail(settlement.getId(), order.getId()));
        }

        return SettlementResponse.from(settlement);
    }

    /**
     * 查看商户结算记录
     */
    public Page<SettlementResponse> getSettlements(Long merchantId, int page, int size) {
        return settlementRepository
                .findByMerchantIdOrderBySettlementDateDesc(merchantId, PageRequest.of(page, size))
                .map(SettlementResponse::from);
    }
}
