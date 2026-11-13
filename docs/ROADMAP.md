# 原生安卓路线图

状态：提案。优先顺序：架构定案 → 开发 Bot 选设计规范与 Web 原型 → June 签字 → Android 骨架/门禁及发布流程实施 → 功能。[ARCHITECTURE](ARCHITECTURE.md) 是本 PR 核心；mobile 仅作[功能对照](PARITY.md)。本次交付仅文档，不把批准文档 PR 等同于批准任何功能实现。

**第 0 阶段必须先经 June 明确批准，才能开始；其中 Web 原型须 June 签字后才进入 Android 实现，第 0 阶段整体验收通过后才进入功能开发。** minSdk 33（Android 13）、targetSdk 36 已由 June 于 **2026-11-13 决定**，不再询问或回退。若后续发现核心边界需要改变，回到架构评审，不在功能 PR 顺便引入历史包装。

MVP = 能播放 + 能通过现有 lux-music-server 同步歌单（含两客户端可观察的往返与断线恢复）。只有播放器或只有本地歌单都不算 MVP。歌词、统计、推荐、桌面歌词等不阻塞 MVP。

下表每一行是一份可单独评审的小 PR；实际变大时继续拆。工作量单位均为单人有效开发日的**粗估**，不含等待决策、源站变化和设备供应；不是工期承诺。

