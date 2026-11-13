# 账号、认证与切换架构

2026-11-13，设计提案，不含视觉稿、颜色、字体或布局。视觉与交互原型由开发 Bot 按 [design 占位说明](design/README.md) 承接；本文仅定义状态、数据、协议与可验收行为。[原始调研](references/LOGIN-UX-REFERENCES.md) 按原文归档，其 1.2/4.6 的反面清单用于审查，4.5 的 server 建议不是现有 API。

证据：mobile `3ab131e141ccbdc807d200a99eb216a17017c8c8`、server `794fcc4cfee03cdc12c56dad0272f116e3f0d8a9`，下文 M:/S: 为相对源码路径，[完整已实现契约](SYNC-CONTRACT.md) 提供固定仓库链接。外部来源只吸收原则，不复制实现/许可证。minSdk 33、targetSdk 36 已由 June 于 2026-11-13 决定。

## 单一账号身份、单一状态机

建议 v1 只支持“稳定 serverId + 服务端 user.id”确定的 **Lux 账号**；不做 LX 连接码登录，本地模式作为独立 LocalProfile 可选，二者是否纳入 v1 待 June 确认。不能先写 local/lx/lux 三套登录实现再靠开关切换。账号上限建议 5，待 June 决定；这是客户端资源取舍，不声称 server 已有限制。

唯一入口为 `core/api` 的 **AccountSession**；唯一实现为 `data/account` 内部 **AccountStore**，它就是 ARCHITECTURE 的账号端口实现，不再增加一个 AccountRepository/SessionManager/coordinator。它公开一个只读 `StateFlow<SessionState>` 和类型化命令；UI、播放与同步只订阅/消费这个状态，不另存“已登录”布尔量或当前账号全局变量。

| 模型 | 不变式 |
| --- | --- |
| AccountKey(ServerId, UserId) | 两个强类型 ID；URL 是连接地址，不是账号主键；同名用户在不同服务器不合并 |
| Account | key、规范化 endpoint、服务器名、公开资料、认证状态、lastUsedAt；不含密码或裸令牌 |
| SessionState | Loading / NoProfile / Active(profile, accounts, connectivity)；需要切换时包含 pendingTarget 的瞬态状态，旧 profile 在提交前仍可识别；不先清空界面数据 |
| Profile | Remote(AccountKey) 或可选 LocalProfile；不是复刻 RN 的 LX 身份变体 |
| AuthStatus | Valid / NeedsReauth(ReauthReason) / SignedOut；网络故障与认证被拒不是同一种状态 |
| Connectivity | Online / Connecting / Offline / Incompatible；账号可 NeedsReauth 且仍使用本地数据 |
| ReauthReason | RefreshRejected / CredentialUnavailable / Unknown 等；PasswordChanged、RevokedElsewhere、AccountDisabled 仅在服务端有可靠信号时细分，不从同一个 401 猜原因 |
| ServerProbe | Idle / Checking(endpoint,generation) / Ready(info,transport) / Incompatible / Unreachable(ProbeError)；过期 probe 结果不能覆盖新输入 |
| AuthError | InvalidCredentials / InviteInvalid(kind) / RateLimited(retryAt?) / Network / Incompatible / Server(code)；现行笼统 INVALID_REQUEST 保持通用中文说明，不解析英文 message |

操作面：probe、login、register、switchTo、reauth、logout(keepData)、remove。ViewModel 只投递 sealed Event/命令并展示不可变 UiState，必要的一次性 Effect 不承担持久登录事实。输入草稿限当前操作内存，服务器最近使用列表从账号元数据派生，不复制地址历史/最近登录/会话草稿三套记录。

