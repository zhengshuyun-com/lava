# lava-bom

`lava-bom` 是 Lava 模块的 Maven 版本清单。它通过 `dependencyManagement` 对齐所有 Lava 模块版本，本身不会把任何模块加入应用运行时。

## 导入 BOM

```xml
<properties>
    <lava.version>x.y.z</lava.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.zhengshuyun</groupId>
            <artifactId>lava-bom</artifactId>
            <version>${lava.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

随后按需声明模块，不再为各模块重复填写版本：

```xml
<dependencies>
    <dependency>
        <groupId>com.zhengshuyun</groupId>
        <artifactId>lava-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zhengshuyun</groupId>
        <artifactId>lava-http</artifactId>
    </dependency>
</dependencies>
```

## 管理范围

BOM 使用同一个版本管理以下构件：

- `lava-core`
- `lava-json`
- `lava-http`
- `lava-pay-wechat`
- `lava-pay-alipay`
- `lava-schedule`
- `lava-crypto`
- `lava-mail`

## 边界

- BOM 只管理版本，不等于声明依赖；
- 应用不应继承仓库内部使用的 `lava-parent`；
- BOM 不管理应用直接使用的 Jackson、OkHttp、JUnit 等第三方依赖；
- 不要同时导入多个不同版本的 Lava BOM；
- 不要为受 BOM 管理的 Lava 模块单独覆盖版本。

## 验证版本

```bash
mvn help:effective-pom
mvn dependency:tree
```

`effective-pom` 用于确认 `dependencyManagement` 是否生效，`dependency:tree` 用于确认应用实际引入了哪些模块和传递依赖。
