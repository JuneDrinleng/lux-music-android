# 来源、固定版本与证据范围

> 决策更新（2026-11-14）：本文保留原始研究结论与建议，不是当前实施规范。已确定先 LX、后 Lux；SDK 33/36、Hilt、dev/main、同包名开发/稳定通道、独立本机资料。最新要求见 [plan.md](../plan.md)，与历史建议冲突时以最新计划为准。

调研日期：2026-11-14（UTC）。本文件和 sources.json 保存可重新访问的固定提交链接；临时克隆不纳入产品仓库。这里只列证据索引，不复制第三方项目源码。

## 方法

直接使用 GitHub 搜索与仓库元信息筛选，再克隆源码到临时研究目录，读取具体文件。先比较 mobile/server 分支，发现认证合同不同后分别固定快照。依 package-lock 核对 message2call 实际为 0.1.3，另检出对应 tag，而非沿用库当前 2.x 主干。商业参考只采用官方页面、官方帮助；没有向任何人发送消息或使用真实账号。

“优秀”在这里按职责分离、生命周期、实际业务覆盖、可审阅边界和构建实例判断；不是排行榜。当前维护状态和默认分支会变化，本文结论仅对应所列提交。Tempo 快照停在 2025 年，不作为现代依赖版本推荐。

这些项目都是研究样本，不能据此认为全部架构、登录、许可证或 workflow 都适合复制。Finamp 是 Flutter UX 样本；Jellyfin 主客户端是 Web wrapper；Metrolist 的 WebView 登录是特定服务适配；原生目标不能由它们替代。

## 快照总表

