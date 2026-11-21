package com.armyja.boxscreen;

import android.util.Log;

/** Selects by the actual organizer APIs available on this OS build, not by device model. */
final class SplitStageOrganizerSelector {
    private static final String TAG = "BoxScreenSplit";

    private SplitStageOrganizerSelector() {}

    static SplitStageOrganizer create(int displayId, Object organizer, Class<?> organizerType,
            Class<?> tokenType) throws ReflectiveOperationException {
        try {
            SplitStageOrganizer backend = new TaskCreationParamsSplitOrganizer(
                    displayId, organizer, organizerType, tokenType);
            Log.i(TAG, "selected TaskCreationParams split backend");
            return backend;
        } catch (ReflectiveOperationException | LinkageError unavailable) {
            Log.i(TAG, "TaskCreationParams backend unavailable; probing createRootTask backend");
        }

        try {
            SplitStageOrganizer backend = new RootTaskSplitOrganizer(
                    displayId, organizer, organizerType);
            Log.i(TAG, "selected root-task callback split backend");
            return backend;
        } catch (ReflectiveOperationException | LinkageError unavailable) {
            throw new NoSuchMethodException("当前系统没有可用的分屏任务创建接口："
                    + unavailable.getClass().getSimpleName() + ": " + unavailable.getMessage());
        }
    }
}
