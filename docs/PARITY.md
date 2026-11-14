# mobile 功能对照

用途：列用户能做的事、源码入口与原生对应物，不按目录或实现原样搬迁。证据基于 mobile `3ab131e141ccbdc807d200a99eb216a17017c8c8`，[固定源码](https://github.com/JuneDrinleng/lux-music-mobile/tree/3ab131e141ccbdc807d200a99eb216a17017c8c8)。下列路径均相对 mobile；已读源码证明存在实现入口，不等同于对外部源或每款设备作在线验收。难度均为**粗估**，不是排期。

| 能力/模块 | mobile 真实入口与可观察职责 | 原生侧对应物/范围 | 难度粗估 |
| --- | --- | --- | --- |
| 播放、后台、音量/倍速、焦点 | `src/plugins/player/index.ts` 的 initial/setupPlayer；`src/core/player/player.ts` 控制播放 | Media3 服务 + PlaybackControl；MVP 必需；丢弃包装/迁移见审计 A10 | 粗估高 |
| 队列、上一首/下一首、播放模式 | `src/screens/PlayDetail/Vertical/Lyric.tsx` 消费播放状态与 playNext/playPrev/togglePlayMethod | 服务独占队列，UI 发送命令；强类型队列项身份 | 粗估中 |
| 多平台搜索、榜单、在线歌单、热搜/建议 | `src/utils/musicSdk/index.js` 聚合 kw/kg/tx/wy/mg；`src/utils/musicSdk/kw/index.js` 实际导出 musicSearch/leaderboard/songList/hotSearch/tipSearch；`src/utils/musicSdk/tx/index.js` 聚合目录能力 | data 的 Catalog 与来源适配；按来源/能力逐项验证，不承诺一次全开 | 粗估高 |
| 自定义音源、解析播放 URL | `src/core/init/userApi/index.ts` 处理 musicUrl/lyric/pic 与 HTTP/cancelRequest；`src/utils/nativeModules/userApi.ts` 发送事件；Java `android/app/src/main/java/cn/lux/music/mobile/userApi/QuickJS.java` 执行脚本 | QuickJS + SourceResolver；仅保留真实 ABI 需求；隔离、取消和限额先验证，见 A15 | 粗估高 |
| 歌单、收藏、有序歌曲 | `src/utils/listManage.ts` 的 createUserList/setMusicList 与可变列表；`src/store/list/action.ts` 发布列表状态 | Room + Library 命令；数据边界重做，见 A8/A9 | 粗估中 |
| 文本/CSV 导入解析 | `src/utils/playlistImport/parser.ts` 的 parseText/csvRows | 后续显式文件/粘贴导入功能，校验与预览；不等同于 RN 本地存储迁移 | 粗估中 |
| 本地歌曲、缓存浏览与目录导入 | `src/components/playlist/LocalSongsDetail.tsx` 调用 collectDeviceSongs/importDeviceAudioLibrary/importFolderIntoLibrary，合并本地与缓存行 | 后续 MediaStore/SAF 本地库；音频缓存与用户文件分开，不以私有路径作为跨设备身份 | 粗估高 |
| 下载队列、质量、仅 Wi-Fi | `src/components/download/hooks.ts` 订阅 downloadQueue、维护偏好；Java `android/app/src/main/java/cn/lux/music/mobile/download/DownloadWorker.java` 执行前台任务 | 后续后台下载任务与唯一 Room 任务表；不照抄 UI 偏好双写，旧 API 见 A5 | 粗估高 |
| 行歌词、逐字、翻译/罗马音 | `src/plugins/lyric.ts` 使用 lrc-file-parser 并合并 words/translation/roma；`src/screens/PlayDetail/Vertical/Lyric.tsx`、`Horizontal/Lyric.tsx` 呈现 | 共享不可变歌词时间轴 + 播放时钟；Compose 响应式布局；去重见 A11 | 粗估中至高 |
| 桌面悬浮歌词 | `android/app/src/main/java/cn/lux/music/mobile/lyric/LyricView.java` 配置悬浮窗类型/锁定透明度 | 后续独立评审 overlay 权限、服务与触摸限制，见 A3；非 MVP | 粗估高 |
| 评论 | `src/utils/musicSdk/tx/index.js` 引入 comment；`src/screens/index.ts` 注册 Comment 页面 | Catalog 目录扩展 + feature 页面；外部可用性单独验收 | 粗估中 |
| 登录、资料、设备会话、空间管理 | `src/screens/Account/AccountPage.tsx` 接入资料、sessions/password/spaces；`src/utils/luxAuthApi.ts` 刷新与状态校验 | Lux 后续专项；首版仅本机/LX ProfileSession 与连接管理，不展示 Lux 入口 | 粗估高 |
| server 歌单/不喜欢同步 | `src/plugins/sync/client/modules/list/handler.ts`、`src/plugins/sync/client/sync/handler.ts` 实现数据交换/协商；`src/core/sync.ts` 选择冲突模式 | 唯一数据层内部 sync 包；歌单属 MVP，不喜欢规则后置；见 SYNC-CONTRACT 与 A16 | 粗估高 |
| 听歌历史与跨设备同步 | `src/plugins/sync/playHistorySync.ts` 在 Lux 模式与协商就绪后 pull/push，隔离服务器/用户游标 | 先本地 PlayRecord；跨设备历史为 Lux 后续，不虚报 LX 能力 | 粗估高 |
| 月报、年度报告与分享 | `src/components/stats/MonthlyReportPage.tsx` 计算月份报告与分享；`src/utils/shareCard/reports.ts` 的 annualShareReport 按年份聚合/去重 | 基于同一历史表的纯函数统计 + Compose 报告/分享图；时区与边界测试 | 粗估中至高 |
| 推荐、相似歌曲、私人电台 | `src/utils/recommend/index.ts` 组装历史、不喜欢、local/platformSources，导出 daily/similar/radio | MVP 后单独设计推荐策略；输入同一事实源，不复制旧缓存与转换链 | 粗估高 |
| 一起听、房间角色/连接状态 | `src/store/together/action.ts` 设置房间快照、host/member、connection；`src/plugins/sync/client/client.ts` 处理 together_sync_finished | 后续会话级房间状态与 PlaybackControl 协调；独立 ADR，非歌单同步的前置 | 粗估高 |
| 均衡器、响度、无缝播放 | `src/screens/AudioEffects/index.tsx` 按 native capability 提供预览/提交；`src/plugins/player/index.ts` 恢复这些设置 | 后续 Media3 音效链，能力检测与回滚；不展示不能生效的开关 | 粗估高 |
| 白噪音 | `src/core/player/whiteNoise.ts` 串行修改 noise，明确不在进程启动自动播放 | 后续独立音轨/混音所有权设计，不能隐式多建播放器 | 粗估高 |
| 主题、布局、设置 | `src/config/defaultSetting.ts` 定义 theme/light/dark、字体/布局与播放偏好；`src/theme/themes/index.ts` 消费自动主题 | 原生精简设置 schema 与 Compose theme；每项有消费者再加入，A7/A13/A14 | 粗估中 |
| 发布渠道 | `.github/workflows/ci.yml`、`.github/workflows/release.yml` 对 dev/master 分流 | 已确定 dev/main；开发自动预发布、main 审批稳定版，按 RELEASE-FLOW 实施 | 粗估中 |
| 失败占位源、旧迁移、旧文件选择器 | `src/utils/musicSdk/xm.js`、`src/config/migrate.ts`、`src/config/migrateSetting.ts`、`src/plugins/storage.ts`、`src/components/common/ChoosePath/index.tsx` | 不作为功能 parity 目标；按 [审计](LEGACY-AUDIT.md) A6–A8/A12/A14 丢弃或重设计 | 粗估低（不引入），如有导入例外另估 |

本表是已读入口的能力盘点，未来每个功能 PR 需以用户操作和失败恢复验收；不是支持所有 mobile 设置的承诺，也不是列出相同目录就宣称完成 parity。MVP 与先后顺序以 [ROADMAP](ROADMAP.md) 为准。
