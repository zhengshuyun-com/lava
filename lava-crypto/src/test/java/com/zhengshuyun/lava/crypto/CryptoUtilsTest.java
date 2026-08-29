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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoUtilsTest {

    /** 统一门面必须公开；仅承载公开曲线类型的 EC 工具例外。 */
    @Test
    void exposesOnlyTheFacadeForStatelessCryptography() {
        assertTrue(Modifier.isPublic(CryptoUtils.class.getModifiers()));
        assertFalse(Modifier.isPublic(HmacUtils.class.getModifiers()));
        assertFalse(Modifier.isPublic(RsaSignatureUtils.class.getModifiers()));
        assertFalse(Modifier.isPublic(AesGcmUtils.class.getModifiers()));
        assertTrue(Modifier.isPublic(EcKeyUtils.class.getModifiers()));
        assertFalse(Modifier.isPublic(PemKeyUtils.class.getModifiers()));
    }
}
