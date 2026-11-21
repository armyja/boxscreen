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
import java.util.ArrayList;
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
    private SplitStageOrganizer stageOrganizer;
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

    record Stage(Object token, SurfaceControl surface) {}

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
        if (stageOrganizer == null) {
            stageOrganizer = SplitStageOrganizerSelector.create(
                    displayId, organizer, organizerType, tokenType);
        }
        return stageOrganizer.create(name, mode, parent);
    }

    private Object transaction() throws ReflectiveOperationException {
        return transactionType.getConstructor().newInstance();
    }

    private void apply(Object tx) throws ReflectiveOperationException {
        Class.forName("android.window.IWindowOrganizerController")
                .getMethod("applyTransaction", transactionType).invoke(windowOrganizer, tx);
    }

    String openFromForegroundTask(int foregroundTaskId, int width, int height)
            throws ReflectiveOperationException {
        for (Object task : roots()) {
            if (taskId(task) != foregroundTaskId) continue;
            String packageName = packageOf(task);
            if (packageName == null) break;
            open(token(task), null, width, height);
            return packageName;
        }
        throw new IllegalStateException("前台任务已切换或无法分屏");
    }

    synchronized void open(Object firstTask, Object secondTask, int width, int height) throws ReflectiveOperationException {
        firstAppToken = firstTask;
        secondAppToken = secondTask;
        root = create("BoxScreenSplit", 1, null);
        first = create("BoxScreenSplitFirst", 6, root);
        second = create("BoxScreenSplitSecond", 6, root);
        Object tx = transaction();
        Method reparent = transactionType.getMethod("reparent", tokenType, tokenType, boolean.class);
        if (stageOrganizer != null && stageOrganizer.requiresExplicitStageParenting()) {
            reparent.invoke(tx, first.token(), root.token(), true);
            reparent.invoke(tx, second.token(), root.token(), true);
        }
        Object pair = Array.newInstance(tokenType, 2);
        Array.set(pair, 0, first.token());
        Array.set(pair, 1, second.token());
        transactionType.getMethod("setAdjacentRoots", pair.getClass()).invoke(tx, pair);
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
        Object existing = findRootTask(packageName);
        Integer existingTaskId = existing == null ? null : taskId(existing);
        if (existingTaskId != null) {
            Object resume = transaction();
            setHidden(resume, token(existing), false);
            transactionType.getMethod("startTask", int.class, Bundle.class)
                    .invoke(resume, existingTaskId, options.toBundle());
            apply(resume);
        } else {
            startNewStageActivity(packageName, className, options);
        }
        Object launched = awaitStageTask(target, packageName, existingTaskId);
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

    synchronized String[] dropIntoSplit(int foregroundTaskId, String foregroundPackage,
            String packageName, String className, int recentTaskId, boolean firstSide,
            int width, int height)
            throws ReflectiveOperationException {
        if (packageName == null) throw new IllegalArgumentException("缺少应用包名");
        if (active) {
            String[] current = packages();
            String targetPackage = current[firstSide ? 0 : 1];
            String otherPackage = current[firstSide ? 1 : 0];
            if (packageName.equals(targetPackage)) return current;
            if (packageName.equals(otherPackage)) {
                String maximized = expand(!firstSide);
                return new String[]{maximized};
            }
            if (stageTask(firstSide == reversed ? second : first, null) != null) {
                hideSide(firstSide);
            }
            fillDroppedApp(packageName, className, recentTaskId, firstSide, width, height);
            return packages();
        }

        Object foreground = foregroundTaskId > 0 ? taskById(foregroundTaskId) : null;
        if (foreground == null && foregroundPackage != null) foreground = findRootTask(foregroundPackage);
        if (foreground == null) throw new IllegalStateException("虚拟屏前台任务已切换");
        String actualForegroundPackage = packageOf(foreground);
        if (actualForegroundPackage == null) throw new IllegalStateException("无法读取当前应用");
        boolean sameApp = packageName.equals(actualForegroundPackage);
        // A same-app drop leaves the other pane empty. Otherwise the current app stays
        // opposite the dropped app, including when the user drops toward the left.
        reversed = sameApp ? !firstSide : firstSide;
        open(token(foreground), null, width, height);
        if (!sameApp) fillDroppedApp(packageName, className, recentTaskId, firstSide, width, height);
        return packages();
    }

    private void fillDroppedApp(String packageName, String className, int recentTaskId,
            boolean firstSide, int width, int height) throws ReflectiveOperationException {
        if (recentTaskId > 0) fillTask(recentTaskId, packageName, firstSide, width, height);
        else fill(packageName, className, firstSide, width, height);
    }

    private void fillTask(int recentTaskId, String packageName, boolean firstSide,
            int width, int height) throws ReflectiveOperationException {
        Stage target = firstSide == reversed ? second : first;
        if (stageTask(target, null) != null) throw new IllegalStateException("该分栏已有应用");
        boolean foundInRecents = false;
        for (Object task : systemRecentTasks()) {
            if (taskId(task) == recentTaskId && packageName.equals(packageOf(task))) {
                foundInRecents = true;
                break;
            }
        }
        if (!foundInRecents) throw new IllegalStateException("最近任务已失效，请重新打开状态栏");
        ActivityOptions options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId);
        ActivityOptions.class.getMethod("setLaunchRootTask", tokenType)
                .invoke(options, target.token());
        Object tx = transaction();
        transactionType.getMethod("startTask", int.class, Bundle.class)
                .invoke(tx, recentTaskId, options.toBundle());
        apply(tx);
        Object launched = awaitStageTask(target, null, recentTaskId);
        if (target == first) firstAppToken = launched;
        else secondAppToken = launched;
        Object layout = transaction();
        transactionType.getMethod("reorder", tokenType, boolean.class)
                .invoke(layout, root.token(), true);
        Rect[] targetBounds = bounds(width, height);
        applyLayout(layout, targetBounds);
        showSurfaces(targetBounds);
        stabilizeSurfaces(targetBounds);
    }

    private Object taskById(int id) throws ReflectiveOperationException {
        for (Object task : roots()) if (taskId(task) == id) return task;
        return null;
    }

    int taskIdForPackage(String packageName) throws ReflectiveOperationException {
        Object task = findRootTask(packageName);
        return task == null ? -1 : taskId(task);
    }

    boolean isTaskVisible(int id) throws ReflectiveOperationException {
        Object task = taskById(id);
        if (task == null) return false;
        try {
            return task.getClass().getField("isVisible").getBoolean(task);
        } catch (NoSuchFieldException ignored) {
            return true;
        }
    }

    boolean removeTaskById(int id) throws ReflectiveOperationException {
        Method removeTask = activityTaskManager.getClass().getMethod("removeTask", int.class);
        return Boolean.TRUE.equals(removeTask.invoke(activityTaskManager, id));
    }

    synchronized void restartFullscreenTask(int id) throws ReflectiveOperationException {
        Object task = taskById(id);
        ActivityOptions options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId);
        Object tx = transaction();
        if (task != null) {
            setHidden(tx, token(task), false);
            resetFullscreenBounds(tx, token(task));
        }
        transactionType.getMethod("startTask", int.class, Bundle.class)
                .invoke(tx, id, options.toBundle());
        apply(tx);
    }

    synchronized int splitTaskId(String packageName, boolean firstSide)
            throws ReflectiveOperationException {
        Stage target = firstSide == reversed ? second : first;
        Object task = stageTask(target, packageName);
        return task == null ? -1 : taskId(task);
    }

    synchronized void restartSplitSide(int id, boolean firstSide, int width, int height)
            throws ReflectiveOperationException {
        Stage target = firstSide == reversed ? second : first;
        Object task = stageTaskById(target, id);
        if (task == null) throw new IllegalStateException("应用任务已离开原分栏");
        ActivityOptions options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId);
        ActivityOptions.class.getMethod("setLaunchRootTask", tokenType)
                .invoke(options, target.token());
        Object tx = transaction();
        setHidden(tx, token(task), false);
        transactionType.getMethod("startTask", int.class, Bundle.class)
                .invoke(tx, id, options.toBundle());
        apply(tx);
        Object restarted = awaitStageTask(target, null, id);
        if (target == first) firstAppToken = restarted;
        else secondAppToken = restarted;
        Object layout = transaction();
        Rect[] targetBounds = bounds(width, height);
        applyLayout(layout, targetBounds);
        showSurfaces(targetBounds);
        stabilizeSurfaces(targetBounds);
    }

    private static int taskId(Object task) throws ReflectiveOperationException {
        return task.getClass().getField("taskId").getInt(task);
    }

    private Object findRootTask(String packageName) throws ReflectiveOperationException {
        for (Object task : roots()) {
            if (matchesPackage(task, packageName) && taskId(task) > 0) return task;
        }
        return null;
    }

    private boolean matchesPackage(Object task, String packageName) throws ReflectiveOperationException {
        String topPackage = packageOf(task);
        if (packageName == null || topPackage == null) return false;
        ComponentName base = (ComponentName) task.getClass().getField("baseActivity").get(task);
        return packageName.equals(topPackage) || (base != null && packageName.equals(base.getPackageName()));
    }

    private void setHidden(Object tx, Object taskToken, boolean hidden) throws ReflectiveOperationException {
        transactionType.getMethod("setHidden", tokenType, boolean.class).invoke(tx, taskToken, hidden);
    }

    boolean bringFullscreenTaskToFront(String packageName) throws ReflectiveOperationException {
        Object task = findRootTask(packageName);
        if (task == null) return false;
        Object tx = transaction();
        Object taskToken = token(task);
        resetFullscreenBounds(tx, taskToken);
        setHidden(tx, taskToken, false);
        transactionType.getMethod("reorder", tokenType, boolean.class).invoke(tx, taskToken, true);
        apply(tx);
        return true;
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

    private List<?> stageTasks(Stage stage) throws ReflectiveOperationException {
        if (stage == null) return java.util.Collections.emptyList();
        List<?> tasks = (List<?>) organizerType.getMethod("getChildTasks", tokenType, int[].class)
                .invoke(organizer, stage.token(), null);
        return tasks == null ? java.util.Collections.emptyList() : tasks;
    }

    private Object stageTask(Stage stage, String packageName) throws ReflectiveOperationException {
        for (Object task : stageTasks(stage)) {
            if (packageName == null ? packageOf(task) != null : matchesPackage(task, packageName)) {
                return task;
            }
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
        if (top == null) top = (ComponentName) task.getClass().getField("baseActivity").get(task);
        return top == null ? null : top.getPackageName();
    }

    private List<?> systemRecentTasks() throws ReflectiveOperationException {
        return RecentTaskReader.systemRecentTasks(activityTaskManager);
    }

    synchronized String activateTask(int taskId) throws ReflectiveOperationException {
        String packageName = null;
        if (active) {
            Object selected = stageTaskById(first, taskId);
            if (selected == null) selected = stageTaskById(second, taskId);
            if (selected != null) packageName = packageOf(selected);
            leaveSplit(selected == null ? -1 : taskId);
            if (selected != null) return packageName;
        }
        if (bringTaskToFront(taskId)) return packageForTaskId(taskId);
        return startRecentTaskOnDisplay(taskId);
    }

    private String startRecentTaskOnDisplay(int taskId) throws ReflectiveOperationException {
        Object recentTask = null;
        for (Object candidate : systemRecentTasks()) {
            if (taskId(candidate) == taskId) {
                recentTask = candidate;
                break;
            }
        }
        if (recentTask == null) return null;
        String packageName = packageOf(recentTask);
        if (packageName == null) return null;
        ActivityOptions options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId);
        Object tx = transaction();
        transactionType.getMethod("startTask", int.class, Bundle.class)
                .invoke(tx, taskId, options.toBundle());
        apply(tx);
        return packageName;
    }

    private Object stageTaskById(Stage stage, int taskId) throws ReflectiveOperationException {
        for (Object task : stageTasks(stage)) {
            if (taskId(task) == taskId) return task;
        }
        return null;
    }

    String packageForTaskId(int taskId) throws ReflectiveOperationException {
        for (Object task : roots()) {
            if (taskId(task) == taskId) return packageOf(task);
        }
        return null;
    }

    boolean bringTaskToFront(int taskId) throws ReflectiveOperationException {
        Object task = null;
        for (Object candidate : roots()) {
            if (taskId(candidate) == taskId) {
                task = candidate;
                break;
            }
        }
        if (task == null || packageOf(task) == null) return false;
        Object taskToken = token(task);
        Object tx = transaction();
        resetFullscreenBounds(tx, taskToken);
        setHidden(tx, taskToken, false);
        transactionType.getMethod("reorder", tokenType, boolean.class).invoke(tx, taskToken, true);
        apply(tx);
        return true;
    }

    synchronized String[] packages() throws ReflectiveOperationException {
        if (!active) return null;
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
    synchronized boolean hideSide(boolean visualFirst) throws ReflectiveOperationException {
        Stage stage = visualFirst == reversed ? second : first;
        if (stage == null) return false;
        Object task = stageTask(stage, null);
        if (task == null) return false;
        Object tx = transaction();
        moveToFullscreen(tx, task, false);
        setHidden(tx, token(task), true);
        apply(tx);
        if (stage == first) firstAppToken = null;
        else secondAppToken = null;
        return true;
    }

    /**
     * Force-stop the app in one pane so it restarts from its initial state, then drop its task
     * so the pane falls back to the app picker. Returns the killed package, or null when the
     * pane is already empty.
     */
    synchronized String killSide(boolean visualFirst) throws ReflectiveOperationException {
        Stage stage = visualFirst == reversed ? second : first;
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
                        + (stage == first ? "first" : "second"));
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
        if (stage == first) firstAppToken = null;
        else secondAppToken = null;
        return packageName;
    }

    synchronized String expand(boolean firstSide) throws ReflectiveOperationException {
        Stage target = firstSide == reversed ? second : first;
        Object preferred = stageTask(target, null);
        if (preferred == null) preferred = stageTask(target == first ? second : first, null);
        if (preferred == null) throw new IllegalStateException("分屏中没有可以放大的应用");
        String packageName = packageOf(preferred);
        leaveSplit(taskId(preferred));
        return packageName;
    }

    synchronized boolean leaveSplitForLaunch(String packageName) throws ReflectiveOperationException {
        Object preferred = stageTask(first, packageName);
        if (preferred == null) preferred = stageTask(second, packageName);
        int preferredId = preferred == null ? -1 : taskId(preferred);
        leaveSplit(preferredId);
        return preferred != null || bringFullscreenTaskToFront(packageName);
    }

    private void leaveSplit(int preferredId) throws ReflectiveOperationException {
        Object tx = transaction();
        transactionType.getMethod("clearAdjacentRoots", tokenType).invoke(tx, first.token());
        Object preferred = null;
        for (Stage stage : new Stage[]{first, second}) {
            List<?> tasks = stageTasks(stage);
            // getChildTasks returns top first. Move the bottom task first to retain order.
            for (int index = tasks.size() - 1; index >= 0; index--) {
                Object task = tasks.get(index);
                if (packageOf(task) == null) continue;
                if (taskId(task) == preferredId) preferred = task;
                else moveToFullscreen(tx, task, true);
            }
        }
        if (preferred != null) {
            moveToFullscreen(tx, preferred, true);
            setHidden(tx, token(preferred), false);
        }
        apply(tx);
        if (stageTask(first, null) != null || stageTask(second, null) != null) {
            throw new IllegalStateException("系统未能将分栏应用移回全屏");
        }
        close();
    }

    private void moveToFullscreen(Object tx, Object task, boolean toTop) throws ReflectiveOperationException {
        Object taskToken = token(task);
        resetFullscreenBounds(tx, taskToken);
        transactionType.getMethod("reparent", tokenType, tokenType, boolean.class)
                .invoke(tx, taskToken, null, toTop);
    }

    private void resetFullscreenBounds(Object tx, Object taskToken) throws ReflectiveOperationException {
        transactionType.getMethod("setWindowingMode", tokenType, int.class).invoke(tx, taskToken, 1);
        transactionType.getMethod("setBounds", tokenType, Rect.class).invoke(tx, taskToken, (Rect) null);
    }

    synchronized void adjust(float ratio, boolean swap, int width, int height) throws ReflectiveOperationException {
        this.ratio = SplitGeometry.clamp(ratio, width, height);
        if (swap) reversed = !reversed;
        resize(width, height);
    }

    synchronized void resize(int width, int height) throws ReflectiveOperationException {
        if (!active) return;
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
        try {
            if (root != null) {
                Object tx = transaction();
                Method remove = transactionType.getMethod("removeRootTask", tokenType);
                Method reparent = transactionType.getMethod("reparent", tokenType, tokenType, boolean.class);
                // The system only accepts removeRootTask for top-level organizer tasks.
                // Promote the stages within this display so all three containers can be removed.
                if (first != null) {
                    transactionType.getMethod("clearAdjacentRoots", tokenType).invoke(tx, first.token());
                    reparent.invoke(tx, first.token(), null, false);
                    remove.invoke(tx, first.token());
                }
                if (second != null) {
                    reparent.invoke(tx, second.token(), null, false);
                    remove.invoke(tx, second.token());
                }
                remove.invoke(tx, root.token());
                apply(tx);
            }
        } finally {
            for (Stage stage : new Stage[]{first, second, root}) {
                if (stage != null) stage.surface().release();
            }
            first = second = root = null;
            firstAppToken = secondAppToken = null;
            if (stageOrganizer != null) {
                stageOrganizer.close();
                stageOrganizer = null;
            }
        }
    }
}
