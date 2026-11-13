# 仓库协作规则

- 用中文沟通、编写架构与实现说明。采用小 PR，每个 PR 只解决一个可验收的问题；先查 git status/log 与已有 PR，续做不重复开 PR、不覆盖已有工作。
- 所有后续变更只走任务分支 → PR。唯一直接推 `main` 的授权已在 2026-11-13 用于只含 README 的初始提交 `2080e375c85848287ff15f838130b3edf83330c2`，已经用完。此后永不直接推 `main`，不得 force push。
- 先优化整体架构，再开发功能。以 docs/ARCHITECTURE.md 为核心提案；第 0 阶段须 June 明确批准后开始，验收后才写功能，不将文档 PR 的批准解释为功能授权。
- lux-music-mobile 只用于功能与真实协议对照，不作为架构蓝本；不得带入 RN 兼容 shim、重复状态/缓存、多层 repository、占位模块、旧设置和遗留迁移。v1 默认不导入 RN 本地数据。
- lux-music-server 原样复用；接口事实引用真实文件与提交，不编造端点、数量或功能。未知处标“待确认”；工作量一律标“粗估”。
- dev/master 双通道、是否建 dev、是否实施发布 CI 均由 June 以后决定。当前不得创建 dev、添加发布 workflow 或擅自执行发布。
- 本文档任务只新增文档；不得加入 Kotlin/Gradle/资源/CI 文件。不要合并 PR、发版、打 tag、建 release、删除分支/tag/release 或修改仓库设置。
- 不读取/打印凭据文件、token、key、webhook 地址、全量环境变量或完整进程参数。不发邮件、不联系任何人，不执行 npm install；临时输出放 /dev/shm 或 /tmp，避免大量小文件。
- 参照工作树 `/workspace/codex-dev/playdetail-fixed-lyric-height` 与 `/workspace/codex-dev/_base-server` 严格只读，即使环境允许写入，也不得修改、提交、推送或清理它们。
