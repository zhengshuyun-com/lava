---
name: java-best-practices
description: |
  zhengshuyun Java 开发规范与最佳实践. 涵盖 API 设计哲学、命名约定、Guava 风格、
  Builder 模式、空值与校验、集合与不可变性、异常、泛型、Modern Java 等.
  编写或审阅 Java 代码时自动加载.
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Java 开发规范

zhengshuyun-java 是 Java 项目的编码规范与最佳实践速查. 以 Guava 风格为基础, 强调正确性、一致性和可维护性.

## 核心原则

- **正确优先**: 先保证正确, 再优化结构.
- **最小改动**: 只改必要的, 避免无关重构.
- **保持一致**: 遵循既有架构, 约定, 命名和代码风格.
- **Guava 风格**: 优先使用 Guava 风格 API/集合/工具, 返回不可变集合. 如需保留 null 语义, 使用只读快照 (`Collections.unmodifiableList(new ArrayList<>(...))`) 并在文档标注.
- **复用优先**: 复杂方法/逻辑优先复用 Guava 和 zhengshuyun-common 工具库, 不重复造轮子.
- **SOLID / DRY / KISS / YAGNI**: 遵循但不教条.

## API 设计哲学

源自 Guava 团队和 Josh Bloch 的核心格言, 作为所有设计决策的总纲:

- **疑则不加**: 对功能、类、方法、参数都适用. 公共 API 只有一次机会做对 -- 可以加, 不能减.
- **容易正确使用, 难以错误使用**: 简单的事应该简单, 复杂的事应该可能, 错误的事应该不可能.
- **语义从签名一眼可知**: 好的 API 不应频繁查文档才能看懂. 找不到好名字, 回去重新设计.
- **最小惊讶原则**: 每个方法做最不令人意外的事.
- **Fail fast**: 越早报告错误越好 -- 编译期最佳, 运行时尽早.
- **不让调用方做库能做的事**: 否则调用方会出现模板代码.

## 1. 类命名

### 1.1 角色分类

| 角色 | 命名规则 | 示例 |
|------|----------|------|
| 工具类 | `XxxUtil` | `IoUtil`, `RetryUtil`, `TimeUtil` |
| 执行器 | 动作 + `-er` | `Retrier`, `ByteStreamCopier`, `TaskScheduler` |
| Builder | 执行器的静态内部类 | `Retrier.Builder`, `ByteStreamCopier.Builder` |
| 结果/句柄 | 描述性名词 | `ScheduledTask` |
| 异常 | `XxxException` | `RetryException`, `HttpException` |
| 常量类 | `Xxxs` | `DateTimePatterns`, `ZoneIds` |
| 校验器 | `XxxValidator` | `EmailValidator` |
| 格式化器 | `XxxFormatter` | `DurationFormatter` |

**注意**: 工具类统一 `XxxUtil` 后缀(不用 Guava 的复数名词 `Xxxs`).

### 1.2 命名禁区

- 不用 `Manager`, `Handler`, `Helper`, `Processor` 等模糊角色词.
- 不用 `XxxService` 作为工具类名(除非是 Spring Bean).
- 不用 `Data`, `Info` 等无意义后缀.
- **不用 `Pair` / `Tuple`**: `getFirst()`/`getSecond()` 毫无语义. 用 Record 或具名类替代:
  ```java
  // 差: first 代表什么?
  Pair<String, Integer> result = Pair.of("John", 30);
  // 好: 字段名即文档
  record UserAge(String name, int age) {}
  ```

### 1.3 类修饰符

- 底层工具类和执行器默认 `final`, 防止不当继承破坏不变量.
- 需要扩展时提供明确的 `Abstract*` 基类, 并文档化自用模式.
- 工具类(纯静态方法): 私有构造器, 禁止实例化.

### 1.4 组合优于继承

不为复用代码而继承, 用组合(持有引用 + 委托)代替. 继承只用于真正的 is-a 关系, 且你控制父类和子类:

