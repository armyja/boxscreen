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
import android.os.Build;
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
import android.view.WindowInsets;
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
    private final Handler densityResizeHandler = new Handler();
    private final Runnable applyPendingDensity = this::resizeVirtualDisplay;

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
    private boolean autoMouseSuppressed;
    private boolean phoneScreenDimmed;
    private boolean systemBarsHiddenForBlackout;
    private int previousSystemUiVisibility;
    private float previousWindowBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

    private LinearLayout content;
    private FrameLayout rootLayout;
    private View blackScreenOverlay;
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
            if (mousePointerView != null) mousePointerView.setForwarding(false);
            restorePhoneScreen();
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
        densityResizeHandler.removeCallbacks(applyPendingDensity);
        restorePhoneScreen();
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
        content.setPadding(dp(18), dp(12), dp(18), dp(14));
        content.setBackgroundColor(0xff111318);
        rootLayout = new FrameLayout(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.addView(content);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset;
            int bottomInset;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                topInset = bars.top;
                bottomInset = bars.bottom;
            } else {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }
            content.setPadding(dp(18), dp(12) + topInset, dp(18), dp(14) + bottomInset);
            return insets;
        });
        rootLayout.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        setContentView(rootLayout);
        content.addView(text("BoxScreen", 24, true), fullWidth());
        content.addView(text("手机屏幕与外接屏幕独立显示", 12, false), margins(0, 1, 0, 8));
        statusView = text("", 12, false);
        content.addView(statusView, margins(0, 0, 0, 8));
        showHostControls();
    }

    private void showHostControls() {
        monitorView = text("", 13, true);
        content.addView(monitorView, margins(0, 2, 0, 8));
        updateMonitorLabel();

        LinearLayout primaryActions = new LinearLayout(this);
        primaryActions.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(primaryActions, margins(0, 0, 0, 4));
        mouseRouteButton = button("开启鼠标");
        mouseRouteButton.setEnabled(false);
        LinearLayout.LayoutParams actionCell = weighted();
        actionCell.setMargins(0, 0, dp(5), 0);
        primaryActions.addView(mouseRouteButton, actionCell);
        mouseRouteButton.setOnClickListener(v -> toggleMouseRoute());

        modeButton = button(portraitMode ? "竖屏 · 切换横屏" : "横屏 · 切换竖屏");
        LinearLayout.LayoutParams modeCell = weighted();
        modeCell.setMargins(dp(5), 0, 0, 0);
        primaryActions.addView(modeButton, modeCell);
        modeButton.setOnClickListener(v -> setPortraitMode(!portraitMode));

        LinearLayout densityRow = new LinearLayout(this);
        densityRow.setOrientation(LinearLayout.HORIZONTAL);
        densityRow.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(densityRow, margins(0, 4, 0, 0));
        densityView = text("虚拟屏 DPI：" + densityDpi, 13, true);
        densityRow.addView(densityView, new LinearLayout.LayoutParams(0, -2, 1f));
        Button stop = button("停止输出");
        densityRow.addView(stop, new LinearLayout.LayoutParams(-2, -2));
        stop.setOnClickListener(v -> stopOutput());
        Button blackout = button("手机黑屏");
        LinearLayout.LayoutParams blackoutParams = new LinearLayout.LayoutParams(-2, -2);
        blackoutParams.setMargins(dp(4), 0, 0, 0);
        densityRow.addView(blackout, blackoutParams);
        blackout.setOnClickListener(v -> dimAndBlackoutPhoneScreen());
        densitySlider = new SeekBar(this);
        densitySlider.setMax(46);
        densitySlider.setProgress(Math.max(0, Math.min(46, (densityDpi - 140) / 10)));
        content.addView(densitySlider, margins(-6, -2, -6, 4));
        densitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                densityDpi = 140 + progress * 10;
                densityInitialized = true;
                getPreferences(MODE_PRIVATE).edit().putInt("virtual_density", densityDpi).apply();
                densityView.setText("虚拟屏 DPI：" + densityDpi);
                updateDensityStatus();
                if (fromUser) {
                    densityResizeHandler.removeCallbacks(applyPendingDensity);
                    densityResizeHandler.postDelayed(applyPendingDensity, 220);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                densityResizeHandler.removeCallbacks(applyPendingDensity);
                resizeVirtualDisplay();
            }
        });

        TextView shizukuHint = text("需在本机启动 Shizuku", 11, false);
        content.addView(shizukuHint, margins(0, 0, 0, 4));

        LinearLayout appsHeader = new LinearLayout(this);
        appsHeader.setOrientation(LinearLayout.HORIZONTAL);
        appsHeader.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(appsHeader, margins(0, 8, 0, 2));
        TextView appsTitle = text("可启动的应用", 17, true);
        appsHeader.addView(appsTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        systemAppsSwitch = new Switch(this);
        systemAppsSwitch.setText("系统应用");
        systemAppsSwitch.setTextSize(12);
        systemAppsSwitch.setTextColor(0xfff2f2f2);
        systemAppsSwitch.setChecked(showSystemApps);
        appsHeader.addView(systemAppsSwitch, new LinearLayout.LayoutParams(-2, -2));
        editFavoritesButton = button("编辑收藏");
        appsHeader.addView(editFavoritesButton, new LinearLayout.LayoutParams(-2, -2));
        editFavoritesButton.setOnClickListener(v -> {
            editFavorites = !editFavorites;
            editFavoritesButton.setText(editFavorites ? "完成" : "编辑收藏");
            rebuildAppsGrid();
        });
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
        content.addView(appSearch, margins(0, 0, 0, 4));
        appSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { rebuildAppsGrid(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        addLaunchableApps();
    }

    private void requestShizukuAndStart() {
        if (startPending) return;
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
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
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
            autoEnableMouseIfAvailable();
            launchPendingApp();
        } catch (Exception e) {
            Log.e(TAG, "create virtual display failed", e);
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
                ? "竖屏 · 切换横屏" : "横屏 · 切换竖屏");
    }

    private void setPortraitMode(boolean portrait) {
        portraitMode = portrait;
        Log.i(TAG, "orientation=" + (portrait ? "portrait" : "landscape"));
        resizeVirtualDisplay();
        if (modeButton != null) modeButton.setText(portraitMode
                ? "竖屏 · 切换横屏" : "横屏 · 切换竖屏");
    }

    private void stopOutput() {
        Log.i(TAG, "stop displayId=" + virtualDisplayId);
        densityResizeHandler.removeCallbacks(applyPendingDensity);
        mouseBound = false;
        boundMouseDescriptor = null;
        if (mousePointerView != null) mousePointerView.setForwarding(false);
        restorePhoneScreen();
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
                autoMouseSuppressed = true;
                if (mousePointerView != null) mousePointerView.setForwarding(false);
                restorePhoneScreen();
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
                autoMouseSuppressed = false;
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

    private void autoEnableMouseIfAvailable() {
        if (autoMouseSuppressed || mouseBound || virtualDisplayId < 0 || displayService == null
                || externalDisplay == null) return;
        String descriptor = findExternalMouseDescriptor();
        if (descriptor == null) return;
        try {
            displayService.setMouseDisplay(descriptor, externalDisplay.getDisplayId());
            boundMouseDescriptor = descriptor;
            mouseBound = true;
            if (mousePointerView != null) mousePointerView.setForwarding(true);
            updateMouseRouteButton();
            status("已检测到鼠标并自动启用虚拟屏操控");
            Log.i(TAG, "auto-enabled mouse routing descriptor=" + descriptor);
        } catch (Exception e) {
            Log.e(TAG, "automatic mouse routing failed", e);
            status("自动启用鼠标失败：" + e.getMessage());
        }
    }

    private void dimAndBlackoutPhoneScreen() {
        if (phoneScreenDimmed || rootLayout == null) return;
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        previousWindowBrightness = attributes.screenBrightness;
        attributes.screenBrightness = 0f;
        getWindow().setAttributes(attributes);
        phoneScreenDimmed = true;
        View decor = getWindow().getDecorView();
        previousSystemUiVisibility = decor.getSystemUiVisibility();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        systemBarsHiddenForBlackout = true;

        blackScreenOverlay = new View(this);
        blackScreenOverlay.setBackgroundColor(0xff000000);
        blackScreenOverlay.setClickable(true);
        blackScreenOverlay.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                restorePhoneScreen();
            }
            return true;
        });
        rootLayout.addView(blackScreenOverlay, new FrameLayout.LayoutParams(-1, -1));
        blackScreenOverlay.bringToFront();
        Log.i(TAG, "phone screen dimmed and blacked out; previous window brightness="
                + previousWindowBrightness);
    }

    private void restorePhoneScreen() {
        if (blackScreenOverlay != null) {
            if (blackScreenOverlay.getParent() instanceof FrameLayout) {
                ((FrameLayout) blackScreenOverlay.getParent()).removeView(blackScreenOverlay);
            }
            blackScreenOverlay = null;
        }
        if (!phoneScreenDimmed) return;
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.screenBrightness = previousWindowBrightness;
        getWindow().setAttributes(attributes);
        phoneScreenDimmed = false;
        if (systemBarsHiddenForBlackout) {
            getWindow().getDecorView().setSystemUiVisibility(previousSystemUiVisibility);
            systemBarsHiddenForBlackout = false;
        }
        Log.i(TAG, "phone screen restored");
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
        mouseRouteButton.setText(mouseBound ? "鼠标已开 · 点击关闭" : "开启鼠标");
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
        private boolean pointerVisible;
        private boolean forwarding;

        MousePointerView(Context context) {
            super(context);
            setClickable(true);
            setFocusable(false);
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
            handler.removeCallbacks(hidePointer);
            setPointerIcon(PointerIcon.getSystemIcon(getContext(), enabled
                    ? PointerIcon.TYPE_NULL : PointerIcon.TYPE_ARROW));
            invalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!forwarding || !event.isFromSource(InputDevice.SOURCE_MOUSE)) return false;
            if (transitionFramesRemaining > 0) return true;
            updateVirtualPointer(event.getRawX(), event.getRawY());
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
                updateVirtualPointer(event.getRawX(), event.getRawY());
                showPointer();
                forwardMouseEvent(event);
            }
            return true;
        }

        private void updateVirtualPointer(float x, float y) {
            int virtualWidth = portraitMode ? baseHeight : baseWidth;
            int virtualHeight = portraitMode ? baseWidth : baseHeight;
            float scale = Math.max(virtualWidth / (float) Math.max(1, baseWidth),
                    virtualHeight / (float) Math.max(1, baseHeight));
            virtualPointerX = x * scale;
            virtualPointerY = y * scale;
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
                    Log.w(TAG, "InputManager rejected mouse event display=" + virtualDisplayId
                            + " action=" + mapped.getActionMasked() + " x=" + mapped.getX()
                            + " y=" + mapped.getY());
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
            int availableDp = Math.max(1, Math.round((right - left)
                    / getResources().getDisplayMetrics().density));
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
        autoMouseSuppressed = false;
        if (displayService == null || virtualDisplayId < 0) {
            pendingLaunchPackage = packageName;
            pendingLaunchActivity = activityName;
            requestShizukuAndStart();
            return;
        }
        try {
            autoEnableMouseIfAvailable();
            Log.i(TAG, "launch=" + packageName + "/" + activityName + " display=" + virtualDisplayId);
            displayService.launchActivity(packageName, activityName);
            if (mouseBound) dimAndBlackoutPhoneScreen();
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
        monitorView.setText("外接屏 " + externalDisplay.getName() + " · "
                + mode.getPhysicalWidth() + "×" + mode.getPhysicalHeight()
                + " @ " + Math.round(mode.getRefreshRate()) + " Hz\n"
                + "系统 ID " + externalDisplay.getDisplayId() + " · " + metrics.densityDpi + " dpi");
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
        b.setTextSize(13);
        b.setMinHeight(dp(40));
        b.setMinimumHeight(dp(40));
        b.setPadding(dp(6), 0, dp(6), 0);
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
