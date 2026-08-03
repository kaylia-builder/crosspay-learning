package com.crosspay.module.settlement;

import com.crosspay.module.settlement.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Page<Settlement> findByMerchantIdOrderBySettlementDateDesc(Long merchantId, Pageable pageable);
}
