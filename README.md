# BoxScreen

BoxScreen 是一个把安卓应用放到外接屏幕独立运行的 Android 主机应用，当前版本以 **OnePlus 16（PYB110）** 为目标设备。手机屏幕保留 BoxScreen 控制界面，外接屏显示所选应用。

> **当前公开版本：v1.0.1。** 目前主要针对 OnePlus 16（PYB110）开发和验证。其他 Android 设备可能可用，但外接显示、Shizuku 权限和鼠标路由行为取决于厂商系统，尚未承诺兼容。

v1.0.1 稳定性优化：虚拟屏加入 Android `FLAG_TRUSTED`，修复部分应用打开新页面时被系统窗口管理器送回手机主屏的问题。升级后请停止并重新创建虚拟屏，使新标志生效。

## 功能

- 读取外接显示器当前分辨率和刷新率，运行时创建匹配尺寸的虚拟屏，不写死显示器参数或系统显示 ID。
- 从已安装应用列表中启动应用；第一次点击会按需创建虚拟屏，之后复用当前虚拟屏。
- 横屏输出和竖屏效果一键切换。
- 虚拟屏 DPI 可调，范围 140–600，每格 10 DPI；修改后停止并重新创建虚拟屏时生效。
- 搜索、收藏应用；可选择是否显示系统应用；应用网格会随屏幕方向和可用宽度调整。
- 有外接鼠标时，**点击应用启动后**自动把鼠标转发到外接屏上的虚拟画面，并将手机屏幕调暗、显示黑屏。触摸手机可恢复黑屏和原来的窗口亮度。
- 主界面提供“手机黑屏”按钮，可随时手动黑屏；触摸手机恢复。

## 设备与要求

- 主机：OnePlus 16，型号 **PYB110**，连接支持视频输出的外接显示器。
- Android 端：Shizuku 在 PYB110 上运行，并授予 BoxScreen 权限。
- 外接鼠标为可选项。没有鼠标时仍可使用应用列表和虚拟屏功能。
- 构建环境：JDK 17 或更新版本、Android SDK Platform 36、Android Build Tools，以及仓库自带的 Gradle Wrapper。

## 快速开始

1. 在 PYB110 上启动 Shizuku，并连接外接显示器。
2. 安装并打开 BoxScreen。连接 Shizuku 时按提示授权。
3. 在“可启动的应用”中点选应用。BoxScreen 会按需创建或复用虚拟屏，并把应用启动到外接屏。
4. 若连接了鼠标，点击应用启动后会自动启用鼠标转发；手机随后进入黑屏。触摸手机即可恢复显示和原来的亮度。

更完整的界面说明、操作方法和故障处理见[使用手册](docs/USER_GUIDE_zh-CN.md)。

## 下载与安装

从 [GitHub Releases](https://github.com/armyja/boxscreen/releases/latest) 下载适用于 PYB110 的 APK。首次安装时，按 Android 提示允许该来源安装应用。官方 Release 使用单独的发布签名；若设备上装的是自行构建的 Debug 版，Android 不允许直接覆盖安装，需要先卸载旧版（会清除 BoxScreen 的本地收藏和 DPI 设置）。

## 从源码构建

```sh
./gradlew assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`。

```sh
./gradlew assembleRelease
```

正式发布构建需要在 `~/.gradle/boxscreen-release.properties` 配置 `storeFile`、`storePassword`、`keyAlias`、`keyPassword`。签名密钥和配置不在仓库中；私钥丢失后，已安装官方 Release 的用户将无法通过新签名 APK 原地更新。普通开发和调试不需要发布签名文件。

## 兼容性说明

BoxScreen 使用 Android 的公开虚拟显示能力和 Shizuku 主机服务。个别应用可能拒绝在副屏启动、强制回到主屏，或自行限制方向和窗口尺寸；这些行为由应用自身的副屏适配决定。竖屏效果是把竖屏虚拟画面旋转后铺到保持横向的视频输出上。

## 许可证

本项目使用 [MIT License](LICENSE)。
