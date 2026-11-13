# 分支、PR、验证与发布流程（t435 提案）

2026-11-13，待 June 批准启用。本 PR **只写方案**：不建 dev、不加 workflow/模板/标签、不设保护、不配 secrets、不打 tag、不发版。落地项进入 [ROADMAP 第 0 阶段](ROADMAP.md)，必须另开实施 PR；仓库设置与秘密值由 June 亲手配置。本次 PR #1 仍以 main 为 base，是 dev 建立前的文档引导，不据此允许后续直接推主分支。

## 真实先例与选择

mobile 固定提交 `3ab131e141ccbdc807d200a99eb216a17017c8c8` 的 [release.yml](https://github.com/JuneDrinleng/lux-music-mobile/blob/3ab131e141ccbdc807d200a99eb216a17017c8c8/.github/workflows/release.yml) 实际有三个 job：

| mobile job | 已读行为 | 原生方案 |
| --- | --- | --- |
| `plan` | 计算版本、检查签名配置、先占 tag；脚本为 [next-version.mjs](https://github.com/JuneDrinleng/lux-music-mobile/blob/3ab131e141ccbdc807d200a99eb216a17017c8c8/scripts/next-version.mjs) | 用 Kotlin/Gradle 单一版本模型与串行占号；不引入 Node/package.json/dev-target 历史特例 |
| `build` | checkout tag、构建签名 APK、按 standard/vivo 与 ABI 收集产物、生成 MD5、创建 Release | 一个应用标识/一个产品配置；AGP 签名，验证证书、APK 内容和 SHA-256；不复制渠道矩阵与构建失败时倾倒完整 daemon 日志 |
| `feed` | build 成功后回写 master 的 publish/version.json；含清理旧预发布的步骤 | 只在已发布资产验证后更新托管 feed；不回写 main/dev，不自动删除 tag/Release |

[mobile ci.yml](https://github.com/JuneDrinleng/lux-music-mobile/blob/3ab131e141ccbdc807d200a99eb216a17017c8c8/.github/workflows/ci.yml) 的 RN bundle、Node 检查不是本仓库所需。mobile 的[转正提交 cd10e37e](https://github.com/JuneDrinleng/lux-music-mobile/commit/cd10e37e) 是有两个父提交的 `Merge pull request #176 ... codex/promote-v0.5.4`，证明有 merge commit 先例；它不是 dev→master 直接 PR 的证明。本项目明确选用下面的 dev→main 流程，不编造先例。

## 分支、保护与合入

- **main 稳定、dev 集成、codex/* 或 cursor/* 功能分支**。功能从 dev 起步，小 PR 到 dev；dev→main 为转正 PR。只有已授权且已用完的 README 初始提交曾直接推 main，之后 main/dev 都只走 PR，机器人也无 source 分支直写例外。
- 功能 PR 合入 dev 用 **squash**；dev→main 转正用 **merge commit**，保留被验证的 dev 提交可追踪性，不 squash 或 rebase 转正。平台仓库设置通常不能仅靠一个全局开关按目标分支强制两种方式，需合入前校验 PR 来源/目标与允许方法，June 按约定选择，后续可加专门检查；当前不声称已强制。
- June 建议为 main/dev 启用 ruleset/branch protection：必须 PR、至少一名批准者、更新后旧批准失效、讨论须解决、必需检查全通过、分支合入前更新或 merge queue 验证、禁止 force push/删除、限制直接更新并使管理员遵守。main 另要求来源 dev 且完成转正清单。自动化不得绕过 source 分支保护；批准者/计划可用性由 June 核对，不创建虚假保护状态。
- **main 不启用 require linear history**，否则会阻止所选 merge commit。dev 的 squash 规则也要与所选 merge queue 支持的方法一致；最初可手工合入，不先引入 merge queue。不得为发布给 bot 添加分支保护 bypass。
- 引导顺序：June 批准文档与实施范围 → dev 从批准后的 main 建立（另行授权执行）→ 首个工具链/CI PR 注册检查 → June 选择实际生成的检查名并启用保护 → 验证无直接推送旁路。避免把尚不存在的 check 设为 required 导致首个 PR 无法通过。

## 小 PR、模板与标签（仅列规范）

每 PR 一项可验收行为，架构变化附依赖图/ADR。建议后续模板含：问题和结果、改动范围、关联决定、验证命令/证据、失败与恢复行为、协议/数据迁移影响、无兼容 shim/双数据源、已定 minSdk 33/targetSdk 36 未回退。视觉相关变更由开发 Bot 附 June 批准的原型及交互验收证据，Codex 不写视觉规范。

建议标签：`type:docs`、`type:feature`、`type:fix`、`type:architecture`、`type:release`、`area:auth`、`area:sync`、`area:playback`、`needs-june`。标签用于分拣，不代替批准；后续 PR 提供模板内容，June 决定并创建仓库标签。本任务不添加模板或标签。

## PR CI：只验证，不接触签名

以下是拟定 job/check 名称，实际 Gradle task 在骨架 PR 锁定后映射，不假装仓库已有任务：

| 必需 check | 内容与失败条件 |
| --- | --- |
| `docs` | Markdown 相对链接/外部来源核验、文档范围；纯文档改动可跳过 Android 重构建，但 check 返回明确成功/失败，避免 required check 永久 pending |
| `build-logic` | Wrapper 校验、约定插件自身 check、依赖仓库与锁定工具链；action 固定提交 SHA |
| `format-static` | ktlint（通过 Spotless 或直接任务二选一）与 detekt、Compose 规则，零 baseline 起步，不在 CI 自动推格式或基线 |
| `lint` | Android Lint，NewApi/ObsoleteSdkInt/弃用 API 与违规 suppress；APK badging 校验 SDK、包名和权限变化 |
| `architecture-deps` | Gradle 依赖白名单/无环、Konsist 声明规则、Dependency Guard 差异审查；拒绝 UI 引用 DAO/实现、公开 Any/JSON |
| `unit-contract` | JVM 领域/Fake 测试、Room 事务/真实迁移、协议 fixture、账号刷新与跨账号隔离；不能只用 mock 声称集成正确 |
| `debug-apk` | 编译/构建 debug APK，上传限期 artifact 供人工验收；验证 QuickJS native ABI/16KB 页兼容；不使用发布签名 |

API 33 与 targetSdk 36 对应系统的 instrumentation 作为功能 PR 的补充门禁，播放/账号/数据库改动按路径要求；测试环境与耗时在骨架 PR 确认。截图测试**先不设为第 0 阶段强制项**：视觉规范未选定，先通过 Web 原型验收行为；开发 Bot 的设计稳定后，为关键原生组件引入固定字体/系统/时区的截图基线，PR 人工审核差异，CI 不自动回写。截图不能替代 TalkBack/后台播放与账号切换验证。baseline profile 放性能阶段另 PR。

仅 `pull_request` 验证可信合入结果，默认 contents:read，每 job 有超时和缓存边界；PR CI 可 cancel-in-progress。fork/不可信 PR 不得获得 secrets，不用 pull_request_target checkout 不可信 head 执行代码。发布写权限只授予对应发布 job，不授予整个 PR workflow；供应链和依赖更新须可审查。

## 版本模型（建议锁定的公式）

未来 `version.properties` **只保存目标稳定版本 X.Y.Z**，不同时在 app/脚本/feed 手写版本。实现用 Gradle/Kotlin 解析，versionName/tag/feed 由一个 Version 值派生；当前不创建该文件。dev 以该目标生成 X.Y.Z-dev.N，稳定为 X.Y.Z。转正前 PR 确认目标版本/CHANGELOG，稳定发布后下一目标的变更以小 PR squash 入 dev；不要求自动回写主分支。

参考 [mobile version-code.mjs](https://github.com/JuneDrinleng/lux-music-mobile/blob/3ab131e141ccbdc807d200a99eb216a17017c8c8/scripts/version-code.mjs) 的有限范围编码思路，原生建议：

- `slot = N`（dev：1..98），稳定 `slot = 99`。
- `base = ((major * 100 + minor) * 100 + patch) * 100 + slot`。
- `versionCode = base * 10 + abiId`，universal=0、armeabi-v7a=1、arm64-v8a=2；如将来增加 x86_64，预留 3。它们是**原生建议映射**，不是 mobile ABI 编号。
- major=0..209，minor/patch=0..99，且最终数值主动采用 Google Play 的 2100000000 上限；先做溢出校验。N 用该目标下已有 tag 最大号+1，包含失败构建已占号，不能重用；到 98 时必须转正或提高目标版本，不溢出进稳定 slot。
- 示例：0.1.0-dev.1 universal=100010，arm64=100012；0.1.0 稳定 universal=100990；0.1.1-dev.1 universal=101010。后一 slot 的最低 ABI 编号高于前一 slot 的最大编号，跨通道升级不倒退。
- 相同 version/tag 只能对应同一 SHA、相同签名/构建输入；不可移动 tag、覆盖已发资产或修改旧 versionCode。相同 SHA 的重跑恢复同一占号，若资产已发布则核验并跳过；失败号留存。不自动删旧 tag/Release。
- CI 同时校验语义排序、各 ABI 编码唯一、dev 小于对应稳定、跨 patch/minor/major 单调、N 上限和溢出；不能把 `github.run_number` 当作可重试的唯一版本来源。

Google Play versionCode 上限的官方依据见 [应用版本](https://developer.android.com/studio/publish/versioning)。上述公式为方案，不是实现承诺；June 批准后用边界向量固定。

## dev 与稳定发布流水线（拟定 job）

1. **触发与验源**：经 PR 合入 dev 的 push 启动开发版；dev→main 转正 PR 合入后的 push 启动稳定版。拟 `release-plan` 验证来源、完整检查、版本和 changelog；初始文档/骨架期没有可发 APK，显式跳过，不能每次文档 push 都造空 Release。仅 tag push 不再次构建，避免自动 tag 触发循环。分支/路径过滤须在 checks 中可解释。
2. **串行占号**：dev 自动选择 `vX.Y.Z-dev.N`，main 按目标 X.Y.Z 选择 `vX.Y.Z`；使用单一 release concurrency、cancel-in-progress:false，防止 dev/stable/feed 竞态。占 tag 前确认版本大于已发版本；稳定需来自获批转正，main 不因版本没变而重发。最终检查若发现当前 tag 已归属另一个 SHA，则失败，不 force。占号和 tag 创建只在未来已授权的发布 job 执行。
3. **构建与签名**：`release-build` checkout 已核验 SHA/tag，重跑必要检查，AGP 生成 release APK；`release-sign-verify` 使用受保护 GitHub Environment 的 secrets 进行签名、apksigner 校验证书指纹和签名、核对 applicationId/min/target/versionCode。首批建议 arm64-v8a、armeabi-v7a、universal；只有 QuickJS 所选绑定实际支持的 ABI 才能列入，任何缺失必须阻塞并回到 ADR，不能上传空包。x86_64 先作模拟器验证，是否发包待需要出现再评审。一个产品配置，不引入 RN/vivo/flavor 包袱。
4. **发布**：`release-publish` 先创建 draft，上传全部经过签名验证的 APK、SHA256SUMS、发布清单（版本/源码 SHA/ABI/文件名/大小/哈希/证书指纹）和中文 changelog；全部资产核验后再公开。dev 标 prerelease、latest=false，稳定公开为正式最新。稳定说明必须有该版本 CHANGELOG 段落；dev 从上次开发版至本 SHA 的已合并 PR 生成并保留来源。发布权限与环境批准由 June 配置；本 PR 不执行。
5. **feed**：`release-feed` 仅在公开 Release 成功、资产可访问且清单一致后更新 version.json，见下一节。发布失败不移动 feed；feed 失败可对同一不可变 Release 单独重跑，不重打 APK。新稳定版出现后，dev 不得继续发布低于该稳定版的目标。

与 mobile 不同：不使用不透明第三方签名 action、不输出密钥/完整参数/daemon 日志、不自动向 main/dev 推提交、不删历史、不以缺少说明为理由发布难以追溯的稳定包。GitHub Actions 具体 YAML 在第 0 阶段另 PR 评审，所有 action 固定审过的 SHA。

## Secrets 与 June 配置

下列只写**建议名称与用途**，不创建、索取或保存任何值：

| 名称 | 用途 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | June 离线生成并保管的发布 keystore 编码；base64 不是加密；未来 runner 临时文件解码并限制权限，用后清理 |
| `ANDROID_KEYSTORE_PASSWORD` | 打开 keystore |
| `ANDROID_KEY_ALIAS` | 选择签名条目 |
| `ANDROID_KEY_PASSWORD` | 使用该签名私钥 |

证书 SHA-256 指纹是公开核验信息，可用非敏感 repository/environment variable `ANDROID_SIGNING_CERT_SHA256`；keystore 文件名固定为 runner 临时路径，不增加一个“文件名 secret”。源码中不含签名文件或本地凭据模板值。环境只允许获批的 dev/main 发布任务取得签名材料，PR/debug 绝不使用。是否共用 dev/stable 证书建议共用，以支持原地升级；若未来分应用安装才另案使用 applicationId 后缀与独立 feed，不预建多签名。

`GITHUB_TOKEN` 是 Actions 提供的运行期短效身份，不要求 June 复制 token 到 secret。发布 job 需要受限 contents:write 来建 tag/Release；feed Pages 部署 job 单独需 pages:write/id-token:write。代码验证仍为 read-only。不得为了 feed 绕过 main/dev 保护，也不建议新增长期 PAT。

## 应用内更新 feed

mobile 的 [scripts/update-feed.mjs](https://github.com/JuneDrinleng/lux-music-mobile/blob/3ab131e141ccbdc807d200a99eb216a17017c8c8/scripts/update-feed.mjs) 保持顶层 version/desc/history 为稳定版，另有 dev；[src/utils/version.js](https://github.com/JuneDrinleng/lux-music-mobile/blob/3ab131e141ccbdc807d200a99eb216a17017c8c8/src/utils/version.js) 拉 `publish/version.json` 并按 ABI 选择包。原生只复用“双通道元数据”的思路，不沿用旧 App 顶层兼容结构、代理列表、RN 下载器或 master 回写。

建议原生 feed 为版本化 schema：`schemaVersion`，`stable`/`dev` 两个可空频道；每频道含 version/tag/sourceSha/publishedAt/changelog 与 assets（abi、versionCode、url、sha256、sizeBytes）。这些是**新提案字段**，不是现有 mobile/server API；下载地址来自本仓库 Release，不能拼出未核验资产。客户端只解析显式 DTO，稳定用户不自动升级到开发版，启用开发通道须明确选择。

建议托管为 **GitHub Pages 的部署 artifact**，URL 由 June 启用 Pages 后确定（当前不存在可承诺 URL）；不创建 gh-pages 或 feed 源码分支，不修改 main/dev。发布成功时从本仓库已公开 Release 的不可变发布清单重建两个频道及有限历史，再部署 `version.json`，不依赖 runner 本地状态；单一发布锁串行更新，防止旧任务覆盖新频道。Pages 是否可用、环境/域名与缓存策略由 June 亲手核定。若不启用 Pages，改用 June 选择的静态 HTTPS 托管，另案评审；不悄悄回退为 bot 推 main。

原生更新流程检查 schema、包名/签名渠道、支持 ABI、较高 versionCode、SHA-256 与证书一致性，再交给系统安装器并由用户确认。checksum 不能替代签名信任，feed 失败不影响播放/离线；未知 schema 要保留当前版本并给出中文错误。安装权限、后台下载与重试另功能 PR 实现，当前没有更新器代码。

## June 亲手做的清单与启用条件

- 批准本提案与第 0 阶段实施顺序，决定何时授权建立 dev、首个版本和频道是否共用签名；功能 PR 不能自行启用发布。
- 自己生成、备份 keystore，向受保护 GitHub Environment 配置上述 secrets 与公开证书指纹；不向机器人提供值。验证密钥恢复与签名一致性。
- 亲手设 main/dev 保护规则、必需检查、批准者、合入方式及 Actions 权限；禁止 direct push/force/delete/bypass。检查所用 GitHub 计划支持哪些规则，缺项显式记录替代门禁。
- 配置发布 Environment 的部署分支/审批、Pages Actions artifact 托管与域名（或选择另一静态托管），确认 feed URL/缓存和发布所需最小权限。
- 确认标签命名、版本公式边界、ABI 选择与首版 changelog；手动批准首次签名发布演练，再批准自动 dev 预发布。后续发布授权不从本次文档任务推定。

执行证据由后续 PR 展示：fork PR 拿不到 secrets，四类架构违规被拒，dev squash 与转正 merge commit 可追踪，签名/ABI/checksum/feed 同源，失败与重跑不覆盖历史。本 PR 不将这些写成“已完成”。
