# 邮件附件

`MailAttachment` 用于发送和读取邮件附件. 当前版本读取附件时会把附件内容读入内存中的 `byte[]`.

## 发送附件

```java
MailSendRequest request = MailSendRequest.builder()
        .setFrom(MailAddress.builder().setAddress("your@qq.com").build())
        .addTo(MailAddress.builder().setAddress("friend@example.com").build())
        .setSubject("hello from lava-mail")
        .setTextBody("plain text body")
        .addAttachment(MailAttachment.builder()
                .setFileName("hello.txt")
                .setContentType("text/plain")
                .setContent("hello".getBytes())
                .build())
        .build();
```

## 读取附件

```java
List<MailMessage> messages = reader.listMessages(MailQuery.builder()
        .setFolder(MailFolder.INBOX)
        .setLimit(10)
        .setIncludeAttachments(true)
        .build());
```

- 收信查询默认不解析附件.
- 只有显式设置 `includeAttachments=true` 时, 才会抓取附件内容.

## 常见坑与排查建议

| 异常/消息    | 原因                   | 解决方式                       |
|--------------|------------------------|--------------------------------|
| 附件列表为空 | 查询时未启用附件解析   | 设置 `includeAttachments=true` |
| 内存占用高   | 附件内容读入 `byte[]`  | 控制 `limit` 和附件大小        |
| 附件类型不对 | `contentType` 设置错误 | 按实际文件类型设置 MIME        |
