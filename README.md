# Lux Music Android

Lux Music 的原生 Android 项目，采用 Kotlin、Jetpack Compose、Hilt 和 Media3，与 [lux-music-mobile](https://github.com/JuneDrinleng/lux-music-mobile) 并行发展。

首版提供独立本机模式，直连原版 [LX Music 同步服务](https://github.com/lyswhut/lx-music-sync-server)；Lux Server 账号与扩展能力后续接入。最低 Android 13，服务端不改。

- [实施计划](plan.md) · [架构](docs/ARCHITECTURE.md) · [路线图与进度](docs/ROADMAP.md)
- [资料与连接](docs/ACCOUNT-AUTH.md) · [LX 协议](docs/SYNC-CONTRACT.md) · [开发发布](docs/RELEASE-FLOW.md)
- [设计规范](design.md) · [HTML 基准](ui/index.html) · [调研证据](research/README.md)

Stage 1 已建立可构建的原生工程和 debug APK，包含 Hilt、模块边界、格式/静态分析、Android Lint 和 PR CI 配置。启动页目前仅为基准画布；音源、播放、资料库和完整 HTML 页面尚未实现。Stage 2 已完成 LX 协议编解码与上游互操作测试，真实服务连接仍待接入。

Stage 3 正在验证 QuickJS 3.2.3、隔离进程与死循环终止，并接入用户指定的耳机图标。它仍不开放音源导入和业务页面；实际设备结果与构建结果分别记录。

构建与验证命令见 [Stage 1](docs/STAGE-1.md)、[Stage 2](docs/STAGE-2.md) 和 [Stage 3](docs/STAGE-3.md)。远端检查与保护启用按路线图分别记录。
