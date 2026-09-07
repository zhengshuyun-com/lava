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

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证集合判断的空值边界和元素判断语义。
 */
class CollectionUtilsTest {

    /** 验证 null、空 List 和空 Set 均视为空集合。 */
    @Test
    void identifiesEmptyCollections() {
        assertTrue(CollectionUtils.isEmpty(null));
        assertTrue(CollectionUtils.isEmpty(List.of()));
        assertTrue(CollectionUtils.isEmpty(Set.of()));
        assertFalse(CollectionUtils.isEmpty(List.of("lava")));
        assertFalse(CollectionUtils.isEmpty(Set.of("lava")));
    }

    /** 验证只有包含至少一个元素的集合才视为非空。 */
    @Test
    void identifiesNonEmptyCollections() {
        assertFalse(CollectionUtils.isNotEmpty(null));
        assertFalse(CollectionUtils.isNotEmpty(List.of()));
        assertFalse(CollectionUtils.isNotEmpty(Set.of()));
        assertTrue(CollectionUtils.isNotEmpty(List.of("lava")));
        assertTrue(CollectionUtils.isNotEmpty(Set.of("lava")));
    }
}
