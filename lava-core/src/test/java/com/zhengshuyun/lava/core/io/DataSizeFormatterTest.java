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
package com.zhengshuyun.lava.core.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataSizeFormatterTest {

    @Test
    void distinguishesIecAndSiUnits() {
        assertEquals("1 KiB", DataSizeFormatter.formatIec(1024));
        assertEquals("1.02 kB", DataSizeFormatter.formatSi(1024));
        assertEquals("1 MiB", DataSizeFormatter.formatIec(1024 * 1024));
        assertEquals("1 MB", DataSizeFormatter.formatSi(1_000_000));
        assertEquals("0 B", DataSizeFormatter.formatIec(0));
    }

    @Test
    void rejectsNegativeSizesAndSupportsLongRange() {
        assertThrows(IllegalArgumentException.class, () -> DataSizeFormatter.formatIec(-1));
        assertEquals("8 EiB", DataSizeFormatter.formatIec(Long.MAX_VALUE));
        assertEquals("9.22 EB", DataSizeFormatter.formatSi(Long.MAX_VALUE));
    }
}
