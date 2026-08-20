/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * 邮件操作失败时抛出的结构化异常。
 *
 * <p>异常消息和保留的诊断信息都不会包含服务器原始响应或凭证。为了便于诊断，仅记录原始异常的类型名，
 * 不通过 {@link #getCause()} 保留可能泄露敏感信息的异常对象。
 */
public final class MailException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 稳定的失败类别。
     */
    private final MailFailureKind kind;

    /**
     * 可选底层异常类型名。
     */
    private final @Nullable String causeType;

    /**
     * 创建不带底层异常信息的邮件异常。
     *
     * @param kind    失败类别
     * @param message 凭证安全的错误消息
     */
    public MailException(MailFailureKind kind, String message) {
        super(ValidationUtils.requireNonNull(message, "message"));
        this.kind = ValidationUtils.requireNonNull(kind, "kind");
        causeType = null;
    }

    /**
     * 创建只保留底层异常类型名的邮件异常。
     *
     * @param kind    失败类别
     * @param message 凭证安全的错误消息
     * @param cause   底层异常；其对象、消息和堆栈不会保留
     */
    public MailException(MailFailureKind kind, String message, Throwable cause) {
        super(ValidationUtils.requireNonNull(message, "message"));
        this.kind = ValidationUtils.requireNonNull(kind, "kind");
        causeType = ValidationUtils.requireNonNull(cause, "cause").getClass().getName();
    }

    /**
     * 返回稳定的失败类别。
     *
     * @return 失败类别
     */
    public MailFailureKind kind() {
        return kind;
    }

    /**
     * 返回原始异常的类型名；没有原始异常时返回 {@code null}。
     *
     * @return 原始异常类型名
     */
    public @Nullable String causeType() {
        return causeType;
    }
}
