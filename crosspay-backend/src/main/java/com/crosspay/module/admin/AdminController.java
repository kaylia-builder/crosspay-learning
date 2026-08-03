package com.crosspay.module.admin;

import com.crosspay.common.dto.ApiResponse;
import com.crosspay.module.merchant.entity.Merchant;
import com.crosspay.module.payment.entity.PaymentOrder;
import com.crosspay.module.settlement.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 运营后台 Controller
 *
 * 这些接口都要求 ADMIN 角色（在 SecurityConfig 中配置）。
 * 管理员使用独立登录（/api/auth/admin/login），Token 中带有 ROLE_ADMIN。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ---- 商户管理 ----

    @GetMapping("/merchants")
    public ApiResponse<Page<Merchant>> listMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.listMerchants(page, size));
    }

    // ---- 订单管理 ----

    @GetMapping("/orders")
    public ApiResponse<Page<PaymentOrder>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.listOrders(status, page, size));
    }

    @GetMapping("/orders/{orderNo}")
    public ApiResponse<PaymentOrder> getOrderDetail(@PathVariable String orderNo) {
        return ApiResponse.ok(adminService.getOrderDetail(orderNo));
    }

    // ---- 结算管理 ----

    @GetMapping("/settlements")
    public ApiResponse<Page<Settlement>> listSettlements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.listSettlements(page, size));
    }

    @PostMapping("/settlement/{id}/complete")
    public ApiResponse<String> completeSettlement(@PathVariable Long id) {
        adminService.completeSettlement(id);
        return ApiResponse.ok("结算已确认完成");
    }
}
