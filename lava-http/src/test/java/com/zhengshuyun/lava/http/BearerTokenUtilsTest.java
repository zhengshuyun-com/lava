/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Bearer Token 提取行为测试。
 */
class BearerTokenUtilsTest {

    /**
     * 认证方案名称不区分大小写，且接受完整的 b64token 字符集。
     */
    @Test
    void extractsValidBearerTokens() {
        assertEquals("sk-a8H2_kLm9-Test", BearerTokenUtils.extract("Bearer sk-a8H2_kLm9-Test"));
        assertEquals("abc.DEF~ghi+/=", BearerTokenUtils.extract("bearer abc.DEF~ghi+/="));
    }

    /**
     * 缺失、非 Bearer 认证和不符合严格格式的值均不产生 Token。
     */
    @Test
    void rejectsMissingOrMalformedBearerTokens() {
        assertNull(BearerTokenUtils.extract(null));
        assertNull(BearerTokenUtils.extract(""));
        assertNull(BearerTokenUtils.extract("Basic abc"));
        assertNull(BearerTokenUtils.extract("Bearer"));
        assertNull(BearerTokenUtils.extract("Bearer "));
        assertNull(BearerTokenUtils.extract("BearerX abc"));
        assertNull(BearerTokenUtils.extract("Bearer   abc"));
        assertNull(BearerTokenUtils.extract("Bearer\tabc"));
        assertNull(BearerTokenUtils.extract("Bearer abc def"));
        assertNull(BearerTokenUtils.extract("Bearer abc "));
    }
}
