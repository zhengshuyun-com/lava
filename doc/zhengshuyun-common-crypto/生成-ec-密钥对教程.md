# 生成 EC 密钥对教程

本文演示如何在 `zhengshuyun-common-crypto` 中生成 EC 密钥对, 并完成 PEM 导出与恢复.

## 1. 引入依赖

如果你已经使用 BOM, 只需引入 crypto 模块.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>zhengshuyun-common-crypto</artifactId>
</dependency>
```

## 2. 生成 EC 密钥对

默认曲线是 `P-256`(`secp256r1`). 也可以显式指定曲线.

```java
import com.zhengshuyun.common.crypto.CryptoUtil;
import com.zhengshuyun.common.crypto.EcCurves;

import java.security.KeyPair;

public class EcKeyPairDemo {

    public static void main(String[] args) {
        // 通过 Builder 指定曲线并生成 EC 密钥对
        KeyPair keyPair = CryptoUtil.ecKeyGenerator()
                // P-256, 对应 JWT 里的 ES256
                .setCurve(EcCurves.SECP256R1)
                .build()
                .generate();

        // 这里通常会得到 "EC", 表示密钥体系类型
        String publicAlgorithm = keyPair.getPublic().getAlgorithm();
        String privateAlgorithm = keyPair.getPrivate().getAlgorithm();

        // TODO: 在你的业务中持久化密钥或用于签名验签
    }
}
```

支持的常用曲线:

- `EcCurves.SECP256R1` (P-256, ES256).
- `EcCurves.SECP384R1` (P-384, ES384).
- `EcCurves.SECP521R1` (P-521, ES512).

## 3. 导出为 PEM 字符串

```java
import com.zhengshuyun.common.crypto.CryptoUtil;

import java.security.KeyPair;

public class ExportPemDemo {

    public static void main(String[] args) {
        // 先生成一对 EC 密钥
        KeyPair keyPair = CryptoUtil.ecKeyGenerator().build().generate();

        // 导出为标准 PEM 文本, 便于落库或写文件
        String privatePem = CryptoUtil.toPem(keyPair.getPrivate());
        String publicPem = CryptoUtil.toPem(keyPair.getPublic());

        // TODO: 将 PEM 保存到安全存储(数据库、KMS、密钥文件)
    }
}
```

- 私钥使用 PKCS#8 PEM 格式.
- 公钥使用 X.509 PEM 格式.

## 4. PEM 是什么?

`PEM`(Privacy-Enhanced Mail)是一种文本封装格式, 常用于保存密钥和证书.

- 本质是二进制数据(Base64 编码) + 头尾标记.
- 私钥常见头尾: `-----BEGIN PRIVATE KEY-----` / `-----END PRIVATE KEY-----`.
- 公钥常见头尾: `-----BEGIN PUBLIC KEY-----` / `-----END PUBLIC KEY-----`.
- 便于复制、传输、落库、写配置文件, 也更适合人眼排查.

示例:

```text
-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAExxx...
-----END PUBLIC KEY-----
```

注意:

- PEM 只是编码和封装格式, 不是加密本身.
- 私钥即使是 PEM 文本, 也必须按敏感信息管理.
- 如果你分不清 PEM/DER/JKS/PKCS#12, 可先看 [PEM、DER、JKS、PKCS#12 区别速查表](./pem-der-jks-pkcs12-区别速查表.md).

## 5. 从 PEM 恢复密钥对象

```java
import com.zhengshuyun.common.crypto.CryptoUtil;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public class RestorePemDemo {

    public static void main(String[] args) {
        // 先构造一对原始密钥并导出成 PEM
        KeyPair original = CryptoUtil.ecKeyGenerator().build().generate();
        String privatePem = CryptoUtil.toPem(original.getPrivate());
        String publicPem = CryptoUtil.toPem(original.getPublic());

        // 从 PEM 文本恢复成 Java 密钥对象
        ECPrivateKey privateKey = CryptoUtil.readEcPrivateKey(privatePem);
        ECPublicKey publicKey = CryptoUtil.readEcPublicKey(publicPem);

        // 比较关键字段, 验证往返是否一致
        boolean privateOk = privateKey.getS().equals(((ECPrivateKey) original.getPrivate()).getS());
        boolean publicOk = publicKey.getW().equals(((ECPublicKey) original.getPublic()).getW());

        // TODO: privateOk/publicOk 应为 true, 可据此验证 PEM 往返正确
    }
}
```

## 6. 注意事项

- `EC` 是椭圆曲线密钥体系, `ECDSA` 是基于 EC 密钥的签名算法, 两者不是同一个概念.
- 本教程生成的是 EC 密钥对, 可用于 `ECDSA` 签名验签, 例如 `ES256`/`ES384`/`ES512`.

## 7. 实践建议

- 推荐优先使用 `SECP256R1`, 兼容性最好.
- 私钥不要打印到日志, 也不要提交到 Git.
- `CryptoException` 一般表示参数格式错误或 PEM 内容非法, 优先检查曲线名与 PEM 头尾是否匹配.
