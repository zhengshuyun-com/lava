# lava-mail

`lava-mail` 提供同步邮件收发能力.

- 收信协议: `IMAP`
- 发信协议: `SMTP`
- 支持凭证:
    - `PasswordCredential`
    - `OAuth2RefreshTokenCredential`

注意:

- `OAuth2` 是认证方式, 不是收信协议.
- `QQ邮箱` 首版推荐使用邮箱授权码, 走 `PasswordCredential`.
- `Hotmail/Outlook.com` 首版推荐使用 `OAuth2 refresh token -> access token -> XOAUTH2`.
- 当前版本返回附件内容时, 会把附件读入内存中的 `byte[]`.
- 收信查询默认不解析附件. 只有显式设置 `includeAttachments=true` 时, 才会抓取附件内容.
- 发信时支持自定义发件人显示名称, 例如 `DMIT Inc. <system@notice.dmit.io>`.

## Maven

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-mail</artifactId>
    <version>${lava.version}</version>
</dependency>
```

## 接入原则

推荐优先使用通用手配方式:

- `MailReader + ImapServerConfig + MailCredential`
- `MailSender + SmtpServerConfig + MailCredential`

这样即使没有内置预置的邮件厂商, 也能正常接入.

如果只是想少写样板配置, `lava-mail` 也提供一层可选便利层:

- `MailProviders.qq()`
- `MailProviders.hotmail()`
- `MailProviders.outlook()`

这层便利层只负责提供常见默认值, 例如:

- `ImapServerConfig`
- `SmtpServerConfig`
- 默认 `OAuth2 token endpoint`
- 默认 `OAuth2 scopes`

文件夹名称这类字段支持两种写法:

- 标准常见值可以用 `MailFolder`, 例如 `MailFolder.INBOX`
- 厂商特殊值或自定义文件夹仍然可以继续直接传字符串

## QQ邮箱示例

### 服务器参数

- IMAP: `imap.qq.com:993`, `SSL/TLS`
- SMTP: `smtp.qq.com:465`, `SSL/TLS`
- 认证: 邮箱地址 + 授权码

### 发信

建议把发件端和收件端拆开创建, 同一个账号可以复用同一份凭证对象.

```java
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
        .addAttachment(MailAttachment.builder()
                .setFileName("hello.txt")
                .setContentType("text/plain")
                .setContent("hello".getBytes())
                .build())
        .build());
```

### 自定义发件人名称

如果希望对方客户端显示"发件人名称 + 邮箱地址", 可以给 `MailAddress.personal` 赋值:

```java
MailSendRequest request = MailSendRequest.builder()
        .setFrom(MailAddress.builder()
                .setAddress("system@notice.dmit.io")
                .setPersonal("DMIT Inc.")
                .build())
        .addTo(MailAddress.builder()
                .setAddress("user@example.com")
                .build())
        .setSubject("订单确认信")
        .setTextBody("hello")
        .build();
```

常见客户端通常会显示为:

```text
DMIT Inc. <system@notice.dmit.io>
```

注意:

- `personal` 是显示名称, 不会改变真实发信邮箱地址.
- 最终展示样式会受收件方客户端影响, 但标准邮件头会正确携带这个名称.

### 收信

```java
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
```

### 可选快捷方式

如果你只是想少写一段服务器配置, 也可以使用预置层:

```java
MailProviderPreset provider = MailProviders.qq();

MailReader reader = MailReader.builder()
        .setImapServerConfig(provider.getImapServerConfig())
        .setCredential(credential)
        .build();
```

## Hotmail / Outlook.com 示例

### 服务器参数

- IMAP: `outlook.office365.com:993`, `SSL/TLS`
- SMTP: `smtp-mail.outlook.com:587`, `STARTTLS`
- 认证: `OAuth2 XOAUTH2`

### 构建收件端与发件端

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

MailReader reader = MailReader.builder()
        .setImapServerConfig(ImapServerConfig.builder()
                .setHost("outlook.office365.com")
                .setPort(993)
                .setSecurityMode(MailSecurityMode.SSL_TLS)
                .build())
        .setCredential(credential)
        .build();

MailSender sender = MailSender.builder()
        .setSmtpServerConfig(SmtpServerConfig.builder()
                .setHost("smtp-mail.outlook.com")
                .setPort(587)
                .setSecurityMode(MailSecurityMode.STARTTLS)
                .build())
        .setCredential(credential)
        .build();
```

### 可选快捷方式

如果你已经接受默认的微软参数, 也可以改成更短的预置写法:

```java
MailProviderPreset provider = MailProviders.hotmail();

OAuth2RefreshTokenCredential credential = provider.createOAuth2CredentialBuilder()
        .setUsername("your@hotmail.com")
        .setClientId("your-client-id")
        .setRefreshToken("your-refresh-token")
        .build();
```

### 读取最近邮件

```java
List<MailMessage> messages = reader.listMessages(MailQuery.builder()
        .setFolder(MailFolder.INBOX)
        .setLimit(20)
        .setIncludeBody(true)
        .setIncludeAttachments(true)
        .build());
```

### 发送邮件

```java
MailSendResult result = sender.send(MailSendRequest.builder()
        .setFrom(MailAddress.builder()
                .setAddress("your@hotmail.com")
                .setPersonal("Lava Mail Bot")
                .build())
        .addTo(MailAddress.builder().setAddress("friend@example.com").build())
        .setSubject("hello from lava-mail")
        .setTextBody("plain text body")
        .build());
```

## API 说明

### `MailSender`

- `send(MailSendRequest request)`: 发送邮件

### `MailReader`

- `listMessages(MailQuery query)`: 读取邮件列表

### `MailSendRequest`

- 支持 `textBody`, `htmlBody`, 附件
- `to/cc/bcc/replyTo` 均支持多地址

### `MailQuery`

- 支持 `folder`, `limit`, `unreadOnly`
- 支持 `receivedAfter`, `receivedBefore`
- 支持 `from`, `subjectContains`
- 支持 `includeBody`, `includeAttachments`

## 手工冒烟建议

建议至少做下面 4 步:

1. 用 `QQ邮箱` 给自己发送一封纯文本邮件.
2. 用 `QQ邮箱` 发送一封带 HTML 和附件的邮件, 再通过 `listMessages` 读回正文和附件内容.
3. 用 `Hotmail` 完成 `refresh token` 换取 access token 的收发闭环.
4. 分别验证未读筛选, 主题筛选, 收件箱读取, 附件字节内容.
