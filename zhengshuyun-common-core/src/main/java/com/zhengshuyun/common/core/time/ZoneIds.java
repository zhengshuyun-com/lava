/*
 * Copyright 2025 Toint (599818663@qq.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.common.core.time;

import java.time.ZoneId;

/**
 * @author Toint
 * @since 2025/12/29
 */
public final class ZoneIds {

    /**
     * 协调世界时
     */
    public static final ZoneId UTC = ZoneId.of("UTC");

    /**
     * 上海
     */
    public static final ZoneId ASIA_SHANGHAI = ZoneId.of("Asia/Shanghai");

    /**
     * 美国东部时间 (纽约) 
     */
    public static final ZoneId AMERICA_NEW_YORK = ZoneId.of("America/New_York");

    /**
     * 美国西部时间 (洛杉矶) 
     */
    public static final ZoneId AMERICA_LOS_ANGELES = ZoneId.of("America/Los_Angeles");

    /**
     * 美国中部时间 (芝加哥) 
     */
    public static final ZoneId AMERICA_CHICAGO = ZoneId.of("America/Chicago");

    /**
     * 伦敦/格林威治标准时间
     */
    public static final ZoneId EUROPE_LONDON = ZoneId.of("Europe/London");

    /**
     * 巴黎
     */
    public static final ZoneId EUROPE_PARIS = ZoneId.of("Europe/Paris");

    /**
     * 柏林
     */
    public static final ZoneId EUROPE_BERLIN = ZoneId.of("Europe/Berlin");

    /**
     * 东京
     */
    public static final ZoneId ASIA_TOKYO = ZoneId.of("Asia/Tokyo");

    /**
     * 香港
     */
    public static final ZoneId ASIA_HONG_KONG = ZoneId.of("Asia/Hong_Kong");

    /**
     * 新加坡
     */
    public static final ZoneId ASIA_SINGAPORE = ZoneId.of("Asia/Singapore");

    /**
     * 悉尼
     */
    public static final ZoneId AUSTRALIA_SYDNEY = ZoneId.of("Australia/Sydney");

}
