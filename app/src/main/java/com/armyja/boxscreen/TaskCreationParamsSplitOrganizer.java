package com.armyja.boxscreen;

import android.view.SurfaceControl;

/** Split-stage implementation for systems exposing TaskCreationParams/createTask(). */
final class TaskCreationParamsSplitOrganizer implements SplitStageOrganizer {
    private final int displayId;
    private final Object organizer;
    private final Class<?> organizerType;
    private final Class<?> tokenType;
    private final Class<?> paramsType;
    private final Class<?> builderType;

    TaskCreationParamsSplitOrganizer(int displayId, Object organizer, Class<?> organizerType,
            Class<?> tokenType) throws ReflectiveOperationException {
        this.displayId = displayId;
        this.organizer = organizer;
        this.organizerType = organizerType;
        this.tokenType = tokenType;
        paramsType = Class.forName("android.window.TaskCreationParams");
        builderType = Class.forName("android.window.TaskCreationParams$Builder");
        organizerType.getMethod("createTask", paramsType);
        builderType.getConstructor();
        builderType.getMethod("setDisplayId", int.class);
        builderType.getMethod("setWindowingMode", int.class);
        builderType.getMethod("setName", String.class);
        builderType.getMethod("setParentContainer", tokenType);
        builderType.getMethod("build");
    }

    @Override
    public SplitSession.Stage create(String name, int mode, SplitSession.Stage parent)
            throws ReflectiveOperationException {
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
        SurfaceControl leash = (SurfaceControl) appeared.getClass().getMethod("getLeash").invoke(appeared);
        int taskId = info.getClass().getField("taskId").getInt(info);
        return new SplitSession.Stage(info.getClass().getField("token").get(info), leash, taskId);
    }

    @Override
    public boolean requiresExplicitStageParenting() {
        return false;
    }

    @Override
    public void close() {
        // TaskCreationParams backend uses the existing WindowContainerTransaction lifecycle.
    }
}
