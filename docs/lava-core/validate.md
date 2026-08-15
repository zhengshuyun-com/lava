# Validate 参数校验

`Validate` 用于在方法入口快速校验参数, 未通过校验时抛出 `IllegalArgumentException`.

## 最小可运行示例

```java
import com.zhengshuyun.lava.core.lang.Validate;

public class ValidateDemo {

    public static void main(String[] args) {
        String username = Validate.notBlank("alice", "username must not be blank");
        Integer age = Validate.notNull(18, "age must not be null");
        Validate.isTrue(age >= 18, "age must be >= 18");

        // TODO: 按业务处理 username/age
    }
}
```

- `Validate` 适合参数校验和状态前置校验.
- 校验通过后会返回原值, 方便直接赋值.
- 错误消息支持普通对象和模板参数.

## 常用方法

| 方法            | 说明                 |
|-----------------|----------------------|
| `isTrue(...)`   | 校验表达式为 `true`  |
| `isFalse(...)`  | 校验表达式为 `false` |
| `notNull(...)`  | 校验对象非空         |
| `isNull(...)`   | 校验对象为空         |
| `notBlank(...)` | 校验字符串非空白     |
| `notEmpty(...)` | 校验集合或 Map 非空  |
| `isEmail(...)`  | 校验邮箱格式         |
| `isMobile(...)` | 校验中国手机号格式   |

## 模板错误消息

```java
String userId = Validate.notBlank(
        inputUserId,
        "userId must not be blank, requestId=%s",
        requestId
);
```

- 模板格式使用 Guava `lenientFormat` 语义.
- 错误消息不要包含密码, token, 私钥等敏感信息.

## 常见坑与排查建议

| 异常/消息                  | 原因                         | 解决方式                               |
|----------------------------|------------------------------|----------------------------------------|
| `IllegalArgumentException` | 参数没有通过校验             | 查看异常消息定位具体字段               |
| 敏感信息进入日志           | 错误消息包含明文密码或 token | 错误消息只保留字段名和必要上下文       |
| `notBlank(...)` 仍然通过   | 输入不是空白字符串           | 如果需要业务格式校验, 继续增加业务规则 |
