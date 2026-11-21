# BoxScreen v1.0.16 · 一加 8 / 一加 16

本版将最近任务读取与分屏容器创建分开，并根据系统运行时提供的接口选择分屏实现：

- **新增 OnePlus 8 适配**：支持型号 IN2010 上的第三方 Android 16 系统（API 36，构建号 `BP4A.251205.006`）。该支持针对此系统构建，不代表原厂 OxygenOS 或所有第三方 ROM。
- **保留 OnePlus 16 现有实现**：PYB110 上继续使用原有 `TaskCreationParams` 分屏创建路径；OnePlus 8 使用系统提供的 `createRootTask` 与任务组织回调创建分屏区域。
- **独立读取最近任务**：最近任务读取不再依赖分屏会话是否已经创建，也不依赖所选的分屏容器后端。

适配设备与系统：

- OnePlus 16（PYB110），Android 17，构建号 `PYB110_17.0.0.157(CN01)`。
- OnePlus 8（IN2010），第三方 Android 16，API 36，构建号 `BP4A.251205.006`。

需要 Shizuku 授权。APK 使用项目正式发布签名，可覆盖升级并保留设置。

- APK：`BoxScreen-v1.0.16-OnePlus8-IN2010-OnePlus16-PYB110.apk`
- 版本：`versionName 1.0.16`，`versionCode 19`
- SHA-256：`6ed8d1996da673f34b53e70fc2f3a5bb2a17e05e5499a2f0cef89984a9a2c96c`

安装与操作见 [README](https://github.com/armyja/boxscreen/blob/main/README.md) 和[中文使用手册](https://github.com/armyja/boxscreen/blob/main/docs/USER_GUIDE_zh-CN.md)。
