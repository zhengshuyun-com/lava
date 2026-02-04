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

package com.zhengshuyun.common.core.id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Toint
 * @since 2026/1/6
 */
public class IdUtilTest {
    @Test
    void testNextGetSeataSnowflakeId() {
        long id1 = IdUtil.nextSeataSnowflakeId();
        String id2 = IdUtil.nextSeataSnowflakeIdAsString();
        assertEquals(1L, Long.parseLong(id2) - id1);
    }

    @Test
    void testRandomUUID() {
        assertEquals(36, IdUtil.randomUUID().length());
        assertEquals(32, IdUtil.randomUUIDWithoutDash().length());
    }
}
