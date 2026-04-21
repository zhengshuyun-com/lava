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

package com.zhengshuyun.lava.mail;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhengshuyun.lava.json.JsonUtil;
import com.zhengshuyun.lava.mail.provider.MailProviderPreset;
import com.zhengshuyun.lava.mail.provider.MailProviders;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hotmail 手工联调测试
 * <p>
 * 这个类刻意保留真实联调写法, 方便直接照着抄:
 * <p>
 * - 预置收信: `MailProviders.hotmail() + MailReader`
 * - 预置发信: `MailProviders.hotmail() + MailSender`
 * - 手配收信: `ImapServerConfig + OAuth2RefreshTokenCredential + MailReader`
 * - 手配发信: `SmtpServerConfig + OAuth2RefreshTokenCredential + MailSender`
 * <p>
 * 该测试默认禁用, 需要手动去掉 `@Disabled` 后再执行.
 *
 * @author Toint
 * @since 2026/4/21
 */
@Disabled("手工联调测试, 需要真实 Hotmail 凭证和外网环境")
@DisplayName("Hotmail 手工联调测试")
class ManualHotmailReceiveTest {

    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualHotmailReceiveTest.class);

    /**
     * Hotmail 邮箱地址 (买的测试邮箱, 只能收不能发, 以供测试)
     */
    private static final String USERNAME = "CarmeloKristlik@hotmail.com";

    /**
     * OAuth clientId
     */
    private static final String CLIENT_ID = "9e5f94bc-e8a4-4e73-b8be-63364c29d753";

    /**
     * OAuth refreshToken
     */
    private static final String REFRESH_TOKEN = "M.C518_BL2.0.U.-Cnw3ae!cZeK9P8ypWE8eHHoBKBJy5w2fWw9QzKlBEZ4tfIIAIpDOdbutpr6DluuywAV9NeVuG4UnHCzFO76awUoq!iocnesFUfGCj0kXotpdjlHc9*RwOqNSUTbSzCWTfnicx!A6KdN2QAjnZV3PFd0KRH6PtLsmFPxxl6*tOqRUSmlWwm9uJ67yaqUp1xFk96LAsoj4NUMil0AHgHwYKtTn3Li8uwiNaLLV*!IncSsPEbrcK2Rzmjb5p8cA01zFfOsjYzQCev4rsdugyEH!4L5r106YaPaY8K7gjpPOPpF8zLP5H9uH1ly8CykCEWmqeFc96VRJGLjtR7C8gzsZZAEfJdo81XwHhNQ*LlWkJ8kpAJi5kvygpoLxPXSmLkVB0gSt*q9w3oibLKbuLBPDRaIXmwcZaXIWj94cgl2TjUH8vAp3a1eeSNzCzFodzx!5OQ$$";

    /**
     * Hotmail IMAP 主机
     */
    private static final String IMAP_HOST = "outlook.office365.com";

    /**
     * Hotmail SMTP 主机
     */
    private static final String SMTP_HOST = "smtp-mail.outlook.com";

    /**
     * 微软公共租户 token endpoint
     */
    private static final String TOKEN_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/v2.0/token";

    /**
     * 手工发信时默认发给自己, 这样联调最省事
     */
    private static final String RECIPIENT_ADDRESS = USERNAME;

    /**
     * 使用厂商预置完成收信
     */
    @Test
    @DisplayName("listMessages() - 预置方式应能读取 INBOX 最近邮件")
    void testListInboxMessages() {
        // 先按预置方式创建一个已经带好 Hotmail 默认参数的收件端.
        MailReader reader = createPresetReader();
        // 再开始构建本次收信查询对象.
        List<MailMessage> messages = reader.listMessages(MailQuery.builder()
                // folder 指定本次去收件箱读取邮件.
                .setFolder(MailFolder.INBOX)
                // limit 指定本次最多取回 10 封邮件.
                .setLimit(10)
                // includeBody=true 表示把邮件正文也解析出来.
                .setIncludeBody(true)
                // includeAttachments=false 表示这次先不拉附件, 提高联调速度.
                .setIncludeAttachments(false)
                // build() 把上面的查询条件真正组装成 MailQuery 对象.
                .build());

        // 收信联调先做一层基础断言, 确认链路是真正通的.
        assertBasicMessages(messages, 10);
        // 最后直接把消息对象序列化成 JSON, 方便手工看完整返回结构.
        LOGGER.info("预置收信结果:\n{}", JsonUtil.writeValueAsPrettyString(messages));
    }

    /**
     * 使用厂商预置完成发信
     */
    @Test
    @DisplayName("send() - 预置方式应能发送邮件")
    void testSendMessageWithPreset() {
        // 先按预置方式创建一个已经带好 Hotmail 默认参数的发件端.
        MailSender sender = createPresetSender();
        // send() 会真正连接 SMTP 并发出一封测试邮件.
        MailSendResult result = sender.send(createSendRequest("preset"));

        // 发信结果至少要确认对象不为空, 且 sentAt 已经被填充.
        assertNotNull(result);
        assertNotNull(result.getSentAt());
        // 再把返回结果打印出来, 便于确认 messageId 和服务端摘要.
        LOGGER.info("预置发信结果:\n{}", toSendResultJson(result));
    }

    /**
     * 使用手工配置完成收信
     */
    @Test
    @DisplayName("listMessages() - 手配方式应能读取 INBOX 最近邮件")
    void testListInboxMessagesWithCustomConfig() {
        // 这里故意不用预置层, 直接展示完全手配时该怎么创建收件端.
        MailReader reader = createCustomReader();
        // 查询条件和预置示例保持一致, 便于直接对比两种写法的区别.
        List<MailMessage> messages = reader.listMessages(MailQuery.builder()
                // 这里同样指定读取收件箱.
                .setFolder(MailFolder.INBOX)
                // 这里同样限制最多读取 10 封.
                .setLimit(10)
                // 这里同样要求把正文解析出来.
                .setIncludeBody(true)
                // 这里同样先不解析附件.
                .setIncludeAttachments(false)
                // build() 结束查询对象构建.
                .build());

        // 依然先做基础断言, 确认手配链路也能正常收信.
        assertBasicMessages(messages, 10);
        // 直接打印序列化后的消息列表, 方便和预置方式对照查看.
        LOGGER.info("手配收信结果:\n{}", JsonUtil.writeValueAsPrettyString(messages));
    }

    /**
     * 使用手工配置完成发信
     */
    @Test
    @DisplayName("send() - 手配方式应能发送邮件")
    void testSendMessageWithCustomConfig() {
        // 这里故意不用预置层, 直接展示完全手配时该怎么创建发件端.
        MailSender sender = createCustomSender();
        // send() 会真正连接 SMTP 并发出一封手配方式构建的测试邮件.
        MailSendResult result = sender.send(createSendRequest("manual"));

        // 基础断言依然只验证最关键的成功信号.
        assertNotNull(result);
        assertNotNull(result.getSentAt());
        // 把发信结果打印成 JSON, 便于人工查看.
        LOGGER.info("手配发信结果:\n{}", toSendResultJson(result));
    }

    /**
     * 创建基于预置层的收件端
     *
     * @return 预置方式的收件端
     */
    private static MailReader createPresetReader() {
        // 先取出 Hotmail 预置, 里面已经带好了 IMAP, SMTP 和 OAuth2 默认参数.
        MailProviderPreset provider = MailProviders.hotmail();
        // 下面开始构建一个 MailReader.
        return MailReader.builder()
                // 直接把预置里的 IMAP 配置塞给收件端.
                .setImapServerConfig(provider.getImapServerConfig())
                // 下面开始构建登录凭证.
                .setCredential(provider.createOAuth2CredentialBuilder()
                        // username 是当前 Hotmail 邮箱地址.
                        .setUsername(USERNAME)
                        // clientId 是微软 OAuth 应用的 clientId.
                        .setClientId(CLIENT_ID)
                        // refreshToken 用来换取 access token.
                        .setRefreshToken(REFRESH_TOKEN)
                        // build() 结束 OAuth2 凭证构建.
                        .build())
                // build() 结束 MailReader 构建.
                .build();
    }

    /**
     * 创建基于预置层的发件端
     *
     * @return 预置方式的发件端
     */
    private static MailSender createPresetSender() {
        // 先取出 Hotmail 预置, 里面已经带好了 SMTP 默认参数.
        MailProviderPreset provider = MailProviders.hotmail();
        // 下面开始构建一个 MailSender.
        return MailSender.builder()
                // 直接把预置里的 SMTP 配置塞给发件端.
                .setSmtpServerConfig(provider.getSmtpServerConfig())
                // 下面开始构建登录凭证.
                .setCredential(provider.createOAuth2CredentialBuilder()
                        // username 是当前 Hotmail 邮箱地址.
                        .setUsername(USERNAME)
                        // clientId 是微软 OAuth 应用的 clientId.
                        .setClientId(CLIENT_ID)
                        // refreshToken 用来换取 access token.
                        .setRefreshToken(REFRESH_TOKEN)
                        // build() 结束 OAuth2 凭证构建.
                        .build())
                // build() 结束 MailSender 构建.
                .build();
    }

    /**
     * 创建完全手工配置的收件端
     *
     * @return 手配方式的收件端
     */
    private static MailReader createCustomReader() {
        // 手配方式下, 需要自己把 IMAP 主机, 端口, 安全模式和默认文件夹写全.
        return MailReader.builder()
                // 先手工构建 IMAP 配置对象.
                .setImapServerConfig(ImapServerConfig.builder()
                        // host 指向 Hotmail 的 IMAP 服务器.
                        .setHost(IMAP_HOST)
                        // port=993 是 Hotmail IMAP 的标准 SSL 端口.
                        .setPort(993)
                        // SSL_TLS 表示连接建立时直接走 TLS.
                        .setSecurityMode(MailSecurityMode.SSL_TLS)
                        // 默认文件夹设为 INBOX, 也就是收件箱.
                        .setDefaultFolder(MailFolder.INBOX)
                        // build() 结束 IMAP 配置构建.
                        .build())
                // 再把手工构建的 OAuth2 凭证塞给收件端.
                .setCredential(createCustomCredential())
                // build() 结束 MailReader 构建.
                .build();
    }

    /**
     * 创建完全手工配置的发件端
     *
     * @return 手配方式的发件端
     */
    private static MailSender createCustomSender() {
        // 手配方式下, 需要自己把 SMTP 主机, 端口和安全模式写全.
        return MailSender.builder()
                // 先手工构建 SMTP 配置对象.
                .setSmtpServerConfig(SmtpServerConfig.builder()
                        // host 指向 Hotmail 的 SMTP 服务器.
                        .setHost(SMTP_HOST)
                        // port=587 是 STARTTLS 常用端口.
                        .setPort(587)
                        // STARTTLS 表示先建普通连接, 再升级为 TLS.
                        .setSecurityMode(MailSecurityMode.STARTTLS)
                        // build() 结束 SMTP 配置构建.
                        .build())
                // 再把手工构建的 OAuth2 凭证塞给发件端.
                .setCredential(createCustomCredential())
                // build() 结束 MailSender 构建.
                .build();
    }

    /**
     * 创建手工配置的 OAuth2 凭证
     *
     * @return OAuth2 凭证
     */
    private static OAuth2RefreshTokenCredential createCustomCredential() {
        // 这里完整展示非预置场景下的 OAuth2 凭证长什么样.
        // 账号名, clientId, refreshToken, tokenEndpoint 和 scopes 都要自己提供.
        return OAuth2RefreshTokenCredential.builder()
                // username 对应要登录的 Hotmail 邮箱地址.
                .setUsername(USERNAME)
                // clientId 对应微软 OAuth 应用标识.
                .setClientId(CLIENT_ID)
                // refreshToken 用于换取短期 access token.
                .setRefreshToken(REFRESH_TOKEN)
                // tokenEndpoint 指向微软的换 token 地址.
                .setTokenEndpoint(TOKEN_ENDPOINT)
                // offline_access 允许后续继续拿 refresh token 续期.
                .addScope("offline_access")
                // IMAP.AccessAsUser.All 允许通过 IMAP 读取邮件.
                .addScope("https://outlook.office.com/IMAP.AccessAsUser.All")
                // SMTP.Send 允许通过 SMTP 发邮件.
                .addScope("https://outlook.office.com/SMTP.Send")
                // build() 结束 OAuth2 凭证构建.
                .build();
    }

    /**
     * 创建一封用于手工联调的邮件
     *
     * @param scenario 场景名, 例如 preset 或 manual
     * @return 发信请求
     */
    private static MailSendRequest createSendRequest(String scenario) {
        // 发信请求里最关键的是发件人, 收件人, 主题和正文这几项.
        return MailSendRequest.builder()
                // 发件人地址必须和当前登录账号一致, personal 是展示名称.
                .setFrom(MailAddress.builder()
                        // setAddress() 设置真实发件邮箱地址.
                        .setAddress(USERNAME)
                        // setPersonal() 设置对方客户端看到的显示名称.
                        .setPersonal("Lava Mail Manual Test")
                        // build() 结束发件人地址对象构建.
                        .build())
                // 手工联调默认发给自己, 这样最容易立刻验证收发闭环.
                .addTo(MailAddress.builder()
                        // 收件地址这里直接写自己, 方便马上去 INBOX 验证.
                        .setAddress(RECIPIENT_ADDRESS)
                        // build() 结束收件人地址对象构建.
                        .build())
                // 主题里带上场景名和时间, 方便你直接去邮箱搜索定位这封邮件.
                .setSubject("lava-mail manual test - " + scenario + " - " + Instant.now())
                // 纯文本正文用于验证 text body 是否正常发送.
                .setTextBody("This is a manual mail test sent by lava-mail.")
                // HTML 正文用于验证 multipart/alternative 结构是否正常发送.
                .setHtmlBody("<p>This is a manual mail test sent by <strong>lava-mail</strong>.</p>")
                // build() 结束发信请求构建.
                .build();
    }

    /**
     * 断言收信结果基本有效
     *
     * @param messages 读取到的邮件
     * @param limit    读取上限
     */
    private static void assertBasicMessages(List<MailMessage> messages, int limit) {
        // 第一层先确认方法返回的列表对象本身不是 null.
        assertNotNull(messages);
        // 第二层确认确实读到了邮件, 否则后面的字段断言就没有意义了.
        assertFalse(messages.isEmpty(), "未读取到任何邮件, 请先确认收件箱中已有邮件");
        // 第三层确认 limit 生效, 不应该返回超出上限的消息数量.
        assertTrue(messages.size() <= limit);

        // 再抓首封邮件做字段级断言, 这是最直观的结果样本.
        MailMessage firstMessage = messages.getFirst();
        // 主题应该至少存在, 否则说明解析结果不完整.
        assertNotNull(firstMessage.getSubject());
        // 发件人列表对象也应该存在.
        assertNotNull(firstMessage.getFromList());
        // 发件人列表至少应该有一个地址.
        assertFalse(firstMessage.getFromList().isEmpty(), "首封邮件缺少发件人信息");
    }

    /**
     * 将发信结果转换为 JSON 字符串
     *
     * @param result 发信结果
     * @return 便于手工联调查看的 JSON
     */
    private static String toSendResultJson(MailSendResult result) {
        // 发信结果的关键字段并不多, 这里单独拼一个简洁 JSON 就够了.
        ObjectNode root = JsonUtil.createObjectNode();
        // messageId 方便和服务端或后续日志关联.
        root.put("messageId", result.getMessageId());
        // sentAt 能直接看出本次发信的完成时间.
        root.put("sentAt", result.getSentAt().toString());
        // responseSummary 用来快速确认是通过哪个 host 发出去的.
        root.put("responseSummary", result.getResponseSummary());
        // 最后输出漂亮格式 JSON, 方便人工查看.
        return JsonUtil.writeValueAsPrettyString(root);
    }
}
