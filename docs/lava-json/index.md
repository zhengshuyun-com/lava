# lava-json

`lava-json` 基于 Jackson 3，提供实例化、线程安全且配置确定的 `JsonCodec`。默认编解码器在类初始化时固定，不提供可变的全局初始化入口。

## 添加依赖

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-json</artifactId>
</dependency>
```

版本推荐由 [lava-bom](../lava-bom/) 管理。

## 快速示例

```java
record User(long id, String name) {
}

JsonCodec json = JsonCodec.defaultCodec();

String encoded = json.write(new User(42L, "Ada"));
User decoded = json.read(encoded, User.class);
```

默认行为：

- 使用 `Locale.ROOT`；
- 使用 Jackson 原生 JSON 形态；
- `long` 和 `Long` 始终编码为 JSON number；
- 编解码失败统一抛出 `JsonException`；
- 构建完成的 mapper 和 codec 可以在线程间复用。

继续查看 [JSON 编解码](./codec) 和 [Mapper 配置](./configuration)。
