# JWT 签发与验签概览

`lava-jwt` 对外提供统一 JWT 签发, 验证和解析入口, 底层封装 `java-jwt`.

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

## 模块能力

| 能力             | 入口                      | 文档                                   |
|------------------|---------------------------|----------------------------------------|
| 签发, 验证, 解析 | `JwtUtil`                 | [JwtUtil 签发与验签](./jwt-util.md)    |
| 算法选择         | `Algorithm`               | [JWT 算法选择](./jwt-algorithms.md)    |
| ES256 场景       | `JwtUtil` + `lava-crypto` | [JWT ES256 签发与验证](./jwt-es256.md) |

## 快速示例

```java
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.zhengshuyun.lava.jwt.JwtUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class JwtQuickStartDemo {

    public static void main(String[] args) {
        Algorithm algorithm = Algorithm.HMAC256("replace-with-strong-secret");

        String token = JwtUtil.create()
                .withIssuer("order-center")
                .withSubject("user-1001")
                .withExpiresAt(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)))
                .sign(algorithm);

        DecodedJWT verified = JwtUtil.require(algorithm)
                .withIssuer("order-center")
                .build()
                .verify(token);

        // TODO: 按业务处理 token/verified
    }
}
```

## 安全建议

- 安全敏感流程必须使用 `verify(...)`, 不要只 `decode(...)`.
- 私钥或共享密钥不要硬编码, 不要打印日志.
- 建议统一校验 `iss`, `aud`, `sub` 等基础声明.
