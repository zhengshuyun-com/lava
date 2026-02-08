# lava-bom

`lava-bom` 用于统一管理 Lava 各子模块及第三方依赖版本，避免多模块项目的版本漂移。

## 基本使用

在父 POM 的 `dependencyManagement` 中引入 `lava-bom`.

```xml
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

然后按需引入模块 (无需指定版本):

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>模块名称</artifactId>
</dependency>
```