参考 [Element SessionStore](https://github.com/element-hq/element-x-android/blob/develop/libraries/session-storage/api/src/main/kotlin/io/element/android/libraries/sessionstorage/api/SessionStore.kt) 的单一流、[LoggedInState](https://github.com/element-hq/element-x-android/blob/develop/libraries/session-storage/api/src/main/kotlin/io/element/android/libraries/sessionstorage/api/LoggedInState.kt) 的令牌有效性状态、[Bitwarden AccountSummary](https://github.com/bitwarden/android/blob/main/ui/src/main/kotlin/com/bitwarden/ui/platform/components/account/model/AccountSummary.kt) 的环境标签与账号状态，以及 [Thunderbird Contract](https://github.com/thunderbird/thunderbird-android/blob/main/feature/account/setup/src/main/kotlin/app/k9mail/feature/account/setup/ui/autodiscovery/AccountAutoDiscoveryContract.kt) 的 State/Event/Effect；本项目不复制多套 session 流和庞大框架。

## 存储与按账号隔离

- 一个小的账号目录 Room 库持久化 Account 元数据、活动 AccountKey、密文引用；每账号一个独立业务 Room 库与文件/缓存命名空间，路径用无碰撞编码的 `(serverId,userId)` 的 SHA-256 派生，不用用户名或 URL 拼路径。LocalProfile 如获批也有自己的库。目录仅保存归属信息，不复制业务歌单。
- 业务库由同一个 data 层 DAO 实现 Library/History 等端口，不新增“库选择 repository”。每个异步写入捕获 AccountKey、库句柄和 session generation，并在提交前复核；禁止用返回时的 currentDatabase 接收旧请求。
- 切换不迁移歌单、不搬文件、不做多库双写；唯一持久提交点是目录的活动指针。没有跨库四阶段持久化切换日志。仍需要暂停/取消和代际屏障，这些是必要正确性约束，不能因“换指针”而省略。
- 每账号独立缓存的访问随会话授权；退出账号不再向通知/播放器暴露原账号歌曲。加密保护认证材料不等于整个歌曲库都已加密，业务数据库额外加密需求另评审。

来源：[Element SessionData.sessionPath](https://github.com/element-hq/element-x-android/blob/develop/libraries/session-storage/api/src/main/kotlin/io/element/android/libraries/sessionstorage/api/SessionData.kt) 证明会话目录隔离思路；这是本项目每账号 Room 的设计推导，不声称参考项目使用相同 schema。

## 令牌与网络边界

按 [Android Keystore](https://developer.android.com/privacy-and-security/keystore) 采用平台密钥加密：生成不可导出的 AES-256-GCM 密钥，每次加密用新 nonce，AAD 绑定 AccountKey、记录类型和 schemaVersion。优先验证 StrongBox 能力，无法满足时退到设备 Keystore 支持的实现，记录保护级别；不能静默退为明文。后台刷新所用密钥不要求每次生物认证。绑定/密钥失效或恢复后解密失败 → NeedsReauth(CredentialUnavailable)，保留业务数据。

| 内容 | 唯一存储位置与生命周期 |
| --- | --- |
| accessToken + expiresAt | AccountStore 内存，仅使用现行 session-bound accessToken；不落 Room、DataStore、saved state、日志或崩溃报告 |
| refreshToken | 私有认证 vault 的 GCM 密文；Keystore 存密钥而非直接存任意 token 字符串；每账号独立密文记录，目录只保存引用 |
| clientId + 同步 key | 同一 vault 加密；绑定所属账号/session；仅供现行 WS 握手，不可交给脚本或 UI |
| 密码、邀请码、LX 连接码 | 应用不持久化；请求结束/取消清理内存输入，日志和恢复状态均不得包含。即使以后批准 LX 也不得明文存连接码 |
| 设备身份/公开资料 | 稳定安装 deviceId 与设备名/platform 为非敏感元信息；不使用硬件标识。登录、注册和领取同步 key 使用同一个 deviceId |

vault 只是一处文件/密文存储设施，受 AccountStore 串行管理，不对外另发登录状态。原子替换密文并保证完成后才发布新凭据引用；写入失败不回退到明文、不把新 accessToken 发布为持久成功。自动备份和设备迁移排除 vault、密钥关联元数据及撤销租约；对需要跨设备恢复的非敏感业务数据另定备份策略，测试恢复后的安全重新认证。[备份依据](https://developer.android.com/identity/data/backup)。

不用已被官方弃用的 [security-crypto API](https://developer.android.com/jetpack/androidx/releases/security) 新建 EncryptedSharedPreferences，也不为本项目引入 AccountManager/Authenticator 第二套账号体系。[Bitwarden 的 Keystore 实现](https://github.com/bitwarden/android/blob/main/data/src/main/kotlin/com/bitwarden/data/datasource/disk/KeystoreEncryptedSharedPreferences.kt) 仅作为直接使用平台设施的参考，不照抄其存储封装。可选 [Credential Manager](https://developer.android.com/identity/sign-in/credential-manager) 由系统托管密码填充，不存本应用的 token；用户是否启用系统密码保存不影响 AccountStore 的单一来源。

endpoint 保留反向代理 base path，只规范化 scheme/host 与多余末尾斜杠，不能随意小写整个 URL、剥路径或接受 URL 中嵌入密码。默认 HTTPS，HTTP 是需用户显式确认的独立风险选择；不因证书错误自动改 HTTP，不关闭主机名/证书校验。明文允许范围由后续 [Network Security Config](https://developer.android.com/privacy-and-security/security-config) 验证，不能一句“支持自建”就全局允许明文。多地址、自定义认证头/客户端证书如有需求另案，敏感头同样进 vault，不混进账号可见元数据。

## 登录、注册与重新认证

服务器探测首先读取 `/api/server-info`；现行实现没有 serverId（S:`src/server/app.ts`），所以在 P0 未落地时，**仅为获取稳定身份**使用一个局限在 remote 的 `GET /id` 适配器，去掉源码规定的 idPrefix 后形成 ServerId。不执行 LX 连接码登录，不加 `/hello` 作为额外登录门槛、不维护旧身份实现。P0 落地后用显式能力探测优先 server-info.serverId；删除适配器需按实际 server 支持范围另 PR，不能伪造服务器已有该字段。若 June 要求完全不读旧 `/id`，则账号上线以 P0 为前置。serverId 改变须视为新服务器身份，不静默重绑旧库。

探测显示的可注册性以 `registration` 为准，但当前它在 server 中固定 enabled/inviteOnly；原生不能推断已有可配置关闭注册。minClientVersion 的版本命名空间是否覆盖原生客户端仍待 server 确认，不能把 RN 的版本阈值机械比较到独立 Android 版本后误封登录。

三项**无需改 server 就必须做对**的行为：

1. **登录与注册都带设备信息**：login body 含 username/password/deviceId/deviceName/platform；register 另含 inviteCode/displayName?，同样带设备字段。建议 platform=`lux_music_android_mobile`，此值同时让现行 sync/key 的 includes('mobile') 识别为移动端。参考资料写 `lux_music_android`，仅登录字段允许，但原生统一值时须顾及同步判定。S:`src/server/api/auth.ts` 的 loginCredentials 对两种请求都已读取这些字段；`src/account/sessions.ts:create` 在同一账号/deviceId 重登时撤销旧会话。M:`src/plugins/sync/client/auth.ts:182` 与 `src/utils/luxAuthApi.ts:123–125` 的现有请求没有这些信息，不照搬。
2. **注册成功直接采用返回凭据**：先验证返回用户/凭据、加密持久化 refreshToken，再建立该账号状态并领取 sync key；不再 POST login。S:auth.ts 的 register 已调用 loginCredentials；M:`src/screens/Login/AuthFlow.tsx:118–130` 注册后又 connectLuxServer 是应避免的反例。注册响应丢失是“结果未知”，不能自动重复注册/登录并声称只产生一次会话，须查询/让用户确认后恢复。
3. **登出调用 `POST /api/auth/logout`**：不是只清本地。S:auth.ts 依据 sid 撤销当前会话；无 sid 旧 token 会触发更大范围会话失效，因此原生不选返回的 legacy token。离线与失败处理见下节。M:`src/utils/syncLogout.ts:applySyncAccountLogout` 只清本地，不照搬。

以上验收应模拟真实 HTTP body/调用次数及服务端会话结果：设备字段一致；注册成功仅一次 register、零次 login；在线 logout 真实调用且旧会话不可再用。它们不是等待 P0/P1 才能开始修正的功能。

邀请码先经 `/api/auth/invite/check` 验证，字段错误/失效单独建模；密码规则以服务器明确能力为准，当前只做合理客户端输入校验，不能声称服务端已强制 8 位策略。账号 key 必须以返回 user.id 确认，reauth 若返回不同身份则拒绝替换。表单错误映射 code → 中文，诊断日志只记脱敏分类，不显示原始英文 message。

## 令牌轮换、失效与离线

按 AccountKey 单飞刷新；同一账号普通请求等同一个刷新结果，跨账号互不等待。expiry 依据 accessTokenExpiresAt 与本地时钟偏差处理，不把参考文的 1 小时写死。S:`src/server/api/auth.ts`、`src/account/sessions.ts:rotate` 证明 refreshToken 会轮换，旧 token 重放可撤销会话。

- 刷新成功：先原子写新 refreshToken 密文，再更新内存 accessToken 并放行等待者；提交检查 generation。token 请求体/响应均禁止日志。
- 网络/5xx/429：不据此清凭据或判定账号被撤销。尊重实际存在的 Retry-After，缺失时给有界退避与“稍后重试”，不编造解封时间。**轮换请求若可能已执行但响应丢失，禁止盲目重放旧 refreshToken**；保留离线使用并提示重新认证，不用旧 token 轮询导致 replay 撤销。
- 刷新明确 401 UNAUTHENTICATED：标 NeedsReauth(RefreshRejected)，断开在线同步，不先删账号/vault/本机库。现行 server 没有足够错误码区分改密码/异地撤销/禁用，原因不明时用通用中文说明，P0/P1 以后才能细分。
- 重新认证为原账号的原地操作；用户名/服务器来自已验证元数据。以非阻断的“需要重新登录”横幅提供入口，允许“稍后”并继续使用本机数据，不先整屏退出。它是行为契约，不规定横幅的颜色、位置或布局。

[口袋播客 TokenHandler](https://github.com/Automattic/pocket-casts-android/blob/main/modules/services/servers/src/main/java/au/com/shiftyjelly/pocketcasts/servers/sync/TokenHandler.kt) 提供集中获取/失效端口的参考；单飞和轮换提交顺序是本方案约束，不声称该接口本身证明实现了单飞。M:`src/utils/luxAuthApi.ts` 的 flights 也验证了集中刷新的必要性。

## 原地切换、登出与移除

**切换**：用户选择已有 AccountKey → 同一个 AccountStore 串行命令暂停并卸载旧播放媒体项/焦点 → 冻结旧账号待发、停止其下载/同步和在途刷新、递增 generation → 打开目标库/验证元数据 → 提交活动指针 → 重新绑定 Flow、控制器状态与目标同步。数据在原位，不换成整屏登录页。开库失败保留最后已提交账号，状态显示失败；下一次启动从已提交指针恢复。离线也可切换到已有本地数据的账号，需认证时只标状态、不强制登录。旧队列快照留在旧库，不在新账号自动续播。

这不允许两个活跃播放器/两个同步协调器竞争；切换非当前账号的 refresh/logout 也必须按该 AccountKey 定位。可选本地模式获批后，首次连账号仅在存在本应用本地数据时询问一次显式复制，预览冲突并可取消；这与 **v1 不读取 RN 遗留数据**是两件事。未经 June 批准本地模式，不预建复制迁移器。

**登出**：先冻结账号在线业务，再在有有效 session-bound accessToken 时真实调用 `/api/auth/logout`。成功后清除该会话认证材料、销毁内存 token 与同步 key，状态 SignedOut；由用户明确决定保留或删除业务库，不复用失效会话。移除是在登出基础上删除该账号元数据与业务库，二次确认数据范围；不能删除其他账号。

离线登出不能伪装成远端已撤销。建议在同一 AccountStore 下记录有限的 PendingRevocation：只保留独立加密、不可用于业务/自动登录的撤销租约（账号/sessionId、必要认证密文、过期与尝试状态）；界面立即本地退出，联网后尽力完成 logout，再销毁租约。它是安全生命周期待办，不另造会话状态源；不放明文 token 到 Room outbox。accessToken 原则仍为仅内存，租约持久化仅保存加密 refreshToken 引用，重启后如需撤销可受限单飞换取 accessToken 再 logout；已发生轮换结果不明时不重放旧 refreshToken，提示 June/用户从另一活跃设备撤销。重新登录同一 deviceId 可能已经撤销旧 sid，待办不得误撤销新 sid，先核验租约 sessionId。

若用户选择立即抹除全部认证材料，则撤销租约也必须删除，并说明“已退出本机，远端撤销未确认”，提供以后从设备管理撤销的路径。凭据确已被拒时不能承诺能够补发 logout。租约保留期限与是否允许上述受限刷新，需 June 在账号方案审批时确认；不承诺离线远端撤销必然成功。

**设备管理**：S:`src/server/api/me.ts` 已有 GET /api/me/sessions、DELETE /api/me/sessions/:id、POST /api/me/sessions/revoke-others，以及 GET /api/me/devices、DELETE /api/me/devices/:clientId。前者是会话、后者是同步凭据；当前客户端按可靠 deviceId/session 关系呈现归属，缺字段保持分开并标未知，不按设备名猜合并。S:`src/server/server.ts:registerSyncKeyRevocation` 联动清理带 sid 的 key；不宣称旧无 sid key 同样完整联动。合并视图属于下列 P1 提案。

## server 的 P0/P1 提案（尚未实现）

**以下全部需要另开 lux-music-server PR，并须 June 批准后才能做；本 PR 不改 server，也不授权开 server PR。** 现有证据是 S:`src/server/app.ts`、`src/server/api/auth.ts`/`rateLimit.ts`/`me.ts`、`src/account/store.ts:createUser`、`src/account/sessions.ts`。P0 不意味着可以跳过授权，也不能把建议端点写进当前客户端契约。

| 优先级 | 提案（新字段/接口均非当前保证） | 验收与客户端关系 |
| --- | --- | --- |
| P0 | `/api/server-info` 增加稳定 serverId、authMethods，registration 可配置（例如建议 LUX_REGISTRATION=invite/closed） | 关闭注册时真实拒绝 register；稳定身份可不再读 `/id`；明确 minClientVersion 对不同客户端路线的适用范围 |
| P0 | 注册用户名/密码服务端统一校验，细分 USERNAME_TAKEN/INVALID_USERNAME/WEAK_PASSWORD，并公开 passwordPolicy | 不只在客户端检查；旧 server 的 INVALID_REQUEST 继续通用展示，不能假装新 code 已存在 |
| P1 | 限流改为有界时间窗口衰减并返回 Retry-After | 当前 rateLimit/cache 是失败累计 + TTL，不能给客户端伪造精确等待时间；先独立测试边界/限流退出 |
| P1 | 设备码/扫码：拟 POST /api/auth/device/start → userCode/deviceCode/expiresIn/interval；/approve 鉴权授权；/poll 返回登录凭据 | **拟议端点**，当前不存在；需另定义单次消费、过期/撤销、轮询限速、绑定 serverId 和防误授权，不根据仅有二维码图片就实现免密 |
| P1 | 设备合并视图：拟 GET /api/me/devices-v2 或扩展 sessions，统一 sid/clientId/deviceId 关系及撤销 | 一台设备的会话与 key 可追溯，未知旧 key 显式显示；现行带 sid 的联动不得回退 |

设备码方案只借鉴 [Jellyfin QuickConnectState](https://github.com/jellyfin/jellyfin-androidtv/blob/master/app/src/main/java/org/jellyfin/androidtv/auth/model/QuickConnectState.kt) 与 [Nextcloud LoginFlowV2](https://github.com/nextcloud/android/blob/master/app/src/main/java/com/owncloud/android/authentication/loginflow/LoginFlowV2.kt) 的已登录设备批准/轮询思路，不复用它们的端点或未经核实的 Lux 能力。邀请链接、账号自助导出/注销、管理员一次性密码重置、家庭档案/PIN、Passkey/WebAuthn、Restore Credentials 与 Bearer WS 放远期专项，均不纳入当前 server 保持原样的 MVP 承诺。

## 验收与尚待决定

后续小 PR 至少覆盖：三项客户端修正；两个同名不同 server 用户隔离；库打开失败/切换中杀进程/旧请求晚到；无网络与认证失败区分；刷新并发仅一次与写密文失败；轮换响应丢失不重放；注册不二次登录；reauth 取消可离线；logout 的在线撤销、离线待办、立即抹除与 sid 防串用；备份恢复/Keystore 失效后保留业务数据。验证要用真实协议 fixture 与 Room，不能只检查按钮跳转。

待 June 确认：本地模式与 v1 不做 LX、账号上限、上述撤销租约的有限保留策略、是否先做 P0/选哪些 P1、完全移除 `/id` 是否作为账号上线前置。本方案并未决定视觉入口，开发 Bot 在 Web 原型中验证这些状态与流程，由 June 签字后再实施 Android。
