/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.wechat;

import com.zhengshuyun.lava.crypto.CryptoUtils;
import com.zhengshuyun.lava.http.HttpHeaders;
import com.zhengshuyun.lava.pay.wechat.exception.*;
import com.zhengshuyun.lava.pay.wechat.notification.RefundNotification;
import com.zhengshuyun.lava.pay.wechat.notification.TransactionNotification;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class NotificationParserTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-29T00:00:00Z"), ZoneOffset.UTC);
    private static final String MCHID = "1900000109";
    private static final byte[] API_V3_KEY =
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NONCE = "0123456789ab".getBytes(StandardCharsets.UTF_8);

    private static KeyPair merchantKeys;
    private static KeyPair wechatKeys;
    private WechatPayTestServer server;
    private WechatPayClient client;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        merchantKeys = generator.generateKeyPair();
        wechatKeys = generator.generateKeyPair();
    }

    @BeforeEach
    void setUp() {
        server = WechatPayTestServer.start(wechatKeys.getPrivate(), CLOCK);
        client = WechatPayClient.builder()
                .mchid(MCHID)
                .merchantPrivateKey(merchantKeys.getPrivate())
                .merchantSerialNo("ABCDEF")
                .apiV3Key(API_V3_KEY)
                .wechatPayPublicKeyId(WechatPayTestServer.PUBLIC_KEY_ID)
                .wechatPayPublicKey(wechatKeys.getPublic())
                .apiBaseUrl(server.baseUrl())
                .clock(CLOCK)
                .build();
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.close();
    }

    @Test
    void transactionNotificationIsVerifiedBeforeDecryption() {
        String plaintext = """
                {"appid":"wx1234567890","mchid":"1900000109",
                 "out_trade_no":"ORDER_001","transaction_id":"4200000001",
                 "trade_type":"NATIVE","trade_state":"SUCCESS",
                 "trade_state_desc":"支付成功","bank_type":"OTHERS",
                 "success_time":"2026-08-29T08:00:00+08:00",
                 "payer":{"openid":"openid"},
                 "amount":{"total":100,"payer_total":100,
                 "currency":"CNY","payer_currency":"CNY"}}
                """;
        byte[] body = envelope("TRANSACTION.SUCCESS", "transaction", plaintext);
        HttpHeaders headers = signedHeaders(body, CLOCK.instant().getEpochSecond());

        TransactionNotification notification = client.notifications()
                .parseTransaction(headers, body);

        assertEquals("ORDER_001", notification.transaction().outTradeNo());
        assertEquals("SUCCESS", notification.transaction().tradeState());

        body[body.length - 2] ^= 1;
        WechatPaySecurityException failure = assertThrows(WechatPaySecurityException.class,
                () -> client.notifications().parseTransaction(headers, body));
        assertEquals(WechatPaySecurityFailure.INVALID_SIGNATURE, failure.failure());
    }

    @Test
    void refundNotificationPreservesOfficialStatus() {
        String plaintext = """
                {"mchid":"1900000109","out_trade_no":"ORDER_001",
                 "transaction_id":"4200000001","out_refund_no":"REFUND_001",
                 "refund_id":"5000000001","refund_status":"ABNORMAL",
                 "user_received_account":"支付用户零钱",
                 "amount":{"total":100,"refund":50,
                 "payer_total":100,"payer_refund":50}}
                """;
        byte[] body = envelope("REFUND.ABNORMAL", "refund", plaintext);

        RefundNotification notification = client.notifications().parseRefund(
                signedHeaders(body, CLOCK.instant().getEpochSecond()), body);

        assertEquals("ABNORMAL", notification.refund().refundStatus());
        assertEquals("REFUND_001", notification.refund().outRefundNo());
    }

    @Test
    void staleNotificationAndMerchantMismatchFailClosed() {
        String plaintext = """
                {"appid":"wx1234567890","mchid":"other-mchid",
                 "out_trade_no":"ORDER_001","trade_state":"SUCCESS",
                 "trade_state_desc":"支付成功"}
                """;
        byte[] body = envelope("TRANSACTION.SUCCESS", "transaction", plaintext);

        WechatPaySecurityException stale = assertThrows(WechatPaySecurityException.class,
                () -> client.notifications().parseTransaction(
                        signedHeaders(body, CLOCK.instant().minusSeconds(301).getEpochSecond()),
                        body));
        assertEquals(WechatPaySecurityFailure.EXPIRED_TIMESTAMP, stale.failure());

        WechatPaySecurityException mismatch = assertThrows(WechatPaySecurityException.class,
                () -> client.notifications().parseTransaction(
                        signedHeaders(body, CLOCK.instant().getEpochSecond()), body));
        assertEquals(WechatPaySecurityFailure.MERCHANT_MISMATCH, mismatch.failure());
    }

    @Test
    void malformedEnvelopeFailsAsProtocolErrorAfterSignatureVerification() {
        byte[] body = """
                {"id":"EV-001","create_time":"2026-08-29T08:00:00+08:00",
                 "resource_type":"encrypt-resource",
                 "event_type":"TRANSACTION.SUCCESS","summary":"测试通知"}
                """.getBytes(StandardCharsets.UTF_8);

        assertThrows(WechatPayProtocolException.class,
                () -> client.notifications().parseTransaction(
                        signedHeaders(body, CLOCK.instant().getEpochSecond()), body));

        String incompletePlaintext = """
                {"appid":"wx1234567890","mchid":"1900000109",
                 "out_trade_no":"ORDER_001","transaction_id":"4200000001",
                 "trade_type":"NATIVE","trade_state":"SUCCESS",
                 "trade_state_desc":"支付成功","bank_type":"OTHERS",
                 "success_time":"2026-08-29T08:00:00+08:00"}
                """;
        byte[] incompleteBody = envelope(
                "TRANSACTION.SUCCESS", "transaction", incompletePlaintext);
        assertThrows(WechatPayProtocolException.class,
                () -> client.notifications().parseTransaction(
                        signedHeaders(incompleteBody, CLOCK.instant().getEpochSecond()),
                        incompleteBody));
    }

    @Test
    void unsupportedAlgorithmAndWrongApiV3KeyFailWithStableSecurityCategories() {
        String plaintext = """
                {"appid":"wx1234567890","mchid":"1900000109",
                 "out_trade_no":"ORDER_001","transaction_id":"4200000001",
                 "trade_type":"NATIVE","trade_state":"SUCCESS",
                 "trade_state_desc":"支付成功"}
                """;
        byte[] unsupportedBody = new String(
                envelope("TRANSACTION.SUCCESS", "transaction", plaintext),
                StandardCharsets.UTF_8)
                .replace("AEAD_AES_256_GCM", "UNSUPPORTED")
                .getBytes(StandardCharsets.UTF_8);

        WechatPaySecurityException unsupported = assertThrows(
                WechatPaySecurityException.class,
                () -> client.notifications().parseTransaction(
                        signedHeaders(unsupportedBody, CLOCK.instant().getEpochSecond()),
                        unsupportedBody));
        assertEquals(WechatPaySecurityFailure.UNSUPPORTED_ENCRYPTION_ALGORITHM,
                unsupported.failure());

        byte[] body = envelope("TRANSACTION.SUCCESS", "transaction", plaintext);
        byte[] wrongKey = "fedcba9876543210fedcba9876543210"
                .getBytes(StandardCharsets.UTF_8);
        try (WechatPayClient wrongKeyClient = WechatPayClient.builder()
                .mchid(MCHID)
                .merchantPrivateKey(merchantKeys.getPrivate())
                .merchantSerialNo("ABCDEF")
                .apiV3Key(wrongKey)
                .wechatPayPublicKeyId(WechatPayTestServer.PUBLIC_KEY_ID)
                .wechatPayPublicKey(wechatKeys.getPublic())
                .apiBaseUrl(server.baseUrl())
                .clock(CLOCK)
                .build()) {
            WechatPaySecurityException decryption = assertThrows(
                    WechatPaySecurityException.class,
                    () -> wrongKeyClient.notifications().parseTransaction(
                            signedHeaders(body, CLOCK.instant().getEpochSecond()), body));
            assertEquals(WechatPaySecurityFailure.DECRYPTION_FAILED,
                    decryption.failure());
        }
    }

    private static byte[] envelope(String eventType, String originalType, String plaintext) {
        byte[] ciphertext = CryptoUtils.aesGcmEncrypt(API_V3_KEY, NONCE, new byte[0],
                plaintext.getBytes(StandardCharsets.UTF_8));
        String json = """
                {"id":"EV-001","create_time":"2026-08-29T08:00:00+08:00",
                 "resource_type":"encrypt-resource","event_type":"%s",
                 "summary":"测试通知","resource":{"original_type":"%s",
                 "algorithm":"AEAD_AES_256_GCM","ciphertext":"%s",
                 "associated_data":"","nonce":"%s"}}
                """.formatted(eventType, originalType,
                Base64.getEncoder().encodeToString(ciphertext),
                new String(NONCE, StandardCharsets.UTF_8));
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static HttpHeaders signedHeaders(byte[] body, long timestamp) {
        return WechatPayTestServer.signedHeaders(body, wechatKeys.getPrivate(),
                WechatPayTestServer.PUBLIC_KEY_ID, timestamp);
    }
}
