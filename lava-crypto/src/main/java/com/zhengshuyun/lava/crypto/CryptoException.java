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

/**
 * 加密操作异常
 *
 * @author Toint
 * @since 2026/2/7
 */
public class CryptoException extends RuntimeException {

    /**
     * 创建不带消息和 cause 的异常实例.
     */
    public CryptoException() {
    }

    /**
     * 创建带错误消息的异常实例.
     *
     * @param message 错误消息
     */
    public CryptoException(String message) {
        super(message);
    }

    /**
     * 创建带错误消息和 cause 的异常实例.
     *
     * @param message 错误消息
     * @param cause   根因异常
     */
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建仅包含 cause 的异常实例.
     *
     * @param cause 根因异常
     */
    public CryptoException(Throwable cause) {
        super(cause);
    }

    /**
     * 创建可控制抑制和堆栈写入行为的异常实例.
     *
     * @param message            错误消息
     * @param cause              根因异常
     * @param enableSuppression  是否启用抑制
     * @param writableStackTrace 是否可写堆栈
     */
    public CryptoException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
