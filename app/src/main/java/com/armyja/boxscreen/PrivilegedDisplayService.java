package com.armyja.boxscreen;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.hardware.input.InputManager;
import android.os.Binder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/** Runs inside Shizuku's shell-UID UserService process. */
public final class PrivilegedDisplayService extends IDisplayService.Stub {
    private static final Set<String> ALLOWED_KEYS = new HashSet<>(Arrays.asList(
            "DPAD_UP", "DPAD_DOWN", "DPAD_LEFT", "DPAD_RIGHT", "DPAD_CENTER",
            "BACK", "HOME", "VOLUME_UP", "VOLUME_DOWN", "MEDIA_PLAY_PAUSE"));

    private final Context baseContext;
    private VirtualDisplay virtualDisplay;
    private Surface outputSurface;
    private String mouseAssociationDescriptor;
    private volatile boolean edgeSwitchRunning;
    private volatile Process mouseEventProcess;
    private Thread mouseEventThread;


    public PrivilegedDisplayService(Context context) {
        baseContext = context;
    }

    public PrivilegedDisplayService() {
        baseContext = null;
    }

    @Override
    public int createDisplay(String name, int width, int height, int densityDpi, Surface surface) {
        if (width < 1 || height < 1 || densityDpi < 1 || surface == null || !surface.isValid()) {
            throw new IllegalArgumentException("Invalid display dimensions or surface");
        }
        releaseDisplay();
        try {
            DisplayManager manager = createShellDisplayManager();
            int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                    | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
                    | DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;
            outputSurface = surface;
            virtualDisplay = manager.createVirtualDisplay(name, width, height, densityDpi,
                    outputSurface, flags);
            if (virtualDisplay == null || virtualDisplay.getDisplay() == null) {
                throw new IllegalStateException("Android did not create the virtual display");
            }
            return virtualDisplay.getDisplay().getDisplayId();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create a shell-identity DisplayManager", e);
        }
    }

    private DisplayManager createShellDisplayManager() throws ReflectiveOperationException {
        if (baseContext == null) {
            throw new IllegalStateException("Shizuku UserService did not provide a Context");
        }
        Context shellContext = new ContextWrapper(baseContext) {
            @Override public String getPackageName() { return "com.android.shell"; }
            @Override public String getOpPackageName() { return "com.android.shell"; }
            public String getBasePackageName() { return "com.android.shell"; }
            @Override public String getAttributionTag() { return null; }
        };
        Constructor<DisplayManager> constructor = DisplayManager.class
                .getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        return constructor.newInstance(shellContext);
    }

    @Override
    public void resizeDisplay(int width, int height, int densityDpi) {
        if (virtualDisplay == null) throw new IllegalStateException("Virtual display is not running");
        if (width < 1 || height < 1 || densityDpi < 1) {
            throw new IllegalArgumentException("Invalid display dimensions");
        }
        virtualDisplay.resize(width, height, densityDpi);
    }

    @Override
    public void releaseDisplay() {
        stopMouseEdgeSwitch();
        clearMouseAssociation();
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (outputSurface != null) {
            outputSurface.release();
            outputSurface = null;
        }
    }

    @Override
    public boolean setMouseDisplay(String inputDescriptor, boolean bindToVirtualDisplay) {
        if (inputDescriptor == null || inputDescriptor.isEmpty()) {
            throw new IllegalArgumentException("Mouse descriptor is empty");
        }
        if (bindToVirtualDisplay && (virtualDisplay == null || virtualDisplay.getDisplay() == null)) {
            throw new IllegalStateException("Virtual display is not running");
        }
        try {
            InputManager inputManager = (InputManager) baseContext.getSystemService(Context.INPUT_SERVICE);
            if (inputManager == null) throw new IllegalStateException("InputManager is unavailable");
            if (bindToVirtualDisplay) {
                if (mouseAssociationDescriptor != null
                        && !mouseAssociationDescriptor.equals(inputDescriptor)) clearMouseAssociation();
                Method add = InputManager.class.getDeclaredMethod("addUniqueIdAssociationByDescriptor",
                        String.class, String.class);
                add.setAccessible(true);
                Method getUniqueId = android.view.Display.class.getDeclaredMethod("getUniqueId");
                getUniqueId.setAccessible(true);
                String displayUniqueId = (String) getUniqueId.invoke(virtualDisplay.getDisplay());
                add.invoke(inputManager, inputDescriptor, displayUniqueId);
                mouseAssociationDescriptor = inputDescriptor;
                return true;
            }
            Method remove = InputManager.class.getDeclaredMethod("removeUniqueIdAssociationByDescriptor",
                    String.class);
            remove.setAccessible(true);
            remove.invoke(inputManager, inputDescriptor);
            if (inputDescriptor.equals(mouseAssociationDescriptor)) mouseAssociationDescriptor = null;
            return false;
        } catch (ReflectiveOperationException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new IllegalStateException("Unable to route mouse: " + cause.getMessage(), cause);
        }
    }

    private void clearMouseAssociation() {
        if (mouseAssociationDescriptor == null || baseContext == null) return;
        try {
            InputManager inputManager = (InputManager) baseContext.getSystemService(Context.INPUT_SERVICE);
            Method remove = InputManager.class.getDeclaredMethod("removeUniqueIdAssociationByDescriptor",
                    String.class);
            remove.setAccessible(true);
            remove.invoke(inputManager, mouseAssociationDescriptor);
        } catch (Exception ignored) { }
        mouseAssociationDescriptor = null;
    }

