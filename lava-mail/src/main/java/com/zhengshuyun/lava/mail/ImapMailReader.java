/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;
import jakarta.mail.Address;
import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import org.jspecify.annotations.Nullable;

import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import com.zhengshuyun.lava.core.lang.ValidationUtils;

/** 执行短连接 IMAP 操作，并把 Jakarta Mail 对象转换为模块稳定模型。 */
final class ImapMailReader {
    private static final int FETCH_BATCH_SIZE = 128;

    MailPage<MailMessageSummary> list(
            ImapServerConfig config,
            MailCredential credential,
            @Nullable String accessToken,
            MailQuery query,
            MailLimits limits) {
        String folderName = query.folder() == null ? config.defaultFolder() : query.folder();
        return withFolder(config, credential, accessToken, folderName,
                (folder, uidFolder) -> listOpen(folder, uidFolder, folderName, query, limits));
    }

    MailMessage read(
            ImapServerConfig config,
            MailCredential credential,
            @Nullable String accessToken,
            MailMessageId id,
            MailLimits limits) {
        return withFolder(config, credential, accessToken, id.folder(), (folder, uidFolder) -> {
            verifyUidValidity(uidFolder, id.uidValidity(), "message id");
            Message message = uidFolder.getMessageByUID(id.uid());
            if (message == null) {
                throw new MailException(MailFailureKind.PROTOCOL, "message UID no longer exists");
            }
            return MailMessageParser.message(message, id, limits);
        });
    }

    long download(
            ImapServerConfig config,
            MailCredential credential,
            @Nullable String accessToken,
            MailMessageId id,
            int attachmentIndex,
            OutputStream destination,
            MailLimits limits) {
        return withFolder(config, credential, accessToken, id.folder(), (folder, uidFolder) -> {
            verifyUidValidity(uidFolder, id.uidValidity(), "message id");
            Message message = uidFolder.getMessageByUID(id.uid());
            if (message == null) {
                throw new MailException(MailFailureKind.PROTOCOL, "message UID no longer exists");
            }
            return MailMessageParser.downloadAttachment(message, attachmentIndex, destination, limits);
        });
    }

    static MailPage<MailMessageSummary> listOpen(
            Folder folder,
            UIDFolder uidFolder,
            String folderName,
            MailQuery query,
            MailLimits limits) throws Exception {
        long uidValidity = uidFolder.getUIDValidity();
        if (!MailMessageId.validUid(uidValidity)) {
            throw new MailException(MailFailureKind.PROTOCOL, "mailbox did not provide UIDVALIDITY");
        }
        long beforeUid = startingUid(uidFolder, folderName, uidValidity, query.cursor());
        List<MailMessageSummary> result = new ArrayList<>(query.pageSize());
        while (beforeUid > 1) {
            // 只按固定宽度向更旧 UID 扫描，避免一次把整个邮箱的 Message 对象载入内存。
            long high = beforeUid - 1;
            long low = Math.max(1, high - FETCH_BATCH_SIZE + 1);
            Message[] batch = uidFolder.getMessagesByUID(low, high);
            if (batch == null || batch.length == 0) {
                beforeUid = low;
                continue;
            }
            FetchProfile profile = new FetchProfile();
            profile.add(FetchProfile.Item.ENVELOPE);
            profile.add(FetchProfile.Item.FLAGS);
            profile.add(FetchProfile.Item.CONTENT_INFO);
            profile.add("Message-ID");
            folder.fetch(batch, profile);

            List<MessageWithUid> ordered = new ArrayList<>(batch.length);
            for (Message message : batch) {
                if (message != null) {
                    long uid = uidFolder.getUID(message);
                    if (uid > MailMessageId.MAX_IMAP_UID) {
                        throw new MailException(
                                MailFailureKind.PROTOCOL,
                                "mailbox returned an out-of-range message UID");
                    }
                    if (MailMessageId.validUid(uid) && uid < beforeUid) {
                        ordered.add(new MessageWithUid(message, uid));
                    }
                }
            }
            ordered.sort(Comparator.comparingLong(MessageWithUid::uid).reversed());
            for (MessageWithUid candidate : ordered) {
                if (!matches(candidate.message(), query)) {
                    continue;
                }
                MailMessageId id = new MailMessageId(folderName, uidValidity, candidate.uid());
                result.add(MailMessageParser.summary(candidate.message(), id, limits));
                if (result.size() == query.pageSize()) {
                    // 游标记录最后一条已返回 UID；下一页使用排他上界，因此不会重复或读入新到邮件。
                    MailCursor next = candidate.uid() > 1
                            ? new MailCursor(folderName, uidValidity, candidate.uid())
                            : null;
                    return new MailPage<>(result, next);
                }
            }
            beforeUid = low;
        }
        return new MailPage<>(result, null);
    }

