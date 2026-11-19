package com.armyja.boxscreen;

final class SplitGeometry {
    static final int HALF_GAP_PX = 6;

    private SplitGeometry() {}

    static float minimumRatio(int width, int height) {
        int length = Math.max(width, height);
        int cross = Math.min(width, height);
        if (length <= 1 || cross <= 0) return 0.5f;
        int narrowPane = Math.round(cross * 9f / 20f);
        return Math.min(0.5f, (narrowPane + HALF_GAP_PX) / (float) length);
    }

    static float clamp(float ratio, int width, int height) {
        float min = minimumRatio(width, height);
        return Math.max(min, Math.min(1f - min, ratio));
    }
}
