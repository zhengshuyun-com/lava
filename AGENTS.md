# 项目开发规范

## Builder 命名

- 项目自有 Builder 统一使用 fluent 风格，配置属性的方法直接使用属性名，例如 `baseUrl(...)`、`connectTimeout(...)`、`bearerToken(...)`，不得使用 `setXxx(...)`。
- 集合或动作型方法保留明确语义，例如 `addHeader(...)`、`remove(...)`、`customize(...)`；表达替换语义的 `set(...)` 可以保留。
- 不为旧的 `setXxx(...)` 方法保留兼容别名，测试、文档和示例必须使用统一后的 fluent API。
- 第三方库和 JDK API 不受该命名规则约束。

## 工具类命名

- 项目自有工具类统一以 `Utils` 结尾，例如 `ValidationUtils`、`IdUtils`、`HttpBodyUtils`。
- 工具类指不可实例化、主要通过静态方法提供通用辅助能力的类。
- 创建领域对象的静态辅助类同样遵循该规则，不再使用复数领域名作为工具类名。
- 领域模型、Builder、协议适配器和常量容器不应仅为满足后缀规则而命名为 `Utils`。
