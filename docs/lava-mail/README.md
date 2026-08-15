# 邮件收发概览

`lava-mail` 提供同步邮件发送和收取能力, 支持 SMTP, IMAP, 密码凭证和 OAuth2 refresh token 凭证.

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-mail`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-mail</artifactId>
</dependency>
```

## 模块能力

| 能力           | 入口                      | 文档                                          |
|----------------|---------------------------|-----------------------------------------------|
| 发信           | `MailSender`              | [MailSender 发送邮件](./mail-sender.md)       |
| 收信           | `MailReader`              | [MailReader 读取邮件](./mail-reader.md)       |
| 凭证           | `MailCredential`          | [MailCredential 凭证](./mail-credential.md)   |
| 厂商预置       | `MailProviders`           | [MailProviders 厂商预置](./mail-providers.md) |
| QQ 邮箱        | `MailProviders.qq()`      | [QQ 邮箱接入](./qq-mail.md)                   |
| Outlook OAuth2 | `MailProviders.hotmail()` | [Outlook OAuth2 接入](./outlook-oauth2.md)    |
| 附件           | `MailAttachment`          | [邮件附件](./mail-attachment.md)              |

## 快速示例

```java
import com.zhengshuyun.lava.mail.MailAddress;
import com.zhengshuyun.lava.mail.MailSendRequest;
import com.zhengshuyun.lava.mail.MailSendResult;
import com.zhengshuyun.lava.mail.MailSecurityMode;
import com.zhengshuyun.lava.mail.MailSender;
import com.zhengshuyun.lava.mail.PasswordCredential;
import com.zhengshuyun.lava.mail.SmtpServerConfig;

public class MailQuickStartDemo {

    public static void main(String[] args) {
        PasswordCredential credential = PasswordCredential.builder()
                .setUsername("your@qq.com")
                .setPassword("qq-mail-auth-code")
                .build();

        MailSender sender = MailSender.builder()
                .setSmtpServerConfig(SmtpServerConfig.builder()
                        .setHost("smtp.qq.com")
                        .setPort(465)
                        .setSecurityMode(MailSecurityMode.SSL_TLS)
                        .build())
                .setCredential(credential)
                .build();

        MailSendResult result = sender.send(MailSendRequest.builder()
                .setFrom(MailAddress.builder().setAddress("your@qq.com").build())
                .addTo(MailAddress.builder().setAddress("friend@example.com").build())
                .setSubject("hello from lava-mail")
                .setTextBody("plain text body")
                .build());

        // TODO: 按业务处理 result
    }
}
```

## 使用建议

- 优先使用通用手配方式, 即 `MailReader/MailSender + ServerConfig + MailCredential`.
- 厂商预置只负责提供常见默认参数, 不隐藏协议和凭证概念.
- OAuth2 是认证方式, 不是收信协议.
- 当前版本解析附件时会把附件内容读入内存中的 `byte[]`.
