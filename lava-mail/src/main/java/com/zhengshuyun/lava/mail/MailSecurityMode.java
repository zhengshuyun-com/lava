/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhengshuyun.lava.mail;

/**
 * SMTP 或 IMAP 的传输安全模式；明文模式必须显式选择。
 */
public enum MailSecurityMode {
    /**
     * 从建连开始即使用 TLS。
     */
    SSL_TLS,
    /**
     * 先建立普通连接，再强制升级为 TLS。
     */
    STARTTLS,
    /**
     * 不使用 TLS，只适合受控测试环境。
     */
    PLAINTEXT
}
