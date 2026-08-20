/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail.provider;

import com.zhengshuyun.lava.mail.ImapServerConfig;
import com.zhengshuyun.lava.mail.SmtpServerConfig;

import java.net.URI;
import java.util.List;

/**
 * 常用邮件服务商预设。
 */
public final class MailProviders {
    private static final MailOAuth2Profile MICROSOFT_OAUTH = new MailOAuth2Profile(
            URI.create("https://login.microsoftonline.com/common/oauth2/v2.0/token"),
            List.of(
                    "offline_access",
                    "https://outlook.office.com/IMAP.AccessAsUser.All",
                    "https://outlook.office.com/SMTP.Send"));

    private static final MailProviderPreset HOTMAIL = new MailProviderPreset(
            "hotmail",
            ImapServerConfig.implicitTls("outlook.office365.com", 993),
            SmtpServerConfig.startTls("smtp-mail.outlook.com", 587),
            MICROSOFT_OAUTH);

    private static final MailProviderPreset OUTLOOK = new MailProviderPreset(
            "outlook", HOTMAIL.imap(), HOTMAIL.smtp(), MICROSOFT_OAUTH);

    private static final MailProviderPreset QQ = new MailProviderPreset(
            "qq",
            ImapServerConfig.implicitTls("imap.qq.com", 993),
            SmtpServerConfig.implicitTls("smtp.qq.com", 465),
            null);

    private MailProviders() {
    }

    /**
     * 返回 Hotmail 预设。
     *
     * @return Hotmail 连接与 OAuth2 预设
     */
    public static MailProviderPreset hotmail() {
        return HOTMAIL;
    }

    /**
     * 返回 Outlook.com 预设。
     *
     * @return Outlook.com 连接与 OAuth2 预设
     */
    public static MailProviderPreset outlook() {
        return OUTLOOK;
    }

    /**
     * 返回 QQ 邮箱预设。
     *
     * @return QQ 邮箱连接预设
     */
    public static MailProviderPreset qq() {
        return QQ;
    }
}
