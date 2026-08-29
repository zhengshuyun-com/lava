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

package com.zhengshuyun.lava.pay.wechat.transaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.wechat.exception.WechatPaySecurityException;
import com.zhengshuyun.lava.pay.wechat.exception.WechatPaySecurityFailure;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayRuntime;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayTransport;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayValidationUtils;
import org.jspecify.annotations.Nullable;

import java.net.URI;

/**
 * 普通支付交易查单和关单客户端。
 */
public final class TransactionClient {
    private static final String OUT_TRADE_NO_PREFIX = "/v3/pay/transactions/out-trade-no";
    private static final String TRANSACTION_ID_PREFIX = "/v3/pay/transactions/id";

    private final WechatPayRuntime runtime;

    /**
     * 由根客户端创建交易入口。
     *
     * @param runtime 共享运行时
     */
    public TransactionClient(WechatPayRuntime runtime) {
        this.runtime = ValidationUtils.requireNonNull(runtime, "runtime");
    }

    /**
     * 使用商户订单号查询订单。未支付订单只能使用此方式查询。
     *
     * @param outTradeNo 商户订单号
     * @return 已验签交易状态
     */
    public Transaction queryByOutTradeNo(String outTradeNo) {
        WechatPayTransport transport = runtime.transport();
        outTradeNo = WechatPayValidationUtils.requireOutTradeNo(outTradeNo);
        URI uri = transport.endpoint(OUT_TRADE_NO_PREFIX, outTradeNo, "");
        Transaction transaction = transport.get(
                transport.query(uri, "mchid", transport.mchid()), Transaction.class);
        requireMchid(transport, transaction);
        requireIdentifier(outTradeNo, transaction.outTradeNo());
        return transaction;
    }

    /**
     * 使用微信支付订单号查询已支付订单。
     *
     * @param transactionId 微信支付订单号
     * @return 已验签交易状态
     */
    public Transaction queryByTransactionId(String transactionId) {
        WechatPayTransport transport = runtime.transport();
        transactionId = WechatPayValidationUtils.requireId(
                transactionId, "transactionId", 32);
        URI uri = transport.endpoint(TRANSACTION_ID_PREFIX, transactionId, "");
        Transaction transaction = transport.get(
                transport.query(uri, "mchid", transport.mchid()), Transaction.class);
        requireMchid(transport, transaction);
        requireIdentifier(transactionId, transaction.transactionId());
        return transaction;
    }

    /**
     * 关闭仍处于未支付状态的订单。
     *
     * @param outTradeNo 商户订单号
     */
    public void close(String outTradeNo) {
        WechatPayTransport transport = runtime.transport();
        outTradeNo = WechatPayValidationUtils.requireOutTradeNo(outTradeNo);
        URI uri = transport.endpoint(OUT_TRADE_NO_PREFIX, outTradeNo, "close");
        transport.postNoContent(uri, new ClosePayload(transport.mchid()));
    }

    private static void requireMchid(WechatPayTransport transport,
                                     Transaction transaction) {
        if (!transport.mchid().equals(transaction.mchid())) {
            throw new WechatPaySecurityException(WechatPaySecurityFailure.MERCHANT_MISMATCH);
        }
    }

    private static void requireIdentifier(String expected, @Nullable String actual) {
        if (!expected.equals(actual)) {
            throw new WechatPaySecurityException(WechatPaySecurityFailure.RESPONSE_MISMATCH);
        }
    }

    private record ClosePayload(@JsonProperty("mchid") String mchid) {
    }
}
