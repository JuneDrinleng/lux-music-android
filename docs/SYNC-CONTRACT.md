# 原版 LX Music 同步契约（首版）

2026-11-14。首版目标是原版 LX sync server，不依赖 Lux REST。以下为固定源码核实，尚无 Kotlin/Node 联调或设备运行结论；不得改 server 使测试通过。

## 固定证据

- [LX server d47aca4](https://github.com/lyswhut/lx-music-sync-server/tree/d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36)（package 2.1.2）：src/server/auth.ts、server.ts、app.ts，src/constants.ts、src/utils/tools.ts，src/server/sync/，src/modules/list/，src/types/list*.d.ts、music.d.ts。
- [message2call v0.1.3](https://github.com/lyswhut/message2call/tree/2a5c8b966dc689271c2d5c47ad02587cb896ede3)，不是当前 v2。
- [mobile 研究快照](https://github.com/JuneDrinleng/lux-music-mobile/tree/702113391333c463049c0f7b449ff2c02b3758ba) 提供客户端对照；[研究](../research/02-protocol-compatibility.md) 和 [来源](../research/sources.md) 给出逐文件证据。
- 下方沿用的详细歌单清单以 Lux S:（794fcc4）和 mobile M:（3ab131e）为对照，原版 LX 类型/处理路径也在上述固定提交中；fixtures 必须以原版行为生成，不将 Lux 扩展误作 LX 保证。

## 首次认证与密钥重连

```text
GET /hello → Hello~::^-^::~v4~
GET /id → OjppZDo6 + serverId
GET /ah（m：连接码派生 AES 加密文本）
→ 临时 RSA 私钥解出 clientId/key/serverName
→ 加密保存设备凭据
→ WS /socket?i=clientId&t=加密连接文本
```

连接码派生为 MD5 十六进制字符串前 16 个字符的 UTF-8 字节，再 base64；不能使用原始 MD5 digest 前 16 字节。首次明文是 authMsg、LF、公钥主体、LF、deviceName、LF、lx_music_mobile。mobile 第五行 deviceId 被原上游忽略，不假定提供稳定账号身份。

AES 为 128-bit ECB、Node 默认 padding；RSA OAEP 默认 SHA-1。以实际跨语言向量核实，不能照 mobile 枚举 ECB_128_NoPadding 的名字判断，它的实际字符串值为 AES。wire 限制封装于协议层，本地凭据用 Keystore 而非 ECB。

密钥重认证使用 /ah headers i=clientId、m=encrypt(authMsg+deviceName)，成功返回加密 hello；WS t 为 encrypt("lx-music connect", key)，作为 query 参数正确编码。响应和日志不暴露 key、连接码或 t。

## 帧、调用与生命周期

message2call 0.1.3 请求形状为 `{name:关联值,path:[方法名],data:[参数]}`；成功响应为同 name、error:null、可选 data，失败 error 为字符串。它不是 JSON-RPC 2.0；省略 data 与 data:null 需区分。

长度不超过 1024 个 JS 字符的消息可为普通 JSON；长消息可用 cg_ + base64(gzip(JSON))。正文不是逐帧 AES。中文、emoji、UTF-16 字符计数、null/省略/数值字符串和压缩边界都要 golden fixtures。

分别处理原生 WS ping/pong 与应用文本 ping；后者不交 JSON parser。断线取消等待 RPC、撤销模块就绪，按受控退避重连。消息解码保持接收次序，不能为了串行而阻塞异步 handler 等待互相回复造成死锁。

server 主动调用 getEnabledFeatures('server', versions)，首版仅接受 list v1，未实现 dislike/history/together 不宣告支持。list_sync_finished 后 list ready，finished 是整体回调；WS open 不等于可以发送动作。所有晚回调检查资料/代际。

## 歌单快照与 RPC

Lux 对照 S:`src/types/sync_common.d.ts`、`list_sync.d.ts`、`list.d.ts`，Lux 对照 S:`src/modules/list/sync/sync.ts`/`handler.ts`；mobile 对照 M:`src/plugins/sync/client/modules/list/handler.ts`、`src/plugins/sync/listEvent.ts`。

| 调用方向 | 方法 | 参数/结果 |
| --- | --- | --- |
| server → client | list_sync_get_md5 | 无参 → MD5 字符串；M 计算 `toMD5(JSON.stringify(await getLocalListData()))` |
| server → client | list_sync_get_sync_mode | 无参 → SyncMode |
| server → client | list_sync_get_list_data | 无参 → ListData |
| server → client | list_sync_set_list_data | ListData → 完成；原子落库后才能确认 |
| server → client | list_sync_finished | 无参 → 完成；模块进入可收发状态 |
| 双向 | onListSyncAction | ActionList → 完成；走 list 队列，不能绕开就绪与顺序 |

快照 `ListData` 为 `{defaultList: MusicInfo[], loveList: MusicInfo[], userList: UserListInfoFull[]}`；**不含 tempList**（类型是 Omit）。用户列表为 `{id, name, source?, sourceListId?, locationUpdateTime: number|null, list: MusicInfo[]}`；保留 id 为 `default`、`love`，临时队列不是同步歌单。`locationUpdateTime` 不应擅自解释为歌曲内容版本。

MusicInfo 的 wire 见 Lux 对照 S:`src/types/music.d.ts`：

- 共用 `{id:string, name:string, singer:string, source, interval:string|null, meta}`，meta 基础字段 `{songId:string|number, albumName:string, picUrl?:string|null}`。
- online 共用 `qualitys:[{type,size:string|null}]` 与 `_qualitys` 映射、可选 albumId；kg 另有 meta.hash、各质量 hash；tx 另有 strMediaMid、可选 id/albumMid；mg 另有 copyrightId 与可选 lrcUrl/mrcUrl/trcUrl；kw/wy 用共用结构。
- local 类型的 meta 含 filePath/ext。**类型文件允许 local，不等于另一台设备能访问同一路径**；原生接收时标记不可在本机解析，不能把远端路径当成本地 URI 或删掉后回写。
- Quality/Source 字面值与未见字段的容错需由完整 fixture 锁定；JSON 属性省略/null、数字/字符串身份、字段顺序和中文编码都会影响序列化。原生不可用 Kotlin 默认序列化结果直接假定 MD5 一致。

动作是 RPC 参数中的判别联合 `{action, data}`：

| action | data（与 Lux 对照 S:list.d.ts 一致） |
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

`addMusicLocationType` 在实际处理里为 top/bottom，位置数值边界按 handler/管理代码验收，不编造所有动作统一校验规则。Lux 对照 S:applyListAction 共享 mutation/snapshot/broadcast 路径，并更新来源设备快照，给其他同用户且 list ready 的连接发动作。它没有给客户端提供通用幂等操作 ID，因此超时不等于未执行；重试前应进入重协调，不能宣称恰好一次投递。

## 初次同步、快照与冲突

Lux 对照 S:`src/modules/list/sync/sync.ts` 在存在可用设备快照且未 skipSnapshot 时，先用 MD5 检查是否一致，再决定是否走三方快照合并；否则双方非空时询问模式，单侧空时复制非空侧。因此“server 优先”是首次引导策略，不应绕开现有冲突逻辑强行覆盖。正常增量后 server 为设备维护快照；不能只做 pull 全量就宣称完整互操作。

SyncMode 来自 Lux 对照 S:`src/types/list_sync.d.ts`：merge_local_remote、merge_remote_local、overwrite_local_remote、overwrite_remote_local、overwrite_local_remote_full、overwrite_remote_local_full、cancel。Lux 对照 S:`src/constants.ts` 的 TRANS_MODE **会反转客户端视角方向**；在原生 UI 中 local=本机、remote=server，必须通过双端 fixture 证明方向。

普通 overwrite 的 `overwriteList` 仍保留目标端独有用户列表，full 才是整包采用单侧；不要把两者都标“完全覆盖”。三方合并涉及基准快照删除和顺序规则，不是简单数组并集。mobile 对照 M:`src/core/sync.ts` 只记忆 merge、不记忆 overwrite；原生保留该确认语义。


## 后续能力与验证门槛

首版不启用 dislike、Lux 历史、一起听、设备命令、REST 登录/注册/刷新。未来 Lux 协议证据见 [归档](future/LUX-SYNC-CONTRACT.md)，不是当前实现合同。

Node↔Kotlin 验证包括 AES/OAEP、UTF-8、base64/gzip、0.1.3 envelope、JSON 顺序/MD5；代理子路径、密钥拒绝、双客户端增量/三方合并/方向、响应丢失及重连必须联调。只读 server 源码不代表这些测试已通过。
