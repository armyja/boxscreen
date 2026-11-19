package com.armyja.boxscreen;

import android.content.ComponentName;
import android.graphics.Rect;
import android.os.IBinder;
import android.view.SurfaceControl;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Owns the task containers and their surfaces for one BoxScreen display. */
final class SplitSession {
    private final int displayId;
    private final Object organizer;
    private final Object windowOrganizer;
    private final Class<?> organizerType = Class.forName("android.window.ITaskOrganizerController");
    private final Class<?> transactionType = Class.forName("android.window.WindowContainerTransaction");
    private final Class<?> tokenType = Class.forName("android.window.WindowContainerToken");
    private final Class<?> paramsType = Class.forName("android.window.TaskCreationParams");
    private final Class<?> builderType = Class.forName("android.window.TaskCreationParams$Builder");
    private Stage root;
    private Stage first;
    private Stage second;
    private Object firstAppToken;
    private Object secondAppToken;
    private boolean active;
    private float ratio = 0.5f;
    private boolean reversed;
    private long layoutGeneration;
    private final android.os.Handler surfaceHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    private record Stage(Object token, SurfaceControl surface) {}

    SplitSession(int displayId) throws ReflectiveOperationException {
        this.displayId = displayId;
        IBinder binder = (IBinder) Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class).invoke(null, "activity_task");
        Object atm = Class.forName("android.app.IActivityTaskManager$Stub")
                .getMethod("asInterface", IBinder.class).invoke(null, binder);
        windowOrganizer = Class.forName("android.app.IActivityTaskManager")
                .getMethod("getWindowOrganizerController").invoke(atm);
        organizer = Class.forName("android.window.IWindowOrganizerController")
                .getMethod("getTaskOrganizerController").invoke(windowOrganizer);
        organizerType.getMethod("createTask", paramsType);
        transactionType.getMethod("removeRootTask", tokenType);
    }

    private List<?> roots() throws ReflectiveOperationException {
        return (List<?>) organizerType.getMethod("getRootTasks", int.class, int[].class)
                .invoke(organizer, displayId, null);
    }

    Set<Integer> taskIds() throws ReflectiveOperationException {
        Set<Integer> ids = new HashSet<>();
        for (Object item : roots()) ids.add(item.getClass().getField("taskId").getInt(item));
        return ids;
    }

    Object awaitTask(String packageName, Set<Integer> before) throws ReflectiveOperationException {
        for (int attempt = 0; attempt < 15; attempt++) {
            Object reused = null;
            for (Object item : roots()) {
                ComponentName top = (ComponentName) item.getClass().getField("topActivity").get(item);
                if (top == null || !packageName.equals(top.getPackageName())) continue;
                if (!before.contains(item.getClass().getField("taskId").getInt(item))) return token(item);
                if (reused == null) reused = item;
            }
            if (reused != null && attempt >= 3) return token(reused);
            android.os.SystemClock.sleep(50);
        }
        throw new IllegalStateException("应用未在虚拟屏创建任务：" + packageName);
    }

    private Object token(Object taskInfo) throws ReflectiveOperationException {
        return taskInfo.getClass().getField("token").get(taskInfo);
    }

    private Stage create(String name, int mode, Stage parent) throws ReflectiveOperationException {
        Object builder = builderType.getConstructor().newInstance();
        builderType.getMethod("setDisplayId", int.class).invoke(builder, displayId);
        builderType.getMethod("setWindowingMode", int.class).invoke(builder, mode);
        builderType.getMethod("setName", String.class).invoke(builder, name);
        if (parent != null) {
            builderType.getMethod("setParentContainer", tokenType).invoke(builder, parent.token());
        }
        Object appeared = organizerType.getMethod("createTask", paramsType)
                .invoke(organizer, builderType.getMethod("build").invoke(builder));
        if (appeared == null) throw new IllegalStateException("系统未创建分屏区域");
        Object info = appeared.getClass().getMethod("getTaskInfo").invoke(appeared);
        SurfaceControl surface = (SurfaceControl) appeared.getClass().getMethod("getLeash").invoke(appeared);
        return new Stage(token(info), surface);
    }

    private Object transaction() throws ReflectiveOperationException {
        return transactionType.getConstructor().newInstance();
    }

    private void apply(Object tx) throws ReflectiveOperationException {
        Class.forName("android.window.IWindowOrganizerController")
                .getMethod("applyTransaction", transactionType).invoke(windowOrganizer, tx);
    }

    synchronized void open(Object firstTask, Object secondTask, int width, int height) throws ReflectiveOperationException {
        firstAppToken = firstTask;
        secondAppToken = secondTask;
        root = create("BoxScreenSplit", 1, null);
        first = create("BoxScreenSplitFirst", 6, root);
        second = create("BoxScreenSplitSecond", 6, root);
        Object tx = transaction();
        Object pair = Array.newInstance(tokenType, 2);
        Array.set(pair, 0, first.token());
        Array.set(pair, 1, second.token());
        transactionType.getMethod("setAdjacentRoots", pair.getClass()).invoke(tx, pair);
        Method reparent = transactionType.getMethod("reparent", tokenType, tokenType, boolean.class);
        reparent.invoke(tx, firstTask, first.token(), true);
        if (secondTask != null) reparent.invoke(tx, secondTask, second.token(), true);
        transactionType.getMethod("reorder", tokenType, boolean.class).invoke(tx, root.token(), true);
        Rect[] bounds = bounds(width, height);
        applyLayout(tx, bounds);
        active = true;
        // The organizer-created stages may be hidden by the opening transition.
        android.os.SystemClock.sleep(900);
        showSurfaces(bounds);
        stabilizeSurfaces(bounds);
    }

    private Rect[] bounds(int width, int height) {
        int half = Math.round((width >= height ? width : height) * ratio);
        int gap = Math.min(6, Math.max(0, half - 1));
        Rect[] result = width >= height
                ? new Rect[]{new Rect(0, 0, half - gap, height), new Rect(half + gap, 0, width, height)}
                : new Rect[]{new Rect(0, 0, width, half - gap), new Rect(0, half + gap, width, height)};
        return reversed ? new Rect[]{result[1], result[0]} : result;
    }

    private void setBounds(Object tx, Rect[] bounds) throws ReflectiveOperationException {
        Method set = transactionType.getMethod("setBounds", tokenType, Rect.class);
        set.invoke(tx, first.token(), bounds[0]);
        set.invoke(tx, second.token(), bounds[1]);
        // ColorOS may keep an entering task's surface under the display area during reparent.
        // Size and position the application task too, so it cannot retain its fullscreen origin.
        if (firstAppToken != null) set.invoke(tx, firstAppToken, bounds[0]);
        if (secondAppToken != null) set.invoke(tx, secondAppToken, bounds[1]);
    }

    private void applyLayout(Object tx, Rect[] bounds) throws ReflectiveOperationException {
        setBounds(tx, bounds);
        attachStageGeometry(tx, first, bounds[0]);
        attachStageGeometry(tx, second, bounds[1]);
        apply(tx);
    }

    private void attachStageGeometry(Object layout, Stage stage, Rect bounds)
            throws ReflectiveOperationException {
        Class<?> type = Class.forName("android.view.SurfaceControl$Transaction");
        Method attach;
        try {
            attach = transactionType.getMethod("setBoundsChangeTransaction", tokenType, type);
        } catch (NoSuchMethodException ignored) {
            // Older firmware still gets the explicit surface transaction after layout.
            return;
        }
        Object geometry = type.getConstructor().newInstance();
        // The WCT retains the transaction until it is parcelled and sent.
        setStageGeometry(type, geometry, stage, bounds);
        attach.invoke(layout, stage.token(), geometry);
    }

    private void setStageGeometry(Class<?> type, Object tx, Stage stage, Rect bounds)
            throws ReflectiveOperationException {
        Class<SurfaceControl> surface = SurfaceControl.class;
        type.getMethod("show", surface).invoke(tx, stage.surface());
        type.getMethod("setAlpha", surface, float.class).invoke(tx, stage.surface(), 1f);
        type.getMethod("setPosition", surface, float.class, float.class)
                .invoke(tx, stage.surface(), (float) bounds.left, (float) bounds.top);
        type.getMethod("setWindowCrop", surface, int.class, int.class)
                .invoke(tx, stage.surface(), bounds.width(), bounds.height());
    }

    private void showSurfaces(Rect[] bounds) throws ReflectiveOperationException {
        Class<SurfaceControl> surface = SurfaceControl.class;
        Class<?> type = Class.forName("android.view.SurfaceControl$Transaction");
        Object tx = type.getConstructor().newInstance();
        try {
            Method show = type.getMethod("show", surface);
            Method alpha = type.getMethod("setAlpha", surface, float.class);
            for (Stage stage : new Stage[]{root, first, second}) {
                show.invoke(tx, stage.surface());
                alpha.invoke(tx, stage.surface(), 1f);
            }
            setStageGeometry(type, tx, first, bounds[0]);
            setStageGeometry(type, tx, second, bounds[1]);
            type.getMethod("apply").invoke(tx);
        } finally {
            type.getMethod("close").invoke(tx);
        }
    }

    synchronized void fill(Object task, int width, int height) throws ReflectiveOperationException {
        secondAppToken = task;
        Object tx = transaction();
        transactionType.getMethod("reparent", tokenType, tokenType, boolean.class)
                .invoke(tx, task, second.token(), true);
        transactionType.getMethod("reorder", tokenType, boolean.class).invoke(tx, root.token(), true);
        Rect[] bounds = bounds(width, height);
        applyLayout(tx, bounds);
        android.os.SystemClock.sleep(900);
        showSurfaces(bounds);
        stabilizeSurfaces(bounds);
    }

    synchronized void adjust(float ratio, boolean swap, int width, int height) throws ReflectiveOperationException {
        this.ratio = Math.max(1f / 3f, Math.min(2f / 3f, ratio));
        if (swap) reversed = !reversed;
        resize(width, height);
    }

    synchronized void resize(int width, int height) throws ReflectiveOperationException {
        if (!active) return;
        Rect[] bounds = bounds(width, height);
        Object tx = transaction();
        applyLayout(tx, bounds);
        showSurfaces(bounds);
        stabilizeSurfaces(bounds);
    }

    private void stabilizeSurfaces(Rect[] bounds) {
        final long generation = ++layoutGeneration;
        // The launch and resize transitions can write stage transforms after applyTransaction().
        // Restore the current geometry once those transitions have had time to finish.
        for (long delay : new long[]{120, 350, 750}) {
            surfaceHandler.postDelayed(() -> {
                synchronized (this) {
                    if (!active || generation != layoutGeneration) return;
                    try {
                        showSurfaces(bounds);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        android.util.Log.w("BoxScreenSplit", "Could not restore split surface geometry", e);
                    }
                }
            }, delay);
        }
    }

    synchronized void close() throws ReflectiveOperationException {
        active = false;
        layoutGeneration++;
        if (root == null) return;
        try {
            Method remove = transactionType.getMethod("removeRootTask", tokenType);
            Object tx = transaction();
            remove.invoke(tx, root.token());
            apply(tx);
        } finally {
            for (Stage stage : new Stage[]{first, second, root}) {
                if (stage != null) stage.surface().release();
            }
            first = second = root = null;
            firstAppToken = secondAppToken = null;
        }
    }
}
