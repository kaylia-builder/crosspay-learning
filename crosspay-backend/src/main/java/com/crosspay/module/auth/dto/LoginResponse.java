package com.crosspay.module.auth.dto;

public class LoginResponse {

    private String token;
    private String merchantNo;
    private String name;

    public LoginResponse(String token, String merchantNo, String name) {
        this.token = token;
        this.merchantNo = merchantNo;
        this.name = name;
    }

    public String getToken() { return token; }
    public String getMerchantNo() { return merchantNo; }
    public String getName() { return name; }
}
