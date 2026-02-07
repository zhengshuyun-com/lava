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

package com.zhengshuyun.lava.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.zhengshuyun.lava.core.lang.Validate;

import java.io.File;
import java.io.InputStream;

/**
 * @author Toint
 * @since 2025/12/29
 */
public final class JsonUtil {

    private static volatile ObjectMapper objectMapper;

    private JsonUtil() {
    }

    /**
     * 初始化底层 ObjectMapper, 不调用本方法则使用默认实现
     * <p>
     * 注意：必须在首次直接或间接调用 {@link #getObjectMapper()} 之前调用, 否则将抛出 IllegalArgumentException
     *
     * @param newObjectMapper newObjectMapper
     */
    public static void initObjectMapper(ObjectMapper newObjectMapper) {
        synchronized (JsonUtil.class) {
            Validate.isNull(objectMapper, "JsonUtil is already initialized");
            Validate.notNull(newObjectMapper, "ObjectMapper must not be null");
            objectMapper = newObjectMapper;
        }
    }

    /**
     * @deprecated 使用 {@link #initObjectMapper(ObjectMapper)} 代替
     */
    @Deprecated(since = "1.0.0", forRemoval = true)
    public static void init(ObjectMapper newObjectMapper) {
        initObjectMapper(newObjectMapper);
    }

    private static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            synchronized (JsonUtil.class) {
                if (objectMapper == null) {
                    objectMapper = builder().build();
                }
            }
        }
        return objectMapper;
    }

    public static JsonBuilder builder() {
        return new JsonBuilder();
    }

    public static ObjectNode createObjectNode() {
        return getObjectMapper().createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return getObjectMapper().createArrayNode();
    }

    public static String writeValueAsPrettyString(Object value) {
        try {
            return getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static String writeValueAsString(Object value) {
        try {
            return getObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static byte[] writeValueAsBytes(Object value) {
        try {
            return getObjectMapper().writeValueAsBytes(value);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static JsonNode readTree(InputStream in) {
        try {
            return getObjectMapper().readTree(in);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static JsonNode readTree(String content) {
        try {
            return getObjectMapper().readTree(content);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static JsonNode readTree(byte[] content) {
        try {
            return getObjectMapper().readTree(content);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static JsonNode readTree(File file) {
        try {
            return getObjectMapper().readTree(file);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T readValue(File src, Class<T> valueType) {
        try {
            return getObjectMapper().readValue(src, valueType);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T readValue(File src, TypeReference<T> valueTypeRef) {
        try {
            return getObjectMapper().readValue(src, valueTypeRef);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T readValue(File src, JavaType valueType) {
        try {
            return getObjectMapper().readValue(src, valueType);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T readValue(String content, Class<T> valueType) {
        try {
            return getObjectMapper().readValue(content, valueType);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T readValue(String content, TypeReference<T> valueTypeRef) {
        try {
            return getObjectMapper().readValue(content, valueTypeRef);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T readValue(String content, JavaType valueType) {
        try {
            return getObjectMapper().readValue(content, valueType);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T readValue(InputStream src, Class<T> valueType) {
        try {
            return getObjectMapper().readValue(src, valueType);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T readValue(InputStream src, TypeReference<T> valueTypeRef) {
        try {
            return getObjectMapper().readValue(src, valueTypeRef);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T readValue(InputStream src, JavaType valueType) {
        try {
            return getObjectMapper().readValue(src, valueType);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T readValue(byte[] src, Class<T> valueType) {
        try {
            return getObjectMapper().readValue(src, valueType);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T readValue(byte[] src, TypeReference<T> valueTypeRef) {
        try {
            return getObjectMapper().readValue(src, valueTypeRef);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T readValue(byte[] src, JavaType valueType) {
        try {
            return getObjectMapper().readValue(src, valueType);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        try {
            return getObjectMapper().convertValue(fromValue, toValueType);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        try {
            return getObjectMapper().convertValue(fromValue, toValueTypeRef);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T> T convertValue(Object fromValue, JavaType toValueType) {
        try {
            return getObjectMapper().convertValue(fromValue, toValueType);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static <T extends JsonNode> T valueToTree(Object fromValue) {
        try {
            return getObjectMapper().valueToTree(fromValue);
        } catch (Exception e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static TypeFactory getTypeFactory() {
        return getObjectMapper().getTypeFactory();
    }
}
