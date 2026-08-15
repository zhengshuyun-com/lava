# Outlook OAuth2 接入

Hotmail/Outlook.com 推荐使用 OAuth2 refresh token, 通过 XOAUTH2 接入 IMAP 和 SMTP.

## 服务器参数

| 协议 | 地址                    | 端口  | 安全模式   |
|------|-------------------------|-------|------------|
| IMAP | `outlook.office365.com` | `993` | `SSL/TLS`  |
| SMTP | `smtp-mail.outlook.com` | `587` | `STARTTLS` |

## 最小可运行示例

```java
import com.zhengshuyun.lava.mail.OAuth2RefreshTokenCredential;
import com.zhengshuyun.lava.mail.provider.MailProviderPreset;
import com.zhengshuyun.lava.mail.provider.MailProviders;

public class OutlookOAuth2Demo {

    public static void main(String[] args) {
        MailProviderPreset provider = MailProviders.hotmail();

        OAuth2RefreshTokenCredential credential = provider.createOAuth2CredentialBuilder()
                .setUsername("your@hotmail.com")
                .setClientId("your-client-id")
                .setRefreshToken("your-refresh-token")
                .build();

        // TODO: 用 provider 和 credential 构建 MailReader/MailSender
    }
}
```

## 常见坑与排查建议

| 异常/消息      | 原因                                        | 解决方式             |
|----------------|---------------------------------------------|----------------------|
| token 换取失败 | `clientId`, `refreshToken` 或 endpoint 错误 | 检查 OAuth2 授权配置 |
| SMTP 无权限    | scope 缺少 `SMTP.Send`                      | 重新授权对应 scope   |
| IMAP 无权限    | scope 缺少 `IMAP.AccessAsUser.All`          | 重新授权对应 scope   |
