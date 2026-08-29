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

package com.zhengshuyun.lava.pay.wechat.refund;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayTransport;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayValidationUtils;

/**
 * 普通支付退款申请与查询客户端。
 */
public final class RefundClient {
    private static final String REFUND_PATH = "/v3/refund/domestic/refunds";

    private final WechatPayTransport transport;
    private final Runnable openCheck;

    /**
     * 由根客户端创建退款入口。
     *
     * @param transport 共享协议传输层
     * @param openCheck 根客户端存活检查
     */
    public RefundClient(WechatPayTransport transport, Runnable openCheck) {
        this.transport = ValidationUtils.requireNonNull(transport, "transport");
        this.openCheck = ValidationUtils.requireNonNull(openCheck, "openCheck");
    }

    /**
     * 申请全部或部分退款。成功仅表示微信支付已受理，最终结果应结合通知或查询确认。
     *
     * @param request 退款申请
     * @return 已验签退款单
     */
    public Refund apply(RefundRequest request) {
        openCheck.run();
        return transport.post(transport.endpoint(REFUND_PATH),
                ValidationUtils.requireNonNull(request, "request must not be null"),
                Refund.class);
    }

    /**
     * 使用商户退款单号查询单笔退款。
     *
     * @param outRefundNo 商户退款单号
     * @return 已验签退款单
     */
    public Refund queryByOutRefundNo(String outRefundNo) {
        openCheck.run();
        outRefundNo = WechatPayValidationUtils.requireOutRefundNo(outRefundNo);
        return transport.get(transport.endpoint(REFUND_PATH, outRefundNo, ""),
                Refund.class);
    }
}
