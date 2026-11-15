# Stage 4：资料与 LX 连接

2026-11-15。实施中，LX 首版、Lux 后置，原服务端保持不变。补充连接设计已形成[交互评审稿](design/lx-connection-preview.html)，尚待 June 审阅；当前 App 仍为启动画布，没有将未审阅页面接入原生界面。

## 已实现的边界

- `core:api/ProfileSession` 是唯一只读资料状态与类型化命令入口；页面不接收令牌、客户端 key、Room 对象或原始 JSON。Hilt 绑定在 data 内部，未新增模块或第二套账号状态。
- 一个 Room 目录库保存独立本机资料、LX 绑定、加密授权、活动指针与待清理标记；每份资料有 UUID 命名的业务库。相同服务器、相同连接码仍创建新的资料与 bindingId，不把 serverId 当用户 ID。重名资料分配本地编号，不能假装它是远端用户名。
- 业务库只保留一份规范 wire 快照；尚无第二份歌曲/歌单表与之双存。未知字段和 JSON 顺序保留，未来业务投影不能另立可写数据来源。wire 按安全字符边界分块，避免大歌单超过 Android CursorWindow 单行限制。
- 本地动作与 outbox 在同一 Room 事务提交；远端动作走同一快照变换、事务提交但不生成 outbox。覆盖失败回滚全部分块、版本与待办。不增加跨库切换 journal。
- Keystore AES-256-GCM，随机 12 字节 nonce，AAD 绑定 ProfileId、记录类型和版本；连接码仅用于请求并清空字符数组。没有明文降级。失效密钥只要求重新授权，保留歌单；需要授权的事实持久化在目录库，不另设内存 AccountStore。
- 切换先准备目标库，停止旧会话，再在不可取消的短提交阶段原子更新目录指针和内存资料；读写捕获资料 ID、generation 和数据库句柄，在同一切换屏障下校验。失败不能把旧消息写入新资料。取消提交之后的调用不等于回滚已提交的活动指针。
- 断开同步持久化为暂停，保留授权与歌单；退出清本机授权，保留业务库；删除先切离活动资料，留下可恢复的清理标记，再删除库与文件。不调用不存在的远端撤销接口。

## 真实网络与同步

OkHttp 4.12.0，禁用认证请求自动重试及所有重定向，保留子路径，限制探测/认证响应大小。证书错误不降级 HTTP。Android 的 cleartext 开关是静态的；平台允许 HTTP，但私有 LxHttp 在每次使用前要求该连接显式确认，未导出通用 HTTP 客户端或关闭 TLS 验证。

实现 hello/id、临时 RSA 认证、设备密钥重认证、真实 WebSocket、应用文本 ping 与底层自动 pong。输入/错误分类包含地址、证书、网络、拒绝、限流和协议；不传播原始含凭据 URL/响应异常。

只协商 list v1，支持该模块的 12 种动作。入站按接收次序进入单一 actor；RPC 的开始注册使用 UNDISPATCHED，等嵌套回复仍不阻塞接收。模块完成后可接收增量动作；必须同时收到 list_sync_finished 和 finished 才将整体连接标为就绪。整体 finished 不能掩盖模块失败。

任何未确定结果的发送失败都会先重连协调快照，不盲目重放。**每次尝试重新捕获协调版本范围**，完成后才清除已经协调的 outbox；不能沿用首次连接的范围，否则会重复发送丢失回复的已提交动作。初次同步等待显式选择，覆盖不记忆；取消保留独立资料并暂停同步。

## 验证与复现

本地 28 项 data JVM 测试通过、0 跳过：原有协议/Node 互操作，入站顺序，5 项真实 HTTP MockWebServer 测试、2 项模块完成时序测试，以及原版 ListDataManage 的 12 动作逐步 JSON/MD5 对照。本地 verify（格式、Detekt、架构、JVM、Lint、debug APK）与 data 测试 APK 构建通过；最终设备结果另记，不能把构建成功算设备通过。

设备矩阵沿用 API 33 / 4096 页与 API 36 / 实际 16384 页，测试 APK targetSdk 与应用同为 36：

- 8 项真实 Room/Keystore 用例：资料隔离、迟到消息、事务回滚、大 Unicode 歌单、远端不回传/退出保留、开库失败、Keystore 失效、密文替换。
- 2 项真实部署端到端用例：子路径代理、动态 RSA、同码新建绑定、双客户端增量、账户隔离、丢回复不重复发送、离线双端修改后恢复、自签名 TLS 拒绝、被拒绝授权状态跨重启保留。
- 单独 prepare → force-stop → recover 两次 instrumentation，检查活动指针、歌单、outbox 与 Keystore 跨进程保留；保留测试 APK，避免测试框架卸载包造成虚假的数据丢失。
- 原有 6 项 QuickJS 测试继续运行。设备报告区分原始 JUnit、真实页大小、恢复阶段与服务端日志，不能接受跳过。

```sh
export LUX_LX_SOURCE=/path/to/pinned-lx-checkout
python3 scripts/lx-contract/prepare.py
python3 scripts/lx-contract/prepare_server.py
./gradlew verify :data:assembleDebugAndroidTest --console=plain
# 有 KVM 和固定 SDK 的 Linux 环境；脚本启动并停止真实原版 LX 测试部署：
python3 scripts/test_source_device.py 33 default 4096 2
python3 scripts/test_source_device.py 36 google_apis_ps16k 16384 7
```

prepare_server.py 只从固定 d47aca4 导出独立部署副本，npm ci 忽略安装脚本，按原版 build 编译；校验 src 未修改及实际 message2call 发布包。用户只通过官方 LX_USER_、PORT、DATA_PATH 配置公开测试资料；代理和 Node 对端是独立测试辅助进程，不向 server 增加 API。未使用真实服务器或用户账号。

## 设计与交付

[连接补充交互稿](design/lx-connection-preview.html) 复用原 HTML 最终 CSS 和 MingCute 返回图标，覆盖本机使用、服务器与 HTTP 确认、连接码、初次合并/覆盖确认、资料切换及退出/删除范围。评审控件在产品预览之外。Playwright 已验证 HTTP 确认、认证跳转和覆盖取消；不是原生截图验收。

按 docs/design/README.md 的“经 June 审阅形成验收基准”，原生可见页面仍等待确认。其他 HTML 不改。完整音乐页面、播放/音源、Android 后台常驻同步、ARM 真机及发行恢复仍不在本次已完成声明中。

本轮局部 Detekt 例外：ProfileStore 的命令数对应唯一 ProfileSession 状态所有者，不为门禁拆成多套全局状态；ProfileFiles.open/switchLocked 的 catch 仅作资源关闭和一致性恢复后原样重抛；LxSyncSession 的连接及 actor 边界将跨协议/存储异常分类并截断生命周期。不修改全局阈值或新增加密 Lint 忽略。
