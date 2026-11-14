package com.armyja.boxscreen;

import android.app.Activity;
import android.app.Presentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.SurfaceHolder;
import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.view.InputDevice;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public final class MainActivity extends Activity {
    private static final String TAG = "BoxScreen";
    private static final int SHIZUKU_REQUEST = 410;

    private DisplayManager displayManager;
    private Display externalDisplay;
    private int baseWidth;
    private int baseHeight;
    private int densityDpi = 250;
    private boolean densityInitialized;
    private int virtualDisplayId = -1;
    private int transitionFramesRemaining;
    private boolean portraitMode;
    private boolean startPending;
    private String pendingLaunchPackage;
    private String pendingLaunchActivity;

    private LinearLayout content;
    private TextView statusView;
    private TextView monitorView;
    private TextView densityView;
    private SeekBar densitySlider;
    private GridLayout appGrid;
    private EditText appSearch;
    private Switch systemAppsSwitch;
    private Button editFavoritesButton;
    private List<ResolveInfo> appEntries = Collections.emptyList();
    private Set<String> favoritePackages = new HashSet<>();
    private boolean showSystemApps;
    private boolean editFavorites;
    private int appColumns;
    private Button modeButton;
    private Button mouseRouteButton;
    private boolean mouseBound;
    private String boundMouseDescriptor;
    private TextureView outputSurface;
    private MousePointerView mousePointerView;
    private Presentation presentation;
    private IDisplayService displayService;
    private Shizuku.UserServiceArgs shizukuServiceArgs;
    private final ServiceConnection shizukuServiceConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            displayService = IDisplayService.Stub.asInterface(binder);
            status("Shizuku 主机服务已连接");
            if (startPending) {
                startPending = false;
                showExternalOutput();
            }
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            displayService = null;
            virtualDisplayId = -1;
            mouseBound = false;
            boundMouseDescriptor = null;
            updateMouseRouteButton();
            status("Shizuku 服务已断开");
        }
    };

    private final Shizuku.OnRequestPermissionResultListener permissionListener = (requestCode, grantResult) -> {
        if (requestCode == SHIZUKU_REQUEST && grantResult == PackageManager.PERMISSION_GRANTED) {
            bindShizukuService();
        } else if (requestCode == SHIZUKU_REQUEST) {
            status("未授予 Shizuku 权限");
        }
    };

    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override public void onDisplayAdded(int displayId) {
            externalDisplay = findExternalDisplay();
            if (monitorView != null) updateMonitorLabel();
        }
        @Override public void onDisplayRemoved(int displayId) {
            if (displayId == (externalDisplay == null ? -1 : externalDisplay.getDisplayId())) stopOutput();
            externalDisplay = findExternalDisplay();
            if (monitorView != null) updateMonitorLabel();
        }
        @Override public void onDisplayChanged(int displayId) {
            externalDisplay = findExternalDisplay();
            if (monitorView != null) updateMonitorLabel();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        displayManager.registerDisplayListener(displayListener, new Handler());
        Shizuku.addRequestPermissionResultListener(permissionListener);
        externalDisplay = findExternalDisplay();
        showSystemApps = getPreferences(MODE_PRIVATE).getBoolean("show_system_apps", false);
        favoritePackages = new HashSet<>(getPreferences(MODE_PRIVATE).getStringSet("favorite_apps", Collections.emptySet()));
        int savedDensity = getPreferences(MODE_PRIVATE).getInt("virtual_density", 0);
        if (savedDensity >= 140 && savedDensity <= 600) {
            densityDpi = 140 + Math.round((savedDensity - 140) / 10f) * 10;
            densityInitialized = true;
        }
        showRoleScreen();
    }

    @Override protected void onResume() {
        super.onResume();
        externalDisplay = findExternalDisplay();
        if (monitorView != null) updateMonitorLabel();
    }

    @Override protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        if (presentation != null) presentation.dismiss();
        if (displayManager != null) displayManager.unregisterDisplayListener(displayListener);
        if (shizukuServiceArgs != null && displayService != null) {
            try { Shizuku.unbindUserService(shizukuServiceArgs, shizukuServiceConnection, true); }
            catch (RuntimeException ignored) { }
        }
        super.onDestroy();
    }

    private void showRoleScreen() {
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(20));
        content.setBackgroundColor(0xff111318);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
        content.addView(text("BoxScreen", 28, true), fullWidth());
        content.addView(text("手机屏幕与外接屏幕独立显示", 14, false), margins(0, 4, 0, 16));
        statusView = text("", 14, false);
        content.addView(statusView, margins(0, 0, 0, 12));
        showHostControls();
    }

    private void showHostControls() {
        monitorView = text("", 16, true);
        content.addView(monitorView, cardMargins());
        updateMonitorLabel();

        mouseRouteButton = button("开启鼠标操控虚拟屏");
        mouseRouteButton.setEnabled(false);
        content.addView(mouseRouteButton, margins(0, 0, 0, 8));
        mouseRouteButton.setOnClickListener(v -> toggleMouseRoute());

        modeButton = button("当前：横屏显示　·　切换为竖屏效果");
        content.addView(modeButton, margins(0, 0, 0, 8));
        modeButton.setOnClickListener(v -> setPortraitMode(!portraitMode));

        densityView = text("虚拟屏 DPI：" + densityDpi, 15, true);
        content.addView(densityView, margins(0, 0, 0, 2));
        densitySlider = new SeekBar(this);
        densitySlider.setMax(46);
        densitySlider.setProgress(Math.max(0, Math.min(46, (densityDpi - 140) / 10)));
        content.addView(densitySlider, margins(0, 0, 0, 10));
        densitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                densityDpi = 140 + progress * 10;
                densityInitialized = true;
                getPreferences(MODE_PRIVATE).edit().putInt("virtual_density", densityDpi).apply();
                densityView.setText("虚拟屏 DPI：" + densityDpi);
                updateDensityStatus();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { resizeVirtualDisplay(); }
        });

        Button stop = button("停止虚拟屏");
        content.addView(stop, margins(0, 0, 0, 14));
        stop.setOnClickListener(v -> stopOutput());

        TextView shizukuHint = text("Shizuku 在本机授权后即可使用。", 13, false);
        content.addView(shizukuHint, cardMargins());


        TextView appsTitle = text("虚拟屏可启动的应用", 18, true);
        content.addView(appsTitle, margins(0, 18, 0, 6));
        editFavoritesButton = button("编辑收藏");
        content.addView(editFavoritesButton, margins(0, 0, 0, 4));
        editFavoritesButton.setOnClickListener(v -> {
            editFavorites = !editFavorites;
            editFavoritesButton.setText(editFavorites ? "完成" : "编辑收藏");
            rebuildAppsGrid();
        });
        systemAppsSwitch = new Switch(this);
        systemAppsSwitch.setText("显示系统应用");
        systemAppsSwitch.setTextColor(0xfff2f2f2);
        systemAppsSwitch.setChecked(showSystemApps);
        content.addView(systemAppsSwitch, margins(0, 0, 0, 4));
        systemAppsSwitch.setOnCheckedChangeListener((button, checked) -> {
            showSystemApps = checked;
            getPreferences(MODE_PRIVATE).edit().putBoolean("show_system_apps", checked).apply();
            rebuildAppsGrid();
        });
        appSearch = new EditText(this);
        appSearch.setSingleLine(true);
        appSearch.setTextColor(0xfff2f2f2);
        appSearch.setHintTextColor(0xff9b9b9b);
        appSearch.setHint("搜索应用");
        content.addView(appSearch, margins(0, 0, 0, 8));
        appSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { rebuildAppsGrid(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        addLaunchableApps();
    }

    private void requestShizukuAndStart() {
        externalDisplay = findExternalDisplay();
        if (externalDisplay == null) {
            status("没有检测到外接显示器");
            updateMonitorLabel();
            return;
        }
        if (!Shizuku.pingBinder()) {
            status("请先在 PYB110 启动 Shizuku");
            return;
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            startPending = true;
            Shizuku.requestPermission(SHIZUKU_REQUEST);
            status("请在 Shizuku 提示中授权 BoxScreen");
            return;
        }
        if (displayService == null) {
            startPending = true;
            bindShizukuService();
        } else {
            showExternalOutput();
        }
    }

    private void bindShizukuService() {
        try {
            shizukuServiceArgs = new Shizuku.UserServiceArgs(
                    new ComponentName(getPackageName(), PrivilegedDisplayService.class.getName()))
                    .daemon(false)
                    .processNameSuffix("display")
                    .version(1);
            Shizuku.bindUserService(shizukuServiceArgs, shizukuServiceConnection);
            status("正在连接 Shizuku 主机服务…");
        } catch (RuntimeException e) {
            status("Shizuku 连接失败：" + e.getMessage());
        }
    }

    private void showExternalOutput() {
        if (presentation != null) {
            if (virtualDisplayId >= 0) {
                launchPendingApp();
                return;
            }
            if (outputSurface != null) {
                if (outputSurface.isAvailable() && displayService != null) {
                    createVirtualDisplay(outputSurface.getSurfaceTexture());
                }
                return;
            }
            stopOutput();
        }
        externalDisplay = findExternalDisplay();
        if (externalDisplay == null || displayService == null) {
            status("外接显示器或 Shizuku 服务不可用");
            return;
        }
        Display.Mode mode = externalDisplay.getMode();
        baseWidth = mode.getPhysicalWidth();
        baseHeight = mode.getPhysicalHeight();
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        externalDisplay.getRealMetrics(metrics);
        if (!densityInitialized) {
            densityDpi = Math.max(140, Math.min(600, Math.round(metrics.densityDpi / 10f) * 10));
            densityInitialized = true;
            getPreferences(MODE_PRIVATE).edit().putInt("virtual_density", densityDpi).apply();
            if (densityView != null) densityView.setText("虚拟屏 DPI：" + densityDpi);
            if (densitySlider != null) densitySlider.setProgress((densityDpi - 140) / 10);
        }
        if (baseWidth < baseHeight) {
            int swap = baseWidth; baseWidth = baseHeight; baseHeight = swap;
        }

        try {
            presentation = new Presentation(this, externalDisplay);
            presentation.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN);
            FrameLayout frame = new FrameLayout(this);
            frame.setBackgroundColor(0xff000000);
            outputSurface = new TextureView(this);
            outputSurface.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                    createVirtualDisplay(texture);
                }
                @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                    resizeVirtualDisplay();
                }
                @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                    releaseVirtualDisplay();
                    return true;
                }
                @Override public void onSurfaceTextureUpdated(SurfaceTexture texture) {
                    if (transitionFramesRemaining > 0 && --transitionFramesRemaining == 0) {
                        outputSurface.setVisibility(View.VISIBLE);
                        outputSurface.setAlpha(1f);
                        Log.i(TAG, "orientation transition complete on fresh frame");
                    }
                }
            });
            outputSurface.setOpaque(true);
            frame.addView(outputSurface);
            mousePointerView = new MousePointerView(this);
            frame.addView(mousePointerView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            presentation.setContentView(frame);
            presentation.show();
            updateOutputLayout();
            status("外接输出已启动：" + baseWidth + "×" + baseHeight + "，" + densityDpi + " dpi");
        } catch (RuntimeException e) {
            status("无法在外接显示器上显示：" + e.getMessage());
        }
    }

    private void createVirtualDisplay(SurfaceTexture texture) {
        if (displayService == null || texture == null) return;
        int width = portraitMode ? baseHeight : baseWidth;
        int height = portraitMode ? baseWidth : baseHeight;
        try {
            texture.setDefaultBufferSize(width, height);
            Log.i(TAG, "create buffer=" + width + "x" + height + " dpi=" + densityDpi);
            virtualDisplayId = displayService.createDisplay("BoxScreen", width, height,
                    densityDpi, new android.view.Surface(texture));
            updateMouseRouteButton();
            status("虚拟屏已创建，系统 display ID=" + virtualDisplayId);
            launchPendingApp();
        } catch (Exception e) {
            status("创建虚拟屏失败：" + e.getMessage());
        }
    }

    private void resizeVirtualDisplay() {
        if (displayService == null || virtualDisplayId < 0) return;
        int width = portraitMode ? baseHeight : baseWidth;
        int height = portraitMode ? baseWidth : baseHeight;
        try {
            Log.i(TAG, "resize=" + width + "x" + height + " dpi=" + densityDpi + " portrait=" + portraitMode);
            TextureView view = outputSurface;
            boolean orientationChange = view != null && view.isAvailable()
                    && ((view.getWidth() != width) || (view.getHeight() != height));
            if (orientationChange) {
                transitionFramesRemaining = 2;
                if (mousePointerView != null) mousePointerView.resetPointerPosition();
                view.setAlpha(0f);
                view.setVisibility(View.INVISIBLE);
                view.postDelayed(() -> {
                    if (outputSurface == view && transitionFramesRemaining > 0) {
                        transitionFramesRemaining = 0;
                        view.setVisibility(View.VISIBLE);
                        view.setAlpha(1f);
                        Log.w(TAG, "orientation transition used timeout fallback");
                    }
                }, 180);
            }
            if (outputSurface != null && outputSurface.isAvailable()) {
                outputSurface.getSurfaceTexture().setDefaultBufferSize(width, height);
            }
            displayService.resizeDisplay(width, height, densityDpi);
            updateOutputLayout();
            updateDensityStatus();
        } catch (Exception e) {
            status("切换显示方向失败：" + e.getMessage());
        }
    }

    private void updateDensityStatus() {
        if (statusView == null) return;
        int width = portraitMode ? baseHeight : baseWidth;
        int height = portraitMode ? baseWidth : baseHeight;
        int dpWidth = Math.round(width * 160f / densityDpi);
        int dpHeight = Math.round(height * 160f / densityDpi);
        status("虚拟屏：" + width + "×" + height + " px / " + dpWidth + "×" + dpHeight
                + " dp，DPI " + densityDpi);
    }

    private void updateOutputLayout() {
        if (outputSurface == null || presentation == null) return;
        int width = portraitMode ? baseHeight : baseWidth;
        int height = portraitMode ? baseWidth : baseHeight;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height, Gravity.CENTER);
        outputSurface.setLayoutParams(params);
        if (outputSurface.isAvailable()) outputSurface.getSurfaceTexture().setDefaultBufferSize(width, height);
        outputSurface.setRotation(portraitMode ? 90f : 0f);
        if (mousePointerView != null) {
            FrameLayout.LayoutParams pointerParams = new FrameLayout.LayoutParams(width, height, Gravity.CENTER);
            mousePointerView.setLayoutParams(pointerParams);
            mousePointerView.setRotation(portraitMode ? 90f : 0f);
        }
        if (modeButton != null) modeButton.setText(portraitMode
                ? "当前：竖屏效果　·　切换为横屏"
                : "当前：横屏显示　·　切换为竖屏效果");
    }

    private void setPortraitMode(boolean portrait) {
        portraitMode = portrait;
        Log.i(TAG, "orientation=" + (portrait ? "portrait" : "landscape"));
        resizeVirtualDisplay();
        if (modeButton != null) modeButton.setText(portraitMode
                ? "当前：竖屏效果　·　切换为横屏"
                : "当前：横屏显示　·　切换为竖屏效果");
    }

    private void stopOutput() {
        Log.i(TAG, "stop displayId=" + virtualDisplayId);
        mouseBound = false;
        boundMouseDescriptor = null;
        if (mousePointerView != null) mousePointerView.setForwarding(false);
        updateMouseRouteButton();
        if (virtualDisplayId >= 0 && displayService != null) {
            try { displayService.releaseDisplay(); } catch (RemoteException ignored) { }
        }
        virtualDisplayId = -1;
        if (presentation != null) {
            presentation.dismiss();
            presentation = null;
        }
        outputSurface = null;
        status("虚拟屏已停止");
    }

    private void toggleMouseRoute() {
        if (displayService == null || virtualDisplayId < 0) {
            status("请先创建虚拟屏");
            return;
        }
        try {
            if (mouseBound) {
                String descriptor = boundMouseDescriptor != null
                        ? boundMouseDescriptor : findExternalMouseDescriptor();
                if (descriptor == null) throw new IllegalStateException("未检测到已连接的外接鼠标");
                displayService.setMouseDisplay(descriptor, -1);
                mouseBound = false;
                boundMouseDescriptor = null;
                if (mousePointerView != null) mousePointerView.setForwarding(false);
                status("鼠标已解绑，返回本机主屏");
            } else {
                String descriptor = findExternalMouseDescriptor();
                if (descriptor == null) {
                    status("未检测到已连接的外接鼠标");
                    return;
                }
                if (externalDisplay == null) {
                    status("未检测到外接显示器");
                    return;
                }
                displayService.setMouseDisplay(descriptor, externalDisplay.getDisplayId());
                boundMouseDescriptor = descriptor;
                mouseBound = true;
                if (mousePointerView != null) mousePointerView.setForwarding(true);
                status(portraitMode
                        ? "鼠标已转发到竖屏虚拟画面，光标随画面旋转"
                        : "鼠标已转发到横屏虚拟画面");
            }
            updateMouseRouteButton();
        } catch (Exception e) {
            Log.e(TAG, "mouse display routing failed", e);
            status("鼠标转发失败：" + e.getMessage());
            updateMouseRouteButton();
        }
    }

    private String findExternalMouseDescriptor() {
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device == null || device.isVirtual() || !device.supportsSource(InputDevice.SOURCE_MOUSE)) continue;
            String descriptor = device.getDescriptor();
            if (descriptor != null && !descriptor.isEmpty()) return descriptor;
        }
        return null;
    }

    private void updateMouseRouteButton() {
        if (mouseRouteButton == null) return;
        mouseRouteButton.setEnabled(displayService != null && virtualDisplayId >= 0);
        mouseRouteButton.setText(mouseBound ? "虚拟屏鼠标控制已开启 · 点击关闭" : "开启鼠标操控虚拟屏");
    }

    private final class MousePointerView extends View {
        private final Handler handler = new Handler();
        private final Runnable hidePointer = () -> {
            pointerVisible = false;
            invalidate();
        };
        private final Paint pointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path pointerShape = new Path();
        private float pointerX;
        private float pointerY;
        private float virtualPointerX;
        private float virtualPointerY;
        private float lastRawX;
        private float lastRawY;
        private boolean pointerVisible;
        private boolean forwarding;
        private boolean pointerCaptured;
        private boolean rawPositionInitialized;

        MousePointerView(Context context) {
            super(context);
            setClickable(true);
            setFocusable(true);
            pointerPaint.setColor(0xffffffff);
            pointerPaint.setStyle(Paint.Style.FILL);
            outlinePaint.setColor(0xff111111);
            outlinePaint.setStyle(Paint.Style.STROKE);
            outlinePaint.setStrokeWidth(3f);
            outlinePaint.setStrokeJoin(Paint.Join.ROUND);
            pointerShape.moveTo(1f, 1f);
            pointerShape.lineTo(1f, 27f);
            pointerShape.lineTo(8f, 20f);
            pointerShape.lineTo(13f, 31f);
            pointerShape.lineTo(18f, 29f);
            pointerShape.lineTo(13f, 18f);
            pointerShape.lineTo(24f, 18f);
            pointerShape.close();
            setForwarding(false);
        }

        void setForwarding(boolean enabled) {
            forwarding = enabled;
            pointerVisible = false;
            rawPositionInitialized = false;
            handler.removeCallbacks(hidePointer);
            setPointerIcon(PointerIcon.getSystemIcon(getContext(), enabled
                    ? PointerIcon.TYPE_NULL : PointerIcon.TYPE_ARROW));
            if (enabled) {
                if (virtualPointerX == 0f && virtualPointerY == 0f) {
                    virtualPointerX = (portraitMode ? baseHeight : baseWidth) / 2f;
                    virtualPointerY = (portraitMode ? baseWidth : baseHeight) / 2f;
                }
                post(() -> {
                    if (forwarding && isAttachedToWindow() && hasWindowFocus()) requestPointerCapture();
                });
            } else if (hasPointerCapture()) {
                releasePointerCapture();
            }
            invalidate();
        }

        @Override
        public void onPointerCaptureChange(boolean hasCapture) {
            super.onPointerCaptureChange(hasCapture);
            pointerCaptured = hasCapture;
            rawPositionInitialized = false;
            Log.i(TAG, "mouse pointer capture=" + hasCapture);
        }

        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);
            if (hasWindowFocus && forwarding) requestPointerCapture();
        }

        @Override
        public boolean onCapturedPointerEvent(MotionEvent event) {
            if (!forwarding || !event.isFromSource(InputDevice.SOURCE_MOUSE)) return false;
            if (transitionFramesRemaining > 0) return true;
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                int virtualWidth = portraitMode ? baseHeight : baseWidth;
                int virtualHeight = portraitMode ? baseWidth : baseHeight;
                // Captured deltas use one shared gain so portrait aspect ratio cannot
                // make one physical axis travel farther than the other.
                virtualPointerX += event.getAxisValue(MotionEvent.AXIS_RELATIVE_X);
                virtualPointerY += event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y);
                virtualPointerX = Math.max(0f, Math.min(virtualWidth - 1f, virtualPointerX));
                virtualPointerY = Math.max(0f, Math.min(virtualHeight - 1f, virtualPointerY));
                showPointer();
            }
            forwardMouseEvent(event);
            return true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!forwarding || !event.isFromSource(InputDevice.SOURCE_MOUSE)) return false;
            if (transitionFramesRemaining > 0) return true;
            updateVirtualPointerFromAbsolute(event.getRawX(), event.getRawY());
            showPointer();
            forwardMouseEvent(event);
            return true;
        }

        @Override
        public boolean onGenericMotionEvent(MotionEvent event) {
            if (!forwarding || !event.isFromSource(InputDevice.SOURCE_MOUSE)) return false;
            int action = event.getActionMasked();
            if (action != MotionEvent.ACTION_HOVER_ENTER
                    && action != MotionEvent.ACTION_HOVER_MOVE
                    && action != MotionEvent.ACTION_HOVER_EXIT
                    && action != MotionEvent.ACTION_SCROLL
                    && action != MotionEvent.ACTION_BUTTON_PRESS
                    && action != MotionEvent.ACTION_BUTTON_RELEASE) return false;
            if (transitionFramesRemaining > 0) return true;
            if (action == MotionEvent.ACTION_HOVER_EXIT) {
                forwardMouseEvent(event);
                handler.removeCallbacks(hidePointer);
                pointerVisible = false;
                invalidate();
            } else {
                updateVirtualPointerFromAbsolute(event.getRawX(), event.getRawY());
                showPointer();
                forwardMouseEvent(event);
            }
            return true;
        }

        private void updateVirtualPointerFromAbsolute(float x, float y) {
            int virtualWidth = portraitMode ? baseHeight : baseWidth;
            int virtualHeight = portraitMode ? baseWidth : baseHeight;
            if (pointerCaptured) return;
            if (!rawPositionInitialized) {
                virtualPointerX = x * virtualWidth / Math.max(1, baseWidth);
                virtualPointerY = y * virtualHeight / Math.max(1, baseHeight);
                rawPositionInitialized = true;
            } else {
                // Fallback for Android versions/devices that do not grant pointer
                // capture: keep both axes at the same pixels-per-motion gain.
                virtualPointerX += x - lastRawX;
                virtualPointerY += y - lastRawY;
            }
            lastRawX = x;
            lastRawY = y;
            virtualPointerX = Math.max(0f, Math.min(virtualWidth - 1f, virtualPointerX));
            virtualPointerY = Math.max(0f, Math.min(virtualHeight - 1f, virtualPointerY));
        }

        private void showPointer() {
            pointerX = virtualPointerX;
            pointerY = virtualPointerY;
            pointerVisible = true;
            handler.removeCallbacks(hidePointer);
            handler.postDelayed(hidePointer, 1800);
            invalidate();
        }

        private void forwardMouseEvent(MotionEvent source) {
            if (displayService == null || virtualDisplayId < 0 || externalDisplay == null) return;
            if (transitionFramesRemaining > 0) return;
            float screenWidth = baseWidth;
            float screenHeight = baseHeight;
            if (screenWidth <= 0 || screenHeight <= 0) return;
            int virtualWidth = portraitMode ? baseHeight : baseWidth;
            int virtualHeight = portraitMode ? baseWidth : baseHeight;
            MotionEvent mapped = MotionEvent.obtain(source);
            try {
                if (portraitMode) {
                    float scaleX = virtualWidth / screenWidth;
                    float scaleY = virtualHeight / screenHeight;
                    Matrix transform = new Matrix();
                    transform.setValues(new float[]{
                            0f, -scaleX, screenWidth * scaleX,
                            scaleY, 0f, 0f,
                            0f, 0f, 1f
                    });
                    mapped.transform(transform);
                }
                mapped.setLocation(virtualPointerX, virtualPointerY);
                if (!displayService.injectMouseEvent(mapped, virtualDisplayId)) {
                    Log.w(TAG, "InputManager rejected mouse event for display " + virtualDisplayId);
                }
            } catch (RemoteException | RuntimeException e) {
                Log.e(TAG, "Unable to forward mouse event to virtual display", e);
            } finally {
                mapped.recycle();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!forwarding || !pointerVisible) return;
            canvas.save();
            canvas.translate(pointerX, pointerY);
            canvas.drawPath(pointerShape, outlinePaint);
            canvas.drawPath(pointerShape, pointerPaint);
            canvas.restore();
        }

        void hidePointer() {
            handler.removeCallbacks(hidePointer);
            pointerVisible = false;
            invalidate();
        }

        void resetPointerPosition() {
            hidePointer();
            virtualPointerX = (portraitMode ? baseHeight : baseWidth) / 2f;
            virtualPointerY = (portraitMode ? baseWidth : baseHeight) / 2f;
            rawPositionInitialized = false;
        }
    }

    private void releaseVirtualDisplay() {
        if (displayService != null && virtualDisplayId >= 0) {
            try { displayService.releaseDisplay(); } catch (RemoteException ignored) { }
        }
        virtualDisplayId = -1;
    }

    private void addLaunchableApps() {
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        appEntries = getPackageManager().queryIntentActivities(launcher, 0);
        appGrid = new GridLayout(this);
        appGrid.setColumnCount(1);
        appGrid.setUseDefaultMargins(false);
        appGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        content.addView(appGrid, fullWidth());
        appGrid.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int availableDp = Math.max(1, (right - left) / dp(1));
            int columns = Math.max(2, Math.min(8, availableDp / 80));
            if (columns != appColumns) {
                appColumns = columns;
                appGrid.setColumnCount(columns);
                rebuildAppsGrid();
            }
        });
        appGrid.post(this::rebuildAppsGrid);
    }

    private void rebuildAppsGrid() {
        if (appGrid == null) return;
        appGrid.removeAllViews();
        if (appColumns == 0) appColumns = 4;
        String query = appSearch == null ? "" : appSearch.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
        ArrayList<ResolveInfo> visible = new ArrayList<>();
        for (ResolveInfo app : appEntries) {
            String packageName = app.activityInfo.packageName;
            if (packageName.equals(getPackageName())) continue;
            int flags = app.activityInfo.applicationInfo.flags;
            boolean system = (flags & (android.content.pm.ApplicationInfo.FLAG_SYSTEM
                    | android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            if (system && !showSystemApps) continue;
            String label = String.valueOf(app.loadLabel(getPackageManager()));
            if (!query.isEmpty() && !label.toLowerCase(java.util.Locale.ROOT).contains(query)
                    && !packageName.toLowerCase(java.util.Locale.ROOT).contains(query)) continue;
            visible.add(app);
        }
        Collections.sort(visible, (a, b) -> {
            boolean af = favoritePackages.contains(a.activityInfo.packageName);
            boolean bf = favoritePackages.contains(b.activityInfo.packageName);
            if (!editFavorites && af != bf) return af ? -1 : 1;
            return String.valueOf(a.loadLabel(getPackageManager()))
                    .compareToIgnoreCase(String.valueOf(b.loadLabel(getPackageManager())));
        });
        int cellWidth = appGrid.getWidth() > 0 ? appGrid.getWidth() / appColumns : -1;
        for (ResolveInfo app : visible) {
            String packageName = app.activityInfo.packageName;
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setMinimumHeight(dp(98));
            tile.setPadding(dp(3), dp(6), dp(3), dp(6));
            tile.setBackgroundResource(android.R.drawable.list_selector_background);

            FrameLayout iconBox = new FrameLayout(this);
            android.widget.ImageView icon = new android.widget.ImageView(this);
            icon.setImageDrawable(app.loadIcon(getPackageManager()));
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER);
            iconBox.addView(icon, iconParams);
            CheckBox favorite = new CheckBox(this);
            favorite.setButtonTintList(android.content.res.ColorStateList.valueOf(0xfff2f2f2));
            favorite.setChecked(favoritePackages.contains(packageName));
            favorite.setContentDescription("收藏 " + app.loadLabel(getPackageManager()));
            favorite.setVisibility(editFavorites ? View.VISIBLE : View.GONE);
            tile.addView(favorite, new LinearLayout.LayoutParams(-2, dp(30)));
            tile.addView(iconBox, new LinearLayout.LayoutParams(-1, dp(54)));

            TextView label = text(String.valueOf(app.loadLabel(getPackageManager())), 12, false);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            tile.addView(label, new LinearLayout.LayoutParams(-1, -2));

            GridLayout.LayoutParams cell = new GridLayout.LayoutParams();
            cell.width = cellWidth;
            cell.height = -2;
            cell.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            appGrid.addView(tile, cell);
            tile.setOnClickListener(v -> launchPackage(packageName, app.activityInfo.name));
            favorite.setOnCheckedChangeListener((button, checked) -> {
                if (checked) favoritePackages.add(packageName); else favoritePackages.remove(packageName);
                getPreferences(MODE_PRIVATE).edit().putStringSet("favorite_apps", new HashSet<>(favoritePackages)).apply();
            });
        }
    }

    private void launchPackage(String packageName, String activityName) {
        if (displayService == null || virtualDisplayId < 0) {
            pendingLaunchPackage = packageName;
            pendingLaunchActivity = activityName;
            requestShizukuAndStart();
            return;
        }
        try {
            Log.i(TAG, "launch=" + packageName + "/" + activityName + " display=" + virtualDisplayId);
            displayService.launchActivity(packageName, activityName);
            status("已请求在虚拟屏中打开 " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "launch failed=" + packageName + ": " + e.getMessage(), e);
            status("启动应用失败：" + e.getMessage());
        }
    }

    private void launchPendingApp() {
        if (pendingLaunchPackage == null || pendingLaunchActivity == null
                || displayService == null || virtualDisplayId < 0) return;
        String packageName = pendingLaunchPackage;
        String activityName = pendingLaunchActivity;
        pendingLaunchPackage = null;
        pendingLaunchActivity = null;
        launchPackage(packageName, activityName);
    }

    private Display findExternalDisplay() {
        if (displayManager == null) return null;
        Display best = null;
        for (Display display : displayManager.getDisplays()) {
            if (display.getDisplayId() == Display.DEFAULT_DISPLAY
                    || (display.getFlags() & Display.FLAG_PRESENTATION) == 0) continue;
            if (best == null || display.getMode().getPhysicalWidth() * display.getMode().getPhysicalHeight()
                    > best.getMode().getPhysicalWidth() * best.getMode().getPhysicalHeight()) best = display;
        }
        return best;
    }

    private void updateMonitorLabel() {
        if (monitorView == null) return;
        externalDisplay = findExternalDisplay();
        if (externalDisplay == null) {
            monitorView.setText("未检测到外接显示器\n连接显示器后会自动读取 EDID 提供的当前模式。");
            return;
        }
        Display.Mode mode = externalDisplay.getMode();
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        externalDisplay.getRealMetrics(metrics);
        monitorView.setText("外接屏：" + externalDisplay.getName()
                + "\n当前模式：" + mode.getPhysicalWidth() + "×" + mode.getPhysicalHeight()
                + " @ " + Math.round(mode.getRefreshRate()) + " Hz"
                + "\n系统屏幕 ID：" + externalDisplay.getDisplayId() + "（运行时发现）"
                + "\n密度：" + metrics.densityDpi + " dpi");
    }

    private boolean hasShizukuPackage() {
        try { getPackageManager().getPackageInfo("moe.shizuku.privileged.api", 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    private void status(String text) {
        if (statusView != null) statusView.setText(text);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(0xfff2f2f2);
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        return b;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = fullWidth();
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return p;
    }

    private LinearLayout.LayoutParams cardMargins() { return margins(0, 6, 0, 12); }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, -2, 1f);
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

}
