/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.json.JsonCodec;
import com.zhengshuyun.lava.json.JsonException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipayApiException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipayProtocolException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipaySecurityException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipaySecurityFailure;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从支付宝原始 JSON 中提取签名节点、验签并解析业务响应。
 */
public final class AlipayResponseParser {
    private static final String ERROR_RESPONSE = "error_response";

    private final PublicKey alipayPublicKey;
    private final JsonCodec jsonCodec;

    /**
     * 创建响应解析器。
     *
     * @param alipayPublicKey 支付宝公钥
     * @param jsonCodec       JSON 编解码器
     */
    public AlipayResponseParser(PublicKey alipayPublicKey, JsonCodec jsonCodec) {
        this.alipayPublicKey = alipayPublicKey;
        this.jsonCodec = jsonCodec;
    }

    /**
     * 从原始响应中提取并验证指定接口的业务对象。
     *
     * @param method       接口名称
     * @param body         未修改的 UTF-8 响应正文
     * @param responseType 业务响应类型
     * @param traceId      可选链路标识
     * @param <T>          业务响应类型
     * @return 已验签业务响应
     */
    public <T> T parse(
            String method,
            byte[] body,
            Class<T> responseType,
            @Nullable String traceId
    ) {
        if (body.length == 0) {
            throw new AlipayProtocolException("支付宝响应缺少正文");
        }
        String json = new String(body, StandardCharsets.UTF_8);

        // 1. 先由 JSON 解析器验证整体语法，再使用轻量扫描保留业务节点的原始字节表示。
        try {
            jsonCodec.read(json, Object.class);
        } catch (JsonException exception) {
            throw new AlipayProtocolException("支付宝响应不是有效 JSON");
        }
        Map<String, String> values = topLevelValues(json);
        String responseName = method.replace('.', '_') + "_response";
        String responseSource = values.get(responseName);
        String errorSource = values.get(ERROR_RESPONSE);
        if ((responseSource == null) == (errorSource == null)) {
            throw new AlipayProtocolException("支付宝响应缺少唯一业务节点");
        }
        String source = responseSource == null ? errorSource : responseSource;

        // 2. 签名必须验证原始业务节点，禁止反序列化后重新编码构造验签内容。
        String signatureSource = values.get("sign");
        if (signatureSource == null) {
            throw new AlipaySecurityException(AlipaySecurityFailure.MISSING_SIGNATURE);
        }
        String signature;
        try {
            signature = jsonCodec.read(signatureSource, String.class);
        } catch (JsonException exception) {
            throw new AlipaySecurityException(AlipaySecurityFailure.MISSING_SIGNATURE);
        }
        boolean valid = AlipayCryptoUtils.verify(source, signature, alipayPublicKey);
        if (!valid && source.contains("\\/")) {
            // 与支付宝官方 Java SDK 保持兼容：部分网关会对斜杠转义形式做等价签名。
            valid = AlipayCryptoUtils.verify(
                    source.replace("\\/", "/"), signature, alipayPublicKey);
        }
        if (!valid) {
            throw new AlipaySecurityException(AlipaySecurityFailure.INVALID_SIGNATURE);
        }

        // 3. 验签通过后才解析错误码和业务数据，未验证的内容不能进入业务判断。
        GatewayStatus status;
        try {
            status = jsonCodec.read(source, GatewayStatus.class);
        } catch (JsonException exception) {
            throw new AlipayProtocolException("支付宝业务响应结构无效");
        }
        String code = AlipayValidationUtils.requireResponseText(status.code, "code");
        String message = AlipayValidationUtils.requireResponseText(status.message, "msg");
        if (!"10000".equals(code)) {
            throw new AlipayApiException(
                    code,
                    message,
                    status.subCode,
                    status.subMessage,
                    traceId
            );
        }
        if (responseSource == null) {
            throw new AlipayProtocolException("支付宝成功码位于错误响应节点");
        }
        try {
            return jsonCodec.read(source, responseType);
        } catch (JsonException exception) {
            throw new AlipayProtocolException("支付宝业务响应不是预期结构");
        }
    }

