# EmailValidator 邮箱校验

`EmailValidator` 用于判断邮箱地址格式是否有效. 如果需要失败时直接抛异常, 可以使用 `Validate.isEmail(...)`.

## 最小可运行示例

```java
import com.zhengshuyun.lava.core.lang.EmailValidator;
import com.zhengshuyun.lava.core.lang.Validate;

public class EmailValidatorDemo {

    public static void main(String[] args) {
        boolean valid = EmailValidator.isValid("user@example.com");
        String email = Validate.isEmail("user@example.com", "email is invalid");

        // TODO: 按业务处理 valid/email
    }
}
```

- `EmailValidator.isValid(...)`: 返回 `boolean`, 不抛校验异常.
- `Validate.isEmail(...)`: 校验失败时抛出 `IllegalArgumentException`.

## 校验规则

| 规则         | 说明                 |
|--------------|----------------------|
| 本地部分长度 | 最大 `64` 个字符     |
| 域名部分长度 | 最大 `255` 个字符    |
| 点号位置     | 不允许首尾点和连续点 |
| 空格         | 不允许首尾空格       |
| 国际化域名   | 支持 IDN 域名        |

## 常见坑与排查建议

| 异常/消息                      | 原因                 | 解决方式                     |
|--------------------------------|----------------------|------------------------------|
| 返回 `false`                   | 邮箱为空或格式不合法 | 检查首尾空格, `@` 位置和域名 |
| `Validate.isEmail(...)` 抛异常 | 邮箱格式校验失败     | 在接口边界转换为业务错误提示 |
| 国际化域名校验失败             | 域名无法转换为 ASCII | 检查域名是否符合 IDN 规范    |
