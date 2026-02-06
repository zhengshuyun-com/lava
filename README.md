# zhengshuyun-common

zhengshuyun-common 是 Java 通用工具库集合, 使用 JDK 25 编译.

## Maven

- [Maven 中央仓库](https://central.sonatype.com/search?q=zhengshuyun-common)

## 模块结构

当前仓库包含以下模块:

| 模块                            | 说明                                 |
|-------------------------------|------------------------------------|
| `zhengshuyun-common-bom`      | BOM (Bill of Materials), 统一管理依赖版本. |
| `zhengshuyun-common-core`     | 核心工具类, 包含重试、IO、时间、ID 生成、参数校验等.     |
| `zhengshuyun-common-crypto`   | 加密工具, 包含密码哈希、对称/非对称加密等.            |
| `zhengshuyun-common-http`     | HTTP 客户端封装, 基于 OkHttp.             |
| `zhengshuyun-common-json`     | JSON 序列化工具, 基于 Jackson.            |
| `zhengshuyun-common-jwt`      | JWT 工具, 支持 HMAC 和 ECDSA 签名.        |
| `zhengshuyun-common-schedule` | 定时任务工具, 基于 Quartz.                 |

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
