# lava-crypto

`lava-crypto` 提供常用密码学基础能力：

- HMAC-SHA-256；
- RSA-SHA256 签名与验签；
- AES-GCM 加解密；
- Argon2id 密码哈希；
- JDK EC 密钥生成；
- EC/RSA PEM 编码与严格解析。

## 添加依赖

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-crypto</artifactId>
</dependency>
```

无状态算法既可以从 `CryptoUtils` 统一进入，也可以使用 `AesGcmUtils`、`HmacUtils`、`RsaSignatureUtils`、`EcKeyUtils` 和 `PemKeyUtils`。带策略状态的密码哈希由 `PasswordHasher` 提供。

模块使用 Bouncy Castle lightweight Argon2 API，但不会向 JVM 全局注册 Bouncy Castle Provider。标准 RSA、AES 和 EC 能力通过 JCA 获取。

继续查看 [密码哈希](./password)、[HMAC、RSA 与 AES-GCM](./algorithms) 和 [密钥与 PEM](./keys-pem)。
