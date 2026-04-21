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

package com.zhengshuyun.lava.mail.internal;

import com.zhengshuyun.lava.mail.MailAddress;
import com.zhengshuyun.lava.mail.MailQuery;
import com.zhengshuyun.lava.mail.MailSendRequest;
import jakarta.mail.Flags;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.SearchTerm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MailSearchTermFactory 单元测试
 *
 * @author Toint
 * @since 2026/4/21
 */
@DisplayName("MailSearchTermFactory 单元测试")
class MailSearchTermFactoryTest {

    @Test
    @DisplayName("create() - 无筛选条件时应返回 null")
    void testCreateWithoutCriteria() {
        SearchTerm searchTerm = MailSearchTermFactory.create(MailQuery.builder().build());
        assertNull(searchTerm);
    }

    @Test
    @DisplayName("create() - 多条件查询应只匹配目标邮件")
    void testCreateWithMultipleCriteria() throws Exception {
        MailQuery query = MailQuery.builder()
                .setUnreadOnly(true)
                .setFrom("sender@qq.com")
                .setSubjectContains("验证码")
                .setReceivedAfter(Instant.parse("2026-04-20T00:00:00Z"))
                .setReceivedBefore(Instant.parse("2026-04-22T00:00:00Z"))
                .build();

        SearchTerm searchTerm = MailSearchTermFactory.create(query);
        assertNotNull(searchTerm);

        MimeMessage matched = createMessage("sender@qq.com", "你的验证码", Instant.parse("2026-04-21T08:00:00Z"), false);
        MimeMessage wrongSender = createMessage("other@qq.com", "你的验证码", Instant.parse("2026-04-21T08:00:00Z"), false);
        MimeMessage readMessage = createMessage("sender@qq.com", "你的验证码", Instant.parse("2026-04-21T08:00:00Z"), true);

        assertTrue(searchTerm.match(matched));
        assertFalse(searchTerm.match(wrongSender));
        assertFalse(searchTerm.match(readMessage));
    }

    private static MimeMessage createMessage(String from,
                                             String subject,
                                             Instant receivedAt,
                                             boolean seen) throws Exception {
        MailSendRequest request = MailSendRequest.builder()
                .setFrom(MailAddress.builder().setAddress(from).build())
                .addTo(MailAddress.builder().setAddress("receiver@qq.com").build())
                .setSubject(subject)
                .setTextBody("body")
                .build();

        TestMimeMessage message = new TestMimeMessage(
                MimeMessageFactory.create(Session.getInstance(new Properties()), request)
        );
        message.setReceivedDateForTest(Date.from(receivedAt));
        message.setFlag(Flags.Flag.SEEN, seen);
        return message;
    }

    private static final class TestMimeMessage extends MimeMessage {

        private Date receivedDate;

        private TestMimeMessage(MimeMessage source) throws Exception {
            super(source);
        }

        @Override
        public Date getReceivedDate() {
            return receivedDate;
        }

        private void setReceivedDateForTest(Date receivedDate) {
            this.receivedDate = receivedDate;
        }
    }
}
