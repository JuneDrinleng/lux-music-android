# Stage 3：QuickJS 验证与应用图标

## 范围与门槛

本阶段先证明绑定可装载、原版 mobile 脚本 ABI 的关键消息可运行、资源越界可终止并恢复。`QuickJsSandbox` 是内部验证边界，**不是已经接入播放器的音源系统**。尚不开放导入页面、业务 SourceResolver 或脚本网络权限；不能把原版 preload 的测试宿主称为完整原生 ABI 实现。

## 绑定选择与证据

沿用 mobile 的 `wang.harlon.quickjs` 系列，锁定 `wrapper-android:3.2.3` 及其 `wrapper-java:3.2.3`。没有 fork 引擎，也没有复制 RN bridge。Maven Central 元数据当前最新版为 3.2.3；选它的依据是实际 AAR，而非 README 的兼容承诺。

| 发布包 | SHA-256 |
| --- | --- |
| wrapper-android-3.2.3.aar | b1d0c550818f07e411b6e6f0eb21b28d83ed2e227d6129395be9e5a2167f4f0c |
| wrapper-java-3.2.3.jar | 2f795c28787ecf77b7081fdc9764ce75dc7f9d39402c56b09161625d3ce97de3 |
| wrapper-java-3.2.3-sources.jar | a2f895e3195a6c7b408032848a2ed410e36f512821fe4b1b39ca83e89a9d4955 |

实际 AAR 提供 arm64-v8a、armeabi-v7a、x86、x86_64；各 PT_LOAD 的 p_align 为 0x4000。debug 工程明确保留 **arm64-v8a、armeabi-v7a、x86_64**，排除 x86；两个 ARM ABI 用于手机，x86_64 用于模拟器。最终 APK 的所有原生库（包括 AndroidX 传递库）均须通过 ELF 与 ZIP 16 KB 对齐检查，不能只验 QuickJS。

