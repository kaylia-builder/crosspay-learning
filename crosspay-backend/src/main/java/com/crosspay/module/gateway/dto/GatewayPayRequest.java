package com.crosspay.module.gateway.dto;

import java.math.BigDecimal;

/**
 * 发送给渠道的支付请求
 * 注意：这里有 callbackUrl —— 渠道处理完后回调这个地址通知我们结果
 */
public class GatewayPayRequest {

    private String platformOrderNo;   // 平台订单号
    private BigDecimal amount;
    private String currency;
    private String callbackUrl;       // 渠道回调地址

    public GatewayPayRequest() {}

    public GatewayPayRequest(String platformOrderNo, BigDecimal amount, String currency, String callbackUrl) {
        this.platformOrderNo = platformOrderNo;
        this.amount = amount;
        this.currency = currency;
        this.callbackUrl = callbackUrl;
    }

    public String getPlatformOrderNo() { return platformOrderNo; }
    public void setPlatformOrderNo(String platformOrderNo) { this.platformOrderNo = platformOrderNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
}
