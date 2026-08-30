# Argon2id 密码哈希

## 基本使用

```java
PasswordHasher hasher = PasswordHasher.create();
char[] password = readPassword();

try {
    String encoded = hasher.hash(password);
    boolean matches = hasher.verify(password, encoded);
    boolean upgrade = hasher.needsRehash(encoded);
} finally {
    Arrays.fill(password, '\0');
}
```

`char[]` 是主入口。Lava 借用但不修改调用方数组，调用方应在使用完成后清零。`String` 重载只是便利入口，原始不可变字符串无法从内存中主动清除。

空密码和全空白密码可以被算法处理；最小长度、复杂度、泄露密码检查和登录限速属于业务认证策略。

## 默认生成参数

| 参数 | 默认值 |
| --- | ---: |
| memory | 65536 KiB（64 MiB） |
| iterations | 3 |
| parallelism | 1 |
| salt | 16 字节 |
| hash | 32 字节 |

## 自定义策略

```java
PasswordHashPolicy.Generation generation =
        new PasswordHashPolicy.Generation(
                65_536,
                3,
                1,
                16,
                32
        );

PasswordHashPolicy.VerificationLimits limits =
        new PasswordHashPolicy.VerificationLimits(
                262_144,
                10,
                16,
                64,
                64,
                1_024
        );

PasswordHasher hasher = PasswordHasher.withPolicy(
        new PasswordHashPolicy(generation, limits)
);
```

生成参数和验证资源上限分离，避免攻击者提交超大 PHC 参数触发高额内存分配或 CPU 消耗。

## 验证语义

- 普通密码不匹配返回 `false`；
- 畸形、不支持或超过资源上限的 PHC 抛出 `CryptoException`；
- `needsRehash(...)` 只接受合法且处于限制内的 PHC；
- PHC 参数与当前生成策略不一致时返回需要升级。

不要记录密码、完整 PHC 或派生中间值。PHC 虽不包含明文密码，仍属于敏感认证数据。
