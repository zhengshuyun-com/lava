# lava-pay-alipay

`lava-pay-alipay` 用于实现支付宝开放平台支付协议，不依赖支付宝官方 SDK。模块基于 Lava 的 HTTP 和 JSON 能力，使用 JDK 提供的
RSA 签名、验签与密钥处理 API 完成协议交互。

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-pay-alipay</artifactId>
</dependency>
```

版本建议由 [`lava-bom`](../lava-bom/README.md) 管理。

本模块只负责支付宝支付协议适配，不负责业务支付订单、幂等、渠道路由、回调事件持久化、补偿任务和对账差异处理。
