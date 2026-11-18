# Lux Music Android：先实现 LX 同步，后续接入 Lux Server

## 1. 目标与已定选择

首版围绕本机模式与原版 LX Music 同步服务完成音乐核心闭环。Lux Server 的登录、注册、刷新和扩展能力后续实施。用户已确认方案并要求推进；当前 PR #1 交付规范收敛，工程与功能按后续小 PR 推进。

- Kotlin、Compose、Hilt；minSdk 33，compileSdk/targetSdk 36。
- 首先兼容固定版本 LX v4 协议，服务端保持原样。
- 首个测试版完成搜索、音源、歌单、队列、后台播放、歌词、LX 双向同步和恢复。
- 现有 HTML 是视觉/交互基准，仅补充本机模式、LX 连接和资料切换设计。
- dev 集成、main 稳定；通过短分支 PR 开发。
- 包名 cn.lux.music.android；开发/稳定版共用包名、签名和数据，与 mobile 并存。
- 首版不实现 Lux REST 认证，不展示 Lux 登录注册入口，不预建未来认证空模块。

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

## 3. 开发、PR 与发布

### 分支与合并

```text
短任务分支 ──功能 PR / squash──→ dev
dev ──转正 PR / merge commit──→ main
main ──回合并 PR / merge commit──→ dev
```

