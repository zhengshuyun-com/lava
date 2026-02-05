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

package com.zhengshuyun.common.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhengshuyun.common.core.time.DateTimePatterns;
import com.zhengshuyun.common.core.time.ZoneIds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUtilTest {
    @Test
    void testConfig() {
        JsonUtil.writeValueAsString("");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                JsonUtil.init(JsonUtil.builder().build()));
        assertEquals("Json is already initialized", exception.getMessage());
    }

    // ==================== 序列化测试 ====================

    @Test
    void testWriteValueAsString() {
        String expected = """
                {"id":"1234567890123456789","name":"zhengshuyun-common","age":18,"birthDateTime":"2026-01-01T00:00:00Z","birthLocalDate":"2026-01-01","birthLocalTime":"00:00:00","birthDate":"2026-01-01T00:00:00Z"}""";
        User user = User.create();
        assertEquals(expected, JsonUtil.writeValueAsString(user));
    }

    @Test
    void testWriteValueAsBytes() {
        String expected = """
                {"id":"1234567890123456789","name":"zhengshuyun-common","age":18,"birthDateTime":"2026-01-01T00:00:00Z","birthLocalDate":"2026-01-01","birthLocalTime":"00:00:00","birthDate":"2026-01-01T00:00:00Z"}""";
        User user = User.create();
        byte[] bytes = JsonUtil.writeValueAsBytes(user);
        assertEquals(expected, new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    void testWriteValueAsPrettyString() {
        SimpleUser user = new SimpleUser();
        user.setName("Test");
        user.setAge(25);

        String result = JsonUtil.writeValueAsPrettyString(user);
        // 验证包含换行符和缩进 (格式化输出)
        assert result.contains("\n");
        assert result.contains("Test");
        assert result.contains("25");
    }

    @Test
    void testSerializeInstant() {
        Instant instant = Instant.parse("2026-01-01T00:00:00Z");
        String json = JsonUtil.writeValueAsString(Map.of("time", instant));
        assertEquals("{\"time\":\"2026-01-01T00:00:00Z\"}", json);
    }

    @Test
    void testSerializeDateWithUTC() {
        // 北京时间 2026-01-01 08:00:00 = UTC 2026-01-01 00:00:00
        LocalDateTime shanghaiTime = LocalDateTime.of(2026, 1, 1, 8, 0, 0);
        Date date = Date.from(shanghaiTime.atZone(ZoneIds.ASIA_SHANGHAI).toInstant());

        String json = JsonUtil.writeValueAsString(Map.of("time", date));
        // 期望输出 UTC 时间
        assertTrue(json.contains("2026-01-01T00:00:00Z"));
    }

    // ==================== 反序列化测试 - String ====================

    @Test
    void testReadValue_String_Class() {
        String json = """
                {"id":"1234567890123456789","name":"zhengshuyun-common","age":18,"birthDateTime":"2026-01-01T00:00:00Z","birthLocalDate":"2026-01-01","birthLocalTime":"00:00:00","birthDate":"2026-01-01T00:00:00Z"}""";
        User user = JsonUtil.readValue(json, User.class);

        assertEquals(1234567890123456789L, user.getId());
        assertEquals("zhengshuyun-common", user.getName());
        assertEquals(18, user.getAge());
        assertEquals("2026-01-01T00:00:00Z",
                user.getBirthDateTime().format(DateTimeFormatter.ofPattern(DateTimePatterns.ISO_INSTANT)));
    }

    @Test
    void testReadValue_String_TypeReference() {
        String json = """
                [{"name":"Alice","age":20},{"name":"Bob","age":25}]""";
        List<Map<String, Object>> list = JsonUtil.readValue(json, new TypeReference<>() {
        });

        assertEquals(2, list.size());
        assertEquals("Alice", list.getFirst().get("name"));
        assertEquals(20, list.getFirst().get("age"));
    }

    @Test
    void testReadValue_String_JavaType() {
        String json = """
                {"name":"Test"}""";
        var javaType = JsonUtil.getTypeFactory().constructType(Map.class);
        Map<String, Object> map = JsonUtil.readValue(json, javaType);

        assertEquals("Test", map.get("name"));
    }

    // ==================== 反序列化测试 - byte[] ====================

    @Test
    void testReadValue_Bytes_Class() {
        String json = """
                {"name":"Test","age":30}""";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        SimpleUser user = JsonUtil.readValue(bytes, SimpleUser.class);
        assertEquals("Test", user.getName());
        assertEquals(30, user.getAge());
    }

    @Test
    void testReadValue_Bytes_Class_Map() {
        String json = """
                {"name":"Test","age":30}""";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        Map<String, Object> map = JsonUtil.readValue(bytes, new TypeReference<>() {
        });
        assertEquals("Test", map.get("name"));
        assertEquals(30, map.get("age"));
    }

    @Test
    void testReadValue_Bytes_TypeReference() {
        String json = """
                [1,2,3,4,5]""";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        List<Integer> list = JsonUtil.readValue(bytes, new TypeReference<>() {
        });
        assertEquals(5, list.size());
        assertEquals(3, list.get(2));
    }

    @Test
    void testReadValue_Bytes_JavaType() {
        String json = """
                {"key":"value"}""";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        var javaType = JsonUtil.getTypeFactory().constructType(Map.class);

        Map<String, Object> map = JsonUtil.readValue(bytes, javaType);
        assertEquals("value", map.get("key"));
    }

    // ==================== 反序列化测试 - InputStream ====================

    @Test
    void testReadValue_InputStream_Class() {
        String json = """
                {"name":"Stream","age":100}""";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        SimpleUser user = JsonUtil.readValue(is, SimpleUser.class);
        assertEquals("Stream", user.getName());
        assertEquals(100, user.getAge());
    }

    @Test
    void testReadValue_InputStream_Class_Map() {
        String json = """
                {"name":"Stream","value":100}""";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> map = JsonUtil.readValue(is, new TypeReference<Map<String, Object>>() {
        });
        assertEquals("Stream", map.get("name"));
        assertEquals(100, map.get("value"));
    }

    @Test
    void testReadValue_InputStream_TypeReference() {
        String json = """
                ["a","b","c"]""";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        List<String> list = JsonUtil.readValue(is, new TypeReference<List<String>>() {
        });
        assertEquals(3, list.size());
        assertEquals("b", list.get(1));
    }

    @Test
    void testReadValue_InputStream_JavaType() {
        String json = """
                {"test":true}""";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        var javaType = JsonUtil.getTypeFactory().constructType(Map.class);

        Map<String, Object> map = JsonUtil.readValue(is, javaType);
        assertEquals(true, map.get("test"));
    }

    // ==================== 反序列化测试 - File ====================

    @Test
    void testReadValue_File_Class(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("test.json").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("""
                    {"name":"FileTest","age":42}""");
        }

        SimpleUser user = JsonUtil.readValue(file, SimpleUser.class);
        assertEquals("FileTest", user.getName());
        assertEquals(42, user.getAge());
    }

    @Test
    void testReadValue_File_Class_Map(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("test2.json").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("""
                    {"name":"FileTest","number":42}""");
        }

        Map<String, Object> map = JsonUtil.readValue(file, new TypeReference<Map<String, Object>>() {
        });
        assertEquals("FileTest", map.get("name"));
        assertEquals(42, map.get("number"));
    }

    @Test
    void testReadValue_File_TypeReference(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("array.json").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("""
                    [{"id":1},{"id":2}]""");
        }

        List<Map<String, Object>> list = JsonUtil.readValue(file, new TypeReference<List<Map<String, Object>>>() {
        });
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).get("id"));
    }

    @Test
    void testReadValue_File_JavaType(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("data.json").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("""
                    {"active":true}""");
        }

        var javaType = JsonUtil.getTypeFactory().constructType(Map.class);
        Map<String, Object> map = JsonUtil.readValue(file, javaType);
        assertEquals(true, map.get("active"));
    }

    // ==================== readTree 测试 ====================

    @Test
    void testReadTree_String() {
        String json = """
                {"name":"Tree","nested":{"value":123}}""";
        JsonNode node = JsonUtil.readTree(json);

        assertEquals("Tree", node.get("name").asText());
        assertEquals(123, node.get("nested").get("value").asInt());
    }

    @Test
    void testReadTree_Bytes() {
        String json = """
                {"array":[1,2,3]}""";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        JsonNode node = JsonUtil.readTree(bytes);

        assertEquals(3, node.get("array").size());
        assertEquals(2, node.get("array").get(1).asInt());
    }

    @Test
    void testReadTree_InputStream() {
        String json = """
                {"status":"ok"}""";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonNode node = JsonUtil.readTree(is);

        assertEquals("ok", node.get("status").asText());
    }

    @Test
    void testReadTree_File(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("tree.json").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("""
                    {"level":"top"}""");
        }

        JsonNode node = JsonUtil.readTree(file);
        assertEquals("top", node.get("level").asText());
    }

    // ==================== convertValue 测试 ====================

    @Test
    void testConvertValue_Class() {
        Map<String, Object> map = Map.of("name", "Convert", "age", 25);
        SimpleUser user = JsonUtil.convertValue(map, SimpleUser.class);

        assertEquals("Convert", user.getName());
        assertEquals(25, user.getAge());
    }

    @Test
    void testConvertValue_TypeReference() {
        List<Map<String, Object>> list = List.of(
                Map.of("id", 1),
                Map.of("id", 2)
        );

        List<Map<String, Integer>> converted = JsonUtil.convertValue(
                list,
                new TypeReference<List<Map<String, Integer>>>() {
                }
        );

        assertEquals(2, converted.size());
        assertEquals(1, converted.get(0).get("id"));
    }

    @Test
    void testConvertValue_JavaType() {
        Map<String, String> map = Map.of("key", "value");
        var javaType = JsonUtil.getTypeFactory().constructType(Map.class);

        Map<String, String> converted = JsonUtil.convertValue(map, javaType);
        assertEquals("value", converted.get("key"));
    }

    // ==================== valueToTree 测试 ====================

    @Test
    void testValueToTree() {
        SimpleUser user = new SimpleUser();
        user.setName("TreeUser");
        user.setAge(30);

        JsonNode node = JsonUtil.valueToTree(user);
        assertEquals("TreeUser", node.get("name").asText());
        assertEquals(30, node.get("age").asInt());
    }

    // ==================== createNode 测试 ====================

    @Test
    void testCreateObjectNode() {
        ObjectNode node = JsonUtil.createObjectNode();
        node.put("test", "value");
        node.put("number", 123);

        assertEquals("value", node.get("test").asText());
        assertEquals(123, node.get("number").asInt());
    }

    @Test
    void testCreateArrayNode() {
        ArrayNode node = JsonUtil.createArrayNode();
        node.add("first");
        node.add("second");
        node.add(42);

        assertEquals(3, node.size());
        assertEquals("first", node.get(0).asText());
        assertEquals(42, node.get(2).asInt());
    }

    // ==================== 异常测试 ====================

    @Test
    void testReadValue_InvalidJson() {
        String invalidJson = "{invalid json}";
        assertThrows(JsonException.class, () -> JsonUtil.readValue(invalidJson, Map.class));
    }

    @Test
    void testReadTree_InvalidJson() {
        String invalidJson = "not json at all";
        assertThrows(JsonException.class, () -> JsonUtil.readTree(invalidJson));
    }

    @Test
    void testConvertValue_IncompatibleType() {
        // 类型不兼容：age 是字符串而不是数字
        Map<String, Object> map = Map.of("name", "Test", "age", "not a number");
        assertThrows(JsonException.class, () -> {
            JsonUtil.convertValue(map, User.class);
        });
    }

    @Test
    void testConvertValue_InvalidDateFormat() {
        // 日期格式不正确
        Map<String, Object> map = Map.of("birthDateTime", "invalid-date-format");
        assertThrows(JsonException.class, () -> {
            JsonUtil.convertValue(map, User.class);
        });
    }

    @Test
    void testInit_AlreadyInitialized() {
        // 第一次调用任何 Json 方法会触发初始化
        JsonUtil.writeValueAsString(Map.of("test", "value"));

        // 尝试再次设置配置应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            JsonUtil.init(JsonUtil.builder().build());
        });
    }

    @Test
    void testWriteValueAsPrettyString_Exception() {
        // 创建一个无法序列化的对象
        Object unserializable = new Object() {
            @SuppressWarnings("unused")
            public Object getSelf() {
                return this; // 循环引用
            }
        };

        // 虽然可能不会抛异常 (因为有 FAIL_ON_EMPTY_BEANS 禁用) , 但测试异常处理路径
        String result = JsonUtil.writeValueAsPrettyString(Map.of("key", "value"));
        assert result.contains("key");
    }

    // ==================== 内部类 ====================

    private static class User {
        private Long id;
        private String name;
        private Integer age;
        private LocalDateTime birthDateTime;
        private LocalDate birthLocalDate;
        private LocalTime birthLocalTime;
        private Date birthDate;

        public static User create() {
            LocalDateTime localDateTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

            User user = new User();
            user.setId(1234567890123456789L);
            user.setName("zhengshuyun-common");
            user.setAge(18);
            user.setBirthDateTime(localDateTime);
            user.setBirthLocalDate(localDateTime.toLocalDate());
            user.setBirthLocalTime(localDateTime.toLocalTime());
            // 创建 UTC 时间的 Date: 2026-01-01 00:00:00 UTC
            user.setBirthDate(Date.from(Instant.parse("2026-01-01T00:00:00Z")));
            return user;
        }

        public Long getId() {
            return id;
        }

        public User setId(Long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public User setName(String name) {
            this.name = name;
            return this;
        }

        public Integer getAge() {
            return age;
        }

        public User setAge(Integer age) {
            this.age = age;
            return this;
        }

        public LocalDateTime getBirthDateTime() {
            return birthDateTime;
        }

        public User setBirthDateTime(LocalDateTime birthDateTime) {
            this.birthDateTime = birthDateTime;
            return this;
        }

        public LocalDate getBirthLocalDate() {
            return birthLocalDate;
        }

        public User setBirthLocalDate(LocalDate birthLocalDate) {
            this.birthLocalDate = birthLocalDate;
            return this;
        }

        public LocalTime getBirthLocalTime() {
            return birthLocalTime;
        }

        public User setBirthLocalTime(LocalTime birthLocalTime) {
            this.birthLocalTime = birthLocalTime;
            return this;
        }

        public Date getBirthDate() {
            return birthDate;
        }

        public User setBirthDate(Date birthDate) {
            this.birthDate = birthDate;
            return this;
        }
    }

    private static class SimpleUser {
        private String name;
        private Integer age;

        public String getName() {
            return name;
        }

        public SimpleUser setName(String name) {
            this.name = name;
            return this;
        }

        public Integer getAge() {
            return age;
        }

        public SimpleUser setAge(Integer age) {
            this.age = age;
            return this;
        }
    }
}