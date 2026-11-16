package com.armyja.boxscreen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.hardware.display.VirtualDisplayConfig;
import android.hardware.input.InputManager;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/** Runs inside Shizuku's shell-UID UserService process. */
public final class PrivilegedDisplayService extends IDisplayService.Stub {
    private static final int IME_POLICY_RETRY_ATTEMPTS = 8;
    private static final long IME_POLICY_RETRY_DELAY_MS = 100;
    private static final Set<String> ALLOWED_KEYS = new HashSet<>(Arrays.asList(
            "DPAD_UP", "DPAD_DOWN", "DPAD_LEFT", "DPAD_RIGHT", "DPAD_CENTER",
            "BACK", "HOME", "VOLUME_UP", "VOLUME_DOWN", "MEDIA_PLAY_PAUSE"));

    private final Context baseContext;
    private VirtualDisplay virtualDisplay;
    private Object virtualDevice;
    private static final String ASSOCIATION_MAC = "02:42:4f:58:53:02";
    private static final String ASSOCIATION_PROFILE = "android.app.role.COMPANION_DEVICE_APP_STREAMING";
    private int associationId = -1;
    private Surface outputSurface;
    private String mouseAssociationDescriptor;
    private WindowManager imeWindowManager;
    private int imePolicyDisplayId = -1;
    private int previousDisplayImePolicy = -1;
    public PrivilegedDisplayService(Context context) {
        useShellIdentity();
        baseContext = context;
    }

    public PrivilegedDisplayService() {
        useShellIdentity();
        baseContext = null;
    }

    private static void useShellIdentity() {
        if (android.os.Process.myUid() != 0) return;
        try {
            android.system.Os.setgid(2000);
            android.system.Os.setuid(2000);
            Log.i("BoxScreenService", "user service switched to shell uid=" + android.os.Process.myUid());
        } catch (android.system.ErrnoException e) {
            throw new IllegalStateException("Cannot switch display service to shell identity", e);
        }
    }

