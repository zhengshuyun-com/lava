/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HttpResponse 单元测试
 * <p>
 * 测试 HttpResponse 的响应体读取资源管理功能
 *
 * @author Toint
 * @since 2026/2/5
 */
@DisplayName("HttpResponse 单元测试")
class HttpResponseTest {

    /**
     * 测试 URL
     */
    private static final String TEST_URL = "https://postman-echo.com/get";

    // 响应体读取测试

    @Nested
    @DisplayName("响应体读取测试")
    class ResponseBodyReadTest {

        @Test
        @DisplayName("getBodyAsBytes() - 正常读取")
        void testGetBodyAsBytes() {
            HttpRequest request = HttpRequest.get(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                byte[] bytes = response.getBodyAsBytes();
                assertNotNull(bytes, "响应体不应该为 null");
                assertTrue(bytes.length > 0, "响应体应该有内容");
            }
        }

        @Test
        @DisplayName("getBodyAsBytes() - 重复调用返回相同内容")
        void testGetBodyAsBytesMultipleTimes() {
            HttpRequest request = HttpRequest.get(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                byte[] bytes1 = response.getBodyAsBytes();
                byte[] bytes2 = response.getBodyAsBytes();
                byte[] bytes3 = response.getBodyAsBytes();

                assertNotNull(bytes1, "第一次读取不应该为 null");
                assertNotNull(bytes2, "第二次读取不应该为 null");
                assertNotNull(bytes3, "第三次读取不应该为 null");

                assertArrayEquals(bytes1, bytes2, "多次读取应该返回相同内容");
                assertArrayEquals(bytes1, bytes3, "多次读取应该返回相同内容");
            }
        }

