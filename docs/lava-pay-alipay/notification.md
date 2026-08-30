# 异步通知

支付宝支付通知和银行卡冲退通知是 URL 编码表单，不使用 REST V3 响应头验签。Web 框架完成一次表单 URL 解码后，把全部参数交给 `NotificationParser`。

## 支付通知处理顺序

```java
TradeNotification notification = client.notifications().parseTrade(formParameters);

Order order = orderRepository.findByOutTradeNo(notification.outTradeNo());

notification.requireOrder(
        order.outTradeNo(),
        order.totalAmount()
);

if (notification.paid()) {
    orderService.markPaidIdempotently(
            notification.notifyId(),
            notification.tradeNo(),
            notification.paymentTime()
    );
}

return NotificationParser.SUCCESS;
```

稳定流程是：

1. 先用支付宝公钥执行 RSA2 验签；
2. 校验 `app_id`、`seller_id` 和通知类型；
3. 根据商户订单号加载后端可信订单；
4. 使用 `requireOrder(...)` 核对订单号和金额；
5. 按 `notifyId` 或商户订单号幂等持久化；
6. 事务可靠提交后才返回 `success`。

解析器已经完成前两步。`seller_id` 是当前客户端配置的卖家支付宝用户 ID，用来拒绝发给其他收款方的通知；当前模块不使用可能变化的卖家邮箱代替它。

## 支付成功状态

只有以下状态会使 `notification.paid()` 返回 `true`：

- `TRADE_SUCCESS`：支付成功，交易仍可能退款；
- `TRADE_FINISHED`：交易结束，不可退款。

其他状态不能当作已支付。

## 失败响应

验签、业务核对或持久化任一步失败时返回：

```java
return NotificationParser.FAILURE;
```

即字符串 `fail`，让支付宝按通知策略重试。不要在数据库事务提交前返回 `success`，否则本地失败后支付宝可能不再重试。

## 银行卡冲退完成通知

退款涉及银行卡原路退回时，可处理冲退完成通知：

```java
RefundDepositBackNotification notification = client.notifications()
        .parseRefundDepositBack(formParameters);

Refund refund = refundRepository.findByOutRequestNo(
        notification.outRequestNo()
);

notification.requireRefund(
        refund.outTradeNo(),
        refund.outRequestNo(),
        refund.amount()
);

if (notification.bankDepositSucceeded()) {
    refundService.markBankDepositSucceededIdempotently(
            notification.notifyId(),
            notification.outRequestNo()
    );
}

return NotificationParser.SUCCESS;
```

退款冲退通知会校验签名、应用 ID、通知方法、最终状态和成功金额。调用方仍需使用后端可信退款记录核对订单号、退款请求号与金额。

::: danger 参数处理
传入的是 Web 框架已经完成一次 URL 解码的表单参数。不要再次 URL 解码，也不要记录完整通知参数；其中可能包含买家信息和业务回传数据。
:::
