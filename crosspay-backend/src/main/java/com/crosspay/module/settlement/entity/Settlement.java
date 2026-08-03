package com.crosspay.module.settlement.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算记录
 *
 * === 支付 vs 结算：为什么是两个概念？ ===
 *
 * 支付（Payment）：买家付钱的瞬间。钱从买家 → 渠道 → 平台。
 * 结算（Settlement）：商户收到钱的时刻。钱从平台 → 商户银行账户。
 *
 * 中间有延迟，因为：
 * 1. 平台需要时间汇总交易、计算手续费
 * 2. 需要风控审核（防止洗钱、拒付）
 * 3. 银行批量打款有时间窗口（T+1, T+3）
 * 4. 有最低结算金额（比如攒够 100 USD 才打款）
 *
 * 这个 Separation of Concerns 是支付系统最重要的设计原则之一。
 * 支付模块不知道结算逻辑，结算模块不修改支付数据。
 */
@Entity
@Table(name = "settlements")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_no", nullable = false, unique = true, length = 30)
    private String settlementNo;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(length = 10)
    private String currency = "USD";

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSettlementNo() { return settlementNo; }
    public void setSettlementNo(String settlementNo) { this.settlementNo = settlementNo; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
