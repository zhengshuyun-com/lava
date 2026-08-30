# JSON 编解码

## 写入 JSON

```java
JsonCodec json = JsonCodec.defaultCodec();

String compact = json.write(value);
String pretty = json.writePretty(value);
byte[] bytes = json.writeBytes(value);
```

`writeBytes(...)` 返回独立字节数组，调用方可以安全持有和修改返回值。

## 读取对象

```java
User user = json.read(content, User.class);
User fromBytes = json.read(bytes, User.class);
```

泛型使用 Jackson 3 的 `TypeReference`：

```java
List<User> users = json.read(
        content,
        new TypeReference<List<User>>() {
        }
);
```

需要动态构造复杂类型时使用 `JavaType`：

```java
JavaType type = json.typeFactory()
        .constructCollectionType(List.class, User.class);

List<User> users = json.read(content, type);
```

## 文件与输入流

```java
User fromFile = json.read(path, User.class);
User fromStream = json.read(input, User.class);
JsonNode tree = json.readTree(input);
```

资源所有权：

- `read(InputStream, ...)` 和 `readTree(InputStream)` 借用输入流，不关闭；
- `read(Path, ...)` 由 codec 打开并关闭文件流；
- `JsonCodec` 本身不限制输入字节数。

处理网络或其他不可信输入时，应先在 HTTP、IO 或应用边界限制大小，再交给 JSON 解析器。

## 树模型与转换

```java
ObjectNode object = json.objectNode();
object.put("name", "Ada");

ArrayNode array = json.arrayNode();
array.add(object);

User user = json.convert(object, User.class);
```

树节点由当前 codec 对应的 mapper 创建。JSON 文档为空或根值解析为 `null` 时会抛出 `JsonException`，不会静默返回 `null`。
