package com.armyja.boxscreen;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
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
    private static final String TAG = "BoxScreenSplit";

    private final int displayId;
    private final Object organizer;
    private final Object activityTaskManager;
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
    private Stage expandedStage;
    private float ratio = 0.5f;
    private boolean reversed;
    private long layoutGeneration;
    private final android.os.Handler surfaceHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    private record Stage(Object token, SurfaceControl surface) {}

    SplitSession(int displayId) throws ReflectiveOperationException {
        this.displayId = displayId;
        IBinder binder = (IBinder) Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class).invoke(null, "activity_task");
        activityTaskManager = Class.forName("android.app.IActivityTaskManager$Stub")
                .getMethod("asInterface", IBinder.class).invoke(null, binder);
        windowOrganizer = Class.forName("android.app.IActivityTaskManager")
                .getMethod("getWindowOrganizerController").invoke(activityTaskManager);
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
        // No gap: the panes meet on the same integer coordinate, so a task that is still
        // around (for example the app that was fullscreen before the split) cannot show
        // through between them.
        Rect[] result = width >= height
                ? new Rect[]{new Rect(0, 0, half, height), new Rect(half, 0, width, height)}
                : new Rect[]{new Rect(0, 0, width, half), new Rect(0, half, width, height)};
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

    synchronized void fill(String packageName, String className, boolean firstSide, int width, int height)
            throws ReflectiveOperationException {
        Stage target = firstSide == reversed ? second : first;
        if (stageTask(target, null) != null) throw new IllegalStateException("该分栏已有应用");
        ActivityOptions options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId);
        ActivityOptions.class.getMethod("setLaunchRootTask", tokenType)
                .invoke(options, target.token());
        Integer existingTaskId = findReusableTask(packageName);
        if (existingTaskId != null) {
            Object resume = transaction();
            transactionType.getMethod("startTask", int.class, Bundle.class)
                    .invoke(resume, existingTaskId, options.toBundle());
            apply(resume);
        } else {
            startNewStageActivity(packageName, className, options);
        }
        Object launched = awaitStageTask(target, packageName, existingTaskId);
        if (existingTaskId != null) {
            android.util.Log.i("BoxScreenSplit", "reused task=" + existingTaskId
                    + " package=" + packageName + " display=" + displayId);
        }
        if (target == first) firstAppToken = launched;
        else secondAppToken = launched;
        Object tx = transaction();
        transactionType.getMethod("reorder", tokenType, boolean.class)
                .invoke(tx, root.token(), true);
        Rect[] bounds = bounds(width, height);
        applyLayout(tx, bounds);
        showSurfaces(bounds);
        stabilizeSurfaces(bounds);
    }

    private Integer findReusableTask(String packageName) throws ReflectiveOperationException {
        for (Object task : roots()) {
            ComponentName base = (ComponentName) task.getClass().getField("baseActivity").get(task);
            ComponentName top = (ComponentName) task.getClass().getField("topActivity").get(task);
            if (top == null) continue;
            if (packageName.equals(top.getPackageName())
                    || (base != null && packageName.equals(base.getPackageName()))) {
                int taskId = task.getClass().getField("taskId").getInt(task);
                if (taskId > 0) return taskId;
            }
        }
        return null;
    }

    private void startNewStageActivity(String packageName, String className, ActivityOptions options)
            throws ReflectiveOperationException {
        Intent launch = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName.createRelative(packageName, className))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        Method start = Class.forName("android.app.IActivityTaskManager").getMethod(
                "startActivityAsUser", Class.forName("android.app.IApplicationThread"),
                String.class, String.class, Intent.class, String.class, IBinder.class,
                String.class, int.class, int.class, Class.forName("android.app.ProfilerInfo"),
                Bundle.class, int.class);
        int userId = android.os.Process.myUid() / 100000;
        int result = (Integer) start.invoke(activityTaskManager, null, "com.android.shell",
                null, launch, null, null, null, 0, 0, null, options.toBundle(), userId);
        if (result < 0) throw new IllegalStateException("系统拒绝在分栏中启动应用：" + result);
    }

    private Object stageTask(Stage stage, String packageName) throws ReflectiveOperationException {
        if (stage == null) return null;
        List<?> tasks = (List<?>) organizerType.getMethod("getChildTasks", tokenType, int[].class)
                .invoke(organizer, stage.token(), null);
        if (tasks == null) return null;
        for (Object task : tasks) {
            ComponentName top = (ComponentName) task.getClass().getField("topActivity").get(task);
            if (top != null && (packageName == null || packageName.equals(top.getPackageName()))) return task;
        }
        return null;
    }

    private Object awaitStageTask(Stage stage, String packageName, Integer existingTaskId)
            throws ReflectiveOperationException {
        for (int attempt = 0; attempt < 30; attempt++) {
            Object task = stageTask(stage, existingTaskId == null ? packageName : null);
            if (task != null && (existingTaskId == null
                    || task.getClass().getField("taskId").getInt(task) == existingTaskId)) {
                return token(task);
            }
            android.os.SystemClock.sleep(50);
        }
        throw new IllegalStateException(existingTaskId == null
                ? "系统未把应用放进指定分栏：" + packageName
                : "系统未能将原有任务移入分栏，已保留原任务：" + packageName);
    }

    private String packageOf(Object task) throws ReflectiveOperationException {
        if (task == null) return null;
        ComponentName top = (ComponentName) task.getClass().getField("topActivity").get(task);
        return top == null ? null : top.getPackageName();
    }

    synchronized String[] packages() throws ReflectiveOperationException {
        if (!active || expandedStage != null) return null;
        Object one = stageTask(first, null);
        Object two = stageTask(second, null);
        firstAppToken = one == null ? null : token(one);
        secondAppToken = two == null ? null : token(two);
        String a = packageOf(one);
        String b = packageOf(two);
        return reversed ? new String[]{b, a} : new String[]{a, b};
    }

    /**
     * Hide the app in one pane without killing it: the task stays alive (and can be restored)
     * while the pane falls back to the app picker.
     */
    synchronized boolean hideSide(boolean firstSide) throws ReflectiveOperationException {
        Stage stage = firstSide ? first : second;
        if (stage == null) return false;
        Object task = stageTask(stage, null);
        if (task == null) return false;
        Object tx = transaction();
        transactionType.getMethod("setHidden", tokenType, boolean.class)
                .invoke(tx, token(task), true);
        apply(tx);
        if (firstSide) firstAppToken = null;
        else secondAppToken = null;
        return true;
    }

    /**
     * Force-stop the app in one pane so it restarts from its initial state, then drop its task
     * so the pane falls back to the app picker. Returns the killed package, or null when the
     * pane is already empty.
     */
    synchronized String killSide(boolean firstSide) throws ReflectiveOperationException {
        Stage stage = firstSide ? first : second;
        if (stage == null) return null;
        Object task = stageTask(stage, null);
        if (task == null) return null;
        int taskId = task.getClass().getField("taskId").getInt(task);
        String packageName = packageOf(task);
        if (packageName != null) {
            try {
                Process process = new ProcessBuilder("/system/bin/am", "force-stop", packageName)
                        .redirectErrorStream(true).start();
                process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
                Log.i("BoxScreenSplit", "force-stop " + packageName + " pane="
                        + (firstSide ? "first" : "second"));
            } catch (Exception error) {
                Log.w("BoxScreenSplit", "force-stop failed for " + packageName, error);
            }
        }
        try {
            Method removeTask = activityTaskManager.getClass().getMethod("removeTask", int.class);
            removeTask.invoke(activityTaskManager, taskId);
        } catch (ReflectiveOperationException error) {
            Log.w("BoxScreenSplit", "removeTask failed for " + taskId, error);
        }
        if (firstSide) firstAppToken = null;
        else secondAppToken = null;
        return packageName;
    }

    synchronized boolean isExpanded() {
        return expandedStage != null;
    }

    synchronized void beginAgain(String packageName, int width, int height)
            throws ReflectiveOperationException {
        if (expandedStage == null) throw new IllegalStateException("当前已处于分屏");
        Object task = stageTask(expandedStage, packageName);
        if (task == null) throw new IllegalStateException("当前应用已退出");
        reversed = expandedStage == second;
        firstAppToken = expandedStage == first ? token(task) : null;
        secondAppToken = expandedStage == second ? token(task) : null;
        Object tx = transaction();
        transactionType.getMethod("setWindowingMode", tokenType, int.class)
                .invoke(tx, expandedStage.token(), 6);
        transactionType.getMethod("setWindowingMode", tokenType, int.class)
                .invoke(tx, token(task), 0);
        Object pair = Array.newInstance(tokenType, 2);
        Array.set(pair, 0, first.token());
        Array.set(pair, 1, second.token());
        transactionType.getMethod("setAdjacentRoots", pair.getClass()).invoke(tx, pair);
        expandedStage = null;
        ratio = 0.5f;
        Rect[] bounds = bounds(width, height);
        applyLayout(tx, bounds);
        showSurfaces(bounds);
        stabilizeSurfaces(bounds);
    }

    synchronized String expand(boolean firstSide, int width, int height) throws ReflectiveOperationException {
        Stage keep = firstSide == reversed ? second : first;
        Stage other = keep == first ? second : first;
        Object app = stageTask(keep, null);
        if (app == null) {
            Stage previous = keep;
            keep = other;
            other = previous;
            app = stageTask(keep, null);
        }
        if (app == null) throw new IllegalStateException("分屏中没有可以放大的应用");
        Object tx = transaction();
        transactionType.getMethod("clearAdjacentRoots", tokenType).invoke(tx, keep.token());
        // Maximising hides the other pane's app instead of killing it: the task moves out of the
        // split root to the display area and is marked hidden, so the app keeps running and can
        // be brought back with its state intact.
        Object otherApp = stageTask(other, null);
        if (otherApp != null) {
            try {
                transactionType.getMethod("reparent", tokenType, tokenType, boolean.class)
                        .invoke(tx, token(otherApp), null, false);
            } catch (Throwable ignored) {
                // Some builds reject a null parent; hiding in place still keeps the task alive.
            }
            try {
                transactionType.getMethod("setHidden", tokenType, boolean.class)
                        .invoke(tx, token(otherApp), true);
            } catch (Throwable error) {
                Log.w("BoxScreenSplit", "could not hide the other pane", error);
            }
        }
        transactionType.getMethod("setWindowingMode", tokenType, int.class)
                .invoke(tx, keep.token(), 1);
        transactionType.getMethod("setWindowingMode", tokenType, int.class)
                .invoke(tx, token(app), 1);
        expandedStage = keep;
        firstAppToken = keep == first ? token(app) : null;
        secondAppToken = keep == second ? token(app) : null;
        applyExpandedLayout(tx, width, height);
        return packageOf(app);
    }

    private void applyExpandedLayout(Object tx, int width, int height)
            throws ReflectiveOperationException {
        Rect full = new Rect(0, 0, width, height);
        Method set = transactionType.getMethod("setBounds", tokenType, Rect.class);
        set.invoke(tx, expandedStage.token(), full);
        Object app = stageTask(expandedStage, null);
        if (app != null) set.invoke(tx, token(app), full);
        transactionType.getMethod("reorder", tokenType, boolean.class)
                .invoke(tx, expandedStage.token(), true);
        attachStageGeometry(tx, expandedStage, full);
        apply(tx);
        showExpandedSurface(full);
        stabilizeSurfaces(new Rect[]{full, full});
    }

    private void showExpandedSurface(Rect full) throws ReflectiveOperationException {
        Class<?> type = Class.forName("android.view.SurfaceControl$Transaction");
        Object tx = type.getConstructor().newInstance();
        try {
            type.getMethod("show", SurfaceControl.class).invoke(tx, root.surface());
            setStageGeometry(type, tx, expandedStage, full);
            Stage other = expandedStage == first ? second : first;
            type.getMethod("hide", SurfaceControl.class).invoke(tx, other.surface());
            type.getMethod("apply").invoke(tx);
        } finally {
            type.getMethod("close").invoke(tx);
        }
    }

    synchronized void adjust(float ratio, boolean swap, int width, int height) throws ReflectiveOperationException {
        this.ratio = SplitGeometry.clamp(ratio, width, height);
        if (swap) reversed = !reversed;
        resize(width, height);
    }

    synchronized void resize(int width, int height) throws ReflectiveOperationException {
        if (!active) return;
        if (expandedStage != null) {
            applyExpandedLayout(transaction(), width, height);
            return;
        }
        ratio = SplitGeometry.clamp(ratio, width, height);
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
                        if (expandedStage == null) showSurfaces(bounds);
                        else showExpandedSurface(bounds[0]);
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
            first = second = root = expandedStage = null;
            firstAppToken = secondAppToken = null;
        }
    }
}
