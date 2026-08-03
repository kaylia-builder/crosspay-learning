package com.crosspay.module.auth;

import com.crosspay.common.exception.BusinessException;
import com.crosspay.common.util.OrderNoGenerator;
import com.crosspay.module.auth.dto.LoginRequest;
import com.crosspay.module.auth.dto.LoginResponse;
import com.crosspay.module.auth.dto.RegisterRequest;
import com.crosspay.module.merchant.MerchantRepository;
import com.crosspay.module.merchant.entity.Merchant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务
 *
 * 商户注册时做了三件事：
 * 1. 生成商户编号（M + 日期 + 随机数）
 * 2. 密码 BCrypt 加密（绝不能存明文）
 * 3. 设置默认费率 3%
 *
 * 真实支付公司还会做：KYC 身份验证、资质审核、反洗钱筛查。
 * 本项目跳过这些，只保留核心流程。
 */
@Service
public class AuthService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(MerchantRepository merchantRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse register(RegisterRequest req) {
        // 邮箱唯一性检查
        if (merchantRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException(40001, "该邮箱已注册");
        }

        Merchant merchant = new Merchant();
        merchant.setMerchantNo(OrderNoGenerator.generateMerchantNo());
        merchant.setName(req.getName());
        merchant.setEmail(req.getEmail());
        merchant.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        merchant.setCountry(req.getCountry());

        merchant = merchantRepository.save(merchant);

        String token = jwtTokenProvider.generateMerchantToken(
                merchant.getId(), merchant.getMerchantNo(), merchant.getEmail());

        return new LoginResponse(token, merchant.getMerchantNo(), merchant.getName());
    }

    public LoginResponse login(LoginRequest req) {
        Merchant merchant = merchantRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(40002, "邮箱或密码错误"));

        if (!passwordEncoder.matches(req.getPassword(), merchant.getPasswordHash())) {
            throw new BusinessException(40002, "邮箱或密码错误");
        }

        // 统一返回错误信息，不让攻击者分辨是邮箱不存在还是密码错误
        // （防止用户枚举攻击）

        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new BusinessException(40004, "商户已被禁用，请联系客服");
        }

        String token = jwtTokenProvider.generateMerchantToken(
                merchant.getId(), merchant.getMerchantNo(), merchant.getEmail());

        return new LoginResponse(token, merchant.getMerchantNo(), merchant.getName());
    }
}
