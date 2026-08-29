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

package com.zhengshuyun.lava.pay.wechat;

import com.zhengshuyun.lava.pay.wechat.internal.WechatPayTransport;
import com.zhengshuyun.lava.pay.wechat.nativepay.NativePayClient;

import java.net.URI;

/**
 * 绑定一个 APPID 与支付通知地址的轻量微信支付应用上下文。
 */
public final class WechatPayApplication {
    private final String appid;
    private final URI notifyUrl;
    private final NativePayClient nativePayClient;

    WechatPayApplication(WechatPayTransport transport, String appid, URI notifyUrl,
                         Runnable openCheck) {
        this.appid = appid;
        this.notifyUrl = notifyUrl;
        nativePayClient = new NativePayClient(transport, appid, notifyUrl, openCheck);
    }

    /**
     * 返回当前应用 ID。
     *
     * @return APPID
     */
    public String appid() {
        return appid;
    }

    /**
     * 返回当前应用固定的支付通知地址。
     *
     * @return 通知 URI
     */
    public URI notifyUrl() {
        return notifyUrl;
    }

    /**
     * 返回 Native 支付入口。
     *
     * @return Native 支付客户端
     */
    public NativePayClient nativePay() {
        return nativePayClient;
    }
}
