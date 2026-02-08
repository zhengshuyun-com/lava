# JWT 签发与验签教程

`lava-jwt` 对外提供统一 JWT 签发, 验证, 解析 API, 底层封装 `java-jwt`.

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-jwt`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-jwt</artifactId>
</dependency>
```

如果你使用 `ES256/ES384/ES512`, 通常还需要 `lava-crypto` 来生成和读取 EC 密钥.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-crypto</artifactId>
</dependency>
```

## 最小可运行示例

```java
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.zhengshuyun.lava.jwt.JwtUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class JwtQuickStartDemo {

    public static void main(String[] args) {
        // 1) 准备签名算法(示例使用 HS256)
        Algorithm algorithm = Algorithm.HMAC256("replace-with-strong-secret");

        // 2) 签发 token
        String token = JwtUtil.create()
                .withIssuer("order-center")
                .withAudience("order-api")
                .withSubject("user-1001")
                .withClaim("role", "admin")
                .withIssuedAt(new Date())
                .withExpiresAt(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)))
                .sign(algorithm);

        // 3) 验证 token 并读取声明
        DecodedJWT verified = JwtUtil.require(algorithm)
                .withIssuer("order-center")
                .withAudience("order-api")
                .acceptLeeway(60)
                .build()
                .verify(token);

        String subject = verified.getSubject();
        String role = verified.getClaim("role").asString();

        // TODO: 按业务处理 token/subject/role
    }
}
```

- `JwtUtil.create()`: 进入签发链路.
- `JwtUtil.require(...)`: 进入验签链路.
- `acceptLeeway(60)`: 允许前后 `60s` 时钟偏差.

## 签发与验证流程

### 签发

```java
String token = JwtUtil.create()
        .withIssuer("auth-service")
        .withAudience("mobile-app")
        .withSubject("user-2001")
        .withClaim("tenant", "cn-hz")
        .withIssuedAt(new Date())
        .withNotBefore(new Date())
        .withExpiresAt(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
        .sign(algorithm);
```

- `iss`/`aud`/`sub` 建议作为基础声明固定校验.
- 过期时间建议短周期, 减少 token 泄露后的可用窗口.

### 验证

```java
DecodedJWT jwt = JwtUtil.require(algorithm)
        .withIssuer("auth-service")
        .withAudience("mobile-app")
        .acceptLeeway(30)
        .build()
        .verify(token);
```

- 验证包括签名校验和声明校验.
- 校验失败会抛异常, 建议在网关或鉴权层统一转换为业务错误码.

### 仅解析(不验签)

```java
DecodedJWT decoded = JwtUtil.decode(token);
String issuer = decoded.getIssuer();
String subject = decoded.getSubject();
```

- `decode(...)` 只做结构解析, 不保证 token 可信.
- 安全敏感流程必须使用 `verify(...)`.

## 算法选择建议

| 算法 | 密钥形态 | 典型场景 | 建议 |
|------|----------|----------|------|
| `HS256` | 单个共享密钥 | 单体或内网少量服务 | 简单易用, 但签发方和验签方都持有密钥 |
| `RS256` | RSA 私钥签发 + 公钥验签 | 多服务分发公钥验签 | 生态成熟, 密钥管理清晰 |
| `ES256` | EC 私钥签发 + 公钥验签 | 高并发或追求更短密钥 | 性能和安全性平衡较好 |

## 常见坑与排查建议

- `decode(...)` 当成验证接口使用, 会绕过签名和过期校验.
- 签发和验证算法不一致, 常见于 `HS256` 与 `RS256` 混用.
- 没有校验 `iss`/`aud`, 容易把其他系统签发的 token 当作合法 token.
- 私钥或共享密钥硬编码在仓库中, 存在严重泄露风险.

## 生产环境建议

- 密钥不要硬编码, 不要打印日志, 建议接入 KMS 或配置中心.
- 统一设置 access token 过期时间(如 `15~30` 分钟).
- 鉴权层统一捕获验证异常, 输出脱敏日志和稳定错误码.

## 相关阅读

- ES256 专项教程: [jwt-es256-签发-验证-解析教程](./jwt-es256-签发-验证-解析教程.md).
- EC 密钥教程: [生成-ec-密钥对教程](../lava-crypto/生成-ec-密钥对教程.md).
