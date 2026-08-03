package com.crosspay.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "商户名称不能为空")
    @Size(max = 100)
    private String name;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6, max = 50)
    private String password;

    private String country = "Kenya";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
