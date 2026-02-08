# Argon2id 用户登录密码加密落库指南

本指南适用于账号注册和登录场景, 前置条件是你已经引入 `lava-crypto` 并具备用户表读写能力.

## 完整流程

```java
import com.zhengshuyun.lava.crypto.CryptoUtil;
import com.zhengshuyun.lava.crypto.PasswordHasher;

import java.time.Instant;

public final class AccountPasswordService {

    // 1) 统一复用 PasswordHasher, 不要每次请求新建
    private static final PasswordHasher PASSWORD_HASHER = CryptoUtil.passwordHasher()
            .setMemoryKiB(65536)
            .setIterations(3)
            .setParallelism(1)
            .setSaltLengthBytes(16)
            .setHashLengthBytes(32)
            .build();

    private final UserRepository userRepository;

    public AccountPasswordService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public long register(String username, String rawPassword) {
        // 2) 注册时哈希后入库(存完整 PHC 字符串)
        String passwordHash = PASSWORD_HASHER.hash(rawPassword);
        return userRepository.insert(username, passwordHash, Instant.now());
    }

    public boolean login(String username, String rawPassword) {
        // 3) 查询用户并读取 password_hash
        UserAccount account = userRepository.findByUsername(username);
        if (account == null) {
            return false;
        }

        // 4) 校验明文和哈希是否匹配
        boolean ok = PASSWORD_HASHER.verify(rawPassword, account.passwordHash());
        if (!ok) {
            return false;
        }

        // 5) 参数升级窗口内可登录成功后重算并覆盖
        if (shouldUpgrade(account.passwordHash())) {
            String upgradedHash = PASSWORD_HASHER.hash(rawPassword);
            userRepository.updatePasswordHash(account.userId(), upgradedHash, Instant.now());
        }

        return true;
    }

    private boolean shouldUpgrade(String encodedHash) {
        // 示例策略: 不匹配当前参数模板则重算
        return !encodedHash.contains("m=65536,t=3,p=1");
    }

    public record UserAccount(long userId, String username, String passwordHash) {
    }

    public interface UserRepository {
        long insert(String username, String passwordHash, Instant updatedAt);

        UserAccount findByUsername(String username);

        void updatePasswordHash(long userId, String passwordHash, Instant updatedAt);
    }
}
```

- 注册只存 `passwordHash`, 不存明文密码.
- 登录使用 `verify(rawPassword, passwordHash)`, 不需要手工拆盐值.
- 参数升级时建议使用登录后重哈希策略, 避免批量离线迁移风险.

## 参数调优

| 参数 | 默认值 | 建议值 | 说明 |
|------|--------|--------|------|
| `memoryKiB` | `65536` | `65536` 起步 | 内存成本, 越高越抗 GPU 暴力破解 |
| `iterations` | `3` | `3` 起步 | 迭代轮数, 越高越慢 |
| `parallelism` | `1` | `1~4` | 与 CPU 和并发模型相关 |
| `saltLengthBytes` | `16` | `16` | 建议至少 `16` 字节 |
| `hashLengthBytes` | `32` | `32` | 常见默认值, 兼顾强度和长度 |

模块内置参数上限保护, 超限会在 `build()` 或 `verify()` 阶段抛出异常.

## 数据库落库建议

| 字段 | 建议类型 | 说明 |
|------|----------|------|
| `password_hash` | `VARCHAR(255)` 或更大 | 存完整 PHC 字符串 |
| `password_updated_at` | `DATETIME` / `TIMESTAMP` | 记录密码更新时间 |

- `password_hash` 建议整串保存, 不拆分盐值和参数.
- 对外日志不要打印完整哈希串.

## 生产环境注意事项

- 不要硬编码密码策略参数和私密配置, 建议使用配置中心.
- 明文密码, 哈希串, token 都不要打印日志.
- 登录失败提示统一返回通用文案, 避免泄露账号是否存在.
- 生产环境建议接入 KMS 管理其他密钥类机密, 不要与密码哈希逻辑混用.
