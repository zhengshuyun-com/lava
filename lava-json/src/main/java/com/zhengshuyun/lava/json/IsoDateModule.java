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

package com.zhengshuyun.lava.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * java.util.Date ISO 8601 UTC 格式序列化模块
 *
 * <p>确保 Date 输出为 ISO 8601 格式并带 Z 后缀 (如 2026-01-01T00:00:00Z)
 *
 * @author Toint
 * @since 2026/02/05
 */
final class IsoDateModule extends SimpleModule {

    IsoDateModule(ZoneId zoneId) {
        super("IsoDateModule");
        addSerializer(Date.class, new IsoDateSerializer(zoneId));
    }

    private static class IsoDateSerializer extends JsonSerializer<Date> {
        private final ZoneId zoneId;

        IsoDateSerializer(ZoneId zoneId) {
            this.zoneId = zoneId;
        }

        @Override
        public void serialize(Date value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            // 转换为指定时区的 Instant，然后格式化为 ISO 8601
            Instant instant = value.toInstant().atZone(zoneId).toInstant();
            gen.writeString(DateTimeFormatter.ISO_INSTANT.format(instant));
        }
    }
}
