# lux-music-android 架构参考：优秀原生安卓开源仓库调研

调研时间：2026-11-13（太平洋时间）。方法：用 GitHub API（`gh api repos/...`、`git/trees?recursive=1`）列出文件树，用 raw.githubusercontent.com 读取原文件。**没有 clone 任何仓库。**
star 数、最后推送时间、许可证、是否归档都是本次 API 返回的实时值。「最后推送」取的是 `pushed_at`，指任意分支的最后一次推送，按太平洋时间折算到日期。
文中每条结论都附了文件链接。只看到配置、没有读到实际用法的地方，标注为 **待确认**。

---

## 1. 仓库总表

### 1.1 采用的仓库（12 个）

| # | 仓库 | Stars | 最后推送 | 许可证 | 归档 | 为什么相关 |
|---|---|---|---|---|---|---|
| 1 | [android/nowinandroid](https://github.com/android/nowinandroid) | 22,077 | 2026-11-12 | Apache-2.0 | 否 | Google 官方的模块化与 build-logic 范本：feature api/impl 拆分、Dependency Guard、模块图检查 |
| 2 | [android/architecture-samples](https://github.com/android/architecture-samples) | 45,899 | 2026-11-11 | Apache-2.0 | 否 | 官方单模块 UDF + Repository + Fake 测试的最小范本 |
| 3 | [androidx/media](https://github.com/androidx/media)（session demos） | 3,074 | 2026-11-13 | Apache-2.0 | 否 | Media3 官方的 `MediaLibraryService` 写法，是播放服务的标准答案 |
| 4 | [Automattic/pocket-casts-android](https://github.com/Automattic/pocket-casts-android) | 2,855 | 2026-11-13 | MPL-2.0 | 否 | 商业级音频播放 App，约 50 个模块；能看到旧播放服务迁到 Media3 的真实过程 |
| 5 | [OxygenCobalt/Auxio](https://github.com/OxygenCobalt/Auxio) | 4,454 | 2026-11-01 | GPL-3.0 | 否 | 口碑很好的本地音乐播放器，播放状态不以 MediaSession 为中心 |
| 6 | [FoedusProgramme/Gramophone](https://github.com/FoedusProgramme/Gramophone)（原 AkaneTan/Gramophone，API 已重定向） | 2,393 | 2026-11-11 | GPL-3.0 | 否 | Media3 音乐播放器，歌词功能强，带 baseline profile |
| 7 | [MetrolistGroup/Metrolist](https://github.com/MetrolistGroup/Metrolist)（InnerTune 系的活跃 fork） | 14,205 | 2026-11-02 | GPL-3.0 | 否 | 在线音源 + Compose + Media3 + Room，多歌词源，CI 发布签名 APK |
| 8 | [mihonapp/mihon](https://github.com/mihonapp/mihon) | 25,255 | 2026-11-12 | Apache-2.0 | 否 | 扩展（插件）模型：source-api、签名信任、lib 版本校验，可类比自定义音源 |
| 9 | [JunkFood02/Seal](https://github.com/JunkFood02/Seal) | 30,621 | 2026-09-25 | GPL-3.0 | 否 | 小团队 Compose App 的版本号与 ABI 分包写法 |
| 10 | [thunderbird/thunderbird-android](https://github.com/thunderbird/thunderbird-android) | 14,213 | 2026-11-12 | Apache-2.0 | 否 | 大型模块化：api/internal 拆分、ADR、detekt + spotless + dependencyGuard 三道质量检查 |
| 11 | [element-hq/element-x-android](https://github.com/element-hq/element-x-android) | 2,504 | 2026-11-13 | AGPL-3.0 | 否 | 约 270 个模块；用 Konsist 架构测试把规矩写成测试；Presenter 状态模式 |
| 12 | [cashapp/zipline](https://github.com/cashapp/zipline) | 2,319 | 2026-11-11 | Apache-2.0 | 否 | Kotlin 宿主嵌 QuickJS 跑 JS，接口用强类型桥接；给自定义音源沙箱做对照 |

### 1.2 补充参考（体量小，不当架构范本）

| 仓库 | Stars | 最后推送 | 许可证 | 用途 |
|---|---|---|---|---|
| [HarlonWang/quickjs-wrapper](https://github.com/HarlonWang/quickjs-wrapper) | 272 | 2025-07-28 | Apache-2.0 | lux-music-mobile 现在就用它：[`android/app/build.gradle`](https://github.com/JuneDrinleng/lux-music-mobile/blob/master/android/app/build.gradle) 依赖 `wang.harlon.quickjs:wrapper-android:2.4.0` |
| [zhangnannull-debug/LuneLX](https://github.com/zhangnannull-debug/LuneLX) | 3 | 2026-10-20 | GPL-3.0 | 原生 Kotlin 实现的 lx 兼容音源脚本引擎（2026-10 新建） |
| [MiNoMinoaa/PixelPlayerOLX](https://github.com/MiNoMinoaa/PixelPlayerOLX) | 31 | 2026-11-09 | GPL-3.0 | 同上；两者的 `LxScriptEngine.kt` 结构几乎一样 |
| [z-huang/InnerTune](https://github.com/z-huang/InnerTune) | 6,147 | 2025-11-12 | GPL-3.0 | Metrolist 的上游；只借鉴它「音源/歌词源拆成纯 JVM 模块」的做法 |

### 1.3 剔除的候选

| 仓库 | 原因 |
|---|---|
| [android/uamp](https://github.com/android/uamp) | **已归档**（archived=true），基于旧 API |
| [vfsfitvnm/ViMusic](https://github.com/vfsfitvnm/ViMusic) | **已归档**，最后推送 2024-07-15 |
| z-huang/InnerTune | 未归档，但最后推送 2025-11-12，已近一年不活跃，降为补充参考 |
| [PixelPlayerHQ/PixelPlayer](https://github.com/PixelPlayerHQ/PixelPlayer)（6,967★） | 代码不错，但 [`LICENSE`](https://github.com/PixelPlayerHQ/PixelPlayer/blob/master/LICENSE) 写的是 **Proprietary License**（API 返回 NOASSERTION），不能参考或借用代码 |

> 许可证提醒：Auxio、Gramophone、Metrolist、Seal 是 GPL-3.0，Element X 是 AGPL-3.0。lux-music-android 只借鉴它们的**思路**，不复制代码，除非我们也用 GPL 发布。Apache-2.0 的（NiA、media3、mihon、Thunderbird、zipline）可以按许可证借用代码片段。

---

## 2. 逐仓库发现

### 2.1 android/nowinandroid（NiA）
- **模块**：[`settings.gradle.kts`](https://github.com/android/nowinandroid/blob/main/settings.gradle.kts) 列出了 `:app`、`:core:{analytics,common,data,data-test,database,datastore,datastore-proto,designsystem,domain,model,navigation,network,notifications,testing,ui,...}`、`:feature:{foryou,interests,bookmarks,topic,search}:{api,impl}`、`:lint`、`:sync:work`、`:benchmarks`。同一文件里还开了 `TYPESAFE_PROJECT_ACCESSORS` 和 `RepositoriesMode.FAIL_ON_PROJECT_REPOS`。
- **convention plugins**：`includeBuild("build-logic")`。[`build-logic/convention/build.gradle.kts`](https://github.com/android/nowinandroid/blob/main/build-logic/convention/build.gradle.kts) 注册了 16 个插件，包括 androidApplication(Compose)、androidLibrary(Compose)、androidFeatureApi/Impl、hilt、androidRoom、androidLint、jvmLibrary、jacoco 等，并开了 `validatePlugins { failOnWarning = true }`。
  - [`AndroidFeatureImplConventionPlugin.kt`](https://github.com/android/nowinandroid/blob/main/build-logic/convention/src/main/kotlin/AndroidFeatureImplConventionPlugin.kt)：只要应用这个插件，就自动依赖 `:core:ui`、`:core:designsystem`、lifecycle-viewmodel-compose、hilt、navigation3。feature 模块不用自己写依赖。
  - [`AndroidFeatureApiConventionPlugin.kt`](https://github.com/android/nowinandroid/blob/main/build-logic/convention/src/main/kotlin/AndroidFeatureApiConventionPlugin.kt)：api 模块只 `api(:core:navigation)`，加 serialization（放导航 key）。
- **SDK**：[`KotlinAndroid.kt`](https://github.com/android/nowinandroid/blob/main/build-logic/convention/src/main/kotlin/com/google/samples/apps/nowinandroid/KotlinAndroid.kt) 里是 `compileSdk = 37`、`minSdk = 24`；[`AndroidApplicationConventionPlugin.kt`](https://github.com/android/nowinandroid/blob/main/build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt) 里是 `targetSdk = 36`，并统一挂 `com.dropbox.dependency-guard`、lint、spotless、badging。
- **版本**（[`gradle/libs.versions.toml`](https://github.com/android/nowinandroid/blob/main/gradle/libs.versions.toml)）：AGP 9.3.2、Kotlin 2.3.0、Hilt 2.59、Room 2.8.3、Navigation3 1.2.0、DataStore 1.2.0 + protobuf、Roborazzi 1.56.0。
- **数据层**：[`core/data/.../repository/`](https://github.com/android/nowinandroid/tree/main/core/data/src/main/kotlin/com/google/samples/apps/nowinandroid/core/data/repository) 里是接口加实现，如 `NewsRepository` + `OfflineFirstNewsRepository`。`core/domain` 放少量 UseCase。`sync/work` 用 WorkManager 同步。
- **State**：每个 feature/impl 一个 `XxxViewModel`，比如 [`ForYouViewModel.kt`](https://github.com/android/nowinandroid/blob/main/feature/foryou/impl/src/main/kotlin/com/google/samples/apps/nowinandroid/feature/foryou/impl/ForYouViewModel.kt)。架构说明在 [`docs/ArchitectureLearningJourney.md`](https://github.com/android/nowinandroid/blob/main/docs/ArchitectureLearningJourney.md) 和 [`docs/ModularizationLearningJourney.md`](https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md)。
- **自定义 lint**：[`lint/.../designsystem/DesignSystemDetector.kt`](https://github.com/android/nowinandroid/blob/main/lint/src/main/kotlin/com/google/samples/apps/nowinandroid/lint/designsystem/DesignSystemDetector.kt) 禁止在 feature 里直接用 Material 组件，只能用设计系统组件。lux 的杂志风规范可以照这个思路，用 lint 卡住。[`AndroidLintConventionPlugin.kt`](https://github.com/android/nowinandroid/blob/main/build-logic/convention/src/main/kotlin/AndroidLintConventionPlugin.kt) 开了 `xmlReport`、`sarifReport` 和 `checkDependencies`。
- **模块图**：[`Graph.kt`](https://github.com/android/nowinandroid/blob/main/build-logic/convention/src/main/kotlin/com/google/samples/apps/nowinandroid/Graph.kt) 用 `graphDump`/`graphUpdate` 任务生成每个模块 README 里的 mermaid 依赖图，CI 检查图有没有过期。
- **CI**（[`.github/workflows/Build.yaml`](https://github.com/android/nowinandroid/blob/main/.github/workflows/Build.yaml)）：
  - PR 和 push main 都会跑。
  - 依次是 `:build-logic:convention:check` → `spotlessCheck` → `dependencyGuard`（基线变化时，在 PR 里自动提交新基线，fork 来的 PR 直接失败）→ `graphUpdate` 检查 → Roborazzi 截图测试 → `testDemoDebug` → assemble → `lintProdRelease`（上传 SARIF）→ `checkProdReleaseBadging`。
  - 另有 androidTest 矩阵（API 26/34）和 jacoco 覆盖率门槛（总体 40，改动文件 60）。
  - `concurrency: cancel-in-progress`。
- **发版**：[`Release.yml`](https://github.com/android/nowinandroid/blob/main/.github/workflows/Release.yml) 在 push `v*` tag 时构建 `assembleDemoRelease` 并上传到 Release。用的是已不再维护的 `actions/create-release@v1`，不要照抄。[`NightlyBaselineProfiles.yaml`](https://github.com/android/nowinandroid/blob/main/.github/workflows/NightlyBaselineProfiles.yaml) 每天定时生成 baseline profile。
- **PR 模板**：[`.github/pull_request_template.md`](https://github.com/android/nowinandroid/blob/main/.github/pull_request_template.md) 要求先本地跑 `testDemoDebug` 和 `spotlessApply`。

### 2.2 android/architecture-samples
- 只有两个模块，见 [`settings.gradle.kts`](https://github.com/android/architecture-samples/blob/main/settings.gradle.kts)：`:app` 和 `:shared-test`。
- [`app/build.gradle.kts`](https://github.com/android/architecture-samples/blob/main/app/build.gradle.kts) 用 Hilt + KSP + Room，SDK 版本从 version catalog 读。
- 示范了 `TaskRepository` 接口和 `DefaultTaskRepository` 实现，测试用 [`shared-test/.../FakeTaskRepository.kt`](https://github.com/android/architecture-samples/blob/main/shared-test/src/main/java/com/example/android/architecture/blueprints/todoapp/data/FakeTaskRepository.kt)，**测试用 Fake 而不是 mock**。
- CI 见 [`build_test.yaml`](https://github.com/android/architecture-samples/blob/main/.github/workflows/build_test.yaml)，在模拟器上跑 `:app:connectedCheck`。
- 价值是最小的 UDF 样板。模块划分对 lux 来说太粗。

### 2.3 androidx/media（Media3 session demo）
- 当前稳定版是 **1.11.1（2026-09-10）**，见 [`RELEASENOTES.md`](https://github.com/androidx/media/blob/release/RELEASENOTES.md)。库自身 `minSdkVersion = "23"`，见 [`gradle/libs.versions.toml`](https://github.com/androidx/media/blob/release/gradle/libs.versions.toml)。
- [`demos/session_service/.../DemoPlaybackService.kt`](https://github.com/androidx/media/blob/release/demos/session_service/src/main/java/androidx/media3/demo/session/DemoPlaybackService.kt) 的结构：
  - `open class DemoPlaybackService : MediaLibraryService()`。
  - `onCreate` 里建 `ExoPlayer` 和 `MediaLibrarySession.Builder(this, player, callback)`；`onGetSession` 返回 session；`onDestroy` 依次 `session.release()`、`player.release()`。
  - `setAudioAttributes(AudioAttributes.DEFAULT, handleAudioFocus = true)`，由框架处理音频焦点。
  - 用 `CommandButton` 和 `setMediaButtonPreferences` 做自定义按钮（随机播放）。
  - 用 DataStore 存「最后播放的条目」来支持播放恢复。
  - 监听 `onForegroundServiceStartNotAllowedException`。
- [`DemoMediaLibrarySessionCallback.kt`](https://github.com/androidx/media/blob/release/demos/session_service/src/main/java/androidx/media3/demo/session/DemoMediaLibrarySessionCallback.kt) 实现了 `onGetLibraryRoot/onGetItem/onGetChildren/onAddMediaItems/onSetMediaItems/onPlaybackResumption/onSearch`。媒体库浏览、车机、播放恢复都靠这些回调。
- 服务与 UI 分开。`session_service` 是独立模块，App 壳 [`demos/session/.../PlaybackService.kt`](https://github.com/androidx/media/blob/release/demos/session/src/main/java/androidx/media3/demo/session/PlaybackService.kt) 只覆写 `getSingleTopActivity` 和 `getBackStackedActivity` 两个 PendingIntent。
- [`demos/session/src/main/AndroidManifest.xml`](https://github.com/androidx/media/blob/release/demos/session/src/main/AndroidManifest.xml) 声明了 `FOREGROUND_SERVICE_MEDIA_PLAYBACK`、`foregroundServiceType="mediaPlayback"`，以及 `androidx.media3.session.MediaLibraryService` 和 `android.media.browse.MediaBrowserService` 两个 action。
- 构建逻辑在 [`build-logic/`](https://github.com/androidx/media/tree/release/build-logic/src/main/kotlin)，用的是 precompiled script plugin（`media3.android-library.gradle.kts` 等）。

### 2.4 Automattic/pocket-casts-android
- **模块**：[`settings.gradle.kts`](https://github.com/Automattic/pocket-casts-android/blob/main/settings.gradle.kts) 分三层：`:app/:automotive/:tv/:wear` 四个 App 壳，`:modules:features:*`（21 个，比如 player、podcasts、search、endofyear、transcripts），`:modules:services:*`（22 个，比如 model、repositories、preferences、servers、localization、compose、ui、sharedtest）。**没有 build-logic**，公共配置放在根目录的 [`dependencies.gradle.kts`](https://github.com/Automattic/pocket-casts-android/blob/main/dependencies.gradle.kts) 和 [`build.gradle.kts`](https://github.com/Automattic/pocket-casts-android/blob/main/build.gradle.kts)。
- **SDK**：在 [`dependencies.gradle.kts`](https://github.com/Automattic/pocket-casts-android/blob/main/dependencies.gradle.kts) 里：手机 minSdk 24，Wear 26，车机和 TV 28；targetSdk 36；compileSdk 37。
- **版本**：Hilt 2.60.1、Kotlin 2.4.21、Media3 1.11.1、Room 2.8.5，见 [`libs.versions.toml`](https://github.com/Automattic/pocket-casts-android/blob/main/gradle/libs.versions.toml)。版本号写在 [`version.properties`](https://github.com/Automattic/pocket-casts-android/blob/main/version.properties)（`versionName=8.25-rc-1`、`versionCode=9468`）。
- **播放**：在 [`modules/services/repositories/.../playback/`](https://github.com/Automattic/pocket-casts-android/tree/main/modules/services/repositories/src/main/java/au/com/shiftyjelly/pocketcasts/repositories/playback)。
  - `PlaybackService.kt`（208 行）是 `@AndroidEntryPoint ... MediaLibraryService()`。
  - 同目录还留着 `LegacyPlaybackService.kt`。`PlaybackServiceToggle.kt` 用 `FeatureFlag.MEDIA3_SESSION` 和 `setComponentEnabled` 在新旧服务之间切换。
  - 逻辑主要在 `PlaybackManager.kt`（**3,242 行**）和 `MediaSessionManager.kt`（**1,720 行**）。
  - 结论：大厂也在从旧 session 迁到 Media3，迁移期的双服务和巨型 Manager 正是 lux 应该从第一天就避开的包袱。
- **格式与 lint**：根 `build.gradle.kts` 用 Spotless + ktlint + compose-rules，并支持只检查 pre-commit 改动的文件。
- **CI**：主 CI 在 **Buildkite**，不在 GitHub Actions，见 [`.buildkite/pipeline.yml`](https://github.com/Automattic/pocket-casts-android/blob/main/.buildkite/pipeline.yml)。依次是 Gradle Wrapper 校验、Danger、Lint、单测、**依赖差异评论**（`comment_with_dependency_diff`）、**合并后 Manifest 差异**、Spotless、仪器测试、Release APK。GitHub Actions 只做辅助（wrapper 校验、依赖提交、Claude review）。
- **PR 模板**：[`.github/pull_request_template.md`](https://github.com/Automattic/pocket-casts-android/blob/main/.github/pull_request_template.md) 的清单要求：CHANGELOG、`spotlessApply`、字符串集中放在 localization 模块、Compose 组件要有 preview、UI 改动要在主题、横屏、大字体、TalkBack 下都测过。

### 2.5 OxygenCobalt/Auxio
- **模块**：[`settings.gradle`](https://github.com/OxygenCobalt/Auxio/blob/dev/settings.gradle) 里只有 `:app` 和 `:musikr`（音乐库扫描和标签解析库）。
  - 通过 `includeMedia3.execute(file('media'))` **把自己 fork 的 media3 当子模块用源码编译**，见 [`.gitmodules`](https://github.com/OxygenCobalt/Auxio/blob/dev/.gitmodules)。
  - `musikr` 里还用 C++ 子模块编译 taglib。
- **服务**：[`AuxioService.kt`](https://github.com/OxygenCobalt/Auxio/blob/dev/app/src/main/java/org/oxycblt/auxio/AuxioService.kt) 是 `@AndroidEntryPoint class AuxioService : MediaBrowserServiceCompat()`，内部再拆成 `PlaybackServiceFragment` 和 `MusicServiceFragment` 两个由工厂注入的部件。
- **播放状态**：[`playback/state/PlaybackStateManager.kt`](https://github.com/OxygenCobalt/Auxio/blob/dev/app/src/main/java/org/oxycblt/auxio/playback/state/PlaybackStateManager.kt) 的注释写明，Auxio 不以 MediaSession 为中心，而是由这个类集中管理播放状态，并且**不应在 playback 模块外直接使用**：UI 用 `PlaybackViewModel`，播放器一侧走 `PlaybackStateHolder` 接口（见 [`PlaybackStateHolder.kt`](https://github.com/OxygenCobalt/Auxio/blob/dev/app/src/main/java/org/oxycblt/auxio/playback/state/PlaybackStateHolder.kt)）。
- **DI 和数据**：Hilt，按功能分 `XxxModule.kt`（如 `playback/PlaybackModule.kt`、`music/MusicModule.kt`）；Room（[`app/build.gradle`](https://github.com/OxygenCobalt/Auxio/blob/dev/app/build.gradle)）。
- **SDK**：根 [`build.gradle`](https://github.com/OxygenCobalt/Auxio/blob/dev/build.gradle) 里 `min_sdk = 24`、`target_sdk = 37`；Spotless 用 `ktfmt().kotlinlangStyle()`。
- **CI**：[`lint.yml`](https://github.com/OxygenCobalt/Auxio/blob/dev/.github/workflows/lint.yml) 跑 `spotlessCheck` 和 `musikr:testDebug`；[`android.yml`](https://github.com/OxygenCobalt/Auxio/blob/dev/.github/workflows/android.yml) 跑 `app:packageDebug` 并上传。
- **PR 模板**：[`.github/PULL_REQUEST_TEMPLATE.md`](https://github.com/OxygenCobalt/Auxio/blob/dev/.github/PULL_REQUEST_TEMPLATE.md) 要求勾选改动类型（用户可见 / 开发者可见）并附 debug APK。
- UI 用 Fragment + View 体系（从 `MainFragment.kt` 和 `*Fragment.kt` 看出），不是 Compose。

### 2.6 FoedusProgramme/Gramophone
- **模块**：[`settings.gradle.kts`](https://github.com/FoedusProgramme/Gramophone/blob/beta/settings.gradle.kts) 里有 `:app`、`:hificore`、`:baselineprofile`、`:misc:*`。同样通过 `includeBuild("media3")` 加 `dependencySubstitution` **用自己 fork 的 media3**（submodule `nift4/media`，`gramophone` 分支，见 [`.gitmodules`](https://github.com/FoedusProgramme/Gramophone/blob/beta/.gitmodules)）。
- **服务**：[`logic/GramophonePlaybackService.kt`](https://github.com/FoedusProgramme/Gramophone/blob/beta/app/src/main/java/org/akanework/gramophone/logic/GramophonePlaybackService.kt) 有 **1,994 行**，是典型的巨型服务。
- **歌词**：歌词解析在 [`logic/utils/SemanticLyrics.kt`](https://github.com/FoedusProgramme/Gramophone/blob/beta/app/src/main/java/org/akanework/gramophone/logic/utils/SemanticLyrics.kt)；歌词 View 有 `LyricsView.kt`、`NewLyricsView.kt`，还有 `LegacyLyricsAdapter.kt`（新旧两套并存）。
- [`app/build.gradle.kts`](https://github.com/FoedusProgramme/Gramophone/blob/beta/app/build.gradle.kts)：
  - `minSdk = 23`、`targetSdk = 37`、`compileSdk = 37`。
  - 有 Compose（compose-bom 2025.05.00），**没看到 Hilt/Koin 依赖**（DI 方式待确认，推测是手工注入）。
  - 多套签名配置读的是 `AKANE_RELEASE_*` 属性。
- **baseline profile**：[`baselineprofile/build.gradle.kts`](https://github.com/FoedusProgramme/Gramophone/blob/beta/baselineprofile/build.gradle.kts) 用 Gradle Managed Device `pixel6Api34` 生成，生成结果提交在 `app/src/*/generated/baselineProfiles/baseline-prof.txt`。
- **CI**：[`.github/workflows/android.yml`](https://github.com/FoedusProgramme/Gramophone/blob/beta/.github/workflows/android.yml)。PR 只跑 `assembleDebug`；push 到 beta 时把 secrets 里的 base64 keystore 解码写进 `~/.gradle/gradle.properties`，再 `assembleRelease`。还设了 `paths-ignore`（文档、fastlane 改动不触发构建）。

### 2.7 MetrolistGroup/Metrolist（InnerTune 系）
- **模块**：[`settings.gradle.kts`](https://github.com/MetrolistGroup/Metrolist/blob/main/settings.gradle.kts) 里只有 `:app` 和 `:innertube`。[`innertube/build.gradle.kts`](https://github.com/MetrolistGroup/Metrolist/blob/main/innertube/build.gradle.kts) 是 Android library，用 ktor + kotlinx.serialization。
- **对比上游 InnerTune**：[`InnerTune/settings.gradle.kts`](https://github.com/z-huang/InnerTune/blob/dev/settings.gradle.kts) 把 `:innertube`、`:kugou`、`:lrclib` 拆成独立模块，并且都是 **`kotlin("jvm")` 纯 JVM 模块**（见 [`kugou/build.gradle.kts`](https://github.com/z-huang/InnerTune/blob/dev/kugou/build.gradle.kts)），不依赖 Android，可以直接跑 JVM 单测。lux 的平台接口层应该学这个。
- **播放**：在 [`app/.../playback/`](https://github.com/MetrolistGroup/Metrolist/tree/main/app/src/main/kotlin/com/metrolist/music/playback)。`MusicService.kt` 有 **5,080 行**，文件头是 `@file:Suppress("DEPRECATION")`，类上标 `@AndroidEntryPoint`。`MediaLibrarySessionCallback.kt`、`PlayerConnection.kt`、`SleepTimer.kt`、`StreamUrlCache.kt` 是拆出来的部件。
- **数据**：[`db/DatabaseDao.kt`](https://github.com/MetrolistGroup/Metrolist/blob/main/app/src/main/kotlin/com/metrolist/music/db/DatabaseDao.kt) 是一个 **2,046 行的大 DAO**；[`MusicDatabase.kt`](https://github.com/MetrolistGroup/Metrolist/blob/main/app/src/main/kotlin/com/metrolist/music/db/MusicDatabase.kt) 已经累积到 `AutoMigration(from = 37)`，可见迁移的历史包袱。DI 是 Hilt（`di/AppModule.kt` 等）。
- **歌词**：[`lyrics/LyricsProvider.kt`](https://github.com/MetrolistGroup/Metrolist/blob/main/app/src/main/kotlin/com/metrolist/music/lyrics/LyricsProvider.kt) 接口加 9 个实现（KuGou、LrcLib、YouTube 等），形成可插拔的多歌词源。缺点是接口方法带 `Context` 参数，和 Android 耦合了。
- [`app/build.gradle.kts`](https://github.com/MetrolistGroup/Metrolist/blob/main/app/build.gradle.kts)：`minSdk = 26`、`targetSdk = 36`；flavors 有 `foss/gms/izzy`；签名读 `System.getenv("STORE_PASSWORD")` 等。
- **CI**：[`release.yml`](https://github.com/MetrolistGroup/Metrolist/blob/main/.github/workflows/release.yml) 先检测版本有没有变，按 flavor 矩阵 `assemble…Release`，再用 `ilharp/sign-android-release` 和 secrets `KEYSTORE/KEY_ALIAS/KEY_PASSWORD/KEYSTORE_PASSWORD` 签名；另有 `build_pr.yml`。依赖更新用 [`renovate.json`](https://github.com/MetrolistGroup/Metrolist/blob/main/renovate.json)。

### 2.8 mihonapp/mihon（扩展模型）
- **模块**：[`settings.gradle.kts`](https://github.com/mihonapp/mihon/blob/main/settings.gradle.kts) 里有 `:app`、`:domain`、`:data`、`:source-api`、`:source-local`、`:core:{common,archive,metro}`、`:presentation-core`、`:presentation-widget`、`:i18n`、`:baseline-profile`、`:telemetry`。build-logic 放在 `gradle/build-logic`，含 `PluginAndroidApplication/Library/ComposeAndroid/KotlinMultiplatform/Spotless.kt`。
- **SDK**：放在单独的 [`gradle/mihon.versions.toml`](https://github.com/mihonapp/mihon/blob/main/gradle/mihon.versions.toml)：`android-sdk-min = "26"`、`compile = "37.2"`、`target = "36"`。
- **扩展 API**：[`source-api/.../source/Source.kt`](https://github.com/mihonapp/mihon/blob/main/source-api/src/main/kotlin/eu/kanade/tachiyomi/source/Source.kt)、`CatalogueSource.kt`、`online/HttpSource.kt`、`model/*`，是一个独立模块，第三方扩展只依赖它。
- **加载与信任**（[`extension/util/ExtensionLoader.kt`](https://github.com/mihonapp/mihon/blob/main/app/src/main/java/eu/kanade/tachiyomi/extension/util/ExtensionLoader.kt)）：
  1. 从 APK metadata 读 `tachiyomix.extensionLib`，**校验 lib 版本在 `SUPPORTED_LIB_VERSIONS` 里**，不在就 `UnsupportedLibVersion`。
  2. 没签名就 `Unsigned`。
  3. 用 [`TrustExtension.kt`](https://github.com/mihonapp/mihon/blob/main/app/src/main/java/eu/kanade/domain/extension/interactor/TrustExtension.kt) 判断签名指纹是否属于已信任的仓库。用户手动信任时绑定 `包名:versionCode:签名`，**扩展一升级就要重新信任**。
  4. 最后才用 `DelegateLastClassLoaderCompat` 加载类，代码注释写明「便宜的检查先做」。
  - 每种失败都有明确的 `Extension.NotLoaded.Reason`，可以直接显示在 UI 上。
- **DI**：[`libs.versions.toml`](https://github.com/mihonapp/mihon/blob/main/gradle/libs.versions.toml) 里 `injekt` 和 `metro 1.5.2` 同时存在，`ExtensionLoader` 里已经在用 `context.appGraph`。**看起来正从 Injekt 迁到 Metro，迁移进度待确认。**
- **数据和 UI**：数据层用 SQLDelight，CI 跑 `verifySqlDelightMigration`；UI 导航用 Voyager。
- **CI**：
  - [`build.yml`](https://github.com/mihonapp/mihon/blob/main/.github/workflows/build.yml)：`spotlessCheck`、`testDebugUnitTest`、`verifySqlDelightMigration`、`assembleRelease -Pdist=ci`、`actions/dependency-review-action`。**所有 action 都钉在 commit SHA 上**，比如 `actions/checkout@3d3c42e…`。
  - [`release.yml`](https://github.com/mihonapp/mihon/blob/main/.github/workflows/release.yml)：push tag 触发；出 arm64-v8a、armeabi-v7a、universal 和 foss 四种 APK；签名用 secrets `SIGNING_KEY/ALIAS/KEY_STORE_PASSWORD/KEY_PASSWORD`；用 `softprops/action-gh-release` 发布。
- **ABI 分包**：[`app/build.gradle.kts`](https://github.com/mihonapp/mihon/blob/main/app/build.gradle.kts) 里 `splits.abi { include("armeabi-v7a","arm64-v8a","x86","x86_64"); isUniversalApk = true }`。

### 2.9 JunkFood02/Seal
- 模块只有 `:app` 和 `:color`，见 [`settings.gradle.kts`](https://github.com/JunkFood02/Seal/blob/main/settings.gradle.kts)。DI 用 Koin 4.0.0，数据用 Room 2.6.1，格式化用 ktfmt，见 [`libs.versions.toml`](https://github.com/JunkFood02/Seal/blob/main/gradle/libs.versions.toml)。
- **版本模型**：[`buildSrc/src/main/kotlin/Version.kt`](https://github.com/JunkFood02/Seal/blob/main/buildSrc/src/main/kotlin/Version.kt) 用 `sealed class Version { Alpha/Beta/ReleaseCandidate/Stable }` 生成 versionName（如 `x.y.z-beta.N`）和单调递增的 versionCode。[`app/build.gradle.kts`](https://github.com/JunkFood02/Seal/blob/main/app/build.gradle.kts) 里用 `check(versionCode == currentVersionCode)` 防止两处版本号不一致，并按 ABI 给 versionCode 加偏移（`abiCodes`）。
- `minSdk = 24`、`targetSdk = 35`；Room 配了 `schemaDirectory("$projectDir/schemas")`。
- **CI**：[`android.yml`](https://github.com/JunkFood02/Seal/blob/main/.github/workflows/android.yml) 用 `ilharp/sign-android-release@nightly` 签名。用浮动的 `@nightly` 标签不安全，不要照抄。

### 2.10 thunderbird/thunderbird-android
- **模块与插件**：
  - [`settings.gradle.kts`](https://github.com/thunderbird/thunderbird-android/blob/main/settings.gradle.kts) 里有 `includeBuild("build-plugin")`，还有独立的 `components` 构建。
  - [`build-plugin/src/main/kotlin/`](https://github.com/thunderbird/thunderbird-android/tree/main/build-plugin/src/main/kotlin) 下的 precompiled 插件有 `thunderbird.app.android(.compose)`、`thunderbird.library.android(.compose)`、`thunderbird.library.jvm`、`thunderbird.library.kmp(.compose)`、`thunderbird.dependency.check`。
  - [`build-plugin/plugin/`](https://github.com/thunderbird/thunderbird-android/tree/main/build-plugin/plugin/src/main/kotlin/net/thunderbird/gradle/plugin) 下还有 badging 检查、baseline profile 生产/消费、versioning、feature flag 代码生成。
- **SDK**：[`ProjectConfig.kt`](https://github.com/thunderbird/thunderbird-android/blob/main/build-plugin/plugin/src/main/kotlin/net/thunderbird/gradle/plugin/ProjectConfig.kt) 里 `sdkMin = 24`、`sdkTarget = 36`、`sdkCompile = 37`。
- **架构文档**：[`docs/architecture/`](https://github.com/thunderbird/thunderbird-android/tree/main/docs/architecture) 下有 module-organization、module-structure、repository-pattern、ui-architecture、feature-flags、legacy-module-integration 等，ADR 放在 [`docs/engineering/adr/`](https://github.com/thunderbird/thunderbird-android/tree/main/docs/engineering/adr)，例如 `0002-ui-wrap-material-components-in-atomic-design-system.md`。
  - [`module-structure.md`](https://github.com/thunderbird/thunderbird-android/blob/main/docs/architecture/module-structure.md)：每个模块拆成 **api** 和 **internal**（ADR-0009 把 `:impl` 改名为 `:internal`）。internal 不能被别的模块依赖。
  - [`module-organization.md`](https://github.com/thunderbird/thunderbird-android/blob/main/docs/architecture/module-organization.md)：跨领域只能依赖 `:feature:*:api` 或 `:core:*:api`；依赖单向，不能有环；还有一个 app-composition 组装模块。
  - **这些规则是否有工具强制执行，待确认**：文件树里没找到专门的边界检查任务，`thunderbird.dependency.check` 实际做的是依赖更新检查，见 [该文件](https://github.com/thunderbird/thunderbird-android/blob/main/build-plugin/src/main/kotlin/thunderbird.dependency.check.gradle.kts)。
- **UI**：[`ui-architecture.md`](https://github.com/thunderbird/thunderbird-android/blob/main/docs/architecture/ui-architecture.md) 用改良版 MVI：`State` + `Event`（UI 发起）+ `Effect`（一次性导航等），单向数据流。
- **DI 与检查工具**：Koin（koin-bom 4.2.2），detekt 2.0.0-alpha.6 + compose 规则，ktlint 1.5.0，spotless，dependency-guard 0.5.0，见 [`libs.versions.toml`](https://github.com/thunderbird/thunderbird-android/blob/main/gradle/libs.versions.toml)。detekt 配置在 [`config/detekt/detekt.yml`](https://github.com/thunderbird/thunderbird-android/blob/main/config/detekt/detekt.yml)。
- **CI**：[`quality-checks.yml`](https://github.com/thunderbird/thunderbird-android/blob/main/.github/workflows/quality-checks.yml) 把 `spotlessCheck`、`detekt`（含 KMP 源集）、`dependencyGuard` 拆成三个独立 job。另有 `security-codeql.yml`、`security-scorecard.yml`、`validate-gradle.yml`、`validate-workflows.yml`。
- **分支**：保留了多个 `X.Y-MAINT` 维护分支，说明是长线维护模式，lux 用不到。

### 2.11 element-hq/element-x-android
- **规模和分支**：约 270 个 `build.gradle.kts` 模块，`features/<name>/{api,impl}`（如 [`features/analytics/api`](https://github.com/element-hq/element-x-android/tree/develop/features/analytics/api)）。默认分支是 `develop`，另有 `main`。
- **插件**：放在 [`plugins/src/main/kotlin/`](https://github.com/element-hq/element-x-android/tree/develop/plugins/src/main/kotlin)，有 `io.element.android-compose-application/-compose-library/-library/-root`、`io.element.jvm-library`。
- **SDK 与版本号**：[`plugins/src/main/kotlin/Versions.kt`](https://github.com/element-hq/element-x-android/blob/develop/plugins/src/main/kotlin/Versions.kt) 里 `COMPILE_SDK = 37`、`TARGET_SDK = 37`、`MIN_SDK_FOSS = 24`（企业版 33）。注释说明版本号是类 CalVer 方案：versionCode 形如 `202_501_00a`，最后一位 `a` 是 ABI 编号。
- **DI 与状态**：DI 是 Metro 1.4.5，Konsist 测试里有 `KonsistDiTest`，见 [`libs.versions.toml`](https://github.com/element-hq/element-x-android/blob/develop/gradle/libs.versions.toml)。状态用 `Presenter<State>`，`@Composable fun present(): State`，见 [`AnalyticsOptInPresenter.kt`](https://github.com/element-hq/element-x-android/blob/develop/features/analytics/impl/src/main/kotlin/io/element/android/features/analytics/impl/AnalyticsOptInPresenter.kt)，即 Molecule 风格，在 Compose runtime 里算状态。
- **架构测试（重点）**：[`tests/konsist/`](https://github.com/element-hq/element-x-android/tree/develop/tests/konsist/src/test/kotlin/io/element/android/tests/konsist) 共 16 个测试类。[`KonsistArchitectureTest.kt`](https://github.com/element-hq/element-x-android/blob/develop/tests/konsist/src/test/kotlin/io/element/android/tests/konsist/KonsistArchitectureTest.kt) 断言了这些规则：
  - `XxxState` 的构造参数不许有默认值；
  - `Event` 必须是 sealed interface；
  - Composable 参数里的 sealed interface 必须标 `@Immutable` 或 `@Stable`。
  CI 在 [`quality.yml`](https://github.com/element-hq/element-x-android/blob/develop/.github/workflows/quality.yml) 里跑 `:tests:konsist:testDebugUnitTest`。
- **其他守护**：
  - `tools/check/forbidden_strings_in_code.txt`：禁用字符串列表。
  - [`tools/dependencies/checkDependencies.py`](https://github.com/element-hq/element-x-android/blob/develop/tools/dependencies/checkDependencies.py)：检查依赖到的模块是否存在，并禁止 App 依赖 test 模块。
  - detekt（[`tools/detekt/detekt.yml`](https://github.com/element-hq/element-x-android/blob/develop/tools/detekt/detekt.yml)）、ktlint、kover、`dependencyAnalysis` 插件。
- **PR 模板**：见 [`.github/pull_request_template.md`](https://github.com/element-hq/element-x-android/blob/develop/.github/pull_request_template.md)。

### 2.12 cashapp/zipline（JS 沙箱对照）
- 定位是「在 Kotlin/JVM、Kotlin/Native 里运行 Kotlin/JS 库」，README 第 13 行写明「通过在 Kotlin/JVM 或 Kotlin/Native 程序里嵌入 QuickJS 引擎」，并且会把 JS 预编译成 QuickJS 字节码。宿主和 JS 之间用 Kotlin 接口加序列化做强类型调用（见 [README.md](https://github.com/cashapp/zipline/blob/trunk/README.md)）。
- lux 的音源脚本是第三方写的普通 JS（lx 协议），不是 Kotlin/JS，**所以不能直接用 zipline**。可以借鉴的是：桥接接口强类型、跨边界只传序列化数据、引擎跑在独立线程、加载前校验代码。zipline 是否在 Android 上做内存/CPU 配额，**待确认**（本次没读源码）。
- 补充：LuneLX 的 [`lx/engine/LxScriptEngine.kt`](https://github.com/zhangnannull-debug/LuneLX/blob/main/app/src/main/java/com/lunelx/music/lx/engine/LxScriptEngine.kt)（1,006 行）和 PixelPlayerOLX 的 [`data/lxmusic/LxScriptEngine.kt`](https://github.com/MiNoMinoaa/PixelPlayerOLX/blob/main/app/src/main/java/com/minoppol/music/data/lxmusic/LxScriptEngine.kt)（984 行）做法一样：
  - 都用 `com.whl.quickjs`（quickjs-wrapper）；
  - 每个脚本一个 `HandlerThread("LxScript-<name>")`；
  - 先 evaluate `user-api-preload.js`，再 evaluate 用户脚本；
  - 通过 `callIntoJs(runtime, "request", payload)` 发请求，用 `CompletableDeferred` + `withTimeoutOrNull(EVENT_TIMEOUT_MS = 30_000)` 等结果；
  - JSON 用 Gson。
  - 这和 lux-music-mobile 现有的 [`userApi/JavaScriptThread.java`](https://github.com/JuneDrinleng/lux-music-mobile/blob/master/android/app/src/main/java/cn/lux/music/mobile/userApi/JavaScriptThread.java) 和 [`QuickJS.java`](https://github.com/JuneDrinleng/lux-music-mobile/blob/master/android/app/src/main/java/cn/lux/music/mobile/userApi/QuickJS.java) 思路一致，说明「QuickJS + 每脚本一个线程 + preload」在原生端可行。但这两个项目都是一个 1000 行的大类，不值得照搬。

---

## 3. lux-music-android 应吸收的做法

### 3.1 模块布局（建议）
参考 NiA 的 feature api/impl、Thunderbird 的 api/internal、InnerTune 的纯 JVM 音源模块和 mihon 的 source-api。

```
:app                         # 只做组装：DI 图、导航宿主、Application、Manifest
:core:model                  # 纯 Kotlin（jvm）：Track/Album/Playlist/LyricLine/SourceId 等不可变模型
:core:common                 # 纯 Kotlin：Result/错误类型、Dispatcher 注入、时间
:core:designsystem           # 杂志风 Compose 组件与 token（配自定义 lint，禁止 feature 直接用 Material）
:core:ui                     # 通用 Compose 片段（列表项、封面、空状态）
:core:database               # Room：Entity/DAO（按聚合拆小 DAO），schema 导出
:core:datastore              # 设置（Proto/Preferences DataStore），强类型
:core:network                # OkHttp/ktor 客户端、缓存、UA
:core:data                   # Repository 接口 + 实现（离线优先），对外只暴露接口
:core:testing                # Fake 仓库、测试 Dispatcher、Media3 测试替身
:playback:api                # PlayerController 接口、PlaybackState（StateFlow）、Queue 模型
:playback:service            # MediaLibraryService + ExoPlayer + Callback（独立模块，不依赖任何 feature）
:source:api                  # 音源 SPI（纯 Kotlin）：search/songUrl/lyric/playlist/rank，带版本号
:source:builtin-<平台>       # 内置平台实现（纯 jvm 模块，可直接 JVM 单测），按需逐个加
:source:script               # 自定义脚本音源：QuickJS 宿主 + 沙箱（见 3.6）
:lyrics                      # LRC/逐字歌词解析与对齐（纯 jvm），多歌词源用 Provider 接口，不带 Context
:sync                        # 对接 lux-music-server 的同步客户端（契约见 SYNC-CONTRACT.md）
:feature:<x>:api / :impl     # home / search / player / playlist / library / stats / settings / sources
:lint                        # 自定义 lint 规则（设计系统、禁止 API）
:baselineprofile             # 宏基准与 baseline profile 生成
```
- 依赖规则：
  - `feature:*:impl` 只能依赖 `feature:*:api`、`core:*`、`playback:api`、`source:api`；
  - **feature 之间不许直接依赖 impl**；
  - 只有 `:app` 和 `:playback:service` 能依赖 `playback:service`；
  - `source:builtin-*` 和 `lyrics` 不许依赖 Android（用 `jvmLibrary` 插件强制）。
- 第一阶段别拆太细。参考 Seal、Auxio：两三个人维护的项目模块少也能写好。建议先建 core 的 4~6 个模块、playback 的 2 个和 source:api，feature 等功能真的出现再加。避免 Element 那种 270 个模块的维护成本。

### 3.2 Convention plugins
- 用 `includeBuild("build-logic")` 加类插件，做法和 NiA 一样。建议的插件：`lux.android.application`、`lux.android.library`、`lux.android.library.compose`、`lux.android.feature`、`lux.jvm.library`、`lux.android.room`、`lux.hilt`（或 `lux.metro`，看 3.3）、`lux.android.lint`、`lux.spotless`、`lux.detekt`。
- SDK 版本、JVM toolchain 和 Kotlin 编译选项（`allWarningsAsErrors`、`explicitApi()` 限定在 api 模块）**只在插件里写一次**。
- 放一个 `gradle/libs.versions.toml`。开 `TYPESAFE_PROJECT_ACCESSORS` 和 `FAIL_ON_PROJECT_REPOS`（NiA 的 `settings.gradle.kts`）。build-logic 本身也开 `validatePlugins { failOnWarning = true }`。

### 3.3 DI
- **建议用 Hilt**。NiA、architecture-samples、Pocket Casts、Auxio、Metrolist 都用它；官方文档和 Media3 + ViewModel 集成最成熟；编译期校验依赖图。
- **备选是 Metro**（Element X 已在用，mihon 在迁移）。它编译期生成、对 KMP 友好，但还比较新。
- 不建议 Koin：Seal、Thunderbird 在用，但它是运行时解析，依赖图写错要等运行才暴露，和「用工具守规矩」的目标相悖。
- 待 June 拍板：Hilt 还是 Metro，倾向 Hilt。

### 3.4 UDF 状态管理
- 每个页面一个 ViewModel，暴露 `StateFlow<XxxUiState>`，再加一个 `onEvent(XxxEvent)` 入口。一次性效果（导航、Toast）用 `Effect`。这是 Thunderbird [`ui-architecture.md`](https://github.com/thunderbird/thunderbird-android/blob/main/docs/architecture/ui-architecture.md) 的 State/Event/Effect 写法。
- 规则用 Konsist 测试固化（照 Element 的 [`KonsistArchitectureTest.kt`](https://github.com/element-hq/element-x-android/blob/develop/tests/konsist/src/test/kotlin/io/element/android/tests/konsist/KonsistArchitectureTest.kt)）：
  - `*UiState` 必须是不可变 data class，或标 `@Immutable`；
  - `*Event` 必须是 sealed interface；
  - ViewModel 不许暴露 `MutableStateFlow`；
  - Composable 不许拿 Repository 当参数。
- Element 的 Molecule Presenter 很优雅，但学习成本和调试成本高，第一版不用。

### 3.5 Media3 播放服务结构
- 骨架照 [`DemoPlaybackService.kt`](https://github.com/androidx/media/blob/release/demos/session_service/src/main/java/androidx/media3/demo/session/DemoPlaybackService.kt)：
  - `LuxPlaybackService : MediaLibraryService`，只负责组装和生命周期；
  - ExoPlayer 用 Hilt 提供；
  - 交给框架的事：`setAudioAttributes(..., handleAudioFocus = true)`、`setHandleAudioBecomingNoisy(true)`、通知栏（`DefaultMediaNotificationProvider`）和自定义按钮（`CommandButton` + `setMediaButtonPreferences`）。
- `MediaLibrarySession.Callback` 单独成类，实现 `onGetLibraryRoot/onGetChildren/onAddMediaItems/onSetMediaItems/onPlaybackResumption`：
  - `onAddMediaItems` 负责**按需解析音源 URL**。媒体项里只放 `mediaId`（如 `source:kw:12345`），真正的 URL 在这里通过 `source:api` 拿到，还可以用 `ResolvingDataSource` 在播放时解析和刷新过期链接。这样可以避免 Metrolist 那种 `StreamUrlCache` 和 5000 行服务缠在一起。
  - 播放恢复（`onPlaybackResumption`）从 Room 读队列快照。
- **单一事实来源**：UI 通过 `MediaController` 包装出的 `PlayerController`（`playback:api`）读 `StateFlow<PlaybackState>`，不直接碰 ExoPlayer。Auxio 的 `PlaybackStateManager` 注释也强调状态不能在 playback 模块外直接用。
- **硬约束**：服务类不超过约 300 行（detekt `LargeClass` 和 `LongMethod` 卡住）；队列、睡眠定时、听歌统计埋点、歌词同步各自独立成类，通过 `Player.Listener` 挂上去。
- Manifest 照官方 demo：`foregroundServiceType="mediaPlayback"`、`FOREGROUND_SERVICE_MEDIA_PLAYBACK`、`POST_NOTIFICATIONS`，以及两个 intent-filter action。
- **用官方 Media3 发行版，不 fork**（反例见第 4 节）。版本跟 1.11.x。

### 3.6 自定义音源沙箱
- **引擎**：沿用 lux-music-mobile 已经验证过的 `wang.harlon.quickjs:wrapper-android`，迁移风险最小。实际可用版本和 16KB page size 兼容性**待确认**。
- **宿主结构**：
  - 每个脚本一个专属线程（单线程 `Executor`，包装成 `CoroutineDispatcher`），和 LuneLX、PixelPlayerOLX、mobile 的 `JavaScriptThread` 一样；
  - 先注入固定的 `preload.js`，提供 lx 兼容的 `globalThis.lx` API；
  - 每次调用都有超时（参考它们的 30s，建议更短，可配置），超时就销毁并重建 context。
- **强类型边界**：
  - `source:api` 定义 `MusicSource` 接口（search/musicUrl/lyric/pic……）；
  - `ScriptMusicSource` 实现它，跨 JS 边界只传 kotlinx.serialization 的 JSON DTO，不让 JS 对象泄漏出模块（zipline 的思路）；
  - 内置平台源和脚本源对上层完全等价。
- **加载前检查**（借鉴 mihon 的 [`ExtensionLoader.kt`](https://github.com/mihonapp/mihon/blob/main/app/src/main/java/eu/kanade/tachiyomi/extension/util/ExtensionLoader.kt) 和 [`TrustExtension.kt`](https://github.com/mihonapp/mihon/blob/main/app/src/main/java/eu/kanade/domain/extension/interactor/TrustExtension.kt)）：
  - 解析脚本头部注释的元数据（name/version/author）；
  - 校验支持的 API 版本，不支持就明确拒绝，给出 `NotLoaded.Reason`；
  - 用户导入时记录脚本 **SHA-256**，脚本内容一变就要重新确认（相当于 mihon 的「包名 + versionCode + 签名」）。
- **能力收敛**：
  - JS 只能通过宿主提供的 `request`（OkHttp，可加域名白名单和响应大小上限）、`crypto`（md5/aes/rsa/base64）、`setTimeout` 访问外部；
  - 没有文件和其他 Android API；
  - console 输出进环形日志，供设置页查看。
- **测试**：`source:script` 用 JVM 单测加几份脱敏的示例脚本做协议回归。真机端只跑冒烟测试。

### 3.7 数据层
- **Room**：
  - 每个聚合一个小 DAO（PlaylistDao、HistoryDao、StatsDao……），不要 Metrolist 那种 2000 行的 `DatabaseDao`；
  - `room { schemaDirectory("$projectDir/schemas") }`（Seal 的写法），schema 进 git；
  - CI 跑 `MigrationTestHelper` 校验迁移（对应 mihon 的 `verifySqlDelightMigration`）。
  - 第一版**不导入老数据**，所以版本从 1 开始干净起步。
- 设置用 DataStore，最好用 Proto 强类型（NiA 的 `core:datastore-proto`），**一个设置只存一处**。
- Repository 只暴露接口，实现是 internal。测试统一用 Fake（architecture-samples 的 `FakeTaskRepository`），**禁止 mock Repository**。
- 同步：`:sync` 用 WorkManager，参考 NiA 的 `sync/work`，契约以 server 实际接口为准。

### 3.8 守护规则（用工具，不靠文档）
| 关卡 | 工具 | 出处 |
|---|---|---|
| 格式 | Spotless + ktlint（+ compose-rules），或 ktfmt | NiA、Pocket Casts、Auxio、mihon |
| 静态检查 | detekt（+ compose 规则），零 baseline 起步 | Thunderbird [`config/detekt/detekt.yml`](https://github.com/thunderbird/thunderbird-android/blob/main/config/detekt/detekt.yml)、Element |
| 架构规则 | Konsist 测试（State/Event/ViewModel 规则、包名与模块对应） | Element [`tests/konsist`](https://github.com/element-hq/element-x-android/tree/develop/tests/konsist) |
| 模块依赖方向 | 在 convention plugin 里加白名单校验（如 feature impl 不许依赖别的 feature impl），配置阶段直接失败；再加模块图生成 | NiA [`Graph.kt`](https://github.com/android/nowinandroid/blob/main/build-logic/convention/src/main/kotlin/com/google/samples/apps/nowinandroid/Graph.kt)；Thunderbird 只有文档规则（工具强制待确认），我们要用代码强制 |
| 依赖变化可见 | Dependency Guard 基线（`releaseRuntimeClasspath`） | NiA、Thunderbird |
| 依赖健康 | dependency-analysis 插件（未用、误用 api） | Element 根 [`build.gradle.kts`](https://github.com/element-hq/element-x-android/blob/develop/build.gradle.kts) |
| 设计系统 | 自定义 lint：feature 禁止直接用 `androidx.compose.material3.*` 组件 | NiA [`DesignSystemDetector.kt`](https://github.com/android/nowinandroid/blob/main/lint/src/main/kotlin/com/google/samples/apps/nowinandroid/lint/designsystem/DesignSystemDetector.kt) |
| Android Lint | `checkDependencies`，输出 xml 和 SARIF（NiA 已有）；lux 再加 `warningsAsErrors`（这条是我们的建议，NiA 里没核实到） | NiA `AndroidLintConventionPlugin.kt` |
| APK 内容 | badging 检查（权限和 SDK 不被意外改动） | NiA `checkProdReleaseBadging`、Thunderbird badging 插件 |
| 禁用写法 | 禁用字符串或正则列表。Element 的列表里有 `import static`、冲突标记 `<<<<<<<`、常见拼写错误；lux 可以加 `GlobalScope`、`runBlocking` 等 | Element [`tools/check/forbidden_strings_in_code.txt`](https://github.com/element-hq/element-x-android/blob/develop/tools/check/forbidden_strings_in_code.txt) |
| 供应链 | action 钉 SHA，Gradle wrapper 校验，dependency-review | mihon [`build.yml`](https://github.com/mihonapp/mihon/blob/main/.github/workflows/build.yml)、Pocket Casts |

### 3.9 CI 与发版流程
- **PR 检查**（`pull_request` 到 dev 或 main，`concurrency: cancel-in-progress`），按 Thunderbird 的做法拆成多个并行 job，方便定位：
  1. `build-logic:check`（插件代码本身）
  2. `spotlessCheck` + `detekt`
  3. `lint`（SARIF 上传）
  4. `test`（JVM 单测 + Konsist + Room 迁移测试）
  5. `dependencyGuard` + 模块规则
  6. `assembleDebug` 并上传 APK artifact
  - 每个 job 都设 `timeout-minutes`。前期不要像 NiA 那样在 CI 里自动提交基线，改为直接失败，并提示本地运行哪条命令。
- **dev 开发版**：push 到 dev 后自动发 `vX.Y.Z-dev.N` 预发布，沿用 mobile 的做法。
- **稳定版**：dev 转正合进 main 后，按 version catalog 或 `version.properties` 打 `vX.Y.Z` tag，发签名的 Release。
  - ABI 分包：`arm64-v8a`、`armeabi-v7a`、universal（`splits.abi`，mihon 的写法）；x86 和 x86_64 看需要。
  - 每个 APK 附 SHA-256 和 CHANGELOG 段落。
  - 签名：secrets 存 base64 的 keystore 和三个密码，CI 解码到临时文件，**签名配置只在 CI 环境变量存在时启用**（Gramophone、Metrolist 的写法）。**不用浮动版本的第三方签名 action**，用 AGP 自带签名或钉 SHA 的 action。
- **版本号**：版本名统一来源（Seal 的 `sealed class Version` 加 `check()` 一致性校验）。versionCode 用 `major*10^6 + minor*10^4 + patch*10^2 + build`，按 ABI 加偏移，确保 dev.N 小于对应的稳定版并且单调递增。具体公式在发版文档里写死，**待确认**。
- **baseline profile**：放第 0 阶段之后做，参考 NiA 的 nightly workflow 和 Gramophone 的 `:baselineprofile` 模块。用 GMD 生成，结果进 git。
- **PR 模板**：综合 Pocket Casts 和 Auxio。改动类型；测试步骤；截图；勾选项包括「已运行 `./gradlew check`」「UI 改动已在深色模式和大字体下检查」「字符串已放进资源」「没有新增兼容分支」。

### 3.10 minSdk 选择
- 参照项目的值：Gramophone 23，NiA、Pocket Casts、Auxio、Seal、Thunderbird、Element 都是 24，Metrolist、mihon 是 26，PixelPlayer 是 30。Media3 自身支持 23。
- **建议 minSdk 26（Android 8.0）**：
  - `java.time` 原生可用，不用 desugaring；
  - 通知渠道、自适应图标、`startForegroundService` 都是基线能力，可以去掉大量 `if (SDK_INT >= O)` 分支；
  - mihon 和 Metrolist 这样的现役项目也选 26。
- 想更激进可以选 **29**（分区存储、系统深色模式都是基线），代价是放弃 Android 8–9 设备。**待 June 根据自己和用户的设备分布决定。**
- targetSdk 和 compileSdk 跟最新：多数参照项目是 compile 37 / target 36，Element 和 Auxio 已经 target 37。

---

## 4. 不建议照搬的

| 做法 | 出处 | 原因 |
|---|---|---|
| fork media3 并作为子模块用源码编译 | Auxio [`.gitmodules`](https://github.com/OxygenCobalt/Auxio/blob/dev/.gitmodules)、Gramophone [`settings.gradle.kts`](https://github.com/FoedusProgramme/Gramophone/blob/beta/settings.gradle.kts) | 升级要手工 rebase，构建变慢，CI 还要 `submodules: recursive` 和 ninja。lux 没有 HiFi/USB DAC 这类需求，官方 Media3 足够 |
| 巨型播放服务和巨型 DAO | Metrolist `MusicService.kt`（5,080 行）、`DatabaseDao.kt`（2,046 行）；Gramophone 服务 1,994 行；Pocket Casts `PlaybackManager.kt`（3,242 行） | 正是 June 想摆脱的「冗余和包袱」。用 detekt 的类和函数长度上限卡住 |
| 新旧播放服务并存，用开关切换 | Pocket Casts [`PlaybackServiceToggle.kt`](https://github.com/Automattic/pocket-casts-android/blob/main/modules/services/repositories/src/main/java/au/com/shiftyjelly/pocketcasts/repositories/playback/PlaybackServiceToggle.kt) | 那是迁移期的无奈之举。新仓库从第一天就只用 `MediaLibraryService` |
| `MediaBrowserServiceCompat` | Auxio [`AuxioService.kt`](https://github.com/OxygenCobalt/Auxio/blob/dev/app/src/main/java/org/oxycblt/auxio/AuxioService.kt) | 是旧版 androidx.media 的 API，Media3 已经提供了替代 |
| 新旧两套歌词 View 并存 | Gramophone `LyricsView` / `NewLyricsView` / `LegacyLyricsAdapter` | 只保留一套实现 |
| 歌词或音源接口带 `Context` | Metrolist [`LyricsProvider.kt`](https://github.com/MetrolistGroup/Metrolist/blob/main/app/src/main/kotlin/com/metrolist/music/lyrics/LyricsProvider.kt) | 这样接口模块就不能是纯 JVM，也没法直接单测。改为构造时注入依赖 |
| 270 个模块、Molecule Presenter、Appyx 导航 | Element X | 对小团队维护成本太高，第一版按需拆模块，用 ViewModel + StateFlow |
| 运行时 DI（Koin、Injekt） | Seal、Thunderbird、mihon（老部分） | 依赖图错误要等运行才暴露 |
| Buildkite 等外部 CI | Pocket Casts | lux 用 GitHub Actions 就够了 |
| 不再维护或浮动版本的 action | NiA `actions/create-release@v1`；Seal `ilharp/sign-android-release@nightly` | 安全和可复现性差，改用钉 SHA 的 action，比如 `softprops/action-gh-release` |
| CI 自动往 PR 推基线和截图提交 | NiA `Build.yaml` 的 git-auto-commit 步骤 | 会给 Codex 和人工 review 制造噪音，改成失败并提示本地命令 |
| 多个 `X.Y-MAINT` 长线维护分支 | Thunderbird | lux 只用 main 加 dev 双通道 |
| 多 flavor（foss/gms/izzy）、多签名、企业版 | Metrolist、Gramophone、Element | 第一版只要一个 flavor。vivo 版之类的需求出现再说，到时候也用 convention plugin 统一管理 |
| 照抄 1000 行的 `LxScriptEngine` | LuneLX、PixelPlayerOLX | 可以证明可行，但结构是单个大类加 Gson。按 3.6 拆成「引擎宿主 / 协议适配 / 能力桥 / 加载校验」四部分 |
| 复制 GPL 或 AGPL 代码 | Auxio、Gramophone、Metrolist、Seal（GPL-3.0）、Element（AGPL-3.0） | 许可证会传染。只学思路，不拷代码 |

---

## 5. 待确认清单
1. mihon 的 DI 从 Injekt 迁到 Metro 的进度（toml 里两个都有，`ExtensionLoader` 已经在用 `appGraph`）。
2. Thunderbird 的 api/internal 边界规则有没有工具强制执行（本次只确认了文档）。
3. Gramophone 的 DI 方式（`app/build.gradle.kts` 里没看到 Hilt/Koin）。
4. quickjs-wrapper 最新可用版本，以及它对 16KB page size 和 arm64 的兼容情况。
5. zipline 在 Android 上有没有内存和 CPU 配额机制（只读了 README 定位）。
6. lux 的 versionCode 公式和 minSdk 最终取值（要 June 决定）。
