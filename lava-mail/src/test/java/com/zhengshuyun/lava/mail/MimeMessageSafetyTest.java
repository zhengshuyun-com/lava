/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import jakarta.activation.DataHandler;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MimeMessageSafetyTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void listsMetadataAndReadsBodiesWithoutRetainingAttachment() {
        MimeMessage source = MimeMessageFactory.create(
                session(), request(), MailLimits.DEFAULT, CLOCK);
        MailMessageId id = new MailMessageId("INBOX", 42, 7);

        MailMessage parsed = MailMessageParser.message(source, id, MailLimits.DEFAULT);

        assertEquals("plain", parsed.textBody());
        assertEquals("<p>html</p>", parsed.htmlBody());
        assertEquals(1, parsed.summary().attachments().size());
        assertEquals("sample.txt", parsed.summary().attachments().getFirst().fileName());
        assertNull(parsed.summary().attachments().getFirst().contentId());
    }

    @Test
    void streamsAttachmentWithoutClosingOrFlushingBorrowedDestination() {
        MimeMessage source = MimeMessageFactory.create(
                session(), request(), MailLimits.DEFAULT, CLOCK);
        TrackingOutputStream destination = new TrackingOutputStream();

        long count = MailMessageParser.downloadAttachment(
                source, 0, destination, MailLimits.DEFAULT);

        assertEquals(6, count);
        assertArrayEquals("abcdef".getBytes(UTF_8), destination.toByteArray());
        assertFalse(destination.closed);
        assertFalse(destination.flushed);
    }

    @Test
    void rejectsBodiesAndAttachmentsWithoutUnboundedAllocationOrWrites() throws Exception {
        MimeMessage body = new MimeMessage(session());
        body.setText("12345", UTF_8.name());
        body.saveChanges();
        MailLimits small = new MailLimits(4, 4, 8, 4);

        MailException bodyFailure = assertThrows(MailException.class,
                () -> MailMessageParser.message(
                        body, new MailMessageId("INBOX", 1, 1), small));
        assertEquals(MailFailureKind.SIZE_LIMIT, bodyFailure.kind());

        MimeMessage attachment = MimeMessageFactory.create(
                session(), request(), MailLimits.DEFAULT, CLOCK);
        ByteArrayOutputStream boundedDestination = new ByteArrayOutputStream();
        MailException attachmentFailure = assertThrows(MailException.class,
                () -> MailMessageParser.downloadAttachment(
                        attachment, 0, boundedDestination, small));
        assertEquals(MailFailureKind.SIZE_LIMIT, attachmentFailure.kind());
        assertTrue(boundedDestination.size() <= small.maxAttachmentBytes());
    }

    @Test
    void reportsContentIdAndSafeFallbackMetadataWithoutReadingAttachmentBytes() throws Exception {
        MimeMessage source = MimeMessageFactory.create(
                session(), request(), MailLimits.DEFAULT, CLOCK);
        MimeMultipart mixed = (MimeMultipart) source.getContent();
        MimeBodyPart attachment = (MimeBodyPart) mixed.getBodyPart(1);
        attachment.setHeader("Content-ID", "<asset@example.com>");
        attachment.setHeader("Content-Type", "not a valid content type");

        MailMessageSummary summary = MailMessageParser.summary(
                source, new MailMessageId("INBOX", 1, 2), MailLimits.DEFAULT);

        MailAttachmentInfo info = summary.attachments().getFirst();
        assertEquals("asset@example.com", info.contentId());
        assertEquals("application/octet-stream", info.contentType());
    }

    @Test
    void closesOpenedAttachmentInputButLeavesDestinationBorrowed() throws Exception {
        TrackingInputStream input = new TrackingInputStream("streamed".getBytes(UTF_8));
        MimeBodyPart attachment = new MimeBodyPart() {
            @Override
            public InputStream getInputStream() {
                return input;
            }

            @Override
            public int getSize() {
                return -1;
            }
        };
        attachment.setDisposition(MimeBodyPart.ATTACHMENT);
        attachment.setFileName("stream.txt");
        MimeMultipart multipart = new MimeMultipart("mixed");
        multipart.addBodyPart(attachment);
        MimeMessage source = new MimeMessage(session());
        source.setContent(multipart);
        source.saveChanges();
        TrackingOutputStream destination = new TrackingOutputStream();

        long written = MailMessageParser.downloadAttachment(
                source, 0, destination, MailLimits.DEFAULT);

        assertEquals(8, written);
        assertTrue(input.closed);
        assertFalse(destination.closed);
        assertFalse(destination.flushed);
    }

    @Test
    void rejectsMissingNegativeAndDeeplyNestedAttachments() throws Exception {
        MimeMessage source = MimeMessageFactory.create(
                session(), request(), MailLimits.DEFAULT, CLOCK);
        assertThrows(IllegalArgumentException.class, () -> MailMessageParser.downloadAttachment(
                source, -1, new ByteArrayOutputStream(), MailLimits.DEFAULT));
        MailException missing = assertThrows(MailException.class,
                () -> MailMessageParser.downloadAttachment(
                        source, 10, new ByteArrayOutputStream(), MailLimits.DEFAULT));
        assertEquals(MailFailureKind.PARSING, missing.kind());

        MimeBodyPart leaf = new MimeBodyPart();
        leaf.setDataHandler(new DataHandler(
                new ByteArrayDataSource("x".getBytes(UTF_8), "application/octet-stream")));
        leaf.setFileName("deep.bin");
        MimeBodyPart nested = leaf;
        for (int depth = 0; depth < 3; depth++) {
            MimeMultipart level = new MimeMultipart("mixed");
            level.addBodyPart(nested);
            MimeBodyPart container = new MimeBodyPart();
            container.setContent(level);
            nested = container;
        }
        MimeMultipart root = new MimeMultipart("mixed");
        root.addBodyPart(nested);
        MimeMessage deep = new MimeMessage(session());
        deep.setContent(root);
        deep.saveChanges();
        MailException depthFailure = assertThrows(MailException.class,
                () -> MailMessageParser.summary(
                        deep, new MailMessageId("INBOX", 1, 3),
                        new MailLimits(10, 10, 20, 2)));
        assertEquals(MailFailureKind.SIZE_LIMIT, depthFailure.kind());
    }

    @Test
    void enforcesPerBodyAndCombinedDecodedLimitsForInboundAndOutboundMail() throws Exception {
        MailSendRequest request = request();
        MailException outgoingBody = assertThrows(MailException.class,
                () -> MimeMessageFactory.create(
                        session(), request, new MailLimits(4, 10, 20, 5), CLOCK));
        assertEquals(MailFailureKind.SIZE_LIMIT, outgoingBody.kind());

        MailException outgoingAttachment = assertThrows(MailException.class,
                () -> MimeMessageFactory.create(
                        session(), request, new MailLimits(20, 5, 30, 5), CLOCK));
        assertEquals(MailFailureKind.SIZE_LIMIT, outgoingAttachment.kind());

        MailException outgoingTotal = assertThrows(MailException.class,
                () -> MimeMessageFactory.create(
                        session(), request, new MailLimits(15, 10, 15, 5), CLOCK));
        assertEquals(MailFailureKind.SIZE_LIMIT, outgoingTotal.kind());

        MimeMultipart alternative = new MimeMultipart("alternative");
        MimeBodyPart text = new MimeBodyPart();
        text.setText("1234", UTF_8.name());
        alternative.addBodyPart(text);
        MimeBodyPart html = new MimeBodyPart();
        html.setContent("5678", "text/html; charset=UTF-8");
        alternative.addBodyPart(html);
        MimeMessage inbound = new MimeMessage(session());
        inbound.setContent(alternative);
        MailException inboundTotal = assertThrows(MailException.class,
                () -> MailMessageParser.message(
                        inbound, new MailMessageId("INBOX", 1, 4),
                        new MailLimits(4, 4, 7, 5)));
        assertEquals(MailFailureKind.SIZE_LIMIT, inboundTotal.kind());
    }

    @Test
    void countsUtf8WithoutAllocatingAnEncodedCopyAndMatchesReplacementSemantics() {
        assertEquals(1, MimeMessageFactory.boundedUtf8Length("a", 1));
        assertEquals(2, MimeMessageFactory.boundedUtf8Length("é", 2));
        assertEquals(3, MimeMessageFactory.boundedUtf8Length("汉", 3));
        assertEquals(4, MimeMessageFactory.boundedUtf8Length("😀", 4));
        assertEquals(1, MimeMessageFactory.boundedUtf8Length("\ud800", 1));
        assertEquals(-1, MimeMessageFactory.boundedUtf8Length("😀😀", 7));

        MailSendRequest oversized = MailSendRequest.text(
                new MailAddress("sender@example.com"),
                List.of(new MailAddress("receiver@example.com")),
                "subject", "😀".repeat(10_000));
        MailException failure = assertThrows(MailException.class,
                () -> MimeMessageFactory.create(
                        session(), oversized, new MailLimits(7, 7, 7, 2), CLOCK));
        assertEquals(MailFailureKind.SIZE_LIMIT, failure.kind());
    }

    @Test
    void createsAndParsesTextOnlyAndHtmlOnlyMessages() {
        MailSendRequest text = MailSendRequest.text(
                new MailAddress("sender@example.com"),
                List.of(new MailAddress("receiver@example.com")), "text", "plain");
        MailMessage parsedText = MailMessageParser.message(
                MimeMessageFactory.create(session(), text, MailLimits.DEFAULT, CLOCK),
                new MailMessageId("INBOX", 1, 5), MailLimits.DEFAULT);
        assertEquals("plain", parsedText.textBody());
        assertNull(parsedText.htmlBody());

        MailSendRequest html = new MailSendRequest(
                text.from(), text.to(), List.of(), List.of(), List.of(), "html",
                null, "<strong>html</strong>", List.of());
        MailMessage parsedHtml = MailMessageParser.message(
                MimeMessageFactory.create(session(), html, MailLimits.DEFAULT, CLOCK),
                new MailMessageId("INBOX", 1, 6), MailLimits.DEFAULT);
        assertNull(parsedHtml.textBody());
        assertEquals("<strong>html</strong>", parsedHtml.htmlBody());
    }

    private static MailSendRequest request() {
        return new MailSendRequest(
                new MailAddress("sender@example.com"),
                List.of(new MailAddress("receiver@example.com")),
                List.of(), List.of(), List.of(), "subject", "plain", "<p>html</p>",
                List.of(new MailAttachment("sample.txt", "text/plain", "abcdef".getBytes(UTF_8))));
    }

    private static Session session() {
        return Session.getInstance(new Properties());
    }

    private static final class TrackingOutputStream extends ByteArrayOutputStream {
        private boolean closed;
        private boolean flushed;

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public void flush() {
            flushed = true;
        }
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {
        private boolean closed;

        private TrackingInputStream(byte[] content) {
            super(content);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
