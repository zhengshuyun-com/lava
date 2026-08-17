/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

/**
 * 由文件夹名、UIDVALIDITY 和 UID 组成的稳定 IMAP 消息标识。
 *
 * @param folder 文件夹全名
 * @param uidValidity 邮箱的 UIDVALIDITY
 * @param uid 消息 UID
 */
public record MailMessageId(String folder, long uidValidity, long uid) {
    static final long MAX_IMAP_UID = 0xffff_ffffL;

    /**
     * 校验并规范化消息标识。
     *
     * @param folder 文件夹全名
     * @param uidValidity 邮箱 UIDVALIDITY
     * @param uid 消息 UID
     */
    public MailMessageId {
        folder = PasswordCredential.requireNonBlankWithoutControls(folder, "folder");
        if (!validUid(uidValidity) || !validUid(uid)) {
            throw new IllegalArgumentException(
                    "uidValidity and uid must be unsigned 32-bit positive values");
        }
    }

    static boolean validUid(long value) {
        return value >= 1 && value <= MAX_IMAP_UID;
    }
}
