# 账单

模块支持交易账单和资金账单申请，并负责安全下载、可选 GZIP 解压和 SHA-1 完整性校验。

## 交易账单

```java
BillDownloadInfo info = client.bills().applyTradeBill(
        TradeBillRequest.builder()
                .billDate(LocalDate.of(2026, 8, 28))
                .billType(TradeBillType.ALL)
                .build()
);
```

## 资金账单

```java
BillDownloadInfo info = client.bills().applyFundFlowBill(
        FundFlowBillRequest.builder()
                .billDate(LocalDate.of(2026, 8, 28))
                .accountType(FundFlowAccountType.BASIC)
                .build()
);
```

账单日期必须早于当天，且不早于最近三个月。

## 下载

```java
BillDownloadResult result = client.bills().download(
        info,
        Path.of("/data/bills/2026-08-28.csv")
);
```

下载流程：

1. 在目标目录创建临时文件；
2. 流式下载，避免账单整体进入内存；
3. 请求 GZIP 时先解压；
4. 计算账单原文 SHA-1；
5. 与申请账单返回的摘要比较；
6. 校验成功后发布目标文件。

目标文件已存在时拒绝覆盖。模块不解析 CSV 字段，调用方仍需负责账单入库、对账和差异处理。

下载 URL 含临时 token，不得写入日志。摘要不一致时抛出 `WechatPaySecurityException`，临时文件不会作为成功账单发布。
