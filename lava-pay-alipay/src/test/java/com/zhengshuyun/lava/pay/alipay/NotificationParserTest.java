/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay;

import com.zhengshuyun.lava.pay.alipay.exception.AlipayPaySecurityException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipayPaySecurityFailure;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayPayCryptoUtils;
import com.zhengshuyun.lava.pay.alipay.notification.RefundDepositBackNotification;
import com.zhengshuyun.lava.pay.alipay.notification.TradeNotification;
import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationParserTest {
    private static final String APP_ID = "2026000000000001";
    private static final String SELLER_ID = "2088123456789012";

    private static KeyPair appKeys;
    private static KeyPair alipayKeys;

    private AlipayPayClient client;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        appKeys = generator.generateKeyPair();
        alipayKeys = generator.generateKeyPair();
    }

    @BeforeEach
    void setUp() {
        client = AlipayPayClient.builder()
                .appId(APP_ID)
                .sellerId(SELLER_ID)
                .appPrivateKey(appKeys.getPrivate())
                .alipayPublicKey(alipayKeys.getPublic())
                .build();
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void tradeNotificationIsVerifiedBeforeBusinessMatching() {
        Map<String, String> params = signed(Map.ofEntries(
                Map.entry("notify_time", "2026-08-29 12:03:00"),
                Map.entry("notify_type", "trade_status_sync"),
                Map.entry("notify_id", "notify-trade-001"),
                Map.entry("charset", "UTF-8"),
                Map.entry("version", "1.0"),
                Map.entry("sign_type", "RSA2"),
                Map.entry("app_id", APP_ID),
                Map.entry("seller_id", SELLER_ID),
                Map.entry("trade_no", "2026000000000000001"),
                Map.entry("out_trade_no", "ORDER_001"),
                Map.entry("trade_status", "TRADE_SUCCESS"),
                Map.entry("total_amount", "1.23"),
                Map.entry("receipt_amount", "1.00"),
                Map.entry("gmt_payment", "2026-08-29 12:02:00")));

        TradeNotification notification = client.notifications().parseTrade(params)
                .requireOrder("ORDER_001", 123);

        assertTrue(notification.paid());
        assertEquals(100L, notification.receiptAmount());
        assertEquals(LocalDateTime.of(2026, 8, 29, 12, 2),
                notification.paymentTime());
        assertTrue(params.containsKey("sign"), "parser must not mutate caller parameters");
        AlipayPaySecurityException mismatch = assertThrows(
                AlipayPaySecurityException.class,
                () -> notification.requireOrder("ORDER_001", 124));
        assertEquals(AlipayPaySecurityFailure.RESPONSE_MISMATCH, mismatch.failure());
    }

    @Test
    void tamperedOrWrongMerchantNotificationsFailClosed() {
        Map<String, String> tampered = signed(baseTradeParams(SELLER_ID));
        tampered.put("total_amount", "9.99");
        AlipayPaySecurityException invalid = assertThrows(
                AlipayPaySecurityException.class,
                () -> client.notifications().parseTrade(tampered));
        assertEquals(AlipayPaySecurityFailure.INVALID_SIGNATURE, invalid.failure());

        Map<String, String> wrongSeller = signed(baseTradeParams("2088000000000000"));
        AlipayPaySecurityException seller = assertThrows(
                AlipayPaySecurityException.class,
                () -> client.notifications().parseTrade(wrongSeller));
        assertEquals(AlipayPaySecurityFailure.SELLER_MISMATCH, seller.failure());
    }

    @Test
    void refundDepositBackMessageUsesFromAlipayV1Signature() {
        Map<String, String> params = signed(Map.ofEntries(
                Map.entry("notify_id", "notify-refund-001"),
                Map.entry("utc_timestamp", "1787976000000"),
                Map.entry("msg_method", "alipay.trade.refund.depositback.completed"),
                Map.entry("app_id", APP_ID),
                Map.entry("version", "1.1"),
                Map.entry("charset", "UTF-8"),
                Map.entry("sign_type", "RSA2"),
                Map.entry("biz_content", """
                        {"trade_no":"2026000000000000001","out_trade_no":"ORDER_001",
                        "out_request_no":"REFUND_001","dback_status":"S",
                        "dback_amount":"0.50","bank_ack_time":"2026-08-29 12:10:00",
                        "est_bank_receipt_time":"2026-08-30 12:00:00"}
                        """.strip())));

        RefundDepositBackNotification notification =
                client.notifications().parseRefundDepositBack(params);

        assertTrue(notification.bankDepositSucceeded());
        assertEquals(50L, notification.depositBackAmount());
        assertEquals(Instant.ofEpochMilli(1_787_976_000_000L), notification.sentAt());
        assertEquals("REFUND_001", notification.outRequestNo());
    }

    private static Map<String, String> baseTradeParams(String sellerId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("notify_time", "2026-08-29 12:03:00");
        params.put("notify_type", "trade_status_sync");
        params.put("notify_id", "notify-trade-001");
        params.put("charset", "UTF-8");
        params.put("version", "1.0");
        params.put("sign_type", "RSA2");
        params.put("app_id", APP_ID);
        params.put("seller_id", sellerId);
        params.put("trade_no", "2026000000000000001");
        params.put("out_trade_no", "ORDER_001");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("total_amount", "1.23");
        return params;
    }

    private static Map<String, String> signed(Map<String, String> values) {
        Map<String, String> params = new LinkedHashMap<>(values);
        Map<String, String> signable = new LinkedHashMap<>(params);
        signable.remove("sign_type");
        params.put("sign", AlipayPayCryptoUtils.sign(signable, alipayKeys.getPrivate()));
        return params;
    }
}
