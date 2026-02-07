# JWT ES256 签发、验证、解析教程

本文演示如何使用 ES256(= ECDSA + P-256)完成 JWT 的签发、验证、解析.

## 1. 依赖

推荐通过 BOM 管理版本, 然后引入 JWT 和 Crypto 模块.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>zhengshuyun-common-jwt</artifactId>
</dependency>

<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>zhengshuyun-common-crypto</artifactId>
</dependency>
```

## 2. 准备 ES256 所需密钥

ES256 需要 EC P-256 密钥对. 下面示例用 `CryptoUtil` 生成.

```java
import com.zhengshuyun.common.crypto.CryptoUtil;
import com.zhengshuyun.common.crypto.EcCurves;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

// ES256 需要 P-256 曲线的 EC 密钥对
KeyPair keyPair = CryptoUtil.ecKeyGenerator()
        .setCurve(EcCurves.SECP256R1)
        .build()
        .generate();

// 拆出私钥和公钥, 供后续签发/验签使用
ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
```

### 如果你已经有 PEM 密钥

`PEM` 是一种文本封装格式(带 `BEGIN/END` 头尾, 中间是 Base64 内容). 很多系统会以 PEM 形式下发密钥.

```java
import com.zhengshuyun.common.crypto.CryptoUtil;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

String privatePem = """
        -----BEGIN PRIVATE KEY-----
        // TODO: 替换为你的私钥 PEM 内容
        -----END PRIVATE KEY-----
        """;

String publicPem = """
        -----BEGIN PUBLIC KEY-----
        // TODO: 替换为你的公钥 PEM 内容
        -----END PUBLIC KEY-----
        """;

// 从 PEM 文本恢复为 Java 密钥对象
ECPrivateKey privateKey = CryptoUtil.readEcPrivateKey(privatePem);
ECPublicKey publicKey = CryptoUtil.readEcPublicKey(publicPem);
```

## 3. 签发 JWT

```java
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

// ES256 签名算法: 公钥 + 私钥
Algorithm signAlgorithm = Algorithm.ECDSA256(publicKey, privateKey);

// 构建并签发 JWT
String token = JWT.create()
        // iss: 签发方
        .withIssuer("zhengshuyun")
        // aud: 目标受众
        .withAudience("api")
        // sub: 主题(通常是用户 ID)
        .withSubject("user-1001")
        // 自定义业务声明
        .withClaim("role", "admin")
        // 签发时间
        .withIssuedAt(new Date())
        // 生效时间(一般可设为当前时间)
        .withNotBefore(new Date())
        // 过期时间
        .withExpiresAt(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
        // 用私钥进行签名, 生成 token 字符串
        .sign(signAlgorithm);
```

## 4. 验证 JWT(验签 + 声明校验)

```java
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;

// 验签只需要公钥, 私钥传 null
Algorithm verifyAlgorithm = Algorithm.ECDSA256(publicKey, null);

// verify 会同时做: 签名校验 + 你配置的声明校验
DecodedJWT verified = JWT.require(verifyAlgorithm)
        // 校验 issuer 必须匹配
        .withIssuer("zhengshuyun")
        // 校验 audience 必须匹配
        .withAudience("api")
        // 允许前后各 60 秒时钟偏差(可理解为 +/-60s), 避免服务器轻微时间漂移导致误判
        .acceptLeeway(60)
        .build()
        .verify(token);

// 验证通过后再读取声明
String subject = verified.getSubject();
String role = verified.getClaim("role").asString();

// 显式读取时间声明(排查问题时很有用)
Date issuedAt = verified.getIssuedAt();
Date notBefore = verified.getNotBefore();
Date expiresAt = verified.getExpiresAt();
```

说明:

- 验签只需要公钥, 所以 `Algorithm.ECDSA256(publicKey, null)` 是常见做法.
- `verify(...)` 默认会校验时间声明(如 `exp` 和 `nbf`), 失败会抛异常.
- `acceptLeeway(60)` 表示允许前后各 60 秒偏差(可理解为 +/-60s):
  - 对 `exp` 来说, 过期后 60 秒内仍可通过.
  - 对 `nbf`/`iat` 来说, 可提前最多 60 秒通过.

## 5. 解析 JWT(仅解码, 不验签)

```java
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

// decode 只解析结构, 不会校验签名和过期时间
DecodedJWT decoded = JWT.decode(token);
String rawIssuer = decoded.getIssuer();
String rawSubject = decoded.getSubject();
```

说明:

- `JWT.decode(...)` 只做结构解析, 不验证签名和有效期.
- 安全敏感流程必须使用 `verify(...)`, 不能只 `decode(...)`.

## 6. 一段可直接参考的完整示例

```java
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.zhengshuyun.common.crypto.CryptoUtil;
import com.zhengshuyun.common.crypto.EcCurves;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class JwtEs256Demo {

    public static void main(String[] args) {
        // 1) 生成 ES256 对应的 EC(P-256) 密钥对
        KeyPair keyPair = CryptoUtil.ecKeyGenerator()
                .setCurve(EcCurves.SECP256R1)
                .build()
                .generate();

        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();

        // 2) 用私钥签发 JWT
        Algorithm signAlgorithm = Algorithm.ECDSA256(publicKey, privateKey);
        String token = JWT.create()
                .withIssuer("zhengshuyun")
                .withAudience("api")
                .withSubject("user-1001")
                .withClaim("role", "admin")
                .withIssuedAt(new Date())
                .withNotBefore(new Date())
                .withExpiresAt(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)))
                .sign(signAlgorithm);

        // 3) 用公钥验证 JWT
        Algorithm verifyAlgorithm = Algorithm.ECDSA256(publicKey, null);
        DecodedJWT verified = JWT.require(verifyAlgorithm)
                .withIssuer("zhengshuyun")
                .withAudience("api")
                .acceptLeeway(60)
                .build()
                .verify(token);

        // 4) 如需仅查看载荷内容, 可 decode(不验签)
        DecodedJWT decoded = JWT.decode(token);

        // 5) 读取业务字段
        String subject = verified.getSubject();
        String role = verified.getClaim("role").asString();
        String issuerFromDecode = decoded.getIssuer();
        Date issuedAt = verified.getIssuedAt();
        Date expiresAt = verified.getExpiresAt();

        // TODO: 在业务代码中使用 subject/role/issuerFromDecode/issuedAt/expiresAt
    }
}
```

## 7. 注意事项

- `EC` 是密钥体系, `ECDSA` 是签名算法. ES256 代表 ECDSA + P-256.
- ES256 推荐搭配 `EcCurves.SECP256R1`, 与 `Algorithm.ECDSA256(...)` 对应.
- PEM 是密钥的文本封装格式, 不是加密本身. 详情可参考 [生成-ec-密钥对教程](../zhengshuyun-common-crypto/生成-ec-密钥对教程.md).
- 私钥只用于签发, 公钥用于验证, 私钥不要落日志或提交到仓库.
- 生产环境建议将私钥存入 KMS/密钥管理系统, 不要硬编码在代码中.
