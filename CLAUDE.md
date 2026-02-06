# CLAUDE.md

本文件用于指导代码助手在本仓库中进行开发与协作。

## 项目概览

zhengshuyun-common 是一个 Java 通用工具库集合，采用 Maven 多模块架构。要求 JDK 25。

**Maven 模块结构**:
- `zhengshuyun-common-core` - 核心工具类(重试、IO、时间、ID 生成等)
- `zhengshuyun-common-json` - JSON 序列化工具(基于 Jackson)
- `zhengshuyun-common-http` - HTTP 客户端封装(基于 OkHttp)
- `zhengshuyun-common-spring-boot-starter` - Spring Boot 自动配置

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
修改 core 模块后，需要先安装到本地仓库再测试依赖它的模块：
```bash
mvn install -pl zhengshuyun-common-core -DskipTests
mvn test -pl zhengshuyun-common-json
```

## 关键约定

- git 提交信息以中文为主。
- 文档与新建文件命名尽量使用中文；需要保留英文专业术语时，使用“烤肉串”格式(如：`java-jwt-使用教程.md`)。
- 复用优先: 复杂方法/逻辑尽量复用 Guava 和 core 包(指 `zhengshuyun-common-core` 模块内的工具类), 不要自己造轮子。
- 方法命名规范: 赋值操作统一使用 `set` 等动词开头；获取操作统一使用 `get` 等动词开头。

### 时间处理标准
- **格式常量**: 使用 `DateTimePatterns` 统一管理(位于 core 模块)
- **时区常量**: 使用 `ZoneIds` 统一管理
- **默认时区**: UTC(通用库标准)
- **解析工具**: `TimeUtil.parse()` 支持多种格式自动识别

### 异常处理规范
- 自定义异常继承自 `RuntimeException`
- 命名规范: `XxxException`(如 `JsonException`, `HttpException`, `RetryException`)
- 包含详细的错误信息和上下文

### 测试规范
- 测试类命名: `XxxTest`
- 测试方法命名: `testXxx` 或 `testXxx_条件`
- 使用 JUnit 5 (Jupiter)
- 测试数据: 使用 `User.create()` 等静态工厂方法
