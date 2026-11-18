# Lux Music Android：实现前调研

> 决策更新（2026-11-14）：本文保留原始研究结论与建议，不是当前实施规范。已确定先 LX、后 Lux；SDK 33/36、Hilt、dev/main、同包名开发/稳定通道、独立本机资料。最新要求见 [plan.md](../plan.md)，与历史建议冲突时以最新计划为准。

调研日期：2026-11-14（UTC）。状态：**调研完成，架构与产品方案为建议稿，尚未实施**。

本轮直接检索 GitHub，下载并逐文件阅读公开仓库；对照 mobile 的 master/dev、Lux server 的 master/dev 和原版 LX sync server。商业产品使用官方登录/注册网页与帮助资料。所有仓库固定到本次读取的提交，见 [sources.md](sources.md) 和 [sources.json](sources.json)。没有代入“最新主干必然兼容”的假设。

## 先读结论

1. **原生重建可行，但不应机械移植 RN 的 store、bridge、patch 和迁移链。** 推荐 Kotlin、Compose、ViewModel/StateFlow、Room、DataStore、Media3；从少量模块起步。依据和取舍见 [03](03-architecture-options.md)。
2. **两种 server 的核心同步通道可共用，认证方式不能强行合一。** LX 是连接码/设备密钥，Lux 是账号认证后换同步密钥；不改 server，但客户端必须精确实现旧协议。见 [02](02-protocol-compatibility.md)。
3. **Lux master/dev 存在实质差异。** master 没有当前 dev 的 server-info、refresh 和邀请预检；注册返回值、退出与改密语义也不同。不能拿 mobile 现有 AUTH_CONTRACT 当作所有部署的事实。
4. **账号体验要先把四件事拆开：服务器、账号、本地资料、同步连接。** 网络断开不等于登出，换账号不等于清数据，注册不等于 LX 连接。推荐渐进表单与明确切换提交点，见 [04](04-auth-and-account-ux.md)。
5. **保持当前 ui/ 视觉复刻目标。** 登录/注册/账号管理改进是用户新要求下的研究建议，不自动覆盖 design.md，也不在本轮更改 HTML。最终账号流程需先形成设计基准再开发。
6. **发版采用 main + 短分支 PR + 不可变 tag，默认不再保留长期 dev/master 双轨。** 自动测试构建与签名发布分离；nightly、beta、stable 区分版本/安装通道，见 [05](05-git-pr-ci-release.md)。
7. **“实现 mobile 内容”不能缩成 HTML 中的几个 mock 页面。** 源适配、下载、统计、一起听、账号数据隔离等都需要独立验收；AI/管理入口等需明确产品边界。见 [01](01-mobile-baseline.md) 和 [06](06-decisions-and-validation.md)。

## 阅读路径

| 文件 | 内容 |
| --- | --- |
| [sources.md](sources.md) / [sources.json](sources.json) | 方法、固定 SHA、源码路径、许可证、商业资料和证据限制 |
| [01-mobile-baseline.md](01-mobile-baseline.md) | mobile 功能资产、兼容负担的具体证据、原生功能对照 |
| [02-protocol-compatibility.md](02-protocol-compatibility.md) | HTTP/认证/密钥/RPC/同步矩阵与未知项 |
| [03-architecture-options.md](03-architecture-options.md) | 亲自阅读的原生仓库比较、简化模块方案、状态与数据边界 |
| [04-auth-and-account-ux.md](04-auth-and-account-ux.md) | 开源及商业账号流程、建议页面、切换与失败恢复 |
| [05-git-pr-ci-release.md](05-git-pr-ci-release.md) | 分支、PR、Actions、签名、版本号与发布恢复 |
| [06-decisions-and-validation.md](06-decisions-and-validation.md) | 决策状态、未决问题、架构验证门槛、后续计划输入 |
| [07-unified-auth-options.md](07-unified-auth-options.md) | 连接码兑换统一会话的可行性、权限边界、原版 LX 兼容与取舍 |

## 证据用语

- **已核实**：读取固定提交源码、构建配置，或已打开的官方资料支持。
- **建议**：根据本项目约束作出的选择，不是其他项目的承诺，也不是已批准实现方案。
- **待验证**：需要真实 Android 构建/设备/服务端联调/用户选择才能确定。

本轮没有创建 Android 工程、业务代码、workflow、远程分支、PR 或 Release，没有运行/修改服务端，没有使用真实账号登录或测试线上注册。源码和商业页面研究不等于端到端运行验证；商业网页文本不等于 Android app 像素级截图。暂不写完整实施排期，先保留有证据的选项与验证门槛。