    private Map<String, String> topLevelValues(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        int index = skipWhitespace(json, 0);
        if (index >= json.length() || json.charAt(index) != '{') {
            throw new AlipayProtocolException("支付宝响应根节点必须是 JSON 对象");
        }
        index = skipWhitespace(json, index + 1);
        if (index < json.length() && json.charAt(index) == '}') {
            return result;
        }

        while (index < json.length()) {
            if (json.charAt(index) != '"') {
                throw new AlipayProtocolException("支付宝响应属性名无效");
            }
            int nameEnd = stringEnd(json, index);
            String name;
            try {
                name = jsonCodec.read(json.substring(index, nameEnd), String.class);
            } catch (JsonException exception) {
                throw new AlipayProtocolException("支付宝响应属性名无效");
            }
            index = skipWhitespace(json, nameEnd);
            if (index >= json.length() || json.charAt(index) != ':') {
                throw new AlipayProtocolException("支付宝响应属性缺少分隔符");
            }
            int valueStart = skipWhitespace(json, index + 1);
            int valueEnd = valueEnd(json, valueStart);
            if (result.putIfAbsent(name, json.substring(valueStart, valueEnd)) != null) {
                throw new AlipayProtocolException("支付宝响应包含重复顶层属性");
            }

            index = skipWhitespace(json, valueEnd);
            if (index >= json.length()) {
                throw new AlipayProtocolException("支付宝响应 JSON 未闭合");
            }
            char separator = json.charAt(index++);
            if (separator == '}') {
                if (skipWhitespace(json, index) != json.length()) {
                    throw new AlipayProtocolException("支付宝响应 JSON 包含尾随内容");
                }
                return result;
            }
            if (separator != ',') {
                throw new AlipayProtocolException("支付宝响应属性分隔符无效");
            }
            index = skipWhitespace(json, index);
        }
        throw new AlipayProtocolException("支付宝响应 JSON 未闭合");
    }

    private static int valueEnd(String json, int start) {
        if (start >= json.length()) {
            throw new AlipayProtocolException("支付宝响应属性缺少值");
        }
        char first = json.charAt(start);
        if (first == '"') {
            return stringEnd(json, start);
        }
        if (first != '{' && first != '[') {
            int index = start;
            while (index < json.length() && json.charAt(index) != ','
                    && json.charAt(index) != '}') {
                index++;
            }
            return index;
        }

        char[] stack = new char[json.length() - start];
        int depth = 0;
        boolean inString = false;
        int escapes = 0;
        for (int index = start; index < json.length(); index++) {
            char current = json.charAt(index);
            if (current == '"' && escapes % 2 == 0) {
                inString = !inString;
            } else if (!inString && (current == '{' || current == '[')) {
                stack[depth++] = current;
            } else if (!inString && (current == '}' || current == ']')) {
                if (depth == 0 || current == '}' && stack[depth - 1] != '{'
                        || current == ']' && stack[depth - 1] != '[') {
                    throw new AlipayProtocolException("支付宝响应 JSON 嵌套结构无效");
                }
                if (--depth == 0) {
                    return index + 1;
                }
            }
            escapes = current == '\\' ? escapes + 1 : 0;
        }
        throw new AlipayProtocolException("支付宝响应 JSON 嵌套结构未闭合");
    }

    private static int stringEnd(String json, int start) {
        int escapes = 0;
        for (int index = start + 1; index < json.length(); index++) {
            char current = json.charAt(index);
            if (current == '"' && escapes % 2 == 0) {
                return index + 1;
            }
            escapes = current == '\\' ? escapes + 1 : 0;
        }
        throw new AlipayProtocolException("支付宝响应 JSON 字符串未闭合");
    }

    private static int skipWhitespace(String text, int start) {
        int index = start;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GatewayStatus(
            @JsonProperty("code") @Nullable String code,
            @JsonProperty("msg") @Nullable String message,
            @JsonProperty("sub_code") @Nullable String subCode,
            @JsonProperty("sub_msg") @Nullable String subMessage
    ) {
    }
}
