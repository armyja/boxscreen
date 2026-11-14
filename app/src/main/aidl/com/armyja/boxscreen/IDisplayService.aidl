package com.armyja.boxscreen;

import android.view.Surface;

interface IDisplayService {
    int createDisplay(String name, int width, int height, int densityDpi, in Surface surface);
    void resizeDisplay(int width, int height, int densityDpi);
    void releaseDisplay();
    void launchActivity(String packageName, String className);
    void sendKey(String keyCode);
    boolean setMouseDisplay(String inputDescriptor, boolean bindToVirtualDisplay);
    void startMouseEdgeSwitch(String inputDescriptor, int mainWidth, int mainHeight);
    void stopMouseEdgeSwitch();
}
