# 原生 Android 架构基线：LX 优先

2026-11-14。已由用户确认；实施顺序见 [计划](../plan.md) 和 [路线图](ROADMAP.md)。SDK 33/36、Hilt、本机模式、LX 首版、Lux 后续均已确定。

## 2. 架构与 LX 连接模型

### 模块与依赖

| 模块 | 职责 |
| --- | --- |
| app | 入口、导航、Hilt 装配、组件注册 |
| core:model | 纯 Kotlin 身份、歌曲、队列项、命令、错误 |
| core:api | 纯 Kotlin 业务端口，依赖 model |
| core:designsystem | HTML 设计参数、图标、基础组件 |
| feature | Compose 页面、ViewModel、UiState；按功能分包 |
| data | 唯一业务数据层：Room、资料管理、LX 认证/协议/同步事务 |
| playback | Media3 服务、控制、队列恢复、系统交互 |
| source-runtime | QuickJS、脚本 ABI、能力、信任管理 |

app 是装配点；feature 仅依赖公共端口、模型和设计系统。data、playback、source-runtime 通过公共端口协作，不相互引用实现。DAO、Entity、DTO、codec 和 QuickJS 对象保持内部可见。只有 core:model/core:api 要求纯 Kotlin，设计系统正常使用 Android/Compose。不为每个页面预建 api/impl 模块。

公共端口为 ProfileSession、Library、Catalog、SyncControl、PlaybackControl、SourceResolver。ProfileSession 公开唯一只读状态流及类型化命令；页面使用 UiState/Event，不接收裸 JSON、令牌、密钥或 Player。复杂跨领域规则出现时再提取用例，不给每个 getter 套用例。

### 资料、连接与状态

- 独立本机资料空间；每个 LX 连接独立业务 Room 库和文件根目录，默认不合并。
- 小型目录库保存归属、连接元数据及活动指针；业务资料、设置、历史和同步记录存 Room，不再用 DataStore 双存。
- LX 没有稳定远端 userId；首次连接分配本地 bindingId，关联 serverId 和设备凭据。serverId 不能单独识别账号，URL 也不是账号主键。
- 连接码只用于认证、不落盘；clientId/key 用 Keystore 加密。换码默认新建连接，不静默重用资料或凭据。
- 资料身份、认证、连接和模块就绪分别表达；断网不删除连接和本地资料。
- 切换：准备目标 → 停止旧任务/播放 → 原子提交活动指针 → 绑定目标。异步任务捕获资料标识和 generation，提交前复核；进程恢复以最后提交指针为准，不建跨库多阶段切换日志。
- 页面：本机使用/添加连接 → 检查服务器 → 输入连接码 → 认证 → 初次同步选择。资料管理展示本机与 LX 连接，不设任意五账号上限。
- 断开连接保留授权；退出连接清本机凭据；删除资料另行确认。原 LX 没有完整会话撤销 API，不声称已远端撤销，不保留后台撤销租约。

### 同步与唯一数据来源

实现 hello/id、连接码/密钥认证、WebSocket、message2call 0.1.3、压缩、协商和歌单同步动作。首版只协商实现过的 list v1，dislike 等未实现能力不宣告支持。

本地编辑与同步待办同一 Room 事务提交；远端编辑走相同内部写入路径并标记 Remote，避免回传。初次同步明确合并、覆盖方向和取消。没有通用幂等键时，响应不确定先协调快照，不盲目重放。wire 字段顺序、null/省略、歌曲身份及未知平台元数据需无损保留，不能直接序列化 Room 实体计算 MD5。

### 播放与音源

使用官方 Media3、MediaLibraryService、MediaController；Player 拥有实时播放状态和队列，Room 仅存恢复快照。QueueEntryId 与 SongId 分离，支持同歌重复入队。服务只组装和管理生命周期，解析、恢复、统计、睡眠定时分别组织。临近播放解析地址，过期后有限重试。

QuickJS 沿用 mobile 的绑定库系列，具体版本在 ABI、16 KB 页和脚本验证后锁定；不能假定原 2.4.0 满足全部要求。脚本独立上下文、专属线程、可终止隔离进程，独立看门狗处理死循环。协程超时不等于执行中断。宿主代理网络，不提供凭据、任意文件、数据库和播放器。

导入记录 SHA-256，内容变化重新确认；仅兼容实际脚本证明需要的 ABI。目录搜索与脚本 musicUrl/lyric/pic 能力分别声明，不假设每个脚本均支持搜索。GPL/AGPL 样本只借鉴结构；依赖与素材保留各自许可义务。

## 架构参考的采纳与舍弃

原始调研按字节保存在 [ARCH-REFERENCES](references/ARCH-REFERENCES.md) 与 [LOGIN-UX-REFERENCES](references/LOGIN-UX-REFERENCES.md)。它们是历史输入，不是本仓库规则：其中 minSdk 26/29、杂志风、模块全家桶、单纯 coroutine 超时即可终止 JS 等建议均不能直接执行。本节按真实文件选择做法；具体依赖版本、行数与 star 数不作为决策依据，不复制参考仓库的代码或许可证。