    @Override
    public int createDisplay(String name, int width, int height, int densityDpi, Surface surface,
            boolean blockPointerCapture) {
        if (width < 1 || height < 1 || densityDpi < 1 || surface == null || !surface.isValid()) {
            throw new IllegalArgumentException("Invalid display dimensions or surface");
        }
        releaseDisplay();
        int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
                | DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                | (1 << 10); // VIRTUAL_DISPLAY_FLAG_TRUSTED
        try {
            outputSurface = surface;
            virtualDisplay = blockPointerCapture
                    ? createCaptureIneligibleDisplay(name, width, height, densityDpi, outputSurface, flags)
                    : createShellDisplayManager().createVirtualDisplay(name, width, height,
                            densityDpi, outputSurface, flags);
            if (virtualDisplay == null || virtualDisplay.getDisplay() == null) {
                throw new IllegalStateException("Android did not create the virtual display");
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.e("BoxScreenService", "failed to create virtual display", e);
            cleanupPartialDisplay(e);
            throw new IllegalStateException("Cannot create virtual display: "
                    + describeFailure(e), e);
        }

        int displayId = virtualDisplay.getDisplay().getDisplayId();
        try {
            setDisplayImePolicyLocal(displayId);
            return displayId;
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.e("BoxScreenService", "failed to set local IME policy for virtual display="
                    + displayId, e);
            cleanupPartialDisplay(e);
            throw new IllegalStateException("Cannot set local IME policy for virtual display "
                    + displayId + ": " + describeFailure(e), e);
        }
    }

    private VirtualDisplay createCaptureIneligibleDisplay(String name, int width, int height,
            int densityDpi, Surface surface, int flags) throws ReflectiveOperationException {
        String[] list = {"/system/bin/cmd", "companiondevice", "list", "0"};
        String associations = run(list);
        Matcher match = Pattern.compile("Association\\{mId=(\\d+),[^\\n]*mPackageName='com\\.android\\.shell'[^\\n]*mDeviceMacAddress=" + ASSOCIATION_MAC, Pattern.CASE_INSENSITIVE).matcher(associations);
        if (!match.find()) {
            run(new String[]{"/system/bin/cmd", "companiondevice", "associate", "0",
                    "com.android.shell", ASSOCIATION_MAC, ASSOCIATION_PROFILE});
            match = Pattern.compile("Association\\{mId=(\\d+),[^\\n]*mPackageName='com\\.android\\.shell'[^\\n]*mDeviceMacAddress=" + ASSOCIATION_MAC, Pattern.CASE_INSENSITIVE).matcher(run(list));
            if (!match.find()) throw new IllegalStateException("Cannot find BoxScreen association");
        }
        associationId = Integer.parseInt(match.group(1));
        Context shell = createShellContext();
        Class<?> managerClass = Class.forName("android.companion.virtual.VirtualDeviceManager");
        Object manager = shell.getSystemService(managerClass);
        java.lang.reflect.Field service = managerClass.getDeclaredField("mService");
        service.setAccessible(true);
        manager = managerClass.getConstructors()[0].newInstance(service.get(manager), shell);
        Class<?> builderClass = Class.forName("android.companion.virtual.VirtualDeviceParams$Builder");
        Object builder = builderClass.getConstructor().newInstance();
        Object params = builderClass.getMethod("build").invoke(builder);
        Class<?> paramsClass = Class.forName("android.companion.virtual.VirtualDeviceParams");
        virtualDevice = managerClass.getMethod("createVirtualDevice", int.class, paramsClass)
                .invoke(manager, associationId, params);
        VirtualDisplayConfig config = new VirtualDisplayConfig.Builder(name, width, height, densityDpi)
                .setFlags(flags).setSurface(surface).build();
        return (VirtualDisplay) virtualDevice.getClass().getMethod("createVirtualDisplay",
                VirtualDisplayConfig.class, Executor.class, VirtualDisplay.Callback.class)
                .invoke(virtualDevice, config, null, null);
    }

    private void closeVirtualDevice() {
        if (virtualDevice != null) {
            try { virtualDevice.getClass().getMethod("close").invoke(virtualDevice); }
            catch (ReflectiveOperationException | RuntimeException e) { Log.w("BoxScreenService", "virtual device close failed", e); }
            virtualDevice = null;
        }
        if (associationId >= 0) {
            try { run(new String[]{"/system/bin/cmd", "companiondevice", "disassociate", "0", "com.android.shell", ASSOCIATION_MAC}); }
            catch (RuntimeException e) { Log.w("BoxScreenService", "association cleanup failed", e); }
            associationId = -1;
        }
    }

    private DisplayManager createShellDisplayManager() throws ReflectiveOperationException {
        if (baseContext == null) {
            throw new IllegalStateException("Shizuku UserService did not provide a Context");
        }
        Context shellContext = createShellContext();
        Constructor<DisplayManager> constructor = DisplayManager.class
                .getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        return constructor.newInstance(shellContext);
    }

    private Context createShellContext() {
        return new ContextWrapper(baseContext) {
            @Override public Context getApplicationContext() { return this; }
            @Override public String getPackageName() { return "com.android.shell"; }
            @Override public String getOpPackageName() { return "com.android.shell"; }
            @Override public android.content.AttributionSource getAttributionSource() {
                return new android.content.AttributionSource.Builder(2000).setPackageName("com.android.shell").build();
            }
            public String getBasePackageName() { return "com.android.shell"; }
            @Override public String getAttributionTag() { return null; }
        };
    }

    private void setDisplayImePolicyLocal(int displayId) throws ReflectiveOperationException {
        WindowManager windowManager = (WindowManager) createShellContext()
                .getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            throw new IllegalStateException("WindowManager is unavailable in shell context");
        }
        Method getPolicy = windowManager.getClass()
                .getMethod("getDisplayImePolicy", int.class);
        Method setPolicy = windowManager.getClass()
                .getMethod("setDisplayImePolicy", int.class, int.class);
        int previousPolicy = -1;
        InvocationTargetException lastInvocationFailure = null;
        for (int attempt = 1; attempt <= IME_POLICY_RETRY_ATTEMPTS; attempt++) {
            try {
                if (previousPolicy < 0) {
                    previousPolicy = (Integer) getPolicy.invoke(windowManager, displayId);
                    imeWindowManager = windowManager;
                    imePolicyDisplayId = displayId;
                    previousDisplayImePolicy = previousPolicy;
                }
                setPolicy.invoke(windowManager, displayId, 0); // DISPLAY_IME_POLICY_LOCAL
                Log.i("BoxScreenService", "set display IME policy=LOCAL display=" + displayId
                        + " previous=" + previousPolicy + " attempt=" + attempt);
                return;
            } catch (InvocationTargetException e) {
                lastInvocationFailure = e;
                Log.w("BoxScreenService", "IME policy attempt " + attempt + "/"
                        + IME_POLICY_RETRY_ATTEMPTS + " failed for display=" + displayId
                        + ": " + describeFailure(e), e);
                if (attempt < IME_POLICY_RETRY_ATTEMPTS) {
                    SystemClock.sleep(IME_POLICY_RETRY_DELAY_MS);
                }
            }
        }
        throw new ReflectiveOperationException("IME policy retries exhausted for display="
                + displayId + " after " + IME_POLICY_RETRY_ATTEMPTS + " attempts",
                lastInvocationFailure);
    }

