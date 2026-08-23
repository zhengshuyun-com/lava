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

/**
 * 邮件摘要和经过大小限制的正文，不会在此对象中保留附件字节。
 *
 * <p>HTML 正文来自远端且未净化，渲染前必须由应用执行安全处理。</p>
 *
 * @param summary  消息摘要
 * @param textBody 第一段纯文本正文，没有时为 {@code null}
 * @param htmlBody 第一段 HTML 正文，没有时为 {@code null}
 */
public record MailMessage(
        MailMessageSummary summary,
        @Nullable String textBody,
        @Nullable String htmlBody) {
    /**
     * 校验邮件消息。
     *
     * @param summary  消息摘要
     * @param textBody 可选纯文本正文
     * @param htmlBody 可选 HTML 正文
     */
    public MailMessage {
        ValidationUtils.requireNonNull(summary, "summary");
    }
}
