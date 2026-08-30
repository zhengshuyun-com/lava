# 请求体与 URL

## 请求体

```java
HttpBody bytes = HttpBodyUtils.bytes(payload, "application/octet-stream");
HttpBody text = HttpBodyUtils.text("内容");
HttpBody json = HttpBodyUtils.json(value);
HttpBody form = HttpBodyUtils.form(Map.of("name", "Ada"));
HttpBody file = HttpBodyUtils.file(path, "application/pdf");
```

流式上传借用输入流，调用方负责关闭：

```java
HttpBody body = HttpBodyUtils.stream(
        input,
        contentLength,
        "application/octet-stream"
);

HttpRequest request = HttpRequest.post(url)
        .body(body)
        .build();
```

## Multipart

```java
HttpRequest.MultipartBuilder multipart = HttpRequest.MultipartBuilder.builder()
        .addFormField("description", "报告")
        .addFile("file", path, "application/pdf");

HttpRequest request = HttpRequest.post("/v1/files")
        .multipartBody(multipart)
        .build();
```

## URL 构建

```java
URI url = HttpUrlBuilder.from(channelBaseUrl)
        .appendPath("/v1/chat/completions")
        .queryParam("model", model)
        .build();
```

`appendPath(...)` 追加路径，不会因为参数以 `/` 开头就丢弃基础路径前缀。`from(...)` 会保留原地址已有路径、查询参数和片段。

已经拥有编码完成的原始查询串时：

```java
URI url = HttpUrlBuilder.from(channelBaseUrl)
        .appendPath("/v1/chat/completions")
        .encodedQuery(rawQuery)
        .build();
```

带 `encoded` 的方法要求调用方已经完成正确百分号编码，不能与接收普通文本的方法混用，否则可能重复编码。

::: warning 外部路径输入
点段、编码点段和反斜杠会按 HTTP URL 规则归一化。不要把未经校验的外部输入直接用作可信路径前缀；参与签名的 URL 还必须确认规范化结果符合目标协议。
:::
