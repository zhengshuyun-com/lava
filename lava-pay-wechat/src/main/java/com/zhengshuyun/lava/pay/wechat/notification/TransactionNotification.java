/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.wechat.notification;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.wechat.transaction.Transaction;

import java.time.OffsetDateTime;

/**
 * 已验签并解密的支付成功通知。
 *
 * @param id 通知唯一编号，业务侧可用于辅助幂等
 * @param createTime 通知创建时间
 * @param eventType 固定为 TRANSACTION.SUCCESS
 * @param summary 通知摘要
 * @param transaction 支付成功交易
 */
public record TransactionNotification(
        String id,
        OffsetDateTime createTime,
        String eventType,
        String summary,
        Transaction transaction) {
    /**
     * 校验通知必填字段。
     */
    public TransactionNotification {
        ValidationUtils.requireNotBlank(id, "id must not be blank");
        ValidationUtils.requireNonNull(createTime, "createTime must not be null");
        ValidationUtils.requireNotBlank(eventType, "eventType must not be blank");
        ValidationUtils.requireNonNull(summary, "summary must not be null");
        ValidationUtils.requireNonNull(transaction, "transaction must not be null");
    }
}