        @Test
        @DisplayName("getBodyAsStream() - 正常读取")
        void testGetBodyAsStream() throws Exception {
            HttpRequest request = HttpRequest.get(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                InputStream stream = response.getBodyAsStream();
                assertNotNull(stream, "响应流不应该为 null");

                // 读取流内容
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[1024];
                int nRead;
                while ((nRead = stream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                byte[] content = buffer.toByteArray();
                assertTrue(content.length > 0, "响应流应该有内容");
            }
        }

        @Test
        @DisplayName("getBodyAsStream() - 重复调用抛出异常")
        void testGetBodyAsStreamTwiceThrowsException() {
            HttpRequest request = HttpRequest.get(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                // 首次调用成功
                InputStream stream1 = response.getBodyAsStream();
                assertNotNull(stream1, "首次调用应该成功");

                // 二次调用抛出异常
                IllegalStateException exception = assertThrows(
                        IllegalStateException.class,
                        response::getBodyAsStream,
                        "重复调用应该抛出 IllegalStateException"
                );

                assertTrue(exception.getMessage().contains("already been consumed"),
                        "异常消息应该包含 'already been consumed'");
            }
        }

        @Test
        @DisplayName("getBodyAsBytes() 后可以多次调用 getBodyAsStream()")
        void testGetBodyAsBytesEnablesMultipleStreamReads() {
            HttpRequest request = HttpRequest.get(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                // 先缓存
                byte[] bytes = response.getBodyAsBytes();
                assertNotNull(bytes, "缓存的字节数组不应该为 null");
                assertTrue(bytes.length > 0, "缓存的字节数组应该有内容");

                // 然后可以多次调用 getBodyAsStream
                InputStream stream1 = response.getBodyAsStream();
                InputStream stream2 = response.getBodyAsStream();
                InputStream stream3 = response.getBodyAsStream();

                assertNotNull(stream1, "第一次调用应该成功");
                assertNotNull(stream2, "第二次调用应该成功");
                assertNotNull(stream3, "第三次调用应该成功");
            }
        }

        @Test
        @DisplayName("getBodyAsStream() 后调用 getBodyAsBytes() 抛出异常")
        void testGetBodyAsBytesAfterStreamThrowsException() {
            HttpRequest request = HttpRequest.get(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                // 先调用 stream
                InputStream stream = response.getBodyAsStream();
                assertNotNull(stream, "首次调用应该成功");

                // 再调用 bytes 抛出异常
                IllegalStateException exception = assertThrows(
                        IllegalStateException.class,
                        response::getBodyAsBytes,
                        "应该抛出 IllegalStateException"
                );

                assertTrue(exception.getMessage().contains("already been consumed"),
                        "异常消息应该包含 'already been consumed'");
            }
        }

        @Test
        @DisplayName("getBodyAsString() - 正常读取")
        void testGetBodyAsString() {
            HttpRequest request = HttpRequest.get(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                String body = response.getBodyAsString();
                assertNotNull(body, "响应字符串不应该为 null");
                assertTrue(body.length() > 0, "响应字符串应该有内容");
                assertTrue(body.contains("\"url\""), "响应应该包含 URL 信息");
            }
        }

        @Test
        @DisplayName("getBodyAsString() - 重复调用返回相同内容")
        void testGetBodyAsStringMultipleTimes() {
            HttpRequest request = HttpRequest.get(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                String string1 = response.getBodyAsString();
                String string2 = response.getBodyAsString();
                String string3 = response.getBodyAsString();

                assertNotNull(string1, "第一次读取不应该为 null");
                assertNotNull(string2, "第二次读取不应该为 null");
                assertNotNull(string3, "第三次读取不应该为 null");

                assertEquals(string1, string2, "多次读取应该返回相同内容");
                assertEquals(string1, string3, "多次读取应该返回相同内容");
            }
        }

        @Test
        @DisplayName("getBodyAsString() 指定编码")
        void testGetBodyAsStringWithCharset() {
            HttpRequest request = HttpRequest.get(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                String utf8Body = response.getBodyAsString(StandardCharsets.UTF_8);
                String isoBody = response.getBodyAsString(StandardCharsets.ISO_8859_1);

                assertNotNull(utf8Body, "UTF-8 编码响应不应该为 null");
                assertNotNull(isoBody, "ISO-8859-1 编码响应不应该为 null");
            }
        }

        @Test
        @DisplayName("混合调用 - getBodyAsBytes(), getBodyAsString(), getBodyAsStream()")
        void testMixedCalls() {
            HttpRequest request = HttpRequest.get(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                // 先调用 getBodyAsBytes 缓存
                byte[] bytes = response.getBodyAsBytes();
                assertNotNull(bytes, "字节数组不应该为 null");

                // 然后可以调用其他方法
                String string = response.getBodyAsString();
                InputStream stream = response.getBodyAsStream();

                assertNotNull(string, "字符串不应该为 null");
                assertNotNull(stream, "流不应该为 null");

                // 验证内容一致
                assertEquals(new String(bytes, StandardCharsets.UTF_8), string,
                        "字节数组和字符串应该一致");
            }
        }
    }

    // 响应体为空测试

    @Nested
    @DisplayName("响应体为空测试")
    class EmptyResponseBodyTest {

        @Test
        @DisplayName("HEAD 请求 - 响应体为空")
        void testHeadRequestEmptyBody() {
            HttpRequest request = HttpRequest.head(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                byte[] bytes = response.getBodyAsBytes();
                assertNotNull(bytes, "响应体不应该为 null");
                assertEquals(0, bytes.length, "HEAD 请求响应体应该为空");
            }
        }

        @Test
        @DisplayName("HEAD 请求 - getBodyAsStream() 返回空流")
        void testHeadRequestEmptyStream() throws Exception {
            HttpRequest request = HttpRequest.head(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                InputStream stream = response.getBodyAsStream();
                assertNotNull(stream, "响应流不应该为 null");

                // 空流应该立即到达末尾
                assertEquals(-1, stream.read(), "空流应该立即返回 -1");
            }
        }
    }

    // 元数据测试

    @Nested
    @DisplayName("元数据测试")
    class MetadataTest {

        @Test
        @DisplayName("getMetadata() - 返回元数据")
        void testGetMetadata() {
            HttpRequest request = HttpRequest.get(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                HttpCallMetadata metadata = response.getMetadata();
                assertNotNull(metadata, "元数据不应该为 null");
                assertNotNull(metadata.getRequestId(), "请求 ID 不应该为 null");
                assertEquals("GET", metadata.getMethod(), "方法应该是 GET");
                assertEquals(TEST_URL, metadata.getUrl(), "URL 应该正确");
            }
        }

        @Test
        @DisplayName("getCode() - 返回状态码")
        void testGetCode() {
            HttpRequest request = HttpRequest.get(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                assertEquals(200, response.getCode(), "状态码应该是 200");
            }
        }

        @Test
        @DisplayName("getMessage() - 返回状态消息")
        void testGetMessage() {
            HttpRequest request = HttpRequest.get(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                String message = response.getMessage();
                assertNotNull(message, "状态消息不应该为 null");
            }
        }

        @Test
        @DisplayName("isSuccessful() - 返回是否成功")
        void testIsSuccessful() {
            HttpRequest request = HttpRequest.get(TEST_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                assertTrue(response.isSuccessful(), "请求应该成功");
            }
        }
    }
}
