# 07 · 连接码兑换会话：能统一到什么程度

> 决策更新（2026-11-14）：本文保留原始研究结论与建议，不是当前实施规范。已确定先 LX、后 Lux；SDK 33/36、Hilt、dev/main、同包名开发/稳定通道、独立本机资料。最新要求见 [plan.md](../plan.md)，与历史建议冲突时以最新计划为准。

补充研究日期：2026-11-14。只讨论方案，不修改 server。依据 Lux dev `794fcc4cfee03cdc12c56dad0272f116e3f0d8a9`：

- [账号与同步用户映射](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/account/store.ts)
- [连接码认证与同步 key 有效性](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/server/auth.ts)
- [登录/注册、会话签发与 refresh](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/server/api/auth.ts)
- [账号和会话类型](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/account/types.ts)
- [REST 身份与权限检查](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/server/api/authGuard.ts)

## 结论

可以把“用户名密码”和“连接码”作为两个认证入口，在 **升级后的 Lux server** 上汇入同一种可续期会话。它解决的是认证入口、稳定身份和会话生命周期，而不是重写同步协议。

但不能凭新增 Lux 接口，让未修改的原版 LX server 也突然支持 access/refresh/logout。若仍直连原版 LX，客户端内部仍需一个小型 legacy 凭据驱动；它可以隐藏在统一会话接口后，UI/账号切换/资料库不需要重复实现。

## 已确认的身份映射

`getSyncUsers()` 同时返回两类同步身份：

1. **Lux managed user**：`user.username` 映射同步用户名，`user.lxSyncCode` 放进同步连接码字段。此类码可以明确找到原有 Lux userId，不需要再创建一个不同账号。
2. **config/env 用户**：来自旧配置或环境变量，没有相同的 managed account 记录；管理视图的 `config:用户名` 是展示 ID，不能直接当可签发当前 Lux 会话的稳定 subject。

因此“连接码是否能对应用户”不是全未知：managed 已经有关联，config/env 尚需账户身份策略。当前 `authGuard` 按 managed userId 查记录，所以只新增一个路由并把 config 名字塞进 token 不能生效。

## 建议的收敛路径

```text
用户名密码 ─→ 验证密码 ────────┐
                             ├→ 认证主体 + 本次授权范围
连接码 ───→ 验证码并解析主体 ───┘
                                → 同一 SessionIssuer
                                → access/refresh + 稳定身份
                                → /api/sync/key（绑定 session）
                                → 原有 LX v4 WebSocket 同步
```

接口示意（尚未定案）：`POST /api/auth/login/code`，输入 code、deviceId/deviceName/platform，输出与现代登录相同形状的会话结果。后续 refresh、退出、本机账号切换复用同一流程。连接码只用于首次认证，不作客户端持久身份，也不必长期保存；失去 refresh/key 后仍可能需要用户重新输入。

旧 `/ah`、hello/id、WebSocket 认证和 RPC 格式保持兼容；新 Android 在支持新入口的 Lux 上使用现代链路，旧 LX 客户端继续旧链路。

## 不能只考虑返回值相同

### 1. 同步授权不应无意升级为完整账号授权

当前连接码授权的是同步设备。直接调用现有 `loginCredentials(user)` 返回普通账号会话，会让持码者进入 requireAuth 保护的资料/会话/凭据等接口；若对应 admin，还可能获得 requireAdmin 权限。当前 guards 没有区分“密码认证”和“同步码认证”的 scope。

建议 token/session 模型统一，但授权能力区分：连接码会话默认只有约定的同步和必要会话生命周期权限；账号管理、改密、查看/重置同步码、撤销其他会话、管理后台等需要密码认证或明确提权。refresh 必须保留原授权范围，不能续一次就变成完整账号权限。

因此生产方案可以对旧客户端保持 API 向后兼容，但内部需要增加认证方式/权限约束及 guards 检查，不能承诺“只加一个接口、其他逻辑完全不动”。若产品有意把连接码定义为完整登录凭证，那是扩大凭据权限的产品决策，不是无风险的技术等价转换。

### 2. 旧配置用户需要稳定主体

推荐先支持 managed user 的连接码兑换。旧 config/env 若要纳入，需显式迁移/登记成持久账号主体并保持原资料目录映射；不要新建空资料、自动设置可猜测密码，或把用户名可变视图 ID 伪装成稳定 userId。是否采用统一 principal registry 还是纳入 managed store，须单独设计，不先建两套用户系统。

### 3. 会话必须连到实际同步授权

兑换出的 session 应通过现有 `/api/sync/key` 取得绑定 sessionId 的设备授权。否则 REST logout 了，旧 sessionless key 仍可能保持同步，用户以为退出实际没有退出。

当前 resetSyncCode 只撤销 sessionless 同步 key；若新增可续期 code session，还要规定重置连接码是否撤销既有 code sessions。建议区分认证方式，重置码时可撤销该码派生会话，不误伤密码会话。旧设备 key 与新 session 关联/别名不能顺便破坏其他在用客户端。

### 4. 码种类和请求策略

LX 同步连接码、注册邀请码、房间邀请码是三类凭据。兑换接口只接受同步连接码。走 HTTPS、限制失败尝试和重复提交，避免把码放 URL、日志或普通历史记录；不能利用新入口绕过旧认证失败限制。

## 三种方案的取舍

| 方案 | 客户端统一程度 | 服务端改动 | 原版 LX 直连 |
| --- | --- | --- | --- |
| 统一 App Session + Lux/LX 凭据驱动 | UI、切换、资料、错误模型统一；凭据续期方式不同 | 无 | 保留 |
| Lux 增加连接码会话入口，客户端保留 legacy 驱动 | 升级 Lux 上认证/续期也统一；旧 LX 差异局限在一个 adapter | 新入口 + scope/身份/撤销关联等内部扩展，旧协议不破坏 | 保留 |
| 网关把原版 LX 包装成 Lux | App 可只面对 Lux 风格认证 | 新服务、部署、会话/身份映射、代理与信任边界 | 需经网关，不再是无附加组件的直连 |

推荐第二种作为可以讨论的演进方向；在后端尚未升级或坚持 server 不改时，第一种仍然成立。为了表面“一套 token”而引入网关通常会增加本项目希望减少的运维和状态复杂度。

不建议以统一为由把 v1 直接缩成“只支持用户名密码”：那会失去已要求的原版 LX 兼容。可以统一应用层，而诚实保留最小协议差异。

## 决策前的边界

本轮没有授权或执行 server PR。若以后选定此方向，应先确认：连接码会话权限、config/env 身份处理、重置码及退出撤销规则、设备 key 的关联、server-info 能力声明与旧客户端回归。待确认后再做可审阅的后端合同提案和实现，不将本文当作已部署能力。