```java
// 差: 继承 HashSet, addAll 内部调 add, 重复计数
class CountingSet<E> extends HashSet<E> { ... }

// 好: 组合 + 委托, 不依赖父类内部实现
class CountingSet<E> implements Set<E> {
    private final Set<E> delegate = new HashSet<>();
    // 所有方法委托给 delegate
}
```

## 2. 方法命名与签名

### 2.1 前缀规则

| 操作 | 前缀 | 示例 |
|------|------|------|
| 获取 | `get` | `getId()`, `getNextFireTime()` |
| 赋值 | `set` | `setId()`, `setMaxAttempts()` |
| 判断 | `is` / `has` | `isPaused()`, `hasTask()` |
| 创建 | `create` / `of` / `builder` | `Stopwatch.createStarted()`, `Duration.of()` |
| 转换 | `to` / `as` | `toQuartzTrigger()`, `nextIdAsString()` |
| 执行 | 明确动词 | `execute()`, `copy()`, `schedule()`, `format()` |

**强制**: Builder 配置方法统一 `set*` 前缀(不用 Guava 的省略式). 看到 `set` = 配置, `get` = 取值.

### 2.2 签名设计

- **参数不超过 3-5 个**. 超过就用 Builder 或参数对象.
- **boolean 参数用枚举替代**: `sort(list, SortOrder.ASCENDING)` 而非 `sort(list, true)`.
- **方法重载谨慎**: 不导出两个参数数量相同的重载, 用不同方法名代替. 不在同一参数位置重载不同的函数式接口(lambda 调用会产生歧义).

## 3. Builder 模式

### 3.1 调用链模式

```java
// 模式 A: Util.概念() → Builder.set*() → .build() → 执行器.动作()
RetryUtil.retrier()
    .setMaxAttempts(3)
    .setFixedDelayMillis(1000)
    .build()
    .execute(() -> riskyOperation());

// 模式 B: Util.概念() → Builder.set*() → .终端操作() → 句柄
ScheduleUtil.scheduler(() -> healthCheck())
    .setId("health-check")
    .setTrigger(Trigger.interval(5000).build())
    .schedule();

// 模式 C: 类.builder() → Builder.set*() → .build() → 不可变对象
DurationFormatter formatter = DurationFormatter.builder()
    .setRange(ChronoUnit.HOURS, ChronoUnit.SECONDS)
    .setChinese()
    .build();
```

### 3.2 Builder 规则

- Builder 入口统一 `Xxx.builder()`.
- Util 快捷入口 = 执行器概念的简写名词, 不带 `Builder` 后缀(如 `RetryUtil.retrier()`, `IoUtil.copier()`).
- Builder 是执行器的静态内部类 `Xxx.Builder`.
- 终端操作: `.build()` 返回执行器/不可变对象, `.schedule()` 等返回句柄.

## 4. 常量与字段

| 类型 | 规则 | 示例 |
|------|------|------|
| `static final` 常量 | `UPPER_SNAKE_CASE` | `TWEPOCH`, `MAX_WORKER_ID` |
| 实例字段 | `camelCase` + **中文注释** | `/** 工作节点 ID */ private final long workerId;` |
| Key / 配置键 | `kebab-case` | `"health-check"`, `"daily-backup"` |

**强制**: 成员变量必须有中文注释.

**枚举替代 int 常量**: 需要固定值集合时用枚举, 不用 `static final int`. 枚举提供类型安全, 且支持 `EnumSet`(替代位域)和 `EnumMap`(性能优于 HashMap):

```java
// 差: 可传任意 int, 编译器不报错
public static final int STATUS_RUNNING = 0;

// 好: 类型安全, 编译器保证合法值
enum Status { RUNNING, PAUSED, STOPPED }
```

## 5. 空值处理

- **默认非空**: 入参和成员变量均不允许为 null, 无需额外标注.
- **允许为空**: 仅语义上允许 null 时, 使用 `@Nullable`(`org.jspecify.annotations.Nullable`), 注解放在**类型前**.
- **懒加载/延迟初始化**: 不视为允许 null.
- **参数校验**: 非空参数在赋值给成员变量前必须做 fast-fail 校验.

