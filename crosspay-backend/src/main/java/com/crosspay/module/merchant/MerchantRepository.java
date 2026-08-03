package com.crosspay.module.merchant;

import com.crosspay.module.merchant.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByEmail(String email);

    Optional<Merchant> findByMerchantNo(String merchantNo);

    boolean existsByEmail(String email);
}
