# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

zhengshuyun-common 是一个 Java 通用工具库集合，采用 Maven 多模块架构。要求 JDK 25。

**Maven 模块结构**:
- `zhengshuyun-common-core` - 核心工具类（重试、IO、时间、ID 生成等）
- `zhengshuyun-common-json` - JSON 序列化工具（基于 Jackson）
- `zhengshuyun-common-http` - HTTP 客户端封装（基于 OkHttp）
- `zhengshuyun-common-spring-boot-starter` - Spring Boot 自动配置

## Build & Test Commands

### 完整构建
```bash
mvn clean install
```

### 仅编译（跳过测试）
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

# 包含依赖模块（-am: also make）
mvn test -pl zhengshuyun-common-json -am
```

### 模块依赖顺序
修改 core 模块后，需要先安装到本地仓库再测试依赖它的模块：
```bash
mvn install -pl zhengshuyun-common-core -DskipTests
mvn test -pl zhengshuyun-common-json
```

## Architecture & Design Patterns

### JSON 序列化架构（zhengshuyun-common-json）

**设计理念**: 提供统一的 JSON 配置，默认采用 ISO 8601 UTC 标准。

**核心组件**:
1. **JsonBuilder** - Builder 模式配置 ObjectMapper
   - 默认配置: ISO 8601 格式 + UTC 时区 + Locale.ROOT
   - 可通过 setter 自定义时区/格式/地区

2. **JsonUtil** - 单例门面，提供静态方法
   - 初始化: `JsonUtil.init(ObjectMapper)` 或自动使用默认配置
   - 线程安全: 双重检查锁确保单例

3. **IsoDateModule** - 自定义 Date 序列化器
   - 确保 `java.util.Date` 输出 `yyyy-MM-dd'T'HH:mm:ss'Z'` 格式
   - 处理时区转换为 UTC

**时间类型序列化规则**:
- `Date/Instant`: `2026-01-01T00:00:00Z` (UTC + Z 后缀)
- `LocalDateTime`: `2026-01-01T00:00:00Z` (直接当作 UTC)
- `LocalDate`: `2026-01-01`
- `LocalTime`: `12:30:00`

**扩展方式**:
```java
ObjectMapper mapper = JsonUtil.builder()
    .setCustomizer(builder -> {
        // 添加自定义模块
    })
    .build();
```

### 重试机制架构（zhengshuyun-common-core）

**设计理念**: 提供声明式和编程式两种重试方式。

**核心组件**:
1. **Retrier** - 可配置的重试执行器
   - 支持多种重试策略: 固定间隔、指数退避、线性增长
   - 支持条件重试: 异常类型、自定义条件
   - 支持监听器: 重试前、成功、失败、达到最大次数

2. **RetryUtil** - 简化的静态方法门面
   - 快速重试: `RetryUtil.retry(() -> {...})`
   - 自定义配置: `RetryUtil.retry(() -> {...}, maxRetries, interval)`

**使用示例**:
```java
// 编程式
Retrier.builder()
    .setMaxRetries(3)
    .setStrategy(RetryStrategy.exponentialBackoff(Duration.ofSeconds(1)))
    .setOnRetry((attempt, exception) -> log.warn("Retry {} due to {}", attempt, exception))
    .build()
    .execute(() -> riskyOperation());
```

### IO 工具架构（zhengshuyun-common-core）

**ByteStreamCopier** - Builder 模式的流复制工具:
- 支持多种输入源: String、byte[]、InputStream、File
- 支持多种输出目标: OutputStream、File、String
- 支持进度监听: `ProgressListener`
- 自动资源管理: try-with-resources

**DataTransferUtil** - 静态方法简化常见操作:
- `transfer(InputStream, OutputStream)` - 流拷贝
- `transferWithProgress(...)` - 带进度的流拷贝

## Key Conventions

### 时间处理标准
- **格式常量**: 使用 `DateTimePatterns` 统一管理（位于 core 模块）
- **时区常量**: 使用 `ZoneIds` 统一管理
- **默认时区**: UTC（通用库标准）
- **解析工具**: `TimeUtil.parse()` 支持多种格式自动识别

### 异常处理规范
- 自定义异常继承自 `RuntimeException`
- 命名规范: `XxxException`（如 `JsonException`, `HttpException`, `RetryException`）
- 包含详细的错误信息和上下文

### 测试规范
- 测试类命名: `XxxTest`
- 测试方法命名: `testXxx` 或 `testXxx_条件`
- 使用 JUnit 5 (Jupiter)
- 测试数据: 使用 `User.create()` 等静态工厂方法

## Breaking Changes History

### JSON 序列化格式变更 (1.0.0)
- **变更**: 默认格式从中国本地化改为 ISO 8601 UTC
- **影响**: Date/LocalDateTime 输出格式从 `yyyy-MM-dd HH:mm:ss` 变为 `yyyy-MM-dd'T'HH:mm:ss'Z'`
- **迁移**: 需要更新前端/客户端的日期解析逻辑，或通过 JsonBuilder setter 恢复旧格式
