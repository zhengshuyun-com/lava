# MailSender 发送邮件

`MailSender` 用于通过 SMTP 发送邮件, 支持纯文本, HTML, 附件, cc, bcc 和 replyTo.

## 最小可运行示例

```java
import com.zhengshuyun.lava.mail.MailAddress;
import com.zhengshuyun.lava.mail.MailSendRequest;
import com.zhengshuyun.lava.mail.MailSendResult;
import com.zhengshuyun.lava.mail.MailSecurityMode;
import com.zhengshuyun.lava.mail.MailSender;
import com.zhengshuyun.lava.mail.PasswordCredential;
import com.zhengshuyun.lava.mail.SmtpServerConfig;

public class MailSenderDemo {

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
                .setFrom(MailAddress.builder()
                        .setAddress("your@qq.com")
                        .setPersonal("Lava Mail Bot")
                        .build())
                .addTo(MailAddress.builder().setAddress("friend@example.com").build())
                .setSubject("hello from lava-mail")
                .setTextBody("plain text body")
                .setHtmlBody("<p>html body</p>")
                .build());

        // TODO: 按业务处理 result
    }
}
```

## 自定义发件人名称

```java
MailSendRequest request = MailSendRequest.builder()
        .setFrom(MailAddress.builder()
                .setAddress("system@example.com")
                .setPersonal("System Notice")
                .build())
        .addTo(MailAddress.builder().setAddress("user@example.com").build())
        .setSubject("订单确认信")
        .setTextBody("hello")
        .build();
```

- `personal` 是显示名称, 不会改变真实发信邮箱地址.
- 最终展示样式受收件方客户端影响.

## 常见坑与排查建议

| 异常/消息            | 原因                           | 解决方式                        |
|----------------------|--------------------------------|---------------------------------|
| SMTP 鉴权失败        | 密码, 授权码或 OAuth2 凭证错误 | 核对邮箱服务商认证方式          |
| 收件人为空           | 未设置 `to/cc/bcc`             | 至少设置一个收件人              |
| 发件人显示不符合预期 | 客户端展示规则不同             | 确认标准邮件头已携带 `personal` |
