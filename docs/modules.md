# 模块概览

`Lava` 按基础设施能力拆分模块. 新项目建议先使用 `lava-bom` 管理版本, 再按场景选择模块.

| 模块            | 适用场景                             | 文档                              |
|-----------------|--------------------------------------|-----------------------------------|
| `lava-core`     | 基础工具, 重试, 时间, ID, IO         | [概览](./lava-core/README.md)     |
| `lava-json`     | JSON 编解码, Tree 模型, 自定义配置   | [概览](./lava-json/README.md)     |
| `lava-http`     | HTTP 请求, 响应读取, 客户端配置, SSE | [概览](./lava-http/README.md)     |
| `lava-crypto`   | 密码哈希, EC 密钥, 密钥格式          | [概览](./lava-crypto/README.md)   |
| `lava-jwt`      | JWT 签发, 验签, 解析, 算法选择       | [概览](./lava-jwt/README.md)      |
| `lava-schedule` | 间隔任务, Cron 任务, 任务生命周期    | [概览](./lava-schedule/README.md) |
| `lava-mail`     | SMTP 发信, IMAP 收信, 凭证和厂商预置 | [概览](./lava-mail/README.md)     |
| `lava-bom`      | Lava 子模块和第三方依赖版本管理      | [概览](./lava-bom/README.md)      |

## 推荐路径

- 首次接入: 先看 [快速开始](./quick-start.md).
- 只想了解定位: 看 [项目介绍](./introduction.md).
- 已知道目标模块: 直接进入对应模块概览.
