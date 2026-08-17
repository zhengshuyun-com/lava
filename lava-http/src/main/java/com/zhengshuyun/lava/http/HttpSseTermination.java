/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

/** SSE 会话进入终态的原因。 */
enum HttpSseTermination {
    CANCELLED,
    REMOTE_CLOSED,
    FAILED
}
