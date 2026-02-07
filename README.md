# Lava — 让 Java 基础设施开发更简单、更安全、更一致.

- [GitHub](https://github.com/zhengshuyun-com/lava)
- [Issues](https://github.com/zhengshuyun-com/lava/issues)

## 模块结构

当前仓库包含以下模块:

| 模块            | 说明                                                                | 文档                                     |
|---------------|-------------------------------------------------------------------|----------------------------------------|
| lava-bom      | Lava 依赖版本清单                                                       | [README](docs/lava-bom/README.md)      |
| lava-core     | 核心工具, 基于 [Guava](https://github.com/google/guava)                 | [README](docs/lava-core/README.md)     |
| lava-crypto   | 加密与哈希工具, 基于 [BouncyCastle](https://github.com/bcgit/bc-java)      | [README](docs/lava-crypto/README.md)   |
| lava-http     | HTTP 客户端封装, 基于 [OkHttp](https://github.com/square/okhttp)         | [README](docs/lava-http/README.md)     |
| lava-json     | JSON 序列化与反序列化, 基于 [Jackson](https://github.com/FasterXML/jackson) | [README](docs/lava-json/README.md)     |
| lava-jwt      | JWT 生成与验证, 基于 [Auth0](https://github.com/auth0/java-jwt)          | [README](docs/lava-jwt/README.md)      |
| lava-schedule | 任务调度, 基于 [Quartz](https://github.com/quartz-scheduler/quartz)     | [README](docs/lava-schedule/README.md) |

## 依赖引入

- [Maven Central](https://central.sonatype.com/search?q=zhengshuyun.com:lava)

### 方式一: 使用 BOM 统一版本管理 (推荐)

在 `pom.xml` 中添加 BOM:

```xml
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
```

然后按需引入模块 (无需指定版本):

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>模块名称</artifactId>
</dependency>
```

### 方式二: 直接引入 (不使用 BOM)

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>模块名称</artifactId>
    <version>${version}</version>
</dependency>
```
