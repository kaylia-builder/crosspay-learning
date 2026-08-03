package com.crosspay.module.payment;

import com.crosspay.common.exception.BusinessException;
import com.crosspay.common.util.OrderNoGenerator;
import com.crosspay.module.merchant.MerchantRepository;
import com.crosspay.module.merchant.entity.Merchant;
import com.crosspay.module.gateway.GatewayRouter;
import com.crosspay.module.gateway.dto.GatewayPayRequest;
import com.crosspay.module.gateway.dto.GatewayPayResponse;
import com.crosspay.module.payment.dto.CreatePaymentRequest;
import com.crosspay.module.payment.dto.PaymentResponse;
import com.crosspay.module.payment.entity.PaymentOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付服务
 *
 * === 一笔支付订单的完整创建流程 ===
 *
 * 1. 验证商户存在且状态正常
 * 2. 生成平台订单号（PAY + 日期 + 随机数）
 * 3. 创建订单，状态 = CREATED
 * 4. 调用 GatewayRouter 路由到合适的支付渠道
 * 5. 按渠道返回更新订单状态
 *
 * 整个方法用 @Transactional 保证原子性。
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final GatewayRouter gatewayRouter;

    public PaymentService(PaymentRepository paymentRepository,
                          MerchantRepository merchantRepository,
                          GatewayRouter gatewayRouter) {
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
        this.gatewayRouter = gatewayRouter;
    }

    @Transactional
    public PaymentResponse createPayment(Long merchantId, CreatePaymentRequest req) {
        // 1. 验证商户
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(40002, "商户不存在"));

        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new BusinessException(40004, "商户已被禁用，无法发起支付");
        }

        // 2. 创建订单（状态 = CREATED）
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(OrderNoGenerator.generatePaymentNo());
        order.setMerchantId(merchantId);
        order.setMerchantOrderNo(req.getMerchantOrderNo());
        order.setAmount(req.getAmount());
        order.setCurrency(req.getCurrency());

        order = paymentRepository.save(order);
        log.info("Payment order created: orderNo={}, merchantId={}, amount={} {}",
                order.getOrderNo(), merchantId, order.getAmount(), order.getCurrency());

        // 3. 调用支付渠道
        order.markProcessing("MOCK_AFRICA");
        paymentRepository.save(order);

        GatewayPayResponse gatewayResp = gatewayRouter.route(
                new GatewayPayRequest(order.getOrderNo(), order.getAmount(),
                        order.getCurrency(), "http://localhost:8080/api/callback/MOCK_AFRICA"));

        // 4. 按渠道返回更新订单状态
        switch (gatewayResp.getStatus()) {
            case "SUCCESS":
                order.markSuccess(gatewayResp.getChannelOrderNo());
                break;
            case "FAILED":
                order.markFailed(gatewayResp.getMessage());
                break;
            case "PROCESSING":
                // 渠道异步处理中，订单保持 PROCESSING，等待后续回调
                log.info("Payment order waiting for callback: orderNo={}", order.getOrderNo());
                break;
        }
        paymentRepository.save(order);

        return PaymentResponse.from(order);
    }

    /**
     * 查询支付订单
     * 只有属于该商户的订单才能被查询（防止越权）
     */
    public PaymentResponse queryPayment(Long merchantId, String orderNo) {
        PaymentOrder order = paymentRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(40003, "订单不存在"));

        if (!order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(40300, "无权查询此订单");
        }

        return PaymentResponse.from(order);
    }

    /**
     * 查看商户交易记录（分页）
     */
    public Page<PaymentResponse> getTransactions(Long merchantId, int page, int size) {
        return paymentRepository
                .findByMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(page, size))
                .map(PaymentResponse::from);
    }
}
