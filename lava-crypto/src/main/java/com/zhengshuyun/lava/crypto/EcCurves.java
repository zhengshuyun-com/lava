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

package com.zhengshuyun.lava.crypto;

/**
 * EC 曲线常量
 *
 * @author Toint
 * @since 2026/2/7
 */
public final class EcCurves {

    private EcCurves() {
    }

    /**
     * P-256 (ES256)
     */
    public static final String SECP256R1 = "secp256r1";

    /**
     * P-384 (ES384)
     */
    public static final String SECP384R1 = "secp384r1";

    /**
     * P-521 (ES512)
     */
    public static final String SECP521R1 = "secp521r1";
}
