package com.crosspay.module.payment;

import com.crosspay.common.dto.ApiResponse;
import com.crosspay.module.payment.dto.CreatePaymentRequest;
import com.crosspay.module.payment.dto.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * 创建支付订单
     */
    @PostMapping("/create")
    public ApiResponse<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest req) {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.ok(paymentService.createPayment(merchantId, req));
    }

    /**
     * 查询支付订单状态
     */
    @GetMapping("/{orderNo}")
    public ApiResponse<PaymentResponse> query(@PathVariable String orderNo) {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.ok(paymentService.queryPayment(merchantId, orderNo));
    }

    /**
     * 交易列表
     */
    @GetMapping("/transactions")
    public ApiResponse<Page<PaymentResponse>> transactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.ok(paymentService.getTransactions(merchantId, page, size));
    }

    private Long getCurrentMerchantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
