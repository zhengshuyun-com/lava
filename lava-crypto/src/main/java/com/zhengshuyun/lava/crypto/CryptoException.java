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

/** 加密、密码哈希或密钥处理失败时抛出的通用异常。 */
public final class CryptoException extends RuntimeException {

    /**
     * 创建带错误消息的异常实例。
     *
     * @param message 错误消息
     */
    public CryptoException(String message) {
        super(message);
    }

    /**
     * 创建带错误消息和原因的异常实例。
     *
     * @param message 错误消息
     * @param cause 根因异常
     */
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
