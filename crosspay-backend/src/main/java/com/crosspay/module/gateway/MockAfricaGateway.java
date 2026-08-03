package com.crosspay.module.gateway;

import com.crosspay.module.gateway.dto.GatewayPayRequest;
import com.crosspay.module.gateway.dto.GatewayPayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟非洲支付渠道
 *
 * === 模拟逻辑 ===
 * - 80% 概率返回 SUCCESS
 * - 15% 概率返回 FAILED（模拟真实场景：余额不足、卡过期等）
 * - 5%  概率返回 PROCESSING（模拟异步处理中）
 *
 * 真实渠道对接时，这里会是 HTTP 调用第三方 API：
 *   restTemplate.postForEntity("https://api.flutterwave.com/v3/payments", ...)
 */
@Component
public class MockAfricaGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockAfricaGateway.class);

    @Override
    public GatewayPayResponse pay(GatewayPayRequest request) {
        log.info("MockAfricaGateway: processing payment orderNo={}, amount={} {}",
                request.getPlatformOrderNo(), request.getAmount(), request.getCurrency());

        // 模拟网络延迟（真实场景 200ms ~ 2s）
        simulateLatency();

        int random = ThreadLocalRandom.current().nextInt(100);
        String channelOrderNo = "AFR_" + request.getPlatformOrderNo();

        if (random < 80) {
            log.info("MockAfricaGateway: SUCCESS orderNo={}", request.getPlatformOrderNo());
            return new GatewayPayResponse(true, channelOrderNo, "SUCCESS", "Payment processed successfully");
        } else if (random < 95) {
            String[] reasons = {
                "Insufficient funds",
                "Card expired",
                "Transaction declined by issuer",
                "Do not honor"
            };
            String reason = reasons[ThreadLocalRandom.current().nextInt(reasons.length)];
            log.warn("MockAfricaGateway: FAILED orderNo={}, reason={}", request.getPlatformOrderNo(), reason);
            return new GatewayPayResponse(false, channelOrderNo, "FAILED", reason);
        } else {
            log.info("MockAfricaGateway: PROCESSING orderNo={}", request.getPlatformOrderNo());
            return new GatewayPayResponse(true, channelOrderNo, "PROCESSING",
                    "Payment is being processed asynchronously");
        }
    }

    @Override
    public String getChannelName() {
        return "MOCK_AFRICA";
    }

    private void simulateLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(300, 800));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
