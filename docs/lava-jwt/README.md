# JWT 教程

`lava-jwt` 对外提供统一 JWT 签发、验证、解析 API，支持 HS256、RS256、ES256 等多种签名算法。重点讲通用用法，具体到某种签名算法时再看对应专项教程。

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-jwt`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-jwt</artifactId>
</dependency>
```

如果你选择 ES256/ES384/ES512 等椭圆曲线算法, 通常还会用到 `lava-crypto` 生成和读取密钥:

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-crypto</artifactId>
</dependency>
```

## 先建立一个整体认知

JWT 在业务里通常分 3 步:

1. 签发: 服务端生成 token.
2. 验证: 服务端验签并校验声明.
3. 解析: 读取声明字段给业务使用.

不管你选 HS256、RS256 还是 ES256, 这 3 步都不变, 变的是"算法和密钥".

## 通用签发示例

```java
import com.auth0.jwt.algorithms.Algorithm;
import com.zhengshuyun.lava.jwt.JwtUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class JwtIssueDemo {

    public static void main(String[] args) {
        // 创建签名算法, 根据具体场景选择 HS256/RS256/ES256
        Algorithm algorithm = createAlgorithm();

        // 构建 JWT 并签发
        String token = JwtUtil.create()
                .withIssuer("your-system")           // 签发者标识, 填系统名称或服务名
                .withAudience("your-api")            // 受众标识, 填接收方标识
                .withSubject("user-1001")            // 主题标识, 填用户ID或业务主键
                .withClaim("role", "admin")          // 自定义声明, 存业务数据
                .withIssuedAt(new Date())            // 签发时间
                .withExpiresAt(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)))  // 过期时间(30分钟后)
                .sign(algorithm);                     // 用算法签名

        System.out.println("token: " + token);
    }

    // 这里演示 HS256, 生产环境可用 RS256/ES256
    private static Algorithm createAlgorithm() {
        return Algorithm.HMAC256("your-secret-key");
    }
}
```

## 通用验证示例

```java
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.zhengshuyun.lava.jwt.JwtUtil;

public class JwtVerifyDemo {

    public static void main(String[] args) {
        String token = "your-token";

        // 创建签名算法, 必须与签发时一致
        Algorithm algorithm = createAlgorithm();

        // 验证 JWT 并获取解码结果
        DecodedJWT decoded = JwtUtil.require(algorithm)
                .withIssuer("your-system")      // 验证签发者
                .withAudience("your-api")       // 验证受众
                .acceptLeeway(60)               // 允许时间偏差60秒(处理机器时钟不同步)
                .build()
                .verify(token);                 // 执行验证(签名+声明)

        // 提取声明数据
        String subject = decoded.getSubject();              // 获取主题
        String role = decoded.getClaim("role").asString(); // 获取自定义声明
    }

    // 这里演示 HS256, 验证时必须与签发时使用相同算法和密钥
    private static Algorithm createAlgorithm() {
        return Algorithm.HMAC256("your-secret-key");
    }
}
```

## 仅解析(不验签)

```java
import com.auth0.jwt.interfaces.DecodedJWT;
import com.zhengshuyun.lava.jwt.JwtUtil;

public class JwtDecodeDemo {

    public static void main(String[] args) {
        String token = "your-token";

        // 仅解析 JWT 结构, 不验证签名和有效期
        DecodedJWT decoded = JwtUtil.decode(token);

        // 读取声明字段
        String issuer = decoded.getIssuer();    // 签发者
        String subject = decoded.getSubject(); // 主题
    }
}
```

注意:

- `decode(...)` 只做结构解析, 不验证签名和有效期.
- 安全敏感流程必须走 `verify(...)`.

## 算法怎么选

| 算法    | 密钥形态             | 典型场景    | 说明                 |
|-------|------------------|---------|--------------------|
| HS256 | 共享密钥(同一把 secret) | 单体/内部服务 | 简单, 但签发和验签都能拿到同一密钥 |
| RS256 | RSA 私钥签发 + 公钥验签  | 多服务验签   | 公私钥分离明确, 生态成熟      |
| ES256 | EC 私钥签发 + 公钥验签   | 性能与安全平衡 | 密钥更短, 常用于现代系统      |

建议:

- 内部简单场景可先 HS256.
- 多系统分发公钥验签时优先 RS256/ES256.
- 你们已在用 EC 体系时, 直接 ES256 很自然.

## 生产环境必做项

- 私钥或共享密钥不要硬编码, 放 KMS 或密钥管理系统.
- access token 设置合理过期时间(例如 15-30 分钟).
- 统一校验 `iss`、`aud`、`exp`、`nbf`.
- 设置 `acceptLeeway(...)` 处理机器间轻微时钟偏差.
- 不要把完整 token 打到日志, 至少做脱敏.

## 相关阅读

- ES256 专项教程: [jwt-es256-签发-验证-解析教程](./jwt-es256-签发-验证-解析教程.md).
- EC 密钥教程: [生成-ec-密钥对教程](../lava-crypto/生成-ec-密钥对教程.md).
