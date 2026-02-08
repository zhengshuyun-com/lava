# lava-crypto 使用教程

`lava-crypto` 提供密码哈希（Argon2id）和密钥工具（EC 密钥对生成、PEM 导出）两大能力。先读本文建立整体认知，再按场景阅读同目录的专项文档。

## 模块定位

`lava-crypto` 提供两类核心能力:

- 密码哈希: 基于 Argon2id 的密码加密与校验.
- 密钥工具: EC 密钥对生成、PEM 导出与恢复.

统一入口是 `CryptoUtil`.

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-crypto`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-crypto</artifactId>
</dependency>
```

## 快速上手

### 密码哈希(登录密码场景)

```java
import com.zhengshuyun.lava.crypto.CryptoUtil;
import com.zhengshuyun.lava.crypto.PasswordHasher;

// 1) 构建 PasswordHasher(可单例复用)
PasswordHasher hasher = CryptoUtil.passwordHasher()
        .setMemoryKiB(65536)
        .setIterations(3)
        .setParallelism(1)
        .setSaltLengthBytes(16)
        .setHashLengthBytes(32)
        .build();

// 2) 注册时: 生成哈希后落库
String passwordHash = hasher.hash(rawPassword);

// 3) 登录时: 读取哈希并校验
boolean ok = hasher.verify(rawPassword, passwordHash);
```

关键点:

- 注册时只存 `passwordHash`, 不存明文密码.
- `hash(...)` 每次会自动生成随机盐.
- `verify(...)` 会根据哈希串中的参数完成校验.

### EC 密钥对与 PEM

```java
import com.zhengshuyun.lava.crypto.CryptoUtil;
import com.zhengshuyun.lava.crypto.EcCurves;

import java.security.KeyPair;

// 1) 生成 EC 密钥对(P-256)
KeyPair keyPair = CryptoUtil.ecKeyGenerator()
        .setCurve(EcCurves.SECP256R1)
        .build()
        .generate();

// 2) 导出为 PEM 文本
String privatePem = CryptoUtil.toPem(keyPair.getPrivate());
String publicPem = CryptoUtil.toPem(keyPair.getPublic());

// 3) 从 PEM 恢复为 Java 密钥对象
var privateKey = CryptoUtil.readEcPrivateKey(privatePem);
var publicKey = CryptoUtil.readEcPublicKey(publicPem);
```

## 场景与文档导航

| 你的目标                      | 推荐文档                                                      |
|---------------------------|-----------------------------------------------------------|
| 用户登录密码加密落库                | [argon2id-用户登录密码加密落库指南](./argon2id-用户登录密码加密落库指南.md)       |
| 生成 EC 密钥对并导出 PEM          | [生成-ec-密钥对教程](./生成-ec-密钥对教程.md)                           |
| 搞清 PEM/DER/JKS/PKCS#12 概念 | [pem-der-jks-pkcs12-区别速查表](./pem-der-jks-pkcs12-区别速查表.md) |

## 安全建议

- 私钥和密码相关数据不要写日志.
- 不要把密钥硬编码在代码里, 生产环境建议接入 KMS.
- 对密码场景优先使用 Argon2id, 不要用可逆加密代替.
