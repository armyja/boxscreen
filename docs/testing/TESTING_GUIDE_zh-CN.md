# BoxScreen 真机自动化测试指南

本文档面向参与开发的工程师与 AI Agent，说明如何在连接外接显示器的 Android 真机上对 BoxScreen 做**端到端自动化验证**（覆盖 Shizuku 宿主服务、多屏窗口栈、自绘鼠标光标、快捷状态栏与分屏生命周期）。

> 编写与执行原则：所有显示 ID、设备序列号、包名与路径均应在运行时动态探测，不要在脚本或文档中硬编码个人环境信息。

---

## 1. 前置条件与环境变量

- Android 真机已连接支持视频输出的外接显示器。
- 设备端已安装并运行 Shizuku，且已授予 BoxScreen 使用权限。
- 主机已连接 ADB（USB 或无线调试），存在可用环境变量：

```sh
export SERIAL="<你的_adb_device_serial>"       # adb devices 查看
export JAVA_HOME="<JDK_17_或_21_安装目录>"
export ANDROID_SDK_ROOT="<Android_SDK_目录>"   # 需含 platforms/android-36 与 build-tools/*/d8
```

---

## 2. 构建、签名与 Shizuku 服务版本

1. **JDK 版本选择**：使用 **JDK 17 或 JDK 21** 构建。过新的 JDK（例如 JDK 27）在 AGP 的 `JdkImageTransform`（`jlink`）阶段可能失败：

   ```sh
   JAVA_HOME="$JAVA_HOME" ./gradlew assembleDebug
   # 或有发布签名配置时：
   JAVA_HOME="$JAVA_HOME" ./gradlew assembleRelease
   ```

2. **核对已安装 APK 签名**：覆盖安装前先确认设备上已有包的签名指纹，避免与本地 Debug/Release 签名冲突：

   ```sh
   adb -s "$SERIAL" shell dumpsys package com.armyja.boxscreen | grep -E "versionName|versionCode|signatures"
   ```

3. **修改 AIDL 必须升级服务版本号**：每次修改 `app/src/main/aidl/com/armyja/boxscreen/IDisplayService.aidl` 后，必须同步递增 `MainActivity.java` 中 `Shizuku.UserServiceArgs(...).version(...)` 的版本号，否则 Shizuku 可能继续复用旧版 Binder 进程。

4. **安装并核对进程**：

   ```sh
   adb -s "$SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
   adb -s "$SERIAL" shell am start -n com.armyja.boxscreen/.MainActivity
   adb -s "$SERIAL" shell "ps -A | grep boxscreen"
   # 预期同时看到主进程 com.armyja.boxscreen 与宿主服务进程 com.armyja.boxscreen:display
   ```

---

## 3. 动态发现三类显示 ID（严禁硬编码）

BoxScreen 测试涉及三个不同层级的 Display ID，会随插拔和会话重建变化，每次测试前必须重新探测：

| 显示对象 | 含义 | 探测命令 | 用途 |
|---|---|---|---|
| 手机主屏 | 固定 `0` | — | 控制面板所在屏 |
| 外接物理屏（逻辑 ID） | `Presentation` 窗口所在屏 | `adb -s "$SERIAL" shell "dumpsys window windows \| grep -B1 -A3 'com.armyja.boxscreen'"` | 鼠标事件注入目标（`mDisplayId=`，且窗口无 `taskId`） |
| 外接物理屏（SurfaceFlinger 物理 ID） | 硬件合成器显示 ID | `adb -s "$SERIAL" shell "dumpsys SurfaceFlinger --display-id"` | `screencap -d <sf-display-id>` 截图必须传该长整型 ID |
| 虚拟屏（逻辑 ID） | `BoxScreen` VirtualDisplay | `adb -s "$SERIAL" shell "dumpsys display \| grep -E 'BoxScreen\|mDisplayId='"` | `dumpsys activity activities` 中对应 `Display #<vd-id>` |

