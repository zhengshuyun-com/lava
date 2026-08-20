# lava-json

`lava-json` 基于 Jackson 3.2.1，提供实例化、线程安全且配置确定的 `JsonCodec`。它没有可变全局初始化入口；默认 codec
在类初始化时即固定。

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-json</artifactId>
</dependency>
```

## 默认 codec

```java
record User(long id, String name) {}

JsonCodec json = JsonCodec.defaultCodec();
String encoded = json.write(new User(42L, "Ada"));
User decoded = json.read(encoded, User.class);
```

默认使用 `Locale.ROOT` 和 Jackson 原生 JSON shape。`long`/`Long` 始终输出 JSON number，不按数值大小切换类型。泛型可使用
Jackson 3 的 `TypeReference<T>` 或 `JavaType`。

## 显式配置

```java
ObjectMapper mapper = JsonMapperFactory.builder()
        .locale(Locale.SIMPLIFIED_CHINESE)
        .zone(ZoneId.of("Asia/Shanghai"))
        .localDateTimePattern("uuuu-MM-dd HH:mm:ss")
        .build();

JsonCodec json = new JsonCodec(mapper);
```

构建完成的 mapper/codec 可在线程间复用。字段上的 `@JsonFormat` 仍优先于 mapper 默认格式。
`JsonMapperFactory.Builder.customize` 和 `JsonCodec.mapper()` 是明确的 Jackson 逃生口；使用它们时，调用方负责额外 Jackson
特性的安全性和兼容性。

## Long 全字符串模块

只有显式注册 `LongAsStringModule` 才会把所有 long shape 固定输出为 string：

```java
JsonCodec json = new JsonCodec(JsonMapperFactory.builder()
        .addModule(new LongAsStringModule())
        .build());
```

该规则覆盖 `long`、`Long`、数组、集合、嵌套对象和 `OptionalLong`。单个字段可以固定恢复为 number：

```java
record Payload(
        @JsonFormat(shape = JsonFormat.Shape.NUMBER) long numericId,
        long stringId) {}
```

模块不会根据 JavaScript 安全整数范围动态改变 JSON 类型。

## 输入流所有权

- `read(InputStream, ...)` 和 `readTree(InputStream)` 借用调用方流，读取后不关闭。
- `read(Path, ...)` 由 codec 打开并关闭文件流。
- `writeBytes` 返回独立字节数组；树节点由当前 mapper 创建。

JSON codec 本身不限制输入字节数。处理不可信或网络输入时，应先在 HTTP、IO 或应用边界执行大小限制；不要对不可信数据启用危险的多态类型配置。
