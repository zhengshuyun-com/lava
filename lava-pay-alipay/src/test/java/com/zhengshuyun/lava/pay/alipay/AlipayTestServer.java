/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zhengshuyun.lava.crypto.CryptoUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 为支付宝客户端测试提供本地 HTTP 网关，按队列返回可签名响应并捕获实际请求。
 */
final class AlipayTestServer implements AutoCloseable {
    /**
     * 模拟 V3 响应签名使用的固定毫秒时间戳。
     */
    private static final String RESPONSE_TIMESTAMP = "1787976000000";
    /**
     * 模拟 V3 响应签名使用的固定随机字符串。
     */
    private static final String RESPONSE_NONCE = "response-nonce-001";

    /**
     * 绑定到本机随机端口的 JDK HTTP 服务端。
     */
    private final HttpServer server;
    /**
     * 为每个模拟 HTTP 交换创建虚拟线程的执行器。
     */
    private final ExecutorService executor;
    /**
     * 用于生成模拟支付宝 V3 响应签名的 RSA 私钥。
     */
    private final PrivateKey alipayPrivateKey;
    /**
     * 按请求到达顺序消费的预设响应队列；未预设时服务端返回 500。
     */
    private final BlockingQueue<PlannedResponse> responses = new LinkedBlockingQueue<>();
    /**
     * 保存已接收请求的阻塞队列，供测试线程按到达顺序取出断言。
     */
    private final BlockingQueue<CapturedRequest> requests = new LinkedBlockingQueue<>();

    /**
     * 封装已创建但由该测试服务器管理生命周期的 HTTP 服务端、执行器和签名私钥。
     *
     * @param server 已绑定本地地址的 HTTP 服务端
     * @param executor 处理请求的虚拟线程执行器
     * @param alipayPrivateKey 签署模拟支付宝响应的 RSA 私钥
     */
    private AlipayTestServer(
            HttpServer server,
            ExecutorService executor,
            PrivateKey alipayPrivateKey
    ) {
        this.server = server;
        this.executor = executor;
        this.alipayPrivateKey = alipayPrivateKey;
    }

    /**
     * 在本机回环地址的随机端口启动支付宝模拟网关。
     *
     * @param alipayPrivateKey 签署模拟 V3 响应的 RSA 私钥
     * @return 已启动且可接收请求的模拟服务端
     * @throws AssertionError 当本地地址绑定或 HTTP 服务端创建失败时抛出
     */
    static AlipayTestServer start(PrivateKey alipayPrivateKey) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            AlipayTestServer result = new AlipayTestServer(
                    server,
                    executor,
                    alipayPrivateKey
            );
            server.createContext("/", result::handle);
            server.setExecutor(executor);
            server.start();
            return result;
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    /**
     * 返回模拟网关当前实际监听的 HTTP 根地址。
     *
     * @return 指向本机回环地址和随机端口的 URI
     */
    URI baseUrl() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    /**
     * 预设下一次请求返回经支付宝私钥签名的 200 JSON 响应。
     *
     * @param responseBody 响应 JSON 文本，入队前会去除首尾空白
     */
    void enqueueSigned(String responseBody) {
        enqueueSigned(200, responseBody);
    }

    /**
     * 预设下一次请求返回指定状态码且经支付宝私钥签名的 JSON 响应。
     *
     * @param status 要返回的 HTTP 状态码
     * @param responseBody 响应 JSON 文本，入队前会去除首尾空白
     */
    void enqueueSigned(int status, String responseBody) {
        String body = responseBody.strip();
        String source = RESPONSE_TIMESTAMP + "\n" + RESPONSE_NONCE + "\n" + body + "\n";
        String signature = Base64.getEncoder().encodeToString(
                CryptoUtils.rsaSha256Sign(
                        alipayPrivateKey,
                        source.getBytes(StandardCharsets.UTF_8)
                )
        );
        responses.add(new PlannedResponse(
                status,
                body.getBytes(StandardCharsets.UTF_8),
                signature,
                false
        ));
    }

    /**
     * 预设下一次请求返回不含任何 V3 签名头的原始响应。
     *
     * @param status 要返回的 HTTP 状态码
     * @param body 不作修改的 UTF-8 响应正文
     */
    void enqueueRaw(int status, String body) {
        responses.add(new PlannedResponse(
                status,
                body.getBytes(StandardCharsets.UTF_8),
                null,
                false
        ));
    }

    /**
     * 预设下一次请求返回由调用方指定签名值的原始响应，用于构造篡改或错误密钥场景。
     *
     * @param status 要返回的 HTTP 状态码
     * @param body 不作修改的 UTF-8 响应正文
     * @param signature 直接写入 {@code alipay-signature} 头的 Base64 签名
     */
    void enqueueRawSigned(int status, String body, String signature) {
        responses.add(new PlannedResponse(
                status,
                body.getBytes(StandardCharsets.UTF_8),
                signature,
                false
        ));
    }

