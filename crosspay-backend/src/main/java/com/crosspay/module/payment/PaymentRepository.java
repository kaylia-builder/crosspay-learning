package com.crosspay.module.payment;

import com.crosspay.module.payment.entity.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderNo(String orderNo);

    Page<PaymentOrder> findByMerchantIdOrderByCreatedAtDesc(Long merchantId, Pageable pageable);

    /**
     * 今日交易笔数
     */
    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.merchantId = :merchantId AND p.createdAt >= :since")
    long countTodayByMerchant(@Param("merchantId") Long merchantId, @Param("since") LocalDateTime since);

    /**
     * 今日交易总金额
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p WHERE p.merchantId = :merchantId AND p.createdAt >= :since")
    BigDecimal sumTodayAmountByMerchant(@Param("merchantId") Long merchantId, @Param("since") LocalDateTime since);

    /**
     * 今日成功交易笔数
     */
    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.merchantId = :merchantId AND p.status = 'SUCCESS' AND p.createdAt >= :since")
    long countTodaySuccessByMerchant(@Param("merchantId") Long merchantId, @Param("since") LocalDateTime since);
}
