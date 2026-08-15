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

import com.zhengshuyun.lava.core.lang.Validate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author Toint
 * @since 2026/1/6
 */
public final class IdUtil {

    /**
     * 十六进制字符表
     */
    private static final String HEX_DIGITS = "0123456789abcdef";

    /**
     * 默认Seata雪花ID生成器
     */
    private static volatile SeataSnowflake seataSnowflake;

    private IdUtil() {
    }

    /**
     * 初始化底层 SeataSnowflake, 不调用本方法则使用默认实现
     * <p>
     * 注意：必须在首次调用 {@link #nextSeataSnowflakeId()} 或
     * {@link #nextSeataSnowflakeIdAsString()} 之前调用, 否则默认实现已经生效,
     * 本方法将抛出 {@link IllegalArgumentException}
     *
     * @param newSeataSnowflake newSeataSnowflake
     * @throws IllegalArgumentException 如果参数为 null, 或已经初始化过
     */
    public static void initSeataSnowflake(SeataSnowflake newSeataSnowflake) {
        Validate.notNull(newSeataSnowflake, "newSeataSnowflake must not be null");
        synchronized (IdUtil.class) {
            Validate.isNull(seataSnowflake,
                    "seataSnowflake is already initialized, initSeataSnowflake must be called before the first id generation");
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
        return toHexWithoutDash(UUID.randomUUID());
    }

    /**
     * 将 UUID 编码为 32 位小写十六进制 (无横杠)
     * <p>
     * 直接把 128 位写成十六进制, 避免 {@code UUID.toString() + replace} 的两次额外字符串分配.
     * 结果与 {@code uuid.toString().replace("-", "")} 完全一致
     *
     * @param uuid UUID
     * @return 32 位十六进制字符串
     */
    static String toHexWithoutDash(UUID uuid) {
        byte[] hex = new byte[32];
        writeHex(hex, 0, uuid.getMostSignificantBits());
        writeHex(hex, 16, uuid.getLeastSignificantBits());
        return new String(hex, StandardCharsets.US_ASCII);
    }

    /**
     * 将 long 按大端序写成 16 位十六进制字符
     *
     * @param target 目标数组
     * @param offset 起始下标
     * @param value  待写入的值
     */
    private static void writeHex(byte[] target, int offset, long value) {
        for (int i = 0; i < 16; i++) {
            // 从最高的 4 位开始, 每次取一个十六进制位
            int nibble = (int) (value >>> ((15 - i) * 4)) & 0xF;
            target[offset + i] = (byte) HEX_DIGITS.charAt(nibble);
        }
    }
}
