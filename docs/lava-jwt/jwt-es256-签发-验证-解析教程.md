# JWT ES256 签发-验证-解析教程

本教程面向已经理解 JWT 基础流程的开发者, 重点讲清楚如何在 `lava-jwt` 中正确落地 `ES256`.

## 先记住 4 个关键点

- `ES256` = `ECDSA` + `P-256(secp256r1)`.
- 签发用私钥, 验签用公钥.
- `decode(...)` 仅解码结构, 不能替代 `verify(...)`.
- 私钥不要硬编码, 不要打印日志, 生产环境建议接入 KMS.

## 核心流程

```java
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.zhengshuyun.lava.crypto.CryptoUtil;
import com.zhengshuyun.lava.crypto.EcCurves;
import com.zhengshuyun.lava.jwt.JwtUtil;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class JwtEs256Demo {

    public static void main(String[] args) {
        // 1) 生成 P-256 密钥对
        KeyPair keyPair = CryptoUtil.ecKeyGenerator()
                .setCurve(EcCurves.SECP256R1)
                .build()
                .generate();
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();

        // 2) 使用私钥签发 JWT
        Algorithm signAlgorithm = Algorithm.ECDSA256(publicKey, privateKey);
        String token = JwtUtil.create()
                .withIssuer("auth-service")
                .withAudience("api-gateway")
                .withSubject("user-1001")
                .withClaim("role", "admin")
                .withIssuedAt(new Date())
                .withNotBefore(new Date())
                .withExpiresAt(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)))
                .sign(signAlgorithm);

        // 3) 使用公钥验签并校验声明
        Algorithm verifyAlgorithm = Algorithm.ECDSA256(publicKey, null);
        DecodedJWT verified = JwtUtil.require(verifyAlgorithm)
                .withIssuer("auth-service")
                .withAudience("api-gateway")
                .acceptLeeway(60)
                .build()
                .verify(token);

        // 4) 如需仅查看结构可 decode(不验签)
        DecodedJWT decoded = JwtUtil.decode(token);

        // 5) 读取业务字段
        String subject = verified.getSubject();
        String role = verified.getClaim("role").asString();
        String issuer = decoded.getIssuer();

        // TODO: 按业务处理 subject/role/issuer
    }
}
```

- `Algorithm.ECDSA256(publicKey, privateKey)`: 签发时同时传公钥和私钥.
- `Algorithm.ECDSA256(publicKey, null)`: 验签时只需公钥.
- `acceptLeeway(60)`: 允许 `exp/nbf/iat` 前后各 `60s` 偏差.

## 从现有 PEM 密钥接入

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

- PEM 教程见 [生成-ec-密钥对教程](../lava-crypto/生成-ec-密钥对教程.md).
- PEM 文本中的私钥仍是敏感信息, 需要和明文密钥同级保护.

## 可以直接复用的配置

| 项目 | 建议值 | 说明 |
|------|--------|------|
| `curve` | `EcCurves.SECP256R1` | 与 `ES256` 对应 |
| `acceptLeeway` | `30~60` 秒 | 处理机器时钟轻微漂移 |
| access token 过期 | `15~30` 分钟 | 降低泄露后可利用窗口 |
| 必校验声明 | `iss`,`aud`,`exp`,`nbf` | 保证 token 来源和时效 |

## 常见错误与排查

- 使用了错误曲线(如 `SECP384R1`)却按 `ES256` 验签, 会导致签名校验失败.
- 只调用 `decode(...)` 不调用 `verify(...)`, 属于高风险实现.
- 签发和验签使用不同公私钥对, 会出现稳定验签失败.
- 明文私钥写入配置文件和日志, 需要立刻下线并轮转密钥.
