# lava-pay-alipay

`lava-pay-alipay` 是面向支付宝 OpenAPI 自研普通商户的同步 Java 工具包，不依赖支付宝官方 SDK。模块基于 Lava HTTP、JSON、
Crypto 与 JDK RSA 能力，完成 RSA2 请求签名、响应及通知验签、电脑网站支付表单生成、交易查询与关闭、退款、退款查询、
银行卡冲退通知解析和账单下载地址查询。

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-pay-alipay</artifactId>
</dependency>
```

版本建议由 [`lava-bom`](../lava-bom/README.md) 管理。

## 接入边界

当前版本支持：

- 自研普通商户、RSA2 公钥模式；
- `alipay.trade.page.pay` 电脑网站支付 POST 表单；
- `alipay.trade.query` 查单和 `alipay.trade.close` 关单；
- `alipay.trade.refund` 退款和 `alipay.trade.fastpay.refund.query` 退款查询；
- 支付结果通知和 `alipay.trade.refund.depositback.completed` 银行卡冲退通知；
- `alipay.data.dataservice.bill.downloadurl.query` 普通商户日/月账单下载地址查询。

当前不包含服务商代调用、公钥证书模式、直付通、分账、花呗分期、指定买家、开票、账单文件下载及解析。模块只负责协议适配，
不负责业务支付订单、业务幂等、渠道路由、通知持久化、轮询补偿和对账差异处理。

## 创建客户端

调用方需要准备应用 ID、卖家支付宝用户 ID、PKCS#8 应用私钥和支付宝公钥。Java 快速沙箱配置必须使用返回的
`appPrivateKey` 字段，不能改用 PKCS#1 `appPrivatePkcsKey`，也不能自行添加 PEM 头尾或转换格式。

```java
import com.zhengshuyun.lava.pay.alipay.AlipayPayClient;

import java.nio.file.Path;

AlipayPayClient client = AlipayPayClient.builder()
        .appId(System.getenv("ALIPAY_APP_ID"))
        .sellerId(System.getenv("ALIPAY_SELLER_ID"))
        .appPrivateKey(Path.of("/secure/alipay_app_private_key.pem"))
        .alipayPublicKey(Path.of("/secure/alipay_public_key.pem"))
        .build();
```

客户端线程安全，应作为长生命周期对象复用并在应用停止时关闭。默认 HTTP 客户端由它拥有；通过 `.httpClient(...)` 借入的
客户端不会随支付宝客户端关闭。借入客户端应关闭自动重试、HTTP 重定向和跨协议重定向，让支付调用保持显式失败语义。

沙箱联调时显式切换网关：

```java
.gatewayUrl(AlipayPayClient.SANDBOX_GATEWAY_URL)
```

## 电脑网站支付

页面支付不会向支付宝发送普通服务端 API 请求，而是在商户服务端生成完整的签名 HTML 表单：

```java
import com.zhengshuyun.lava.pay.alipay.pagepay.PagePayForm;
import com.zhengshuyun.lava.pay.alipay.pagepay.PagePayRequest;

var pagePay = client.pagePay(
        "https://pay.example.com/alipay/notify",
        "https://pay.example.com/alipay/return");

PagePayForm form = pagePay.createForm(PagePayRequest.builder()
        .outTradeNo("ORDER_001")
        .totalAmount(100) // 单位：分
        .subject("订单 ORDER_001")
        .build());

httpResponse.setContentType(PagePayForm.CONTENT_TYPE);
httpResponse.getWriter().write(form.html());
```

必须把 `form.html()` 作为 HTML 页面渲染并自动提交，不能将其赋给 `window.location.href`。`product_code` 固定为
`FAST_INSTANT_TRADE_PAY`，`integration_type` 固定为 `PCWEB`；应用 ID、`notify_url` 和 `return_url` 均由客户端参与签名并注入。

前台同步返回只用于页面展示，不代表支付成功。`return_url` 处理逻辑应主动调用查单接口确认结果。

## 支付通知

Web 层把完成一次表单 URL 解码后的全部参数整理为 `Map<String, String>`，再交给框架无关解析器：

```java
var notification = client.notifications().parseTrade(formParameters);

