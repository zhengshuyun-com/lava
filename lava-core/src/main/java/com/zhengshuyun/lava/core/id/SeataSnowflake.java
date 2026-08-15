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

import org.jspecify.annotations.Nullable;

import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.OptionalLong;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Seata改进的雪花算法ID
 *
 * <p>Seata 对传统雪花算法进行了改良, 核心改进是通过 {@link AtomicLong} 实现严格连续递增的 ID 生成,
 * 相比传统实现具有更好的数据库索引性能和更高的并发吞吐量.
 *
 * @author Toint
 * @see <a href="https://seata.apache.org/zh-cn/blog/seata-snowflake-explain">关于新版雪花算法的答疑</a>
 * @see <a href="https://seata.apache.org/zh-cn/blog/seata-analysis-UUID-generator">Seata基于改良版雪花算法的分布式UUID生成器分析</a>
 * @since 2026/1/6
 */
public final class SeataSnowflake {
    /**
     * 起始时间戳 (2020-05-03)
     */
    private static final long TWEPOCH = 1588435200000L;

    /**
     * workerId 占用的位数
     */
    private static final int WORKER_ID_BITS = 10;

    /**
     * 时间戳占用的位数
     */
    private static final int TIMESTAMP_BITS = 41;

    /**
     * 序列号占用的位数
     */
    private static final int SEQUENCE_BITS = 12;

    /**
     * 最大支持的机器ID, 结果是1023
     */
    private static final int MAX_WORKER_ID = ~(-1 << WORKER_ID_BITS);

    /**
     * 业务含义：机器ID (0 ~ 1023)
     * 实际内存布局：
     * 最高位1位：0
     * 中间10位：workerId
     * 最低位53位：全0
     */
    private final long workerId;

    /**
     * 时间戳和序列号混合在一个Long中
     * 最高11位：未使用
     * 中间41位：时间戳
     * 最低12位：序列号
     */
    private final AtomicLong timestampAndSequence;

    /**
     * 用于从long中提取时间戳和序列号的掩码
     */
    private static final long TIMESTAMP_AND_SEQUENCE_MASK = ~(-1L << (TIMESTAMP_BITS + SEQUENCE_BITS));

    /**
     * 使用自动分配的workerId实例化IdWorker
     */
    public SeataSnowflake() {
        this(null);
    }

    /**
     * 使用给定的workerId实例化IdWorker
     *
     * @param workerId 机器ID (0 ~ 1023) 如果为null, 则自动分配一个
     */
    public SeataSnowflake(@Nullable Long workerId) {
        // 立即初始化第一个时间戳和序列号
        this.timestampAndSequence = new AtomicLong(getNewestTimestamp() << SEQUENCE_BITS);

        long resolvedWorkerId = workerId != null ? workerId : generateWorkerId();
        if (resolvedWorkerId > MAX_WORKER_ID || resolvedWorkerId < 0) {
            String message = String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID);
            throw new IllegalArgumentException(message);
        }
        this.workerId = resolvedWorkerId << (TIMESTAMP_BITS + SEQUENCE_BITS);
    }

    /**
     * 生成严格连续递增的雪花ID
     *
     * @return 雪花ID. 示例: 7584796163497562113
     */
    public long nextId() {
        waitIfNecessary();
        long next = timestampAndSequence.incrementAndGet();
        long timestampWithSequence = next & TIMESTAMP_AND_SEQUENCE_MASK;
        return workerId | timestampWithSequence;
    }

    /**
     * @see SeataSnowflake#nextId()
     */
    public String nextIdAsString() {
        return Long.toString(nextId());
    }

    /**
     * 如果获取UUID的QPS过高导致当前序列号空间耗尽, 则阻塞当前线程
     * <p>
     * 注意：必须恢复线程中断状态, 否则在线程池等场景下可能导致死锁
     */
    private void waitIfNecessary() {
        long currentWithSequence = timestampAndSequence.get();
        long current = currentWithSequence >>> SEQUENCE_BITS;
        long newest = getNewestTimestamp();
        if (current >= newest) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 获取相对于起始时间戳的最新时间戳
     */
    private long getNewestTimestamp() {
        return System.currentTimeMillis() - TWEPOCH;
    }

    /**
     * 自动生成workerId, 优先尝试使用MAC地址, 如果失败则随机生成一个
     *
     * @return workerId
     */
    private static long generateWorkerId() {
        return generateWorkerIdBaseOnMac().orElseGet(SeataSnowflake::generateRandomWorkerId);
    }

    /**
     * 使用可用MAC地址的最低10位作为workerId
     *
     * @return workerId, 没有可用MAC地址时返回空
     */
    private static OptionalLong generateWorkerIdBaseOnMac() {
        // 这里刻意捕获 Exception 而非 SocketException, 与上游 Seata 实现保持一致:
        // getNetworkInterfaces() 在 SecurityManager 受限策略下会抛 SecurityException (RuntimeException),
        // 且在部分平台/容器环境下可能返回 null. ID 生成位于启动关键路径,
        // 任何探测失败都必须降级为随机 workerId, 不能让异常冒泡导致服务无法生成 ID
        try {
            Enumeration<NetworkInterface> all = NetworkInterface.getNetworkInterfaces();
            if (all == null) {
                return OptionalLong.empty();
            }
            while (all.hasMoreElements()) {
                NetworkInterface networkInterface = all.nextElement();
                boolean isLoopback = networkInterface.isLoopback();
                boolean isVirtual = networkInterface.isVirtual();
                byte[] mac = networkInterface.getHardwareAddress();
                // 上游只判 mac == null, 但非以太网卡 (firewire 等) 的 MAC 可能不足 6 字节,
                // 上游会因 mac[4]/mac[5] 越界抛 AIOOBE 后整体降级为随机;
                // 这里跳过该网卡继续找下一张, 严格优于上游
                if (isLoopback || isVirtual || mac == null || mac.length < 6) {
                    continue;
                }
                return OptionalLong.of(((mac[4] & 0B11) << 8) | (mac[5] & 0xFF));
            }
        } catch (Exception e) {
            return OptionalLong.empty();
        }
        return OptionalLong.empty();
    }

    /**
     * 随机生成一个作为workerId
     *
     * @return workerId
     */
    private static long generateRandomWorkerId() {
        return ThreadLocalRandom.current().nextInt(MAX_WORKER_ID + 1);
    }
}
