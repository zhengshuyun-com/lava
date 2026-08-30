# lava-mail

`lava-mail` 提供 SMTP 发信和 IMAP 收信能力，支持密码与 OAuth 2 refresh token 认证、TLS 主机身份校验、有界 MIME 处理，以及基于 UID/UIDVALIDITY 的稳定分页。

## 添加依赖

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-mail</artifactId>
</dependency>
```

模块依赖 `lava-http`、`lava-json` 和 Angus/Jakarta Mail。

## 能力概览

| 能力 | 入口 |
| --- | --- |
| SMTP 发信 | `MailSender` |
| IMAP 列表、正文和附件 | `MailReader` |
| 密码认证 | `PasswordCredential` |
| OAuth 2 refresh token | `OAuth2RefreshTokenCredential` |
| 提供商预设 | `MailProviders` |
| MIME 上限 | `MailLimits`、`MailClientOptions` |

`MailSender` 和 `MailReader` 都是可关闭的长生命周期对象。继续查看 [发送邮件](./sending)、[OAuth 2](./oauth2)、[读取邮件](./reading) 和 [限制与安全](./limits-security)。
