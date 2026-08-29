/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.pay.wechat.exception;

/**
 * 微信支付消息真实性、时效性、机密性或业务响应一致性校验失败的稳定分类。
 */
public enum WechatPaySecurityFailure {
    /**
     * 消息缺少必要的签名请求头。
     */
    MISSING_SIGNATURE_HEADER,
    /**
     * 消息携带重复的签名请求头，无法确定唯一验签原文。
     */
    DUPLICATE_SIGNATURE_HEADER,
    /**
     * 消息声明的微信支付公钥 ID 与配置不一致。
     */
    UNEXPECTED_PUBLIC_KEY_ID,
    /**
     * 消息使用了当前实现不支持的签名类型。
     */
    UNSUPPORTED_SIGNATURE_TYPE,
    /**
     * 签名时间戳不是有效整数。
     */
    INVALID_TIMESTAMP,
    /**
     * 签名时间戳超过允许的时间偏差。
     */
    EXPIRED_TIMESTAMP,
    /**
     * RSA 签名无效。
     */
    INVALID_SIGNATURE,
    /**
     * 通知资源使用了不支持的加密算法。
     */
    UNSUPPORTED_ENCRYPTION_ALGORITHM,
    /**
     * 通知资源无法通过 AES-GCM 认证或解密。
     */
    DECRYPTION_FAILED,
    /**
     * 响应或解密资源中的商户号与客户端配置不一致。
     */
    MERCHANT_MISMATCH,
    /**
     * 已验签响应中的业务标识或关键金额与当前请求不一致。
     */
    RESPONSE_MISMATCH,
    /**
     * 下载账单的摘要与申请结果不一致。
     */
    HASH_MISMATCH
}
