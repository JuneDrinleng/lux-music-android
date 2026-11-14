# mobile 兼容与冗余审计

只读对照，不修改 mobile。证据固定为 mobile 提交 `3ab131e141ccbdc807d200a99eb216a17017c8c8`（本地工作树 `playdetail-fixed-lyric-height`）。以下路径均相对此仓库，符号用于稳定定位；在线源文件基址：[mobile 固定提交](https://github.com/JuneDrinleng/lux-music-mobile/tree/3ab131e141ccbdc807d200a99eb216a17017c8c8)。这是针对已读路径的抽样审计，不声称穷尽全部历史或确认所有功能可在线使用。

“丢弃”指原生仓库不引入该实现，并非删除 mobile。不能因为目录名称相似就认定代码重复，也不能把有消费者的设置称作死设置。

| 编号/类别 | 真实代码与观察 | 原生侧结论 |
| --- | --- | --- |
| A1 旧 API 网络 | `android/app/src/main/java/cn/lux/music/mobile/utils/UtilsModule.java:274–310`，`readUnmetered` 按 API 23 分流，`readUnmeteredLegacy` 使用已废弃 NetworkInfo | **丢弃**旧分支；minSdk 33 下直接使用 NetworkCapabilities，保留“不计费网络”判断能力 |
| A2 旧 API 相册 | `android/app/src/main/java/cn/lux/music/mobile/gallery/GalleryModule.java:43–84`，小于 29 时请求 WRITE_EXTERNAL_STORAGE、创建公共路径并写 MediaStore.DATA；29+ 改用 RELATIVE_PATH/IS_PENDING | **丢弃**旧写文件路径；后续图片导出只用 MediaStore/SAF |
| A3 旧 API 悬浮窗 | `android/app/src/main/java/cn/lux/music/mobile/lyric/LyricView.java:321–340`，26 以下 TYPE_SYSTEM_ALERT，以上 TYPE_APPLICATION_OVERLAY；另有 Android 12 后 alpha 调整 | **重新设计**悬浮歌词；丢弃 26 以下类型分支，现代系统的遮挡/触摸限制仍须验证，不能把正确权限行为当包袱删掉 |
| A4 旧 API 编解码重载 | `android/app/src/main/java/cn/lux/music/mobile/userApi/QuickJS.java:89–100`，URLDecoder.decode 按 API 33 使用 Charset 或字符串重载 | **丢弃**旧重载分支；保留脚本编码语义，边界测试验证结果一致 |
| A5 基线分支 | `android/app/src/main/java/cn/lux/music/mobile/download/DownloadWorker.java` 的 `foreground` 中分别判断 26/29；`android/app/src/main/java/cn/lux/music/mobile/utils/NotificationPermissionUtil.java` 判断 O；`android/app/src/main/java/cn/lux/music/mobile/utils/BatteryOptimizationUtil.java` 判断 M | **重新设计**前台任务、通知、电池提示，删除低于 33 的分支。前台服务/通知的当前系统要求仍需保留，不能简单移除权限 |
| A6 数据迁移 | `src/config/migrate.ts` 的 `getAllListData`、`migrateListData`、`migrateMetaData` 读取旧 key、转换旧歌曲、保存新列表；文件前部还保留注释掉的 JSON 文件迁移实现 | **丢弃**整个 RN 本地迁移链；v1 从原版 LX 同步连接歌单，不导入旧队列和设置 |
| A7 设置迁移/弱类型 | `src/config/migrateSetting.ts` 默认导出接收 `setting: any`，按 `version < 2.0` 把嵌套字段迁移为点号 key，含旧 portrait/landscape 字段 | **丢弃**迁移与旧字段；原生从有类型的设置 schema 1 开始，只维护自己未来实际发生的升级 |
| A8 存储两代协议 | `src/plugins/storage.ts` 的 `buildData`、`handleGetDataOld`、`handleGetData` 同时支持分隔符旧分片与数组新分片，`saveData` 先删再 multiSet；`src/utils/data.ts` 再包业务 key、空间和保存方法 | **重新设计**为唯一 Room 数据层与事务；不引入 AsyncStorage 分片、通用 JSON storage 或对应的多代读取器 |
| A9 多处可变列表 | `src/utils/listManage.ts` 导出可变 `userLists` 与 `allMusicList`；`src/store/list/action.ts` 的 `setUserLists` 再更新 `state.userList/state.allList` 并发全局事件；持久化方法位于 `src/utils/data.ts` | **重新设计**为 DAO → Flow → UI 的只读投影；这些是多处状态维护的具体证据，未据此断言现有代码一定丢数据 |
| A10 播放间接层 | `src/core/player/player.ts` 调用 `@/plugins/player`；`src/plugins/player/index.ts` 初始化 TrackPlayer、维护 global 初始化标志、再导出 `./utils` 方法；其 `initial` 调用 `migratePlayerCache` | **重新设计**为单一 Media3 服务实现 PlaybackControl；保留后台、焦点与队列能力，丢弃 JS/native bridge 和旧缓存迁移 |
| A11 双布局实现 | `src/screens/PlayDetail/Vertical/Lyric.tsx` 与 `src/screens/PlayDetail/Horizontal/Lyric.tsx` 都消费歌词和播放状态，各自处理列表/行渲染；竖屏还接分享选择，横屏有 PlayLine | **重新设计**为共享歌词时钟、状态和行模型，布局按窗口变化；两者并非完全相同代码，不复制两套状态控制器 |
| A12 占位模块 | `src/utils/musicSdk/xm.js` 的 getMusicUrl/getLyric/getPic/comment 均直接 reject；`src/utils/musicSdk/index.js` 仍导入/导出 xm，但可选 sources 列表没有 xm | **丢弃**失败占位适配器；未知来源显式 Unsupported，不注册永远失败的模块 |
| A13 注释残留设置 | `src/config/defaultSetting.ts` 注释的 `playDetail.isZoomActiveLrc`、`playDetail.isShowLyricProgressSetting`，以及后面实际启用的同名 progress 设置；`src/types/app_setting.d.ts` 也同时有注释与有效 progress 声明 | **丢弃**注释残留与重复声明；有效 progress 设置**保留并说明理由**：`src/screens/PlayDetail/Horizontal/Lyric.tsx` 与 `src/screens/PlayDetail/components/SettingPopup/settings/SettingLyricProgress.tsx`实际消费它。不能把这个有效设置称为死功能 |
| A14 双文件选择路径/设置 | `src/config/defaultSetting.ts` 的 `common.useSystemFileSelector`；`src/components/common/ChoosePath/index.tsx` 的 `show` 同时走 SAF 导出和自建目录浏览，失败路径可更新设置 | **重新设计**为任务明确的系统选文件/目录与持久 URI 权限；原生不提供“关闭系统选择器”开关，避免重新维护两套浏览器。该设置当前有用，不属于死设置 |
| A15 脚本桥接/动态对象 | `src/utils/nativeModules/userApi.ts` 的 `SendResponseParams.body`、`ResponseParams.result`、`RequestParams.options.data/headers` 为 any；`src/core/init/userApi/index.ts` 动态构建 `apis`、注册多种事件并维护请求 Map | **重新设计**强类型 SourceRequest/Result 和受限宿主 HTTP；**保留**QuickJS 执行 JS 的能力，因为该文件实际处理 musicUrl/lyric/pic；不保留任意对象在业务层传递 |
| A16 冲突策略与会话保护 | `src/core/sync.ts` 的 `selectSyncMode` 只记忆 merge，overwrite 每次确认；`src/plugins/sync/client/modules/list/handler.ts` 检查 sessionRemoteWritesFrozen 与 moduleReadys；`src/plugins/sync/client/client.ts` 用 sessionId 拦旧连接回调 | **保留并说明理由**：防止覆盖和跨会话写入是正确行为；以原生状态机/代际令牌实现，不能当“多余间接层”一起删掉 |

## 最低 SDK 的决策依据与边界

`android/build.gradle:8` 的 minSdk 21 解释了部分旧分支存在的背景。**June 已于 2026-11-13 决定 minSdk 33、targetSdk 36**。33 是新产品愿意缩小系统支持范围的取舍，依据是 A1–A5 的实际维护路径；不代表所有兼容问题自动消失，也不代表有用户分布统计。高于 33 的行为差异仍集中在 Android 适配边界处理。已决基线不回退至更低版本；后续只处理基线以上真实系统差异。

## 审计保留项

音频焦点、账号隔离、离线恢复、脚本取消、冲突确认是需求，不是历史包袱。新代码每新增一个设置，都应说明消费者、默认行为、持久化归属与能力条件；不从 defaultSetting 整体生成一份设置页面。本地遗留导入默认关闭，例外决策入口见 [ARCHITECTURE](ARCHITECTURE.md)。
