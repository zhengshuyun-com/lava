# MailReader 读取邮件

`MailReader` 用于通过 IMAP 读取邮件列表, 支持文件夹, 未读筛选, 时间范围, 发件人, 主题和正文附件选项.

## 最小可运行示例

```java
import com.zhengshuyun.lava.mail.ImapServerConfig;
import com.zhengshuyun.lava.mail.MailFolder;
import com.zhengshuyun.lava.mail.MailMessage;
import com.zhengshuyun.lava.mail.MailQuery;
import com.zhengshuyun.lava.mail.MailReader;
import com.zhengshuyun.lava.mail.MailSecurityMode;
import com.zhengshuyun.lava.mail.PasswordCredential;

import java.util.List;

public class MailReaderDemo {

    public static void main(String[] args) {
        PasswordCredential credential = PasswordCredential.builder()
                .setUsername("your@qq.com")
                .setPassword("qq-mail-auth-code")
                .build();

        MailReader reader = MailReader.builder()
                .setImapServerConfig(ImapServerConfig.builder()
                        .setHost("imap.qq.com")
                        .setPort(993)
                        .setSecurityMode(MailSecurityMode.SSL_TLS)
                        .build())
                .setCredential(credential)
                .build();

        List<MailMessage> messages = reader.listMessages(MailQuery.builder()
                .setFolder(MailFolder.INBOX)
                .setLimit(10)
                .setUnreadOnly(true)
                .setSubjectContains("验证码")
                .build());

        // TODO: 按业务处理 messages
    }
}
```

## 查询选项

| 选项                         | 说明             |
|------------------------------|------------------|
| `setFolder(...)`             | 设置邮件文件夹   |
| `setLimit(...)`              | 限制读取数量     |
| `setUnreadOnly(...)`         | 只读未读邮件     |
| `setReceivedAfter(...)`      | 接收时间下限     |
| `setReceivedBefore(...)`     | 接收时间上限     |
| `setFrom(...)`               | 按发件人筛选     |
| `setSubjectContains(...)`    | 按主题关键字筛选 |
| `setIncludeBody(...)`        | 是否解析正文     |
| `setIncludeAttachments(...)` | 是否解析附件     |

## 常见坑与排查建议

| 异常/消息    | 原因                     | 解决方式                                          |
|--------------|--------------------------|---------------------------------------------------|
| 读取不到附件 | 默认不解析附件           | 设置 `includeAttachments=true`                    |
| 内存占用高   | 附件内容会读入 `byte[]`  | 限制查询数量和附件大小                            |
| 文件夹不存在 | 文件夹名称和服务商不一致 | 标准文件夹用 `MailFolder`, 特殊文件夹直接传字符串 |
