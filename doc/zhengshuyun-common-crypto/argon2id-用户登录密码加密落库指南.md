# Argon2id 用户登录密码加密落库指南

本文面向初学者, 用最少步骤讲清楚在 `zhengshuyun-common-crypto` 里怎么做“注册加密 + 登录校验 + 数据库存储”.

## 1. 先看结论

你只需要记住 3 件事:

1. 注册时: `hash(明文密码)` 后, 把结果存到 `password_hash`.
2. 登录时: `verify(明文密码, password_hash)` 做校验.
3. 落库时: 直接存完整 PHC 字符串, 不要自己拆盐值.

`PasswordHasher` 输出的是 PHC 格式, 例如:

`$argon2id$v=19$m=65536,t=3,p=1$<salt>$<hash>`

## 2. 模块里已经提供了什么

`zhengshuyun-common-crypto` 已经内置 `PasswordHasher`(Argon2id), 入口是 `CryptoUtil.passwordHasher()`.

- 算法固定为 `Argon2id`, 版本 `v=19`.
- `hash(...)` 每次自动生成随机盐, 所以同一密码每次结果都不同.
- `verify(...)` 会从数据库里的哈希串解析参数后再计算, 所以旧参数生成的哈希也能继续验证.
- `build/hash/verify` 使用同一组参数上限保护, 可降低恶意参数导致的资源耗尽风险.

## 3. 这 5 个参数是什么意思

| 参数 | 示例值 | 作用 | 新手建议 |
| --- | --- | --- | --- |
| `memoryKiB` | `65536` | 算法使用的内存大小(KiB), 越大越抗 GPU 暴力破解 | 先用 `65536` |
| `iterations` | `3` | 迭代轮数, 越大越慢 | 先用 `3` |
| `parallelism` | `1` | 并行度(线程通道数) | 先用 `1` |
| `saltLengthBytes` | `16` | 盐长度, 防止同密码同哈希 | 先用 `16` |
| `hashLengthBytes` | `32` | 输出哈希长度 | 先用 `32` |

这组默认值就是当前模块默认值, 可直接用于第一版上线.

参数取值范围如下, 超限会在 `build()` 阶段抛出 `IllegalArgumentException`:

- `memoryKiB`: `1 ~ 4194304`.
- `iterations`: `1 ~ 100`.
- `parallelism`: `1 ~ 128`.
- `saltLengthBytes`: `8 ~ 256`.
- `hashLengthBytes`: `4 ~ 256`.

## 4. 注册流程(加密并落库)

```java
import com.zhengshuyun.common.crypto.CryptoUtil;
import com.zhengshuyun.common.crypto.PasswordHasher;

public final class PasswordService {

    // 单例复用即可, 不需要每次请求都 new.
    private static final PasswordHasher PASSWORD_HASHER = CryptoUtil.passwordHasher()
            .setMemoryKiB(65536)      // 内存成本, 64 MiB
            .setIterations(3)         // 迭代次数
            .setParallelism(1)        // 并行度
            .setSaltLengthBytes(16)   // 盐长度, 16 bytes = 128 bit
            .setHashLengthBytes(32)   // 输出长度, 32 bytes = 256 bit
            .build();

    public String hashForStore(String rawPassword) {
        return PASSWORD_HASHER.hash(rawPassword);
    }
}
```

注册接口里这样做:

1. 接收用户输入的明文密码.
2. 调用 `hashForStore(rawPassword)`.
3. 把返回值写入数据库 `password_hash`.
4. 明文密码不要写日志, 不要入库, 不要发 MQ.

## 5. 登录流程(读取并校验)

```java
public boolean verifyLoginPassword(String rawPassword, String storedPasswordHash) {
    return PASSWORD_HASHER.verify(rawPassword, storedPasswordHash);
}
```

登录接口里这样做:

1. 根据账号查到用户记录和 `password_hash`.
2. 调用 `verifyLoginPassword(rawPassword, passwordHash)`.
3. 返回 `true` 则登录成功, `false` 则失败.

建议对外统一错误文案, 比如“用户名或密码错误”, 不要告诉用户“账号不存在”还是“密码错误”.

## 6. 数据库字段建议

建议最少有这两个字段:

- `password_hash`: 存完整 PHC 字符串, 建议 `VARCHAR(255)` 或更大.
- `password_updated_at`: 密码最后更新时间.

示例 DDL(MySQL):

```sql
ALTER TABLE user_account
    ADD COLUMN password_hash VARCHAR(255) NOT NULL COMMENT 'Argon2id PHC hash',
    ADD COLUMN password_updated_at DATETIME NOT NULL COMMENT '密码更新时间';
```

## 7. 参数升级怎么做

未来你可能把参数从 `m=65536,t=3,p=1` 升级到更高(但不超过上限).

可用这套平滑策略:

1. 新注册用户直接使用新参数.
2. 老用户登录成功后, 用新参数重新 `hash(rawPassword)`.
3. 把新哈希覆盖原 `password_hash`.

因为 `verify(...)` 使用的是“哈希串内的历史参数”, 所以迁移期间不会影响老用户登录.

## 8. 新手最常见误区

- 误区 1: 把密码做 AES 加密后落库. 错, 密码应使用单向哈希, 不应可逆.
- 误区 2: 盐值单独一列存库. 不需要, PHC 已包含盐和参数.
- 误区 3: 每次请求都创建新的 `PasswordHasher`. 没必要, 单例复用即可.
- 误区 4: 把明文密码或完整哈希打印到日志. 高风险, 必须避免.

## 9. 除了登录密码, 还能用于哪些场景?

本质上, Argon2id 适用于"只需要校验是否一致, 不需要还原明文"的秘密值.

常见可用场景:

- 管理后台账号口令、运维口令、合作方控制台口令.
- API Key / Access Token 的"服务端校验副本"(只做提交值比对, 不做明文回显).
- 本地解锁口令、支付二次确认口令、设备 PIN 等用户输入型口令.
- 系统发放的恢复码/备用码(落库保存哈希, 使用时做一次性校验).

不建议使用 Argon2id 的场景:

- 需要取回明文再调用下游系统的密钥(如第三方 API Secret、数据库密码、签名私钥). 这类应使用 KMS 或对称加密托管.
- 短期一次性验证码(短信/邮件 OTP). 这类更适合短 TTL + 限次 + 风控策略.

落库格式建议仍然是完整 PHC 串:

- `$argon2id$v=19$m=65536,t=3,p=4$<salt>$<hash>`
- 不拆列, 不改写, 不二次编码.

## 10. 一句话总结

用户密码场景下, 用 `PasswordHasher.hash()` 生成 PHC 全串直接落库, 用 `PasswordHasher.verify()` 登录校验, 就是最稳妥且实现成本最低的方案.
