package com.crosspay.module.payment.entity;

import com.crosspay.common.exception.BusinessException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单实体
 *
 * === 为什么支付系统必须有状态机 ===
 *
 * 支付订单不能简单只有 SUCCESS/FAILED 两个状态。原因是：
 *
 * 1. 外部系统不可靠 — 你调用第三方支付 API，对方可能：
 *    - 处理了但没告诉你结果（回调丢失）
 *    - 处理了但很慢（30 秒才返回）
 *    - 返回了"处理中"（异步模式）
 *
 * 2. 资金安全要求 — 同一个订单不能被重复处理。
 *    如果没有 PROCESSING 状态，就无法区分"正在处理"和"还没处理"，
 *    可能导致重复扣款。
 *
 * 3. 对账需要 — 每天结束时，T+1 日需要核对：
 *    - 平台有多少 SUCCESS 订单
 *    - 银行/渠道有多少成功交易
 *    - 两边对不上的订单要从 PROCESSING 里排查
 *
 * === 状态流转规则 ===
 *
 * CREATED → PROCESSING → SUCCESS
 *                     → FAILED
 *
 * 非法转换（如 SUCCESS → FAILED, FAILED → SUCCESS）会被拒绝。
 */
@Entity
@Table(name = "payment_orders")
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 30)
    private String orderNo;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "merchant_order_no", length = 100)
    private String merchantOrderNo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    private String currency = "USD";

    @Column(nullable = false, length = 20)
    private String status = "CREATED";

    @Column(length = 50)
    private String channel;

    @Column(name = "channel_order_no", length = 100)
    private String channelOrderNo;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @Column(name = "callback_received_at")
    private LocalDateTime callbackReceivedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- JPA lifecycle ----
    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---- 状态机 ----

    /**
     * CREATED → PROCESSING
     * 表示：订单已发送到支付渠道，等待渠道返回结果
     */
    public void markProcessing(String channel) {
        if (!"CREATED".equals(this.status)) {
            throw new BusinessException(40004,
                    "订单状态不允许此操作，当前状态: " + this.status);
        }
        this.status = "PROCESSING";
        this.channel = channel;
    }

    /**
     * PROCESSING → SUCCESS
     * 表示：渠道确认支付成功
     */
    public void markSuccess(String channelOrderNo) {
        if (!"PROCESSING".equals(this.status)) {
            throw new BusinessException(40004,
                    "订单状态不允许标记成功，当前状态: " + this.status);
        }
        this.status = "SUCCESS";
        this.channelOrderNo = channelOrderNo;
        this.callbackReceivedAt = LocalDateTime.now();
    }

    /**
     * PROCESSING → FAILED
     * 表示：渠道返回支付失败
     */
    public void markFailed(String failReason) {
        if (!"PROCESSING".equals(this.status)) {
            throw new BusinessException(40004,
                    "订单状态不允许标记失败，当前状态: " + this.status);
        }
        this.status = "FAILED";
        this.failReason = failReason;
        this.callbackReceivedAt = LocalDateTime.now();
    }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public String getMerchantOrderNo() { return merchantOrderNo; }
    public void setMerchantOrderNo(String merchantOrderNo) { this.merchantOrderNo = merchantOrderNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getChannelOrderNo() { return channelOrderNo; }
    public String getFailReason() { return failReason; }
    public LocalDateTime getCallbackReceivedAt() { return callbackReceivedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
