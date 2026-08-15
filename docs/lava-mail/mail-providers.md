# MailProviders 厂商预置

`MailProviders` 提供常见邮箱服务商的默认服务器配置和 OAuth2 参数.

## 最小可运行示例

```java
import com.zhengshuyun.lava.mail.MailReader;
import com.zhengshuyun.lava.mail.PasswordCredential;
import com.zhengshuyun.lava.mail.provider.MailProviderPreset;
import com.zhengshuyun.lava.mail.provider.MailProviders;

public class MailProvidersDemo {

    public static void main(String[] args) {
        MailProviderPreset provider = MailProviders.qq();
        PasswordCredential credential = PasswordCredential.builder()
                .setUsername("your@qq.com")
                .setPassword("qq-mail-auth-code")
                .build();

        MailReader reader = MailReader.builder()
                .setImapServerConfig(provider.getImapServerConfig())
                .setCredential(credential)
                .build();

        // TODO: 使用 reader 读取邮件
    }
}
```

## 预置厂商

| 方法                      | 说明                               |
|---------------------------|------------------------------------|
| `MailProviders.qq()`      | QQ 邮箱默认 IMAP/SMTP 配置         |
| `MailProviders.hotmail()` | Hotmail 默认配置和 OAuth2 参数     |
| `MailProviders.outlook()` | Outlook.com 默认配置和 OAuth2 参数 |

## 常见坑与排查建议

| 异常/消息              | 原因                           | 解决方式                                     |
|------------------------|--------------------------------|----------------------------------------------|
| 预置参数不符合企业邮箱 | 企业邮箱和公共邮箱配置不同     | 改用手配 `ImapServerConfig/SmtpServerConfig` |
| OAuth2 仍认证失败      | 预置只提供默认参数, 不负责授权 | 检查 refresh token 和 scope                  |
| 文件夹名称不一致       | 服务商有自定义文件夹           | 标准值用 `MailFolder`, 特殊值直接传字符串    |
