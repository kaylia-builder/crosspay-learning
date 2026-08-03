package com.crosspay.module.settlement.dto;

import com.crosspay.module.settlement.entity.Settlement;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SettlementResponse {

    private String settlementNo;
    private BigDecimal totalAmount;
    private BigDecimal feeAmount;
    private BigDecimal netAmount;
    private String currency;
    private LocalDate settlementDate;
    private String status;

    public static SettlementResponse from(Settlement s) {
        SettlementResponse r = new SettlementResponse();
        r.settlementNo = s.getSettlementNo();
        r.totalAmount = s.getTotalAmount();
        r.feeAmount = s.getFeeAmount();
        r.netAmount = s.getNetAmount();
        r.currency = s.getCurrency();
        r.settlementDate = s.getSettlementDate();
        r.status = s.getStatus();
        return r;
    }

    public String getSettlementNo() { return settlementNo; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public String getCurrency() { return currency; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public String getStatus() { return status; }
}
