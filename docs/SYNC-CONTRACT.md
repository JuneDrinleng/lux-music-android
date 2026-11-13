# 与 lux-music-server 的同步契约

## 证据范围

- server：只读检出 `_base-server`，提交 `794fcc4cfee03cdc12c56dad0272f116e3f0d8a9`；[固定源码](https://github.com/JuneDrinleng/lux-music-server/tree/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9)。工作树已有 admin-ui/dist 删除和 `.lock-installed` 未跟踪项，本任务未改动它们；引用的是未改动的 src 代码。
- mobile：只读检出 `playdetail-fixed-lyric-height`，提交 `3ab131e141ccbdc807d200a99eb216a17017c8c8`；[固定源码](https://github.com/JuneDrinleng/lux-music-mobile/tree/3ab131e141ccbdc807d200a99eb216a17017c8c8)。
- 下文用 S:/M: 标识相对对应仓库的路径。它是已读版本的契约摘录，不是对线上部署版本的保证。没有运行真实账号登录或同步，没有读取运行时凭据；示例仅表示类型。

原生 App 直接连接既有 server，不新增后端、不调整服务端模型。账号 HTTP、同步设备 key、WS RPC 是不同步骤；不能把 HTTP 歌单接口与 WS 快照同步混写成两个数据源。

## HTTP 账号与设备初始化

依据 S:`src/server/api/auth.ts`、`authGuard.ts`、`sync.ts`、`errors.ts`；M:`src/plugins/sync/client/auth.ts` 的 authenticateLuxCandidate/getLuxSyncKey 与 `src/utils/luxAuthApi.ts`。

| 方法/路径 | 输入 | 返回/行为 |
| --- | --- | --- |
| GET `/api/server-info` | 无；S:`src/server/app.ts` | name/version/packageVersion/minClientVersion/publicUrl/adminContact、registration、features 等；HTTP features 是账号能力列表，不等于 WS 的 list/dislike 版本映射 |
| POST `/api/auth/login` | `{username, password}`，可附 deviceId/deviceName/platform（loginCredentials 使用） | `{token, accessToken, accessTokenExpiresAt, sessionId, refreshToken, user}`；user 是公开资料，含 id/username/displayName/avatar/gender/signature/role/source/status。mobile 登录路径优先采用 accessToken，不能把旧 token 当成唯一新契约 |
| GET 或 POST `/api/auth/invite/check` | query/body `{inviteCode}`；mobile AUTH_CONTRACT 用 POST | `{valid, inviterName, role, expiresAt}`；无效/过期返回错误 |
| POST `/api/auth/register` | `{inviteCode, username, password, displayName?}`，可附设备元信息 | 同 loginCredentials；本原生 MVP 是否开放注册 UI 待定 |
| POST `/api/auth/refresh` | `{refreshToken}` | 轮换后 `{accessToken, accessTokenExpiresAt, sessionId, refreshToken}`；失败 401。M 的刷新客户端有单航班与过期回写防护，原生应保留语义 |
| GET `/api/auth/me` | Authorization: Bearer（占位，非实际值） | `{user}` |
| POST `/api/auth/logout` | Bearer | `{ok:true}`；有 sessionId 时撤销该会话，否则失效用户会话 |
| POST `/api/sync/key` | Bearer；JSON `{deviceId?, clientId?, deviceName?, platform?}` | `{clientId, key, serverName, deviceName}`；Cache-Control: no-store；deviceId/clientId 若提供必须为非空、长度不超过 256 的字符串；冲突 409 DEVICE_CONFLICT。platform 含 mobile 时标记 isMobile |

HTTP auth guard 校验 active 用户、sessionVersion 和可选 sid 的活跃性；不能只靠客户端“已登录”标志。`sendError` 的格式为 `{message, code}`，不要编造统一 `{ok:false}` 的 HTTP 包装。典型登录错误为 INVALID_CREDENTIALS/UNAUTHENTICATED，限流为 RATE_LIMITED；以各 handler 为准。

M:`src/plugins/sync/deviceId.ts`/`deviceIdentity.ts` 生成并持久安装身份，`client/auth.ts` 请求 key 时传设备信息；M 的请求构建代码中 platform 为 `lux_music_mobile`。原生建议用新的稳定安装 ID、platform `lux_music_android_mobile`（**提案字段值**，server 按 includes('mobile') 接受），不得复用 RN 安装的 deviceId 或 clientId。key 的服务端生命周期关联账号 session（S:`src/server/auth.ts` 的 isSyncKeyActive、`src/server/server.ts` 的 registerSyncKeyRevocation）；注销/撤销后要断开和重新认证。

旧 LX 连接码路径仍见 S:`src/server/app.ts` 的 `/hello`、`/id`、`/ah` 与 `src/server/auth.ts`，M:`src/plugins/sync/client/auth.ts` 有 codeAuth/keyAuth。它们涉及已有 AES/RSA 握手。原生 v1 建议只做 Lux 账号路径，不引入旧连接码、旧 token 存储迁移等兼容入口；若 June 要求，另行核定覆盖范围。保留**现行 WS 握手**所需 codec 不是新增旧 API shim。

## WebSocket 连接与帧

依据 M:`src/plugins/sync/client/client.ts` 的 connect，S:`src/server/server.ts`、`auth.ts`、`messageQueue.ts` 与 `src/utils/tools.ts`。

1. mobile 连接 `<wsProtocol>//<hostPath>/socket?i=<编码后的 clientId>&t=<编码后的认证密文>`；`t` 是用 key 对 `SYNC_CODE.msgConnect` 做 AES 处理。S:`src/constants.ts` 的 msgConnect 为 `lx-music connect`，S:authConnect 从 query i/t 校验。server 当前 upgrade handler 未限定 pathname，但新客户端应沿用 `/socket`，不能据此虚构新的路径。
2. 服务端 AES helper 使用 AES-128-ECB、base64 key/密文；M:`src/plugins/sync/utils.ts` 通过 native crypto 调用，枚举名含 NoPadding，不能只凭名称推导实际填充。**跨语言填充、UTF-8/base64 行为需第 0 阶段用服务端向量确认**，此文不提供未验证的 Kotlin 伪实现。
3. 连接后使用 `message2call` RPC，`createQueueRemote('list')` 与 `('dislike')` 为有序业务队列。RPC 外层请求 ID、method/path、错误帧等由依赖决定；本次未审阅该依赖实现，**精确帧 envelope 待确认**，不得直接把 `{action,data}` 当整个 WS 帧。后续需从锁定依赖/真实互操作 fixture 得出 Kotlin codec。
4. S:`encryptMsg` **没有对普通正文加密**：JS 字符串长度大于 1024 时发送 `cg_` + gzip 后 base64，否则原文；decryptMsg 检查前缀解压。不能根据函数名宣称消息 E2EE；部署应使用 HTTPS/WSS。压缩阈值以 JS 字符串长度语义为准。服务端 `createMessageQueue` 保持异步 codec 与分发的线序，但不等待每个异步 RPC handler 完成，以免相互等待回复导致死锁。
5. S:server.ts 每 30000ms 发 WS ping，isMobile 客户端另发应用消息 `ping`；M:client.ts 处理裸文本 `ping` 并维护超时，不能把它交给 JSON 解析器。正常关闭码 1000、失败 4100 来自 S:`src/constants.ts`。新客户端的重试退避是自身策略，不是服务端保证。

## 能力协商与就绪

S:`src/server/sync/sync.ts` 先调用客户端 `getEnabledFeatures('server', featureVersion)`，再经 handler.onFeatureChanged 逐模块同步，最后回调客户端 `finished()`。S:`src/modules/index.ts` 广播 list/dislike 版本 1，并有 playHistory/outcome/together 等扩展。M:`src/plugins/sync/client/sync/handler.ts` 对版本相等的 list/dislike 返回 `{skipSnapshot:false}`，Lux 模式才启用适配版本的 playHistory/together。

原生 MVP 仅启用已实现的 list 能力，其余省略/禁用，不虚报支持。模块完成与连接完成是不同状态；M:`client/modules/list/handler.ts` 在 list_sync_finished 后才置 moduleReadys.list、注册本地发送事件。S:`src/server/sync/handler.ts` 禁用时立即撤销模块 readiness。每个异步回调须检查连接代际/账号空间；断线立即撤销就绪，不能把 WS open 当成可写。

## 歌单快照与 RPC

S:`src/types/sync_common.d.ts`、`list_sync.d.ts`、`list.d.ts`，S:`src/modules/list/sync/sync.ts`/`handler.ts`；M:`src/plugins/sync/client/modules/list/handler.ts`、`src/plugins/sync/listEvent.ts`。

| 调用方向 | 方法 | 参数/结果 |
| --- | --- | --- |
| server → client | list_sync_get_md5 | 无参 → MD5 字符串；M 计算 `toMD5(JSON.stringify(await getLocalListData()))` |
| server → client | list_sync_get_sync_mode | 无参 → SyncMode |
| server → client | list_sync_get_list_data | 无参 → ListData |
| server → client | list_sync_set_list_data | ListData → 完成；原子落库后才能确认 |
| server → client | list_sync_finished | 无参 → 完成；模块进入可收发状态 |
| 双向 | onListSyncAction | ActionList → 完成；走 list 队列，不能绕开就绪与顺序 |

快照 `ListData` 为 `{defaultList: MusicInfo[], loveList: MusicInfo[], userList: UserListInfoFull[]}`；**不含 tempList**（类型是 Omit）。用户列表为 `{id, name, source?, sourceListId?, locationUpdateTime: number|null, list: MusicInfo[]}`；保留 id 为 `default`、`love`，临时队列不是同步歌单。`locationUpdateTime` 不应擅自解释为歌曲内容版本。

MusicInfo 的 wire 见 S:`src/types/music.d.ts`：

- 共用 `{id:string, name:string, singer:string, source, interval:string|null, meta}`，meta 基础字段 `{songId:string|number, albumName:string, picUrl?:string|null}`。
- online 共用 `qualitys:[{type,size:string|null}]` 与 `_qualitys` 映射、可选 albumId；kg 另有 meta.hash、各质量 hash；tx 另有 strMediaMid、可选 id/albumMid；mg 另有 copyrightId 与可选 lrcUrl/mrcUrl/trcUrl；kw/wy 用共用结构。
- local 类型的 meta 含 filePath/ext。**类型文件允许 local，不等于另一台设备能访问同一路径**；原生接收时标记不可在本机解析，不能把远端路径当成本地 URI 或删掉后回写。
- Quality/Source 字面值与未见字段的容错需由完整 fixture 锁定；JSON 属性省略/null、数字/字符串身份、字段顺序和中文编码都会影响序列化。原生不可用 Kotlin 默认序列化结果直接假定 MD5 一致。

动作是 RPC 参数中的判别联合 `{action, data}`：

| action | data（与 S:list.d.ts 一致） |
| --- | --- |
| list_data_overwrite | ListActionDataOverwrite：完整列表结构，tempList 在该动作类型中可选；不可与常规 ListData 混为一谈 |
| list_create | `{position, listInfos:UserListInfo[]}` |
| list_remove | `string[]` |
| list_update | `UserListInfo[]` |
| list_update_position | `{ids:string[], position:number}` |
| list_music_add | `{id, musicInfos:MusicInfo[], addMusicLocationType}` |
| list_music_move | `{fromId, toId, musicInfos:MusicInfo[], addMusicLocationType}` |
| list_music_remove | `{listId, ids:string[]}` |
| list_music_update | `[{id, musicInfo:MusicInfo}]` |
| list_music_update_position | `{listId, position, ids:string[]}` |
| list_music_overwrite | `{listId, musicInfos:MusicInfo[]}` |
| list_music_clear | `string[]`（列表 ID） |

`addMusicLocationType` 在实际处理里为 top/bottom，位置数值边界按 handler/管理代码验收，不编造所有动作统一校验规则。S:applyListAction 共享 mutation/snapshot/broadcast 路径，并更新来源设备快照，给其他同用户且 list ready 的连接发动作。它没有给客户端提供通用幂等操作 ID，因此超时不等于未执行；重试前应进入重协调，不能宣称恰好一次投递。

## 初次同步、快照与冲突

S:`src/modules/list/sync/sync.ts` 在存在可用设备快照且未 skipSnapshot 时，先用 MD5 检查是否一致，再决定是否走三方快照合并；否则双方非空时询问模式，单侧空时复制非空侧。因此“server 优先”是首次引导策略，不应绕开现有冲突逻辑强行覆盖。正常增量后 server 为设备维护快照；不能只做 pull 全量就宣称完整互操作。

SyncMode 来自 S:`src/types/list_sync.d.ts`：merge_local_remote、merge_remote_local、overwrite_local_remote、overwrite_remote_local、overwrite_local_remote_full、overwrite_remote_local_full、cancel。S:`src/constants.ts` 的 TRANS_MODE **会反转客户端视角方向**；在原生 UI 中 local=本机、remote=server，必须通过双端 fixture 证明方向。

普通 overwrite 的 `overwriteList` 仍保留目标端独有用户列表，full 才是整包采用单侧；不要把两者都标“完全覆盖”。三方合并涉及基准快照删除和顺序规则，不是简单数组并集。M:`src/core/sync.ts` 只记忆 merge、不记忆 overwrite；原生保留该确认语义。

## 其他同步能力（MVP 后）

不喜欢：S:`src/types/dislike_list.d.ts` 定义 DislikeRules 为字符串；`dislike_sync.d.ts` 动作为 dislike_data_overwrite（字符串）、dislike_music_add（`{name,singer}[]`）、dislike_music_clear（无 data），不应杜撰成歌曲 ID 列表。RPC 包括 dislike_sync_get_md5/get_sync_mode/get_list_data/set_list_data/finished 与 onDislikeSyncAction，定义在 sync_common.d.ts；规则字符串完整语法尚未审阅，**待确认**后才实现。

历史：S:`src/modules/playHistory/protocol.ts`、`validate.ts`、`store.ts` 和 `src/types/play_history.d.ts`；M:`src/plugins/sync/playHistorySync.ts` 实际在握手完成后分段 pull/push，并按服务器与用户保留游标。

- RPC `playHistory:push({records}) → {accepted, ignored, cursor}`；`playHistory:pull({since?}) → {records, cursor, hasMore}`，都是单段方法名。
- Record 为 `{id, deviceId, startedAt, endedAt, listenedMs, completed?, skipped?, song}`；song 为 `{source, songmid, name, singer, albumName?, interval?, img?}`，与歌单 MusicInfo **不是同一结构**。
- 校验要求 id=`deviceId:startedAt`，时间/时长为非负安全整数，completed/skipped 不可同时为 true。不要擅自增加此处源码没有的时间大小关系保证。
- 当前 maxBatch=500、maxPage=1000，来源 protocol.ts；同 id 首写优先、重复计 ignored，游标是服务端 receivedAt 单调值，不是歌曲播放时间。按 hasMore 拉取，持久化结果后才推进本地游标；该保证需由原生事务落实。

一起听/设备控制：S:`src/modules/index.ts` 有 together 扩展和 device.command；M:client.ts 处理 together_sync_finished，`src/store/together/action.ts` 管理房间。完整房间/控制协议不在本次 MVP 展开范围，**待后续专项确认**，不得通过虚报能力启动推送。

S:`src/server/api/data.ts` 另外有 **PAT scope 鉴权**的 `/api/v1/playlists`、`/api/v1/playlists/:id`、`/api/v1/playlists/:id/songs`、history/stats/devices 接口。这些不是 mobile 当前 WS 快照同步的替代方案；本次原生方案不引入第二套 PAT 歌单写入链，也不声称它接受普通账号 accessToken。

## 实施前必须关闭的未知项

1. message2call 锁定版本的逐字节帧、错误和队列 envelope；Java/Kotlin 对 JSON 数字、属性顺序、可选字段的编码与快照 MD5 向量。
2. 握手加密填充/UTF-8 的实际互操作、代理子路径与 HTTPS/WSS 部署方式；设备同一身份并发连接/撤销时的客户端恢复。
3. 双端冲突各方向、三方删除/排序、未知来源/字段和 local 路径的无损往返；不能默默把不能理解的数据变成空对象。
4. 半发送/已执行但回复丢失时的协调策略；协议没有通用幂等键，原生本地 outbox 不能凭空改变此事实。
5. 后续能力的规则语法和完整 RPC；线上 server 版本若与本证据提交不同，应先重新核对并更新契约 fixture，不要求修改 server 来迁就客户端。

这些未知项是 [ROADMAP](ROADMAP.md) 第 0 阶段的验收输入。全部结论为文档设计与静态阅读结果，本 PR 没有声称完成端到端验证。

## 账号方案的进一步约束（2026-11-13）

[ACCOUNT-AUTH](ACCOUNT-AUTH.md) 明确 AccountSession 是唯一账号端口、登录和注册都附设备信息、注册直接使用返回凭据、退出调用 POST /api/auth/logout；这三项当前 server 已支持，无需等待 P0/P1。原生只使用 sid 绑定的 accessToken，不把旧 token 当新会话替身；refreshToken 轮换不确定时不重放。当前 GET /id 的稳定身份适配与拟在 server-info 增加 serverId 需区分，后者必须经 June 批准另开 server PR。v1 是否做本地模式/LX、账号上限及离线撤销租约仍待定；完整 P0/P1 为提案，不是本文已实现 API 的补充保证。
