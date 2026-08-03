package com.crosspay.module.gateway;

import com.crosspay.module.gateway.dto.GatewayPayRequest;
import com.crosspay.module.gateway.dto.GatewayPayResponse;

/**
 * 支付渠道统一接口
 *
 * === 为什么支付系统需要 Adapter 模式 ===
 *
 * 真实支付公司对接的不是一家银行，而是几十上百家：
 * - 非洲 → M-Pesa, Flutterwave, Paystack
 * - 东南亚 → GrabPay, Dana, GCash
 * - 拉美   → Pix, Mercado Pago
 * - 欧美   → Stripe, Adyen, PayPal
 *
 * 每家接口不一样：
 * - Stripe:   POST /v1/payment_intents
 * - PayPal:   POST /v2/checkout/orders
 * - Flutterwave: POST /v3/payments
 *
 * 如果 PaymentService 里直接写 if-stripe-else-paypal...
 * 每加一个渠道就要改支付核心代码 → 风险极高。
 *
 * Adapter 模式的核心思想：
 * 1. 定义统一接口（这个 Interface）
 * 2. 每个渠道有自己的实现（MockAfricaGateway, StripeGateway...）
 * 3. GatewayRouter 决定用哪个渠道
 * 4. PaymentService 只依赖接口，不依赖具体实现
 *
 * 结果：加一个渠道 = 新建一个类，不碰任何已有代码。
 */
public interface PaymentGateway {

    /**
     * 发起支付请求
     * 返回渠道的处理结果
     */
    GatewayPayResponse pay(GatewayPayRequest request);

    /**
     * 渠道名称（用于日志和路由）
     */
    String getChannelName();
}