| ID | 仓库 / 本次 ref | 固定提交 | 提交日期 | 研究范围 |
| --- | --- | --- | --- | --- |
| mobile | [JuneDrinleng/lux-music-mobile](https://github.com/JuneDrinleng/lux-music-mobile) / `master` | [`702113391333`](https://github.com/JuneDrinleng/lux-music-mobile/tree/702113391333c463049c0f7b449ff2c02b3758ba) | 2026-11-12 | RN 业务、账号与发布基线 |
| mobile-dev | [JuneDrinleng/lux-music-mobile](https://github.com/JuneDrinleng/lux-music-mobile) / `dev` | [`4f75d71b3184`](https://github.com/JuneDrinleng/lux-music-mobile/tree/4f75d71b318426f86364ad22bf949955a5d7ca41) | 2026-11-12 | 开发分支对照与源运行时 |
| lux-server | [JuneDrinleng/lux-music-server](https://github.com/JuneDrinleng/lux-music-server) / `master` | [`7834abd66bb4`](https://github.com/JuneDrinleng/lux-music-server/tree/7834abd66bb4363e75cbd72f59d8ba1988eae99d) | 2026-10-08 | 旧 Lux 账号与核心协议 |
| lux-server-dev | [JuneDrinleng/lux-music-server](https://github.com/JuneDrinleng/lux-music-server) / `dev` | [`794fcc4cfee0`](https://github.com/JuneDrinleng/lux-music-server/tree/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9) | 2026-10-18 | 新 Lux 会话、发现、注册与扩展 |
| lx-server | [lyswhut/lx-music-sync-server](https://github.com/lyswhut/lx-music-sync-server) / `master` | [`d47aca4284a7`](https://github.com/lyswhut/lx-music-sync-server/tree/d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36) | 2025-01-18 | 原版 LX v4 协议 |
| message2call | [lyswhut/message2call](https://github.com/lyswhut/message2call) / `v0.1.3` | [`2a5c8b966dc6`](https://github.com/lyswhut/message2call/tree/2a5c8b966dc689271c2d5c47ad02587cb896ede3) | 2023-09-14 | 实际 lockfile 依赖版本的 RPC 格式 |
| nia | [android/nowinandroid](https://github.com/android/nowinandroid) / `main` | [`2982e63cb691`](https://github.com/android/nowinandroid/tree/2982e63cb691ad49cd009aab43dc206fee6699c2) | 2026-10-20 | 官方分层、模块、离线数据、CI |
| auxio | [OxygenCobalt/Auxio](https://github.com/OxygenCobalt/Auxio) / `dev` | [`f243bceab0b5`](https://github.com/OxygenCobalt/Auxio/tree/f243bceab0b55fdae7462723a3d14e7eec21a432) | 2026-11-01 | 原生播放状态、持久化和系统/UI 边界 |
| metrolist | [MetrolistGroup/Metrolist](https://github.com/MetrolistGroup/Metrolist) / `main` | [`e6fea3b36c68`](https://github.com/MetrolistGroup/Metrolist/tree/e6fea3b36c68841bad21daa6252e1d69ecd3afbe) | 2026-11-03 | Compose/Media3 实际应用及账号登录反例 |
| seal | [JunkFood02/Seal](https://github.com/JunkFood02/Seal) / `main` | [`7677f61a20fd`](https://github.com/JunkFood02/Seal/tree/7677f61a20fda4210e84215fef9e9b51d25daa47) | 2026-09-25 | 下载任务与 CI 划分 |
| tempo | [CappielloAntonio/tempo](https://github.com/CappielloAntonio/tempo) / `main` | [`9cf62c8c0c8d`](https://github.com/CappielloAntonio/tempo/tree/9cf62c8c0c8d90e26be971570c797836dcf0d575) | 2025-01-31 | 原生自托管音乐登录和服务器配置 |
| jellyfin | [jellyfin/jellyfin-android](https://github.com/jellyfin/jellyfin-android) / `master` | [`44b94e659e43`](https://github.com/jellyfin/jellyfin-android/tree/44b94e659e432cf41a90a2079b801a2c71128c23) | 2026-11-12 | 原生连接外壳（主客户端为 Web wrapper） |
| finamp | [finamp-app/finamp](https://github.com/finamp-app/finamp) / `redesign` | [`5cd29afec3e7`](https://github.com/finamp-app/finamp/tree/5cd29afec3e72ad90cdeab0df9ce16c857867173) | 2026-11-13 | Flutter，仅账号 UX 对照，不作原生架构依据 |

## mobile

### mobile · master

许可观察：Apache-2.0 派生部分 + Lux 原创声明，逐文件核查。阅读范围：静态源码，不代表构建或运行验证。

- [package.json](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/package.json)
- [LICENSE-NOTICE.md](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/LICENSE-NOTICE.md)
- [src/utils/loginSession.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/utils/loginSession.ts)
- [src/utils/accountSpaceSwitch.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/utils/accountSpaceSwitch.ts)
- [src/utils/loginCredentialRepository.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/utils/loginCredentialRepository.ts)
- [src/utils/luxAuthApi.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/utils/luxAuthApi.ts)
- [src/utils/serverCheck.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/utils/serverCheck.ts)
- [src/utils/dataSpace/index.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/utils/dataSpace/index.ts)
- [src/core/appSession/coordinator.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/core/appSession/coordinator.ts)
- [src/core/appSession/index.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/core/appSession/index.ts)
- [src/utils/sessionFence.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/utils/sessionFence.ts)
- [src/screens/Login/AuthFlow.tsx](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/screens/Login/AuthFlow.tsx)
- [src/screens/Login/ServerStep.tsx](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/screens/Login/ServerStep.tsx)
- [src/screens/Account/AccountSpaces.tsx](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/screens/Account/AccountSpaces.tsx)
- [src/plugins/sync/client/auth.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/plugins/sync/client/auth.ts)
- [src/plugins/sync/client/client.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/plugins/sync/client/client.ts)
- [src/plugins/sync/client/credentials.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/plugins/sync/client/credentials.ts)
- [src/plugins/sync/client/session.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/plugins/sync/client/session.ts)
- [src/plugins/sync/client/sync/handler.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/plugins/sync/client/sync/handler.ts)
- [src/plugins/sync/client/modules/list/handler.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/plugins/sync/client/modules/list/handler.ts)
- [src/plugins/sync/listEvent.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/plugins/sync/listEvent.ts)
- [src/utils/playHistory/wire.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/utils/playHistory/wire.ts)
- [src/core/download/model.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/core/download/model.ts)
- [src/core/init/userApi/index.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/core/init/userApi/index.ts)
- [src/utils/nativeModules/userApi.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/utils/nativeModules/userApi.ts)
- [android/app/src/main/java/cn/lux/music/mobile/crypto/AES.java](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/android/app/src/main/java/cn/lux/music/mobile/crypto/AES.java)
- [android/app/src/main/java/cn/lux/music/mobile/crypto/RSA.java](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/android/app/src/main/java/cn/lux/music/mobile/crypto/RSA.java)
- [.github/workflows/ci.yml](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/.github/workflows/ci.yml)
- [.github/workflows/release.yml](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/.github/workflows/release.yml)
- [src/utils/nativeModules/crypto.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/utils/nativeModules/crypto.ts)
- [src/plugins/sync/utils.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/plugins/sync/utils.ts)
- [src/plugins/sync/client/utils.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/src/plugins/sync/client/utils.ts)

### mobile-dev · dev

许可观察：同仓库逐文件声明。阅读范围：静态源码，不代表构建或运行验证。

- [README.md](https://github.com/JuneDrinleng/lux-music-mobile/blob/4f75d71b318426f86364ad22bf949955a5d7ca41/README.md)
- [src/core/init/userApi/request.js](https://github.com/JuneDrinleng/lux-music-mobile/blob/4f75d71b318426f86364ad22bf949955a5d7ca41/src/core/init/userApi/request.js)
- [src/utils/nativeModules/userApi.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/4f75d71b318426f86364ad22bf949955a5d7ca41/src/utils/nativeModules/userApi.ts)
- [src/utils/recommend/service.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/4f75d71b318426f86364ad22bf949955a5d7ca41/src/utils/recommend/service.ts)
- [src/utils/playHistory/tracker.ts](https://github.com/JuneDrinleng/lux-music-mobile/blob/4f75d71b318426f86364ad22bf949955a5d7ca41/src/utils/playHistory/tracker.ts)

## lux-server

### lux-server · master

许可观察：Apache-2.0（仓库 LICENSE）。阅读范围：静态源码，不代表构建或运行验证。

- [src/server/api/auth.ts](https://github.com/JuneDrinleng/lux-music-server/blob/7834abd66bb4363e75cbd72f59d8ba1988eae99d/src/server/api/auth.ts)
- [src/server/api/authGuard.ts](https://github.com/JuneDrinleng/lux-music-server/blob/7834abd66bb4363e75cbd72f59d8ba1988eae99d/src/server/api/authGuard.ts)
- [src/server/api/sync.ts](https://github.com/JuneDrinleng/lux-music-server/blob/7834abd66bb4363e75cbd72f59d8ba1988eae99d/src/server/api/sync.ts)
- [src/server/api/me.ts](https://github.com/JuneDrinleng/lux-music-server/blob/7834abd66bb4363e75cbd72f59d8ba1988eae99d/src/server/api/me.ts)
- [src/server/app.ts](https://github.com/JuneDrinleng/lux-music-server/blob/7834abd66bb4363e75cbd72f59d8ba1988eae99d/src/server/app.ts)
- [src/account/token.ts](https://github.com/JuneDrinleng/lux-music-server/blob/7834abd66bb4363e75cbd72f59d8ba1988eae99d/src/account/token.ts)
- [src/server/server.ts](https://github.com/JuneDrinleng/lux-music-server/blob/7834abd66bb4363e75cbd72f59d8ba1988eae99d/src/server/server.ts)
- [src/modules/index.ts](https://github.com/JuneDrinleng/lux-music-server/blob/7834abd66bb4363e75cbd72f59d8ba1988eae99d/src/modules/index.ts)
- [docs/sync-server-mobile-flow.md](https://github.com/JuneDrinleng/lux-music-server/blob/7834abd66bb4363e75cbd72f59d8ba1988eae99d/docs/sync-server-mobile-flow.md)

### lux-server-dev · dev

许可观察：Apache-2.0（仓库 LICENSE）。阅读范围：静态源码，不代表构建或运行验证。

- [src/server/api/auth.ts](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/server/api/auth.ts)
- [src/server/api/sync.ts](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/server/api/sync.ts)
- [src/server/api/me.ts](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/server/api/me.ts)
- [src/server/app.ts](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/server/app.ts)
- [src/account/token.ts](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/account/token.ts)
- [src/account/sessions.ts](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/account/sessions.ts)
- [src/modules/index.ts](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/modules/index.ts)
- [src/modules/playHistory/protocol.ts](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/modules/playHistory/protocol.ts)
- [src/modules/together/protocol.ts](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/modules/together/protocol.ts)
- [src/types/list_sync.d.ts](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/src/types/list_sync.d.ts)
- [package-lock.json](https://github.com/JuneDrinleng/lux-music-server/blob/794fcc4cfee03cdc12c56dad0272f116e3f0d8a9/package-lock.json)

## lx-server

### lx-server · master

许可观察：Apache-2.0。阅读范围：静态源码，不代表构建或运行验证。

- [package.json](https://github.com/lyswhut/lx-music-sync-server/blob/d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36/package.json)
- [package-lock.json](https://github.com/lyswhut/lx-music-sync-server/blob/d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36/package-lock.json)
- [src/constants.ts](https://github.com/lyswhut/lx-music-sync-server/blob/d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36/src/constants.ts)
- [src/server/server.ts](https://github.com/lyswhut/lx-music-sync-server/blob/d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36/src/server/server.ts)
- [src/server/auth.ts](https://github.com/lyswhut/lx-music-sync-server/blob/d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36/src/server/auth.ts)
- [src/server/sync/sync.ts](https://github.com/lyswhut/lx-music-sync-server/blob/d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36/src/server/sync/sync.ts)
- [src/utils/tools.ts](https://github.com/lyswhut/lx-music-sync-server/blob/d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36/src/utils/tools.ts)
- [src/modules/list/sync/sync.ts](https://github.com/lyswhut/lx-music-sync-server/blob/d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36/src/modules/list/sync/sync.ts)
- [src/modules/list/utils.ts](https://github.com/lyswhut/lx-music-sync-server/blob/d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36/src/modules/list/utils.ts)
- [src/modules/dislike/utils.ts](https://github.com/lyswhut/lx-music-sync-server/blob/d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36/src/modules/dislike/utils.ts)
- [src/types/music.d.ts](https://github.com/lyswhut/lx-music-sync-server/blob/d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36/src/types/music.d.ts)
- [src/types/list_sync.d.ts](https://github.com/lyswhut/lx-music-sync-server/blob/d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36/src/types/list_sync.d.ts)

## message2call

### message2call · v0.1.3

许可观察：MIT。阅读范围：静态源码，不代表构建或运行验证。

- [package.json](https://github.com/lyswhut/message2call/blob/2a5c8b966dc689271c2d5c47ad02587cb896ede3/package.json)
- [src/index.ts](https://github.com/lyswhut/message2call/blob/2a5c8b966dc689271c2d5c47ad02587cb896ede3/src/index.ts)
- [types/common.d.ts](https://github.com/lyswhut/message2call/blob/2a5c8b966dc689271c2d5c47ad02587cb896ede3/types/common.d.ts)

## nia

### nia · main

许可观察：Apache-2.0。阅读范围：静态源码，不代表构建或运行验证。

- [docs/ArchitectureLearningJourney.md](https://github.com/android/nowinandroid/blob/2982e63cb691ad49cd009aab43dc206fee6699c2/docs/ArchitectureLearningJourney.md)
- [docs/ModularizationLearningJourney.md](https://github.com/android/nowinandroid/blob/2982e63cb691ad49cd009aab43dc206fee6699c2/docs/ModularizationLearningJourney.md)
- [settings.gradle.kts](https://github.com/android/nowinandroid/blob/2982e63cb691ad49cd009aab43dc206fee6699c2/settings.gradle.kts)
- [core/data/src/main/kotlin/com/google/samples/apps/nowinandroid/core/data/repository/OfflineFirstTopicsRepository.kt](https://github.com/android/nowinandroid/blob/2982e63cb691ad49cd009aab43dc206fee6699c2/core/data/src/main/kotlin/com/google/samples/apps/nowinandroid/core/data/repository/OfflineFirstTopicsRepository.kt)
- [feature/foryou/impl/src/main/kotlin/com/google/samples/apps/nowinandroid/feature/foryou/impl/ForYouViewModel.kt](https://github.com/android/nowinandroid/blob/2982e63cb691ad49cd009aab43dc206fee6699c2/feature/foryou/impl/src/main/kotlin/com/google/samples/apps/nowinandroid/feature/foryou/impl/ForYouViewModel.kt)
- [.github/workflows/Build.yaml](https://github.com/android/nowinandroid/blob/2982e63cb691ad49cd009aab43dc206fee6699c2/.github/workflows/Build.yaml)
- [.github/workflows/Release.yml](https://github.com/android/nowinandroid/blob/2982e63cb691ad49cd009aab43dc206fee6699c2/.github/workflows/Release.yml)

## auxio

### auxio · dev

许可观察：GPL-3.0，具体文件 GPL-3.0-or-later。阅读范围：静态源码，不代表构建或运行验证。

- [README.md](https://github.com/OxygenCobalt/Auxio/blob/f243bceab0b55fdae7462723a3d14e7eec21a432/README.md)
- [LICENSE](https://github.com/OxygenCobalt/Auxio/blob/f243bceab0b55fdae7462723a3d14e7eec21a432/LICENSE)
- [app/src/main/java/org/oxycblt/auxio/playback/state/PlaybackStateManager.kt](https://github.com/OxygenCobalt/Auxio/blob/f243bceab0b55fdae7462723a3d14e7eec21a432/app/src/main/java/org/oxycblt/auxio/playback/state/PlaybackStateManager.kt)
- [app/src/main/java/org/oxycblt/auxio/playback/persist/PersistenceRepository.kt](https://github.com/OxygenCobalt/Auxio/blob/f243bceab0b55fdae7462723a3d14e7eec21a432/app/src/main/java/org/oxycblt/auxio/playback/persist/PersistenceRepository.kt)
- [.github/workflows/android.yml](https://github.com/OxygenCobalt/Auxio/blob/f243bceab0b55fdae7462723a3d14e7eec21a432/.github/workflows/android.yml)
- [app/src/main/java/org/oxycblt/auxio/playback/PlaybackViewModel.kt](https://github.com/OxygenCobalt/Auxio/blob/f243bceab0b55fdae7462723a3d14e7eec21a432/app/src/main/java/org/oxycblt/auxio/playback/PlaybackViewModel.kt)
- [app/src/main/java/org/oxycblt/auxio/playback/service/ExoPlaybackStateHolder.kt](https://github.com/OxygenCobalt/Auxio/blob/f243bceab0b55fdae7462723a3d14e7eec21a432/app/src/main/java/org/oxycblt/auxio/playback/service/ExoPlaybackStateHolder.kt)

## metrolist

### metrolist · main

许可观察：GPL-3.0。阅读范围：静态源码，不代表构建或运行验证。

- [settings.gradle.kts](https://github.com/MetrolistGroup/Metrolist/blob/e6fea3b36c68841bad21daa6252e1d69ecd3afbe/settings.gradle.kts)
- [LICENSE](https://github.com/MetrolistGroup/Metrolist/blob/e6fea3b36c68841bad21daa6252e1d69ecd3afbe/LICENSE)
- [app/src/main/kotlin/com/metrolist/music/playback/MusicService.kt](https://github.com/MetrolistGroup/Metrolist/blob/e6fea3b36c68841bad21daa6252e1d69ecd3afbe/app/src/main/kotlin/com/metrolist/music/playback/MusicService.kt)
- [app/src/main/kotlin/com/metrolist/music/ui/screens/LoginScreen.kt](https://github.com/MetrolistGroup/Metrolist/blob/e6fea3b36c68841bad21daa6252e1d69ecd3afbe/app/src/main/kotlin/com/metrolist/music/ui/screens/LoginScreen.kt)
- [app/src/main/kotlin/com/metrolist/music/viewmodels/AccountViewModel.kt](https://github.com/MetrolistGroup/Metrolist/blob/e6fea3b36c68841bad21daa6252e1d69ecd3afbe/app/src/main/kotlin/com/metrolist/music/viewmodels/AccountViewModel.kt)
- [.github/workflows/build.yml](https://github.com/MetrolistGroup/Metrolist/blob/e6fea3b36c68841bad21daa6252e1d69ecd3afbe/.github/workflows/build.yml)
- [.github/workflows/build_pr.yml](https://github.com/MetrolistGroup/Metrolist/blob/e6fea3b36c68841bad21daa6252e1d69ecd3afbe/.github/workflows/build_pr.yml)
- [.github/workflows/release.yml](https://github.com/MetrolistGroup/Metrolist/blob/e6fea3b36c68841bad21daa6252e1d69ecd3afbe/.github/workflows/release.yml)

## seal

### seal · main

许可观察：GPL-3.0。阅读范围：静态源码，不代表构建或运行验证。

- [settings.gradle.kts](https://github.com/JunkFood02/Seal/blob/7677f61a20fda4210e84215fef9e9b51d25daa47/settings.gradle.kts)
- [LICENSE](https://github.com/JunkFood02/Seal/blob/7677f61a20fda4210e84215fef9e9b51d25daa47/LICENSE)
- [app/src/main/java/com/junkfood/seal/download/Task.kt](https://github.com/JunkFood02/Seal/blob/7677f61a20fda4210e84215fef9e9b51d25daa47/app/src/main/java/com/junkfood/seal/download/Task.kt)
- [app/src/main/java/com/junkfood/seal/download/TaskFactory.kt](https://github.com/JunkFood02/Seal/blob/7677f61a20fda4210e84215fef9e9b51d25daa47/app/src/main/java/com/junkfood/seal/download/TaskFactory.kt)
- [app/src/main/java/com/junkfood/seal/download/DownloaderV2.kt](https://github.com/JunkFood02/Seal/blob/7677f61a20fda4210e84215fef9e9b51d25daa47/app/src/main/java/com/junkfood/seal/download/DownloaderV2.kt)
- [.github/workflows/android.yml](https://github.com/JunkFood02/Seal/blob/7677f61a20fda4210e84215fef9e9b51d25daa47/.github/workflows/android.yml)
- [.github/workflows/android_ci.yml](https://github.com/JunkFood02/Seal/blob/7677f61a20fda4210e84215fef9e9b51d25daa47/.github/workflows/android_ci.yml)

## tempo

### tempo · main

许可观察：GPL-3.0。阅读范围：静态源码，不代表构建或运行验证。

- [LICENSE](https://github.com/CappielloAntonio/tempo/blob/9cf62c8c0c8d90e26be971570c797836dcf0d575/LICENSE)
- [app/src/main/java/com/cappielloantonio/tempo/viewmodel/LoginViewModel.java](https://github.com/CappielloAntonio/tempo/blob/9cf62c8c0c8d90e26be971570c797836dcf0d575/app/src/main/java/com/cappielloantonio/tempo/viewmodel/LoginViewModel.java)
- [app/src/main/java/com/cappielloantonio/tempo/repository/ServerRepository.java](https://github.com/CappielloAntonio/tempo/blob/9cf62c8c0c8d90e26be971570c797836dcf0d575/app/src/main/java/com/cappielloantonio/tempo/repository/ServerRepository.java)
- [app/src/main/java/com/cappielloantonio/tempo/ui/dialog/ServerSignupDialog.java](https://github.com/CappielloAntonio/tempo/blob/9cf62c8c0c8d90e26be971570c797836dcf0d575/app/src/main/java/com/cappielloantonio/tempo/ui/dialog/ServerSignupDialog.java)
- [app/src/main/res/layout/dialog_server_signup.xml](https://github.com/CappielloAntonio/tempo/blob/9cf62c8c0c8d90e26be971570c797836dcf0d575/app/src/main/res/layout/dialog_server_signup.xml)
- [app/src/main/res/layout/fragment_login.xml](https://github.com/CappielloAntonio/tempo/blob/9cf62c8c0c8d90e26be971570c797836dcf0d575/app/src/main/res/layout/fragment_login.xml)

## jellyfin

### jellyfin · master

许可观察：GPL-2.0。阅读范围：静态源码，不代表构建或运行验证。

- [README.md](https://github.com/jellyfin/jellyfin-android/blob/44b94e659e432cf41a90a2079b801a2c71128c23/README.md)
- [app/src/main/java/org/jellyfin/mobile/ui/screens/connect/ServerSelection.kt](https://github.com/jellyfin/jellyfin-android/blob/44b94e659e432cf41a90a2079b801a2c71128c23/app/src/main/java/org/jellyfin/mobile/ui/screens/connect/ServerSelection.kt)
- [app/src/main/java/org/jellyfin/mobile/data/entity/ServerUser.kt](https://github.com/jellyfin/jellyfin-android/blob/44b94e659e432cf41a90a2079b801a2c71128c23/app/src/main/java/org/jellyfin/mobile/data/entity/ServerUser.kt)
- [app/src/main/java/org/jellyfin/mobile/data/entity/ServerEntity.kt](https://github.com/jellyfin/jellyfin-android/blob/44b94e659e432cf41a90a2079b801a2c71128c23/app/src/main/java/org/jellyfin/mobile/data/entity/ServerEntity.kt)
- [.github/workflows/app-test.yaml](https://github.com/jellyfin/jellyfin-android/blob/44b94e659e432cf41a90a2079b801a2c71128c23/.github/workflows/app-test.yaml)
- [LICENSE.md](https://github.com/jellyfin/jellyfin-android/blob/44b94e659e432cf41a90a2079b801a2c71128c23/LICENSE.md)

## finamp

### finamp · redesign

许可观察：MPL-2.0。阅读范围：静态源码，不代表构建或运行验证。

- [LICENSE](https://github.com/finamp-app/finamp/blob/5cd29afec3e72ad90cdeab0df9ce16c857867173/LICENSE)
- [lib/components/LoginScreen/login_flow.dart](https://github.com/finamp-app/finamp/blob/5cd29afec3e72ad90cdeab0df9ce16c857867173/lib/components/LoginScreen/login_flow.dart)
- [lib/components/LoginScreen/login_server_selection_page.dart](https://github.com/finamp-app/finamp/blob/5cd29afec3e72ad90cdeab0df9ce16c857867173/lib/components/LoginScreen/login_server_selection_page.dart)
- [lib/components/LoginScreen/login_user_selection_page.dart](https://github.com/finamp-app/finamp/blob/5cd29afec3e72ad90cdeab0df9ce16c857867173/lib/components/LoginScreen/login_user_selection_page.dart)
- [lib/components/LoginScreen/login_authentication_page.dart](https://github.com/finamp-app/finamp/blob/5cd29afec3e72ad90cdeab0df9ce16c857867173/lib/components/LoginScreen/login_authentication_page.dart)

## 官方网页

| 来源 | 链接 | 本次证据范围 |
| --- | --- | --- |
| Spotify login | [官方页面](https://accounts.spotify.com/en/login) | 已打开公开页面文本；第一步 email/continue 和其他登录选项，未提交 |
| Spotify signup | [官方页面](https://www.spotify.com/signup) | 已打开，重定向 jp；第一步 email/next，未提交 |
| Apple Music Android | [官方页面](https://support.apple.com/en-us/109340) | 官方支持流程，未操作 app |
| TIDAL signup | [官方页面](https://support.tidal.com/hc/en-us/articles/115003671729-Sign-up-for-Tidal) | 官方注册说明；登录根 URL 另返回 404，未完成动态流程 |
| Symfonium providers | [官方页面](https://docs.symfonium.app/wiki/settings/settings-manage-media-providers/) | 官方 provider 管理页，含列表及添加入口说明 |
| Symfonium WebDAV | [官方页面](https://docs.symfonium.app/wiki/providers/webdav-media-provider-configuration/) | 官方配置页，URL/登录/路径与连接验证 |
| Android architecture | [官方页面](https://developer.android.com/topic/architecture/recommendations) | 官方架构建议，非强制模板 |
| Media3 background | [官方页面](https://developer.android.com/media/media3/session/background-playback) | Player/Session 服务生命周期 |
| Media3 downloads | [官方页面](https://developer.android.com/media/media3/exoplayer/downloading-media) | DownloadService/Manager/Index |
| GitHub secure use | [官方页面](https://docs.github.com/en/actions/reference/security/secure-use) | Actions 最小权限/依赖固定/不受信代码边界 |
| GitHub protected branches | [官方页面](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches) | 分支保护与必需检查概念 |

## 检索后未作为主要实现依据的样本

- z-huang/InnerTune：确认仓库元信息，未深读实现，不能当作本报告架构结论的证据。
- vfsfitvnm/ViMusic：本次 GitHub 元信息为 archived，未将其列为持续维护模板。
- anandnet/Harmony-Music：README 明确 Flutter 且不再维护；只作筛选记录，未作原生架构依据。
- 搜索时部分旧地址返回 404；Finamp 最终使用搜索确认的 finamp-app/finamp，Tempo 使用 CappielloAntonio/tempo。未把 404 当作项目不存在的通用结论。

## 证据限制

没有在 Android Studio/模拟器/设备运行样本；没有访问生产部署；没有做真实登录/注册；没有全量审计源站接口、所有源码与历史版本。本报告里标记为“建议/待验证”的内容不因为附有仓库链接就成为已实现事实。商业网站会按区域/实验变化，公开文本与官方文章不足以证明某个 Android 版本的精确视觉或全部错误路径。

没有把 GPL/MPL 样本代码或商业截图直接复制到产品仓库。未来直接复用代码时应按文件核查授权和署名；当前研究主要吸收结构/流程并拟定独立实现。
