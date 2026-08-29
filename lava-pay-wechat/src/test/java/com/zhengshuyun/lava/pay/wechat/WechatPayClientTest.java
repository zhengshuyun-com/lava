/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.wechat;

import com.zhengshuyun.lava.crypto.CryptoUtils;
import com.zhengshuyun.lava.http.HttpClient;
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.json.JsonCodec;
import com.zhengshuyun.lava.pay.wechat.exception.*;
import com.zhengshuyun.lava.pay.wechat.nativepay.NativePrepayDetail;
import com.zhengshuyun.lava.pay.wechat.nativepay.NativePrepayRequest;
import com.zhengshuyun.lava.pay.wechat.nativepay.NativePrepayResponse;
import com.zhengshuyun.lava.pay.wechat.nativepay.NativePrepaySceneInfo;
import com.zhengshuyun.lava.pay.wechat.refund.Refund;
import com.zhengshuyun.lava.pay.wechat.refund.RefundRequest;
import com.zhengshuyun.lava.pay.wechat.transaction.TradeState;
import com.zhengshuyun.lava.pay.wechat.transaction.Transaction;
import org.junit.jupiter.api.*;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class WechatPayClientTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-29T00:00:00Z"), ZoneOffset.UTC);
    private static final String MCHID = "1900000109";
    private static final String APPID = "wx1234567890";
    private static final String API_V3_KEY = "0123456789abcdef0123456789abcdef";
    private static final String REQUEST_NONCE = "0123456789abcdef0123456789abcdef";
    private static final Pattern SIGNATURE = Pattern.compile("signature=\"([^\"]+)\"");

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
        client = clientBuilder().build();
    }

    private WechatPayClient.Builder clientBuilder() {
        return WechatPayClient.builder()
                .mchid(MCHID)
                .merchantPrivateKey(merchantKeys.getPrivate())
                .merchantSerialNo("ABCDEF0123456789")
                .apiV3Key(API_V3_KEY)
                .wechatPayPublicKeyId(WechatPayTestServer.PUBLIC_KEY_ID)
                .wechatPayPublicKey(wechatKeys.getPublic())
                .apiBaseUrl(server.baseUrl())
                .clock(CLOCK)
                .nonceSupplier(() -> REQUEST_NONCE);
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.close();
    }

    @Test
    void nativePrepayInjectsApplicationConfigurationAndSignsExactBody() {
        server.enqueueSigned(200,
                "{\"code_url\":\"weixin://wxpay/bizpayurl?pr=test\"}");
        NativePrepayRequest request = NativePrepayRequest.builder()
                .description("测试订单")
                .outTradeNo("ORDER_001")
                .amount(100)
                .profitSharing(false)
                .detail(NativePrepayDetail.builder()
                        .costPrice(100)
                        .build())
                .build();

        NativePrepayResponse response = client
                .application(APPID, "https://example.com/pay/notify")
                .nativePay().prepay(request);

        assertEquals("weixin", response.codeUrl().getScheme());
        WechatPayTestServer.CapturedRequest captured = server.takeRequest();
        assertEquals("POST", captured.method());
        assertEquals("/v3/pay/transactions/native", captured.target());
        JsonNode body = JsonCodec.defaultCodec().readTree(
                new String(captured.body(), StandardCharsets.UTF_8));
        assertEquals(APPID, body.get("appid").stringValue());
        assertEquals(MCHID, body.get("mchid").stringValue());
        assertEquals("https://example.com/pay/notify",
                body.get("notify_url").stringValue());
        assertEquals(100, body.get("amount").get("total").asInt());
        assertFalse(body.has("time_expire"));
        assertFalse(body.has("attach"));
        assertFalse(body.get("detail").has("invoice_id"));
        assertFalse(body.get("detail").has("goods_detail"));
        assertTrue(verifyRequestSignature(captured));
        assertEquals(WechatPayTestServer.PUBLIC_KEY_ID,
                captured.header("Wechatpay-Serial"));
    }

    @Test
    void transactionQueryAndCloseUseOfficialPaths() {
        String transaction = """
                {"appid":"wx1234567890","mchid":"1900000109",
                 "out_trade_no":"ORDER_001","trade_state":"NOTPAY",
                 "trade_state_desc":"未支付","future_field":"kept-compatible"}
                """;
        server.enqueueSigned(200, transaction);
        server.enqueueSigned(204, "");

        Transaction result = client.transactions().queryByOutTradeNo("ORDER_001");
        client.transactions().close("ORDER_001");

        assertEquals(TradeState.NOTPAY, result.tradeState());
        WechatPayTestServer.CapturedRequest query = server.takeRequest();
        assertEquals("GET", query.method());
        assertEquals("/v3/pay/transactions/out-trade-no/ORDER_001?mchid=1900000109",
                query.target());
        assertTrue(verifyRequestSignature(query));
        WechatPayTestServer.CapturedRequest close = server.takeRequest();
        assertEquals("/v3/pay/transactions/out-trade-no/ORDER_001/close",
                close.target());
        assertTrue(verifyRequestSignature(close));
    }

    @Test
    void transactionIdQueryUsesOfficialPath() {
        server.enqueueSigned(200, """
                {"appid":"wx1234567890","mchid":"1900000109",
                 "out_trade_no":"ORDER_001","transaction_id":"4200000001",
                 "trade_state":"SUCCESS","trade_state_desc":"支付成功"}
                """);

        Transaction result = client.transactions()
                .queryByTransactionId("4200000001");

        assertEquals("4200000001", result.transactionId());
        WechatPayTestServer.CapturedRequest query = server.takeRequest();
        assertEquals("/v3/pay/transactions/id/4200000001?mchid=1900000109",
                query.target());
        assertTrue(verifyRequestSignature(query));
    }

    @Test
    void refundApplyAndQueryShareTypedRefundModel() {
        String refund = refundJson();
        server.enqueueSigned(200, refund);
        server.enqueueSigned(200, refund);
        RefundRequest request = RefundRequest.builder()
                .outTradeNo("ORDER_001")
                .outRefundNo("REFUND_001")
                .amount(50, 100)
                .reason("用户取消")
                .notifyUrl("https://example.com/refund/notify")
                .build();

        Refund applied = client.refunds().apply(request);
        Refund queried = client.refunds().queryByOutRefundNo("REFUND_001");

        assertEquals("REFUND_001", applied.outRefundNo());
        assertEquals(applied.refundId(), queried.refundId());
        WechatPayTestServer.CapturedRequest applyRequest = server.takeRequest();
        assertEquals("/v3/refund/domestic/refunds", applyRequest.target());
        JsonNode applyBody = JsonCodec.defaultCodec().readTree(
                new String(applyRequest.body(), StandardCharsets.UTF_8));
        assertFalse(applyBody.has("transaction_id"));
        assertEquals("https://example.com/refund/notify",
                applyBody.get("notify_url").stringValue());
        assertFalse(applyBody.get("amount").has("from"));
        assertEquals("/v3/refund/domestic/refunds/REFUND_001",
                server.takeRequest().target());
    }

    @Test
    void signedApiErrorIsStructuredAndRawBodyIsNotInExceptionText() {
        server.enqueueSigned(400, """
                {"code":"PARAM_ERROR","message":"参数错误",
                 "detail":{"field":"/amount/total","value":"secret-value",
                 "issue":"must be positive","location":"body"}}
                """);

        WechatPayApiException failure = assertThrows(WechatPayApiException.class,
                () -> client.transactions().queryByOutTradeNo("ORDER_001"));

        assertEquals(400, failure.statusCode());
        assertEquals("PARAM_ERROR", failure.code());
        assertEquals("request-id-001", failure.requestId());
        assertEquals("/amount/total", failure.detail().field());
        assertFalse(failure.toString().contains("secret-value"));
    }

    @Test
    void responseWithUnexpectedPublicKeyIdFailsClosed() {
        server.enqueueSigned(200,
                "{\"code_url\":\"weixin://wxpay/bizpayurl?pr=test\"}"
                        .getBytes(StandardCharsets.UTF_8),
                "PUB_KEY_ID_999", wechatKeys.getPrivate(),
                CLOCK.instant().getEpochSecond());

        WechatPaySecurityException failure = assertThrows(WechatPaySecurityException.class,
                () -> client.application(APPID, "https://example.com/pay/notify")
                        .nativePay().prepay(NativePrepayRequest.builder()
                                .description("测试")
                                .outTradeNo("ORDER_001")
                                .amount(1)
                                .build()));

        assertEquals(WechatPaySecurityFailure.UNEXPECTED_PUBLIC_KEY_ID,
                failure.failure());
    }

    @Test
    void responseWithInvalidSignatureFailsClosed() {
        server.enqueueSigned(200,
                "{\"code_url\":\"weixin://wxpay/bizpayurl?pr=test\"}"
                        .getBytes(StandardCharsets.UTF_8),
                WechatPayTestServer.PUBLIC_KEY_ID, merchantKeys.getPrivate(),
                CLOCK.instant().getEpochSecond());

        WechatPaySecurityException failure = assertThrows(WechatPaySecurityException.class,
                () -> client.application(APPID, "https://example.com/pay/notify")
                        .nativePay().prepay(NativePrepayRequest.builder()
                                .description("测试")
                                .outTradeNo("ORDER_001")
                                .amount(1)
                                .build()));

        assertEquals(WechatPaySecurityFailure.INVALID_SIGNATURE, failure.failure());
    }

    @Test
    void signedMalformedSuccessResponseUsesProtocolException() {
        server.enqueueSigned(200, "{}");

        assertThrows(WechatPayProtocolException.class,
                () -> client.application(APPID, "https://example.com/pay/notify")
                        .nativePay().prepay(NativePrepayRequest.builder()
                                .description("测试")
                                .outTradeNo("ORDER_001")
                                .amount(1)
                                .build()));
    }

    @Test
    void transactionQueryRejectsResponseForAnotherMerchant() {
        server.enqueueSigned(200, """
                {"appid":"wx1234567890","mchid":"1900000999",
                 "out_trade_no":"ORDER_001","trade_state":"NOTPAY",
                 "trade_state_desc":"未支付"}
                """);

        WechatPaySecurityException failure = assertThrows(
                WechatPaySecurityException.class,
                () -> client.transactions().queryByOutTradeNo("ORDER_001"));
        assertEquals(WechatPaySecurityFailure.MERCHANT_MISMATCH,
                failure.failure());
    }

    @Test
    void transactionAndRefundResponsesMustMatchRequestedBusinessIdentifiers() {
        server.enqueueSigned(200, """
                {"appid":"wx1234567890","mchid":"1900000109",
                 "out_trade_no":"ORDER_002","trade_state":"NOTPAY",
                 "trade_state_desc":"未支付"}
                """);

        WechatPaySecurityException transactionFailure = assertThrows(
                WechatPaySecurityException.class,
                () -> client.transactions().queryByOutTradeNo("ORDER_001"));
        assertEquals(WechatPaySecurityFailure.RESPONSE_MISMATCH,
                transactionFailure.failure());

        server.enqueueSigned(200, refundJson().replace(
                "\"refund\":50", "\"refund\":40"));
        RefundRequest request = RefundRequest.builder()
                .outTradeNo("ORDER_001")
                .outRefundNo("REFUND_001")
                .amount(50, 100)
                .build();

        WechatPaySecurityException refundFailure = assertThrows(
                WechatPaySecurityException.class,
                () -> client.refunds().apply(request));
        assertEquals(WechatPaySecurityFailure.RESPONSE_MISMATCH,
                refundFailure.failure());
    }

    @Test
    void closingWechatClientDoesNotCloseBorrowedHttpClient() {
        client.close();
        try (HttpClient borrowed = HttpClient.builder().build()) {
            try (WechatPayClient borrowedWechat = clientBuilder()
                    .httpClient(borrowed)
                    .build()) {
                borrowedWechat.close();
            }

            server.enqueueUnsigned(200, "ok".getBytes(StandardCharsets.UTF_8),
                    "text/plain; charset=utf-8");
            var response = borrowed.send(HttpRequest.get(
                    server.baseUrl().resolve("borrowed-client-check")).build());
            assertEquals(200, response.statusCode());
            assertEquals("ok", response.getBodyAsString());
        }
    }

    @Test
    void requestBuildersRejectAmbiguousIdentifiersAndOverflowingContributions() {
        assertThrows(IllegalArgumentException.class, () -> RefundRequest.builder()
                .outRefundNo("REFUND_001")
                .amount(1, 1)
                .build());
        assertThrows(IllegalArgumentException.class, () -> RefundRequest.builder()
                .transactionId("4200000001")
                .outTradeNo("ORDER_001")
                .outRefundNo("REFUND_001")
                .amount(1, 1)
                .build());
        assertThrows(IllegalArgumentException.class, () -> RefundRequest.builder()
                .outTradeNo("ORDER_001")
                .outRefundNo("REFUND_001")
                .amount(Long.MAX_VALUE, Long.MAX_VALUE)
                .addAmountFrom(new RefundRequest.AmountFrom("AVAILABLE", Long.MAX_VALUE))
                .addAmountFrom(new RefundRequest.AmountFrom("UNAVAILABLE", 1))
                .build());
        assertThrows(IllegalArgumentException.class,
                () -> WechatPayClient.builder().apiV3Key(
                        "0123456789abcdef0123456789abcde!"));
        assertThrows(IllegalArgumentException.class,
                () -> WechatPayClient.builder().apiBaseUrl("http://example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> WechatPayClient.builder().apiBaseUrl("https://example.com/proxy"));
        assertThrows(IllegalArgumentException.class,
                () -> client.application(APPID, "https://127.0.0.1/pay/notify"));
        assertThrows(IllegalArgumentException.class,
                () -> client.application(APPID, "https://localhost/pay/notify"));
        assertThrows(IllegalArgumentException.class,
                () -> NativePrepaySceneInfo.builder()
                        .payerClientIp("not-an-ip-address")
                        .build());
        assertThrows(IllegalArgumentException.class,
                () -> NativePrepaySceneInfo.builder()
                        .payerClientIp("fe80::1%en0")
                        .build());
        assertThrows(IllegalArgumentException.class,
                () -> NativePrepayDetail.GoodsDetail.builder()
                        .merchantGoodsId("不支持的编码")
                        .quantity(1)
                        .unitPrice(1)
                        .build());
    }

    @Test
    void closedClientRejectsExistingEntryObjects() {
        var transactions = client.transactions();
        var refunds = client.refunds();
        var bills = client.bills();
        var notifications = client.notifications();
        var nativePay = client.application(
                APPID, "https://example.com/pay/notify").nativePay();

        client.close();

        assertThrows(IllegalStateException.class,
                () -> transactions.queryByOutTradeNo("ORDER_001"));
        assertThrows(IllegalStateException.class, () -> refunds.apply(null));
        assertThrows(IllegalStateException.class, () -> bills.applyTradeBill(null));
        assertThrows(IllegalStateException.class,
                () -> notifications.parseTransaction(null, (byte[]) null));
        assertThrows(IllegalStateException.class, () -> nativePay.prepay(null));
    }

    private static boolean verifyRequestSignature(
            WechatPayTestServer.CapturedRequest request) {
        String authorization = request.header("Authorization");
        Matcher matcher = SIGNATURE.matcher(authorization);
        assertTrue(matcher.find());
        byte[] signature = Base64.getDecoder().decode(matcher.group(1));
        String message = request.method() + '\n'
                + request.target() + '\n'
                + CLOCK.instant().getEpochSecond() + '\n'
                + REQUEST_NONCE + '\n'
                + new String(request.body(), StandardCharsets.UTF_8) + '\n';
        return CryptoUtils.rsaSha256Verify(merchantKeys.getPublic(),
                message.getBytes(StandardCharsets.UTF_8), signature);
    }

    private static String refundJson() {
        return """
                {"refund_id":"5000000001","out_refund_no":"REFUND_001",
                 "transaction_id":"4200000001","out_trade_no":"ORDER_001",
                 "channel":"ORIGINAL","user_received_account":"支付用户零钱",
                 "create_time":"2026-08-29T08:00:00+08:00","status":"PROCESSING",
                 "funds_account":"BASIC","amount":{"total":100,"refund":50,
                 "payer_total":100,"payer_refund":50,"settlement_refund":50,
                 "settlement_total":100,"discount_refund":0,"currency":"CNY"}}
                """;
    }
}