    private void cleanupPartialDisplay(Throwable originalFailure) {
        clearMouseAssociation();
        restoreDisplayImePolicy();
        if (virtualDisplay != null) {
            try {
                virtualDisplay.release();
            } catch (RuntimeException cleanupFailure) {
                originalFailure.addSuppressed(cleanupFailure);
                Log.e("BoxScreenService", "failed to release partially created virtual display",
                        cleanupFailure);
            } finally {
                virtualDisplay = null;
            }
        }
        closeVirtualDevice();
        if (outputSurface != null) {
            try {
                outputSurface.release();
            } catch (RuntimeException cleanupFailure) {
                originalFailure.addSuppressed(cleanupFailure);
                Log.e("BoxScreenService", "failed to release partial display surface",
                        cleanupFailure);
            } finally {
                outputSurface = null;
            }
        }
    }

    private static String describeFailure(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }

    private void restoreDisplayImePolicy() {
        if (imeWindowManager == null || imePolicyDisplayId < 0 || previousDisplayImePolicy < 0) return;
        try {
            Method setPolicy = imeWindowManager.getClass()
                    .getMethod("setDisplayImePolicy", int.class, int.class);
            setPolicy.invoke(imeWindowManager, imePolicyDisplayId, previousDisplayImePolicy);
            Log.i("BoxScreenService", "restored display IME policy=" + previousDisplayImePolicy
                    + " display=" + imePolicyDisplayId);
        } catch (Exception e) {
            Log.e("BoxScreenService", "failed to restore display IME policy", e);
        } finally {
            imeWindowManager = null;
            imePolicyDisplayId = -1;
            previousDisplayImePolicy = -1;
        }
    }

    @Override
    public void resizeDisplay(int width, int height, int densityDpi) {
        resizeDisplayInternal(width, height, densityDpi, true);
    }

    @Override
    public void resizeDisplaySize(int width, int height, int densityDpi) {
        resizeDisplayInternal(width, height, densityDpi, false);
    }

    private void resizeDisplayInternal(int width, int height, int densityDpi,
                                       boolean forceDensityOverride) {
        if (virtualDisplay == null) throw new IllegalStateException("Virtual display is not running");
        if (width < 1 || height < 1 || densityDpi < 1) {
            throw new IllegalArgumentException("Invalid display dimensions");
        }
        int displayId = virtualDisplay.getDisplay().getDisplayId();
        Log.i("BoxScreenService", "resize display=" + displayId + " to " + width + "x" + height
                + " densityDpi=" + densityDpi);
        virtualDisplay.resize(width, height, densityDpi);
        if (!forceDensityOverride) return;
        try {
            String result = run(new String[]{"/system/bin/wm", "density", Integer.toString(densityDpi),
                    "-d", Integer.toString(displayId)});
            Log.i("BoxScreenService", "wm density display=" + displayId + " dpi=" + densityDpi
                    + " result=" + result);
        } catch (RuntimeException e) {
            Log.w("BoxScreenService", "could not force density refresh for display=" + displayId, e);
        }
    }

