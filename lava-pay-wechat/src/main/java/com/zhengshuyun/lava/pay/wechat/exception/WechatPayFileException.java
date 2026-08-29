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

import com.zhengshuyun.lava.core.lang.ValidationUtils;

import java.io.Serial;

/**
 * 账单目标文件冲突或本地文件系统操作失败。
 */
public final class WechatPayFileException extends WechatPayException {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 稳定的文件失败类别。 */
    private final WechatPayFileFailure failure;
    /** 底层异常类型名。 */
    private final String causeType;

    /**
     * 创建账单文件异常。
     *
     * @param failure 精确失败类别
     * @param causeType 底层异常类型名；没有底层异常时传空字符串
     */
    public WechatPayFileException(WechatPayFileFailure failure, String causeType) {
        super("微信支付账单文件处理失败: "
                + ValidationUtils.requireNonNull(failure, "failure"));
        this.failure = failure;
        this.causeType = ValidationUtils.requireNonNull(causeType, "causeType");
    }

    /**
     * 返回文件失败类别。
     *
     * @return 文件失败类别
     */
    public WechatPayFileFailure failure() {
        return failure;
    }

    /**
     * 返回底层异常类型名。
     *
     * @return 底层异常类型名；没有时为空字符串
     */
    public String causeType() {
        return causeType;
    }
}
