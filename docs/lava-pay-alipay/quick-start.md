# 快速开始

## 环境与依赖

模块要求 JDK 25 或更高版本。推荐通过 `lava-bom` 管理版本：

```xml
<properties>
    <lava.version>x.y.z</lava.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.zhengshuyun</groupId>
            <artifactId>lava-bom</artifactId>
            <version>${lava.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.zhengshuyun</groupId>
        <artifactId>lava-pay-alipay</artifactId>
    </dependency>
</dependencies>
```

## 准备配置

普通商户公钥模式需要四项配置：

| 配置 | 用途 | 约束 |
| --- | --- | --- |
| 应用 ID | 标识支付宝开放平台应用 | 生产与沙箱不能混用 |
| 卖家支付宝用户 ID | 校验异步通知确实属于当前收款方 | 以 `2088` 开头的 16 位数字，不使用邮箱替代 |
| 应用私钥 | 商户请求签名 | Java 使用 PKCS#8，至少 2048 位 RSA |
| 支付宝公钥 | 验证支付宝响应和通知 | X.509 RSA 公钥，不是应用公钥 |

Java 快速沙箱配置应使用支付宝返回的 `appPrivateKey`，不要改用 PKCS#1 的 `appPrivatePkcsKey`，也不要自行添加 PEM 头尾或转换格式。

## 创建客户端

```java
AlipayClient client = AlipayClient.builder()
        .appId(System.getenv("ALIPAY_APP_ID"))
        .sellerId(System.getenv("ALIPAY_SELLER_ID"))
        .appPrivateKey(Path.of(System.getenv("ALIPAY_APP_PRIVATE_KEY_PATH")))
        .alipayPublicKey(Path.of(System.getenv("ALIPAY_PUBLIC_KEY_PATH")))
        .build();
```

`AlipayClient` 应作为单例长期复用，并在应用停止时调用 `close()`。关闭是幂等的；关闭后，之前取得的业务子客户端也不能继续调用。

## 沙箱环境

沙箱联调时显式切换基础地址：

```java
AlipayClient client = AlipayClient.builder()
        .appId(System.getenv("ALIPAY_APP_ID"))
        .sellerId(System.getenv("ALIPAY_SELLER_ID"))
        .appPrivateKey(Path.of(System.getenv("ALIPAY_APP_PRIVATE_KEY_PATH")))
        .alipayPublicKey(Path.of(System.getenv("ALIPAY_PUBLIC_KEY_PATH")))
        .baseUrl(AlipayClient.SANDBOX_BASE_URL)
        .build();
```

应用 ID、应用私钥、支付宝公钥和卖家 ID 必须属于同一个沙箱应用。沙箱 AppID 请求生产网关，或生产 AppID 请求沙箱网关，都会得到“无效的 AppID 参数”一类错误。

## 自定义 HTTP 客户端

不配置时，模块创建并管理内部 HTTP 客户端。传入自定义客户端时，资源生命周期仍由调用方负责，并且必须关闭以下行为：

- 连接失败自动重试；
- HTTP 重定向；
- HTTP/HTTPS 跨协议重定向。

构建器会拒绝不安全的配置，防止支付请求被隐式重放或发送到不同目标。自定义拦截器属于调用方可信边界，不得记录私钥、签名原文、完整请求正文或敏感响应。

## 下一步

1. 使用 [电脑网站支付](./page-pay) 创建支付请求；
2. 按 [异步通知](./notification) 验签并幂等更新订单；
3. 在结果未知时使用 [查单与关单](./transaction) 确认最终状态。
