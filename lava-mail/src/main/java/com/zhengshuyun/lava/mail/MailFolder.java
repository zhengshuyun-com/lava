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

package com.zhengshuyun.lava.mail;

/**
 * 常见邮件文件夹
 * <p>
 * 这里提供的是常见默认值, 不同厂商的真实文件夹名可能存在差异.
 * 如果枚举值不满足实际场景, 仍然可以继续使用字符串方式传自定义文件夹名.
 *
 * @author Toint
 * @since 2026/4/21
 */
public enum MailFolder {

    /**
     * 收件箱
     */
    INBOX("INBOX"),

    /**
     * 草稿箱
     */
    DRAFTS("Drafts"),

    /**
     * 已发送
     */
    SENT("Sent"),

    /**
     * 垃圾箱
     */
    TRASH("Trash"),

    /**
     * 垃圾邮件
     */
    SPAM("Spam");

    /**
     * 文件夹名
     */
    private final String value;

    MailFolder(String value) {
        this.value = value;
    }

    /**
     * 获取文件夹名
     *
     * @return 文件夹名
     */
    public String getValue() {
        return value;
    }
}
