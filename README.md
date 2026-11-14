# BoxScreen

Android TV-style display host and phone remote, built as a new project.

## Device roles

- **PYB110 host:** detects the connected display, requests Shizuku access, creates a public Android `VirtualDisplay`, launches selected installed apps on it, and serves authenticated remote commands over the local network.
- **PJF110 remote:** uses the same APK in remote mode. It only sends navigation, back/home, volume and display-mode commands. It does not use Shizuku.

The virtual display dimensions are read from the connected display's active mode at runtime. Landscape uses `W×H`; portrait uses `H×W` and rotates the output surface while keeping the external video signal in the display's native landscape mode. Display IDs are discovered at runtime and never hardcoded.

The current development hardware reports an H24T7 external monitor at 2560×1440, with 2560×1440@60/75/120 Hz modes. The external display ID observed during discovery was 21; this is not a stable identifier.

## Build

Requires Android SDK platform 36 or newer and JDK 17+. Build the debug APK with:

```sh
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Shizuku

Shizuku must be running and authorized only on the PYB110 host. On a non-root device, Android Wireless Debugging is used to restart Shizuku after a reboot. The remote phone does not need it.

## Network

The host displays its local address and a short-lived pairing code. The PJF110 remote enters both to connect. Only a small allowlist of remote commands is accepted; the remote cannot send arbitrary shell commands.
