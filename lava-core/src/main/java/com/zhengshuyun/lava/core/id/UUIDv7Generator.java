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

import com.zhengshuyun.lava.core.lang.ValidationUtils;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

/**
 * RFC 9562 UUIDv7 生成器。
 *
 * <p>生成器在同一毫秒和时钟回拨时递增 74 位随机区，因此单个实例生成的 UUID 保持唯一且单调。
 * 实例可在线程间安全复用。
 *
 * <p>单调性由递增实现，因此同一毫秒内的相邻值仅相差 1，可由前一个值推出后一个值。不要将本类的
 * 输出用作安全令牌或需要防猜测的资源标识，这类场景请改用 {@link IdUtils#nextUUID()} 的 UUIDv4。
 */
public final class UUIDv7Generator {

    private static final int RANDOM_A_BOUND = 1 << 12;
    private static final int MAX_RANDOM_A = RANDOM_A_BOUND - 1;
    private static final long MAX_RANDOM_B = (1L << 62) - 1;
    private static final long MAX_UNIX_MILLIS = (1L << 48) - 1;
    private static final long VERSION_7 = 0x7000L;
    private static final long RFC_4122_VARIANT = 0x8000_0000_0000_0000L;

    private final Clock clock;
    private final SecureRandom random;

    private long lastUnixMillis = -1;
    private int randomA;
    private long randomB;

    /**
     * 使用 UTC 系统时钟和安全随机源创建生成器。
     */
    public UUIDv7Generator() {
        this(Clock.systemUTC(), new SecureRandom());
    }

    /**
     * 使用指定时钟和安全随机源创建生成器。
     *
     * @param clock  读取当前时间的时钟
     * @param random 用于 UUIDv7 随机位的安全随机源
     */
    public UUIDv7Generator(Clock clock, SecureRandom random) {
        this.clock = ValidationUtils.requireNonNull(clock, "clock");
        this.random = ValidationUtils.requireNonNull(random, "random");
    }

    /**
     * 返回下一个 UUIDv7。
     *
     * @return 新的 UUIDv7
     */
    public synchronized UUID next() {
        long unixMillis = clock.millis();
        if (unixMillis < 0 || unixMillis > MAX_UNIX_MILLIS) {
            throw new IdGenerationException(
                    "UUIDv7 timestamp is outside the 48-bit Unix millisecond range: "
                            + unixMillis);
        }

        if (unixMillis > lastUnixMillis) {
            lastUnixMillis = unixMillis;
            randomA = random.nextInt(RANDOM_A_BOUND);
            randomB = random.nextLong() & MAX_RANDOM_B;
        } else {
            incrementRandomBits();
        }

        long mostSignificantBits = (lastUnixMillis << 16) | VERSION_7 | randomA;
        long leastSignificantBits = RFC_4122_VARIANT | randomB;
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    private void incrementRandomBits() {
        if (randomB < MAX_RANDOM_B) {
            randomB++;
            return;
        }
        if (randomA == MAX_RANDOM_A) {
            throw new IdGenerationException(
                    "UUIDv7 random sequence exhausted at Unix millisecond " + lastUnixMillis);
        }
        randomA++;
        randomB = 0;
    }
}
