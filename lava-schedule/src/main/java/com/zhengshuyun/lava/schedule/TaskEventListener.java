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

/** 接收终态执行事件；监听器失败会与调度器隔离。 */
@FunctionalInterface
public interface TaskEventListener {

    /**
     * 接收一个终态事件。
     *
     * <p>调用发生在产生事件的调度或执行线程；监听器应避免长时间阻塞。
     *
     * @param event 任务终态事件
     */
    void onEvent(TaskEvent event);
}
