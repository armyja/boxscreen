# BoxScreen v1.0.14 · 一加 16（PYB110）

- **竖屏支持大画面缓冲**。Android 16 及更新版本上，横竖屏均按相同的 250 万像素阈值选择输出。竖屏复用直出的系统图层旋转，真机确认 2K 外屏画面铺满、鼠标点击对位，以及横竖屏来回切换后显示正常。
- **修复缓冲输出偶发掉帧**。缓冲沿用生产端的帧时间戳，取消额外设定的一个刷新周期。实际显示延迟取决于合成时序。
- **2K 真机复测**。同一 Chrome 动画页，横竖屏各采样约 12 秒，两个方向的 BoxScreen 输出层帧数均与 Chrome 图层一致。

适用于一加 16（PYB110），需要 Shizuku 授权。APK 使用项目正式签名，可覆盖升级并保留设置。

- APK：`BoxScreen-v1.0.14-OnePlus16-PYB110.apk`
- 版本：`versionName 1.0.14`，`versionCode 17`
- SHA-256：`a3436e7330c4e0b9545ec21620775613d2ff5672ec53ec25f8b98de938febf06`

安装与操作见 [README](../../README.md) 和[中文使用手册](../guides/USER_GUIDE_zh-CN.md)。
