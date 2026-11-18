# 02 · 不改 server 的兼容边界

> 决策更新（2026-11-14）：本文保留原始研究结论与建议，不是当前实施规范。已确定先 LX、后 Lux；SDK 33/36、Hilt、dev/main、同包名开发/稳定通道、独立本机资料。最新要求见 [plan.md](../plan.md)，与历史建议冲突时以最新计划为准。

证据：[LX](sources.md#lx-server)、[Lux master/dev](sources.md#lux-server)、[mobile](sources.md#mobile)、[message2call 0.1.3](sources.md#message2call)。除特别注明，以下为固定源码静态核实，尚未做 Kotlin/Node 联调。

## 服务器能力矩阵

| 能力 | 原版 LX（2.1.2 / d47aca4） | Lux master（7834abd） | Lux dev（794fcc4） |
| --- | --- | --- | --- |
| `/hello`、`/id`、`/ah` | 有 | 有 | 有 |
| `/socket?i=&t=` 与 list/dislike | 有 | 有 | 有 |
| `/api/server-info` | 无 | 无 | 有：name、version、minClientVersion、registration、features 等 |
| 账号登录 | 无 | POST login → `token,user` | POST login → `token,accessToken,accessTokenExpiresAt,sessionId,refreshToken,user` |
| 注册 | 无客户端注册 API | 邀请码注册 → `{user}`，**不自动登录** | 邀请码注册 → 登录凭据 |
| 邀请预检 | 无 | 无 | GET/POST `/api/auth/invite/check` |
| refresh | 无 | 无 | POST `/api/auth/refresh`，轮换 refresh token |
| 账号换同步 key | 无 | POST `/api/sync/key`，每次创建 key | 同路径，支持 deviceId/clientId 复用/冲突检测，关联 session |
| 退出 | 断开/本地处理，设备 key 不因此撤销 | `/api/auth/logout` 仅 `{ok:true}` | 当前 session 撤销；sessionless 旧 token 情形会影响更广范围 |
| 改密 | 无账号 API | `password`，无旧密码字段要求 | `oldPassword,password,revokeOtherDevices?`；返回新登录凭据 |
| 会话管理 | 无 | 无 me/sessions | 有 me/sessions、撤销其他会话 |
| 个人资料/设备/同步码 | 无同类 REST | 有 `/api/me/*` | 有并扩展 |
| 历史、一起听、设备命令 | 无原生支持 | 不应假设支持 | WebSocket feature 协商扩展，逐版本匹配 |

注意：分支名称只是本次研究标签，不是客户端运行时能查询的协议版本。实际用户部署可能是其他提交或反向代理，不能通过名字硬编码。

## 建议的适配策略

对外只有账号仓库和同步端口；内部保留三种 **明确、可测试** 的 auth adapter：LX connection-code、Lux legacy、Lux renewable。后两者共享非认证的 REST/DTO/同步代码，不复制整个网络栈。

1. 先规范化用户确认的 URL，保留部署子路径；`host/a` 和 `host/b` 不可折叠。TLS 失败不自动改 HTTP，也不关闭证书检查。
2. 读取 `/hello` 和 `/id` 验证 LX v4 协议及 server identity。读取 `/api/server-info` 可识别现代 Lux。
3. 缺失 server-info 不等于网络断开，也不充分证明是原版 LX：上游未知路径可能返回 401 Forbidden，Lux 旧版可能 404。不要把 401 探测结果当凭据过期。
4. 无现代元信息时，由明确选择的“Lux 账号”或“LX 连接码”进入对应表单；旧 Lux 可以依据既有 API 返回确认能力，禁止乱试密码/连接码。
5. `features` 声明 REST 会话能力；一起听、历史等要等 WebSocket `getEnabledFeatures` 的版本协商，不能只判断 `serverName` 含 Lux。
6. 不把 Android 自身 0.x SemVer 与旧 mobile minClientVersion 直接比较：新客户端与 RN 不是同一版本序列。未来需定义客户端兼容版本映射；无法确认时展示明确兼容提示，不伪报“Android 太旧”。

建议最低覆盖原版 LX v4 与本次两个 Lux 快照；不承诺任意历史 fork。兼容成本集中在 adapter 和 fixtures，不能散入 Composable、ViewModel、数据库实体。

## LX 的首次认证与重连

已核实的逻辑链：

```text
hello (Hello~::^-^::~v4~)
  → id (OjppZDo6 + serverId)
  → 连接码派生 key + 临时 RSA 公钥
  → GET /ah, header m
  → RSA 解密 {clientId,key,serverName}
  → 持久化设备凭据引用
  → WS /socket?i=clientId&t=加密连接文本
```

- 连接码 key：MD5 的十六进制字符串前 16 个字符，再按字符字节编码成 base64；不是 MD5 原始 digest 的前 16 字节。
- 首次认证明文行：`lx-music auth::`、公钥主体、设备名、`lx_music_mobile`。mobile 追加 deviceId，第五行被旧上游忽略；新客户端 platform 标记不能随意改成上游无法识别的值。
- 服务端通过能解密的连接码定位用户，首次请求没有用户名字段；LX 表单不应该要求用户提供虚构的 Lux 用户名。
- 保存后的 key 重连使用 `/ah` header `i=clientId`、`m=加密(authMsg+deviceName)`；WS 的 `t` 加密文本严格为 `lx-music connect`。
- 服务端 AES 是 128-bit ECB、Node 默认 padding；RSA 为 OAEP（默认 SHA-1）。mobile 的 `AES_MODE.ECB_128_NoPadding` 实际字符串值是 `AES`，并不是 `AES/ECB/NoPadding`；不能仅据枚举名字移植。需以 server 结果和 cross-language golden vectors 核对平台默认填充、UTF-8、base64 和 OAEP MGF 参数。
- MD5/ECB 是既有 wire 兼容限制，只能封装在协议边界，不能拿来加密 Android 的本地凭据；传输使用可验证的 HTTPS/WSS。
- 不把连接码当稳定账号 ID；改码后可以仍是同一远端用户，但旧协议没有公开稳定 user ID。见 04 的 LX 本地 binding 方案与待确认项。

## Lux 登录、注册与凭据生命周期

| 端点 | 请求要点 | 客户端处理 |
| --- | --- | --- |
| POST `/api/auth/login` | username/password；现代附设备信息 | 优先现代 `accessToken`，保留明确 legacy 结果解析；不要同时用两种 token 随机重试 |
| POST `/api/auth/register` | inviteCode、username、password、可选 displayName | 现代直接接凭据；旧版返回 user 后显式登录，注册成功但登录失败需保留成功状态，不反复注册 |
| POST `/api/auth/refresh` | refreshToken | 按账号 single-flight；安全持久化新 refresh 后发布；请求最多一次认证重试 |
| GET `/api/auth/me` | Bearer token | 验证身份；离线/超时不清资料；401 与权限 403 分类 |
| POST `/api/sync/key` | deviceName/platform；现代含 deviceId、可选 clientId | 复用同账户授权 key，不每次重连创建设备；modern 409 DEVICE_CONFLICT 不无限重试 |
| GET/PATCH `/api/me/profile` | profile 字段 | profile 与身份 ID 分开；昵称变化不创建资料空间 |
| POST `/api/me/password` | 依 adapter 的字段 | 现代新凭据需作为一组保存；旧 token/session 语义单独呈现 |
| POST `/api/auth/logout` | 现代 Bearer | 区分远端撤销、本机退出、删除缓存；旧版不承诺远端 token 已失效 |

已看到的 token `iat/exp` 使用 `Date.now()` 毫秒；不能让通用 JWT 库按“秒”自行判断。现代 `accessTokenExpiresAt` 也为毫秒。现代默认 access TTL 3600 秒，refresh 默认 90 天且可配置，不能在 UI 写死有效期。

源码中没有通用邮件验证码、短信登录、OAuth、邮箱重置密码 API。本客户端不能为了模仿商业软件而添加会调用不存在接口的入口；忘记密码提供管理员帮助，不能承诺已发邮件。不要在客户端暴露管理员 bootstrap 作为普通注册流程。

## WebSocket 与 RPC：最容易被遗漏的兼容点

所有研究的 server/mobile lockfile 都解析到 **message2call 0.1.3**。仓库默认主干已是 2.0.3，格式不同。本次另读取 tag `v0.1.3`；不能直接照最新库移植。

0.1.3 的消息对象不是 JSON-RPC 2.0：

```json
{"name":"唯一请求关联值","path":["list_sync_get_md5"],"data":[]}
```

成功响应含相同 `name`、`error:null`、可选 `data`；失败为 `error` 字符串。`path` 是字符串数组，`data` 是调用参数数组；`playHistory:push` 是单个 path 段，不按冒号拆开。省略的 undefined 与显式 null 也需要区分。按 list/dislike 分组的请求需要串行；连接关闭必须结束所有等待请求。

- payload ≤1024 个 JS 字符时为 JSON 文本；更长可为 `cg_` + base64(gzip(JSON))。注意 Kotlin 字符/字节计数差异，接收必须兼容两者。
- 这里 `encryptMsg` 名字容易误导：正文当前并未逐帧 AES 加密，不能擅自新增密文包装。
- 同时存在 WebSocket 原生 ping/pong 与应用文本 `ping`（mobile 心跳观察）。要对照 server 的实际消息分派和超时；不要把 `ping` 交给 JSON 解析器。
- server 主动调用客户端协商、拉数据、请求合并模式；客户端不只是“连上后推本地列表”。

## 初始同步、实时变更与校验

```text
WS 建立
 → server 调 getEnabledFeatures(serverType, versions)
 → 按 list/dislike（现代可扩展 history/together）完成同步
 → 每个模块 ready
 → finished()
 → 允许对应模块发送本地实时 action
```

list 数据：`defaultList`、`loveList`、`userList`（每项包含列表元信息及 `list`）；`tempList` 不进入同步。userList 的字段顺序在 `buildUserListInfoFull` 中刻意统一。

关键 RPC：`list_sync_get_md5/get_sync_mode/get_list_data/set_list_data/finished` 与 dislike 对应方法；实时 `onListSyncAction`、`onDislikeSyncAction`、动态 `onFeatureChanged`。list 的新增、移除、更新、移动、排序、覆盖、清空 action 逐一按 `types/list_sync.d.ts` 编码，不用“整表上传”取代全部增量协议。

校验是对 `JSON.stringify` 结果做 MD5，不是语义相等就一定同 hash。必须检查字段顺序、缺失字段、null、Unicode、数值字符串、数组顺序。不得擅自按键排序再称“规范化兼容”。内部 Room model 不直接序列化成 wire model；未知可往返字段保留在 adapter 的扩展字段区，避免一同步就删掉其他客户端元信息。

有共同快照时 server 做三方合并；没有快照时可能让用户选合并/覆盖/取消。`merge_local_remote` 等方向会在 server 的 TRANS_MODE 反转到对应视角，UI 必须用“保留本机/使用服务器/合并”明确描述，不能直译枚举误导覆盖方向。

原协议没有通用客户端 mutationId 去重确认，因此不能设计一个盲目无限重放的 outbox 并声称 exactly-once。可以本地记录待同步状态，但断线后必须在初始快照协调后再决定发送/对账；远端应用通过 origin 标记避免回声。

现代 history 支持 `playHistory:push/pull`，batch 上限 500；together/chat/request/reaction 等各有 feature version 和 ready 生命周期。原版 LX 不协商这些能力时保留本地统计，并提供明确的“不支持”反馈，不改 server 造接口。

## 必须实测后才能关闭的风险

1. Kotlin ↔ Node AES/RSA、base64/gzip、message2call 0.1.3、中文/emoji/空值黄金样例。
2. JSON/MD5 一致性及列表顺序、三种服务端的合并/覆盖/取消。
3. HTTP 与 WS 都经过反向代理子路径，且 endpoint 不意外丢前缀。
4. 续期轮换期间进程死亡、切账号晚响应、key 被撤销、device 冲突、401/403/429/5xx 的不同恢复。
5. 两客户端并发编辑、重连、模块部分失败、旧连接晚消息不得污染新资料空间。

这轮没有运行这些测试，因此结论是“具备可实现合同与明确风险”，不是“已经兼容”。不得通过修改 server 让测试通过。
