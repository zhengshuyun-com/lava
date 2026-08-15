# JwtUtil 签发与验签

`JwtUtil` 是 JWT 签发, 验证和解析的统一入口.

## 最小可运行示例

```java
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.zhengshuyun.lava.jwt.JwtUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class JwtUtilDemo {

    public static void main(String[] args) {
        Algorithm algorithm = Algorithm.HMAC256("replace-with-strong-secret");

        String token = JwtUtil.create()
                .withIssuer("auth-service")
                .withAudience("mobile-app")
                .withSubject("user-2001")
                .withExpiresAt(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .sign(algorithm);

        DecodedJWT jwt = JwtUtil.require(algorithm)
                .withIssuer("auth-service")
                .withAudience("mobile-app")
                .acceptLeeway(30)
                .build()
                .verify(token);

        // TODO: 按业务处理 token/jwt
    }
}
```

- `JwtUtil.create()`: 进入签发链路.
- `JwtUtil.require(...)`: 进入验签链路.
- `JwtUtil.decode(...)`: 只解析结构, 不验证可信性.

## 常见坑与排查建议

| 异常/消息                | 原因                             | 解决方式                       |
|--------------------------|----------------------------------|--------------------------------|
| `decode(...)` 被当成验证 | 只解析结构, 未校验签名和过期时间 | 安全流程必须使用 `verify(...)` |
| 验签失败                 | 算法, 密钥或声明不一致           | 检查签发端和验证端配置         |
| token 泄露风险大         | 过期时间过长                     | access token 建议短周期        |
