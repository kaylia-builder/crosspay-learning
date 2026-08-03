package com.crosspay.module.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class CreatePaymentRequest {

    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    private BigDecimal amount;

    private String currency = "USD";

    private String merchantOrderNo;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getMerchantOrderNo() { return merchantOrderNo; }
    public void setMerchantOrderNo(String merchantOrderNo) { this.merchantOrderNo = merchantOrderNo; }
}
