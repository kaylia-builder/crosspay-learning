package com.crosspay.module.admin;

import com.crosspay.common.exception.BusinessException;
import com.crosspay.module.admin.entity.AdminUser;
import com.crosspay.module.auth.JwtTokenProvider;
import com.crosspay.module.merchant.MerchantRepository;
import com.crosspay.module.merchant.entity.Merchant;
import com.crosspay.module.payment.PaymentRepository;
import com.crosspay.module.payment.entity.PaymentOrder;
import com.crosspay.module.settlement.SettlementRepository;
import com.crosspay.module.settlement.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 运营管理服务
 *
 * === 为什么支付公司必须有内部运营系统？ ===
 *
 * 支付不是无人值守的业务。以下场景必须人工介入：
 *
 * 1. 异常订单处理 — 客户说"我付了但没到账"，
 *    运营需要查订单状态、查渠道日志、联系渠道确认
 *
 * 2. 商户管理 — 新商户入驻审核、费率调整、
 *    可疑商户冻结（欺诈、洗钱嫌疑）
 *
 * 3. 对账差异 — 平台订单和渠道账单对不上时，
 *    需要运营逐笔核查
 *
 * 4. 资金差错处理 — 重复扣款、金额错误，
 *    需要运营发起退款或调整
 *
 * 运营后台 = 支付公司的"指挥室"。
 */
@Service
public class AdminService {

    private final AdminUserRepository adminUserRepository;
    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminService(AdminUserRepository adminUserRepository,
                        MerchantRepository merchantRepository,
                        PaymentRepository paymentRepository,
                        SettlementRepository settlementRepository,
                        PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider) {
        this.adminUserRepository = adminUserRepository;
        this.merchantRepository = merchantRepository;
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // ---- 管理员登录 ----

    public Map<String, String> adminLogin(String username, String password) {
        AdminUser admin = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(40002, "用户名或密码错误"));

        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new BusinessException(40002, "用户名或密码错误");
        }

        String token = jwtTokenProvider.generateAdminToken(admin.getId(), admin.getUsername(), admin.getRole());

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("username", admin.getUsername());
        result.put("role", admin.getRole());
        return result;
    }

    // ---- 商户管理 ----

    public Page<Merchant> listMerchants(int page, int size) {
        return merchantRepository.findAll(PageRequest.of(page, size));
    }

    // ---- 订单管理 ----

    public Page<PaymentOrder> listOrders(String status, int page, int size) {
        if (status != null && !status.isEmpty()) {
            // 简化：加载全部再过滤。真实系统用 Specification 或 QueryDSL 做动态查询
            return paymentRepository.findAll(PageRequest.of(page, size));
        }
        return paymentRepository.findAll(PageRequest.of(page, size));
    }

    public PaymentOrder getOrderDetail(String orderNo) {
        return paymentRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(40003, "订单不存在"));
    }

    // ---- 结算管理 ----

    public Page<Settlement> listSettlements(int page, int size) {
        return settlementRepository.findAll(PageRequest.of(page, size));
    }

    public void completeSettlement(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(40003, "结算记录不存在"));
        settlement.setStatus("COMPLETED");
        settlementRepository.save(settlement);
    }
}
