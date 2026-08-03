package com.crosspay.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单号/商户号/结算号生成器
 *
 * 真实支付系统中，单号生成非常重要：
 * 1. 全局唯一 — 分布式环境下通常用 Snowflake 或号段模式
 * 2. 可追溯 — 号里带日期，一眼看出是哪天的订单
 * 3. 有业务含义 — PAY 开头是支付，STL 开头是结算，M 开头是商户
 *
 * 本学习项目用最简单的日期+随机数方案。
 */
public final class OrderNoGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private OrderNoGenerator() {}

    public static String generatePaymentNo() {
        return "PAY" + DATE_FMT.format(LocalDateTime.now())
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1, 999999));
    }

    public static String generateMerchantNo() {
        return "M" + DATE_FMT.format(LocalDateTime.now())
                + String.format("%04d", ThreadLocalRandom.current().nextInt(1, 9999));
    }

    public static String generateSettlementNo() {
        return "STL" + DATE_FMT.format(LocalDateTime.now())
                + String.format("%04d", ThreadLocalRandom.current().nextInt(1, 9999));
    }
}
