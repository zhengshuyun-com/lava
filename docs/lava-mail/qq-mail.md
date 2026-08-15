# QQ 邮箱接入

QQ 邮箱推荐使用邮箱授权码, 通过 `PasswordCredential` 接入 IMAP 和 SMTP.

## 服务器参数

| 协议 | 地址          | 端口  | 安全模式  |
|------|---------------|-------|-----------|
| IMAP | `imap.qq.com` | `993` | `SSL/TLS` |
| SMTP | `smtp.qq.com` | `465` | `SSL/TLS` |

## 发送邮件

```java
PasswordCredential credential = PasswordCredential.builder()
        .setUsername("your@qq.com")
        .setPassword("qq-mail-auth-code")
        .build();

MailProviderPreset provider = MailProviders.qq();

MailSender sender = MailSender.builder()
        .setSmtpServerConfig(provider.getSmtpServerConfig())
        .setCredential(credential)
        .build();
```

## 读取邮件

```java
MailReader reader = MailReader.builder()
        .setImapServerConfig(provider.getImapServerConfig())
        .setCredential(credential)
        .build();
```

## 常见坑与排查建议

| 异常/消息  | 原因                       | 解决方式                                         |
|------------|----------------------------|--------------------------------------------------|
| 认证失败   | 使用了登录密码而不是授权码 | 在 QQ 邮箱后台生成授权码                         |
| 无法连接   | 端口或安全模式错误         | IMAP 使用 `993 SSL/TLS`, SMTP 使用 `465 SSL/TLS` |
| 收不到邮件 | 文件夹或筛选条件不对       | 先读取 `MailFolder.INBOX` 并放宽筛选             |
