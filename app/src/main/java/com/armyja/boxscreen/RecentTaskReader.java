package com.armyja.boxscreen;

import android.content.ComponentName;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Reads launcher tasks without depending on either split-stage creation API. */
final class RecentTaskReader {
    private static final String TAG = "BoxScreenRecentTasks";

    private RecentTaskReader() {}

    static String[] read(int displayId) throws ReflectiveOperationException {
        Object activityTaskManager = activityTaskManager();
        LinkedHashMap<Integer, String> tasks = new LinkedHashMap<>();
        try {
            add(tasks, systemRecentTasks(activityTaskManager));
        } catch (ReflectiveOperationException | RuntimeException error) {
            Log.w(TAG, "system recent-task query unavailable; using display tasks", error);
        }

        // Recents can omit tasks that are only attached to the virtual display. Walk that
        // display's organizer tree as a separate, read-only query; this does not construct
        // or register a split organizer and works even when a split backend is unavailable.
        try {
            Object organizer = taskOrganizer(activityTaskManager);
            Class<?> organizerType = Class.forName("android.window.ITaskOrganizerController");
            List<?> roots = (List<?>) organizerType.getMethod("getRootTasks", int.class, int[].class)
                    .invoke(organizer, displayId, null);
            if (roots != null) {
                for (Object root : roots) {
                    try {
                        addTree(tasks, organizerType, organizer, root, 0);
                    } catch (ReflectiveOperationException | RuntimeException error) {
                        Log.d(TAG, "could not inspect one virtual-display task", error);
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException error) {
            Log.w(TAG, "virtual-display task query unavailable; keeping system recents", error);
        }

        ArrayList<String> result = new ArrayList<>(tasks.size());
        for (var entry : tasks.entrySet()) result.add(entry.getKey() + "\t" + entry.getValue());
        return result.toArray(new String[0]);
    }

    static List<?> systemRecentTasks(Object activityTaskManager) throws ReflectiveOperationException {
        Class<?> taskManagerType = Class.forName("android.app.IActivityTaskManager");
        Object recentSlice = taskManagerType
                .getMethod("getRecentTasks", int.class, int.class, int.class)
                .invoke(activityTaskManager, 50, 0, 0);
        if (recentSlice == null) return List.of();
        Object list = recentSlice.getClass().getMethod("getList").invoke(recentSlice);
        return list instanceof List<?> recentList ? recentList : List.of();
    }

    private static Object activityTaskManager() throws ReflectiveOperationException {
        IBinder binder = (IBinder) Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class).invoke(null, "activity_task");
        return Class.forName("android.app.IActivityTaskManager$Stub")
                .getMethod("asInterface", IBinder.class).invoke(null, binder);
    }

    private static Object taskOrganizer(Object activityTaskManager) throws ReflectiveOperationException {
        Object windowOrganizer = Class.forName("android.app.IActivityTaskManager")
                .getMethod("getWindowOrganizerController").invoke(activityTaskManager);
        return Class.forName("android.window.IWindowOrganizerController")
                .getMethod("getTaskOrganizerController").invoke(windowOrganizer);
    }

    private static void addTree(LinkedHashMap<Integer, String> result, Class<?> organizerType,
            Object organizer, Object task, int depth) throws ReflectiveOperationException {
        addOne(result, task);
        if (depth >= 8) return;
        Object token = task.getClass().getField("token").get(task);
        List<?> children = (List<?>) organizerType
                .getMethod("getChildTasks", token.getClass(), int[].class)
                .invoke(organizer, token, null);
        if (children == null) return;
        for (Object child : children) addTree(result, organizerType, organizer, child, depth + 1);
    }

    private static void add(LinkedHashMap<Integer, String> result, List<?> taskList)
            throws ReflectiveOperationException {
        for (Object task : taskList) addOne(result, task);
    }

    private static void addOne(LinkedHashMap<Integer, String> result, Object task)
            throws ReflectiveOperationException {
        int id = task.getClass().getField("taskId").getInt(task);
        String packageName = packageOf(task);
        if (id > 0 && packageName != null && !packageName.equals("com.armyja.boxscreen")
                && !isSystemRecentsActivity(task)) {
            result.putIfAbsent(id, packageName);
        }
    }

    private static String packageOf(Object task) throws ReflectiveOperationException {
        ComponentName top = (ComponentName) task.getClass().getField("topActivity").get(task);
        if (top == null) top = (ComponentName) task.getClass().getField("baseActivity").get(task);
        return top == null ? null : top.getPackageName();
    }

    private static boolean isSystemRecentsActivity(Object task) throws ReflectiveOperationException {
        ComponentName top = (ComponentName) task.getClass().getField("topActivity").get(task);
        ComponentName base = (ComponentName) task.getClass().getField("baseActivity").get(task);
        return isRecentsActivity(top) || isRecentsActivity(base);
    }

    private static boolean isRecentsActivity(ComponentName activity) {
        return activity != null && activity.getClassName().contains("RecentsActivity");
    }
}
