# 异常与安全

所有支付宝领域异常都继承 `AlipayException`。不同异常表示不同的失败边界，不能统一吞掉后当作“支付失败”。

## 异常分类

| 异常 | 含义 | 业务处理建议 |
| --- | --- | --- |
| `AlipayApiException` | 支付宝返回结构化 V3 API 错误 | 按错误码分类；未验签错误只用于诊断 |
| `AlipayTransportException` | DNS、连接、TLS、超时或非成功 HTTP 状态 | 将结果视为未知，查单或查退款 |
| `AlipaySecurityException` | 签名、应用、卖家或业务标识不匹配 | 拒绝结果并告警，不更新业务状态 |
| `AlipayProtocolException` | 响应结构、金额、日期或页面跳转数据不符合协议 | 拒绝结果，记录脱敏诊断信息 |
| `IllegalArgumentException` | 本地配置或请求参数不符合约束 | 修正调用代码，不发送请求 |

## 处理 V3 API 错误

```java
try {
    return client.transactions().queryByOutTradeNo(outTradeNo);
} catch (AlipayApiException exception) {
    log.warn(
            "支付宝 API 调用失败，statusCode={}, verified={}, code={}, traceId={}",
            exception.statusCode(),
            exception.verified(),
            exception.code(),
            exception.traceId()
    );
    throw exception;
}
```

支付宝允许错误响应不携带签名，因此 `verified=false` 的错误正文只能用于诊断和重试分类，不能据此把订单标记为关闭、失败或已退款。

## 安全失败分类

`AlipaySecurityException.failure()` 可区分：

- 缺失或重复签名元数据；
- 不支持的签名类型或字符集；
- RSA2 签名无效；
- 应用 ID、卖家 ID 或通知类型不匹配；
- 响应订单号、退款号或金额与可信值不一致。

安全异常表示信任边界校验没有通过，不应自动降级为“继续解析业务字段”。

## 日志红线

日志中不得记录：

- 应用私钥、密钥文件内容或密钥环境变量；
- 签名原文和完整 `Authorization`；
- 完整支付 GET URL 或自动提交表单；
- 完整异步通知参数；
- 完整支付宝响应正文；
- 账单临时下载地址中的令牌。

可以记录脱敏后的商户订单号、异常类型、结构化错误码、HTTP 状态码和支付宝 `traceId`。

## 支付业务安全清单

1. 私钥只存在于商户服务端的安全配置、密钥服务或 HSM；
2. 前台同步跳转不作为支付成功依据；
3. 异步通知先验签，再校验应用、卖家、订单号和金额；
4. 通知持久化成功后才返回 `success`；
5. 网络失败后先查单或查退款，不直接认定失败；
6. 未确认旧订单状态前不要求用户重复付款；
7. 退款请求号稳定且唯一，结果未知时不得更换；
8. 客户端不启用 HTTP 自动重试和重定向。

## 官方文档

- [OpenAPI V3 请求规则](https://opendocs.alipay.com/open-v3/054oog)
- [OpenAPI V3 请求签名](https://opendocs.alipay.com/open-v3/054q58)
- [OpenAPI V3 响应验签](https://opendocs.alipay.com/open-v3/054d0z)
- [统一收单下单并支付页面接口](https://opendocs.alipay.com/open-v3/2423fad5_alipay.trade.page.pay?scene=22&pathHash=1dc76737)
- [统一收单交易查询](https://opendocs.alipay.com/open-v3/e9ce4f59_alipay.trade.query?scene=23&pathHash=9b1a06e3)
- [统一收单交易关闭](https://opendocs.alipay.com/open-v3/429ffb46_alipay.trade.close?scene=common&pathHash=4d948bd6)
- [统一收单交易退款](https://opendocs.alipay.com/open-v3/01073208_alipay.trade.refund?scene=common&pathHash=dff16ab4)
- [统一收单交易退款查询](https://opendocs.alipay.com/open-v3/46bff59c_alipay.trade.fastpay.refund.query?scene=common&pathHash=3901bb82)
- [退款冲退完成通知](https://opendocs.alipay.com/open-v3/42a9ce75_alipay.trade.refund.depositback.completed?scene=common&pathHash=9c33d734)
- [查询对账单下载地址](https://opendocs.alipay.com/open-v3/d6c4d425_alipay.data.dataservice.bill.downloadurl.query?scene=common&pathHash=b88e9ae1)
