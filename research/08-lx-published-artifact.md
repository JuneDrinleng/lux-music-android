# 08 · LX 实际发布依赖与源码基准差异

2026-11-14，Stage 2 执行核查。此前源码研究继续保留；本节纠正将 Git tag 内置 dist 等同于部署产物的假设。

LX 固定提交 d47aca4 的 package-lock.json 将 message2call 锁到 0.1.3：

- 发布地址：https://registry.npmjs.org/message2call/-/message2call-0.1.3.tgz
- 完整性：sha512-jOOPKWTE5LMfuxz8D1o186ex+kJl7FgRz033Z371kvyfZDyCn+qhtrsIvgf4kPUf4zYAWM2SU4GFWbA7F2U+Ag==
- 发布包 dist/message2call.js 的 SHA-256：e60f226619e81a0c700aeb72ede193cd14ca5e0d119c7830dd97fb89381b623e。

实际下载、校验 tarball 后确认：它与 Git tag v0.1.3（2a5c8b9）提交的 dist 不相同。tag 中 src 公开 message/destroy，tag 中 dist 公开 onMessage/onDestroy；npm 实际发布包公开 message/destroy。研究版本应同时记录仓库提交、lockfile 与发布包完整性，不能只看默认主干或 tag 名称。

Stage 2 oracle 改为加载经过上述完整性校验的发布包。部署协议仍为 {name,path,data} 请求和 {name,error,data?} 回复，不是 JSON-RPC 2.0。互操作测试和来源可复现方法见 [Stage 2](../docs/STAGE-2.md)。

只执行公开测试材料与只读源码，不改 server、不安装 npm 依赖、不使用真实连接码。认证及同步 handler 的内存测试宿主不等于完整服务部署联调。
