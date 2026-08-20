/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ImapUidPaginationTest {
    private static final Instant RECEIVED = Instant.parse("2026-08-17T00:00:00Z");

    @Test
    void smallFirstPageFetchesOneBoundedUidRangeAndCursorExcludesNewMail() throws Exception {
        FakeUidFolder folder = new FakeUidFolder("INBOX", 42);
        folder.put(999, message("older", "alice@example.com", false, RECEIVED));
        folder.put(1_000, message("newest", "alice@example.com", false, RECEIVED));

        MailPage<MailMessageSummary> first = ImapMailReader.listOpen(
                folder, folder, "INBOX", MailQuery.firstPage(1), MailLimits.DEFAULT);

        assertEquals(List.of(1_000L), first.items().stream().map(item -> item.id().uid()).toList());
        MailCursor cursor = Objects.requireNonNull(first.nextCursor());
        assertEquals(1_000, cursor.beforeUid());
        assertEquals(1, folder.ranges.size());
        assertTrue(folder.ranges.getFirst().width() <= 128);

        folder.put(1_001, message("arrived later", "alice@example.com", false, RECEIVED));
        MailPage<MailMessageSummary> second = ImapMailReader.listOpen(
                folder, folder, "INBOX", MailQuery.firstPage(1).nextPage(cursor),
                MailLimits.DEFAULT);

        assertEquals(List.of(999L), second.items().stream().map(item -> item.id().uid()).toList());
    }

    @Test
    void appliesFiltersWhileKeepingUidDescendingOrder() throws Exception {
        FakeUidFolder folder = new FakeUidFolder("INBOX", 7);
        folder.put(1, message("Quarterly report", "alice@example.com", false, RECEIVED));
        folder.put(2, message("Quarterly report", "alice@example.com", true, RECEIVED));
        folder.put(3, message("unrelated", "bob@example.com", false, RECEIVED));
        folder.put(4, message("QUARTERLY REPORT", "Alice Team <alice@example.com>", false, RECEIVED));

        MailQuery query = new MailQuery(
                "INBOX", 10, null, true,
                RECEIVED.minusSeconds(1), RECEIVED.plusSeconds(1), "ALICE", "quarterly");
        MailPage<MailMessageSummary> page = ImapMailReader.listOpen(
                folder, folder, "INBOX", query, MailLimits.DEFAULT);

        assertEquals(List.of(4L, 1L), page.items().stream().map(item -> item.id().uid()).toList());
        assertNull(page.nextCursor());
        assertEquals(1, folder.fetches);
    }

    @Test
    void rejectsStaleOrForeignCursorsAndMissingUidMetadata() {
        FakeUidFolder folder = new FakeUidFolder("INBOX", 10);
        MailQuery stale = MailQuery.firstPage(1).nextPage(new MailCursor("INBOX", 9, 2));
        MailException staleFailure = assertThrows(MailException.class,
                () -> ImapMailReader.listOpen(
                        folder, folder, "INBOX", stale, MailLimits.DEFAULT));
        assertEquals(MailFailureKind.PROTOCOL, staleFailure.kind());

        MailQuery foreign = MailQuery.firstPage(1).nextPage(new MailCursor("Archive", 10, 2));
        MailException foreignFailure = assertThrows(MailException.class,
                () -> ImapMailReader.listOpen(
                        folder, folder, "INBOX", foreign, MailLimits.DEFAULT));
        assertEquals(MailFailureKind.CONFIGURATION, foreignFailure.kind());

        MailQuery forged = MailQuery.firstPage(1).nextPage(new MailCursor("INBOX", 10, 10_000));
        MailException forgedFailure = assertThrows(MailException.class,
                () -> ImapMailReader.listOpen(
                        folder, folder, "INBOX", forged, MailLimits.DEFAULT));
        assertEquals(MailFailureKind.CONFIGURATION, forgedFailure.kind());
        assertTrue(folder.ranges.isEmpty());

        folder.uidNext = 0;
        MailException missingUidNext = assertThrows(MailException.class,
                () -> ImapMailReader.listOpen(
                        folder, folder, "INBOX", MailQuery.firstPage(1), MailLimits.DEFAULT));
        assertEquals(MailFailureKind.PROTOCOL, missingUidNext.kind());

        folder.uidValidity = 0;
        MailException missingValidity = assertThrows(MailException.class,
                () -> ImapMailReader.listOpen(
                        folder, folder, "INBOX", MailQuery.firstPage(1), MailLimits.DEFAULT));
        assertEquals(MailFailureKind.PROTOCOL, missingValidity.kind());
    }

    @Test
    void uidOneTerminatesPaginationWithoutSyntheticCursor() throws Exception {
        FakeUidFolder folder = new FakeUidFolder("INBOX", 1);
        folder.put(1, message("only", "sender@example.com", false, RECEIVED));

        MailPage<MailMessageSummary> page = ImapMailReader.listOpen(
                folder, folder, "INBOX", MailQuery.firstPage(1), MailLimits.DEFAULT);

        assertEquals(1, page.items().size());
        assertNull(page.nextCursor());
    }

    private static MimeMessage message(
            String subject, String from, boolean seen, Instant received) throws Exception {
        ReceivedMimeMessage message = new ReceivedMimeMessage(received);
        message.setFrom(InternetAddress.parse(from, true)[0]);
        message.setRecipients(Message.RecipientType.TO, "receiver@example.com");
        message.setSubject(subject);
        message.setText("body");
        message.setFlag(Flags.Flag.SEEN, seen);
        message.saveChanges();
        return message;
    }

    private record UidRange(long low, long high) {
        long width() {
            return high - low + 1;
        }
    }

    private static final class ReceivedMimeMessage extends MimeMessage {
        private final Date received;

        private ReceivedMimeMessage(Instant received) {
            super(jakarta.mail.Session.getInstance(new Properties()));
            this.received = Date.from(received);
        }

        @Override
        public Date getReceivedDate() {
            return new Date(received.getTime());
        }
    }

    private static final class FakeUidFolder extends Folder implements UIDFolder {
        private final String name;
        private final NavigableMap<Long, Message> messages = new TreeMap<>();
        private final Map<Message, Long> reverse = new IdentityHashMap<>();
        private final List<UidRange> ranges = new ArrayList<>();
        private long uidValidity;
        private long uidNext = 1;
        private int fetches;
        private boolean open;

        private FakeUidFolder(String name, long uidValidity) {
            super(new FakeStore());
            this.name = name;
            this.uidValidity = uidValidity;
        }

        void put(long uid, Message message) {
            messages.put(uid, message);
            reverse.put(message, uid);
            uidNext = Math.max(uidNext, uid + 1);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getFullName() {
            return name;
        }

        @Override
        public Folder getParent() {
            return this;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public Folder[] list(String pattern) {
            return new Folder[0];
        }

        @Override
        public char getSeparator() {
            return '/';
        }

        @Override
        public int getType() {
            return HOLDS_MESSAGES;
        }

        @Override
        public boolean create(int type) {
            return false;
        }

        @Override
        public boolean hasNewMessages() {
            return false;
        }

        @Override
        public Folder getFolder(String child) {
            return this;
        }

        @Override
        public boolean delete(boolean recurse) {
            return false;
        }

        @Override
        public boolean renameTo(Folder target) {
            return false;
        }

        @Override
        public void open(int mode) {
            open = true;
        }

        @Override
        public void close(boolean expunge) {
            open = false;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public Flags getPermanentFlags() {
            return new Flags();
        }

        @Override
        public int getMessageCount() {
            return messages.size();
        }

        @Override
        public Message getMessage(int messageNumber) {
            return messages.values().stream().skip(messageNumber - 1L).findFirst().orElseThrow();
        }

        @Override
        public void appendMessages(Message[] added) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Message[] expunge() {
            return new Message[0];
        }

        @Override
        public void fetch(Message[] batch, FetchProfile profile) {
            fetches++;
        }

        @Override
        public long getUIDValidity() {
            return uidValidity;
        }

        @Override
        public @Nullable Message getMessageByUID(long uid) {
            return messages.get(uid);
        }

        @Override
        public Message[] getMessagesByUID(long start, long end) {
            ranges.add(new UidRange(start, end));
            return messages.subMap(start, true, end, true).values().toArray(Message[]::new);
        }

        @Override
        public Message[] getMessagesByUID(long[] uids) {
            List<Message> result = new ArrayList<>();
            for (long uid : uids) {
                Message message = messages.get(uid);
                if (message != null) {
                    result.add(message);
                }
            }
            return result.toArray(Message[]::new);
        }

        @Override
        public long getUID(Message message) {
            Long uid = reverse.get(message);
            return uid == null ? -1 : uid;
        }

        @Override
        public long getUIDNext() {
            return uidNext;
        }
    }

    private static final class FakeStore extends Store {
        @SuppressWarnings("NullAway")
        private FakeStore() {
            super(jakarta.mail.Session.getInstance(new Properties()), null);
        }

        @Override
        protected boolean protocolConnect(
                @Nullable String host,
                int port,
                @Nullable String user,
                @Nullable String password) {
            return true;
        }

        @Override
        public Folder getDefaultFolder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Folder getFolder(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Folder getFolder(URLName url) {
            throw new UnsupportedOperationException();
        }
    }
}
