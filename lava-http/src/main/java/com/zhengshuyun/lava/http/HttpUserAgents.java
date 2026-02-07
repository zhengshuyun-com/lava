/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.http;

/**
 * User-Agent 常量
 * <p>
 * 包含常用的浏览器和设备的 User-Agent 字符串, 用于模拟真实浏览器请求
 *
 * @author Toint
 * @since 2026/1/12
 */
public final class HttpUserAgents {

    private HttpUserAgents() {
    }

    /**
     * Chrome on macOS
     */
    public static final String CHROME_MAC =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36";

    /**
     * Chrome on Windows
     */
    public static final String CHROME_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36";

    /**
     * Chrome on Linux
     */
    public static final String CHROME_LINUX =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36";

    /**
     * 默认推荐的 User-Agent (Chrome on macOS)
     * <p>
     * 这是最常用的 User-Agent, 兼容性好, 不容易被识别为爬虫
     */
    public static final String DEFAULT = CHROME_MAC;
}