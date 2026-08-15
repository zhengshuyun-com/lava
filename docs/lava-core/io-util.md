# IoUtil IO 与数据传输

`IoUtil` 用于处理字节流复制, 资源关闭和 IO 异常包装. 数据传输进度文本可以配合 `DataTransferUtil` 使用.

## 最小可运行示例

```java
import com.zhengshuyun.lava.core.io.DataTransferUtil;
import com.zhengshuyun.lava.core.io.IoUtil;

import java.nio.file.Path;

public class IoUtilDemo {

    public static void main(String[] args) {
        long copied = IoUtil.copier()
                .setSource(Path.of("/tmp/source.bin"))
                .build()
                .write(Path.of("/tmp/target.bin"));

        DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(10 * 1024 * 1024);
        String progress = tracker.format(copied);

        // TODO: 按业务处理 copied/progress
    }
}
```

- `IoUtil.copier()`: 创建字节流复制器.
- `write(...)`: 写入到目标路径, 文件或输出流.
- `DataTransferUtil.tracker(...)`: 格式化传输进度, 速率和剩余时间.

## 常见数据源

```java
byte[] bytes = IoUtil.copier()
        .setSource("hello")
        .build()
        .writeBytes();

String text = IoUtil.copier()
        .setSource(bytes)
        .build()
        .writeString();
```

| 数据源       | 写法                               |
|--------------|------------------------------------|
| 字符串       | `setSource(String)`                |
| 字节数组     | `setSource(byte[])`                |
| 本地路径     | `setSource(Path)`                  |
| 文件         | `setSource(File)`                  |
| 输入流       | `setSource(InputStream)`           |
| 输入流提供者 | `setSource(Supplier<InputStream>)` |

## 资源关闭和异常包装

```java
IoUtil.closeQuietly(resource1, resource2);

String body = IoUtil.wrapIOException(() -> readBody());
```

- `closeQuietly(...)`: 按逆序关闭资源, 并忽略关闭异常.
- `wrapIOException(...)`: 把 `IOException` 包装为 `UncheckedIOException`.

## 常见坑与排查建议

| 异常/消息                                  | 原因                            | 解决方式                                                 |
|--------------------------------------------|---------------------------------|----------------------------------------------------------|
| `InputStream source can only be used once` | 同一个 `InputStream` 被重复写入 | 多次写入时使用 `Path`, `File` 或 `Supplier<InputStream>` |
| 进度百分比不准确                           | 内容长度未知                    | 能确定长度时传入内容长度或使用可获取大小的数据源         |
| 资源未释放                                 | 手动管理流时遗漏关闭            | 优先使用 `IoUtil.copier()`, 或使用 `try-with-resources`  |