    @Override
    public void setDisplaySurface(Surface surface) {
        if (virtualDisplay == null) {
            throw new IllegalStateException("Virtual display is not running");
        }
        if (surface != null && !surface.isValid()) {
            throw new IllegalArgumentException("Display surface is invalid");
        }
        int displayId = virtualDisplay.getDisplay().getDisplayId();
        Surface previousSurface = outputSurface;
        virtualDisplay.setSurface(surface);
        outputSurface = surface;
        if (previousSurface != null && previousSurface != surface) {
            previousSurface.release();
        }
        Log.i("BoxScreenService", (surface == null ? "detached" : "attached")
                + " output surface display=" + displayId);
    }

    @Override
    public void releaseDisplay() {
        clearMouseAssociation();
        restoreDisplayImePolicy();
        if (virtualDisplay != null) {
            int displayId = virtualDisplay.getDisplay().getDisplayId();
            try {
                run(new String[]{"/system/bin/wm", "density", "reset", "-d",
                        Integer.toString(displayId)});
            } catch (RuntimeException e) {
                Log.w("BoxScreenService", "could not reset density override for display=" + displayId, e);
            }
            virtualDisplay.release();
            virtualDisplay = null;
        }
        closeVirtualDevice();
        if (outputSurface != null) {
            outputSurface.release();
            outputSurface = null;
        }
    }

    @Override
    @SuppressLint("BlockedPrivateApi")
    public boolean setMouseDisplay(String inputDescriptor, int displayId) {
        if (inputDescriptor == null || inputDescriptor.isEmpty()) {
            throw new IllegalArgumentException("Mouse descriptor is empty");
        }
        try {
            InputManager inputManager = (InputManager) baseContext.getSystemService(Context.INPUT_SERVICE);
            if (inputManager == null) throw new IllegalStateException("InputManager is unavailable");
            if (displayId < 0) {
                Method remove = InputManager.class.getDeclaredMethod(
                        "removeUniqueIdAssociationByDescriptor", String.class);
                remove.setAccessible(true);
                remove.invoke(inputManager, inputDescriptor);
                if (inputDescriptor.equals(mouseAssociationDescriptor)) mouseAssociationDescriptor = null;
                return false;
            }
            DisplayManager manager = createShellDisplayManager();
            Display target = manager.getDisplay(displayId);
            if (target == null) throw new IllegalStateException("Target display is unavailable");
            Method getUniqueId = Display.class.getDeclaredMethod("getUniqueId");
            getUniqueId.setAccessible(true);
            String uniqueId = (String) getUniqueId.invoke(target);
            if (uniqueId == null || uniqueId.isEmpty()) {
                throw new IllegalStateException("Target display has no unique ID");
            }
            if (mouseAssociationDescriptor != null
                    && !mouseAssociationDescriptor.equals(inputDescriptor)) clearMouseAssociation();
            Method add = InputManager.class.getDeclaredMethod(
                    "addUniqueIdAssociationByDescriptor", String.class, String.class);
            add.setAccessible(true);
            add.invoke(inputManager, inputDescriptor, uniqueId);
            mouseAssociationDescriptor = inputDescriptor;
            return true;
        } catch (ReflectiveOperationException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new IllegalStateException("Unable to route mouse: " + cause.getMessage(), cause);
        }
    }

