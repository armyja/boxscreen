package com.armyja.boxscreen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.hardware.input.InputManager;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.view.Display;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.io.ByteArrayOutputStream;

/** Runs inside Shizuku's shell-UID UserService process. */
public final class PrivilegedDisplayService extends IDisplayService.Stub {
    private static final Set<String> ALLOWED_KEYS = new HashSet<>(Arrays.asList(
            "DPAD_UP", "DPAD_DOWN", "DPAD_LEFT", "DPAD_RIGHT", "DPAD_CENTER",
            "BACK", "HOME", "VOLUME_UP", "VOLUME_DOWN", "MEDIA_PLAY_PAUSE"));

    private final Context baseContext;
    private VirtualDisplay virtualDisplay;
    private Surface outputSurface;
    private String mouseAssociationDescriptor;
    private WindowManager imeWindowManager;
    private int imePolicyDisplayId = -1;
    private int previousDisplayImePolicy = -1;
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
                    | DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                    | (1 << 10); // VIRTUAL_DISPLAY_FLAG_TRUSTED
            outputSurface = surface;
            virtualDisplay = manager.createVirtualDisplay(name, width, height, densityDpi,
                    outputSurface, flags);
            if (virtualDisplay == null || virtualDisplay.getDisplay() == null) {
                throw new IllegalStateException("Android did not create the virtual display");
            }
            int displayId = virtualDisplay.getDisplay().getDisplayId();
            setDisplayImePolicyLocal(displayId);
            return displayId;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create a shell-identity DisplayManager", e);
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
            @Override public String getPackageName() { return "com.android.shell"; }
            @Override public String getOpPackageName() { return "com.android.shell"; }
            public String getBasePackageName() { return "com.android.shell"; }
            @Override public String getAttributionTag() { return null; }
        };
    }

    private void setDisplayImePolicyLocal(int displayId) throws ReflectiveOperationException {
        imeWindowManager = (WindowManager) createShellContext()
                .getSystemService(Context.WINDOW_SERVICE);
        Method getPolicy = imeWindowManager.getClass()
                .getMethod("getDisplayImePolicy", int.class);
        Method setPolicy = imeWindowManager.getClass()
                .getMethod("setDisplayImePolicy", int.class, int.class);
        previousDisplayImePolicy = (Integer) getPolicy.invoke(imeWindowManager, displayId);
        setPolicy.invoke(imeWindowManager, displayId, 0); // DISPLAY_IME_POLICY_LOCAL
        imePolicyDisplayId = displayId;
        Log.i("BoxScreenService", "set display IME policy=LOCAL display=" + displayId
                + " previous=" + previousDisplayImePolicy);
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
        if (virtualDisplay == null) throw new IllegalStateException("Virtual display is not running");
        if (width < 1 || height < 1 || densityDpi < 1) {
            throw new IllegalArgumentException("Invalid display dimensions");
        }
        int displayId = virtualDisplay.getDisplay().getDisplayId();
        Log.i("BoxScreenService", "resize display=" + displayId + " to " + width + "x" + height
                + " densityDpi=" + densityDpi);
        virtualDisplay.resize(width, height, densityDpi);
    }

    @Override
    public void releaseDisplay() {
        clearMouseAssociation();
        restoreDisplayImePolicy();
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
    public boolean injectMouseEvent(MotionEvent event, int displayId) {
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
            Log.e("BoxScreen", "mouse event injection failed: " + cause.getMessage(), cause);
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
