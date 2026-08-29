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
import com.zhengshuyun.lava.pay.alipay.internal.AlipayCryptoUtils;
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

class AlipayClientTest {
    private static final String APP_ID = "2026000000000001";
    private static final String SELLER_ID = "2088123456789012";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-29T04:00:00Z"), ZoneOffset.UTC);

    private static KeyPair appKeys;
    private static KeyPair alipayKeys;

    private AlipayTestServer server;
    private AlipayClient client;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        appKeys = generator.generateKeyPair();
        alipayKeys = generator.generateKeyPair();
    }

    @BeforeEach
    void setUp() {
        server = AlipayTestServer.start(alipayKeys.getPrivate());
        client = AlipayClient.builder()
                .appId(APP_ID)
                .sellerId(SELLER_ID)
                .appPrivateKey(appKeys.getPrivate())
                .alipayPublicKey(alipayKeys.getPublic())
                .baseUrl(server.baseUrl())
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
                .timeExpire(LocalDateTime.of(
                        2026,
                        8,
                        29,
                        14,
                        0
                ))
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

        PagePayForm form = client.pagePay(URI.create("https://pay.example.com/alipay/notify"), URI.create("https://pay.example.com/alipay/return"))
                .createForm(request);

        assertEquals(PagePayForm.CONTENT_TYPE, "text/html;charset=UTF-8");
        assertFalse(form.html().contains("<script>alert(1)</script>"));
        assertTrue(form.html().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        String action = htmlUnescape(between(form.html(), "action=\"", "\">"));
        String bizContent = htmlUnescape(between(form.html(), "name=\"biz_content\" value=\"", "\">"));
        URI actionUri = URI.create(action);
        assertEquals("/gateway.do", actionUri.getPath());
        Map<String, String> params = queryParams(actionUri);
        String signature = params.remove("sign");
        params.put("biz_content", bizContent);

        assertTrue(AlipayCryptoUtils.verify(AlipayCryptoUtils.signatureContent(params), signature, appKeys.getPublic()));
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
    void queryAndCloseUseV3SignedJsonRequestsAndValidateIdentifiers() {
        server.enqueueSigned(
                """
                               {"trade_no":"2026000000000000001",
                               "out_trade_no":"ORDER_001","trade_status":"TRADE_SUCCESS",
                               "total_amount":"1.23","buyer_pay_amount":"1.00",
                               "send_pay_date":"2026-08-29 12:01:02",
                               "fund_bill_list":[{"fund_channel":"ALIPAYACCOUNT","amount":"1.23"}],
                               "extra":{"url":"https:\\/\\/example.com\\/x"}}
                               """
        );

        Trade trade = client.transactions().query(
                TradeQueryRequest.builder()
                        .outTradeNo("ORDER_001")
                        .addQueryOption(TradeQueryOption.FUND_BILL_LIST)
                        .build());

        assertTrue(trade.paid());
        assertSame(trade, trade.requireOrder("ORDER_001", 123));
        assertEquals(123, trade.totalAmount());
        assertEquals(100L, trade.buyerPayAmount());
        assertEquals(
                LocalDateTime.of(
                        2026,
                        8,
                        29,
                        12,
                        1,
                        2
                ),
                trade.sendPayDate()
        );
        assertEquals(123, trade.fundBills().getFirst().amount());
        assertSignedRequest(
                server.takeRequest(),
                "POST",
                "/v3/alipay/trade/query"
        );

        server.enqueueSigned(
                """
                               {"trade_no":"2026000000000000001",
                               "out_trade_no":"ORDER_001"}
                               """
        );
        TradeCloseResult closed = client.transactions().closeByOutTradeNo("ORDER_001");
        assertEquals("ORDER_001", closed.outTradeNo());
        assertSignedRequest(
                server.takeRequest(),
                "POST",
                "/v3/alipay/trade/close"
        );
    }

    @Test
    void refundRequiresStableRequestNumberAndUnknownResultNeedsQuery() {
        server.enqueueSigned(
                """
                               {"trade_no":"2026000000000000001",
                               "out_trade_no":"ORDER_001","buyer_logon_id":"159****0000",
                               "fund_change":"N","refund_fee":"0.50"}
                               """
        );
        RefundResult applied = client.refunds().apply(RefundRequest.builder()
                .outTradeNo("ORDER_001")
                .outRequestNo("REFUND_001")
                .refundAmount(50)
                .reason("用户取消")
                .build());

        assertFalse(applied.succeeded());
        assertEquals(50, applied.refundedAmount());
        AlipayTestServer.CapturedRequest applyRequest = server.takeRequest();
        assertSignedRequest(
                applyRequest,
                "POST",
                "/v3/alipay/trade/refund"
        );
        JsonNode applyBody = requestBody(applyRequest);
        assertEquals("REFUND_001", applyBody.get("out_request_no").stringValue());
        assertEquals("0.50", applyBody.get("refund_amount").stringValue());
        assertEquals(RefundQueryOption.DEPOSIT_BACK_INFO, applyBody.get("query_options").get(0).stringValue());

        server.enqueueSigned(
                """
                               {"trade_no":"2026000000000000001",
                               "out_trade_no":"ORDER_001","out_request_no":"REFUND_001",
                               "total_amount":"1.23","refund_amount":"0.50",
                               "refund_status":"REFUND_SUCCESS","gmt_refund_pay":"2026-08-29 12:05:00",
                               "deposit_back_info":{"has_deposit_back":"true","dback_status":"S",
                               "dback_amount":"0.50"}}
                               """
        );
        RefundQueryResult queried = client.refunds().query(RefundQueryRequest.builder()
                .outTradeNo("ORDER_001")
                .outRequestNo("REFUND_001")
                .build());

        assertTrue(queried.succeeded());
        assertSame(queried, queried.requireRefund(
                "ORDER_001",
                "REFUND_001",
                123,
                50
        ));
        assertEquals(50L, queried.refundAmount());
        assertEquals(50L, queried.depositBackInfo().amount());
        assertSignedRequest(
                server.takeRequest(),
                "POST",
                "/v3/alipay/trade/fastpay/refund/query"
        );
    }

    @Test
    void billQueryReturnsOnlyVerifiedDownloadUrl() {
        server.enqueueSigned(
                """
                               {"bill_download_url":"https://bill.example.com/download?id=token"}
                               """
        );
        var result = client.bills().queryDaily(BillType.TRADE, LocalDate.of(2026, 8, 28));

        assertEquals("bill.example.com", result.downloadUrl().getHost());
        assertFalse(result.toString().contains("token"));
        AlipayTestServer.CapturedRequest captured = server.takeRequest();
        assertSignedRequest(
                captured,
                "GET",
                "/v3/alipay/data/dataservice/bill/downloadurl/query"
        );
        assertEquals("2026-08-28", captured.queryParams().get("bill_date"));
    }

    @Test
    void signedApiErrorsAreStructuredAndRawResponseIsNotExposed() {
        server.enqueueSigned(
                400,
                """
                               {"code":"ACQ.TRADE_NOT_EXIST","message":"secret-response-value",
                               "links":[{"link":"https://example.com/help","rel":"解决方案"}]}
                               """
        );

        AlipayApiException failure = assertThrows(AlipayApiException.class, () -> client.transactions().queryByOutTradeNo("ORDER_001"));
        assertEquals(400, failure.statusCode());
        assertTrue(failure.verified());
        assertEquals("ACQ.TRADE_NOT_EXIST", failure.code());
        assertEquals("https://example.com/help", failure.links().getFirst().link());
        assertEquals("trace-id-001", failure.traceId());
        assertFalse(failure.toString().contains("secret-response-value"));
    }

    @Test
    void unsignedStructuredApiErrorKeepsStatusAndVerificationState() {
        server.enqueueRaw(
                429,
                """
                        {"code":"RATE_LIMIT","message":"调用频率超限"}
                        """
        );

        AlipayApiException failure = assertThrows(
                AlipayApiException.class,
                () -> client.transactions().queryByOutTradeNo("ORDER_001")
        );

        assertEquals(429, failure.statusCode());
        assertFalse(failure.verified());
        assertEquals("RATE_LIMIT", failure.code());
    }

    @Test
    void acceptedResponseIsNotTreatedAsFinalSuccess() {
        server.enqueueSigned(
                202,
                """
                        {"out_trade_no":"ORDER_001","trade_status":"TRADE_SUCCESS",
                         "total_amount":"1.00"}
                        """
        );

        AlipayTransportException failure = assertThrows(
                AlipayTransportException.class,
                () -> client.transactions().queryByOutTradeNo("ORDER_001")
        );

        assertEquals(202, failure.statusCode());
    }

    @Test
    void unsignedAndTamperedResponsesFailClosed() {
        server.enqueueRaw(
                200,
                """
                               {"out_trade_no":"ORDER_001","trade_status":"TRADE_SUCCESS",
                               "total_amount":"1.00"}
                               """
        );
        AlipaySecurityException missing = assertThrows(AlipaySecurityException.class, () -> client.transactions().queryByOutTradeNo("ORDER_001"));
        assertEquals(AlipaySecurityFailure.MISSING_SIGNATURE, missing.failure());

        String source = "{\"out_trade_no\":\"ORDER_001\",\"trade_status\":\"TRADE_SUCCESS\","
                + "\"total_amount\":\"1.00\"}";
        String wrongSignature = Base64.getEncoder().encodeToString(
                CryptoUtils.rsaSha256Sign(appKeys.getPrivate(), source.getBytes(StandardCharsets.UTF_8)));
        server.enqueueRawSigned(
                200,
                source,
                wrongSignature
        );
        AlipaySecurityException invalid = assertThrows(AlipaySecurityException.class, () -> client.transactions().queryByOutTradeNo("ORDER_001"));
        assertEquals(AlipaySecurityFailure.INVALID_SIGNATURE, invalid.failure());
    }

    @Test
    void duplicateV3SignatureHeadersFailClosed() {
        server.enqueueDuplicateSignature(
                """
                        {"out_trade_no":"ORDER_001","trade_status":"TRADE_SUCCESS",
                         "total_amount":"1.00"}
                        """
        );

        AlipaySecurityException failure = assertThrows(
                AlipaySecurityException.class,
                () -> client.transactions().queryByOutTradeNo("ORDER_001")
        );

        assertEquals(
                AlipaySecurityFailure.DUPLICATE_SIGNATURE_HEADER,
                failure.failure()
        );
    }

    @Test
    void responseIdentifiersAmountsAndHttpStatusFailClosed() {
        server.enqueueSigned(
                """
                               {"out_trade_no":"ORDER_OTHER",
                               "trade_status":"TRADE_SUCCESS","total_amount":"1.00"}
                               """
        );
        AlipaySecurityException mismatch = assertThrows(AlipaySecurityException.class, () -> client.transactions().queryByOutTradeNo("ORDER_001"));
        assertEquals(AlipaySecurityFailure.RESPONSE_MISMATCH, mismatch.failure());

        server.enqueueSigned(
                """
                               {"out_trade_no":"ORDER_001",
                               "trade_status":"TRADE_SUCCESS","total_amount":"1.001"}
                               """
        );
        assertThrows(AlipayProtocolException.class, () -> client.transactions().queryByOutTradeNo("ORDER_001"));

        server.enqueueRaw(502, "upstream failure");
        AlipayTransportException transport = assertThrows(AlipayTransportException.class, () -> client.transactions().queryByOutTradeNo("ORDER_001"));
        assertEquals(502, transport.statusCode());
        assertFalse(transport.toString().contains("upstream failure"));
    }

    @Test
    void closingAlipayClientDoesNotCloseBorrowedHttpClient() {
        HttpClient borrowed = HttpClient.builder()
                .retryOnConnectionFailure(false)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
        try {
            AlipayClient borrowedClient = AlipayClient.builder()
                    .appId(APP_ID)
                    .sellerId(SELLER_ID)
                    .appPrivateKey(appKeys.getPrivate())
                    .alipayPublicKey(alipayKeys.getPublic())
                    .baseUrl(server.baseUrl())
                    .httpClient(borrowed)
                    .build();
            borrowedClient.close();

            server.enqueueRaw(200, "{}");
            var response = borrowed.send(HttpRequest.builder(
                    server.baseUrl().toASCIIString(),
                    HttpMethod.GET
            ).build());
            assertEquals(200, response.statusCode());
        } finally {
            borrowed.close();
        }
    }

    @Test
    void buildersFollowV3IdentifierRulesRejectUnsafeInputsAndAcceptRawBase64Keys() {
        IllegalArgumentException invalidNotifyUrl = assertThrows(IllegalArgumentException.class, () -> client.pagePay("invalid uri", "https://pay.example.com/return"));
        assertTrue(invalidNotifyUrl.getMessage().contains("notifyUrl"));

        assertThrows(
                IllegalArgumentException.class,
                () -> PagePayRequest.builder()
                               .outTradeNo("bad-order")
                               .totalAmount(1)
                               .subject("test")
                               .build()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PagePayRequest.builder()
                               .outTradeNo("ORDER_001")
                               .totalAmount(1)
                               .subject("test")
                               .addEnablePayChannel("balance")
                               .addDisablePayChannel("creditCard")
                               .build()
        );
        assertDoesNotThrow(
                () -> TradeQueryRequest.builder()
                               .outTradeNo("ORDER_001")
                               .tradeNo("2026000000000000001")
                               .build()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RefundRequest.builder()
                               .outTradeNo("ORDER_001")
                               .refundAmount(1)
                               .build()
        );
        assertThrows(IllegalArgumentException.class, () -> client.bills().queryDaily(BillType.TRADE, LocalDate.of(2026, 8, 29)));
        assertThrows(
                IllegalArgumentException.class,
                () -> AlipayClient.builder().baseUrl("https://example.com")
        );
        try (HttpClient unsafeHttpClient = HttpClient.builder().build()) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> AlipayClient.builder().httpClient(unsafeHttpClient)
            );
        }

        try (AlipayClient rawKeyClient = AlipayClient.builder()
                .appId(APP_ID)
                .sellerId(SELLER_ID)
                .appPrivateKey(Base64.getEncoder().encodeToString(
                        appKeys.getPrivate().getEncoded()))
                .alipayPublicKey(Base64.getEncoder().encodeToString(
                        alipayKeys.getPublic().getEncoded()))
                .build()) {
            assertEquals(APP_ID, rawKeyClient.appId());
        }

        AlipayClient.Builder incompleteBuilder = AlipayClient.builder()
                .appId(APP_ID)
                .appPrivateKey(appKeys.getPrivate())
                .alipayPublicKey(alipayKeys.getPublic());
        assertThrows(IllegalArgumentException.class, incompleteBuilder::build);
        incompleteBuilder.sellerId(SELLER_ID);
        IllegalArgumentException clearedPrivateKey = assertThrows(IllegalArgumentException.class, incompleteBuilder::build);
        assertTrue(clearedPrivateKey.getMessage().contains("appPrivateKey"));

        AlipayClient.Builder oneShotBuilder = AlipayClient.builder()
                .appId(APP_ID)
                .sellerId(SELLER_ID)
                .appPrivateKey(appKeys.getPrivate())
                .alipayPublicKey(alipayKeys.getPublic());
        try (AlipayClient ignored = oneShotBuilder.build()) {
            assertThrows(IllegalStateException.class, oneShotBuilder::build);
            assertThrows(IllegalStateException.class, () -> oneShotBuilder.appId(APP_ID));
        }
    }

    @Test
    void closedClientRejectsPreviouslyObtainedEntries() {
        TransactionClient transactions = client.transactions();
        RefundClient refunds = client.refunds();
        client.close();

        assertThrows(IllegalStateException.class, () -> transactions.queryByOutTradeNo("ORDER_001"));
        assertThrows(IllegalStateException.class, () -> refunds.apply(null));
        assertThrows(IllegalStateException.class, client::notifications);
    }

    private static void assertSignedRequest(
            AlipayTestServer.CapturedRequest request,
            String expectedMethod,
            String expectedPath
    ) {
        assertEquals(expectedMethod, request.method());
        assertTrue(request.target().startsWith(expectedPath));
        String authorization = request.header("Authorization");
        assertNotNull(authorization);
        String prefix = "ALIPAY-SHA256withRSA ";
        assertTrue(authorization.startsWith(prefix));
        String parameters = authorization.substring(prefix.length());
        int signatureStart = parameters.lastIndexOf(",sign=");
        assertTrue(signatureStart > 0);
        String authString = parameters.substring(0, signatureStart);
        String signature = parameters.substring(signatureStart + ",sign=".length());
        assertTrue(authString.startsWith("app_id=" + APP_ID + ",nonce="));
        assertTrue(authString.endsWith(",timestamp=1787976000000"));
        String requestId = request.header("alipay-request-id");
        assertNotNull(requestId);
        assertEquals(32, requestId.length());

        String source = authString + "\n"
                + expectedMethod + "\n"
                + request.target() + "\n"
                + request.bodyText() + "\n";
        assertTrue(AlipayCryptoUtils.verify(source, signature, appKeys.getPublic()));
    }

    private static JsonNode requestBody(AlipayTestServer.CapturedRequest request) {
        return JsonCodec.defaultCodec().readTree(request.bodyText());
    }

    private static Map<String, String> queryParams(URI uri) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String item : uri.getRawQuery().split("&")) {
            String[] pair = item.split("=", 2);
            result.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
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
