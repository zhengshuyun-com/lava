# zhengshuyun-common

zhengshuyun-common 是 Java 通用工具库集合, 使用 JDK 25 编译.

## Maven

- [Maven 中央仓库](https://central.sonatype.com/search?q=zhengshuyun-common)

## 模块结构

当前仓库包含以下模块:

| 模块                            | 说明                             | 依赖 |
|-------------------------------|--------------------------------|----|
| `zhengshuyun-common-core`     | 核心工具类, 包含重试、IO、时间、ID 生成、参数校验等. |
| `zhengshuyun-common-json`     | JSON 序列化工具, 基于 Jackson.        |
| `zhengshuyun-common-http`     | HTTP 客户端封装, 基于 OkHttp.         |
| `zhengshuyun-common-schedule` | 定时任务工具, 基于 Quartz.             |

## 依赖引入

按需引入对应模块:

```xml

<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>zhengshuyun-common-core</artifactId>
    <version>${version}</version>
</dependency>
```

```xml

<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>zhengshuyun-common-json</artifactId>
    <version>${version}</version>
</dependency>
```

```xml

<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>zhengshuyun-common-http</artifactId>
    <version>${version}</version>
</dependency>
```

```xml

<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>zhengshuyun-common-schedule</artifactId>
    <version>${version}</version>
</dependency>
```
