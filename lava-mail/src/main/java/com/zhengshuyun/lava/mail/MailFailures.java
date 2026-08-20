/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.FolderClosedException;
import jakarta.mail.MessagingException;
import jakarta.mail.StoreClosedException;
import jakarta.mail.internet.AddressException;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.SSLException;
import java.net.*;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * 为包内邮件传输实现提供凭证安全的失败分类。
 */
final class MailFailures {
    private MailFailures() {
    }

    static MailException wrap(String operation, Throwable failure) {
        if (failure instanceof MailException mailException) {
            return mailException;
        }
        MailFailureKind kind = classify(failure);
        return new MailException(kind, operation + " failed (" + kind + ")", failure);
    }

    private static MailFailureKind classify(Throwable failure) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        MailFailureKind fallback = MailFailureKind.PARSING;
        for (Throwable current = failure;
             current != null && visited.add(current);
             current = next(current)) {
            if (current instanceof AuthenticationFailedException) {
                return MailFailureKind.AUTHENTICATION;
            }
            if (current instanceof SSLException) {
                return MailFailureKind.TLS;
            }
            if (current instanceof SocketTimeoutException) {
                return MailFailureKind.TIMEOUT;
            }
            // IMAP 连接中断通常由 Jakarta Mail 收口为资源关闭异常，而不是直接暴露 SocketException。
            if (current instanceof FolderClosedException || current instanceof StoreClosedException) {
                return MailFailureKind.CONNECTION;
            }
            if (current instanceof ConnectException
                    || current instanceof NoRouteToHostException
                    || current instanceof UnknownHostException
                    || current instanceof SocketException) {
                return MailFailureKind.CONNECTION;
            }
            if (current instanceof AddressException) {
                return MailFailureKind.PARSING;
            }
            if (current instanceof MessagingException) {
                fallback = MailFailureKind.PROTOCOL;
            }
        }
        return fallback;
    }

    @SuppressWarnings("ReferenceEquality") // 检测异常链环路必须比较对象身份。
    private static @Nullable Throwable next(Throwable failure) {
        if (failure instanceof MessagingException messagingException
                && messagingException.getNextException() != null
                && messagingException.getNextException() != failure) {
            return messagingException.getNextException();
        }
        return failure.getCause() == failure ? null : failure.getCause();
    }
}
