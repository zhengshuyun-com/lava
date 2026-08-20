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

/**
 * 标识符生成器因运行状态异常而无法继续生成标识符时抛出。
 */
public final class IdGenerationException extends IllegalStateException {

    /**
     * 使用指定错误消息创建异常。
     *
     * @param message 标识符生成失败的原因
     */
    public IdGenerationException(String message) {
        super(message);
    }
}
