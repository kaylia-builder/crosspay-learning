package com.crosspay.module.gateway;

import com.crosspay.common.exception.BusinessException;
import com.crosspay.module.gateway.dto.GatewayPayRequest;
import com.crosspay.module.gateway.dto.GatewayPayResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 网关路由器
 *
 * === 为什么需要 Router ===
 *
 * 真实支付公司根据多个维度决定用哪个渠道：
 * - 商户所在国家（非洲用 Flutterwave，东南亚用 GrabPay）
 * - 支付方式（信用卡走 Stripe，本地钱包走 M-Pesa）
 * - 金额大小（小额走本地渠道成本低，大额走国际卡组织更安全）
 * - 渠道健康度（某个渠道挂了自动切换到备用渠道）
 *
 * 这个 Router 负责"选择渠道并调用"，对外暴露统一的方法。
 * PaymentService 只需要调用 router.route(...)，不关心具体是哪个渠道。
 */
@Component
public class GatewayRouter {

    private final MockAfricaGateway mockAfricaGateway;

    public GatewayRouter(MockAfricaGateway mockAfricaGateway) {
        this.mockAfricaGateway = mockAfricaGateway;
    }

    /**
     * 路由支付请求到合适的渠道
     *
     * 现阶段只有一个 Mock 渠道。真实系统会在这里：
     * 1. 查 merchant 的国家 → 确定可用渠道列表
     * 2. 按优先级排序（费率低的优先、成功率高的优先）
     * 3. 选择第一个 → 调用
     * 4. 如果失败 → 尝试下一个（fallback）
     */
    public GatewayPayResponse route(GatewayPayRequest request) {
        // 简化：直接使用 Mock Africa Gateway
        return mockAfricaGateway.pay(request);
    }
}
