# 01 · 现有 mobile：功能基线与需要避免的负担

> 决策更新（2026-11-14）：本文保留原始研究结论与建议，不是当前实施规范。已确定先 LX、后 Lux；SDK 33/36、Hilt、dev/main、同包名开发/稳定通道、独立本机资料。最新要求见 [plan.md](../plan.md)，与历史建议冲突时以最新计划为准。

证据：[mobile master/dev 固定提交与路径](sources.md#mobile)、[服务端两分支](sources.md#lux-server)。这是一轮静态源码审阅，不是性能测量或缺陷复现。

## 功能不等于界面预览

`ui/index.html` 定义新 Android 的视觉/交互基准；mobile 则提供需要迁移的业务行为。HTML 中离线数据、模拟登录、提示式下载无法代替真实音乐客户端功能。

| 功能域 | 已看到的 mobile 代码 | 原生重建要保留 / 明确的语义 |
| --- | --- | --- |
| 推荐与榜单 | `src/utils/recommend/`、`Home/Vertical/Tabs/HomeTab.tsx`、`LeaderboardDetail` | 推荐生成、可用性、失败/超时/缓存与刷新，不只三张静态卡片 |
| 搜索和源 | `src/utils/musicSdk/{kw,kg,tx,wy,mg}`、`SearchPage.tsx` | 搜索、歌曲/歌单、封面、歌词、评论、榜单等能力按源声明；线协议 `tx` 与 UI 的 QQ 标签不能混淆 |
| 自定义源 | `src/core/init/userApi/`、`src/utils/nativeModules/userApi.ts`、原生 `userApi/QuickJS.java` | 脚本生命周期、请求/取消、musicUrl/音质、超时；原生 UI 不意味着完全禁止隔离的 JS 插件运行时 |
| 播放器 | `src/plugins/player/`、`PlayDetail/Vertical/`、`Horizontal/` | 队列、随机/循环/顺序、来源、进度、音效、后台、通知、耳机与系统控制；不复制两套历史布局 |
| 歌词和评论 | `PlayDetail/Vertical/Lyric.tsx`、`Comment/`、原生 `lyric/` | 原文/译文/音译/逐字数据、定位、高亮、字体、排序与分页；HTML 和真实歌词数据都要验证 |
| 资料库 | `src/core/list*`、`listManage`、playlist 组件 | 收藏、歌单 CRUD/排序/批量操作、本地歌曲、搜索历史、临时队列；临时列表不自动同步 |
| 下载 | `src/core/download/{model,index}.ts`、原生 `download/{DownloadStore,DownloadScheduler,DownloadWorker}.java` | 任务所有权、音质、Wi-Fi、空间预检、暂停/恢复/失败/去重、过期 URL、文件落盘和离线定位 |
| 聆听统计 | `src/utils/playHistory/`、`tracker.ts`、`wire.ts` | 实际听歌时长/跳过/完成、时间范围、聚合；不能把 seek 或重复回调计为额外聆听 |
| 一起听 | `Home/Vertical/ListenTogether/`、`src/core/together/`、同步 together handlers | 房间、状态/权限、重连、退出与个人队列恢复；按协商能力降级 |
| 账号与同步 | Login、SyncLogin、Account、appSession、sync client | 本地/LX/Lux 区分、账号隔离、设备密钥、续期、冲突选择、离线和切换恢复 |
| 设置与备份 | settings、缓存组件、profile 和历史 backup | 全局设置与账号设置区分；备份版本/导入预览/取消/冲突；不沿用无限格式迁移 |
| 扩展入口 | AiTokenStatus、AdminConsole、WhiteNoisePage、`aiAssistantEntry.ts` | 代码存在不等于当前可见或首版必须开放；记录为范围待确认，不能默默删除或宣称已迁移 |

以上是功能族清单，尚非逐方法全量行为规格；后续实际开发前需要把本次固定版本的路由、设置键、源 action、导入格式逐项转成验收表。

## 具体负担，不把所有兼容性都当成坏设计

| 已核实的结构 | 带来的成本（分析） | 新项目处理建议 |
| --- | --- | --- |
| RN 0.73.11、多个 Git 固定 fork、`postinstall` patch、`--legacy-peer-deps`，standard/vivo 变体 | JS/native 构建耦合；依赖升级需同时理解补丁与桥接 | 不移植依赖链。只有新的实际设备缺陷才引入受控 workaround，并注明移除条件 |
| `normalizeServerUrl` 为 RN URL 实现不完整使用正则解析 | 平台限制渗入业务层 | 用原生 URI/OkHttp HttpUrl；保留子路径、端口、IPv6和大小写语义，别复制 workaround |
| `credentials.ts` 有 scoped/legacy key 选择，repository 将明文索引迁移为安全引用 | 旧存储历史增加身份与写入路径 | 新 schema 直接安全引用；若需要导入 RN 数据，做单次 import adapter，不保留双写 |
| `AuthFlow` 同时处理表单、邀请码、连接、settings、onboarding、导航、旧/新 coordinator 开关 | 流程难以单独验证，多个入口的完成条件可能不同 | 表单只发意图；一个状态所有者定义完成/取消；UI 不修改 `sync.enable` 或发布全局身份 |
| `appSession` 已有 journal、freeze、pause、prepare、commit；`sessionFence` 与 socket session 分开 | 说明并发切换是真问题；再删掉所有 fence 会丢隔离保障 | 保留单一切换提交点、generation、任务所有权；用协程 scope/事务表达，不复制全套兼容 gate |
| `loginSession` 把 LX 映射到 local space，而 Lux 用 `lux:server:user` | LX 连接码身份与本地资料共享语义需要理解 | 建议明确 local/LX/Lux 资料边界；改变默认共享行为需用户确认，见 04 |
| 下载同时有 JS 视图模型及 Java store/scheduler/worker | 两份任务状态需要 reconcile、revision 和 bridge snapshot | 原生以一套持久任务状态为准；UI 只观察，不另造队列 |
| 同时存在横竖/旧新页面与 legacy UI/数据入口 | 修改需要跨多条路径维护一致性 | 产品 UI 只保留新 `ui/` 契约；其他旧布局只提供行为参考 |
| CI 对全量 lint/typecheck `continue-on-error`，原生 build 按路径跳过 | 历史欠账被常态化；看似绿灯不代表全部通过 | 新项目从零要求所有新代码检查阻断，必要的文档跳过由聚合检查明确处理 |

这些判断来自文件阅读，不能据此断言所有潜在竞态已经在当前 mobile 发生；尤其已有 coordinator、凭据隔离、续期单飞是值得保留的保护思想。

## 数据与标识必须先定

- 外部歌曲 ID、source、songId 的字符串/数字类型必须在 wire adapter 里保真；内部键可使用 `(source, providerSongId)`，但不能重写服务端期望的 `id`。
- 本地文件 URI、下载文件、同步元信息不同：同步列表里出现路径不表示 Android 能读取另一台设备文件。
- source 脚本认证、Lux 账号 token、LX 设备 key 属于不同凭据域，不能共用“当前账号 token”。
- 队列、推荐、统计、下载、收藏等写入都需要资料 owner；网络响应不能在切账号后写入“当前全局数据库”。
- 旧 RN 数据是否导入尚未确定；默认新安装独立数据，禁止借“兼容”复制所有旧 key/schema。可选一次性导入的字段/版本/丢失项需另立合同。

## mobile 与 server 版本不能混为一个基线

本次 mobile master 为 `7021133`，dev 为 `4f75d71`；Lux server master 为 `7834abd`，dev 为 `794fcc4`。观察到 mobile auth client 声明 refresh、邀请码预检等 dev 合同，而 Lux master 仍是旧合同。新 Android 不应“逐文件移植 mobile 后再等 server 修复”；先按 02 识别可用能力，保持 server 原样。

## 许可与复用

mobile 的 package 声明 Apache-2.0，但 `LICENSE-NOTICE.md` 和文件头还声明 Lux 原创部分，不能只读 package 就认定整仓可任意拷贝。本轮只做行为与架构研究，没有复制第三方业务代码进 Android。将来直接复用文件时逐文件保留许可证/署名，不能把借鉴架构与复制代码混同。
