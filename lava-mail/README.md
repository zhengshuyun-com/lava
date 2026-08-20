# lava-mail

`lava-mail` 是单模块 SMTP/IMAP 客户端，支持密码和 OAuth 2 refresh token 认证、TLS 主机身份校验、有界 MIME 处理，以及基于
UID/UIDVALIDITY 的稳定分页。它依赖 `lava-http`、`lava-json` 和 Angus/Jakarta Mail。

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-mail</artifactId>
</dependency>
```

## 发送邮件

```java
SmtpServerConfig smtp = SmtpServerConfig.startTls("smtp.example.com", 587);
MailCredential credential = new PasswordCredential("robot@example.com", password);

MailSendRequest request = MailSendRequest.text(
        new MailAddress("robot@example.com", "Robot"),
        List.of(new MailAddress("ops@example.com")),
        "Daily report",
        "All jobs completed.");

try (MailSender sender = new MailSender(smtp, credential)) {
    MailSendResult result = sender.send(request);
}
```

地址由 Jakarta Mail 严格解析，端口范围是 1–65535，连接/读/写超时使用 `Duration`。`SSL_TLS` 和 `STARTTLS` 都启用服务器身份校验；
`STARTTLS` 必须升级成功，不会静默降级。`PLAINTEXT` 只能显式选择，生产凭证不得通过明文连接发送。

## OAuth 2

```java
MailProviderPreset outlook = MailProviders.outlook();
OAuth2RefreshTokenCredential credential = outlook.oauthCredential(
        username, clientId, refreshToken, clientSecret);

try (MailSender sender = new MailSender(outlook.smtp(), credential)) {
    sender.send(request);
}
```

自定义 `OAuth2RefreshTokenCredential` 的 token endpoint 必须是没有 user-info/fragment 的绝对 HTTPS URI。

每个 sender/reader 为 token 交换创建模块私有 HTTP 客户端：不继承应用的拦截器、Cookie、代理或连接池；禁用重定向和自动重试，并将响应限制为
64 KiB。token 刷新使用注入的 `Clock` 和 `MailClientOptions.tokenRefreshAhead`，并发刷新会合并；响应缺少 `expires_in`
时不会猜测缓存期限。

## UID 分页读取

```java
ImapServerConfig imap = ImapServerConfig.implicitTls("imap.example.com", 993);

try (MailReader reader = new MailReader(imap, credential)) {
    MailQuery query = MailQuery.firstPage(50);
    MailPage<MailMessageSummary> page = reader.listMessages(query);

    for (MailMessageSummary summary : page.items()) {
        MailMessage body = reader.readMessage(summary.id());
    }

    if (page.nextCursor() != null) {
        MailPage<MailMessageSummary> older =
                reader.listMessages(query.nextPage(page.nextCursor()));
    }
}
```

列表只返回 header、状态和附件元数据，不保留正文或附件字节。`MailMessageId(folder, uidValidity, uid)` 是后续读取的稳定标识；mailbox
的 UIDVALIDITY 改变后，旧 ID/cursor 会被拒绝，调用方应从第一页重新同步。

每次 reader 操作独立打开并关闭 IMAP store/folder。附件按 summary 中的 index 单独流式下载：

```java
try (OutputStream output = Files.newOutputStream(target)) {
    long bytes = reader.downloadAttachment(messageId, attachmentIndex, output);
}
```

`downloadAttachment` 借用 destination，不关闭也不 flush。调用方拥有输出流，并负责部分写入失败后的临时文件处理。

## 限制与失败

`MailLimits.DEFAULT`：正文 10 MiB、单附件 25 MiB、解码预算 50 MiB、MIME 嵌套 20 层。发送时，解码预算覆盖正文与全部附件的合计；读取正文时覆盖本次解析保留的
text/html，单独下载的每个附件同时受附件上限和解码预算限制。跨多个独立读取/下载调用不会累计同一个进程内预算。限制可通过
`MailClientOptions` 显式调整。

`MailException.kind()` 区分 `CONFIGURATION`、`AUTHENTICATION`、`TLS`、`CONNECTION`、`TIMEOUT`、`PROTOCOL`、`PARSING` 和
`SIZE_LIMIT`。Lava 的异常消息及 credential `toString()` 不包含密码、refresh token 或 access token；credential accessor
返回的仍是真实秘密，调用方不能记录。

`MailMessage.htmlBody()` 是未净化的远端 HTML，附件文件名/content type 也是不可信 metadata；渲染或落盘前必须由应用进行净化和安全命名。
