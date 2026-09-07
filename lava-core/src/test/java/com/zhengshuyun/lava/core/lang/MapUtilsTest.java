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

package com.zhengshuyun.lava.core.lang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证映射判断的空值边界和条目判断语义。
 */
class MapUtilsTest {

    /** 验证 null 和不包含条目的 Map 均视为空映射。 */
    @Test
    void identifiesEmptyMaps() {
        assertTrue(MapUtils.isEmpty(null));
        assertTrue(MapUtils.isEmpty(Map.of()));
        assertFalse(MapUtils.isEmpty(Map.of("name", "lava")));
    }

    /** 验证只有包含至少一个条目的 Map 才视为非空。 */
    @Test
    void identifiesNonEmptyMaps() {
        assertFalse(MapUtils.isNotEmpty(null));
        assertFalse(MapUtils.isNotEmpty(Map.of()));
        assertTrue(MapUtils.isNotEmpty(Map.of("name", "lava")));
    }
}