```java
import org.jspecify.annotations.Nullable;

public class Retrier {
    /** 最大重试次数 */
    private final int maxAttempts;
    /** 自定义监听器, 可选 */
    private final @Nullable RetryListener listener;

    private Retrier(Builder builder) {
        Validate.isTrue(builder.maxAttempts > 0, "maxAttempts must be positive");
        this.maxAttempts = builder.maxAttempts;
        this.listener = builder.listener;
    }
}
```

## 6. 校验分层

三层校验, 抛不同异常, 语义清晰:

| 场景 | 工具 | 异常 |
|------|------|------|
| 调用方传了错误参数 | `Validate.notNull()` / `Validate.isTrue()` | `NullPointerException` / `IllegalArgumentException` |
| 调用方在错误时机调用 | `Validate` + 状态检查 | `IllegalStateException` |
| 内部逻辑不符合预期(非调用方的错) | `Verify.verify()` / `Verify.verifyNotNull()` | `VerifyException` |
| 理论上不可能发生的断言 | Java `assert` | `AssertionError` |

**注意**: 项目使用 `Validate`(zhengshuyun-common)替代 Guava 的 `Preconditions`, 但分层逻辑一致. `Verify` 直接使用 Guava 的 `com.google.common.base.Verify`.

## 7. 集合与不可变性

### 7.1 类型签名策略

| 位置 | 类型选择 | 原因 |
|------|----------|------|
| **返回值 / 字段** | `ImmutableList`, `ImmutableSet`, `ImmutableMap` | 传递语义保证: 不可变、线程安全、无 null 元素、确定迭代顺序 |
| **方法参数** | `Iterable`, `Collection`, `List` | 降低调用方负担, 内部用 `ImmutableXxx.copyOf()` 转换 |

- `ImmutableXxx.copyOf()` 有智能短路: 输入已不可变时直接返回, 零成本.
- 参数用 `ImmutableList` 会强迫调用方转换, 应避免.
- **保留 null 语义**: 使用只读快照 `Collections.unmodifiableList(new ArrayList<>(...))` 并在文档标注.

### 7.2 集合禁忌

- 不在集合中存放 null 元素.
- 不返回 null 表示"空结果" -- 返回 `ImmutableList.of()` 等空集合.

### 7.3 防御性复制

接收可变对象时, 先复制再校验, 防止调用方在校验后偷改:
- 对不可变对象(String, ImmutableList)不需要复制.
- `ImmutableXxx.copyOf()` 是防御性复制的最佳实践.
- 返回可变内部状态时, 也要复制后再返回(或返回不可变视图).

### 7.4 返回类型选择

- 方法返回值优先用 `Collection` / `ImmutableList`(可多次迭代), 不用 `Stream`(只能消费一次).
- `Stream` 适合方法内部的管道操作, 不适合作为 API 返回类型.

## 8. 异常处理

### 8.1 基本规则

- 自定义异常继承 `RuntimeException`, 命名 `XxxException`.
- 非法输入 fail-fast, 不静默吞错.
- 禁止空 catch 块; 确实无需处理时, 变量命名为 `ignored` 并注释原因.

### 8.2 标准异常优先复用

| 异常 | 适用场景 |
|------|----------|
| `IllegalArgumentException` | 非 null 但不合法的参数值 |
| `IllegalStateException` | 对象状态不适合此操作 |
| `NullPointerException` | 参数为 null(由 `Validate.notNull` 抛出) |
| `UnsupportedOperationException` | 方法不被支持 |

自定义异常仅在标准异常无法表达领域语义时使用.

### 8.3 异常信息规范

- **包含上下文**: 导致失败的所有参数和字段值. 如 `"maxAttempts must be positive, got: " + maxAttempts`.
- **不含敏感信息**: 密码、密钥、token 等不得出现在异常消息中.
- **异常转译**: 高层捕获低层异常时, 抛出符合当前抽象层的异常, 并保留原始 cause.

