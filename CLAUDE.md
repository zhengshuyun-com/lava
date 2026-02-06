# CLAUDE.md

本文件用于指导代码助手在 `zhengshuyun-common` 仓库中的协作.

## 项目概览

- `zhengshuyun-common` 是 Java 通用工具库集合, 采用 Maven 多模块架构.
- JDK 要求: 25.
- 详细信息见 `README.md`.

## 构建与测试命令

### 完整构建

```bash
mvn clean install
```

### 仅编译(跳过测试)

```bash
mvn clean install -DskipTests
```

### 运行测试

```bash
# 所有测试
mvn test

# 单个模块
mvn test -pl zhengshuyun-common-core
mvn test -pl zhengshuyun-common-json

# 单个测试类
mvn test -Dtest=JsonUtilTest -pl zhengshuyun-common-json

# 单个测试方法
mvn test -Dtest=JsonUtilTest#testWriteValueAsString -pl zhengshuyun-common-json

# 包含依赖模块(-am: also make)
mvn test -pl zhengshuyun-common-json -am
```

### 模块依赖顺序

修改 zhengshuyun-common-core 模块后, 需要先安装到本地仓库再测试依赖它的模块:

```bash
mvn install -pl zhengshuyun-common-core -DskipTests
mvn test -pl zhengshuyun-common-json
```

## 关键约定

- 复用优先: 复杂方法或逻辑尽量复用 Guava 与 `zhengshuyun-common-core` 模块的工具类.
- 时间处理: 使用 `DateTimePatterns` 与 `ZoneIds` 统一管理, 默认时区 UTC, 解析工具 `TimeUtil.parse()`.
- 文档目录: 本仓库文档集中在 `doc/`.
