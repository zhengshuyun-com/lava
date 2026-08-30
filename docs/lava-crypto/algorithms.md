# HMAC、RSA 与 AES-GCM

## HMAC-SHA-256

```java
String digest = CryptoUtils.hmacSha256Hex(pepper, plaintext);
byte[] raw = CryptoUtils.hmacSha256(keyBytes, dataBytes);
```

字符串入口固定使用 UTF-8，并返回 64 个小写十六进制字符。字节入口返回 32 字节原始摘要。

HMAC 适合消息认证，不适合直接存储用户密码。

## RSA-SHA256

```java
byte[] signature = CryptoUtils.rsaSha256Sign(privateKey, data);
boolean valid = CryptoUtils.rsaSha256Verify(publicKey, data, signature);
```

RSA 工具接受普通 JCA 密钥，也接受 HSM 或其他 Provider 提供的不可导出密钥。签名内容的规范化、字符集和字段顺序属于具体协议，调用方必须在进入签名方法前确定。

## AES-GCM

```java
byte[] ciphertext = CryptoUtils.aesGcmEncrypt(
        key,
        nonce,
        associatedData,
        plaintext
);

byte[] restored = CryptoUtils.aesGcmDecrypt(
        key,
        nonce,
        associatedData,
        ciphertext
);
```

AES-GCM 使用 128 位认证标签。`associatedData` 不加密，但会参与完整性认证，加解密两侧必须完全一致。

::: danger nonce 不能复用
同一 AES 密钥下必须保证 nonce 唯一。nonce 重复会破坏 GCM 的机密性和完整性，不能用固定值，也不能仅依赖可能回拨或碰撞的时间戳。
:::

调用方负责安全生成、存储和轮换密钥，并在使用后尽可能清理密钥和明文字节数组。