### 8.4 设计约束

- **失败原子性**: 方法失败后, 对象应保持调用前的状态. 不可变对象天然满足; 可变对象在修改前校验参数.
- **库代码避免 checked exception**: checked exception 在 lambda/stream 中不友好, 库 API 优先使用 unchecked.

## 9. 泛型

### PECS 原则

Producer-Extends, Consumer-Super. 库 API 参数使用通配符提高灵活性:

```java
// src 产出元素(extends), dest 消费元素(super)
public static <T> void copy(List<? extends T> src, List<? super T> dest);

// Comparable 是消费者: 它消费 T 来比较
public static <T extends Comparable<? super T>> void sort(List<T> list);
```

不确定时: 如果参数只用于"读取", 用 `? extends T`; 只用于"写入", 用 `? super T`; 既读又写, 不用通配符.

## 10. 日志与输出

- **禁止** `System.out` / `System.err` 输出日志或信息.
- 生产代码使用 SLF4J.
- 测试类使用 `slf4j-simple` 输出日志.

## 11. 时间处理

- **默认时区**: UTC, 业务场景注意时区转换.
- **格式常量**: 使用 `DateTimePatterns` 统一管理.
- **时区常量**: 使用 `ZoneIds` 统一管理.
- **解析工具**: `TimeUtil.parse()` 支持多种格式自动识别.

### 时间单位后缀

英文(默认)采用 SI 缩写: `y`, `mo`, `d`, `h`, `min`, `s`, `ms`, `μs`, `ns`.
中文使用时长语义: `小时`, `分钟`, `秒`, `天`, `毫秒`, `微秒`, `纳秒`.

**区分**: "时"是钟点, "小时"是时长; "分"是钟点, "分钟"是时长.

### 状态文本

用户可见文本保持**语言中立**: 未知值用 `?`/`?/s`, 剩余时间不加语言标签.

## 12. 测试规范

- **框架**: JUnit 5 (Jupiter).
- **命名**: 测试类 `XxxTest`, 测试方法 `testXxx` 或 `testXxx_条件`.
- **日志**: 使用 `slf4j-simple`, 不用 `System.out`.
- **数据**: 使用 `User.create()` 等静态工厂方法.
- **断言**: 每个测试方法有明确的断言, 不用 `assertTrue(result != null)` 代替 `assertNotNull(result)`.

## 13. 包命名

遵循 `com.zhengshuyun.common.<domain>` 分层:

```
com.zhengshuyun.common.core       // 核心工具
com.zhengshuyun.common.core.io    // IO
com.zhengshuyun.common.core.time  // 时间
com.zhengshuyun.common.core.id    // ID 生成
com.zhengshuyun.common.core.lang  // 语言扩展 (Validate 等)
com.zhengshuyun.common.core.retry // 重试
com.zhengshuyun.common.json       // JSON
com.zhengshuyun.common.http       // HTTP
com.zhengshuyun.common.schedule   // 定时任务
```

## 14. Modern Java 特性指南

项目要求 JDK 25, 以下特性均可使用.

### 14.1 Records

不可变数据载体, 替代简单值类型. 自动生成 `equals`, `hashCode`, `toString`, 访问器方法.

```java
// 适用: DTO、值对象、事件载体、组合 Map key
public record UserCreatedEvent(long userId, Instant createdAt) {
    public UserCreatedEvent {
        Validate.isTrue(userId > 0, "userId must be positive");
        Validate.notNull(createdAt, "createdAt");
    }
}
```

**不适用**: 需要可变性、继承、JPA 实体映射时, 用传统类.

### 14.2 Sealed Classes

固定子类型层次, 配合 switch 实现穷举匹配. 替代 Visitor 模式和 tagged class 反模式.

```java
public sealed interface Result<T> permits Success, Failure {
    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(Exception error) implements Result<T> {}
}
```

**不适用**: 层次真正开放扩展时, 不用 sealed.

### 14.3 Switch 表达式

箭头语法消除 fall-through, 必须穷举.

