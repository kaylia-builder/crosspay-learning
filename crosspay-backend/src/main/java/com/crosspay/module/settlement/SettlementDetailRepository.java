package com.crosspay.module.settlement;

import com.crosspay.module.settlement.entity.SettlementDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementDetailRepository extends JpaRepository<SettlementDetail, Long> {
}
