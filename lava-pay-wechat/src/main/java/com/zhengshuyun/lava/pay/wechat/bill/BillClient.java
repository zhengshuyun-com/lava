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

package com.zhengshuyun.lava.pay.wechat.bill;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.http.HttpStream;
import com.zhengshuyun.lava.pay.wechat.*;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayTransport;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * 微信支付交易账单、资金账单申请与安全下载客户端。
 */
public final class BillClient {
    private static final String TRADE_BILL_PATH = "/v3/bill/tradebill";
    private static final String FUND_FLOW_BILL_PATH = "/v3/bill/fundflowbill";

    private final WechatPayTransport transport;
    private final Runnable openCheck;

    /**
     * 由根客户端创建账单入口。
     *
     * @param transport 共享协议传输层
     * @param openCheck 根客户端存活检查
     */
    public BillClient(WechatPayTransport transport, Runnable openCheck) {
        this.transport = ValidationUtils.requireNonNull(transport, "transport");
        this.openCheck = ValidationUtils.requireNonNull(openCheck, "openCheck");
    }

    /**
     * 申请交易账单下载信息。
     *
     * @param request 交易账单参数
     * @return 已验签下载信息
     */
    public BillDownloadInfo applyTradeBill(TradeBillRequest request) {
        openCheck.run();
        ValidationUtils.requireNonNull(request, "request must not be null");
        URI uri = transport.query(transport.endpoint(TRADE_BILL_PATH), "bill_date",
                request.billDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (request.billType() != null) {
            uri = transport.query(uri, "bill_type", request.billType().name());
        }
        if (request.tarType() != null) {
            uri = transport.query(uri, "tar_type", request.tarType().name());
        }
        BillInfoPayload payload = transport.get(uri, BillInfoPayload.class);
        return payload.toPublic(request.tarType());
    }

    /**
     * 申请资金账单下载信息。
     *
     * @param request 资金账单参数
     * @return 已验签下载信息
     */
    public BillDownloadInfo applyFundFlowBill(FundFlowBillRequest request) {
        openCheck.run();
        ValidationUtils.requireNonNull(request, "request must not be null");
        URI uri = transport.query(transport.endpoint(FUND_FLOW_BILL_PATH), "bill_date",
                request.billDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (request.accountType() != null) {
            uri = transport.query(uri, "account_type", request.accountType().name());
        }
        if (request.tarType() != null) {
            uri = transport.query(uri, "tar_type", request.tarType().name());
        }
        BillInfoPayload payload = transport.get(uri, BillInfoPayload.class);
        return payload.toPublic(request.tarType());
    }

    /**
     * 将账单流式下载到目标路径并校验 SHA-1。目标已存在时拒绝覆盖，GZIP 文件保持原样。
     *
     * @param info 申请账单返回的已验签下载信息
     * @param target 目标文件路径
     * @return 最终路径、大小和实际摘要
     */
    public BillDownloadResult download(BillDownloadInfo info, Path target) {
        openCheck.run();
        ValidationUtils.requireNonNull(info, "info must not be null");
        ValidationUtils.requireNonNull(target, "target must not be null");
        if (!"SHA1".equalsIgnoreCase(info.hashType())) {
            throw new WechatPayProtocolException("微信支付返回了不支持的账单摘要类型");
        }

        Path absoluteTarget = target.toAbsolutePath().normalize();
        if (Files.exists(absoluteTarget)) {
            throw new WechatPayFileException(WechatPayFileFailure.TARGET_EXISTS, "");
        }
        Path parent = absoluteTarget.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new WechatPayFileException(WechatPayFileFailure.INVALID_TARGET, "");
        }

        Path temporary = null;
        try {
            String fileName = absoluteTarget.getFileName().toString();
            temporary = Files.createTempFile(parent, "." + fileName + ".", ".part");
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            // 1. 账单响应按官方规则不含签名，完整性由申请接口返回的 SHA-1 保证。
            try (HttpStream stream = transport.openDownload(info.downloadUrl());
                 InputStream input = stream.body();
                 OutputStream output = Files.newOutputStream(temporary,
                         StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read == 0) {
                        continue;
                    }
                    digest.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                }
            }

            // 2. 摘要不一致时绝不发布部分或被篡改的账单文件。
            String actualHash = HexFormat.of().formatHex(digest.digest());
            if (!actualHash.equalsIgnoreCase(info.hashValue())) {
                throw new WechatPaySecurityException(WechatPaySecurityFailure.HASH_MISMATCH);
            }

            // 3. 摘要通过后再发布目标文件；文件系统不支持原子移动时仍保持不覆盖语义。
            try {
                Files.move(temporary, absoluteTarget, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, absoluteTarget);
            }
            temporary = null;
            return new BillDownloadResult(absoluteTarget, Files.size(absoluteTarget),
                    "SHA1", actualHash);
        } catch (FileAlreadyExistsException exception) {
            throw new WechatPayFileException(WechatPayFileFailure.TARGET_EXISTS,
                    exception.getClass().getName());
        } catch (WechatPayException exception) {
            throw exception;
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new WechatPayFileException(WechatPayFileFailure.IO,
                    exception.getClass().getName());
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // 主失败已经完整表达，清理失败不能覆盖原始诊断信息。
                }
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BillInfoPayload(
            @JsonProperty("hash_type") String hashType,
            @JsonProperty("hash_value") String hashValue,
            @JsonProperty("download_url") URI downloadUrl) {

        private BillInfoPayload {
            ValidationUtils.requireNotBlank(hashType, "hashType must not be blank");
            ValidationUtils.requireNotBlank(hashValue, "hashValue must not be blank");
            ValidationUtils.requireNonNull(downloadUrl, "downloadUrl must not be null");
        }

        private BillDownloadInfo toPublic(@Nullable BillTarType tarType) {
            return new BillDownloadInfo(hashType, hashValue, downloadUrl, tarType);
        }
    }
}
