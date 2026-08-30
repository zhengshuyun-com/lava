# EC、RSA 与 PEM

## EC 密钥生成

```java
KeyPair defaultPair = CryptoUtils.ecGenerateKeyPair();
KeyPair p256 = CryptoUtils.ecGenerateKeyPair(EcKeyUtils.Curve.P256);
KeyPair p384 = CryptoUtils.ecGenerateKeyPair(EcKeyUtils.Curve.P384);
KeyPair p521 = CryptoUtils.ecGenerateKeyPair(EcKeyUtils.Curve.P521);
```

需要确定性测试或自定义熵源时，可使用接受 `SecureRandom` 的重载。

## PEM 编码

```java
String publicPem = CryptoUtils.pemEncode(pair.getPublic());
String privatePem = CryptoUtils.pemEncode(pair.getPrivate());
```

`pemEncode(...)` 产生未加密私钥文本。调用方必须限制其存储、日志和传输范围。

HSM 或 PKCS#11 不可导出密钥的 `getFormat()` / `getEncoded()` 可能为空，此时抛出 `CryptoException`。不要为了导出而降低 HSM 策略。

## 严格读取

```java
ECPublicKey ecPublicKey = CryptoUtils.pemReadEcPublicKey(publicPem);
ECPrivateKey ecPrivateKey = CryptoUtils.pemReadEcPrivateKey(privatePem);

RSAPublicKey rsaPublicKey = CryptoUtils.pemReadRsaPublicKey(rsaPublicPem);
RSAPrivateKey rsaPrivateKey = CryptoUtils.pemReadRsaPrivateKey(rsaPrivatePem);
```

支持的格式：

| 密钥 | 格式 | PEM 标签 |
| --- | --- | --- |
| EC 私钥 | PKCS#8 | `PRIVATE KEY` |
| EC 公钥 | X.509 SubjectPublicKeyInfo | `PUBLIC KEY` |
| RSA 私钥 | PKCS#8 | `PRIVATE KEY` |
| RSA 公钥 | X.509 SubjectPublicKeyInfo | `PUBLIC KEY` |

解析器限制 PEM 字符数和 DER 字节数，要求唯一且匹配的 header/footer，并校验算法和格式。

不支持传统 `EC PRIVATE KEY`、证书、多个拼接块或加密 PEM。需要这些格式时，应在受控边界使用专门工具转换或解析，并明确私钥暴露风险。
