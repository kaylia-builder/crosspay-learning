package com.crosspay.module.merchant.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户实体
 *
 * 支付系统中"商户"是最核心的概念——平台的一切收入来自商户的交易手续费。
 *
 * 关键字段说明：
 * - merchantNo:  平台内部商户编号，对外暴露比 ID 更安全（防止遍历）
 * - fee_rate:    每个商户的费率可以不同。大商户费率低（1.5%），小商户费率高（3.5%）
 *                这是支付公司的核心商业逻辑之一
 * - status:      商户状态，ACTIVE 才能收款。SUSPENDED 的商户发起的支付会被拒绝
 * - currency:    商户默认结算币种
 */
@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_no", nullable = false, unique = true, length = 20)
    private String merchantNo;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(length = 50)
    private String country;

    @Column(length = 10)
    private String currency = "USD";

    @Column(name = "fee_rate", precision = 5, scale = 4)
    private BigDecimal feeRate = new BigDecimal("0.0300");

    @Column(length = 20)
    private String status = "ACTIVE";

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

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMerchantNo() { return merchantNo; }
    public void setMerchantNo(String merchantNo) { this.merchantNo = merchantNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getFeeRate() { return feeRate; }
    public void setFeeRate(BigDecimal feeRate) { this.feeRate = feeRate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
