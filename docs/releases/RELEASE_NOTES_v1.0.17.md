# BoxScreen v1.0.17 · 一加 8 / 一加 16

本版修复 OnePlus 8 上启动 UU 远程时的虚拟屏创建问题：

- 兼容系统以表格或 `Association{...}` 对象格式返回的 Companion Device 关联列表。
- 复用已有的 BoxScreen 系统关联；只有找不到时才创建，避免每次失败重试都累积重复记录。
- 退出时只清理本次运行创建的关联，不删除之前已存在的关联。
- 保留 OnePlus 16 的原有虚拟屏、分屏和最近任务实现。普通应用仍走原有创建路径。

支持的设备与系统：

- OnePlus 16（PYB110），Android 17，构建号 `PYB110_17.0.0.157(CN01)`。
- OnePlus 8（IN2010），第三方 Android 16，API 36，构建号 `BP4A.251205.006`。此支持针对该系统构建，不代表原厂 OxygenOS 或所有第三方 ROM。

需要 Shizuku 授权。APK 使用项目正式发布签名，可覆盖升级并保留设置。

- APK：`BoxScreen-v1.0.17-OnePlus8-IN2010-OnePlus16-PYB110.apk`
- 版本：`versionName 1.0.17`，`versionCode 20`
- SHA-256：`22f1c976e3243922e0de32f7cbacc2637794d03097b2de6479351c20a41ed368`

安装与操作见 [README](../../README.md) 和[中文使用手册](../guides/USER_GUIDE_zh-CN.md)。
