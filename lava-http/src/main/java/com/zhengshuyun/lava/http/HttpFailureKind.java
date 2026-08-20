/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

/**
 * 传输和本地 HTTP 失败的稳定分类。
 */
public enum HttpFailureKind {
    DNS,
    CONNECTION,
    TLS,
    TIMEOUT,
    CANCELLED,
    PROTOCOL,
    IO,
    RESPONSE_TOO_LARGE
}