    /**
     * 预设下一次请求返回同时含正确签名和额外伪造签名头的 200 响应。
     *
     * @param responseBody 响应 JSON 文本，入队前会去除首尾空白
     */
    void enqueueDuplicateSignature(String responseBody) {
        String body = responseBody.strip();
        String source = RESPONSE_TIMESTAMP + "\n" + RESPONSE_NONCE + "\n" + body + "\n";
        String signature = Base64.getEncoder().encodeToString(
                CryptoUtils.rsaSha256Sign(
                        alipayPrivateKey,
                        source.getBytes(StandardCharsets.UTF_8)
                )
        );
        responses.add(new PlannedResponse(
                200,
                body.getBytes(StandardCharsets.UTF_8),
                signature,
                true
        ));
    }

    /**
     * 在最多 5 秒内取出下一个已捕获请求，避免测试在客户端未发请求时无限阻塞。
     *
     * @return 最早到达且尚未取出的请求
     * @throws AssertionError 当 5 秒内未收到请求，或等待线程被中断时抛出
     */
    CapturedRequest takeRequest() {
        try {
            CapturedRequest result = requests.poll(5, TimeUnit.SECONDS);
            if (result == null) {
                throw new AssertionError("server did not receive a request");
            }
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    /**
     * 捕获入站请求，按先进先出顺序选取预设响应，并写入支付宝 V3 响应头。
     *
     * @param exchange JDK HTTP 服务端提供的当前请求交换
     * @throws IOException 当读取请求或写入响应失败时抛出
     */
    private void handle(HttpExchange exchange) throws IOException {
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        requests.add(new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().toString(),
                Map.copyOf(exchange.getRequestHeaders()),
                requestBody
        ));
        PlannedResponse response = responses.poll();
        if (response == null) {
            response = new PlannedResponse(
                    500,
                    "missing response".getBytes(StandardCharsets.UTF_8),
                    null,
                    false
            );
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        exchange.getResponseHeaders().set("alipay-trace-id", "trace-id-001");
        if (response.signature != null) {
            exchange.getResponseHeaders().set("alipay-timestamp", RESPONSE_TIMESTAMP);
            exchange.getResponseHeaders().set("alipay-nonce", RESPONSE_NONCE);
            exchange.getResponseHeaders().set("alipay-signature", response.signature);
            if (response.duplicateSignature) {
                exchange.getResponseHeaders().add("alipay-signature", "duplicate");
            }
        }
        exchange.sendResponseHeaders(response.status, response.body.length);
        exchange.getResponseBody().write(response.body);
        exchange.close();
    }

    /**
     * 立即停止本地 HTTP 服务端并关闭虚拟线程执行器；仅在测试清理阶段调用。
     */
    @Override
    public void close() {
        server.stop(0);
        executor.close();
    }

    /**
     * 完整保留模拟网关收到的 HTTP 请求，供测试校验路由、请求头、正文和签名。
     *
     * @param method HTTP 请求方法
     * @param target 原始请求目标，包含路径和查询字符串
     * @param headers 请求头的不可变快照，值列表可保留重复请求头
     * @param body 已全量读取的原始请求正文字节
     */
    record CapturedRequest(
            String method,
            String target,
            Map<String, List<String>> headers,
            byte[] body
    ) {
        /**
         * 解析请求目标中的 URL 编码查询参数。
         *
         * @return 已解码的参数映射；目标不含查询字符串时返回空映射
         */
        Map<String, String> queryParams() {
            int question = target.indexOf('?');
            return question < 0 ? Map.of() : parseForm(target.substring(question + 1));
        }

        /**
         * 将原始请求正文按 UTF-8 解码为文本。
         *
         * @return UTF-8 请求正文；无正文时返回空字符串
         */
        String bodyText() {
            return new String(body, StandardCharsets.UTF_8);
        }

        /**
         * 不区分大小写查找指定请求头，重复头仅返回第一个值。
         *
         * @param name 待查找的 HTTP 请求头名称
         * @return 第一个请求头值；请求头缺失或值列表为空时返回 {@code null}
         */
        @Nullable String header(String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name) && !entry.getValue().isEmpty()) {
                    return entry.getValue().getFirst();
                }
            }
            return null;
        }

        /**
         * 将 URL 编码的表单文本解析为有序映射，同名字段保留最后一个值。
         *
         * @param value 不含前导问号的查询或表单文本
         * @return 按字段出现顺序排列的已解码映射
         */
        private static Map<String, String> parseForm(String value) {
            Map<String, String> result = new LinkedHashMap<>();
            if (value.isEmpty()) {
                return result;
            }
            for (String item : value.split("&")) {
                String[] pair = item.split("=", 2);
                String name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String fieldValue = pair.length == 1 ? ""
                        : URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                result.put(name, fieldValue);
            }
            return result;
        }
    }

    /**
     * 描述模拟网关对下一次请求的响应计划，可构造签名缺失或重复头场景。
     *
     * @param status HTTP 响应状态码
     * @param body 要原样写入的响应正文字节
     * @param signature Base64 编码的 V3 响应签名；为 {@code null} 时不写入任何签名头
     * @param duplicateSignature 是否在正确签名后追加第二个伪造签名头
     */
    private record PlannedResponse(
            int status,
            byte[] body,
            @Nullable String signature,
            boolean duplicateSignature
    ) {
    }
}