// 可信值必须来自商户后端订单记录，不能来自前端或当前通知。
notification.requireOrder(trustedOutTradeNo, trustedAmountInCents);
if (notification.paid()) {
    // 按 notifyId 或业务单号幂等更新本地订单。
}

// 只有验签、业务匹配和可靠持久化全部成功后才返回 success。
return NotificationParser.SUCCESS;
```

解析器固定执行 RSA2 验签，并核对 `app_id`、`seller_id` 和通知类型；`requireOrder(...)` 再核对商户订单号与金额。只有
`TRADE_SUCCESS` 或 `TRADE_FINISHED` 表示买家付款成功。处理失败应返回 `fail`，让支付宝按策略重试。

## 查单与关单

```java
var trade = client.transactions().queryByOutTradeNo("ORDER_001");
if (trade.paid()) {
    // 已支付
}

// 仅关闭仍处于 WAIT_BUYER_PAY 的交易。
client.transactions().closeByOutTradeNo("ORDER_001");
```

未收到异步通知、支付请求结果未知或准备更换商户订单号重新支付前，必须先查单。前一订单仍为
`WAIT_BUYER_PAY` 时应先关单；已经支付时不得要求用户重复付款。

## 退款与退款查询

```java
import com.zhengshuyun.lava.pay.alipay.refund.RefundQueryRequest;
import com.zhengshuyun.lava.pay.alipay.refund.RefundRequest;

var refund = client.refunds().apply(RefundRequest.builder()
        .outTradeNo("ORDER_001")
        .outRequestNo("REFUND_001")
        .refundAmount(50) // 单位：分
        .reason("用户取消")
        .build());

if (!refund.succeeded()) {
    // code=10000 只表示退款请求调用成功；fund_change 不是 Y 时继续查询。
    var latest = client.refunds().query(RefundQueryRequest.builder()
            .outTradeNo("ORDER_001")
            .outRequestNo("REFUND_001")
            .build());
    boolean refunded = latest.succeeded(); // 仅 REFUND_SUCCESS 为 true
}
```

每笔退款必须使用稳定且唯一的 `outRequestNo`。网络超时或系统错误后，使用相同退款请求号和金额重试或查询，不能更换请求号。
退款默认请求 `deposit_back_info`，以便银行卡原路退款场景接收冲退完成通知：

```java
var depositBack = client.notifications()
        .parseRefundDepositBack(formParameters);

// 按 notifyId 和 outRequestNo 幂等处理，成功后返回 success。
```

## 账单

```java
import com.zhengshuyun.lava.pay.alipay.bill.BillType;

var info = client.bills().queryDaily(
        BillType.TRADE, java.time.LocalDate.now().minusDays(1));
var downloadUrl = info.downloadUrl();
```

模块只返回已验签下载地址。下载地址有效期很短，调用方应立即下载，并自行完成文件存储、解压、字段解析和对账差异处理。

## 失败语义与安全

- `AlipayPayApiException`：已验签的支付宝业务失败，包含 `code`、`subCode` 和可选 `traceId`；
- `AlipayPayTransportException`：DNS、连接、TLS、超时或非成功 HTTP 状态，结果可能未知；
- `AlipayPaySecurityException`：签名、应用、卖家、订单号或金额不匹配；
- `AlipayPayProtocolException`：响应结构、金额精度或日期格式不符合协议。

模块不会自动重试、轮询或将未知结果判为失败。私钥、签名原文、完整响应正文和账单下载令牌不会进入默认异常文本；业务日志仍不得
记录私钥、完整通知参数或支付表单。

## 官方文档

- [电脑网站支付接入目录](https://ideservice.alipay.com/cms/site/0iztfv)
- [统一收单下单并支付页面接口](https://ideservice.alipay.com/cms/site/0izblh)
- [电脑网站支付异步通知](https://ideservice.alipay.com/cms/site/0izbju)
- [统一收单交易查询](https://ideservice.alipay.com/cms/site/0izam7)
- [统一收单交易退款](https://ideservice.alipay.com/cms/site/0izblj)
- [统一收单退款查询](https://ideservice.alipay.com/cms/site/0izam8)
