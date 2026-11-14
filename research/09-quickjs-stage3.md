# Stage 3 补充：QuickJS 发布包与终止边界

2026-11-14 UTC。延续已有架构选择，只读检查绑定，不改 mobile/server 工作树。

## 实际检查的材料

- [Maven Central wrapper-android 元数据](https://repo.maven.apache.org/maven2/wang/harlon/quickjs/wrapper-android/maven-metadata.xml)：最新发布 3.2.3；mobile 当前使用 2.4.0，不能由旧版本直接推断 16 KB 兼容。
- [3.2.3 AAR](https://repo.maven.apache.org/maven2/wang/harlon/quickjs/wrapper-android/3.2.3/wrapper-android-3.2.3.aar)、POM、wrapper-java JAR 和发布 sources JAR：完整性见 [实施证据](../docs/STAGE-3.md)。没有以 Git 主干代码冒充发布源码。
- 只读参考 [quickjs-wrapper c5f5027](https://github.com/HarlonWang/quickjs-wrapper/tree/c5f5027aafe754108fcb4af12e183a7633c39e85)：`QuickJSContext.java`、`QuickJSLoader.java`、`native/cpp/quickjs_wrapper.cpp` 和 `quickjs_extend_libraries.h`。
- mobile [7021133 preload](https://github.com/JuneDrinleng/lux-music-mobile/blob/702113391333c463049c0f7b449ff2c02b3758ba/android/app/src/main/assets/script/user-api-preload.js)：直接保留为测试资产，附 Apache-2.0 说明；未复制 React Native 宿主。

## 核实与采用

1. AAR 四种 ABI 的 ELF PT_LOAD 均为 0x4000 对齐。但打包仍可能带错 ABI、压缩库或错误 ZIP 对齐，因此门禁检查最终 APK 的所有 `.so`，包括 AndroidX 传递依赖。
2. Java 发布接口有内存/栈限额，没有公开执行中断接口。JNI 会持续 drain Promise jobs，纯协程 timeout 无法打断无限循环或无限微任务。采用隔离进程与独立线程看门狗，超时终止整个实例，再验证下一实例可成功执行。
3. 绑定默认 console.stdout 会抛异常，原版 preload 末尾调用 console.log；必须显式配置。验证运行时使用静默 JS stdout，避免脚本日志直接进入应用日志，不给脚本 Java 日志对象。
4. AAR 带有五个上游开发样例 assets；应用显式排除，测试 preload 也只在 androidTest 中存在。
5. ABI 2.0.0 中，`inited` 能力会按平台筛选；`musicUrl` 响应需 http(s) URL；脚本 HTTP 回调与业务 request 是两层消息。测试使用真实 preload 检查这些行为，不能凭接口名称宣称已实现目录搜索或所有脚本。
6. 包装层 Apache-2.0、引擎 MIT，保留随包许可证。GPL/AGPL 代码没有复制进来。

## 仍需区分的证据

ELF/ZIP 静态检查、JVM 信任检查、Android 真实引擎执行、ARM 真机和业务宿主兼容是不同门槛。设备矩阵及每轮实测结果记录于 docs/STAGE-3.md；未运行的项目保持未验证。

原生 crypto/buffer/timer、宿主网络代理、外部真实脚本、持久信任记录和导入 UI 仍未完成。本阶段先保留内部验证边界，不注册虚假的 SourceResolver 或宣告网络能力。
