# 密码哈希与密钥工具概览

`lava-crypto` 提供密码哈希, EC 密钥生成和密钥格式读写能力, 统一入口是 `CryptoUtil`.

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-crypto`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-crypto</artifactId>
</dependency>
```

## 模块能力

| 能力     | 入口             | 文档                                                  |
|----------|------------------|-------------------------------------------------------|
| 密码哈希 | `PasswordHasher` | [PasswordHasher 密码哈希](./password-hasher.md)       |
| EC 密钥  | `EcKeyGenerator` | [EC 密钥生成与读取](./ec-keys.md)                     |
| 密钥格式 | `CryptoUtil`     | [PEM/DER/JKS/PKCS12 速查](./key-format-cheatsheet.md) |

## 快速示例

```java
import com.zhengshuyun.lava.crypto.CryptoUtil;
import com.zhengshuyun.lava.crypto.EcCurves;

import java.security.KeyPair;

public class CryptoQuickStartDemo {

    public static void main(String[] args) {
        String passwordHash = CryptoUtil.defaultPasswordHasher().hash("P@ssw0rd!");
        boolean verified = CryptoUtil.defaultPasswordHasher().verify("P@ssw0rd!", passwordHash);

        KeyPair keyPair = CryptoUtil.ecKeyGenerator()
                .setCurve(EcCurves.SECP256R1)
                .build()
                .generate();
        String publicPem = CryptoUtil.toPem(keyPair.getPublic());

        // TODO: 按业务处理 verified/publicPem
    }
}
```

## 安全建议

- 登录密码场景请使用 `hash/verify`, 不要用可逆加密替代.
- 生产环境不要硬编码私钥和口令, 建议接入 KMS 或配置中心.
- 不要把明文密码, 私钥, token 写入日志.