| 阶段 / PR | 范围与依赖 | 验收标准 | 工作量粗估 |
| --- | --- | --- | --- |
| 0A 架构定案 | 包名、DI、唯一数据层、QuickJS、账号方案与门禁，吸收 [参考取舍](ARCHITECTURE.md#架构参考的采纳与舍弃)；SDK 已决定 | ADR 确认 AccountStore 单一会话、每账号独立库、强类型端口；资料里 minSdk 26/29 等旧建议不覆盖已决 33/36 | 粗估 1–3 日 |
| 0B 设计规范选定（开发 Bot） | 开发 Bot 从 [awesome-design-md](https://github.com/VoltAgent/awesome-design-md) 选一份非杂志风 DESIGN.md，具体待选 | 开发 Bot 添加 docs/design/DESIGN.md 并记录来源；Codex 不代写视觉规范 | 粗估 1–3 日 |
| 0C Web 交互原型（开发 Bot） | 依赖 0B；仅 Web、无后端、mock 数据 | 覆盖账号登录/注册/切换/过期稍后、播放与同步冲突状态，供 June 测行为；不是 Android/后端实现 | 粗估 3–6 日 |
| 0D June 签字认可 | 依赖 0C | 记录原型版本与 June 明确认可；未签字不开始 Android 骨架或功能 | 粗估 1–2 日（评审处理，不含等待） |
| 0E 建立 dev 与 PR 规则 | 按 [RELEASE-FLOW](RELEASE-FLOW.md) 经 June 批准后，另行授权从已批准 main 建 dev；模板/标签建议分小 PR | main 稳定/dev 集成；codex/*、cursor/*；功能 squash 入 dev、dev→main 用 merge commit；模板先可审查，June 配标签；当前任务不执行 | 粗估 1–2 日 |
| 0F Android 编译骨架 | 依赖 0A/0D/0E；Kotlin/Compose、精简 build-logic/version catalog、编译期 DI，仅空页面/Fake | core 纯 Kotlin；无环依赖白名单；独立包名并装；minSdk 33/targetSdk 36 badging；不预建参考项目的模块堆 | 粗估 2–4 日 |
| 0G 门禁与数据骨架 | 依赖 0F；Room 聚合 DAO、schema 导出、账号库句柄、架构测试 | 四类违规被拒；事务失败无孤立待办；Konsist 约束状态/API；没有 RN 迁移器、并行会话实现或业务功能 | 粗估 2–5 日 |
| 0H 协议与脚本可行性 | 依赖 0F；脱敏 fixture/测试宿主，非功能 UI | message2call 帧、MD5/压缩/填充向量；QuickJS 取消/死循环/内存/ABI/16KB page size；加载版本/哈希信任与许可证落 ADR；不能终止脚本则不通过 | 粗估 3–6 日 |
| 0I GitHub Actions PR CI 实施 | 依赖 0G；另开 workflow PR，对应 RELEASE-FLOW 的必需检查 | lint、ktlint/detekt、JVM/Room/契约、依赖图、debug APK；action SHA 固定、fork 无 secrets、超时/取消/纯文档状态处理正确；不自动提交基线 | 粗估 2–4 日 |
| 0J 发布流水线实施 | 依赖 0H/0I；单独 workflow/版本模型/feed 小 PR | dev.N/稳定 tag 公式边界可测；签名 release、ABI、SHA-256、changelog 同源；无 APK 的骨架期只 dry-run、不发空 Release、不回写 main/dev；实际发版需届时授权 | 粗估 3–6 日 |
| 0K 启用保护与环境（June 亲手） | 检查名注册后，June 按 RELEASE-FLOW 配 secrets、branch protection、环境审批/权限及 feed 托管 | 禁止直接推 main/dev/force/delete/bypass；main 允许 merge commit；批准签名材料/Pages 配置；配置缺项明确阻塞启用，不由 bot 代配 | 粗估 1–2 日（配置/核验，不含等待） |
| 0L 第 0 阶段退出评审 | 依赖以上各项；结合后续真实 APK 条件区分 dry-run 与首次发布演练 | June 验收架构/原型/骨架/门禁/CI 与发布计划可执行性；签名正式发布留待可用 APK 与单独授权；通过后才功能开发 | 粗估 1–2 日（评审处理，不含等待） |
| 1A 播放服务 | 第 0 阶段全通过；单条受控音频 | Media3 服务播放/暂停/seek；后台、锁屏/通知操作、耳机断开/焦点中断正确；失败能重试 | 粗估 3–5 日 |
| 1B 队列与页面 | 依赖 1A | 同歌可出现两次；上一首/下一首/顺序策略正确；进程重启只由服务恢复一次；UI 不维护第二份实时队列 | 粗估 2–4 日 |
| 2A 搜索与目录端口 | 依赖 1B、0H；先完成一个经确认可用的目录来源 | 有界分页、取消、错误/空态；DTO 不出 data；搜索歌曲可进入队列；不注册 xm 占位源 | 粗估 3–5 日 |
| 2B 自定义音源 | 依赖 2A 与 QuickJS 验证 | 用户选脚本、显示能力；musicUrl 返回可播放结果；请求取消与死循环不挂 UI；无账号凭据/文件越权；增加 lyric/pic 可再拆 PR | 粗估 4–8 日 |
| 3A 本地歌单 | 依赖 2A | 创建/改名/排序/删除、收藏、歌曲增删移动均为 Room 事务；页面重开一致，保留 ID 不误删 | 粗估 3–5 日 |
| 3B 数据恢复与账号空间 | 依赖 3A、0G | 每账号独立业务库、单一目录指针；重启/切库失败/旧请求晚到无串数据；Remote 不生成待发；无 RN 私有导入路径 | 粗估 2–4 日 |
| 4A 基础歌词（可与 5 并行安排） | 依赖 1B、2B；非 MVP 阻塞项 | 时间轴解析、seek 后对齐、空/错歌词不影响播放；共享歌词模型适配窗口 | 粗估 2–4 日 |
| 4B 扩展歌词 | 依赖 4A | 翻译/罗马音和逐字仅按真实能力展示；格式不支持可降级；悬浮歌词单独评审权限及生命周期 | 粗估 3–6 日 |
| 5A1 登录与注册 | 依赖 3B、0H；按 [ACCOUNT-AUTH](ACCOUNT-AUTH.md)，可提前于阶段 4 | 两请求均带设备信息；注册直接用返回凭据、零次二次 login；Keystore 密文与内存 token、单飞/轮换不重放、错误中文分类；server P0 若未批准保持明确当前适配边界 | 粗估 3–5 日 |
| 5A2 切换与重新认证 | 依赖 5A1；交互以开发 Bot 获批原型为准 | 原地切库不整屏退出；过期横幅/稍后离线；generation 屏障、Keystore 失效保留业务数据；无双会话实现 | 粗估 2–4 日 |
| 5A3 登出与设备 | 依赖 5A2 | 真正调用 logout；在线 sid 撤销可验；离线撤销租约/立即抹除如实提示；只清所属账号，不误撤销新 sid | 粗估 2–4 日 |
| 5A4 同步握手 | 依赖 5A3、0H | 同一安装 deviceId 领取 key、WS 协商；旧连接无跨账号写入；本 PR 不改 server，提案能力未落地不启用 | 粗估 2–4 日 |
| 5B 首次歌单同步 | 依赖 5A4 | 从非空 server 到全新原生客户端，default/love/userList 顺序与元信息正确；冲突合并/覆盖/取消方向经 fixture 验证；覆盖不静默记忆 | 粗估 3–6 日 |
| 5C 增量与恢复（MVP 门槛） | 依赖 5B | 原生 ↔ server ↔ mobile 创建/改名/排序/歌曲增删往返；重连、超时不确定状态、断电、账号切换无重复/误删/回传环；本地待办最终收敛；播放与同步同时工作 | 粗估 4–8 日 |
| 6A 听歌记录 | MVP 后；先本地行为语义再开启协商能力 | 播放/暂停/跳过/完成生成记录规则明确，ID 去重；push/pull 游标与持久事务一致；未协商不发送 | 粗估 3–5 日 |
| 6B 统计与报告 | 依赖 6A | 本地时区、跨月/跨年、重复记录、未来时间 fixture；月报/年度报告可追溯到记录；分享图为独立 PR | 粗估 3–6 日 |
| 7A 后续能力逐项立项 | 依赖需求决策，参考 PARITY | 下载/本地库、推荐、音效、白噪音、评论、一起听各开 ADR/小 PR；每项先定数据与后台归属，不能整包搬迁 | 每项粗估 3–10 日，立项后细化 |

阶段号表示建议排序，不能为等歌词而推迟 MVP 的同步验证；5A1/5A4 的契约试验应尽早暴露风险。外部目录/音源的在线可用性不由此文承诺；失败应给出可理解的原因而非引入无限换源链。

## 分支、发布与架构参考

完整提案见 [RELEASE-FLOW](RELEASE-FLOW.md)：main 稳定、dev 集成，codex/* 或 cursor/* 功能分支；功能 squash 入 dev，dev→main 转正用 merge commit；PR CI 与签名发布分权、dev.N/稳定版、ABI/SHA-256/changelog/feed 均已规划。**原先“暂不建立 dev、MVP 后再考虑发布”的建议已被 t435 覆盖**。当前只确认方案待启用，不创建 dev、workflow、secret、标签或仓库设置；执行属于第 0 阶段获批后的后续 PR 和 June 亲手配置。

[架构参考原文](references/ARCH-REFERENCES.md) 与 [账号参考原文](references/LOGIN-UX-REFERENCES.md) 原样归档；采纳/舍弃依据见 ARCHITECTURE 的真实文件对照。吸收精简 build-logic、编译期 DI、UDF、MediaLibraryService、按聚合 DAO、schema、Fake 与可执行门禁；不复制模块堆、旧新并行服务、源码 fork 的 Media3、巨型 manager/DAO、运行时双 DI、浮动 action、自动回推基线或旧本地迁移。后续 baseline profile 与截图测试在设计/行为稳定后单独评审。

## 已决事项（2026-11-13）

- minSdk **33**（Android 13）、targetSdk **36**；不再作为开放问题。
- 先完整规划发布/CI，执行按 RELEASE-FLOW 的第 0 阶段 PR 审批，不再延后“考虑要不要规划”。
- 视觉设计与交互归开发 Bot；从 awesome-design-md 选非杂志风 DESIGN.md → mock Web 原型（无后端）→ June 签字 → Android。规范具体选哪一份尚未决定，由开发 Bot 承接，Codex 不代写。

## June 的剩余决策/配置清单

1. 是否批准 ARCHITECTURE/ACCOUNT-AUTH 基线与第 0 阶段范围，以及 MVP“可播放 + server 歌单往返/恢复”的验收？
2. 包名是否采用 `cn.lux.music.android`？
3. 是否批准唯一 Room 数据层、每账号独立库、Media3 状态归属、Hilt（备选 Metro）及门禁组合？SDK 不再待定。
4. 是否批准 QuickJS 受限宿主与先行绑定/许可证/ABI/中断验证？
5. 是否确认 v1 不导入 RN 本地遗留数据；本应用本地模式及显式上传是否需要？
6. v1 是否不做 LX 连接码、账号上限是否 5、离线撤销租约保留策略如何定？
7. server P0/P1 先做哪些（必须另开 server PR 且 June 批准）；账号上线能否在 P0 前仅以 `/id` 获取稳定身份？
8. **是否按 RELEASE-FLOW 启用、何时授权建立 dev 与实施 CI/发布 PR；June 何时亲手配 secrets、保护规则、发布环境及 feed 托管？** 首版版本/ABI 与共用签名待启用时确认，不再询问“要不要以后再想发布策略”。

本 PR 的合入不是上述执行授权，也不是 Web 原型已通过签字或 server 改动已获批准。
