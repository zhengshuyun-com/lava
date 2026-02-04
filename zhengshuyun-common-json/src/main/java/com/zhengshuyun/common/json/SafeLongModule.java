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

package com.zhengshuyun.common.json;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Long值JS安全序列化
 *
 * @author Toint
 * @since 2026/1/2
 */
public class SafeLongModule extends SimpleModule {
    public SafeLongModule() {
        super("SafeLongModule");
        addSerializer(Long.class, new SafeLongSerializer());
        addSerializer(Long.TYPE, new SafeLongSerializer());
    }
}
