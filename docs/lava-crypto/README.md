# 密码哈希与密钥工具教程

`lava-crypto` 提供 Argon2id 密码哈希和 EC 密钥工具, 统一入口是 `CryptoUtil`.

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-crypto`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-crypto</artifactId>
</dependency>
```

## 最小可运行示例

```java
import com.zhengshuyun.lava.crypto.CryptoUtil;
import com.zhengshuyun.lava.crypto.EcCurves;
import com.zhengshuyun.lava.crypto.PasswordHasher;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public class CryptoQuickStartDemo {

    public static void main(String[] args) {
        // 1) 构建密码哈希器并执行注册哈希
        PasswordHasher hasher = CryptoUtil.passwordHasher()
                .setMemoryKiB(65536)
                .setIterations(3)
                .setParallelism(1)
                .setSaltLengthBytes(16)
                .setHashLengthBytes(32)
                .build();
        String encodedHash = hasher.hash("P@ssw0rd!");

        // 2) 登录时校验密码
        boolean verified = hasher.verify("P@ssw0rd!", encodedHash);

        // 3) 生成 EC 密钥对并导出 PEM
        KeyPair keyPair = CryptoUtil.ecKeyGenerator()
                .setCurve(EcCurves.SECP256R1)
                .build()
                .generate();
        String privatePem = CryptoUtil.toPem(keyPair.getPrivate());
        String publicPem = CryptoUtil.toPem(keyPair.getPublic());

        // 4) 从 PEM 恢复密钥对象
        ECPrivateKey privateKey = CryptoUtil.readEcPrivateKey(privatePem);
        ECPublicKey publicKey = CryptoUtil.readEcPublicKey(publicPem);

        // TODO: 按业务处理 verified/privateKey/publicKey
    }
}
```

- `PasswordHasher` 是不可变且线程安全对象, 可单例复用.
- `hash(...)` 输出完整 PHC 字符串, 直接落库即可.
- `toPem(...)` 私钥输出 PKCS#8, 公钥输出 X.509.

## 密码哈希(Argon2id)

```java
// 注册时生成哈希
String passwordHash = CryptoUtil.defaultPasswordHasher().hash(rawPassword);

// 登录时校验
boolean ok = CryptoUtil.defaultPasswordHasher().verify(rawPassword, passwordHash);
```

- `defaultPasswordHasher()` 使用默认参数 `m=65536,t=3,p=1`.
- `verify(...)` 会按哈希串内参数验证, 参数升级后旧哈希仍可校验.

## EC 密钥与 PEM

```java
import com.zhengshuyun.lava.crypto.CryptoUtil;
import com.zhengshuyun.lava.crypto.EcCurves;

import java.security.KeyPair;

// 1) 生成 P-256 密钥对
KeyPair keyPair = CryptoUtil.ecKeyGenerator()
        .setCurve(EcCurves.SECP256R1)
        .build()
        .generate();

// 2) 导出 PEM 文本
String privatePem = CryptoUtil.toPem(keyPair.getPrivate());
String publicPem = CryptoUtil.toPem(keyPair.getPublic());
```

- `SECP256R1` 对应 JWT 场景常用 `ES256`.
- PEM 仅是文本封装格式, 不是加密本身.

## 高级用法

### 参数调优参考

| 参数 | 默认值 | 建议值 | 说明 |
|------|--------|--------|------|
| `memoryKiB` | `65536` | `65536` 起步 | 越大越抗暴力破解, 也越耗内存 |
| `iterations` | `3` | `3` 起步 | 越大越慢, 需结合登录 RT 评估 |
| `parallelism` | `1` | `1~4` | 根据 CPU 核数和并发压测结果调整 |
| `saltLengthBytes` | `16` | `16` | 建议不低于 `16` |
| `hashLengthBytes` | `32` | `32` | 常见配置, 兼顾长度和成本 |

### 文档导航

| 目标 | 文档 |
|------|------|
| 用户登录密码加密落库 | [argon2id-用户登录密码加密落库指南](./argon2id-用户登录密码加密落库指南.md) |
| 生成和恢复 EC 密钥 | [生成-ec-密钥对教程](./生成-ec-密钥对教程.md) |
| 快速区分 PEM/DER/JKS/PKCS#12 | [pem-der-jks-pkcs12-区别速查表](./pem-der-jks-pkcs12-区别速查表.md) |

## 常见坑与排查建议

- 把明文密码, 私钥, token 写入日志是高风险行为, 必须禁止.
- 登录密码场景请用 `hash/verify`, 不要用可逆加密替代.
- PEM 读取失败优先检查头尾标记和 Base64 内容是否完整.
- `setMemoryKiB` 等参数超出模块上限会在构建阶段抛 `IllegalArgumentException`.

## 实践建议

- 生产环境不要硬编码私钥和口令, 建议接入 KMS 或配置中心.
- 密码哈希参数升级建议采用登录后重哈希策略, 平滑迁移历史数据.
- 对外接口返回敏感字段时, 避免回传完整哈希串和私钥内容.
