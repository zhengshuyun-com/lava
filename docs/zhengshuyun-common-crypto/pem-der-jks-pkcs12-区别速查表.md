# PEM、DER、JKS、PKCS#12 区别速查表

这 4 个词经常一起出现, 但它们不是一个维度:

- `PEM`、`DER` 更偏向编码/表示格式.
- `JKS`、`PKCS#12` 更偏向密钥库容器格式.

## 1. 一眼区分

| 名称 | 类型 | 常见扩展名 | 可包含私钥 | 可包含证书链 | 是否可加密/加口令 |
| --- | --- | --- | --- | --- | --- |
| PEM | 文本编码格式(Base64 + BEGIN/END) | `.pem`, `.key`, `.crt`, `.cer` | 可以 | 可以 | 可以(取决于内容) |
| DER | 二进制编码格式 | `.der`, `.cer`, `.crt` | 可以 | 可以 | 通常不直接体现, 取决于对象类型 |
| JKS | Java KeyStore 容器 | `.jks` | 可以 | 可以 | 可以 |
| PKCS#12 | 通用密钥库容器标准 | `.p12`, `.pfx` | 可以 | 可以 | 可以 |

## 2. 常见使用场景

- `PEM`: 人工查看、配置文件粘贴、跨系统传输最常见.
- `DER`: 与部分系统/硬件接口对接时常用(更紧凑的二进制).
- `JKS`: 老 Java 项目常见.
- `PKCS#12`: 现代 Java 和跨语言系统更推荐, 兼容性更好.

## 3. 和本项目有什么关系?

- `CryptoUtil.toPem(...)` 产出的是 `PEM` 文本.
- `CryptoUtil.readEcPrivateKey(...)`/`readEcPublicKey(...)` 读取的是 `PEM` 文本.
- 做 JWT ES256 时, 你可以直接使用 PEM 形式的 EC 密钥.

## 4. 常见转换命令

下面只给最常用转换, 方便排障.

### PEM -> DER (私钥, PKCS#8)

```bash
openssl pkcs8 -topk8 -inform PEM -outform DER -in private.pem -out private.der -nocrypt
```

### DER -> PEM (私钥, PKCS#8)

```bash
openssl pkcs8 -inform DER -outform PEM -in private.der -out private.pem -nocrypt
```

### PKCS#12 -> JKS

```bash
keytool -importkeystore \
  -srckeystore keystore.p12 -srcstoretype PKCS12 \
  -destkeystore keystore.jks -deststoretype JKS
```

### JKS -> PKCS#12

```bash
keytool -importkeystore \
  -srckeystore keystore.jks -srcstoretype JKS \
  -destkeystore keystore.p12 -deststoretype PKCS12
```

## 5. 常见误区

- 误区 1: PEM 就是加密. 不是, PEM 只是文本封装.
- 误区 2: `.cer` 一定是 DER. 不一定, `.cer` 可能是 DER 也可能是 PEM.
- 误区 3: JKS 比 PKCS#12 更通用. 现在通常反过来, PKCS#12 跨语言更友好.

## 6. 实践建议

- 对外交换和长期存储优先考虑 `PKCS#12` 或规范化 `PEM`.
- 在应用配置中使用 PEM 时, 私钥必须走密钥管理系统或密文配置.
- 先确认“格式(PEM/DER)”和“容器(JKS/PKCS#12)”两个维度, 再谈转换.
