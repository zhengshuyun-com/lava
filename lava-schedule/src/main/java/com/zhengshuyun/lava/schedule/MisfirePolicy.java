/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.zhengshuyun.lava.schedule;

/**
 * 任务在一个或多个触发时刻之后才安装或恢复时应用的策略。
 */
public enum MisfirePolicy {
    /**
     * 丢弃全部错过的触发时刻，并从下一个未来时刻继续。
     */
    SKIP,
    /**
     * 执行一次错过的任务，并从当前时刻继续。
     */
    FIRE_ONCE,
    /**
     * 将每次错过的任务交给任务的有界并发策略处理。
     */
    CATCH_UP
}
