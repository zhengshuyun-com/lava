# 异常与安全

公开异常统一继承 `WechatPayException`。

## 异常分类

| 异常 | 含义 | 处理建议 |
| --- | --- | --- |
| `WechatPayApiException` | 微信支付返回非成功 HTTP 状态和结构化错误 | 按状态码、错误码和 Request-ID 分类 |
| `WechatPayTransportException` | DNS、连接、TLS、超时等传输失败 | 结果视为未知，查单或查退款 |
| `WechatPaySecurityException` | 签名、公钥 ID、时间戳、解密、标识或摘要校验失败 | 拒绝结果并告警 |
| `WechatPayProtocolException` | 响应不符合 APIv3 结构 | 拒绝结果，记录脱敏诊断 |
| `WechatPayFileException` | 账单目标冲突或文件系统失败 | 按失败类型处理本地文件 |

## 安全失败

`WechatPaySecurityException` 可区分：

- 缺失或重复签名头；
- 公钥 ID 不符合当前配置；
- 签名类型不支持；
- 时间戳无效或过期；
- RSA 签名无效；
- 通知算法不支持或 AES-GCM 解密失败；
- 商户号不匹配；
- 订单、退款标识或金额不匹配；
- 账单摘要不匹配。

## 日志红线

不得记录：

- 商户 API 私钥；
- APIv3 密钥；
- 请求签名原文和完整 `Authorization`；
- 完整通知正文、密文和签名；
- 账单临时下载 URL 和 token；
- API 错误详情中的原始敏感 `value`。

可以记录脱敏订单号、异常类型、错误码、HTTP 状态、Request-ID 和失败分类。

## 上线检查

1. APPID 已与当前商户号绑定；
2. 私钥与商户 API 证书匹配；
3. 微信支付公钥 ID 与公钥文件属于同一实例；
4. APIv3 密钥为商户平台配置的 32 位密钥；
5. 支付和退款通知地址可由公网 HTTPS 访问；
6. 通知验签、业务核对和幂等持久化完成后才返回成功；
7. 传输异常后先查单或查退款，不自动重放；
8. 账单下载使用临时文件并完成摘要校验；
9. 平台证书迁移灰度已经完成，或仍保留兼容处理链路；
10. 业务日志已经验证不会泄露密钥、签名、通知和下载 token。

## 官方文档

- [Native 支付开发指引](https://pay.weixin.qq.com/doc/v3/merchant/4012791891)
- [APIv3 如何签名和验签](https://pay.weixin.qq.com/doc/v3/merchant/4012365342)
- [从平台证书切换成微信支付公钥](https://pay.weixin.qq.com/doc/v3/merchant/4012154180)