| 核对来源 | 本项目吸收 | 明确不照搬 |
| --- | --- | --- |
| [NiA settings](https://github.com/android/nowinandroid/blob/main/settings.gradle.kts)、[Graph.kt](https://github.com/android/nowinandroid/blob/main/build-logic/convention/src/main/kotlin/com/google/samples/apps/nowinandroid/Graph.kt) | 精简 `includeBuild("build-logic")`、统一 version catalog/toolchain/SDK、依赖仓库集中声明、生成依赖图与白名单检查 | 不整套复制 feature api/impl、UI/designsystem/domain/network 等模块，也不自动提交 CI 基线 |
| [architecture-samples FakeTaskRepository](https://github.com/android/architecture-samples/blob/main/shared-test/src/main/java/com/example/android/architecture/blueprints/todoapp/data/FakeTaskRepository.kt) | 用行为可信的 Fake 端口、测试调度器/时钟验证 UDF | 不增加 repository-on-repository，不用 mock 假造成功来替代协议/事务测试 |
| [Media3 DemoPlaybackService](https://github.com/androidx/media/blob/release/demos/session_service/src/main/java/androidx/media3/demo/session/DemoPlaybackService.kt) | 官方 MediaLibraryService；服务只组装和管理生命周期，Callback、队列恢复、解析器独立；焦点/耳机断开优先使用框架能力 | 不 fork Media3，不另造通知/焦点框架；发行版本在骨架 PR 核对，不照抄调研中的版本号 |
| [Pocket Casts PlaybackServiceToggle](https://github.com/Automattic/pocket-casts-android/blob/main/modules/services/repositories/src/main/java/au/com/shiftyjelly/pocketcasts/repositories/playback/PlaybackServiceToggle.kt)、[Auxio PlaybackStateHolder](https://github.com/OxygenCobalt/Auxio/blob/dev/app/src/main/java/org/oxycblt/auxio/playback/state/PlaybackStateHolder.kt) | UI 只见播放端口，服务内部状态有唯一主人；吸收状态访问隔离 | 不保留新旧播放服务开关，不使用旧 MediaBrowserServiceCompat 或源码 fork 的媒体库 |
| [Gramophone SemanticLyrics](https://github.com/FoedusProgramme/Gramophone/blob/beta/app/src/main/java/org/akanework/gramophone/logic/utils/SemanticLyrics.kt)、[Metrolist LyricsProvider](https://github.com/MetrolistGroup/Metrolist/blob/main/app/src/main/kotlin/com/metrolist/music/lyrics/LyricsProvider.kt) | 共享结构化歌词模型与按能力选源 | 不复制多套新旧歌词 UI；本项目歌词/音源 SPI 不带 Android Context，不做巨型服务/DAO |
| [mihon ExtensionLoader](https://github.com/mihonapp/mihon/blob/main/app/src/main/java/eu/kanade/tachiyomi/extension/util/ExtensionLoader.kt)、[TrustExtension](https://github.com/mihonapp/mihon/blob/main/app/src/main/java/eu/kanade/domain/extension/interactor/TrustExtension.kt) | 加载前先验 API 版本、元信息及信任；脚本记录 SHA-256，内容变化必须重新确认；失败为类型化原因 | 不引入 APK 扩展加载器、ClassLoader 和旧 DI 迁移层；脚本信任不替代资源隔离 |
| [Seal Version](https://github.com/JunkFood02/Seal/blob/main/buildSrc/src/main/kotlin/Version.kt)、[app 构建](https://github.com/JunkFood02/Seal/blob/main/app/build.gradle.kts) | 单一版本模型、ABI 与版本一致性校验、Room schema 导出并纳入未来提交 | 不抄它的版本数值、签名 action 或 flavor；本项目公式见 RELEASE-FLOW |
| [Thunderbird module-organization](https://github.com/thunderbird/thunderbird-android/blob/main/docs/architecture/module-organization.md)、[ui-architecture](https://github.com/thunderbird/thunderbird-android/blob/main/docs/architecture/ui-architecture.md) | 只从公共 API 跨边界、app 组装、State/Event/Effect 与 ADR | 不复制维护分支和模块堆；文档里的边界规则不等于已有工具强制，本项目需独立实现依赖白名单 |
| [Element X KonsistArchitectureTest](https://github.com/element-hq/element-x-android/blob/develop/tests/konsist/src/test/kotlin/io/element/android/tests/konsist/KonsistArchitectureTest.kt) | 将不可变状态、sealed Event、禁止 MutableStateFlow 暴露等规则做架构测试（本项目扩展规则） | 不复制整个模块图、Presenter/Molecule/Appyx 技术栈；Konsist 不代替编译依赖检查 |
| [Zipline README](https://github.com/cashapp/zipline/blob/trunk/README.md) | 宿主/JS 强类型桥接、序列化边界，纳入 QuickJS 绑定对照 | 不假设它可直接运行现有 LX 普通 JS ABI，不将其当成已证明有 CPU/内存限额的沙箱 |


Hilt 为唯一 DI。build-logic 仅集中 application/library/Compose/JVM、Room schema 和检查配置；精确工具版本在可构建的骨架 PR 锁定。旧参考的全套模块、低 SDK 和杂志风不覆盖当前 HTML。
