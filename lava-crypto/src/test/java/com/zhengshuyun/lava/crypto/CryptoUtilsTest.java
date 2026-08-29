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

import java.lang.reflect.Modifier;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoUtilsTest {

    /** 统一门面与按算法划分的专用工具都属于稳定公开 API。 */
    @Test
    void exposesFacadeAndSpecializedStatelessUtilities() throws Exception {
        assertTrue(Modifier.isPublic(CryptoUtils.class.getModifiers()));
        assertTrue(Modifier.isPublic(HmacUtils.class.getModifiers()));
        assertTrue(Modifier.isPublic(RsaSignatureUtils.class.getModifiers()));
        assertTrue(Modifier.isPublic(AesGcmUtils.class.getModifiers()));
        assertTrue(Modifier.isPublic(EcKeyUtils.class.getModifiers()));
        assertTrue(Modifier.isPublic(PemKeyUtils.class.getModifiers()));

        assertTrue(Modifier.isPublic(HmacUtils.class
                .getMethod("sha256", byte[].class, byte[].class).getModifiers()));
        assertTrue(Modifier.isPublic(AesGcmUtils.class
                .getMethod("encrypt", byte[].class, byte[].class, byte[].class, byte[].class)
                .getModifiers()));
        assertTrue(Modifier.isPublic(RsaSignatureUtils.class
                .getMethod("sha256", PrivateKey.class, byte[].class).getModifiers()));
        assertTrue(Modifier.isPublic(RsaSignatureUtils.class
                .getMethod("verifySha256", PublicKey.class, byte[].class, byte[].class)
                .getModifiers()));
        assertTrue(Modifier.isPublic(EcKeyUtils.class
                .getMethod("generate", EcKeyUtils.Curve.class, SecureRandom.class)
                .getModifiers()));
        assertTrue(Modifier.isPublic(PemKeyUtils.class
                .getMethod("toPem", Key.class).getModifiers()));
    }
}