    @Override
    @SuppressLint("BlockedPrivateApi")
    public boolean injectInputEvent(MotionEvent event, int displayId) {
        if (event == null || virtualDisplay == null || virtualDisplay.getDisplay() == null
                || displayId != virtualDisplay.getDisplay().getDisplayId()) {
            return false;
        }
        MotionEvent forwarded = null;
        try {
            int pointerCount = event.getPointerCount();
            MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[pointerCount];
            MotionEvent.PointerCoords[] coordinates = new MotionEvent.PointerCoords[pointerCount];
            for (int i = 0; i < pointerCount; i++) {
                properties[i] = new MotionEvent.PointerProperties();
                coordinates[i] = new MotionEvent.PointerCoords();
                event.getPointerProperties(i, properties[i]);
                event.getPointerCoords(i, coordinates[i]);
            }
            forwarded = MotionEvent.obtain(event.getDownTime(), event.getEventTime(),
                    event.getAction(), pointerCount, properties, coordinates,
                    event.getMetaState(), event.getButtonState(), event.getXPrecision(),
                    event.getYPrecision(), event.getDeviceId(), event.getEdgeFlags(),
                    event.getSource(), event.getFlags());
            Method setDisplayId = InputEvent.class.getDeclaredMethod("setDisplayId", int.class);
            setDisplayId.setAccessible(true);
            setDisplayId.invoke(forwarded, displayId);
            InputManager inputManager = (InputManager) baseContext.getSystemService(Context.INPUT_SERVICE);
            if (inputManager == null) throw new IllegalStateException("InputManager is unavailable");
            Method inject = InputManager.class.getDeclaredMethod("injectInputEvent", InputEvent.class, int.class);
            inject.setAccessible(true);
            Object accepted = inject.invoke(inputManager, forwarded, 0);
            return Boolean.TRUE.equals(accepted);
        } catch (ReflectiveOperationException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            Log.e("BoxScreen", "input event injection failed: " + cause.getMessage(), cause);
            return false;
        } finally {
            if (forwarded != null) forwarded.recycle();
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

    @Override
    @SuppressLint("BlockedPrivateApi")
    public boolean closeTopTaskOnVirtualDisplay() {
        if (virtualDisplay == null || virtualDisplay.getDisplay() == null) {
            throw new IllegalStateException("Virtual display is not running");
        }
        int displayId = virtualDisplay.getDisplay().getDisplayId();
        int taskId = findTopTaskId(displayId);
        if (taskId < 0) {
            Log.w("BoxScreenService", "no foreground app task on virtual display=" + displayId);
            return false;
        }
        try {
            Class<?> activityTaskManager = Class.forName("android.app.ActivityTaskManager");
            Method getService = activityTaskManager.getDeclaredMethod("getService");
            getService.setAccessible(true);
            Object service = getService.invoke(null);
            Method removeTask = service.getClass().getMethod("removeTask", int.class);
            boolean removed = Boolean.TRUE.equals(removeTask.invoke(service, taskId));
            Log.i("BoxScreenService", "remove virtual display task=" + taskId
                    + " display=" + displayId + " result=" + removed);
            return removed;
        } catch (ReflectiveOperationException | RuntimeException e) {
            Throwable cause = e instanceof InvocationTargetException && e.getCause() != null
                    ? e.getCause() : e;
            Log.e("BoxScreenService", "failed to remove task=" + taskId
                    + " from display=" + displayId, cause);
            throw new IllegalStateException("Unable to close virtual display app: "
                    + cause.getMessage(), cause);
        }
    }

    private int findTopTaskId(int displayId) {
        Process process = null;
        try {
            process = new ProcessBuilder("/system/bin/dumpsys", "activity", "activities")
                    .redirectErrorStream(true).start();
            boolean inTargetDisplay = false;
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Display #")) {
                        inTargetDisplay = line.startsWith("Display #" + displayId + " ");
                        continue;
                    }
                    if (!inTargetDisplay || !line.contains("topResumedActivity=ActivityRecord{")) continue;
                    int marker = line.lastIndexOf(" t");
                    int end = line.indexOf('}', marker);
                    if (marker < 0 || end <= marker + 2) continue;
                    if (line.contains(" com.armyja.boxscreen/")) return -1;
                    try {
                        return Integer.parseInt(line.substring(marker + 2, end));
                    } catch (NumberFormatException ignored) { }
                }
            }
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Timed out while locating virtual display task");
            }
            if (process.exitValue() != 0) throw new IllegalStateException("dumpsys activity failed");
            return -1;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect virtual display task: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while inspecting virtual display task", e);
        } finally {
            if (process != null) process.destroy();
        }
    }

    @Override public String getPrimaryDensity() {
        return run(new String[]{"/system/bin/wm", "density", "-d", "0"});
    }

    @Override public void setPrimaryDensity(int dpi) {
        if (dpi != 0 && (dpi < 72 || dpi > 10000)) throw new IllegalArgumentException("Invalid density");
        run(new String[]{"/system/bin/wm", "density", dpi == 0 ? "reset" : Integer.toString(dpi), "-d", "0"});
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
