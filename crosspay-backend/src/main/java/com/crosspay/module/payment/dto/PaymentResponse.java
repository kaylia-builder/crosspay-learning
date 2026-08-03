package com.crosspay.module.payment.dto;

import com.crosspay.module.payment.entity.PaymentOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单响应
 * 用独立 DTO 而不是直接返回 Entity，这样：
 * 1. 不暴露内部字段（如 id）
 * 2. API 契约独立演化（加字段不影响 entity）
 */
public class PaymentResponse {

    private String orderNo;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String channel;
    private String channelOrderNo;
    private String failReason;
    private LocalDateTime createdAt;

    public static PaymentResponse from(PaymentOrder order) {
        PaymentResponse r = new PaymentResponse();
        r.orderNo = order.getOrderNo();
        r.amount = order.getAmount();
        r.currency = order.getCurrency();
        r.status = order.getStatus();
        r.channel = order.getChannel();
        r.channelOrderNo = order.getChannelOrderNo();
        r.failReason = order.getFailReason();
        r.createdAt = order.getCreatedAt();
        return r;
    }

    public String getOrderNo() { return orderNo; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getChannel() { return channel; }
    public String getChannelOrderNo() { return channelOrderNo; }
    public String getFailReason() { return failReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
