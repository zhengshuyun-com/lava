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

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * SMTP 提交成功后的结果。
 *
 * @param messageId 客户端生成的 Message-ID，没有时为 {@code null}
 * @param sentAt    提交完成时间
 */
public record MailSendResult(@Nullable String messageId, Instant sentAt) {
    /**
     * 校验发信结果。
     *
     * @param messageId 可选 Message-ID
     * @param sentAt    提交完成时间
     */
    public MailSendResult {
        ValidationUtils.requireNonNull(sentAt, "sentAt");
    }
}