转正窗口冻结候选版本及 dev 合并；稳定发布完成后回合并 main，再更新开发目标。hotfix/* 从 main 开始，经 PR 回 main，随后回合并 dev。回合并保留 merge commit，不能套用普通功能 squash。

main/dev 禁止直接推送和 force push，已发布 tag 不移动。main 不启用阻止 merge commit 的线性历史规则。单维护者由 June 审阅合入，结合必需检查和解决讨论，不设置阻塞本人 PR 的外部批准要求；有独立审阅者后增加。

### 自动构建与发行

| 触发 | 行为 |
| --- | --- |
| PR | 格式、Lint、架构边界、单测、协议、debug APK；相关改动加设备和截图测试 |
| dev 合入 | 验证实际合入提交；发行启用后自动签名并发布 dev.N |
| 转正合入 main | 验证、构建；发布环境审批后完成稳定发行 |
| 定时 | 扩展系统、原生库、长时间播放/同步恢复测试 |

PR 不接触签名密钥。Actions 固定完整 SHA，默认 contents:read，发布写权限按 job 授予。所有 PR 均有稳定名称的聚合必需检查；文档改动明确跳过无关构建，避免 pending。CI 不自动接受截图或依赖基线，不通过 pull_request_target 执行未信任代码并接触 secrets。

首个目标版本 0.1.0；tag 为 v0.1.0-dev.N / v0.1.0。仓库保存一份目标版本，开发序号根据已占用 tag 分配，失败占号保留；同一 tag 重试不重新编号。

```text
versionCode = ((major × 100 + minor) × 100 + patch) × 1000 + slot
```

开发 slot=1…998，稳定 slot=999；major=0…209，minor/patch=0…99，检查 Android 上限。序号耗尽停止发布，不复用或溢出。例如 0.1.0-dev.1=100001，0.1.0=100999，0.1.1-dev.1=101001。

一个 universal APK 含 arm64-v8a、armeabi-v7a；x86_64 用于模拟器验证。原生库 ABI、16 KB 页、实际 manifest 和签名为发行门槛，不能静默少包。debug 使用 .debug 身份和开发签名，dev/stable 共用正式证书。

资产附版本、源码 SHA、工具链、证书指纹和 SHA-256。全部验证后才公开 Release；dev 标 prerelease/latest=false。不同内容不覆盖同名资产，源码修改用新 tag；缺签名失败。公开完成后才更新 feed，上传/部署失败分别恢复，不重新占号。

开发/稳定共用数据，只做前向迁移；退出开发通道等待更高 versionCode 的稳定版，不通过降级 APK 回退。稳定用户默认不接收开发通道。

开发版允许合并过时待执行请求，仅发布最新合格提交；dev/stable 用独立并发组，不能互相取消。feed 部署单独串行，取得执行权后从已公开 Release 重建两个通道，避免较旧任务覆盖新频道。发布前再次检查目标未落后于已发布版本。

GitHub Pages 使用部署 artifact，不回写 main/dev、不建 feed 源码分支。feed schemaVersion、stable/dev、版本、sourceSha、发布时间、说明和资产哈希/大小/ABI/versionCode 使用显式 DTO。安装前核对包名、较高版本、校验和及证书，再交系统安装器。

编排直接调用发布步骤，不依赖 GITHUB_TOKEN 创建 tag 后触发另一工作流。签名材料、发布环境审批、Pages 和保护由 June 配置；当前文档 PR 不执行远程设置、tag 或发行。

## 4. PR 顺序与验收

| 阶段 | 交付与完成标准 | 当前状态 |
| --- | --- | --- |
| 文档收敛 | 同一 PR #1 整合研究、架构、LX 协议、资料模型、路线图和发行方案，保留 HTML | 本轮交付 |
| 工程基础 | 模块、Hilt、固定工具链、公共构建配置、debug APK、必需检查；纯 Kotlin/依赖白名单可执行 | 未实施 |
| LX 协议验证 | 实际 Node 源码生成 codec/认证/同步 fixtures；双端方向和恢复验证，不修改 server | 未实施 |
| 音源验证 | QuickJS ABI、隔离、取消、死循环、16 KB 页和所选 ABI；失败停止功能集成 | 未实施 |
| 数据与连接 | Room、本机模式、LX 连接、加密凭据、切换、初次同步；先补连接设计基准 | 未实施 |
| 音乐核心闭环 | 搜索/音源 → 歌单 → 队列 → 后台播放/歌词 → 双向同步 | 未实施 |
| 首次发行 | 签名、前向升级、失败恢复演练后启用自动 dev；稳定保留审批 | 未实施 |
| 后续能力 | HTML/mobile 功能对照逐项完成，再启动 Lux 专项 | 未实施 |

每一阶段继续按可验收行为拆小 PR；文档 PR 不混入 Kotlin、Gradle、资源或 CI。尚未建立 dev 时，先将文档合入 main，再从 main 建 dev，注册 CI 检查后启用保护。不得在检查尚未存在时先设为必需造成死锁。合并 PR 与正式发布仍由维护者执行。

关键验收：

- 架构：禁止 UI 引用实现、模型引入 Android、公开裸 JSON/可变状态、循环依赖。类上限 400 行、函数 60 行、服务 300 行；例外局部说明并评审。用负例证明门禁有效。
- LX：真实加密、压缩、RPC、字段顺序 fixtures；中文/emoji/null/省略、1024 字符边界；双端增删移排序、冲突取消、断线及恢复。
- 资料：快速切换、晚响应、开库失败、切换中杀进程、Keystore 失效、离线退出；事务失败无孤立待办，Remote 不生成回传。
- 播放/脚本：锁屏、通知、蓝牙、焦点、队列恢复、URL 过期；死循环、资源耗尽、取消和进程死亡不拖垮主应用。
- UI：固定数据、视口、字体和状态对照 HTML；连接页按补充基准，不修改原型掩盖差异。
- 发行：API 33/36、所选 ABI、缺签名、版本冲突、上传中断、重试、feed 恢复和 dev→stable 前向升级。

上述是后续要求，本轮没有 Android 工程、SDK/设备联调或发行测试。静态研究和文档检查不能写成运行兼容已通过。

## 5. Lux Server 后续接入与文档约定

LX 核心闭环和恢复测试通过后，再核对实际 Lux 部署，扩展资料身份与认证适配，接入密码登录、注册、续期、撤销及服务器能力。复用资料切换、Room、播放与同步路径，不建立第二套全局状态。

连接码兑换 Lux 会话保留为独立后端演进提案，不作首版前置。Lux 旧账号/协议资料归档在 docs/future/，其中历史未决项不覆盖本计划。对外端点不新增、不修改；服务端提案未来另案处理。

调研证据保存在 research/；正式规则集中在 docs/ 并同步 AGENTS.md。旧研究的单 main、低 SDK、首版同时 LX/Lux 或排除 LX 等建议由本计划覆盖。首版不读 RN 私有数据、不自动合并资料、不引入厂商 flavor、不 fork Media3。