    private static long startingUid(
            UIDFolder folder,
            String folderName,
            long uidValidity,
            @Nullable MailCursor cursor) throws Exception {
        if (cursor != null) {
            if (!folderName.equals(cursor.folder())) {
                throw new MailException(
                        MailFailureKind.CONFIGURATION, "cursor belongs to a different folder");
            }
            if (uidValidity != cursor.uidValidity()) {
                throw new MailException(
                        MailFailureKind.PROTOCOL,
                        "cursor UIDVALIDITY no longer matches mailbox");
            }
        }
        long uidNext = folder.getUIDNext();
        if (!MailMessageId.validUid(uidNext)) {
            throw new MailException(MailFailureKind.PROTOCOL, "mailbox did not provide UIDNEXT");
        }
        if (cursor == null) {
            return uidNext;
        }
        if (cursor.beforeUid() > uidNext) {
            throw new MailException(
                    MailFailureKind.CONFIGURATION, "cursor is ahead of the mailbox UIDNEXT");
        }
        return cursor.beforeUid();
    }

    private static boolean matches(Message message, MailQuery query) throws Exception {
        if (query.unreadOnly() && message.isSet(Flags.Flag.SEEN)) {
            return false;
        }
        Instant received = message.getReceivedDate() == null
                ? null
                : message.getReceivedDate().toInstant();
        if (query.receivedAfter() != null
                && (received == null || !received.isAfter(query.receivedAfter()))) {
            return false;
        }
        if (query.receivedBefore() != null
                && (received == null || !received.isBefore(query.receivedBefore()))) {
            return false;
        }
        if (query.subjectContains() != null
                && !containsIgnoreCase(message.getSubject(), query.subjectContains())) {
            return false;
        }
        return query.fromContains() == null || addressesContain(message.getFrom(), query.fromContains());
    }

    private static boolean addressesContain(@Nullable Address[] addresses, String needle) {
        if (addresses == null) {
            return false;
        }
        String normalizedNeedle = needle.toLowerCase(Locale.ROOT);
        return Arrays.stream(addresses).anyMatch(address -> {
            if (address instanceof InternetAddress internetAddress) {
                return containsIgnoreCase(internetAddress.getAddress(), normalizedNeedle)
                        || containsIgnoreCase(internetAddress.getPersonal(), normalizedNeedle);
            }
            return containsIgnoreCase(address.toString(), normalizedNeedle);
        });
    }

    private static boolean containsIgnoreCase(@Nullable String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static void verifyUidValidity(UIDFolder folder, long expected, String source) throws Exception {
        if (folder.getUIDValidity() != expected) {
            throw new MailException(MailFailureKind.PROTOCOL, source + " UIDVALIDITY no longer matches mailbox");
        }
    }

    private static <T> T withFolder(
            ImapServerConfig config,
            MailCredential credential,
            @Nullable String accessToken,
            String folderName,
            FolderOperation<T> operation) {
        ValidationUtils.requireNonNull(config, "config");
        ValidationUtils.requireNonNull(credential, "credential");
        ValidationUtils.requireNonNull(folderName, "folderName");
        Session session = MailSessionFactory.imap(config, credential);
        // 每次操作独立管理 Store 和 Folder，避免把断线状态泄漏到后续调用。
        try (Store store = session.getStore("imap")) {
            store.connect(
                    config.host(), config.port(), credential.username(),
                    MailSessionFactory.authenticationSecret(credential, accessToken));
            try (Folder folder = store.getFolder(folderName)) {
                if (!folder.exists()) {
                    throw new MailException(MailFailureKind.CONFIGURATION, "mail folder does not exist");
                }
                folder.open(Folder.READ_ONLY);
                if (!(folder instanceof UIDFolder uidFolder)) {
                    throw new MailException(MailFailureKind.PROTOCOL, "mail server does not support IMAP UID");
                }
                return operation.apply(folder, uidFolder);
            }
        } catch (MailException exception) {
            throw exception;
        } catch (Exception exception) {
            throw MailFailures.wrap("read IMAP mailbox", exception);
        }
    }

    private record MessageWithUid(Message message, long uid) {
    }

    @FunctionalInterface
    private interface FolderOperation<T> {
        T apply(Folder folder, UIDFolder uidFolder) throws Exception;
    }
}
