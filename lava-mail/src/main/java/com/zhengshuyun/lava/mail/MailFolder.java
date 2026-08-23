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
 * 常见邮箱文件夹名称；服务商可能使用不同的本地化名称。
 */
public enum MailFolder {
    /**
     * 收件箱。
     */
    INBOX("INBOX"),
    /**
     * 草稿箱。
     */
    DRAFTS("Drafts"),
    /**
     * 已发送。
     */
    SENT("Sent"),
    /**
     * 已删除。
     */
    TRASH("Trash"),
    /**
     * 垃圾邮件。
     */
    SPAM("Spam");

    private final String folderName;

    MailFolder(String folderName) {
        this.folderName = folderName;
    }

    /**
     * 返回默认文件夹名。
     *
     * @return 文件夹名
     */
    public String folderName() {
        return folderName;
    }
}
