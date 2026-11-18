# 分支、PR、验证与发布基线

2026-11-14，按用户已确认选择修订。当前没有 workflow、密钥、保护规则或发行运行；实施见 [路线图](ROADMAP.md)。

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

## 配置清单

June 配置 ANDROID_KEYSTORE_BASE64、ANDROID_KEYSTORE_PASSWORD、ANDROID_KEY_ALIAS、ANDROID_KEY_PASSWORD；公开指纹使用 ANDROID_SIGNING_CERT_SHA256。Keystore 自行生成备份，runner 临时解码并清理，不向机器人发送值。Pages 与签名环境按最小权限配置，PR 无 secrets。首轮人工完成签名发布演练后才启用自动 dev 发行。

依据：[GitHub 分支保护](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)、[Actions 安全使用](https://docs.github.com/en/actions/reference/security/secure-use)、[Android 版本](https://developer.android.com/studio/publish/versioning)；源码参照见 [研究](../research/05-git-pr-ci-release.md)，其中单 main/分包名等历史建议已被当前决定覆盖。
