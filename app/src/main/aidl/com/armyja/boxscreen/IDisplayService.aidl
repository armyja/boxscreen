package com.armyja.boxscreen;

import android.view.Surface;
import android.view.MotionEvent;

interface IDisplayService {
    int getVirtualDisplayId();
    int createDisplay(String name, int width, int height, int densityDpi, in Surface surface, boolean blockPointerCapture);
    void setDisplaySurface(in Surface surface);
    void resizeDisplay(int width, int height, int densityDpi);
    void resizeDisplaySize(int width, int height, int densityDpi);
    void releaseDisplay();
    void launchActivity(String packageName, String className);
    void launchViewOnVirtualDisplay(String packageName, String className, String uri, String mimeType);
    void sendKey(String keyCode);
    boolean injectKey(int keyCode, boolean down);
    boolean closeTopTaskOnVirtualDisplay();
    boolean hasVirtualDisplayTask();
    boolean setMouseDisplay(String inputDescriptor, int displayId);
    int clearGamepadAssociations();
    boolean injectInputEvent(in MotionEvent event, int displayId);
    String getPrimaryDensity();
    void setPrimaryDensity(int dpi);
    void launchSplit(String firstPackage, String firstClass, String secondPackage, String secondClass);
    boolean closeSplit();
    String killSplitSide(boolean firstSide);
    boolean hideSplitSide(boolean firstSide);
    String beginSplit();
    void fillSplit(String packageName, String className, boolean firstSide);
    void adjustSplit(float ratio, boolean swap);
    String[] getSplitPackages();
    String expandSplit(boolean firstSide);
    String[] getVirtualDisplayRecentTasks();
    String activateVirtualDisplayTask(int taskId);
    String getVirtualDisplayForegroundPackage();
    String[] dropAppIntoSplit(String packageName, String className, int taskId,
            boolean firstSide, String foregroundPackage);
    String[] restartDisplayedApp(String packageName, String foregroundPackage);
    String[] closeDisplayedApp(String packageName, String foregroundPackage);
    int getForegroundBoxScreenTaskId();
    void returnBoxScreenTaskToPrimary(int taskId);
}
