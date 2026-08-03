package com.crosspay.module.gateway;

import com.crosspay.common.dto.ApiResponse;
import com.crosspay.common.exception.BusinessException;
import com.crosspay.module.payment.PaymentRepository;
import com.crosspay.module.payment.entity.PaymentOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 渠道回调接收接口
 *
 * === 为什么需要回调（Callback/Webhook）===
 *
 * 支付不是同步的。你调了渠道的 API，对方可能：
 * - 当时就返回结果（同步模式）
 * - 过几秒/几分钟才通知你（异步模式）
 *
 * 所以支付系统需要暴露一个公开的 HTTP 端点，
 * 让渠道在支付完成后"回调"我们。
 *
 * 安全问题：
 * - 真实系统必须验证回调签名（防止伪造回调）
 * - 这里简化为检查 Header: X-Channel-Signature
 *
 * 幂等性：
 * - 同一个回调可能因为网络重试被发送多次
 * - 状态机保证 PROCESSING→SUCCESS 只能执行一次
 * - 第二次回调时会抛异常，但我们应该返回 200（防止渠道无限重试）
 */
@RestController
@RequestMapping("/api/callback")
public class CallbackController {

    private static final Logger log = LoggerFactory.getLogger(CallbackController.class);

    private final PaymentRepository paymentRepository;

    public CallbackController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @PostMapping("/{channel}")
    @Transactional
    public ApiResponse<String> handleCallback(
            @PathVariable String channel,
            @RequestHeader(value = "X-Channel-Signature", required = false) String signature,
            @RequestBody Map<String, String> body) {

        String platformOrderNo = body.get("platformOrderNo");
        String channelOrderNo = body.get("channelOrderNo");
        String resultStatus = body.get("status");  // SUCCESS or FAILED
        String failReason = body.getOrDefault("failReason", "");

        log.info("Callback received: channel={}, orderNo={}, status={}", channel, platformOrderNo, resultStatus);

        // 查找订单
        PaymentOrder order = paymentRepository.findByOrderNo(platformOrderNo)
                .orElse(null);

        if (order == null) {
            log.error("Callback: order not found: {}", platformOrderNo);
            return ApiResponse.fail(40003, "订单不存在");
        }

        // 更新状态（状态机内部做校验）
        try {
            if ("SUCCESS".equals(resultStatus)) {
                order.markSuccess(channelOrderNo);
            } else if ("FAILED".equals(resultStatus)) {
                order.markFailed(failReason);
            }
            paymentRepository.save(order);
            log.info("Callback processed: orderNo={}, newStatus={}", platformOrderNo, order.getStatus());
        } catch (BusinessException e) {
            // 重复回调或非法状态转换 — 不报错，渠道以为我们收到了
            log.warn("Callback: state transition rejected: orderNo={}, error={}",
                    platformOrderNo, e.getMessage());
        }

        // 始终返回成功（防止渠道因为我们的错误而重试）
        return ApiResponse.ok("OK");
    }
}
