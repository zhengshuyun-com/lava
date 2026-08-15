# MailCredential 凭证

`lava-mail` 支持密码凭证和 OAuth2 refresh token 凭证.

## 密码凭证

```java
PasswordCredential credential = PasswordCredential.builder()
        .setUsername("your@qq.com")
        .setPassword("qq-mail-auth-code")
        .build();
```

- QQ 邮箱首版推荐使用邮箱授权码.
- 密码或授权码不要硬编码, 建议使用配置中心或密文注入.

## OAuth2 refresh token 凭证

```java
OAuth2RefreshTokenCredential credential = OAuth2RefreshTokenCredential.builder()
        .setUsername("your@hotmail.com")
        .setClientId("your-client-id")
        .setRefreshToken("your-refresh-token")
        .setTokenEndpoint("https://login.microsoftonline.com/common/oauth2/v2.0/token")
        .addScope("offline_access")
        .addScope("https://outlook.office.com/IMAP.AccessAsUser.All")
        .addScope("https://outlook.office.com/SMTP.Send")
        .build();
```

- OAuth2 是认证方式, 不是收信协议.
- Hotmail/Outlook.com 场景推荐使用 OAuth2 XOAUTH2.

## 常见坑与排查建议

| 异常/消息         | 原因                                   | 解决方式                     |
|-------------------|----------------------------------------|------------------------------|
| `535` 或认证失败  | 密码, 授权码或 token 无效              | 核对服务商认证方式和权限     |
| OAuth2 scope 不足 | refresh token 未包含 IMAP 或 SMTP 权限 | 重新授权所需 scope           |
| 凭证泄露风险      | 明文写在代码或日志中                   | 使用 KMS, 配置中心或密文注入 |
