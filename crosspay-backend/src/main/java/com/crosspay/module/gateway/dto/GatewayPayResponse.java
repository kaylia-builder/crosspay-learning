package com.crosspay.module.gateway.dto;

/**
 * 渠道返回的支付结果
 */
public class GatewayPayResponse {

    private boolean success;
    private String channelOrderNo;    // 渠道侧订单号
    private String status;            // PROCESSING | SUCCESS | FAILED
    private String message;           // 描述信息

    public GatewayPayResponse() {}

    public GatewayPayResponse(boolean success, String channelOrderNo, String status, String message) {
        this.success = success;
        this.channelOrderNo = channelOrderNo;
        this.status = status;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getChannelOrderNo() { return channelOrderNo; }
    public void setChannelOrderNo(String channelOrderNo) { this.channelOrderNo = channelOrderNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
