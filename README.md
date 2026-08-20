# Lava 2.0

Lava 是面向 Java 25 的基础设施工具库，提供 ID、重试、受限 IO、JSON、HTTP/SSE、协议中立 AI 请求、进程内调度、密码哈希/EC
密钥和邮件收发能力。2.0 是一次破坏性重构：公开 API 以实例化、不可变配置、明确资源所有权和默认有界为原则。

当前工作区版本为 `2.0.0-SNAPSHOT`。

- [GitHub](https://github.com/zhengshuyuncom/lava)
- [Issues](https://github.com/zhengshuyuncom/lava/issues)

## 模块

| 模块                                       | 定位                                                      |
|--------------------------------------------|-----------------------------------------------------------|
| [`lava-bom`](lava-bom/README.md)           | 独立 BOM，统一全部 Lava 模块版本                          |
| [`lava-core`](lava-core/README.md)         | UUIDv7/Snowflake、重试、受限 IO、时间和校验               |
| [`lava-json`](lava-json/README.md)         | 基于 Jackson 3 的线程安全 `JsonCodec`                     |
| [`lava-http`](lava-http/README.md)         | 基于 OkHttp 5 的实例级 HTTP 客户端、流式响应和 SSE        |
| [`lava-ai`](lava-ai/README.md)             | 协议中立的 AI JSON 请求与 SSE 增量流便利层                |
| [`lava-schedule`](lava-schedule/README.md) | 有界并发的纯进程内调度器；Quartz 仅用于 Cron 计算         |
| [`lava-crypto`](lava-crypto/README.md)     | Argon2id、JDK EC 密钥生成和严格 PEM 处理                  |
| [`lava-mail`](lava-mail/README.md)         | 基于 Angus/Jakarta Mail 的 SMTP、UID 分页 IMAP 和 OAuth 2 |

2.0 不包含 `lava-jwt`，也不提供 Spring、Spring Boot、starter 或自动配置模块。

## 环境

- JDK 25 或更高版本；产物以 `--release 25` 编译
- Maven 3.9.12 或更高版本

```bash
mvn --version
```

## 引入依赖

推荐只导入 `lava-bom`，再按需声明模块。BOM 只管理版本，不会把任何模块加入运行时类路径。

```xml
<properties>
    <lava.version>2.0.0</lava.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.zhengshuyun</groupId>
            <artifactId>lava-bom</artifactId>
            <version>${lava.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.zhengshuyun</groupId>
        <artifactId>lava-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zhengshuyun</groupId>
        <artifactId>lava-http</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zhengshuyun</groupId>
        <artifactId>lava-ai</artifactId>
    </dependency>
</dependencies>
```

不使用 BOM 时，必须在每个 Lava 依赖上显式写同一个版本。声明 Lava 模块后，其生产依赖会按 Maven 规则传递；BOM 只管理 Lava
模块，不会覆盖应用选择的第三方依赖版本。完整清单见 [`lava-bom`](lava-bom/README.md)。

## 设计约定

- 使用 `@NullMarked` 的包默认不接受或返回 `null`，可空位置由 JSpecify 显式标注。
- 客户提供的流和执行器默认视为 borrowed；Lava 自己打开或创建的资源由 Lava 关闭。
- 网络响应、邮件 MIME、任务并发和队列均有默认上限；超出限制会暴露结构化失败，而不是静默无限分配。
- HTTP 4xx/5xx 是正常 `HttpResponse`；DNS、TLS、超时、取消等传输失败才抛 `HttpException`。
- `lava-schedule` 不提供持久化、进程重启恢复或集群一致性。

## 构建

第一次构建允许联网填充 Wrapper 和 Maven 缓存：

```bash
mvn -B -ntp dependency:go-offline
```

随后用与 CI 相同的离线验收：

```bash
mvn -B -ntp --offline clean verify
```

`verify` 会使用 JDK 25 编译并执行全部测试。普通构建不会签名或连接 Central；sources、Javadoc、GPG 和 Central 发布只存在于显式的
`release` profile。

## 许可证

[Apache License 2.0](LICENSE)
