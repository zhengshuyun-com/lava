# 快速开始

## 添加依赖

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-pay-wechat</artifactId>
</dependency>
```

版本推荐由 [lava-bom](../lava-bom/) 管理。

## 准备配置

| 配置 | 用途 |
| --- | --- |
| 商户号 `mchid` | 标识境内普通商户 |
| APPID | 创建支付应用上下文，必须与商户号绑定 |
| 商户 API 私钥 | 生成 APIv3 请求签名 |
| 商户 API 证书或序列号 | 写入请求签名凭据标识 |
| APIv3 密钥 | 解密支付和退款回调资源 |
| 微信支付公钥 ID | 指定应答和回调验签公钥 |
| 微信支付公钥 | 验证微信支付签名 |
| 支付通知地址 | 外网可访问、无查询参数的 HTTPS 地址 |

## 创建根客户端

```java
WechatPayClient client = WechatPayClient.builder()
        .mchid(System.getenv("WECHAT_PAY_MCHID"))
        .merchantPrivateKey(Path.of(
                System.getenv("WECHAT_PAY_PRIVATE_KEY_PATH")
        ))
        .merchantCertificate(Path.of(
                System.getenv("WECHAT_PAY_CERTIFICATE_PATH")
        ))
        .apiV3Key(System.getenv("WECHAT_PAY_API_V3_KEY"))
        .wechatPayPublicKeyId(System.getenv(
                "WECHAT_PAY_PUBLIC_KEY_ID"
        ))
        .wechatPayPublicKey(Path.of(
                System.getenv("WECHAT_PAY_PUBLIC_KEY_PATH")
        ))
        .build();
```

商户证书用于自动提取序列号并检查私钥是否配对。不便加载证书时可改用 `.merchantSerialNo(...)`。JCA `PrivateKey` / `PublicKey` 重载便于接入 HSM 或云密钥服务。

根客户端线程安全，应长期复用，并在应用停止时调用 `close()`。

## 创建应用上下文

一个商户号可以绑定多个 APPID。支付产品通过轻量应用上下文固定 APPID 和通知地址，共享根客户端连接池和商户凭据：

```java
WechatPayApplication application = client.application(
        appid,
        "https://pay.example.com/wechat/transaction-notify"
);
```

## 自定义 HTTP 客户端

不配置时，根客户端创建并管理专属 HTTP 客户端。传入 `.httpClient(...)` 时属于借用资源，关闭微信支付客户端不会关闭它。

借入客户端必须关闭：

- 连接失败自动重试；
- 普通 HTTP 重定向；
- HTTP/HTTPS 跨协议重定向。

构建器会拒绝不安全配置。支付请求出现传输失败时结果可能未知，不能让底层自动重放。

## 主备域名

模块默认使用 `https://api.mch.weixin.qq.com/`，不会自动切换备域名。需要备域名时显式配置：

```java
.apiBaseUrl(WechatPayClient.BACKUP_API_BASE_URL)
```

生产 API 根地址只允许微信支付官方主、备域名；自定义地址仅允许环回主机用于本地协议测试。
