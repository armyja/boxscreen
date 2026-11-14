# BoxScreen

Android virtual-display host for PYB110.

## Device roles

- **PYB110 host:** detects the connected display, requests Shizuku access, creates a public Android `VirtualDisplay`, and launches selected installed apps on it. Shizuku is used only on this device.

The virtual display dimensions are read from the connected display's active mode at runtime. Landscape uses `W×H`; portrait uses `H×W` and rotates the output surface while keeping the external video signal in the display's native landscape mode. Display IDs are discovered at runtime and never hardcoded.

Mouse control keeps the physical mouse associated with the external display so its visible pointer stays there. BoxScreen draws and auto-hides its own pointer over the output, then forwards mouse events to the virtual display. The event transform follows the output rotation, so the pointer and clicks use the virtual app's coordinates in both landscape and portrait modes.

The current development hardware reports an H24T7 external monitor at 2560×1440. Display IDs change at runtime and must not be hardcoded.

## Build

Requires Android SDK platform 36 or newer and JDK 17+. Build the debug APK with:

```sh
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Shizuku

Shizuku must be running and authorized only on the PYB110 host. On a non-root device, Android Wireless Debugging is used to restart Shizuku after a reboot. The remote phone does not need it.

## Network

The project currently provides the PYB110 display host only.
