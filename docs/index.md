---
# https://vitepress.dev/reference/default-theme-home-page
layout: home

hero:
    name: "Lava"
    text: "一致, 安全, 开箱即用"
    tagline: 覆盖常用基础工具, JSON, HTTP, 加密, JWT, 调度, 邮件等 Java 基础设施能力.
    actions:
        -   theme: brand
            text: 快速开始
            link: /quick-start
        -   theme: alt
            text: Maven
            link: https://central.sonatype.com/search?q=lava&namespace=com.zhengshuyun
        -   theme: alt
            text: GitHub
            link: https://github.com/zhengshuyun-com/lava

features:
    -   title: 统一体验
        details: 在多个基础设施场景中保持一致的接入方式和使用风格, 降低团队协作成本.
    -   title: 安全优先
        details: 面向服务端常见安全场景提供稳妥的默认选择, 帮助业务减少重复判断.
    -   title: 开箱即用
        details: 常用能力按模块封装, 接入后即可在业务项目中复用.
    -   title: 按需组合
        details: 按项目需要选择 JSON, HTTP, 加密, JWT, 调度, 邮件等能力, 避免一次性引入过多依赖.
    -   title: 文档完整
        details: 每个模块提供接入说明, 使用示例和常见问题, 方便从试用走到生产落地.
    -   title: 版本统一
        details: 通过 BOM 统一管理依赖版本, 降低升级, 排查和长期维护成本.
---

## 推荐服务

### [ModelRouter 企业级 AI 中转站](https://model.zhengshuyun.net/)

统一承接 Codex, Claude Code, Cursor 和自研应用的模型调用, 集中管理令牌, 转发规则, 调用日志和费用统计.

### [Codex Local Session Manager](https://github.com/zhengshuyun-com/codex-local-session-manager)

本地 Codex 会话管理工具, 用来浏览, 搜索和清理本机 Codex 会话历史.

## 模块概览

| 模块            | 适用场景           | 文档                              |
|-----------------|--------------------|-----------------------------------|
| `lava-core`     | 项目通用能力沉淀   | [概览](./lava-core/README.md)     |
| `lava-json`     | 数据交换与对象转换 | [概览](./lava-json/README.md)     |
| `lava-http`     | 外部接口调用       | [概览](./lava-http/README.md)     |
| `lava-crypto`   | 账号安全与密钥管理 | [概览](./lava-crypto/README.md)   |
| `lava-jwt`      | 登录态与服务间认证 | [概览](./lava-jwt/README.md)      |
| `lava-schedule` | 后台任务与周期任务 | [概览](./lava-schedule/README.md) |
| `lava-mail`     | 邮件发送与收取     | [概览](./lava-mail/README.md)     |
| `lava-bom`      | 多模块依赖版本管理 | [概览](./lava-bom/README.md)      |

更多入口见 [项目介绍](./introduction.md), [快速开始](./quick-start.md) 和 [模块概览](./modules.md).
