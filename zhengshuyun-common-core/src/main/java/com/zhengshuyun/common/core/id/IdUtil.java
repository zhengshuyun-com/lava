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

import com.zhengshuyun.common.core.lang.Validate;

import java.util.UUID;

/**
 * @author Toint
 * @since 2026/1/6
 */
public final class IdUtil {

    private IdUtil() {
    }

    /**
     * 默认Seata雪花ID生成器
     */
    private static volatile SeataSnowflake seataSnowflake;

    /**
     * 初始化底层 SeataSnowflake, 不调用本方法则使用默认实现
     * <p>
     * 注意：必须在首次直接或间接调用 {@link #getSeataSnowflake()} 之前调用, 否则将抛出 IllegalArgumentException
     *
     * @param newSeataSnowflake newSeataSnowflake
     */
    public static void initSeataSnowflake(SeataSnowflake newSeataSnowflake) {
        synchronized (IdUtil.class) {
            Validate.notNull(newSeataSnowflake, "newSeataSnowflake must not be null");
            Validate.isNull(seataSnowflake, "seataSnowflake is already initialized");
            seataSnowflake = newSeataSnowflake;
        }
    }

    private static SeataSnowflake getSeataSnowflake() {
        if (seataSnowflake == null) {
            synchronized (IdUtil.class) {
                if (seataSnowflake == null) {
                    seataSnowflake = new SeataSnowflake();
                }
            }
        }
        return seataSnowflake;
    }

    /**
     * @see SeataSnowflake#nextId()
     */
    public static long nextSeataSnowflakeId() {
        return getSeataSnowflake().nextId();
    }

    /**
     * @see SeataSnowflake#nextId()
     */
    public static String nextSeataSnowflakeIdAsString() {
        return getSeataSnowflake().nextIdAsString();
    }

    /**
     * @return UUID. 示例: 6703d34b-c118-424b-816d-c27bca6f9b1a
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * @return 无横杠的UUID. 示例: e13053cbab634217bac7e6516b1b7030
     */
    public static String randomUUIDWithoutDash() {
        return randomUUID().replace("-", "");
    }
}
