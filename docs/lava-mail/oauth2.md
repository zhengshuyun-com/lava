# OAuth 2 认证

## 提供商预设

```java
MailProviderPreset provider = MailProviders.outlook();

OAuth2RefreshTokenCredential credential = provider.oauthCredential(
        username,
        clientId,
        refreshToken,
        clientSecret
);

try (MailSender sender = new MailSender(provider.smtp(), credential)) {
    sender.send(request);
}

try (MailReader reader = new MailReader(provider.imap(), credential)) {
    MailPage<MailMessageSummary> page = reader.listMessages(
            MailQuery.firstPage(50)
    );
}
```

`MailProviders` 提供常见服务的服务器与 OAuth 2 配置预设。使用前仍需确认租户、应用类型、授权范围和服务商当前策略。

## 自定义配置

```java
OAuth2RefreshTokenCredential credential =
        new OAuth2RefreshTokenCredential(
                username,
                clientId,
                refreshToken,
                tokenEndpoint,
                scopes,
                clientSecret
        );
```

Token endpoint 必须是不含 user-info 和 fragment 的绝对 HTTPS URI。

## 刷新行为

每个 sender 或 reader 为 token 交换创建模块私有 HTTP 客户端：

- 不继承应用拦截器、Cookie、代理或连接池；
- 禁用 HTTP 重定向和自动重试；
- token 响应最大 64 KiB；
- 并发刷新会合并；
- 根据 `Clock` 和 `tokenRefreshAhead` 提前刷新；
- 响应缺少 `expires_in` 时不猜测缓存期限。

密码、refresh token、client secret 和 access token 都不能进入日志。Credential 的 accessor 返回真实秘密，调用方必须自行保护。