    @Override
    public void startMouseEdgeSwitch(String descriptor, int mainWidth, int mainHeight) {
        if (virtualDisplay == null || virtualDisplay.getDisplay() == null)
            throw new IllegalStateException("Virtual display is not running");
        if (descriptor == null || descriptor.isEmpty() || mainWidth < 1 || mainHeight < 1)
            throw new IllegalArgumentException("Invalid mouse or screen size");
        stopMouseEdgeSwitch();
        setMouseDisplay(descriptor, false);
        mouseAssociationDescriptor = descriptor;
        final int virtualWidth = virtualDisplay.getDisplay().getMode().getPhysicalWidth();
        final int virtualHeight = virtualDisplay.getDisplay().getMode().getPhysicalHeight();
        final String path = findInputPath(descriptor);
        edgeSwitchRunning = true;
        mouseEventThread = new Thread(() -> monitorMouse(descriptor, path, mainWidth, mainHeight,
                virtualWidth, virtualHeight), "BoxScreen-mouse-edge");
        mouseEventThread.setDaemon(true);
        mouseEventThread.start();
    }

    @Override
    public void stopMouseEdgeSwitch() {
        edgeSwitchRunning = false;
        Process process = mouseEventProcess;
        mouseEventProcess = null;
        if (process != null) process.destroyForcibly();
        clearMouseAssociation();
    }

    private String findInputPath(String descriptor) {
        String dump = run(new String[]{"/system/bin/dumpsys", "input"});
        String[] lines = dump.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].contains("Descriptor: " + descriptor)) continue;
            for (int j = i - 1; j >= Math.max(0, i - 6); j--) {
                String line = lines[j].trim();
                if (line.startsWith("Path: /dev/input/")) return line.substring("Path: ".length());
            }
        }
        throw new IllegalStateException("Cannot find mouse input node");
    }

    private void monitorMouse(String descriptor, String path, int mainWidth, int mainHeight, int virtualWidth, int virtualHeight) {
        int x = mainWidth / 2, y = mainHeight / 2, dx = 0, dy = 0;
        boolean onVirtual = false;
        try {
            Process process = new ProcessBuilder("/system/bin/getevent", "-lt", path)
                    .redirectErrorStream(true).start();
            mouseEventProcess = process;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while (edgeSwitchRunning && (line = reader.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length < 3) continue;
                    String axis = parts[parts.length - 2];
                    if ("REL_X".equals(axis)) dx += signedHex(parts[parts.length - 1]);
                    else if ("REL_Y".equals(axis)) dy += signedHex(parts[parts.length - 1]);
                    else if ("SYN_REPORT".equals(axis)) {
                        int w = onVirtual ? virtualWidth : mainWidth;
                        int h = onVirtual ? virtualHeight : mainHeight;
                        x = Math.max(0, Math.min(w - 1, x + dx));
                        y = Math.max(0, Math.min(h - 1, y + dy));
                        if (!onVirtual && y <= 1 && dy < 0) {
                            setMouseDisplay(descriptor, true);
                            onVirtual = true; x = virtualWidth / 2; y = virtualHeight - 2;
                            Log.i("BoxScreen", "mouse crossed main top edge to virtual display");
                        } else if (onVirtual && y >= h - 2 && dy > 0) {
                            setMouseDisplay(descriptor, false);
                            onVirtual = false; x = mainWidth / 2; y = 1;
                            Log.i("BoxScreen", "mouse crossed virtual bottom edge to main display");
                        }
                        dx = 0; dy = 0;
                    }
                }
            }
        } catch (Exception e) {
            if (edgeSwitchRunning) Log.e("BoxScreen", "mouse edge monitor failed: " + e.getMessage(), e);
        } finally {
            mouseEventProcess = null;
            if (edgeSwitchRunning) edgeSwitchRunning = false;
        }
    }

    private static int signedHex(String value) {
        try {
            long parsed = Long.parseLong(value, 16);
            return (int) (parsed >= 0x80000000L ? parsed - 0x100000000L : parsed);
        } catch (NumberFormatException ignored) { return 0; }
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == 16777115) {
            releaseDisplay();
            if (reply != null) reply.writeNoException();
            System.exit(0);
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }

    @Override
    public void launchActivity(String packageName, String className) {
        if (virtualDisplay == null) throw new IllegalStateException("Virtual display is not running");
        if (packageName == null || !packageName.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")
                || className == null || !className.matches("[A-Za-z0-9_.$]+")) {
            throw new IllegalArgumentException("Invalid activity component");
        }
        run(new String[]{"/system/bin/am", "start", "--display",
                Integer.toString(virtualDisplay.getDisplay().getDisplayId()),
                "-n", packageName + "/" + className});
    }

    @Override
    public void sendKey(String keyCode) {
        if (virtualDisplay == null) throw new IllegalStateException("Virtual display is not running");
        if (!ALLOWED_KEYS.contains(keyCode)) throw new IllegalArgumentException("Unsupported key");
        run(new String[]{"/system/bin/input", "-d",
                Integer.toString(virtualDisplay.getDisplay().getDisplayId()),
                "keyevent", "KEYCODE_" + keyCode});
    }

    private static String run(String[] args) {
        Process process = null;
        try {
            process = new ProcessBuilder(args).redirectErrorStream(true).start();
            if (!process.waitFor(8, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("System command timed out");
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = process.getInputStream().read(buffer)) != -1) bytes.write(buffer, 0, count);
            byte[] output = bytes.toByteArray();
            String result = new String(output).trim();
            if (process.exitValue() != 0 || result.contains("Error:") || result.contains("Exception")) {
                throw new IllegalStateException(result);
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("System command failed: " + e.getMessage(), e);
        } finally {
            if (process != null) process.destroy();
        }
    }
}
