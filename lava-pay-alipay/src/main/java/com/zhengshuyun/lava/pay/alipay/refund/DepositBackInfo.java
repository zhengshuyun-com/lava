/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.refund;

import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 银行卡冲退信息。
 *
 * @param hasDepositBack        是否存在银行卡冲退
 * @param status                冲退状态；没有时为 {@code null}
 * @param amount                冲退金额，单位为分；没有时为 {@code null}
 * @param bankAckTime           银行响应时间；没有时为 {@code null}
 * @param estimatedReceiptTime  预计银行入账时间；没有时为 {@code null}
 */
public record DepositBackInfo(
        boolean hasDepositBack,
        @Nullable String status,
        @Nullable Long amount,
        @Nullable LocalDateTime bankAckTime,
        @Nullable LocalDateTime estimatedReceiptTime
) {
}
