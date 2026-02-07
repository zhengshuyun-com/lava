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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Argon2id 跨系统兼容性测试.
 * <p>
 * 用于验证外部 PHC 串与本模块实现的一致性, 并生成样例哈希供三方系统交叉校验.
 */
class Argon2CompatibilityTest {

    /**
     * 测试日志记录器.
     */
    private static final Logger log = LoggerFactory.getLogger(Argon2CompatibilityTest.class);

    /**
     * 外部系统生成的 Argon2id PHC 串, 用于兼容性回归验证.
     */
    private static final String EXTERNAL_ARGON2ID_HASH_1 =
            "$argon2id$v=19$m=65536,t=3,p=4$G+p7YJzjVw/NEAFaqKJnLg$3CO8znSvBuO/8j4AvAcTiKXw0tUIOZskYtdKvZceaUQ";

    /**
     * 外部系统生成的 Argon2id PHC 串, 用于兼容性回归验证.
     */
    private static final String EXTERNAL_ARGON2ID_HASH_2 =
            "$argon2id$v=19$m=65536,t=3,p=4$aBrV0JGiAEDzsk4YBYGTrQ$x051lwfhIqD1t5TRMXUAqHLIwTkTxNd3CVez2qZy/wA";

    /**
     * 与上面 PHC 串对应的明文密码.
     */
    private static final String PASSWORD = "zhengshuyun";

    @DisplayName("兼容性验证 - 外部 Argon2id 哈希可通过")
    @Test
    void testVerifyExternalHash() {
        PasswordHasher hasher = CryptoUtil.passwordHasher().build();

        // 目标: 与其他系统生成的 Argon2id 结果保持一致.
        assertTrue(hasher.verify(PASSWORD, EXTERNAL_ARGON2ID_HASH_1));
        assertTrue(hasher.verify(PASSWORD, EXTERNAL_ARGON2ID_HASH_2));
    }

    @DisplayName("兼容性验证 - 错误密码应校验失败")
    @Test
    void testVerifyExternalHashWrongPassword() {
        // verify 使用的是 PHC 串内参数, Builder 配置不会影响校验结果.
        PasswordHasher hasher = CryptoUtil.passwordHasher().build();

        assertFalse(hasher.verify("123", EXTERNAL_ARGON2ID_HASH_1));
    }

    @DisplayName("兼容性验证 - 生成默认与自定义样例哈希")
    @Test
    void testGenerateHashForThirdPartyCheck() {
        String defaultHash = CryptoUtil.passwordHasher()
                .build()
                .hash(PASSWORD);
        log.info("generated default Argon2id hash: {}", defaultHash);
        assertTrue(defaultHash.startsWith("$argon2id$v=19$m=65536,t=3,p=1$"));

        String customHash = CryptoUtil.passwordHasher()
                .setMemoryKiB(65536)
                .setIterations(3)
                .setParallelism(4)
                .setSaltLengthBytes(16)
                .setHashLengthBytes(32)
                .build()
                .hash(PASSWORD);
        log.info("generated custom Argon2id hash: {}", customHash);
        assertTrue(customHash.startsWith("$argon2id$v=19$m=65536,t=3,p=4$"));
    }
}
