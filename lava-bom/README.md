# lava-bom

`lava-bom` 是独立的 Lava 模块版本清单。它通过 Maven `dependencyManagement` 对齐全部 Lava 模块版本，不继承 `lava-parent`，也不管理应用的第三方依赖版本。它是 `pom` 构件，没有运行时依赖，也不会自动引入任何库。

## 使用

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.zhengshuyun</groupId>
            <artifactId>lava-bom</artifactId>
            <version>2.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.zhengshuyun</groupId>
        <artifactId>lava-json</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zhengshuyun</groupId>
        <artifactId>lava-mail</artifactId>
    </dependency>
</dependencies>
```

应用无需、也不应继承 `lava-parent`。`lava-parent` 只服务于本仓库构建；消费者导入的 `lava-bom` 是一个独立 POM。

## 管理范围

| 版本 | 构件 |
| --- | --- |
| 与 BOM 相同 | `lava-core`、`lava-json`、`lava-http`、`lava-schedule`、`lava-crypto`、`lava-mail` |

BOM 管理版本不等于声明依赖。应用仍需在自己的 `<dependencies>` 中声明实际使用的 Lava 模块，只省略 `<version>`；声明 Lava 模块后，该模块的生产依赖会按 Maven 规则传递。只有应用直接使用 Jackson、OkHttp、JUnit 等第三方 API 时，才需要自行声明对应依赖并管理其版本。

## 验证生效版本

在消费者项目中可以检查 effective POM 和依赖树：

```bash
mvn help:effective-pom
mvn dependency:tree
```

不要在同一消费者中同时导入不同版本的 Lava BOM，或为受管 Lava 模块单独覆盖版本。
