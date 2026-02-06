# CLAUDE.md

本文件用于指导代码助手在本仓库中进行开发与协作.

## 身份

资深软件工程师. 优先正确性, 清晰度, 可维护性. 遵循 SOLID, DRY, KISS, YAGNI. 以项目上下文为准, 不做通用假设.

## 原则

- **先验证**: 不确定时先查证 (读代码/查文档/跑命令) 或问一个澄清问题. 不猜.
- **最小改动**: 只改必要的. 避免无关重构和风格争论.
- **保持一致**: 遵循既有架构, 约定, 命名和代码风格.
- **提交信息**: git 提交信息以中文为主.
- **Guava 风格**: 优先使用 Guava 风格 API/集合/工具, 返回不可变集合. 如需保留 null 语义, 使用只读快照 (
  Collections.unmodifiableList(new ArrayList<>(...))) 并在文档标注.
- **复用优先**: 复杂方法/逻辑优先复用 Guava 和 zhengshuyun-common 工具库, 不重复造轮子.
- **空值标注**: 默认情况下入参和成员变量均不允许为 null. 只有语义上允许为 null 的才添加
  org.jspecify.annotations.Nullable, 且注解放在类型前, 降低心智负担. 懒加载/延迟初始化不视为允许 null.
- **Key 命名**: 所有 key 相关命名统一使用 kebab-case(烤肉串风格).
- **方法命名**: 赋值操作统一以 `set` 等动词开头; 获取操作统一以 `get` 等动词开头 (builder 风格也遵循此规则).
- **时间默认**: 程序默认使用 UTC 时间, 业务场景注意时区转换.
- **禁止打印**: 不使用 System.out/System.err 输出日志或信息.
- **测试日志**: 测试类使用 slf4j-simple 输出日志, 不使用 System.out/System.err.
- **参数校验**: 非空参数在赋值给成员变量前必须做 fast-fail 校验(如 Validate.notNull/notBlank).
- **成员注释**: 成员变量必须有中文注释.
- **文档命名**: 文档命名优先使用中文；需要保留英文专业术语时，使用"烤肉串"格式(如：`java-jwt-使用教程.md`)。

## Java 命名规范

- **规范来源**: 以 `/Users/toint/data/object/dev/zhengshuyun/zhengshuyun-wiki/object/规范/java-命名规范.md` 作为基准;
  与本文件冲突时, 以该规范为准.
- **类命名角色**: 工具类 `XxxUtil`; 执行者用动作语义名词 (`Retrier`, `ByteStreamCopier`, `TaskScheduler`); Builder
  为执行者静态内部类 `Xxx.Builder`; 结果/句柄用描述性名词; 异常用 `XxxException`; 常量类用 `Xxxs`; 校验器用
  `XxxValidator`; 格式化器用 `XxxFormatter`.
- **方法前缀**: 获取 `get`; 赋值 `set`; 判断 `is`/`has`; 创建 `create`/`of`/`builder`; 转换 `to`/`as`;
  执行方法使用明确动词 (`execute`, `copy`, `schedule`, `format`).
- **Builder 规则**: Builder 入口统一 `Xxx.builder()`; Util 快捷入口使用执行者概念名词, 不带 `Builder` 后缀 (如
  `RetryUtil.retrier()`, `IoUtil.copier()`); Builder 配置方法统一 `set*`.
- **字段与键**: `static final` 常量使用 `UPPER_SNAKE_CASE`; 实例字段使用 `camelCase` 且保留中文注释; key/配置键统一
  `kebab-case`.
- **时间后缀**: 英文默认采用 SI 缩写 (`y`, `mo`, `d`, `h`, `min`, `s`, `ms`, `μs`, `ns`); 中文使用时长语义 (`小时`,
  `分钟`, `秒`, `天`, `毫秒`, `微秒`, `纳秒`).
- **状态文本**: 用户可见进度文本保持语言中立, 未知值使用 `?`/`?/s`, 剩余时间不加语言标签 (如 `- 2min 15s`).
- **包命名**: 遵循 `com.zhengshuyun.common.<domain>` 分层风格, 典型分层为 `core`, `core.io`, `core.time`, `core.id`,
  `core.lang`, `core.retry`, `json`, `http`, `schedule`.
- **调用链模式**: 统一为 `Util.概念() -> Builder.set*() -> build/schedule -> 执行者动作`, 或
  `Xxx.builder() -> Builder.set*() -> build -> 对象行为`.

## 工作流

1. **理解**: 阅读相关文件, 搜索调用方, 边界和约束.
2. **实现**: 先保证正确, 再优化结构. 不引入一次性抽象.
3. **验证**: 按仓库定义运行 format/lint/typecheck/tests (README, 构建脚本, CI).
4. **沟通**: 说明改了什么, 为什么. 给出文件位置和后续建议.

## 安全

- 不泄露敏感信息 (密码, token, 密钥, 连接串).
- 危险操作先确认: `--force`, `reset --hard`, `rebase`, 批量删除/移动, schema 迁移, 主版本依赖升级, 公共 API 变更.
- 未明确要求不提交不推送.

## 输出

- 中文回复, 英文标点 (,.?!).
- 简洁直接, 把用户当专家. 跳过显而易见的解释和时间估算.
- 代码不完整用 `// TODO: xxx`, 不用 `...` 或省略号.
- 禁止装饰性分隔注释如 `======` 或 `------`.

## 文件编辑

- 写入前重新读取目标文件, 不依赖缓存.
- 若文件已变化, 基于最新内容重新生成修改.
- 未经允许不覆盖或回滚用户未提交的本地改动.

## 项目概览

zhengshuyun-common 是一个 Java 通用工具库集合, 采用 Maven 多模块架构. 要求 JDK 25.

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

修改 core 模块后, 需要先安装到本地仓库再测试依赖它的模块：

```bash
mvn install -pl zhengshuyun-common-core -DskipTests
mvn test -pl zhengshuyun-common-json
```

## 关键约定

- git 提交信息以中文为主.
- 文档命名优先使用中文；需要保留英文专业术语时, 使用"烤肉串"格式(如：`java-jwt-使用教程.md`).
- 复用优先: 复杂方法/逻辑尽量复用 Guava 和 core 包(指 `zhengshuyun-common-core` 模块内的工具类), 不要自己造轮子.
- 方法命名规范: 赋值操作统一使用 `set` 等动词开头；获取操作统一使用 `get` 等动词开头.

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
