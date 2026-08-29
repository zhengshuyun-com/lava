/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay;

import com.zhengshuyun.lava.crypto.CryptoUtils;
import com.zhengshuyun.lava.http.HttpClient;
import com.zhengshuyun.lava.http.HttpMethod;
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.json.JsonCodec;
import com.zhengshuyun.lava.pay.alipay.bill.BillType;
import com.zhengshuyun.lava.pay.alipay.exception.*;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayPayCryptoUtils;
import com.zhengshuyun.lava.pay.alipay.pagepay.*;
import com.zhengshuyun.lava.pay.alipay.refund.*;
import com.zhengshuyun.lava.pay.alipay.transaction.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.*;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlipayPayClientTest {
    private static final String APP_ID = "2026000000000001";
    private static final String SELLER_ID = "2088123456789012";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-29T04:00:00Z"), ZoneOffset.UTC);

    private static KeyPair appKeys;
    private static KeyPair alipayKeys;

    private AlipayPayTestServer server;
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
        server = AlipayPayTestServer.start(alipayKeys.getPrivate());
        client = AlipayPayClient.builder()
                .appId(APP_ID)
                .sellerId(SELLER_ID)
                .appPrivateKey(appKeys.getPrivate())
                .alipayPublicKey(alipayKeys.getPublic())
                .gatewayUrl(server.gatewayUrl())
                .clock(CLOCK)
                .build();
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.close();
    }

    @Test
    void pagePayGeneratesCompleteSignedPostFormAndEscapesHtml() {
        PagePayRequest request = PagePayRequest.builder()
                .outTradeNo("ORDER_001")
                .totalAmount(123)
                .subject("订单 ORDER_001")
                .body("说明\"><script>alert(1)</script>")
                .timeExpire(LocalDateTime.of(2026, 8, 29, 14, 0))
                .qrPayMode(PagePayQrMode.CUSTOM_WIDTH)
                .qrcodeWidth(240)
                .addEnablePayChannel("balance")
                .passbackParams("a=b&c=d")
                .addGoodsDetail(PagePayGoodsDetail.builder()
                        .goodsId("SKU_001")
                        .goodsName("测试商品")
                        .quantity(2)
                        .price(50)
                        .build())
                .storeId("STORE_001")
                .build();

        PagePayForm form = client.pagePay(
                        URI.create("https://pay.example.com/alipay/notify"),
                        URI.create("https://pay.example.com/alipay/return"))
                .createForm(request);

        assertEquals(PagePayForm.CONTENT_TYPE, "text/html;charset=UTF-8");
        assertFalse(form.html().contains("<script>alert(1)</script>"));
        assertTrue(form.html().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        String action = htmlUnescape(between(form.html(), "action=\"", "\">"));
        String bizContent = htmlUnescape(between(
                form.html(), "name=\"biz_content\" value=\"", "\">"));
        Map<String, String> params = queryParams(URI.create(action));
        String signature = params.remove("sign");
        params.put("biz_content", bizContent);

        assertTrue(AlipayPayCryptoUtils.verify(
                AlipayPayCryptoUtils.signatureContent(params), signature,
                appKeys.getPublic()));
        assertEquals("alipay.trade.page.pay", params.get("method"));
        assertEquals("2026-08-29 12:00:00", params.get("timestamp"));
        assertEquals("https://pay.example.com/alipay/notify", params.get("notify_url"));
        assertEquals("https://pay.example.com/alipay/return", params.get("return_url"));
        JsonNode body = JsonCodec.defaultCodec().readTree(bizContent);
        assertEquals("1.23", body.get("total_amount").stringValue());
        assertEquals("FAST_INSTANT_TRADE_PAY", body.get("product_code").stringValue());
        assertEquals("PCWEB", body.get("integration_type").stringValue());
        assertEquals("a%3Db%26c%3Dd", body.get("passback_params").stringValue());
        assertEquals("0.50", body.get("goods_detail").get(0).get("price").stringValue());
    }

    @Test
    void queryAndCloseUseSignedFormRequestsAndValidateIdentifiers() {
        server.enqueueSigned("alipay.trade.query", """
                {"code":"10000","msg":"Success","trade_no":"2026000000000000001",
                "out_trade_no":"ORDER_001","trade_status":"TRADE_SUCCESS",
                "total_amount":"1.23","buyer_pay_amount":"1.00",
                "send_pay_date":"2026-08-29 12:01:02",
                "fund_bill_list":[{"fund_channel":"ALIPAYACCOUNT","amount":"1.23"}],
                "extra":{"url":"https:\\/\\/example.com\\/x"}}
                """);

        Trade trade = client.transactions().query(
                TradeQueryRequest.builder()
                        .outTradeNo("ORDER_001")
                        .addQueryOption(TradeQueryOption.FUND_BILL_LIST)
                        .build());

        assertTrue(trade.paid());
        assertEquals(123, trade.totalAmount());
        assertEquals(100L, trade.buyerPayAmount());
        assertEquals(LocalDateTime.of(2026, 8, 29, 12, 1, 2), trade.sendPayDate());
        assertEquals(123, trade.fundBills().getFirst().amount());
        assertSignedRequest(server.takeRequest(), "alipay.trade.query", "ORDER_001");

        server.enqueueSigned("alipay.trade.close", """
                {"code":"10000","msg":"Success","trade_no":"2026000000000000001",
                "out_trade_no":"ORDER_001"}
                """);
        TradeCloseResult closed = client.transactions().closeByOutTradeNo("ORDER_001");
        assertEquals("ORDER_001", closed.outTradeNo());
        assertSignedRequest(server.takeRequest(), "alipay.trade.close", "ORDER_001");
    }

    @Test
    void refundRequiresStableRequestNumberAndUnknownResultNeedsQuery() {
        server.enqueueSigned("alipay.trade.refund", """
                {"code":"10000","msg":"Success","trade_no":"2026000000000000001",
                "out_trade_no":"ORDER_001","buyer_logon_id":"159****0000",
                "fund_change":"N","refund_fee":"0.50"}
                """);
        RefundResult applied = client.refunds().apply(RefundRequest.builder()
                .outTradeNo("ORDER_001")
                .outRequestNo("REFUND_001")
                .refundAmount(50)
                .reason("用户取消")
                .build());

        assertFalse(applied.succeeded());
        assertEquals(50, applied.refundedAmount());
        AlipayPayTestServer.CapturedRequest applyRequest = server.takeRequest();
        assertSignedRequest(applyRequest, "alipay.trade.refund", "ORDER_001");
        JsonNode applyBody = requestBizContent(applyRequest);
        assertEquals("REFUND_001", applyBody.get("out_request_no").stringValue());
        assertEquals("0.50", applyBody.get("refund_amount").stringValue());
        assertEquals(RefundQueryOption.DEPOSIT_BACK_INFO,
                applyBody.get("query_options").get(0).stringValue());

        server.enqueueSigned("alipay.trade.fastpay.refund.query", """
                {"code":"10000","msg":"Success","trade_no":"2026000000000000001",
                "out_trade_no":"ORDER_001","out_request_no":"REFUND_001",
                "total_amount":"1.23","refund_amount":"0.50",
                "refund_status":"REFUND_SUCCESS","gmt_refund_pay":"2026-08-29 12:05:00",
                "deposit_back_info":{"has_deposit_back":"true","dback_status":"S",
                "dback_amount":"0.50"}}
                """);
        RefundQueryResult queried = client.refunds().query(RefundQueryRequest.builder()
                .outTradeNo("ORDER_001")
                .outRequestNo("REFUND_001")
                .build());

        assertTrue(queried.succeeded());
        assertEquals(50L, queried.refundAmount());
        assertEquals(50L, queried.depositBackInfo().amount());
        assertSignedRequest(server.takeRequest(),
                "alipay.trade.fastpay.refund.query", "ORDER_001");
    }

    @Test
    void billQueryReturnsOnlyVerifiedDownloadUrl() {
        server.enqueueSigned("alipay.data.dataservice.bill.downloadurl.query", """
                {"code":"10000","msg":"Success",
                "bill_download_url":"https://bill.example.com/download?id=token"}
                """);
        var result = client.bills().queryDaily(
                BillType.TRADE, LocalDate.of(2026, 8, 28));

        assertEquals("bill.example.com", result.downloadUrl().getHost());
        AlipayPayTestServer.CapturedRequest captured = server.takeRequest();
        assertSignedRequest(captured,
                "alipay.data.dataservice.bill.downloadurl.query", null);
        assertEquals("2026-08-28",
                requestBizContent(captured).get("bill_date").stringValue());
    }

    @Test
    void signedApiErrorsAreStructuredAndRawResponseIsNotExposed() {
        server.enqueueSigned("alipay.trade.query", """
                {"code":"40004","msg":"Business Failed",
                "sub_code":"ACQ.TRADE_NOT_EXIST","sub_msg":"secret-response-value"}
                """);

        AlipayPayApiException failure = assertThrows(AlipayPayApiException.class,
                () -> client.transactions().queryByOutTradeNo("ORDER_001"));
        assertEquals("40004", failure.code());
        assertEquals("ACQ.TRADE_NOT_EXIST", failure.subCode());
        assertEquals("trace-id-001", failure.traceId());
        assertFalse(failure.toString().contains("secret-response-value"));
    }

    @Test
    void unsignedTamperedAndDuplicateResponsesFailClosed() {
        server.enqueueRaw(200, """
                {"alipay_trade_query_response":{"code":"10000","msg":"Success"}}
                """);
        AlipayPaySecurityException missing = assertThrows(
                AlipayPaySecurityException.class,
                () -> client.transactions().queryByOutTradeNo("ORDER_001"));
        assertEquals(AlipayPaySecurityFailure.MISSING_SIGNATURE, missing.failure());

        String source = "{\"code\":\"10000\",\"msg\":\"Success\","
                + "\"out_trade_no\":\"ORDER_001\",\"trade_status\":\"TRADE_SUCCESS\","
                + "\"total_amount\":\"1.00\"}";
        String wrongSignature = Base64.getEncoder().encodeToString(
                CryptoUtils.rsaSha256Sign(appKeys.getPrivate(),
                        source.getBytes(StandardCharsets.UTF_8)));
        server.enqueueRaw(200, "{\"alipay_trade_query_response\":" + source
                + ",\"sign\":\"" + wrongSignature + "\"}");
        AlipayPaySecurityException invalid = assertThrows(
                AlipayPaySecurityException.class,
                () -> client.transactions().queryByOutTradeNo("ORDER_001"));
        assertEquals(AlipayPaySecurityFailure.INVALID_SIGNATURE, invalid.failure());

        server.enqueueRaw(200, "{\"alipay_trade_query_response\":" + source
                + ",\"alipay_trade_query_response\":" + source
                + ",\"sign\":\"" + wrongSignature + "\"}");
        assertThrows(AlipayPayProtocolException.class,
                () -> client.transactions().queryByOutTradeNo("ORDER_001"));
    }

    @Test
    void responseIdentifiersAmountsAndHttpStatusFailClosed() {
        server.enqueueSigned("alipay.trade.query", """
                {"code":"10000","msg":"Success","out_trade_no":"ORDER_OTHER",
                "trade_status":"TRADE_SUCCESS","total_amount":"1.00"}
                """);
        AlipayPaySecurityException mismatch = assertThrows(
                AlipayPaySecurityException.class,
                () -> client.transactions().queryByOutTradeNo("ORDER_001"));
        assertEquals(AlipayPaySecurityFailure.RESPONSE_MISMATCH, mismatch.failure());

        server.enqueueSigned("alipay.trade.query", """
                {"code":"10000","msg":"Success","out_trade_no":"ORDER_001",
                "trade_status":"TRADE_SUCCESS","total_amount":"1.001"}
                """);
        assertThrows(AlipayPayProtocolException.class,
                () -> client.transactions().queryByOutTradeNo("ORDER_001"));

        server.enqueueRaw(502, "upstream failure");
        AlipayPayTransportException transport = assertThrows(
                AlipayPayTransportException.class,
                () -> client.transactions().queryByOutTradeNo("ORDER_001"));
        assertEquals(502, transport.statusCode());
        assertFalse(transport.toString().contains("upstream failure"));
    }

    @Test
    void closingAlipayClientDoesNotCloseBorrowedHttpClient() {
        HttpClient borrowed = HttpClient.builder().build();
        try {
            AlipayPayClient borrowedClient = AlipayPayClient.builder()
                    .appId(APP_ID)
                    .sellerId(SELLER_ID)
                    .appPrivateKey(appKeys.getPrivate())
                    .alipayPublicKey(alipayKeys.getPublic())
                    .gatewayUrl(server.gatewayUrl())
                    .httpClient(borrowed)
                    .build();
            borrowedClient.close();

            server.enqueueRaw(200, "{}");
            var response = borrowed.send(HttpRequest.builder(
                    server.gatewayUrl().toASCIIString(), HttpMethod.GET).build());
            assertEquals(200, response.statusCode());
        } finally {
            borrowed.close();
        }
    }

    @Test
    void buildersRejectAmbiguousOrUnsafeInputsAndAcceptRawBase64Keys() {
        assertThrows(IllegalArgumentException.class, () -> PagePayRequest.builder()
                .outTradeNo("bad-order")
                .totalAmount(1)
                .subject("test")
                .build());
        assertThrows(IllegalArgumentException.class, () -> PagePayRequest.builder()
                .outTradeNo("ORDER_001")
                .totalAmount(1)
                .subject("test")
                .addEnablePayChannel("balance")
                .addDisablePayChannel("creditCard")
                .build());
        assertThrows(IllegalArgumentException.class, () -> TradeQueryRequest.builder()
                .outTradeNo("ORDER_001")
                .tradeNo("2026000000000000001")
                .build());
        assertThrows(IllegalArgumentException.class, () -> RefundRequest.builder()
                .outTradeNo("ORDER_001")
                .refundAmount(1)
                .build());
        assertThrows(IllegalArgumentException.class, () -> client.bills().queryDaily(
                BillType.TRADE, LocalDate.of(2026, 8, 29)));

        try (AlipayPayClient rawKeyClient = AlipayPayClient.builder()
                .appId(APP_ID)
                .sellerId(SELLER_ID)
                .appPrivateKey(Base64.getEncoder().encodeToString(
                        appKeys.getPrivate().getEncoded()))
                .alipayPublicKey(Base64.getEncoder().encodeToString(
                        alipayKeys.getPublic().getEncoded()))
                .build()) {
            assertEquals(APP_ID, rawKeyClient.appId());
        }
    }

    @Test
    void closedClientRejectsPreviouslyObtainedEntries() {
        TransactionClient transactions = client.transactions();
        RefundClient refunds = client.refunds();
        client.close();

        assertThrows(IllegalStateException.class,
                () -> transactions.queryByOutTradeNo("ORDER_001"));
        assertThrows(IllegalStateException.class, () -> refunds.apply(null));
        assertThrows(IllegalStateException.class, client::notifications);
    }

    private static void assertSignedRequest(AlipayPayTestServer.CapturedRequest request,
                                            String method,
                                            String expectedOutTradeNo) {
        assertEquals("POST", request.method());
        Map<String, String> params = new LinkedHashMap<>(request.queryParams());
        String signature = params.remove("sign");
        String bizContent = request.formParams().get("biz_content");
        params.put("biz_content", bizContent);
        assertEquals(method, params.get("method"));
        assertEquals("2026-08-29 12:00:00", params.get("timestamp"));
        assertTrue(AlipayPayCryptoUtils.verify(
                AlipayPayCryptoUtils.signatureContent(params), signature,
                appKeys.getPublic()));
        if (expectedOutTradeNo != null) {
            assertEquals(expectedOutTradeNo,
                    JsonCodec.defaultCodec().readTree(bizContent)
                            .get("out_trade_no").stringValue());
        }
    }

    private static JsonNode requestBizContent(AlipayPayTestServer.CapturedRequest request) {
        return JsonCodec.defaultCodec().readTree(
                request.formParams().get("biz_content"));
    }

    private static Map<String, String> queryParams(URI uri) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String item : uri.getRawQuery().split("&")) {
            String[] pair = item.split("=", 2);
            result.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
        }
        return result;
    }

    private static String between(String value, String prefix, String suffix) {
        int start = value.indexOf(prefix);
        assertTrue(start >= 0, "prefix not found: " + prefix);
        start += prefix.length();
        int end = value.indexOf(suffix, start);
        assertTrue(end >= 0, "suffix not found: " + suffix);
        return value.substring(start, end);
    }

    private static String htmlUnescape(String value) {
        return value.replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }
}
