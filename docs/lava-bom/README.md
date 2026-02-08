# 依赖版本管理教程

`lava-bom` 用于统一管理 Lava 子模块和第三方依赖版本, 避免多模块项目出现版本漂移和冲突.

## 引入依赖

在父 `pom.xml` 的 `dependencyManagement` 中导入 `lava-bom`.

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

## 最小可运行示例

```xml
<project>
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>demo-app</artifactId>
    <version>1.0.0</version>

    <properties>
        <!-- 1) 统一声明 Lava 版本 -->
        <lava.version>1.0.0-SNAPSHOT</lava.version>
    </properties>

    <!-- 2) 导入 BOM -->
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

    <dependencies>
        <!-- 3) 业务依赖不再单独写版本 -->
        <dependency>
            <groupId>com.zhengshuyun</groupId>
            <artifactId>lava-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.zhengshuyun</groupId>
            <artifactId>lava-json</artifactId>
        </dependency>
    </dependencies>
</project>
```

- BOM 生效后, `lava-*` 依赖建议都不写 `<version>`.
- 版本升级只改一处 `lava.version`, 回归验证成本更低.

## 版本管理能力

### 子模块版本对齐

| 模块 | 是否建议在业务 POM 写版本 | 建议写法 |
|------|---------------------------|----------|
| `lava-core` | 否 | 仅写 `groupId` + `artifactId` |
| `lava-json` | 否 | 仅写 `groupId` + `artifactId` |
| `lava-http` | 否 | 仅写 `groupId` + `artifactId` |
| `lava-schedule` | 否 | 仅写 `groupId` + `artifactId` |
| `lava-jwt` / `lava-crypto` | 否 | 仅写 `groupId` + `artifactId` |

### 第三方依赖托管

`lava-bom` 通过父 POM 统一托管常用依赖版本, 如 `guava`, `jackson`, `okhttp`, `quartz`, `java-jwt`, `bcprov`.

- 当 Lava 模块依赖这些库时, 业务项目通常不需要再手工对齐版本.
- 如确需覆盖, 建议仅在单个服务内最小范围覆盖, 并补充兼容性测试.

## 升级建议

| 场景 | 做法 | 说明 |
|------|------|------|
| 常规小版本升级 | 直接更新 `lava.version` | 先跑单测和冒烟回归 |
| 跨大版本升级 | 建议分支灰度 | 重点验证序列化, HTTP 调用, 定时任务 |
| 仅单模块试点 | 局部覆盖模块版本 | 用完尽快回收, 避免长期分叉 |

## 常见坑与排查建议

- 把 BOM 放在 `<dependencies>` 而不是 `<dependencyManagement>`, 会导致导入不生效.
- 漏写 `<type>pom</type>` 或 `<scope>import</scope>`, Maven 不会按 BOM 处理.
- 业务模块又手工写了 `lava-*` 版本, 可能覆盖 BOM 导致版本不一致.
- 父子 POM 多层继承时, 建议执行 `mvn help:effective-pom` 快速确认最终生效版本.

## 实践建议

- 团队统一约定: `lava-*` 依赖版本只在 BOM 入口声明一次.
- 升级 BOM 后至少执行一轮 `mvn test` 和关键链路联调.
- 对生产系统建议配合发布记录保留 `lava.version` 变更历史, 便于故障回溯.
