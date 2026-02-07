# Lava — 让 Java 基础设施开发更简单、更安全、更一致.

- [GitHub](https://github.com/zhengshuyun-com/lava)
- [Maven](https://central.sonatype.com/search?q=zhengshuyun.com:lava)
- [Docs](docs/)

## 模块结构

当前仓库包含以下模块:

| 模块                                  | 说明                                                                |
|-------------------------------------|-------------------------------------------------------------------|
| [lava-bom](docs/lava-bom)           | Lava 依赖版本清单                                                       |
| [lava-core](docs/lava-core)         | 核心工具, 基于 [Guava](https://github.com/google/guava) 扩展              |
| [lava-crypto](docs/lava-crypto)     | 加密与哈希工具, 基于 [BouncyCastle](https://github.com/bcgit/bc-java)      |
| [lava-http](docs/lava-http)         | HTTP 客户端封装, 基于 [OkHttp](https://github.com/square/okhttp)         |
| [lava-json](docs/lava-json)         | JSON 序列化与反序列化, 基于 [Jackson](https://github.com/FasterXML/jackson) |
| [lava-jwt](docs/lava-jwt)           | JWT 生成与验证, 基于 [Auth0](https://github.com/auth0/java-jwt)          |
| [lava-schedule](docs/lava-schedule) | 任务调度, 基于 [Quartz](https://github.com/quartz-scheduler/quartz)     |

## 依赖引入

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
    <artifactId>lava-core</artifactId>
</dependency>

<dependency>
<groupId>com.zhengshuyun</groupId>
<artifactId>lava-json</artifactId>
</dependency>

<dependency>
<groupId>com.zhengshuyun</groupId>
<artifactId>lava-http</artifactId>
</dependency>

<dependency>
<groupId>com.zhengshuyun</groupId>
<artifactId>lava-crypto</artifactId>
</dependency>

<dependency>
<groupId>com.zhengshuyun</groupId>
<artifactId>lava-jwt</artifactId>
</dependency>

<dependency>
<groupId>com.zhengshuyun</groupId>
<artifactId>lava-schedule</artifactId>
</dependency>
```

### 方式二: 直接引入 (不使用 BOM)

```xml

<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-core</artifactId>
    <version>${version}</version>
</dependency>
```
