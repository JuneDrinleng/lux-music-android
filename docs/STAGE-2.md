# Stage 2 · LX 协议与上游互操作

## 范围与实现

协议代码位于 data 内部的 sync/lx，JSON、wire key、DTO 均不进入 core API 或 UI。实现连接码/设备密钥认证编解码、明确 SHA-1/MGF1 的 RSA-OAEP、AES-128-ECB padding、代理子路径及 socket query 编码、压缩、message2call 0.1.3 envelope、快照 JSON/MD5 与设备视角同步模式。

RPC peer 按连接创建，使用独立关联 ID 空间和有归属的协程；支持并发关联、嵌套调用、超时、取消、关闭与迟到回复隔离。发送失败会关闭所属 peer，入站回调失败返回安全错误；不向公共 API 抛出含凭据/原帧的异常内容。

JSON 编码专门保持 JSON.stringify 的整数字符串键排序、普通键插入顺序、控制字符、孤立代理项以及 IEEE-754 最短数字表示。快照保留未知字段，拒绝把 tempList 放入同步快照。未来 Room mapper 仍须遵循用户列表字段构建顺序，不能直接序列化数据库实体。

默认客户端预算：编码帧 8 MiB、解码帧 32 MiB、JSON 嵌套 64 层、128 个待回复请求、32 个入站处理、请求 30 秒。压缩在超过 1024 个 UTF-16 单元时使用 cg_+base64(gzip)。gzip 的二进制字节可因压缩器不同而变化，验证相互解压后的内容，不要求压缩字节相同。这些是客户端边界，不是上游服务端的限制。

## 基准与来源

- LX 固定 d47aca4284a7c4d9ef755df1f44fb0b0a5b2af36，测试运行时拒绝其他提交或修改过的源码/lockfile。
- message2call 使用该服务端 lockfile 中实际发布的 0.1.3 tgz，校验 SHA-512；dist 再校验 SHA-256 e60f226619e81a0c700aeb72ede193cd14ca5e0d119c7830dd97fb89381b623e。
- Git tag 2a5c8b9 的 dist 与发布包不一致，不能将它当成部署包。测试执行发布包的 message/destroy 入口，不对两种入口做猜测 fallback。
- oracle 使用 Node 24.22.1 的 TypeScript 转换与 VM 模块加载原始源码，绝不回写上游，也不需要 npm install。测试编排代码与实际源文件 SHA-256 一起供检查。

实际执行 tools.ts、auth.ts、server/sync/sync.ts、modules/list/sync/sync.ts 等。存储、日志、广播和 socket 回调用内存 fixture 替代。没有启动完整 HTTP/WS 服务，也未验证磁盘快照；Node 仅为开发测试工具，不打入 Android APK。

## 黄金数据与可复现性

vectors.json 包含来源、公开测试 DER 密钥、认证/AES/RSA、UTF-16 压缩边界、JSON 数字/Unicode、RPC 和 13 种同步场景。密钥和连接码都是公开样例，不能用于实际部署。

生成器每次执行原版函数，覆盖合并/覆盖/取消、两种空端、三方删除、已同步、响应失败及重连协调。整体 finished() 也可能在 list 失败后调用，所以取消/失败必须保持 list 未就绪。

RSA-OAEP 输出带随机性；检查模式保留已记录的合法密文并重新解密核验明文，其他数据重新执行并逐字比较。Kotlin 互操作测试另接收当前运行新生成的随机 RSA 响应，并让 Node 解密 Kotlin 生成的密文，避免只证明单向自洽。

## 运行

先检出固定 LX 提交并设 LUX_LX_SOURCE 为绝对目录，再执行：

```sh
python3 scripts/lx-contract/prepare.py
node --experimental-vm-modules scripts/lx-contract/generate.mjs --check
./gradlew :data:testDebugUnitTest
./gradlew verify
```

没有 LUX_LX_SOURCE 时，黄金回归/生命周期测试仍运行，3 项 Node 互操作测试明确标记跳过；不能据此声称互操作通过。测试任务将 oracle 开关、脚本、发布包和上游 TS 输入纳入缓存键。CI 设置该变量、检出精确源码并校验发布包/fixtures 后执行全部测试。

需要更新黄金数据时，先核对来源及合同，再运行不带 --check 的生成器；将来源和变更一起纳入 PR。CI 不接受新 baseline，也不使用真实服务凭据。

## 阶段边界

尚未接入 Android HTTP/WebSocket、原生 ping/pong/退避重连、Room、同步动作持久事务、资料切换、连接页面或音乐源。通用 RPC 不提供歌单模块事务队列；该队列在后续 sync coordinator 实现。歌单合并由原版服务端执行，客户端不另造第二套三方合并算法。

当前 UI 仍是 Stage 1 米白画布。本阶段不提供新的可用音乐体验包，不宣称真实部署/设备端同步已通过。

## 验证记录

- 本地 verify 通过：Android debug APK、Android Lint、Spotless、Detekt、依赖边界以及 data 单测。
- data 共 18 项 JUnit 测试：8 项 wire/快照、7 项 RPC 生命周期、3 项 Node 互操作，0 失败、0 跳过。
- 真实 Node 测试执行 13 种同步场景；JSON corpus 含边界值与 160 个种子确定的 IEEE-754 候选（过滤非有限值）。
- 测试验证设备视角覆盖方向、同步就绪后的快照 MD5 一致、取消与失败不就绪、三方删除和重连协调。
- Android 设备、真实 HTTP/WebSocket 服务部署与 Room 持久同步尚未验证，远端 CI 结果另行记录。

### 协议限定的 Lint 例外

Android Lint GetInstance 会拒绝 AES-ECB。原 LX v4 的 src/utils/tools.ts 固定 aes-128-ecb；在不改服务端的前提下，客户端必须保持该 wire 算法。因此仅在 LxCrypto.crypt 函数使用有注释的 @SuppressLint("GetInstance")，不全局关闭检查、不产生 baseline，也不用于本地凭据。源码、跨语言向量和互操作测试共同约束该例外。

补充检查：fixture --check 重新执行上游函数并确认无漂移；build-logic test/validatePlugins、三项架构负例、官方 wrapper 和 APK 元数据/签名检查通过。APK ZIP 中不含 vectors.json、Node oracle 或 message2call 测试包；HTML 保持与 main 相同。

构建内存上限调整为 Gradle 2 GiB、Kotlin daemon 1 GiB、最多两个 worker，以控制多 JVM 峰值；不将其视为应用性能测试结果。