> 提示：`screencap -d` 传入逻辑 ID（如 `18`）会报错 `Display Id is not valid`，必须传 `SurfaceFlinger --display-id` 返回的物理 ID；而外接屏上的快捷条/光标由 `Presentation` 绘制，截虚拟屏看不到快捷条，因此视觉验证统一截**外接物理屏**。

---

## 4. 用 `MouseTest.java` 注入真实鼠标事件

### 为什么不能直接用 `adb shell input mouse`

- `adb shell input mouse motionevent` 仅支持 `DOWN` / `MOVE` / `UP` / `CANCEL`，**不支持 `ACTION_HOVER_MOVE`**。
- BoxScreen 的 `MousePointerView` 需要鼠标悬停在画面最上沿（`virtualPointerY <= statusBarRevealZone()`）持续 ≥ 100ms 才唤出状态栏；且没有前序 `ACTION_DOWN` 的裸 `MOVE` 会被 Android View 系统丢弃。
- Android 14+ 移除了公开反射的 `InputManager.getInstance()`，独立命令行工具需通过 `ServiceManager.getService("input")` 获取 `IInputManager`，并反射 `InputEvent.setDisplayId(int)` 投递 `SOURCE_MOUSE` 事件。

### 通用测试注入脚本（放到系统临时目录，不进仓库）

将下面代码保存为 `/tmp/MouseTest.java`：

