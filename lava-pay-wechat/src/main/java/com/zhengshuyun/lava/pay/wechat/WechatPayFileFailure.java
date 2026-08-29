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

/**
 * 账单文件处理失败的稳定分类。
 */
public enum WechatPayFileFailure {
    /**
     * 目标文件已经存在。
     */
    TARGET_EXISTS,
    /**
     * 目标路径或父目录无效。
     */
    INVALID_TARGET,
    /**
     * 本地文件系统读写失败。
     */
    IO
}
