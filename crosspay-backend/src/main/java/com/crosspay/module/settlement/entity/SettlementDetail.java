package com.crosspay.module.settlement.entity;

import jakarta.persistence.*;

/**
 * 结算明细 — 关联结算记录和支付订单
 * 一张结算单包含多笔支付订单
 */
@Entity
@Table(name = "settlement_details")
public class SettlementDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false)
    private Long settlementId;

    @Column(name = "payment_order_id", nullable = false)
    private Long paymentOrderId;

    public SettlementDetail() {}

    public SettlementDetail(Long settlementId, Long paymentOrderId) {
        this.settlementId = settlementId;
        this.paymentOrderId = paymentOrderId;
    }

    public Long getId() { return id; }
    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }
    public Long getPaymentOrderId() { return paymentOrderId; }
    public void setPaymentOrderId(Long paymentOrderId) { this.paymentOrderId = paymentOrderId; }
}
