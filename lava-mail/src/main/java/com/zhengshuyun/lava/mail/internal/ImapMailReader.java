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

import com.zhengshuyun.lava.mail.MailException;
import com.zhengshuyun.lava.core.lang.Validate;
import com.zhengshuyun.lava.mail.ImapServerConfig;
import com.zhengshuyun.lava.mail.MailCredential;
import com.zhengshuyun.lava.mail.MailMessage;
import com.zhengshuyun.lava.mail.MailQuery;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.SearchTerm;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * IMAP 收件读取器
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class ImapMailReader {

    /**
     * 拉取邮件列表
     * <p>
     * 整个收信链路分成 4 步:
     * <p>
     * - 先根据配置和凭证建立 IMAP 连接
     * - 再定位要读取的文件夹并以只读方式打开
     * - 然后按查询条件选出目标消息
     * - 最后把 Jakarta Mail 的消息对象解析成 Lava 自己的消息模型
     *
     * @param session     IMAP 会话
     * @param config      IMAP 服务器配置
     * @param credential  登录凭证
     * @param query       收信查询条件
     * @param accessToken OAuth2 access token, 密码登录场景可为空
     * @return 满足查询条件的邮件列表
     * @throws IllegalArgumentException 入参为空时抛出
     * @throws MailException            IMAP 协议调用或消息解析失败时抛出
     */
    public List<MailMessage> listMessages(Session session,
                                          ImapServerConfig config,
                                          MailCredential credential,
                                          MailQuery query,
                                          @Nullable String accessToken) {
        Validate.notNull(session, "session must not be null");
        Validate.notNull(config, "config must not be null");
        Validate.notNull(credential, "credential must not be null");
        Validate.notNull(query, "query must not be null");

        Store store = null;
        Folder folder = null;
        try {
            // 先建立 IMAP Store, 这里底层既可能是密码登录, 也可能是 XOAUTH2 登录.
            store = session.getStore("imap");
            store.connect(
                    config.getHost(),
                    config.getPort(),
                    credential.getUsername(),
                    MailSessionFactory.resolvePassword(credential, accessToken)
            );

            // 再打开目标文件夹. 收信默认只读, 避免手工查询时误改已读状态或其他元数据.
            folder = store.getFolder(resolveFolderName(config, query));
            folder.open(Folder.READ_ONLY);

            // 先尽量交给服务端做筛选, 这样可以减少不必要的邮件拉取和正文解析.
            SearchTerm searchTerm = MailSearchTermFactory.create(query);
            List<Message> messageList = selectMessages(folder, searchTerm, query.getLimit());

            // 最后统一把协议层对象转成 Lava 自己的消息模型, 对外隐藏 Jakarta Mail 细节.
            return parseMessages(messageList, query);
        } catch (MailException e) {
            throw e;
        } catch (Exception e) {
            throw new MailException("Failed to list mail messages", e);
        } finally {
            // 连接和文件夹都是短生命周期资源, 每次调用结束后都立即释放.
            closeFolder(folder);
            closeStore(store);
        }
    }

    /**
     * 选出需要读取的消息
     * <p>
     * 无筛选条件时, 只读取文件夹尾部的 limit 区间, 避免把整个文件夹都拉进来.
     * 有筛选条件时, 服务端仍会返回全部命中结果, 这里再用本地 topN 收口成最新 limit 条.
     *
     * @param folder     IMAP 文件夹
     * @param searchTerm 查询条件
     * @param limit      最大条数
     * @return 已按接收时间倒序排好的消息列表
     * @throws Exception 读取消息失败时抛出
     */
    private static List<Message> selectMessages(Folder folder,
                                                @Nullable SearchTerm searchTerm,
                                                int limit) throws Exception {
        if (searchTerm == null) {
            // 无筛选条件时直接取文件夹尾部区间, 这是最省事也最省流量的路径.
            int messageCount = folder.getMessageCount();
            if (messageCount <= 0) {
                return List.of();
            }

            int start = Math.max(1, messageCount - limit + 1);
            return sortMessagesByReceivedTime(folder.getMessages(start, messageCount));
        }

        // 有筛选条件时先让服务端做第一轮过滤, 但多数 IMAP 实现仍会返回全部命中消息.
        Message[] matchedMessages = folder.search(searchTerm);
        if (matchedMessages.length <= limit) {
            return sortMessagesByReceivedTime(matchedMessages);
        }

        // 只保留最新的 limit 条消息, 避免对大结果集做全量排序.
        PriorityQueue<Message> latestMessageQueue = new PriorityQueue<>(
                Comparator.comparing(ImapMailReader::safeReceivedTime)
        );
        for (Message message : matchedMessages) {
            if (latestMessageQueue.size() < limit) {
                latestMessageQueue.offer(message);
                continue;
            }

            Message oldestMessage = latestMessageQueue.peek();
            long currentReceivedTime = safeReceivedTime(message);
            long oldestReceivedTime = oldestMessage == null ? 0L : safeReceivedTime(oldestMessage);
            if (oldestMessage != null && currentReceivedTime > oldestReceivedTime) {
                latestMessageQueue.poll();
                latestMessageQueue.offer(message);
            }
        }

        List<Message> result = new ArrayList<>(latestMessageQueue);
        result.sort(Comparator.comparing(ImapMailReader::safeReceivedTime).reversed());
        return List.copyOf(result);
    }

    /**
     * 按接收时间倒序排列消息
     *
     * @param messages 消息数组
     * @return 倒序消息列表
     */
    private static List<Message> sortMessagesByReceivedTime(Message[] messages) {
        // 先复制成可变列表, 再按接收时间倒序排序, 最后返回不可变结果.
        List<Message> result = new ArrayList<>(Arrays.asList(messages));
        result.sort(Comparator.comparing(ImapMailReader::safeReceivedTime).reversed());
        return List.copyOf(result);
    }

    /**
     * 解析要读取的文件夹名
     * <p>
     * 查询对象显式指定了文件夹时优先使用查询值, 否则退回到连接配置里的默认文件夹.
     *
     * @param config IMAP 服务器配置
     * @param query  查询条件
     * @return 实际读取的文件夹名
     */
    private static String resolveFolderName(ImapServerConfig config, MailQuery query) {
        return query.getFolder() == null ? config.getDefaultFolder() : query.getFolder();
    }

    /**
     * 将 Jakarta Mail 消息对象转换成 Lava 自己的消息模型
     *
     * @param messageList 选中的消息列表
     * @param query       查询条件
     * @return 已完成解析的消息列表
     */
    private static List<MailMessage> parseMessages(List<Message> messageList, MailQuery query) {
        List<MailMessage> result = new ArrayList<>(messageList.size());
        for (Message message : messageList) {
            // Angus/Jakarta Mail 在这里理论上应返回 MimeMessage, 这里额外做一次防御性校验.
            if (!(message instanceof MimeMessage mimeMessage)) {
                throw new MailException("Unexpected message type=" + message.getClass().getName());
            }

            // 是否解析正文和附件完全由查询条件控制, 避免手工查询时默认做过重的解析.
            result.add(MailMessageParser.parse(
                    mimeMessage,
                    query.isIncludeBody(),
                    query.isIncludeAttachments()
            ));
        }
        return List.copyOf(result);
    }

    /**
     * 安全地读取消息接收时间
     * <p>
     * IMAP 消息不保证一定带有 receivedDate, 因此这里先取 receivedDate, 再退回 sentDate.
     * 如果两者都没有, 或读取过程中出现异常, 则统一按 0 处理, 避免排序阶段中断.
     *
     * @param message 邮件消息
     * @return 可用于排序的时间戳
     */
    private static long safeReceivedTime(Message message) {
        try {
            // 优先使用 receivedDate, 没有时再退回 sentDate, 保证排序尽量稳定.
            if (message.getReceivedDate() != null) {
                return message.getReceivedDate().getTime();
            }

            if (message.getSentDate() != null) {
                return message.getSentDate().getTime();
            }
            return 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 安静地关闭文件夹
     *
     * @param folder IMAP 文件夹, 允许为空
     */
    private static void closeFolder(@Nullable Folder folder) {
        if (folder != null && folder.isOpen()) {
            try {
                folder.close(false);
            } catch (Exception ignored) {
                // 关闭阶段没有补救动作, 这里忽略异常避免覆盖主异常.
            }
        }
    }

    /**
     * 安静地关闭 Store
     *
     * @param store IMAP Store, 允许为空
     */
    private static void closeStore(@Nullable Store store) {
        if (store != null && store.isConnected()) {
            try {
                store.close();
            } catch (Exception ignored) {
                // 关闭阶段没有补救动作, 这里忽略异常避免覆盖主异常.
            }
        }
    }
}
