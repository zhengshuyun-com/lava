# zhengshuyun-common

Java 通用工具库, 基于 Guava/Jackson/OkHttp/BouncyCastle/Quartz 等国际主流框架的易用性封装.

- [GitHub](https://github.com/zhengshuyun-com/zhengshuyun-common)
- [Maven](https://central.sonatype.com/search?q=zhengshuyun-common)
- [Docs](https://github.com/zhengshuyun-com/zhengshuyun-common/blob/main/docs/index.md)

## 模块结构

当前仓库包含以下模块:

| 模块                            | 说明                                                                     |
|-------------------------------|------------------------------------------------------------------------|
| `zhengshuyun-common-bom`      | BOM模块, 统一管理依赖版本                                                        |
| `zhengshuyun-common-core`     | 核心模块, 基于 [Guava](https://github.com/google/guava) 的易用性封装               |
| `zhengshuyun-common-crypto`   | 加密模块, 基于 [BouncyCastle](https://github.com/bcgit/bc-java) 的易用性封装       |
| `zhengshuyun-common-http`     | HTTP模块, 基于 [OkHttp](https://github.com/square/okhttp) 的易用性封装           |
| `zhengshuyun-common-json`     | JSON模块, 基于 [Jackson](https://github.com/FasterXML/jackson) 的易用性封装      |
| `zhengshuyun-common-jwt`      | JWT 模块, 基于 [Auth0](https://github.com/auth0/java-jwt) 的易用性封装           |
| `zhengshuyun-common-schedule` | 定时任务模块, 基于 [Quartz](https://github.com/quartz-scheduler/quartz) 的易用性封装 |

## 依赖引入

### 使用 BOM 统一版本管理 (推荐)

在 `pom.xml` 中添加 BOM:

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.zhengshuyun</groupId>
            <artifactId>zhengshuyun-common-bom</artifactId>
            <version>${zhengshuyun-common.version}</version>
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
    <artifactId>zhengshuyun-common-core</artifactId>
</dependency>

<dependency>
<groupId>com.zhengshuyun</groupId>
<artifactId>zhengshuyun-common-json</artifactId>
</dependency>

<dependency>
<groupId>com.zhengshuyun</groupId>
<artifactId>zhengshuyun-common-http</artifactId>
</dependency>

<dependency>
<groupId>com.zhengshuyun</groupId>
<artifactId>zhengshuyun-common-crypto</artifactId>
</dependency>

<dependency>
<groupId>com.zhengshuyun</groupId>
<artifactId>zhengshuyun-common-jwt</artifactId>
</dependency>

<dependency>
<groupId>com.zhengshuyun</groupId>
<artifactId>zhengshuyun-common-schedule</artifactId>
</dependency>
```

### 直接引入 (不使用 BOM)

```xml

<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>zhengshuyun-common-core</artifactId>
    <version>${version}</version>
</dependency>
```
