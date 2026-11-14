# Stage 1 · 工程基础

## 范围

建立八个模块及单一 build-logic，core:model/core:api 为 JVM-only；app 使用 Hilt Application/Activity，Compose 入口仅绘制现有 HTML 的米白画布，不是业务页面实现。data/playback/source-runtime 当前只建立编译边界，没有数据库、播放器或 QuickJS 引擎，不虚报 LX 或音源能力。

UI 基准不改；launcher 复用 HTML 内置 MingCute music 路径，许可随 APK 打包。工程不含网络权限、真实账号、假搜索数据或可用性承诺。

## 固定工具链

| 项目 | 版本 |
| --- | --- |
| JDK | Temurin 21.0.13+9；编译目标 Java 17 |
| Gradle | 8.13，官方 wrapper/distribution SHA-256 |
| AGP | 8.13.0 |
| Kotlin / Compose compiler | 2.2.20 |
| KSP | 2.2.20-2.0.4 |
| Hilt | 2.57.1 |
| Compose BOM / Activity | 2025.09.01 / 1.11.0 |
| compile/target/min SDK | 36 / 36 / 33 |
| Build tools | 36.0.0 |
| Spotless / ktlint / Detekt | 7.2.1 / 1.7.1 / 1.23.8 |

版本集中在 version catalog，公共 Android/JVM/Compose/Hilt 配置在 build-logic。版本编码从 version.properties 的稳定目标派生，骨架仅产 0.1.0-bootstrap-debug / 100001，不预占公开 dev tag；release 打包被明确阻断，签名与公开序号分配在发行阶段实施。

## 本地运行

安装完整 JDK 21、Android SDK platform 36/build-tools 36.0.0。使用 ANDROID_HOME，或在不提交的 local.properties 设置 sdk.dir。

```sh
python3 scripts/verify_wrapper.py
./gradlew -p build-logic test validatePlugins
./gradlew verify
python3 scripts/test_architecture.py
python3 scripts/verify_apk.py
```

verify 包含 Spotless、Detekt、真实依赖白名单、JVM 模块测试任务、Android Lint 与 debug APK。核心尚无业务测试，不把 NO-SOURCE 计为测试通过。构建策略测试覆盖版本示例、64 组边界与错误输入。

架构负例通过临时 Gradle init 文件向实际配置注入 feature→data、data→playback、core:api→Android 依赖，必须失败且匹配对应诊断；不改受跟踪文件。Cloud 环境如需要 Java 代理，用环境提供的代理/CA 配置 Gradle，禁止关闭 TLS；负例脚本可通过 LUX_GRADLE_INIT 指定环境自己的 init 文件。

## CI 与验收边界

Verify workflow 在 main/dev/当前堆叠基线 PR 及 main/dev push 执行，required-checks 名称固定，无 secrets、发布或自动基线回写；上传 debug APK/报告七天。配置已提交不等于远端规则已启用，维护者在实际检查注册后设置保护。

APK verifier 用 aapt2/apksigner 检查实际 debug 包名、debuggable、API 33/36 与有效签名。wrapper verifier 校验官方 Gradle 8.13 JAR/distribution，来源为 services.gradle.org 的对应 SHA-256 文件。

本阶段不运行模拟器/设备、不验证像素、不执行 LX 同步或 QuickJS ABI 测试；后者属于下一阶段的独立 PR。CI 首次远端运行结果与本地结果分别记录。

## Lint 与备份策略

所有代码/资源 Lint 警告仍按错误处理。仅禁用 AndroidGradlePluginVersion、GradleDependency、NewerVersionAvailable 三个远程版本可用性提示：固定工具链升级通过每周 Dependabot PR 审查，而不是让相同提交的 CI 随远程版本变动。Dependabot 以 dev 为目标，待主线纳入配置且 dev 建立后生效，不自动合并升级。

SDK 33 下不保留 v26 资源目录。dataExtractionRules 明确排除云备份和设备迁移的私有/外部数据域，allowBackup=false；未来资料备份能力须另行设计，当前不把认证材料带到新设备。

## 本地验证记录（2026-11-14）

- 官方 wrapper JAR 和 distribution SHA-256 检查通过。
- ./gradlew verify：通过，包含 debug APK、Android Lint、Spotless、Detekt 和实际依赖检查。
- build-logic test/validatePlugins：3 项 JUnit 测试，0 失败/0 错误，覆盖 64 组版本边界。
- 三种依赖负例均按预期拒绝：feature→data、data→playback、core:api→Android。
- aapt2/apksigner：包名 cn.lux.music.android.debug，minSdk 33、targetSdk 36、versionCode 100001、debuggable、签名有效。
- HTML 与 main 保持一致；未配置发布密钥、保护、tag 或 Release。
- 未启动模拟器/真机，未验收 Hilt 启动后的实际画面、HTML 1:1、后台播放、LX 或 QuickJS。远端 Actions 的首次运行结果不包含在上述本地结论中。