```java
import android.os.IBinder;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;

import java.lang.reflect.Method;

public final class MouseTest {
    private static Object inputManager;
    private static Method inject;
    private static Method setDisplayId;
    private static int displayId = -1;
    private static int deviceId = 0;
    private static float extWidth = 2560f;
    private static float extHeight = 1440f;
    private static boolean calibrated = false;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: display <id> [size <w> <h>] [hover x y | click x y | drag x1 y1 x2 y2 | sleep ms]...");
            System.exit(1);
        }
        IBinder binder = (IBinder) Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class).invoke(null, "input");
        inputManager = Class.forName("android.hardware.input.IInputManager$Stub")
                .getMethod("asInterface", IBinder.class).invoke(null, binder);
        inject = inputManager.getClass().getMethod("injectInputEvent", InputEvent.class, int.class);
        setDisplayId = InputEvent.class.getDeclaredMethod("setDisplayId", int.class);
        setDisplayId.setAccessible(true);

        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d != null && !d.isVirtual() && d.supportsSource(InputDevice.SOURCE_MOUSE)) {
                deviceId = id;
                break;
            }
        }

        int i = 0;
        while (i < args.length) {
            String cmd = args[i++];
            if ("display".equals(cmd)) {
                displayId = Integer.parseInt(args[i++]);
            } else if ("size".equals(cmd)) {
                extWidth = Float.parseFloat(args[i++]);
                extHeight = Float.parseFloat(args[i++]);
            } else if ("hover".equals(cmd)) {
                moveTo(Float.parseFloat(args[i++]), Float.parseFloat(args[i++]));
            } else if ("click".equals(cmd)) {
                float x = Float.parseFloat(args[i++]);
                float y = Float.parseFloat(args[i++]);
                moveTo(x, y);
                SystemClock.sleep(40);
                long down = SystemClock.uptimeMillis();
                sendTouch(down, down, MotionEvent.ACTION_DOWN, x, y, MotionEvent.BUTTON_PRIMARY);
                SystemClock.sleep(50);
                sendTouch(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0);
            } else if ("drag".equals(cmd)) {
                float x1 = Float.parseFloat(args[i++]);
                float y1 = Float.parseFloat(args[i++]);
                float x2 = Float.parseFloat(args[i++]);
                float y2 = Float.parseFloat(args[i++]);
                int steps = 12;
                moveTo(x1, y1);
                SystemClock.sleep(40);
                long down = SystemClock.uptimeMillis();
                sendTouch(down, down, MotionEvent.ACTION_DOWN, x1, y1, MotionEvent.BUTTON_PRIMARY);
                for (int s = 1; s <= steps; s++) {
                    SystemClock.sleep(16);
                    float cx = x1 + (x2 - x1) * s / steps;
                    float cy = y1 + (y2 - y1) * s / steps;
                    sendTouch(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, cx, cy, MotionEvent.BUTTON_PRIMARY);
                }
                SystemClock.sleep(30);
                sendTouch(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x2, y2, 0);
            } else if ("sleep".equals(cmd)) {
                SystemClock.sleep(Long.parseLong(args[i++]));
            } else {
                throw new IllegalArgumentException("Unknown command: " + cmd);
            }
        }
    }

    private static void moveTo(float x, float y) throws Exception {
        if (displayId < 0) throw new IllegalStateException("Specify display <id> first");
        if (!calibrated) {
            // 归零校准：MousePointerView 使用相对位移增量更新虚拟坐标。
            // 先推到右下角再回到 (0, 0) 会利用边界夹紧把内部虚拟坐标与 lastRaw 同时归零，
            // 总耗时约 20ms（远小于状态栏 100ms 隐藏延迟），不会误关已打开的面板。
            sendHover(extWidth - 1f, extHeight - 1f);
            SystemClock.sleep(10);
            sendHover(0f, 0f);
            SystemClock.sleep(10);
            calibrated = true;
        }
        sendHover(x, y);
    }

    private static void sendHover(float x, float y) throws Exception {
        long now = SystemClock.uptimeMillis();
        MotionEvent.PointerProperties pp = new MotionEvent.PointerProperties();
        pp.id = 0;
        pp.toolType = MotionEvent.TOOL_TYPE_MOUSE;
        MotionEvent.PointerCoords pc = new MotionEvent.PointerCoords();
        pc.x = x;
        pc.y = y;
        MotionEvent ev = MotionEvent.obtain(now, now, MotionEvent.ACTION_HOVER_MOVE,
                1, new MotionEvent.PointerProperties[]{pp}, new MotionEvent.PointerCoords[]{pc},
                0, 0, 1f, 1f, deviceId, 0, InputDevice.SOURCE_MOUSE, 0);
        setDisplayId.invoke(ev, displayId);
        inject.invoke(inputManager, ev, 2);
        ev.recycle();
    }

    private static void sendTouch(long downTime, long eventTime, int action, float x, float y, int buttons) throws Exception {
        MotionEvent.PointerProperties pp = new MotionEvent.PointerProperties();
        pp.id = 0;
        pp.toolType = MotionEvent.TOOL_TYPE_MOUSE;
        MotionEvent.PointerCoords pc = new MotionEvent.PointerCoords();
        pc.x = x;
        pc.y = y;
        pc.pressure = action == MotionEvent.ACTION_UP ? 0f : 1f;
        MotionEvent ev = MotionEvent.obtain(downTime, eventTime, action,
                1, new MotionEvent.PointerProperties[]{pp}, new MotionEvent.PointerCoords[]{pc},
                0, buttons, 1f, 1f, deviceId, 0, InputDevice.SOURCE_MOUSE, 0);
        setDisplayId.invoke(ev, displayId);
        inject.invoke(inputManager, ev, 2);
        ev.recycle();
    }
}
```

### 编译与推送到设备

```sh
D8_BIN=$(ls -d "$ANDROID_SDK_ROOT"/build-tools/*/d8 | tail -n 1)
mkdir -p /tmp/boxscreen-mousetest
"$JAVA_HOME/bin/javac" --release 11 \
  -cp "$ANDROID_SDK_ROOT/platforms/android-36/android.jar" \
  -d /tmp/boxscreen-mousetest /tmp/MouseTest.java
"$D8_BIN" --output /tmp/mousetest.jar /tmp/boxscreen-mousetest/*.class
adb -s "$SERIAL" push /tmp/mousetest.jar /data/local/tmp/mousetest.jar
```

执行单次交互链路示例（在一次 `app_process` 调用里串完悬停、下拉、点击）：

