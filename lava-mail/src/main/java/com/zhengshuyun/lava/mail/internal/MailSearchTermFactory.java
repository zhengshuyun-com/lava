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

import com.zhengshuyun.lava.core.lang.Validate;
import com.zhengshuyun.lava.mail.MailQuery;
import jakarta.mail.Flags;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.FromStringTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MailQuery 转 SearchTerm 工厂
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MailSearchTermFactory {

    private MailSearchTermFactory() {
    }

    /**
     * 将查询条件转换为 SearchTerm
     *
     * @return 无筛选条件时返回 null
     */
    public static @Nullable SearchTerm create(MailQuery query) {
        Validate.notNull(query, "query must not be null");

        List<SearchTerm> searchTermList = new ArrayList<>();
        if (query.isUnreadOnly()) {
            searchTermList.add(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        }
        if (query.getFrom() != null && !query.getFrom().isBlank()) {
            searchTermList.add(new FromStringTerm(query.getFrom()));
        }
        if (query.getSubjectContains() != null && !query.getSubjectContains().isBlank()) {
            searchTermList.add(new SubjectTerm(query.getSubjectContains()));
        }
        if (query.getReceivedAfter() != null) {
            searchTermList.add(new ReceivedDateTerm(
                    ComparisonTerm.GE,
                    Date.from(query.getReceivedAfter())
            ));
        }
        if (query.getReceivedBefore() != null) {
            searchTermList.add(new ReceivedDateTerm(
                    ComparisonTerm.LE,
                    Date.from(query.getReceivedBefore())
            ));
        }

        if (searchTermList.isEmpty()) {
            return null;
        }
        if (searchTermList.size() == 1) {
            return searchTermList.getFirst();
        }
        return new AndTerm(searchTermList.toArray(SearchTerm[]::new));
    }
}
