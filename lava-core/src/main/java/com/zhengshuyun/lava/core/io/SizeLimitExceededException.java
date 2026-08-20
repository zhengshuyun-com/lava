/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.zhengshuyun.lava.core.io;

import java.io.IOException;

/**
 * 在无界流使返回的字节数组超过配置上限前抛出。
 */
public final class SizeLimitExceededException extends IOException {

    private final long maximumBytes;
    private final long observedBytes;

    SizeLimitExceededException(long maximumBytes, long observedBytes) {
        super("Stream exceeds the configured limit of " + maximumBytes + " bytes");
        this.maximumBytes = maximumBytes;
        this.observedBytes = observedBytes;
    }

    /**
     * 返回配置允许读取的最大字节数。
     *
     * @return 最大字节数
     */
    public long maximumBytes() {
        return maximumBytes;
    }

    /**
     * 返回越过上限时观测到的流大小下界。
     *
     * @return 已观测字节数的下界
     */
    public long observedBytes() {
        return observedBytes;
    }
}
