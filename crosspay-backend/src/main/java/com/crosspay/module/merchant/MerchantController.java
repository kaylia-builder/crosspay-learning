package com.crosspay.module.merchant;

import com.crosspay.common.dto.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * 获取当前登录商户信息
     * 商户 ID 从 JWT 中解析并由 SecurityContext 传入
     */
    @GetMapping("/profile")
    public ApiResponse<?> profile() {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.ok(merchantService.getMerchant(merchantId));
    }

    /**
     * 商户 Dashboard
     */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.ok(merchantService.getDashboard(merchantId));
    }

    // TODO: 支付模块完成后，加上 /transactions 接口

    /**
     * 从 SecurityContext 获取当前商户 ID
     * JWT Filter 在请求进来时已经把 userId 放进 Authentication 了
     */
    private Long getCurrentMerchantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
