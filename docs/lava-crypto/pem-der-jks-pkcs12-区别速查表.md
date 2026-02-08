# PEM-DER-JKS-PKCS12 区别速查表

用于快速区分密钥编码格式和密钥库容器格式, 避免排查时把概念混在一起.

## 格式与容器

| 名称 | 分类 | 常见扩展名 | 可含私钥 | 可含证书链 | 典型用途 |
|------|------|------------|----------|------------|----------|
| `PEM` | 文本编码格式 | `.pem`, `.key`, `.crt`, `.cer` | 可以 | 可以 | 配置文件, 跨系统交换, 人工排查 |
| `DER` | 二进制编码格式 | `.der`, `.cer`, `.crt` | 可以 | 可以 | 对接硬件或要求二进制输入的系统 |
| `JKS` | Java 密钥库容器 | `.jks` | 可以 | 可以 | 传统 Java 生态 |
| `PKCS#12` | 通用密钥库容器 | `.p12`, `.pfx` | 可以 | 可以 | Java 与跨语言系统通用 |

## 与 Lava 的对应关系

| 需求 | 对应 API | 输入/输出 |
|------|----------|-----------|
| 导出密钥文本 | `CryptoUtil.toPem(key)` | 输出 `PEM` |
| 读取 EC 私钥 | `CryptoUtil.readEcPrivateKey(pem)` | 输入 `PEM` |
| 读取 EC 公钥 | `CryptoUtil.readEcPublicKey(pem)` | 输入 `PEM` |

- `lava-crypto` 当前直接面向 PEM 文本最友好.
- JWT `ES256` 场景通常直接使用 PEM 即可.

## 常用转换命令

| 目标 | 命令 |
|------|------|
| `PEM -> DER` (PKCS#8 私钥) | `openssl pkcs8 -topk8 -inform PEM -outform DER -in private.pem -out private.der -nocrypt` |
| `DER -> PEM` (PKCS#8 私钥) | `openssl pkcs8 -inform DER -outform PEM -in private.der -out private.pem -nocrypt` |
| `PKCS#12 -> JKS` | `keytool -importkeystore -srckeystore keystore.p12 -srcstoretype PKCS12 -destkeystore keystore.jks -deststoretype JKS` |
| `JKS -> PKCS#12` | `keytool -importkeystore -srckeystore keystore.jks -srcstoretype JKS -destkeystore keystore.p12 -deststoretype PKCS12` |

## 如何选择

| 场景 | 推荐 |
|------|------|
| 应用配置中心下发密钥 | `PEM` |
| Java 老系统存量改造 | 先兼容 `JKS`, 再逐步迁移 `PKCS#12` |
| 新项目或跨语言系统 | `PKCS#12` 或规范化 `PEM` |
| 仅做二进制对接 | `DER` |

- 先确认你在讨论的是 "编码格式" 还是 "容器格式", 再决定转换路径.
- 私钥无论何种格式都属于敏感信息, 不要硬编码, 不要打印日志.
