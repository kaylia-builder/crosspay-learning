package com.crosspay.module.merchant;

import com.crosspay.common.exception.BusinessException;
import com.crosspay.module.merchant.entity.Merchant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

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

    public MerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    /**
     * 获取商户信息
     */
    public Merchant getMerchant(Long merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(40002, "商户不存在"));
    }

    /**
     * 商户首页 Dashboard 数据
     * 真实系统这会是一堆聚合查询（今日交易额、成功率、待结算金额等）
     */
    public Map<String, Object> getDashboard(Long merchantId) {
        Merchant merchant = getMerchant(merchantId);

        // TODO: Module 2 完成后替换为真实统计数据
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("merchantNo", merchant.getMerchantNo());
        dashboard.put("name", merchant.getName());
        dashboard.put("country", merchant.getCountry());
        dashboard.put("currency", merchant.getCurrency());
        dashboard.put("feeRate", merchant.getFeeRate().toString());
        dashboard.put("status", merchant.getStatus());
        dashboard.put("todayTransactionCount", 0);
        dashboard.put("todayTransactionAmount", "0.00");
        dashboard.put("successRate", "0%");
        return dashboard;
    }
}
