package com.armyja.boxscreen;

import android.view.Surface;
import android.view.MotionEvent;

interface IDisplayService {
    int createDisplay(String name, int width, int height, int densityDpi, in Surface surface);
    void setDisplaySurface(in Surface surface);
    void resizeDisplay(int width, int height, int densityDpi);
    void resizeDisplaySize(int width, int height, int densityDpi);
    void releaseDisplay();
    void launchActivity(String packageName, String className);
    void sendKey(String keyCode);
    boolean closeTopTaskOnVirtualDisplay();
    boolean setMouseDisplay(String inputDescriptor, int displayId);
    boolean injectInputEvent(in MotionEvent event, int displayId);
}
