# 生成 ECDSA 密钥对教程

本文演示如何在 `zhengshuyun-common-crypto` 中生成 ECDSA 密钥对, 并完成 PEM 导出与恢复.

## 1. 引入依赖

如果你已经使用 BOM, 只需引入 crypto 模块.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>zhengshuyun-common-crypto</artifactId>
</dependency>
```

## 2. 生成 ECDSA 密钥对

默认曲线是 `P-256`(`secp256r1`). 也可以显式指定曲线.

```java
import com.zhengshuyun.common.crypto.CryptoUtil;
import com.zhengshuyun.common.crypto.EcCurves;

import java.security.KeyPair;

public class EcdsaKeyPairDemo {

    public static void main(String[] args) {
        KeyPair keyPair = CryptoUtil.ecKeyGenerator()
                .setCurve(EcCurves.SECP256R1)
                .build()
                .generate();

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
        KeyPair keyPair = CryptoUtil.ecKeyGenerator().build().generate();

        String privatePem = CryptoUtil.toPem(keyPair.getPrivate());
        String publicPem = CryptoUtil.toPem(keyPair.getPublic());

        // TODO: 将 PEM 保存到安全存储(数据库、KMS、密钥文件)
    }
}
```

- 私钥使用 PKCS#8 PEM 格式.
- 公钥使用 X.509 PEM 格式.

## 4. 从 PEM 恢复密钥对象

```java
import com.zhengshuyun.common.crypto.CryptoUtil;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public class RestorePemDemo {

    public static void main(String[] args) {
        KeyPair original = CryptoUtil.ecKeyGenerator().build().generate();
        String privatePem = CryptoUtil.toPem(original.getPrivate());
        String publicPem = CryptoUtil.toPem(original.getPublic());

        ECPrivateKey privateKey = CryptoUtil.readEcPrivateKey(privatePem);
        ECPublicKey publicKey = CryptoUtil.readEcPublicKey(publicPem);

        boolean privateOk = privateKey.getS().equals(((ECPrivateKey) original.getPrivate()).getS());
        boolean publicOk = publicKey.getW().equals(((ECPublicKey) original.getPublic()).getW());

        // TODO: privateOk/publicOk 应为 true, 可据此验证 PEM 往返正确
    }
}
```

## 5. 实践建议

- 推荐优先使用 `SECP256R1`, 兼容性最好.
- 私钥不要打印到日志, 也不要提交到 Git.
- `CryptoException` 一般表示参数格式错误或 PEM 内容非法, 优先检查曲线名与 PEM 头尾是否匹配.
