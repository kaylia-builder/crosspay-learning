package com.crosspay.module.payment;

import com.crosspay.module.payment.entity.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderNo(String orderNo);

    Page<PaymentOrder> findByMerchantIdOrderByCreatedAtDesc(Long merchantId, Pageable pageable);
}
