package com.crosspay.module.settlement;

import com.crosspay.common.dto.ApiResponse;
import com.crosspay.module.settlement.dto.SettlementResponse;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settlement")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    /**
     * 查看当前商户的结算记录
     */
    @GetMapping("/list")
    public ApiResponse<Page<SettlementResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.ok(settlementService.getSettlements(merchantId, page, size));
    }

    /**
     * 手动触发结算（学习用，真实系统是定时任务）
     */
    @PostMapping("/trigger")
    public ApiResponse<SettlementResponse> trigger() {
        Long merchantId = getCurrentMerchantId();
        SettlementResponse result = settlementService.createSettlement(merchantId);
        return ApiResponse.ok(result);
    }

    private Long getCurrentMerchantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