Java 发布源码提供 `setMemoryLimit`、`setMaxStackSize`，未提供执行 interrupt API。只读架构参考为 [quickjs-wrapper c5f5027](https://github.com/HarlonWang/quickjs-wrapper/tree/c5f5027aafe754108fcb4af12e183a7633c39e85)；这不是发布二进制的可重现构建证明。QuickJS Wrapper Apache-2.0、QuickJS MIT 声明随应用分发。依赖 AAR 夹带的五个开发样例从应用 assets 中排除。

## 隔离、预算与生命周期

- `bindIsolatedService` 每次运行分配独立实例、隔离 UID 和进程；服务不可导出。服务执行前复核隔离 UID 范围，不能退回主进程。
- 每次验证只运行一个脚本、一个 Context、一个 HandlerThread。创建、evaluate、结果读取及 close 均在同一线程；不跨线程 destroy。
- 独立 ScheduledExecutor 看门狗在执行前启动，覆盖 native loader、用户脚本、Promise jobs、表达式、序列化与析构；超时直接终止本隔离进程。主应用另有绑定/IPC 超时，取消时发送 STOP 并解除绑定。
- 脚本 UTF-8 最大 256 KiB、结果表达式 8192 字符、结果 32768 字符、引擎堆 32 MiB、栈 512 KiB、执行最多 5 秒。IPC 载荷受限；JS 内部堆预算不等于整个进程 RSS 上限。
- console.stdout 显式设为静默 JS 函数，避免绑定默认抛错及脚本日志外泄。不提供 Java/Android 对象、任意模块加载器、std/os、网络、文件、数据库、凭据或播放器接口。错误只返回分类文本，不回传原始脚本异常或 URL。
- 每次运行是一次性验证上下文，不承诺长期驻留脚本、宿主计时器、后台网络和更新通知已经实现。业务接入时仍须保持专属线程/上下文与独立看门狗。

`ScriptAdmission` 要求 ABI 2.0.0、SHA-256 与明确确认值一致；任意字节变化重新确认。这里只实现内容绑定检查，不将脚本自报版本、文件名、导入元信息或“已信任”当作资源隔离。

## ABI 验证，不复制历史宿主

测试资产直接使用 mobile [7021133 的原始 preload](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/android/app/src/main/assets/script/user-api-preload.js)，SHA-256 `e0e892ea9472e190aef6f16996ebcd6bec077f7d8659ceb87a30830d8a53fe41`，Apache-2.0 来源与许可在 androidTest assets 中。

Android 设备上加载真实 3.2.3 引擎，在纯 JS 测试宿主中检查 `lx.version/env` 对应的 v2 preload、`inited` 能力筛选、Promise 请求、musicUrl 响应、请求键关联、HTTP 请求/回复消息形状和非法 file URL 拒绝。测试宿主只记录消息并注入固定回复，不发网络；crypto/buffer/timer native helper 明确抛出不支持，不假装成功。preload 和测试宿主均不进入应用 APK。

后续原生宿主只实现真实脚本证明需要的能力。网络一律由宿主代理，须有限额、取消、迟到回复丢弃、资料代次检查，不能把凭据交给脚本。AES/RSA/buffer、真实样本、计时器、lyric/pic 和更新提示等尚未验证，不宣称所有 mobile 脚本已兼容；目录搜索也不能由 musicUrl 能力推导。

## 自动验证

本地命令：

```sh
./gradlew verify :source-runtime:assembleDebugAndroidTest --console=plain
python3 scripts/test_verify_native.py
python3 scripts/verify_native.py
```

设备矩阵：API 33 / default x86_64 / revision 2 / 4096 字节页；API 36 / google_apis_ps16k x86_64 / revision 7 / 16384 字节页。Emulator 锁定 37.2.13，修订变化须审阅更新。脚本同时核对 `getconf PAGE_SIZE` 与测试进程的 `sysconf`，错误页大小直接失败。

6 项设备测试覆盖：实际 native 装载、Unicode/Promise、全局隔离、无限循环、Promise 饥饿、取消、并发隔离、内存/栈/输出越界、异常脱敏及后续成功运行、两组真实 preload 契约。JVM 测试覆盖内容变更、ABI 变更、UTF-8 字节预算和已知 SHA-256。ELF 检查用正反向二进制样本验证。

`required-checks` 依赖构建及两个设备 job；设备失败或跳过不能通过聚合检查。报告包含 JUnit、HTML、镜像/页大小和 emulator/logcat 证据。这里的模拟器验证不代替 ARM 真机及正式发行验收。

本地完整 verify、Android 测试 APK 构建、18 项 LX 回归 + 2 项信任检查（0 失败、0 跳过）、3 项构建策略测试、3 项架构违规负例、2 项 ELF 规则正反例、wrapper 及最终 APK 元数据/签名/对齐检查通过。HTML 与父阶段相同。

本地没有 `/dev/kvm`，且镜像解压曾因磁盘不足失败，因此不提供本地设备运行声明。远端已固定 AVD 创建/发现目录，并配置绑定默认 console；最终设备结果如下。

[Verify #6](https://github.com/JuneDrinleng/lux-music-android/actions/runs/43610838003) 验证代码提交 `1dc835263b9889b90f2ba4503139bb9dcf2952ed`（PR 合成提交 `b71e932b13b8fc90f34667747466992bfe686d05`）：构建、两个设备 job、聚合 required-checks 全部通过。已下载 JUnit 与设备证据复核，不只依据 job 的绿色图标。

| 设备 | 实测页大小 | JUnit |
| --- | --- | --- |
| API 33 / x86_64 / default revision 2 | 4096 | 6 项，0 失败、0 错误、0 跳过 |
| API 36 / x86_64 / google_apis_ps16k revision 7 | 16384 | 6 项，0 失败、0 错误、0 跳过 |

[带耳机图标的 debug APK](https://github.com/JuneDrinleng/lux-music-android/actions/runs/43610838003/artifacts/14120576711) 可下载，Actions 保留 7 天；不是正式 Release，界面仍为启动画布。彩色/单色图标的 APK 解码 RGBA 像素均与仓库 PNG 一致。

以上通过的是引擎与隔离验证基座。完整原生 ABI、外部脚本、宿主代理及 ARM 真机仍须在音源功能接入前继续验证，不把内部 probe 注册成可用音源。

## 用户图标

按本轮要求，用耳机图片替换原启动图标。附件没有挂载为可读源文件，当前 PNG 为 image_gen 基于附件制作的转换版，去除黑色外角、保留白底和耳机布局；不是原附件字节副本。PNG 不经脚本重绘或裁剪，交给 Android 自适应遮罩，背景为白色。主题图标另有透明底的耳机单色轮廓，取代旧音符 monochrome，避免显示旧图案或整块方形。

原 `ui/index.html` 与其他前端设计基准保持不变。最终图标仍需在实际桌面检查圆形/圆角方形遮罩，不把资源打包检查写成桌面验收。
