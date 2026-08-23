/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhengshuyun.lava.mail;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MailClientLifecycleTest {
    private static final PasswordCredential CREDENTIAL = new PasswordCredential("user", "password");

    @Test
    void senderRejectsOperationsAfterIdempotentClose() {
        MailSender sender = new MailSender(smtp(), CREDENTIAL);
        sender.close();
        sender.close();

        assertThrows(IllegalStateException.class, () -> sender.send(MailSendRequest.text(
                new MailAddress("sender@example.com"),
                List.of(new MailAddress("receiver@example.com")), "subject", "body")));
    }

    @Test
    void readerValidatesIndicesLocallyAndRejectsOperationsAfterClose() {
        MailReader reader = new MailReader(imap(), CREDENTIAL);
        MailMessageId id = new MailMessageId("INBOX", 1, 1);
        assertThrows(IllegalArgumentException.class,
                () -> reader.downloadAttachment(id, -1, new ByteArrayOutputStream()));

        reader.close();
        reader.close();

        assertThrows(IllegalStateException.class,
                () -> reader.listMessages(MailQuery.firstPage(1)));
        assertThrows(IllegalStateException.class, () -> reader.readMessage(id));
        assertThrows(IllegalStateException.class,
                () -> reader.downloadAttachment(id, 0, new ByteArrayOutputStream()));
    }

    private static SmtpServerConfig smtp() {
        return new SmtpServerConfig(
                "localhost", 2525, MailSecurityMode.PLAINTEXT,
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    private static ImapServerConfig imap() {
        return new ImapServerConfig(
                "localhost", 1143, MailSecurityMode.PLAINTEXT, "INBOX",
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1));
    }
}
