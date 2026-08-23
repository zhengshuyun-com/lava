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

package com.zhengshuyun.lava.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HmacUtilsTest {

    /** RFC 4231 测试向量应产生标准的 HMAC-SHA-256 结果。 */
    @Test
    void computesRfc4231Sha256Vector() {
        byte[] key = HexFormat.of().parseHex("0b".repeat(20));
        byte[] data = "Hi There".getBytes(StandardCharsets.US_ASCII);
        byte[] expected = HexFormat.of().parseHex(
                "b0344c61d8db38535ca8afceaf0bf12b"
                        + "881dc200c9833da726e9376c2e32cff7");

        assertArrayEquals(expected, HmacUtils.sha256(key, data));
        assertEquals(HexFormat.of().formatHex(expected), HmacUtils.sha256Hex(key, data));
    }

    /** 字符串便利入口应固定使用 UTF-8，并保持密钥在前、数据在后的参数语义。 */
    @Test
    void computesUtf8StringAsLowercaseHex() {
        assertEquals(
                "5bdcc146bf60754e6a042426089575c7"
                        + "5a003f089d2739839dec58b964ec3843",
                HmacUtils.sha256Hex("Jefe", "what do ya want for nothing?"));
        assertEquals(
                "fb085ef4b8dd543fe5ab2573bd46c8e3"
                        + "e664ca70ecea9621c3ce304e8673f885",
                HmacUtils.sha256Hex("密钥", "数据🔐"));
    }

    /** HMAC 密钥必须存在且不能为空，数据则允许为空。 */
    @Test
    void validatesInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> HmacUtils.sha256(new byte[0], new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> HmacUtils.sha256(null, new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> HmacUtils.sha256(new byte[]{1}, null));

        assertEquals(64, HmacUtils.sha256Hex("key", "").length());
    }
}
