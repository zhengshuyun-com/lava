/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.wechat;

import com.zhengshuyun.lava.pay.wechat.bill.*;
import com.zhengshuyun.lava.pay.wechat.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class BillClientTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-29T00:00:00Z"), ZoneOffset.UTC);
    private static KeyPair merchantKeys;
    private static KeyPair wechatKeys;

    @TempDir
    Path temporaryDirectory;
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
                .mchid("1900000109")
                .merchantPrivateKey(merchantKeys.getPrivate())
                .merchantSerialNo("ABCDEF")
                .apiV3Key("0123456789abcdef0123456789abcdef")
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
    void billIsStreamedVerifiedAndPublishedWithoutOverwrite() throws Exception {
        byte[] file = "交易时间,商户订单号\n2026-08-28,ORDER_001\n"
                .getBytes(StandardCharsets.UTF_8);
        String hash = sha1(file);
        String info = "{\"hash_type\":\"SHA1\",\"hash_value\":\"" + hash
                + "\",\"download_url\":\"" + server.baseUrl()
                + "download?token=secret\"}";
        server.enqueueSigned(200, info);
        server.enqueueUnsigned(200, file, "text/csv");

        BillDownloadInfo download = client.bills().applyTradeBill(
                TradeBillRequest.builder()
                        .billDate(LocalDate.of(2026, 8, 28))
                        .billType(TradeBillType.ALL)
                        .build());
        Path target = temporaryDirectory.resolve("trade.csv");
        BillDownloadResult result = client.bills().download(download, target);

        assertArrayEquals(file, Files.readAllBytes(target));
        assertEquals(file.length, result.size());
        assertEquals(hash, result.hashValue());
        assertTrue(server.takeRequest().target().contains("bill_date=2026-08-28"));
        assertEquals("/download?token=secret", server.takeRequest().target());

        WechatPayFileException exists = assertThrows(WechatPayFileException.class,
                () -> client.bills().download(download, target));
        assertEquals(WechatPayFileFailure.TARGET_EXISTS, exists.failure());
    }

    @Test
    void hashMismatchLeavesNoPublishedOrPartialFile() throws Exception {
        byte[] file = "changed".getBytes(StandardCharsets.UTF_8);
        String info = "{\"hash_type\":\"SHA1\",\"hash_value\":\""
                + "0000000000000000000000000000000000000000"
                + "\",\"download_url\":\"" + server.baseUrl() + "download\"}";
        server.enqueueSigned(200, info);
        server.enqueueUnsigned(200, file, "text/csv");
        BillDownloadInfo download = client.bills().applyFundFlowBill(
                FundFlowBillRequest.builder()
                        .billDate(LocalDate.of(2026, 8, 28))
                        .build());
        Path target = temporaryDirectory.resolve("fund.csv");

        WechatPaySecurityException failure = assertThrows(WechatPaySecurityException.class,
                () -> client.bills().download(download, target));

        assertEquals(WechatPaySecurityFailure.HASH_MISMATCH, failure.failure());
        assertFalse(Files.exists(target));
        try (var files = Files.list(temporaryDirectory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".part")));
        }
    }

    @Test
    void gzipBillIsDecompressedBeforeHashVerificationAndPublication() throws Exception {
        byte[] file = "交易时间,商户订单号\n2026-08-28,ORDER_001\n"
                .getBytes(StandardCharsets.UTF_8);
        String info = "{\"hash_type\":\"SHA1\",\"hash_value\":\"" + sha1(file)
                + "\",\"download_url\":\"" + server.baseUrl()
                + "download?token=secret\"}";
        server.enqueueSigned(200, info);
        server.enqueueUnsigned(200, gzip(file), "application/gzip");

        BillDownloadInfo download = client.bills().applyTradeBill(
                TradeBillRequest.builder()
                        .billDate(LocalDate.of(2026, 8, 28))
                        .tarType(BillTarType.GZIP)
                        .build());
        Path target = temporaryDirectory.resolve("trade.csv");
        BillDownloadResult result = client.bills().download(download, target);

        assertArrayEquals(file, Files.readAllBytes(target));
        assertEquals(file.length, result.size());
        assertTrue(server.takeRequest().target().contains("tar_type=GZIP"));
        assertEquals("/download?token=secret", server.takeRequest().target());
    }

    @Test
    void malformedSignedBillMetadataFailsBeforeDownload() {
        String info = "{\"hash_type\":\"SHA1\",\"hash_value\":\"not-a-sha1\","
                + "\"download_url\":\"" + server.baseUrl() + "download\"}";
        server.enqueueSigned(200, info);

        assertThrows(WechatPayProtocolException.class,
                () -> client.bills().applyTradeBill(TradeBillRequest.builder()
                        .billDate(LocalDate.of(2026, 8, 28))
                        .build()));
    }

    @Test
    void manuallyConstructedDownloadInfoCannotSendCredentialsToAnotherOrigin() {
        BillDownloadInfo forged = new BillDownloadInfo(
                "SHA1", "0000000000000000000000000000000000000000",
                URI.create("https://example.com/v3/billdownload/file?token=secret"), null);
        Path target = temporaryDirectory.resolve("forged.csv");

        assertThrows(WechatPayProtocolException.class,
                () -> client.bills().download(forged, target));
        assertFalse(Files.exists(target));
    }

    private static String sha1(byte[] value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(value));
    }

    private static byte[] gzip(byte[] value) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(value);
        }
        return output.toByteArray();
    }
}
