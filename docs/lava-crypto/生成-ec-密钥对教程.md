# 生成 EC 密钥对教程

本教程面向需要落地 `ES256/ES384/ES512` 或其他 EC 场景的开发者, 重点覆盖生成, 导出, 恢复三步.

## 先记住 3 个关键点

- `EC` 是密钥体系, `ECDSA` 是签名算法, 两者不是同一个概念.
- `CryptoUtil.toPem(...)` 导出的是标准 PEM 文本, 私钥为 PKCS#8, 公钥为 X.509.
- 私钥不要硬编码, 不要打印日志, 生产环境建议接入 KMS.

## 核心流程

```java
import com.zhengshuyun.lava.crypto.CryptoUtil;
import com.zhengshuyun.lava.crypto.EcCurves;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public class EcKeyPairDemo {

    public static void main(String[] args) {
        // 1) 选择曲线并生成 EC 密钥对
        KeyPair keyPair = CryptoUtil.ecKeyGenerator()
                .setCurve(EcCurves.SECP256R1)
                .build()
                .generate();

        // 2) 导出 PEM 字符串
        String privatePem = CryptoUtil.toPem(keyPair.getPrivate());
        String publicPem = CryptoUtil.toPem(keyPair.getPublic());

        // 3) 从 PEM 恢复为 Java 密钥对象
        ECPrivateKey privateKey = CryptoUtil.readEcPrivateKey(privatePem);
        ECPublicKey publicKey = CryptoUtil.readEcPublicKey(publicPem);

        // TODO: 按业务处理 privateKey/publicKey
    }
}
```

- 推荐优先使用 `EcCurves.SECP256R1`, 与 `ES256` 对应, 兼容性最好.
- 生成后的密钥可直接用于 JWT 场景.

## 可以直接复用的曲线配置

| 曲线常量 | 别名 | 典型算法 | 说明 |
|----------|------|----------|------|
| `EcCurves.SECP256R1` | P-256 | `ES256` | 默认推荐, 兼容性最佳 |
| `EcCurves.SECP384R1` | P-384 | `ES384` | 安全强度更高, 签名更大 |
| `EcCurves.SECP521R1` | P-521 | `ES512` | 强度更高, 计算和传输成本更高 |

## 从已有 PEM 文本恢复

```java
import com.zhengshuyun.lava.crypto.CryptoUtil;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

String privatePem = """
        -----BEGIN PRIVATE KEY-----
        // TODO: 替换为真实私钥内容
        -----END PRIVATE KEY-----
        """;
String publicPem = """
        -----BEGIN PUBLIC KEY-----
        // TODO: 替换为真实公钥内容
        -----END PUBLIC KEY-----
        """;

ECPrivateKey privateKey = CryptoUtil.readEcPrivateKey(privatePem);
ECPublicKey publicKey = CryptoUtil.readEcPublicKey(publicPem);
```

- PEM 头尾必须匹配, 否则会抛 `CryptoException`.
- 如果你在排查格式问题, 建议先对照 [pem-der-jks-pkcs12-区别速查表](./pem-der-jks-pkcs12-区别速查表.md).

## 常见错误与排查

- 把 `EC` 密钥误称为 "ECDSA 密钥", 导致团队沟通和配置混乱.
- 曲线与算法不匹配, 例如用 `SECP384R1` 却按 `ES256` 验签.
- PEM 中多了无关字符或换行异常, 导致 Base64 解码失败.
- 私钥明文存仓库或日志, 需要立即轮转密钥并排查泄露范围.
