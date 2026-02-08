# lava-json

`lava-json` 提供统一的 JSON 序列化与反序列化 API，底层基于 Jackson 实现。内置安全长整型处理和 ISO 日期格式支持。

## 引入依赖

如果你已经通过 BOM 管理版本，只需引入 `lava-json`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-json</artifactId>
</dependency>
```

## 最小可运行示例

```java
import com.zhengshuyun.lava.json.JsonUtil;

public class JsonQuickStartDemo {

    public static void main(String[] args) {
        // 序列化：对象 -> JSON 字符串
        User user = new User("Alice", 30);
        String jsonStr = JsonUtil.toJsonString(user);
        System.out.println(jsonStr);

        // 反序列化：JSON 字符串 -> 对象
        User parsed = JsonUtil.parseObject(jsonStr, User.class);
        System.out.println(parsed.getName());

        // TODO: 按业务处理 user
    }

    static class User {
        private String name;
        private int age;

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}
```

## 常见操作

### 对象序列化

```java
String json = JsonUtil.toJsonString(object);
```

### 对象反序列化

```java
MyClass obj = JsonUtil.parseObject(jsonStr, MyClass.class);
```

### 数组反序列化

```java
List<MyClass> list = JsonUtil.parseArray(jsonStr, MyClass.class);
```

### 泛型反序列化

```java
List<User> users = JsonUtil.parseObject(jsonStr, new TypeReference<List<User>>(){}.getType());
```

## 安全特性

### 长整型安全处理

`lava-json` 自动处理大整数（超过 JavaScript 安全范围）的序列化，避免精度丢失。

### ISO 8601 日期格式

日期字段自动按 ISO 8601 格式序列化和反序列化（UTC 时区）。

## 常见坑与排查建议

- JSON 中的数字类型需确认是否超过 JavaScript `Number.MAX_SAFE_INTEGER`。
- 日期序列化时确保时区一致，推荐统一使用 UTC。
- 反序列化时如果类型不匹配会抛 `JsonException`，建议在 API 边界处捕获。

## 生产环境建议

- 对外 API 的 JSON 响应应启用长整型保护。
- 统一日期格式为 ISO 8601 标准。
- 记录 JSON 解析异常用于排障。
