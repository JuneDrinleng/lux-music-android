# 05 · 分支、PR、Actions 与发版研究

> 决策更新（2026-11-14）：本文保留原始研究结论与建议，不是当前实施规范。已确定先 LX、后 Lux；SDK 33/36、Hilt、dev/main、同包名开发/稳定通道、独立本机资料。最新要求见 [plan.md](../plan.md)，与历史建议冲突时以最新计划为准。

这是流程建议，尚未创建分支规则、PR、workflow、secrets 或 Release。对照的源码为 [mobile、NIA、Metrolist、Seal、Jellyfin 工作流](sources.md)；规则依据为 [GitHub protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)、[Actions secure use](https://docs.github.com/en/actions/reference/security/secure-use)。

## 从样本学到什么

| 观察 | 采用的思想 | 不直接采用 |
| --- | --- | --- |
| mobile dev/master push 自动发两类版本，构建 standard/vivo、多 ABI，并回写 version.json；提前 tag 占号和补发有明确语义 | 发布不可覆盖、失败可恢复、签名提前校验 | 不复制双长期分支、十 APK、跨分支回写及 RN 变体；业务提交不必等于对外发版 |
| mobile PR 的全量 lint/typecheck 为非阻断 | 保留增量检查有时有价值 | 新项目没有历史理由把核心检查永久 `continue-on-error` |
| NIA Build 与 Release 分开，Release 按 v*；有截图回归及基准 profile | 验证与打包分离、截图有实际用途 | 不照搬样例里自动录制/回写 baseline 的策略；变化需要人工审阅，不能失败就接受新图 |
| Metrolist workflow 注释明确不在 feature push 重复执行 PR 构建，不在 nightly 构建所有小差异变体 | 减少重复任务和矩阵 | 不照搬第三方 runner、全部 GMS/FOSS/Izzy 命名及权限 |
| Seal 分 PR CI 和手动签名；Jellyfin 按工作流区分 build/test/publish、Actions 用完整 SHA | 签名和 PR 隔离、最小权限、可追溯 action | 不采用浮动 nightly action、把所有 workflow 当可直接复制模板 |

## 推荐分支模型

**main 始终可构建，短分支经 PR 合入，版本由 tag 标识。**

| 分支 / tag | 用途 | 合并 / 发布规则 |
| --- | --- | --- |
| `main` | 唯一日常集成线 | PR + 通过检查；默认 squash，合并后删短分支 |
| `feat/*`, `fix/*`, `refactor/*`, `docs/*` | 单一可审阅改动 | 从 main 创建，PR 回 main，避免长期堆积 |
| `release/0.x` | 仅需要支持旧稳定版本时建立 | 不是每版必建；挑选修复，回移 main，明确停止维护时间 |
| `v0.x.y-beta.n` | 固定候选提交、GitHub prerelease | 来源应已通过 main 或维护线检查；不能自动成为 stable latest |
| `v0.x.y` | 固定稳定提交 | 从有验证记录的提交创建、签名构建与发布 |

不推荐默认引入 dev/develop + main + release 三条长期线。若以后确实有独立稳定维护团队，再用事实重新决策，不为“可能”提前保留冗余。

hotfix：从受支持稳定 tag 建短修复分支，PR 到维护线（没有维护线则 main），同时回移主线；不能直接在发行分支修好后忘掉 main。禁止 rebase/move 已发 tag。

## PR 约定

- 一个 PR 解决一个可说明的问题，含问题、行为变化、影响页面/协议/数据、验证、截图或录屏、迁移/恢复说明。不要按“改动行数”机械拆开同一个事务。
- 协议变更须说明影响 LX/Lux 哪个能力，并附固定 server fixtures 或联调记录；不修改 server。账号变更需覆盖切换/离线/取消和数据 owner。
- UI PR 对照 `ui/`/design.md；账号新流程先提交设计基准差异，不混入架构 PR。
- main/维护分支禁止 force push；检查名稳定；至少一次 review（单维护者时可用明确自审记录，但不假装已有外部 reviewer）；新提交撤销过期审批；保护项是否可用以账户/仓库配置为准。
- CODEOWNERS 可按协议/播放/UI/发布列角色，实际 owner 用已确认成员，不虚构其他用户。发布签名和 workflow 变更单独审阅。
- 不把“合并权限”“通过 CI”“允许对外发布”混为一个步骤；流程自动构建，但业务变更不自动覆盖 stable Release。

## Actions 建议职责

以下名字只是拟议 workflow/check，不是已创建配置。

| 工作流 | 触发 | 工作与产物 | 权限 / 密钥 |
| --- | --- | --- | --- |
| `verify` | PR；main push；需要时 merge_group | wrapper/dependency verification、format/lint、单测、debug APK；固定工具链；保存报告 | contents:read，无签名密钥 |
| `contract` | PR 的协议/数据/账号改动，main，定时 | golden vectors、只读 fixtures；可在隔离 CI 启动固定提交未修改 server 做集成 | 临时测试身份，不用真实服务账号；PR 无外部私密凭据 |
| `ui-regression` | UI PR、main | deterministic fixture、固定设备字体/视口截图、导航关键流 | 新 baseline 必须在 PR 审阅；不失败就自动接受 |
| `device-smoke` | 关键 PR、main 或定时 | 模拟器启动、播放/通知/返回/进程恢复/账号切换；nightly 扩展设备矩阵 | PR 默认无 secrets；报告和录屏 |
| `snapshot` | main 有代码变更时定时或手动 | debug/开发快照 APK，含 SHA，不创建正式 Release | 不接生产签名；artifact 保留期限 |
| `release` | 受保护 tag 或手动指定已有 tag | checkout 精确 SHA、校验版本、复跑必要门禁、release 构建、签名校验、checksums、draft→发布 | 最小 contents:write 仅发布 job；受保护 release environment 中的签名材料 |

CI 默认不连用户的实际 Lux/LX 部署。集成容器/进程使用固定源码快照及测试配置，不能改 server handler 来适配 Android。

聚合 `required-checks` 必须总运行；纯文档 PR 的跳过通过它说明“无需 app build”，避免路径过滤导致必需 check 永久 pending。初期全部 Kotlin 改动跑完整低成本检查，等测到构建瓶颈再做依赖图增量，不先造复杂 paths-filter 系统。

PR 并发按 PR/ref 取消旧运行；发布队列按通道串行，不取消正在签名/上传的旧发布。测试失败或 artifact 缺失应阻断，不 fallback 到空包/旧包。

## 包名、通道和 ABI

建议 stable 与 preview 两个安装身份，debug 为第三个开发身份；最终 applicationId 需用户确定，不能默认复用 RN `cn.lux.music.mobile`。新 app 默认不覆盖旧 RN，除非明确选定相同签名/迁移路线。

- stable：GitHub 正式 Release，正式签名；如将来 Play 分发再产 AAB。
- beta：GitHub prerelease、preview applicationId，可与 stable 并存，单独缓存/资料，不共享可变数据库。
- main snapshot：debug/开发签名 artifact，不承诺与 beta 连续覆盖升级；若以后需要可自动更新的 nightly，再定义独立发布计数与签名，不在第一阶段额外维护一整条对外通道。

优先 universal APK（兼顾选定架构），模拟器使用 x86_64，真实主要设备 arm64。JS runtime/native decoder 会影响 ABI，需原型验证后再决定 armv7 是否保留。只有包体/下载量测量证明需要才 split，不先复制十个 APK 和厂商 flavor。

## 版本与签名

研究建议：仓库单一版本源（例如将来的 version.properties），`versionName` 为 SemVer，**发布 `versionCode` 为显式分配的递增整数**。发布 PR 分配下一个号并记录 beta/stable/目标 SHA；不根据 Git 提交数、分支名称或 GitHub 页面顺序猜测升级大小。

beta/stable 均消耗发布序号（即使不同包名也可统一分配以简化审计），重新构建同一个 tag 重用该 tag 的号。失败 tag 不移动；修业务代码须新 tag/新号。main debug snapshot 的 run_number 仅标识开发包，不与发布序号混用。

tag 与 versionName/通道不匹配直接失败；检查 versionCode 上限、已发布记录及构建输出 APK 实际 manifest；不要只检查配置文本。不要在生成 APK 后再注入会改变源码身份的版本/日志，尽量让版本、说明和工具链都在 tag 中可重建。

生产 key 只存在受保护环境和维护者备份；PR/fork 不可触达。若未配置 signing，release 明确失败，**不能回落 debug 签名并叫正式包**。key alias/cert fingerprint 与 APK 通过 apksigner 校验，checksums 随 Release 保存。远程操作与实际环境保护/审批由实施时配置，本轮不要求用户配置 secrets。

## 发布顺序与失败恢复

```text
release PR（版本 / 说明 / 验证记录）
 → main merge
 → 创建不可变 beta/stable tag
 → 校验 tag/版本/来源/所需检查
 → 精确提交构建 + 签名 + verify
 → 所有预期文件齐全，创建/补全 draft Release
 → 发布并标记 prerelease 或 stable
```

- 上传失败：重跑相同 tag 的发布阶段，识别已有同 hash artifact，不重复发布版本；不同 hash 不静默覆盖。
- 构建失败：保留失败记录，修工具问题可重跑；修源码要新 tag。不能“重推同名 tag”隐藏历史。
- beta 不更新 stable latest；stable 发布完成后才更新相关更新信息。不回写另一个分支制造发布循环。
- 不能假设 GITHUB_TOKEN 推的 tag 会触发另一条普通 push 工作流。若未来自动创建 tag，应由同一 orchestration 调 reusable release，或显式 dispatch；不能设计依赖这一隐式触发的链。
- 回滚发行页不等于 Android 可降级；通常发布更高 versionCode 的修复版本，数据库迁移向前兼容需提前设计。
- 能追溯提交/工具链不等于已验证二进制可复现；签名时间戳、依赖和原生库影响需要后续比较构建产物才能判断。

## 工作流实现时的约束

Actions 固定完整 SHA 并由依赖更新 PR 维护；固定 wrapper/JDK/SDK/NDK，缓存键含工具链和锁文件；验 wrapper 与依赖校验。PR job `contents:read`，不要用 `pull_request_target` checkout 未信任 PR 后接触 secrets；发布只拿必要权限。日志不输出 keystore、token、WS query `t`、连接码或下载签名 URL。

本节用于确定成本和责任，不是对样本项目下安全结论；没有访问或审计任何真实仓库密钥。
