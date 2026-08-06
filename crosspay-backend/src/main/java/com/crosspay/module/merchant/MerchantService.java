package com.crosspay.module.merchant;

import com.crosspay.common.exception.BusinessException;
import com.crosspay.module.merchant.entity.Merchant;
import com.crosspay.module.payment.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 商户服务
 *
 * 支付公司里商户服务是基础服务，被支付、结算、风控等多个模块依赖。
 * 这里保持简单：只做查询。真正的支付公司还有商户审核、费率管理、合同管理等。
 */
@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;

    public MerchantService(MerchantRepository merchantRepository,
                           PaymentRepository paymentRepository) {
        this.merchantRepository = merchantRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * 获取商户信息
     */
    public Merchant getMerchant(Long merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(40002, "商户不存在"));
    }

    /**
     * 商户首页 Dashboard — 实时统计今日交易数据
     */
    public Map<String, Object> getDashboard(Long merchantId) {
        Merchant merchant = getMerchant(merchantId);

        // 今日 00:00:00 ~ 现在
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        long totalCount = paymentRepository.countTodayByMerchant(merchantId, todayStart);
        BigDecimal totalAmount = paymentRepository.sumTodayAmountByMerchant(merchantId, todayStart);
        long successCount = paymentRepository.countTodaySuccessByMerchant(merchantId, todayStart);

        String successRate = totalCount > 0
                ? BigDecimal.valueOf(successCount)
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP) + "%"
                : "0%";

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("merchantNo", merchant.getMerchantNo());
        dashboard.put("name", merchant.getName());
        dashboard.put("country", merchant.getCountry());
        dashboard.put("currency", merchant.getCurrency());
        dashboard.put("feeRate", merchant.getFeeRate().toString());
        dashboard.put("status", merchant.getStatus());
        dashboard.put("todayTransactionCount", totalCount);
        dashboard.put("todayTransactionAmount", totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP).toString() : "0.00");
        dashboard.put("successRate", successRate);
        return dashboard;
    }
}
