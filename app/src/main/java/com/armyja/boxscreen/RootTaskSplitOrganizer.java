package com.armyja.boxscreen;

import android.app.ActivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceControl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Split-stage implementation for systems exposing createRootTask() without TaskCreationParams. */
final class RootTaskSplitOrganizer implements SplitStageOrganizer {
    private static final String TAG = "BoxScreenSplit";
    private static final long APPEAR_TIMEOUT_MS = 3_000;

    private final Object controller;
    private final Class<?> controllerType;
    private final int displayId;
    private final Object callbackInterface;
    private final TaskOrganizerCallbackBinder callbackBinder;
    private final Method createRootTask;
    private final Method unregisterTaskOrganizer;
    private final Object callbackLock = new Object();
    private IBinder pendingCookie;
    private int pendingWindowingMode;
    private Set<Integer> pendingExistingTaskIds = Set.of();
    private SplitSession.Stage appearedStage;
    private boolean registered;

    RootTaskSplitOrganizer(int displayId, Object controller, Class<?> controllerType)
            throws ReflectiveOperationException {
        this.displayId = displayId;
        this.controller = controller;
        this.controllerType = controllerType;
        Class<?> organizerType = Class.forName("android.window.ITaskOrganizer");
        Class<?> stubType = Class.forName("android.window.ITaskOrganizer$Stub");
        createRootTask = controllerType.getMethod("createRootTask", int.class, int.class,
                IBinder.class, boolean.class, boolean.class, String.class);
        Method register = controllerType.getMethod("registerTaskOrganizer", organizerType);
        unregisterTaskOrganizer = controllerType.getMethod("unregisterTaskOrganizer", organizerType);
        callbackBinder = new TaskOrganizerCallbackBinder(this::onTaskAppeared, stubType);
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("asBinder")) return callbackBinder;
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "BoxScreenTaskOrganizerCallback";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == (args == null ? null : args[0]);
                    default -> null;
                };
            }
            return null;
        };
        callbackInterface = Proxy.newProxyInstance(organizerType.getClassLoader(),
                new Class<?>[]{organizerType}, handler);
        register.invoke(controller, callbackInterface);
        registered = true;
        Log.i(TAG, "registered root-task organizer backend for display=" + displayId);
    }

    @Override
    public SplitSession.Stage create(String name, int windowingMode, SplitSession.Stage parent)
            throws ReflectiveOperationException {
        IBinder cookie = new Binder();
        Set<Integer> existingTaskIds = new HashSet<>();
        Method getRootTasks = controllerType.getMethod("getRootTasks", int.class, int[].class);
        List<?> existingRoots = (List<?>) getRootTasks.invoke(controller, displayId, null);
        if (existingRoots != null) {
            for (Object task : existingRoots) {
                existingTaskIds.add(task.getClass().getField("taskId").getInt(task));
            }
        }
        synchronized (callbackLock) {
            pendingCookie = cookie;
            pendingWindowingMode = windowingMode;
            pendingExistingTaskIds = existingTaskIds;
            appearedStage = null;
        }

        try {
            // This older API can create display-root tasks only. SplitSession reparents the two
            // pane roots under its organizer-owned parent in the same WCT that attaches apps.
            // removeWithOrganizer keeps abandoned empty stages from surviving a service death.
            createRootTask.invoke(controller, displayId, windowingMode, cookie, true, false, name);
            long deadline = android.os.SystemClock.uptimeMillis() + APPEAR_TIMEOUT_MS;
            synchronized (callbackLock) {
                while (appearedStage == null) {
                    long remaining = deadline - android.os.SystemClock.uptimeMillis();
                    if (remaining <= 0) {
                        throw new IllegalStateException("系统未回报新建的分屏任务 surface");
                    }
                    callbackLock.wait(Math.min(remaining, 100));
                }
                return appearedStage;
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待分屏任务 surface 时被中断", interrupted);
        } finally {
            synchronized (callbackLock) {
                pendingCookie = null;
                pendingExistingTaskIds = Set.of();
                appearedStage = null;
            }
        }
    }

    private void onTaskAppeared(ActivityManager.RunningTaskInfo task, SurfaceControl leash) {
        synchronized (callbackLock) {
            if (pendingCookie == null || task == null || leash == null
                    || !matchesPendingTask(task, pendingCookie, pendingWindowingMode)) return;
            try {
                Object token = task.getClass().getField("token").get(task);
                appearedStage = new SplitSession.Stage(token, leash, task.taskId);
                Log.d(TAG, "received created stage task=" + task.taskId + " display=" + displayId);
                callbackLock.notifyAll();
            } catch (ReflectiveOperationException error) {
                Log.e(TAG, "could not read created stage task token", error);
            }
        }
    }

    private boolean matchesPendingTask(ActivityManager.RunningTaskInfo task, IBinder cookie,
            int expectedWindowingMode) {
        try {
            Object cookies = task.getClass().getField("launchCookies").get(task);
            if (cookies instanceof List<?> list && !list.isEmpty()) {
                for (Object item : list) if (cookie.equals(item)) return true;
                return false;
            }
        } catch (ReflectiveOperationException ignored) {
            // Older RunningTaskInfo variants do not expose launchCookies. Use only a new root
            // task on the expected display and in the requested mode as the fallback.
        }
        try {
            int taskId = task.getClass().getField("taskId").getInt(task);
            if (pendingExistingTaskIds.contains(taskId)) return false;
            if (task.getClass().getField("displayId").getInt(task) != displayId) return false;
            Method getWindowingMode = task.getClass().getMethod("getWindowingMode");
            return ((Number) getWindowingMode.invoke(task)).intValue() == expectedWindowingMode;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    @Override
    public boolean requiresExplicitStageParenting() {
        return true;
    }

    @Override
    public void close() throws ReflectiveOperationException {
        if (!registered) return;
        registered = false;
        unregisterTaskOrganizer.invoke(controller, callbackInterface);
        Log.i(TAG, "unregistered root-task organizer backend for display=" + displayId);
    }

    /** Minimal Binder endpoint for the platform's hidden ITaskOrganizer callback interface. */
    private static final class TaskOrganizerCallbackBinder extends Binder {
        private static final String DESCRIPTOR = "android.window.ITaskOrganizer";
        private final int appearedCode;
        private final Set<Integer> callbackCodes;
        private final Callback callback;

        TaskOrganizerCallbackBinder(Callback callback, Class<?> stubType)
                throws ReflectiveOperationException {
            this.callback = callback;
            callbackCodes = new HashSet<>();
            int foundAppearedCode = -1;
            for (Field field : stubType.getDeclaredFields()) {
                if (!field.getName().startsWith("TRANSACTION_")) continue;
                field.setAccessible(true);
                int code = field.getInt(null);
                callbackCodes.add(code);
                if (field.getName().equals("TRANSACTION_onTaskAppeared")) foundAppearedCode = code;
            }
            if (foundAppearedCode < 0) {
                throw new NoSuchFieldException("ITaskOrganizer.TRANSACTION_onTaskAppeared");
            }
            appearedCode = foundAppearedCode;
            attachInterface(null, DESCRIPTOR);
        }

        @Override
        protected boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply,
                int flags) throws android.os.RemoteException {
            if (!callbackCodes.contains(code)) return super.onTransact(code, data, reply, flags);
            data.enforceInterface(DESCRIPTOR);
            if (code == appearedCode) {
                ActivityManager.RunningTaskInfo task = data.readTypedObject(
                        ActivityManager.RunningTaskInfo.CREATOR);
                SurfaceControl leash = data.readTypedObject(SurfaceControl.CREATOR);
                callback.onTaskAppeared(task, leash);
            }
            if (reply != null) reply.writeNoException();
            return true;
        }
    }

    @FunctionalInterface
    private interface Callback {
        void onTaskAppeared(ActivityManager.RunningTaskInfo task, SurfaceControl leash);
    }
}
