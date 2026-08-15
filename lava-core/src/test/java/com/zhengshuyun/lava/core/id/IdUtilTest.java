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

package com.zhengshuyun.lava.core.id;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /**
     * 无横杠 UUID 必须是 32 位小写十六进制
     */
    @Test
    void testRandomUUIDWithoutDashFormat() {
        for (int i = 0; i < 1000; i++) {
            String id = IdUtil.randomUUIDWithoutDash();
            assertTrue(id.matches("[0-9a-f]{32}"), () -> "unexpected uuid format: " + id);
        }
    }

    /**
     * 十六进制编码必须与 UUID.toString() 去横杠的结果完全一致, 覆盖随机值与位模式边界
     */
    @Test
    void testToHexWithoutDashMatchesUuidToString() {
        List<UUID> cases = new ArrayList<>(List.of(
                new UUID(0L, 0L),
                new UUID(-1L, -1L),
                new UUID(Long.MIN_VALUE, Long.MAX_VALUE),
                UUID.fromString("6703d34b-c118-424b-816d-c27bca6f9b1a"),
                UUID.fromString("e13053cb-ab63-4217-bac7-e6516b1b7030")
        ));
        for (int i = 0; i < 1000; i++) {
            cases.add(UUID.randomUUID());
        }

        for (UUID uuid : cases) {
            assertEquals(uuid.toString().replace("-", ""), IdUtil.toHexWithoutDash(uuid),
                    () -> "hex encoding mismatch for " + uuid);
        }
    }
}