```java
String label = switch (status) {
    case RUNNING -> "运行中";
    case PAUSED -> "已暂停";
    case STOPPED -> "已停止";
};
```

### 14.4 var

仅在类型显而易见且作用域小时使用:

```java
// 适用: 右侧构造器/工厂方法, 类型一目了然
var list = ImmutableList.of("a", "b", "c");
var formatter = DurationFormatter.builder().build();

// 不适用: 方法返回类型不明确, 或作用域大
var result = service.process(input); // result 是什么类型?
```

**禁止**: 不用于字段、方法参数、返回类型. 不与菱形运算符 `<>` 组合(双重推断丢失类型信息).

## 15. 代码风格

- **Imports**: 按标准库 / 第三方 / 内部分组, 不留未使用的 import.
- **注释**: 不加装饰性分隔注释(`======`, `------`). 代码不完整用 `// TODO: xxx`.
- **校验工具**: 使用 `Validate.notNull()` / `Validate.notBlank()`(而非 Guava 的 `Preconditions` 或 JDK 的 `Objects.requireNonNull`).
- **@Override**: 所有覆写方法必须标注.
- **静态成员**: 访问静态成员用类名限定(`TimeUnit.SECONDS.sleep(1)`), 不用实例引用.
- **资源管理**: 所有 `AutoCloseable` 资源必须用 try-with-resources, 不用 try-finally.
- **序列化**: 不用 Java 原生序列化(`Serializable`), 用 JSON(Jackson) 或 Protobuf 替代.
- **提交信息**: git 提交信息以中文为主.

### Javadoc 规范

- 所有 `public` 类和 `public`/`protected` 成员必须有 Javadoc.
- 标签顺序: `@param` → `@return` → `@throws` → `@deprecated`, 每个都要有描述.
- 首句是摘要, 不以"This method returns..."开头.
- 简单成员可用单行 Javadoc: `/** Returns the user ID. */`
- 覆写方法如果父类文档已充分, 可省略.

## 速查: 审阅检查清单

### 命名与结构

- [ ] 类命名符合角色分类(Util / 执行器 / Builder / Exception 等)
- [ ] 底层工具类/执行器标记为 `final`
- [ ] 不用 Pair/Tuple, 用 Record 或具名类
- [ ] 方法前缀正确(get/set/is/has/create/to/as + 动词)
- [ ] Builder 配置方法用 `set*` 前缀
- [ ] 参数不超过 3-5 个, boolean 参数用枚举替代
- [ ] 成员变量有中文注释

### 空值与校验

- [ ] 非空参数有 fast-fail 校验(Validate)
- [ ] 允许 null 的字段标注了 `@Nullable`
- [ ] 校验分层正确: Validate(调用方) / Verify(内部) / assert(不可能)

### 集合与类型

- [ ] 返回值/字段用 `ImmutableXxx`; 参数用 `Iterable`/`Collection`
- [ ] 不返回 null 表示空结果, 返回空集合
- [ ] 集合不存 null 元素
- [ ] 返回 Collection 而非 Stream
- [ ] 接收可变对象时做防御性复制
- [ ] 泛型参数遵循 PECS
- [ ] 固定值集合用枚举, 不用 int 常量

### 异常与资源

- [ ] 异常继承 RuntimeException, 命名 XxxException
- [ ] 优先复用标准异常
- [ ] 异常消息包含上下文参数值, 不含敏感信息
- [ ] 库 API 避免 checked exception
- [ ] AutoCloseable 资源用 try-with-resources
- [ ] 不用空 catch 块

### 风格与文档

- [ ] 无 System.out/System.err
- [ ] public 类/成员有 Javadoc
- [ ] 静态成员用类名访问
- [ ] 不用 Java 原生序列化
- [ ] Key/配置键使用 kebab-case
- [ ] 常量使用 UPPER_SNAKE_CASE
- [ ] 时间默认 UTC
- [ ] 覆写方法标注 @Override
- [ ] 适当使用 Record / sealed class / switch 表达式
