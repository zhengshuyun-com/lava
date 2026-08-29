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

package com.zhengshuyun.lava.pay.wechat.nativepay;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayTransport;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.time.OffsetDateTime;

/**
 * 微信支付 APIv3 普通商户 Native 支付入口。
 */
public final class NativePayClient {
    private static final String PREPAY_PATH = "/v3/pay/transactions/native";

    private final WechatPayTransport transport;
    private final String appid;
    private final URI notifyUrl;
    private final Runnable openCheck;

    /**
     * 由应用上下文创建 Native 支付入口。
     *
     * @param transport 共享协议传输层
     * @param appid 应用 ID
     * @param notifyUrl 固定支付通知地址
     * @param openCheck 根客户端存活检查
     */
    public NativePayClient(WechatPayTransport transport, String appid, URI notifyUrl,
                           Runnable openCheck) {
        this.transport = ValidationUtils.requireNonNull(transport, "transport");
        this.appid = ValidationUtils.requireNotBlank(appid, "appid");
        this.notifyUrl = ValidationUtils.requireNonNull(notifyUrl, "notifyUrl");
        this.openCheck = ValidationUtils.requireNonNull(openCheck, "openCheck");
    }

    /**
     * 创建 Native 预支付订单并返回二维码链接。
     *
     * @param request 下单业务参数
     * @return 已验签的下单结果
     * @throws com.zhengshuyun.lava.pay.wechat.WechatPayException 协议调用失败
     */
    public NativePrepayResponse prepay(NativePrepayRequest request) {
        openCheck.run();
        ValidationUtils.requireNonNull(request, "request must not be null");
        PrepayPayload payload = new PrepayPayload(appid, transport.mchid(),
                request.description(), request.outTradeNo(), request.timeExpire(),
                request.attach(), notifyUrl.toASCIIString(), request.goodsTag(),
                request.supportFapiao(), request.amount(), request.detail(),
                request.sceneInfo(), request.settleInfo());
        return transport.post(transport.endpoint(PREPAY_PATH), payload,
                NativePrepayResponse.class);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record PrepayPayload(
            @JsonProperty("appid") String appid,
            @JsonProperty("mchid") String mchid,
            @JsonProperty("description") String description,
            @JsonProperty("out_trade_no") String outTradeNo,
            @JsonProperty("time_expire") @Nullable OffsetDateTime timeExpire,
            @JsonProperty("attach") @Nullable String attach,
            @JsonProperty("notify_url") String notifyUrl,
            @JsonProperty("goods_tag") @Nullable String goodsTag,
            @JsonProperty("support_fapiao") @Nullable Boolean supportFapiao,
            @JsonProperty("amount") NativePrepayRequest.Amount amount,
            @JsonProperty("detail") NativePrepayRequest.@Nullable Detail detail,
            @JsonProperty("scene_info") NativePrepayRequest.@Nullable SceneInfo sceneInfo,
            @JsonProperty("settle_info") NativePrepayRequest.@Nullable SettleInfo settleInfo) {
    }
}
