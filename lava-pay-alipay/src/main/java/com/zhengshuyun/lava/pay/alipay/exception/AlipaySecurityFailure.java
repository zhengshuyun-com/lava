/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.exception;

/**
 * 支付宝签名与业务一致性校验失败分类。
 */
public enum AlipaySecurityFailure {
    /** 响应或通知未携带签名。 */
    MISSING_SIGNATURE,
    /** V3 响应携带重复的签名元数据头。 */
    DUPLICATE_SIGNATURE_HEADER,
    /** 签名类型不是 RSA2。 */
    UNSUPPORTED_SIGNATURE_TYPE,
    /** 通知声明的字符集不受支持。 */
    UNSUPPORTED_CHARSET,
    /** RSA2 签名无效。 */
    INVALID_SIGNATURE,
    /** 通知 APPID 与当前客户端不一致。 */
    APPLICATION_MISMATCH,
    /** 通知卖家账号与当前客户端不一致。 */
    SELLER_MISMATCH,
    /** 通知类型与解析入口不一致。 */
    NOTIFICATION_TYPE_MISMATCH,
    /** 响应或通知业务标识、金额与可信请求不一致。 */
    RESPONSE_MISMATCH
}