```sh
adb -s "$SERIAL" shell "CLASSPATH=/data/local/tmp/mousetest.jar app_process /data/local/tmp MouseTest \
  display <ext-display-id> size <ext-w> <ext-h> \
  hover 1280 1 sleep 250 drag 500 36 500 220 sleep 250"
```

---

## 5. 坐标换算与手势避坑要点

在 `MousePointerView` 中，所有控件尺寸随当前 `panelDensityDpi` 缩放：

```text
panelDp(dp) = round(dp * panelDensityDpi / 160)
```

可通过 `adb -s "$SERIAL" shell "dumpsys window displays | grep -E 'Display: mDisplayId|cur='"` 查看虚拟屏当前宽高与 DPI。

1. **状态栏与快捷面板手势坐标（以 2560×1440、240 DPI 为例，`panelDp = 1.5x`）**：
   - 状态栏高度：`panelDp(48)`（72px），中心 `y ≈ 36`。
   - 顶边悬停唤出区：`y <= panelDp(3)`（建议悬停 `y = 1` 并等待 ≥ 250ms）。
   - 右上角关闭按钮 `×`：`x` 在 `[width - panelDp(44), width]`，中心约为 `(width - panelDp(22), panelDp(24))`。
   - 最近任务图标槽：从 `closeLeft - panelDp(8)` 向左排布，每槽宽 `panelDp(42)`，最多 16 槽（第 `0` 槽在最左侧）。
   - **下拉面板防误触**：下拉展开快捷面板必须从状态栏**非图标区**起手（如时钟/空白处 `x = 120..500`）；若起点落在某个最近任务图标上并向下拖动，会直接触发该应用的"下拖关闭"手势。

2. **点击动态槽位前的确认**：状态栏最近任务顺序与应用列表排序（收藏优先 + 最近使用）可能随操作变化。点击特定槽位或面板卡片前，建议先 `screencap -d <sf-display-id>` 拉一张截图目视核对，再下单点或拖拽坐标。

3. **竖屏模式坐标换算**：
   - 切到竖屏后，虚拟画面宽高互换（`virtualWidth = baseHeight`，`virtualHeight = baseWidth`），`panelDensityDpi` 切换为竖屏保存的 DPI（横竖屏各自独立）。
   - `MousePointerView.updateVirtualPointer()` 使用 `scale = max(virtualWidth / baseWidth, virtualHeight / baseHeight)` 映射原始鼠标增量。因此要在竖屏虚拟坐标 `(vx, vy)` 点击时，传给 `MouseTest` 的原始坐标应换算为：
     ```text
     rawX = vx / scale
     rawY = vy / scale
     ```
   - 切完横竖屏后，快捷面板仍保持展开状态，无需重复下拉（直接操作面板控件即可）。

---

## 6. 三通道交叉验证法

每次操作后不要只看单一输出，建议同时核对三个通道：

1. **任务栈真值（窗口组织与前后台）**

   ```sh
   adb -s "$SERIAL" shell "dumpsys activity activities | grep -E 'Display #<vd-display-id>|BoxScreenSplit|topResumedActivity|visible=|mode='"
   ```

   重点检查：
   - 分屏在前台：`BoxScreenSplit` 及 `BoxScreenSplitFirst`、`BoxScreenSplitSecond` 均为 `visible=true visibleRequested=true`，两栏各自有 `topResumedActivity`。
   - 分屏在后台保活：`C` 在顶部 `visible=true` 且为 `topResumedActivity`，`BoxScreenSplit` 仍在 `Display #<vd-id>` 栈内且整体为 `visible=false visibleRequested=false`。
   - 分屏解散：不再出现 `BoxScreenSplit` 容器，原两栏任务变为普通 `mode=fullscreen` 根任务。

2. **视觉真值（外接物理屏截图）**

   ```sh
   adb -s "$SERIAL" shell "screencap -d <sf-display-id> /data/local/tmp/verify.png"
   adb -s "$SERIAL" pull /data/local/tmp/verify.png /tmp/verify.png
   ```

   重点检查：
   - 状态栏交叠双图标（左上/右下偏移、深色隔离描边、无对应单应用图标）。
   - 分屏在后台且前台是半透明窗口时，画面不得透出后台分屏内容。
   - 横屏/竖屏旋转并从后台恢复分屏后，两栏裁剪与位置无错位、无黑边。

3. **进程与最近任务真值（关闭/强停验证）**

   ```sh
   adb -s "$SERIAL" shell "ps -A | grep -E '<pkgA>|<pkgB>'"
   adb -s "$SERIAL" shell "dumpsys activity recents | grep -E 'Recent #|<pkgA>|<pkgB>'"
   ```

   > 说明：Chromium 内核浏览器被 `am force-stop` + `removeTask` 清理后，系统仍可能保留 `*_zygote_native` 空闲孵化进程，不代表应用任务仍存活，以主进程消失及任务不再出现在 `Display #<vd-id>` 与 `recents` 列表中为准。

---

## 7. 分屏与状态栏回归测试清单

| 场景 | 操作步骤 | 期望结果 |
|---|---|---|
| 1. 建分屏与组合图标 | 打开 A → 打开快捷面板拖入 B 进分屏 → 悬停唤出状态栏 | `BoxScreenSplit` 激活，状态栏仅显示 `[A+B]` 交叠图标（A 左上、B 右下），不显示独立 A/B |
| 2. 切前台开 C 保活 | 打开第三方全屏（及带半透明属性的）应用 C | C 位于前台全屏，画面无透底；`BoxScreenSplit` 保留在虚拟屏底部且 `visible=false`；状态栏显示 `[C, [A+B], ...]` |
| 3. 恢复与旋转重排 | 切到 C 时先切竖屏，确认分屏未弹出，再点状态栏 `[A+B]`；切回横屏再测一次 | 切方向时后台分屏不抢前台；点击 `[A+B]` 后按最新方向尺寸恢复双栏显示 |
| 4. 状态栏 `×` 隔离 | A+B 在后台、C 在前台时，点状态栏右上角 `×` | 仅关闭 C，`BoxScreenSplit` 继续保留在后台 |
| 5. 直接单开 A 拆散 | A+B 在后台、C 在前台时，从面板或应用列表直接启动 A | `BoxScreenSplit` 解散；A 切到前台全屏；B 作为普通全屏任务保留在后台；状态栏恢复独立图标 |
| 6. 组合图标下拖双杀 | 分别在 **A+B 后台** 与 **A+B 前台** 状态下，向下拖动状态栏 `[A+B]` 图标 | A 和 B 两个进程均被强停并移除任务，分屏容器关闭；后台双杀时不影响前台 C |
| 7. 边界与交换 | 在分屏中点滑块交换左右/上下栏；只打开 A 进入选应用态（半分屏）时直接开 C | 交换后状态栏双图标左上/右下顺序同步翻转；半分屏切 C 不保留残缺组合 |

---

## 8. 现场卫生与安全规范（Human & AI Agent 通用）

1. **避免破坏用户当前会话**：在连接调试前先查看虚拟屏前台正在运行什么应用（尤其当用户正通过某通讯/远控应用操作本机时），测试时挑选其他独立应用作为 A、B、C，**严禁对用户正在使用的应用执行强停或下拖关闭**。
2. **还原现场与清理设备临时文件**：测试结束后关闭临时拉起的测试应用、切回测试前的前台应用，并清理设备临时目录：

   ```sh
   adb -s "$SERIAL" shell "rm -f /data/local/tmp/mousetest.jar /data/local/tmp/*.png /data/local/tmp/*.xml"
   ```

3. **保持仓库干净**：`MouseTest.java`、`.jar`、截图等产物统一放在系统 `/tmp`（或工作区外的独立临时目录），切勿留在 Git 仓库内。
