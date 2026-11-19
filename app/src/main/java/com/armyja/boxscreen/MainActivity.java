package com.armyja.boxscreen;

import android.app.Activity;
import android.app.Presentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Bundle;
import android.os.Build;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.view.InputDevice;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.PixelCopy;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.net.Uri;
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
import java.io.OutputStream;
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
import java.text.SimpleDateFormat;
import java.util.Date;

import rikka.shizuku.Shizuku;

public final class MainActivity extends Activity {
    private static final String TAG = "BoxScreen";
    private static final int PANEL_SCREENSHOT = 2;
    private static final int SHIZUKU_REQUEST = 410;
    private static final int MAX_DISPLAY_RECOVERY_ATTEMPTS = 12;
    private static final long DISPLAY_RECOVERY_DELAY_MS = 250;
    private final Handler densityResizeHandler = new Handler();
    private final Runnable applyPendingDensity = this::resizeVirtualDisplayForDensity;
    private final Handler displayRecoveryHandler = new Handler();
    private final Runnable recoverExternalDisplay = this::recoverExternalDisplay;
    private int densityRefreshGeneration;

    private DisplayManager displayManager;
    private InputManager inputManager;
    private volatile boolean gamepadClearPending;
    private Display externalDisplay;
    private int baseWidth;
    private int baseHeight;
    private int densityDpi = 250;
    private int landscapeDensityDpi = 250;
    private int portraitDensityDpi = 250;
    private boolean densityInitialized;
    private int virtualDisplayId = -1;
    private boolean captureBlockedDisplay;
    private int transitionFramesRemaining;
    private int displayRecoveryAttempts;
    private boolean portraitMode;
    private boolean startPending;
    private boolean reconnectOutputAfterDisplayChange;
    private String pendingLaunchPackage;
    private boolean splitOpening;
    private boolean splitActive;
    private boolean splitChoosing;
    private boolean splitPickerFirst;
    private boolean splitLastFirst = true;
    private String splitFirstPackage;
    private String splitSecondPackage;
    private float splitRatio = 0.5f;
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
    private final ArrayList<String> recentAppPackages = new ArrayList<>();
    private String currentVirtualPackage;
    private Set<String> favoritePackages = new HashSet<>();
    private Set<String> mouseModePackages = new HashSet<>();
    private Set<String> densityCompatPackages = new HashSet<>();
    private ArrayList<ResolveInfo> quickPanelAppCache;
    private final java.util.HashMap<String, ResolveInfo> launcherByPackage = new java.util.HashMap<>();
    private final java.util.HashMap<String, String> launcherLabelCache = new java.util.HashMap<>();
    private final java.util.HashMap<String, android.graphics.drawable.Drawable> launcherIconCache = new java.util.HashMap<>();
    private final java.util.HashMap<String, String> panelDisplayLabelCache = new java.util.HashMap<>();
    private boolean showSystemApps;
    private boolean editFavorites;
    private int appColumns;
    private float appGridDensity;
    private boolean appGridRetryScheduled;

    /**
     * Retry populating the grid until it really has a width and a column count. A single
     * post() is not enough: after a density change the grid can be measured again with
     * width 0 for several frames, and a one-shot retry leaves the list empty for good.
     */
    private void scheduleAppGridRetry() {
        if (appGrid == null) return;
        if (appGridRetryScheduled) return;
        appGridRetryScheduled = true;
        appGrid.getViewTreeObserver().addOnPreDrawListener(
                new android.view.ViewTreeObserver.OnPreDrawListener() {
                    @Override public boolean onPreDraw() {
                        if (appGrid == null || !appGridRetryScheduled) {
                            removeListener(this);
                            return true;
                        }
                        if (appGrid.getWidth() <= 0) return true;
                        appGridRetryScheduled = false;
                        removeListener(this);
                        updateAppGridColumns(appGrid.getWidth());
                        return true;
                    }
                });
    }

    private void removeListener(android.view.ViewTreeObserver.OnPreDrawListener listener) {
        if (appGrid == null) return;
        android.view.ViewTreeObserver observer = appGrid.getViewTreeObserver();
        if (observer.isAlive() && observer != null) observer.removeOnPreDrawListener(listener);
    }
    private Button modeButton;
    private Button mouseRouteButton;
    private boolean mouseBound;
    private String boundMouseDescriptor;
    private TextureView outputSurface;
    private MousePointerView mousePointerView;
    private Presentation presentation;
    private Bitmap screenshotPreviewBitmap;
    private Uri screenshotPreviewUri;
    private boolean screenshotPreviewExpanded;
    private String screenshotPreviewMessage;
    private final Handler screenshotPreviewHandler = new Handler();
    private final Runnable hideScreenshotPreview = this::dismissScreenshotPreview;
    private final Runnable clearScreenshotPreviewMessage = () -> {
        screenshotPreviewMessage = null;
        if (mousePointerView != null) mousePointerView.invalidate();
    };
    private IDisplayService displayService;
    private Shizuku.UserServiceArgs shizukuServiceArgs;
    private final ServiceConnection shizukuServiceConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            displayService = IDisplayService.Stub.asInterface(binder);
            if (virtualDisplayId < 0) restorePrimaryDensity();
            status("Shizuku 主机服务已连接");
            if (startPending) {
                startPending = false;
                showExternalOutput();
            }
            syncSplitState();
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

    private final InputManager.InputDeviceListener inputDeviceListener = new InputManager.InputDeviceListener() {
        @Override public void onInputDeviceAdded(int deviceId) {
            if (virtualDisplayId >= 0) clearGamepadDisplayAssociations();
        }
        @Override public void onInputDeviceRemoved(int deviceId) { }
        @Override public void onInputDeviceChanged(int deviceId) { }
    };

    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override public void onDisplayAdded(int displayId) {
            externalDisplay = findExternalDisplay();
            if (monitorView != null) updateMonitorLabel();
            if (reconnectOutputAfterDisplayChange) scheduleExternalDisplayRecovery(0);
        }
        @Override public void onDisplayRemoved(int displayId) {
            if (displayId == (externalDisplay == null ? -1 : externalDisplay.getDisplayId())) {
                beginExternalDisplayRecovery();
            }
            externalDisplay = findExternalDisplay();
            if (monitorView != null) updateMonitorLabel();
        }
        @Override public void onDisplayChanged(int displayId) {
            externalDisplay = findExternalDisplay();
            if (monitorView != null) updateMonitorLabel();
            if (reconnectOutputAfterDisplayChange) {
                scheduleExternalDisplayRecovery(DISPLAY_RECOVERY_DELAY_MS);
            } else if (presentation != null && externalDisplay != null
                    && presentation.getDisplay().getDisplayId() != externalDisplay.getDisplayId()) {
                beginExternalDisplayRecovery();
            } else if (presentation != null) {
                if (refreshExternalDisplayGeometry()) resizeVirtualDisplay();
                updateOutputLayout();
            }
        }
    };

    /**
     * The phone panel is laid out once and must fit whatever density the device reports. A
     * DPI-sync override or a restore after an update can swing it from ~400 to 560 dpi, which
     * pushes the app list below the fold on a short landscape screen. Pin the panel's own
     * density so the layout (and the visible app list) never depends on that global value.
     */
    @Override
    protected void attachBaseContext(android.content.Context base) {
        android.content.res.Configuration config =
                new android.content.res.Configuration(base.getResources().getConfiguration());
        // Keep the panel near the density it was designed for: about 1000dp across the long
        // side, but never coarser than 480 dpi (which is what used to hide the app list).
        int currentDpi = config.densityDpi;
        int longSidePx = Math.round(Math.max(config.screenWidthDp, config.screenHeightDp)
                * currentDpi / 160f);
        int targetDpi = longSidePx > 0 ? Math.round(longSidePx / 1000f * 160f) : 400;
        config.densityDpi = Math.max(320, Math.min(480, targetDpi));
        super.attachBaseContext(base.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        displayManager.registerDisplayListener(displayListener, new Handler());
        inputManager = (InputManager) getSystemService(INPUT_SERVICE);
        if (inputManager != null) {
            inputManager.registerInputDeviceListener(inputDeviceListener, new Handler());
        }
        Shizuku.addRequestPermissionResultListener(permissionListener);
        externalDisplay = findExternalDisplay();
        showSystemApps = getPreferences(MODE_PRIVATE).getBoolean("show_system_apps", false);
        favoritePackages = new HashSet<>(getPreferences(MODE_PRIVATE).getStringSet("favorite_apps", Collections.emptySet()));
        android.content.SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        mouseModePackages = new HashSet<>(preferences.getStringSet("mouse_mode_apps", Collections.emptySet()));
        densityCompatPackages = new HashSet<>(preferences.getStringSet("density_compat_apps", Collections.emptySet()));
        String savedRecentApps = preferences.getString("recent_apps", "");
        if (!savedRecentApps.isEmpty()) {
            for (String packageName : savedRecentApps.split("\\n")) {
                if (!packageName.isEmpty() && !recentAppPackages.contains(packageName)) {
                    recentAppPackages.add(packageName);
                }
            }
        }
        int savedDensity = preferences.getInt("virtual_density", 0);
        int savedLandscapeDensity = preferences.getInt("virtual_density_landscape", 0);
        int savedPortraitDensity = preferences.getInt("virtual_density_portrait", 0);
        int fallbackDensity = savedDensity >= 140 && savedDensity <= 600
                ? normalizeDensity(savedDensity) : 250;
        landscapeDensityDpi = savedLandscapeDensity >= 140 && savedLandscapeDensity <= 600
                ? normalizeDensity(savedLandscapeDensity) : fallbackDensity;
        portraitDensityDpi = savedPortraitDensity >= 140 && savedPortraitDensity <= 600
                ? normalizeDensity(savedPortraitDensity) : fallbackDensity;
        densityDpi = landscapeDensityDpi;
        densityInitialized = (savedDensity >= 140 && savedDensity <= 600)
                || (savedLandscapeDensity >= 140 && savedLandscapeDensity <= 600)
                || (savedPortraitDensity >= 140 && savedPortraitDensity <= 600);
        showRoleScreen();
        if (preferences.getBoolean("primary_density_restore_pending", false)
                && Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            bindShizukuService();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        externalDisplay = findExternalDisplay();
        if (monitorView != null) updateMonitorLabel();
        if (appGrid != null) refreshLaunchableApps();
        syncSplitState();
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        externalDisplay = findExternalDisplay();
        if (monitorView != null) updateMonitorLabel();
        if (refreshExternalDisplayGeometry()) resizeVirtualDisplay();
        updateOutputLayout();
        if (appGrid != null) {
            GridLayout grid = appGrid;
            grid.post(() -> {
                if (appGrid != grid || grid.getWidth() <= 0) return;
                updateAppGridColumns(grid.getWidth());
                rebuildAppsGrid();
            });
        }
        Log.i(TAG, "phone configuration changed orientation=" + newConfig.orientation
                + " virtualDisplay=" + virtualDisplayId);
    }

    @Override protected void onDestroy() {
        dismissScreenshotPreview();
        restorePrimaryDensity();
        densityResizeHandler.removeCallbacks(applyPendingDensity);
        displayRecoveryHandler.removeCallbacks(recoverExternalDisplay);
        if (displayService != null && virtualDisplayId >= 0) {
            try { displayService.releaseDisplay(); }
            catch (RemoteException e) { Log.w(TAG, "failed to release display on activity destroy", e); }
            virtualDisplayId = -1;
        }
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
                updateDensitySetting(140 + progress * 10, fromUser);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                densityResizeHandler.removeCallbacks(applyPendingDensity);
                if (mousePointerView != null) mousePointerView.applyPanelDensity();
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
            quickPanelAppCache = null;
            if (mousePointerView != null) mousePointerView.invalidate();
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
                    .version(23);
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
            landscapeDensityDpi = densityDpi;
            portraitDensityDpi = densityDpi;
            densityInitialized = true;
            getPreferences(MODE_PRIVATE).edit()
                    .putInt("virtual_density", densityDpi)
                    .putInt("virtual_density_landscape", landscapeDensityDpi)
                    .putInt("virtual_density_portrait", portraitDensityDpi)
                    .apply();
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
                    attachOutputSurface(texture);
                }
                @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                    resizeVirtualDisplay();
                }
                @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                    detachOutputSurfaceForRecreation();
                    return true;
                }
                @Override public void onSurfaceTextureUpdated(SurfaceTexture texture) {
                    if (mousePointerView != null) mousePointerView.onSplitOutputFrame();
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
            Surface surface = new Surface(texture);
            try {
                boolean blockCapture = pendingLaunchPackage == null ? captureBlockedDisplay
                        : "com.netease.uuremote".equals(pendingLaunchPackage);
                virtualDisplayId = displayService.createDisplay("BoxScreen", width, height,
                        densityDpi, surface, blockCapture);
                captureBlockedDisplay = blockCapture;
            } finally {
                surface.release();
            }
            updateMouseRouteButton();
            status("虚拟屏已创建，系统 display ID=" + virtualDisplayId);
            // Clear stale gamepad associations BEFORE enabling mouse routing: the
            // sweep must never race with or undo the mouse association added here.
            clearGamepadDisplayAssociations();
            if (mouseBound && boundMouseDescriptor != null) {
                rebindMouseToExternalDisplay();
            } else {
                autoEnableMouseIfAvailable();
            }
            launchPendingApp();
        } catch (Exception e) {
            Log.e(TAG, "create virtual display failed", e);
            status("创建虚拟屏失败：" + e.getMessage());
        }
    }

    private void attachOutputSurface(SurfaceTexture texture) {
        if (texture == null || displayService == null) return;
        int width = portraitMode ? baseHeight : baseWidth;
        int height = portraitMode ? baseWidth : baseHeight;
        texture.setDefaultBufferSize(width, height);
        if (virtualDisplayId < 0) {
            // The display lives in the shell service and can outlive this activity (new
            // build installed, activity recreated). Re-attach to it instead of creating a
            // second virtual display and losing the running split.
            int existing = -1;
            try {
                existing = displayService.getVirtualDisplayId();
            } catch (RemoteException | RuntimeException e) {
                Log.w(TAG, "could not read the existing virtual display", e);
            }
            if (existing < 0) {
                createVirtualDisplay(texture);
                return;
            }
            Surface surface = new Surface(texture);
            try {
                displayService.setDisplaySurface(surface);
            } catch (RemoteException | RuntimeException e) {
                Log.e(TAG, "could not re-attach the existing virtual display", e);
            } finally {
                surface.release();
            }
            virtualDisplayId = existing;
            Log.i(TAG, "re-attached existing virtual display id=" + existing);
            status("已接回运行中的虚拟屏 display ID=" + existing);
            syncSplitState();
            applyDensitySync();
            refreshVirtualDisplayLayout("re-attach virtual display");
            return;
        }

        Surface surface = new Surface(texture);
        try {
            displayService.setDisplaySurface(surface);
            Log.i(TAG, "reattached output surface to virtual display=" + virtualDisplayId);
            updateOutputLayout();
            if (mouseBound && boundMouseDescriptor != null) rebindMouseToExternalDisplay();
            status("外接屏已恢复，继续使用虚拟屏 ID=" + virtualDisplayId);
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "failed to reattach surface; recreating virtual display", e);
            virtualDisplayId = -1;
            createVirtualDisplay(texture);
        } finally {
            surface.release();
        }
    }

    private void captureVirtualScreen() {
        TextureView source = outputSurface;
        if (source == null || !source.isAvailable() || virtualDisplayId < 0) {
            Toast.makeText(this, "虚拟屏当前没有可截图的画面", Toast.LENGTH_SHORT).show();
            return;
        }
        int width = portraitMode ? baseHeight : baseWidth;
        int height = portraitMode ? baseWidth : baseHeight;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Surface surface = new Surface(source.getSurfaceTexture());
        Log.i(TAG, "screenshot requested display=" + virtualDisplayId + " size=" + width + "x" + height);
        PixelCopy.request(surface, bitmap, result -> {
            surface.release();
            if (result != PixelCopy.SUCCESS) {
                bitmap.recycle();
                Log.e(TAG, "PixelCopy failed result=" + result);
                Toast.makeText(this, "读取虚拟屏画面失败（" + result + "）", Toast.LENGTH_LONG).show();
                return;
            }
            saveVirtualScreenScreenshot(bitmap);
        }, new Handler(getMainLooper()));
    }

    private void saveVirtualScreenScreenshot(Bitmap bitmap) {
        Uri uri = null;
        try {
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(new Date());
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "BoxScreen-" + stamp + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Screenshots");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }
            uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("无法创建图库图片");
            try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
                if (stream == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw new IOException("图片写入失败");
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues ready = new ContentValues();
                ready.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, ready, null, null);
            }
            Log.i(TAG, "screenshot saved uri=" + uri);
            Toast.makeText(this, "截图已保存", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            if (uri != null) getContentResolver().delete(uri, null, null);
            bitmap.recycle();
            Log.e(TAG, "virtual display screenshot failed", e);
            Toast.makeText(this, "截图失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        showScreenshotPreview(uri, bitmap);
    }

    private void showScreenshotPreview(Uri uri, Bitmap bitmap) {
        dismissScreenshotPreview();
        screenshotPreviewUri = uri;
        screenshotPreviewBitmap = bitmap;
        screenshotPreviewExpanded = false;
        screenshotPreviewMessage = null;
        screenshotPreviewHandler.removeCallbacks(clearScreenshotPreviewMessage);
        screenshotPreviewHandler.removeCallbacks(hideScreenshotPreview);
        screenshotPreviewHandler.postDelayed(hideScreenshotPreview, 7000);
        Log.i(TAG, "screenshot thumbnail shown; auto-dismiss in 7 seconds");
        if (mousePointerView != null) mousePointerView.invalidate();
        Log.i(TAG, "external screenshot thumbnail displayed uri=" + uri);
    }

    private void dismissScreenshotPreview() {
        screenshotPreviewHandler.removeCallbacks(hideScreenshotPreview);
        screenshotPreviewHandler.removeCallbacks(clearScreenshotPreviewMessage);
        if (screenshotPreviewBitmap != null) Log.i(TAG, "screenshot preview dismissed");
        if (screenshotPreviewBitmap != null && !screenshotPreviewBitmap.isRecycled()) {
            screenshotPreviewBitmap.recycle();
        }
        screenshotPreviewBitmap = null;
        screenshotPreviewUri = null;
        screenshotPreviewExpanded = false;
        screenshotPreviewMessage = null;
        if (mousePointerView != null) mousePointerView.invalidate();
    }

    private void openScreenshotInGallery() {
        Uri uri = screenshotPreviewUri;
        if (uri == null) return;
        Intent view = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/png")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            if (displayService == null || virtualDisplayId < 0) {
                throw new IllegalStateException("虚拟屏尚未运行");
            }
            ResolveInfo gallery = getPackageManager().resolveActivity(view, 0);
            if (gallery == null || gallery.activityInfo == null) {
                throw new IllegalStateException("没有可打开图片的图库应用");
            }
            displayService.launchViewOnVirtualDisplay(gallery.activityInfo.packageName,
                    gallery.activityInfo.name, uri.toString(), "image/png");
            Log.i(TAG, "opened screenshot in gallery " + gallery.activityInfo.packageName
                    + " on virtual display=" + virtualDisplayId + " uri=" + uri);
            dismissScreenshotPreview();
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "failed to open screenshot in gallery", e);
            screenshotPreviewMessage = e instanceof SecurityException
                    ? "Android 不允许应用把系统图库启动到外接屏"
                    : "无法在外接屏打开系统图库";
            screenshotPreviewHandler.removeCallbacks(clearScreenshotPreviewMessage);
            screenshotPreviewHandler.postDelayed(clearScreenshotPreviewMessage, 6000);
            if (mousePointerView != null) mousePointerView.invalidate();
        }
    }

    private void detachOutputSurfaceForRecreation() {
        if (displayService == null || virtualDisplayId < 0) {
            releaseVirtualDisplay();
            return;
        }
        try {
            displayService.setDisplaySurface(null);
            Log.i(TAG, "detached output surface while preserving virtual display=" + virtualDisplayId);
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "failed to detach output surface; releasing virtual display", e);
            releaseVirtualDisplay();
        }
    }

    private void beginExternalDisplayRecovery() {
        if (presentation == null && virtualDisplayId < 0) return;
        reconnectOutputAfterDisplayChange = true;
        displayRecoveryAttempts = 0;
        detachOutputSurfaceForRecreation();
        Presentation oldPresentation = presentation;
        presentation = null;
        outputSurface = null;
        mousePointerView = null;
        if (oldPresentation != null) {
            try { oldPresentation.dismiss(); }
            catch (RuntimeException e) { Log.w(TAG, "failed to dismiss removed display presentation", e); }
        }
        status("外接显示器正在重新连接，虚拟屏会保留");
        scheduleExternalDisplayRecovery(DISPLAY_RECOVERY_DELAY_MS);
    }

    private void scheduleExternalDisplayRecovery(long delayMs) {
        displayRecoveryHandler.removeCallbacks(recoverExternalDisplay);
        displayRecoveryHandler.postDelayed(recoverExternalDisplay, delayMs);
    }

    private void recoverExternalDisplay() {
        externalDisplay = findExternalDisplay();
        if (externalDisplay == null) {
            if (++displayRecoveryAttempts < MAX_DISPLAY_RECOVERY_ATTEMPTS) {
                scheduleExternalDisplayRecovery(DISPLAY_RECOVERY_DELAY_MS);
            } else {
                Log.w(TAG, "external display did not return after " + displayRecoveryAttempts
                        + " recovery attempts");
                status("外接显示器尚未恢复；虚拟屏仍保留，可重新连接显示器后再打开应用");
            }
            if (monitorView != null) updateMonitorLabel();
            return;
        }

        if (monitorView != null) updateMonitorLabel();
        if (!reconnectOutputAfterDisplayChange) return;
        reconnectOutputAfterDisplayChange = false;
        if (displayService == null) {
            startPending = true;
            bindShizukuService();
            return;
        }

        Log.i(TAG, "recreating external presentation on display=" + externalDisplay.getDisplayId()
                + " while keeping virtual display=" + virtualDisplayId);
        showExternalOutput();
    }

    private void clearGamepadDisplayAssociations() {
        final IDisplayService service = displayService;
        if (service == null || gamepadClearPending) return;
        gamepadClearPending = true;
        new Thread(() -> {
            try {
                int cleared = service.clearGamepadAssociations();
                if (cleared > 0) Log.i(TAG, "cleared gamepad display associations count=" + cleared);
            } catch (RemoteException | RuntimeException e) {
                Log.w(TAG, "failed to clear gamepad display associations", e);
            } finally {
                gamepadClearPending = false;
            }
        }, "gamepad-association").start();
    }

    private void rebindMouseToExternalDisplay() {
        if (displayService == null || externalDisplay == null || boundMouseDescriptor == null) return;
        try {
            displayService.setMouseDisplay(boundMouseDescriptor, externalDisplay.getDisplayId());
            if (mousePointerView != null) mousePointerView.setForwarding(true);
            Log.i(TAG, "restored mouse association to external display="
                    + externalDisplay.getDisplayId());
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "failed to restore mouse association after display reconnect", e);
        }
    }

    private void resizeVirtualDisplay() {
        resizeVirtualDisplay(true);
    }

    private void resizeVirtualDisplayForDensity() {
        syncPrimaryDensityForPackage(currentVirtualPackage);
        resizeVirtualDisplay(false);
    }

    private void resizeVirtualDisplay(boolean forceDensityOverride) {
        if (displayService == null || virtualDisplayId < 0) {
            Log.w(TAG, "skip display resize: service=" + (displayService != null)
                    + " displayId=" + virtualDisplayId + " dpi=" + densityDpi);
            return;
        }
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
            if (forceDensityOverride) {
                displayService.resizeDisplay(width, height, densityDpi);
            } else {
                displayService.resizeDisplaySize(width, height, densityDpi);
            }
            Log.i(TAG, "display resize request sent displayId=" + virtualDisplayId
                    + " dpi=" + densityDpi + " portrait=" + portraitMode
                    + " forceDensityOverride=" + forceDensityOverride);
            updateOutputLayout();
            updateDensityStatus();
        } catch (Exception e) {
            Log.e(TAG, "display resize failed displayId=" + virtualDisplayId
                    + " dpi=" + densityDpi, e);
            status("切换显示方向失败：" + e.getMessage());
        }
    }

    private void refreshVirtualDisplayAfterDensityCommit() {
        resizeVirtualDisplayForDensity();
        refreshVirtualDisplayLayout("density commit");
    }

    private void refreshVirtualDisplayLayout(String reason) {
        refreshVirtualDisplayLayout(reason, null);
    }

    private void refreshVirtualDisplayLayout(String reason, Runnable completion) {
        if (displayService == null || virtualDisplayId < 0) {
            if (completion != null) completion.run();
            return;
        }

        final int expectedDisplayId = virtualDisplayId;
        final int expectedDensity = densityDpi;
        final boolean expectedPortrait = portraitMode;
        final int width = portraitMode ? baseHeight : baseWidth;
        final int height = portraitMode ? baseWidth : baseHeight;
        final int generation = ++densityRefreshGeneration;
        final int nudgePx = Math.max(1, Math.round(expectedDensity / 160f));
        final int nudgeWidth = width <= height ? Math.max(1, width - nudgePx) : width;
        final int nudgeHeight = width <= height ? height : Math.max(1, height - nudgePx);

        densityResizeHandler.postDelayed(() -> {
            if (!isDensityRefreshCurrent(generation, expectedDisplayId, expectedDensity,
                    expectedPortrait)) {
                if (completion != null) completion.run();
                return;
            }
            try {
                if (outputSurface != null && outputSurface.isAvailable()) {
                    outputSurface.getSurfaceTexture().setDefaultBufferSize(nudgeWidth, nudgeHeight);
                }
                displayService.resizeDisplaySize(nudgeWidth, nudgeHeight, expectedDensity);
                Log.i(TAG, "layout refresh (" + reason + ") nudge display=" + expectedDisplayId
                        + " size=" + nudgeWidth + "x" + nudgeHeight
                        + " dpi=" + expectedDensity);
            } catch (Exception e) {
                Log.w(TAG, "layout refresh (" + reason + ") nudge failed display=" + expectedDisplayId, e);
                // Restore the original buffer size even if the resize failed after changing it.
            }

            densityResizeHandler.postDelayed(() -> {
                if (!isDensityRefreshCurrent(generation, expectedDisplayId, expectedDensity,
                        expectedPortrait)) {
                    if (completion != null) completion.run();
                    return;
                }
                try {
                    if (outputSurface != null && outputSurface.isAvailable()) {
                        outputSurface.getSurfaceTexture().setDefaultBufferSize(width, height);
                    }
                    displayService.resizeDisplaySize(width, height, expectedDensity);
                    Log.i(TAG, "layout refresh (" + reason + ") restored display=" + expectedDisplayId
                            + " size=" + width + "x" + height
                            + " dpi=" + expectedDensity);
                } catch (Exception e) {
                    Log.e(TAG, "layout refresh (" + reason + ") restore failed display=" + expectedDisplayId, e);
                } finally {
                    if (completion != null) completion.run();
                }
            }, 80);
        }, 80);
    }

    private boolean isDensityRefreshCurrent(int generation, int expectedDisplayId,
                                            int expectedDensity, boolean expectedPortrait) {
        return densityRefreshGeneration == generation
                && virtualDisplayId == expectedDisplayId
                && densityDpi == expectedDensity
                && portraitMode == expectedPortrait
                && displayService != null;
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

    private int normalizeDensity(int value) {
        int bounded = Math.max(140, Math.min(600, value));
        return 140 + Math.round((bounded - 140) / 10f) * 10;
    }

    private void updateDensitySetting(int value, boolean fromUser) {
        int previousDensity = densityDpi;
        densityDpi = normalizeDensity(value);
        if (portraitMode) portraitDensityDpi = densityDpi;
        else landscapeDensityDpi = densityDpi;
        if (densityDpi != previousDensity) {
            Log.i(TAG, "density setting changed " + previousDensity + "->" + densityDpi
                    + " orientation=" + (portraitMode ? "portrait" : "landscape")
                    + " fromUser=" + fromUser);
        }
        densityInitialized = true;
        int progress = (densityDpi - 140) / 10;
        getPreferences(MODE_PRIVATE).edit()
                .putInt("virtual_density", densityDpi)
                .putInt(portraitMode ? "virtual_density_portrait" : "virtual_density_landscape", densityDpi)
                .apply();
        if (densityView != null) densityView.setText("虚拟屏 DPI：" + densityDpi);
        if (densitySlider != null && densitySlider.getProgress() != progress) {
            densitySlider.setProgress(progress);
        }
        updateDensityStatus();
        if (fromUser) {
            densityResizeHandler.removeCallbacks(applyPendingDensity);
            densityResizeHandler.postDelayed(applyPendingDensity, 220);
        }
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
        if (portraitMode == portrait) return;
        densityRefreshGeneration++;
        int oldWidth = portraitMode ? baseHeight : baseWidth;
        int oldHeight = portraitMode ? baseWidth : baseHeight;
        int newWidth = portrait ? baseHeight : baseWidth;
        int newHeight = portrait ? baseWidth : baseHeight;
        if (mousePointerView != null) {
            mousePointerView.remapVirtualPointerPosition(oldWidth, oldHeight, newWidth, newHeight);
        }
        portraitMode = portrait;
        splitRatio = SplitGeometry.clamp(splitRatio, newWidth, newHeight);
        densityDpi = portraitMode ? portraitDensityDpi : landscapeDensityDpi;
        if (mousePointerView != null) mousePointerView.applyPanelDensity();
        syncPrimaryDensityForPackage(currentVirtualPackage);
        if (densityView != null) densityView.setText("虚拟屏 DPI：" + densityDpi);
        if (densitySlider != null) densitySlider.setProgress((densityDpi - 140) / 10);
        updateDensityStatus();
        Log.i(TAG, "orientation=" + (portrait ? "portrait" : "landscape"));
        resizeVirtualDisplay();
        if (modeButton != null) modeButton.setText(portraitMode
                ? "竖屏 · 切换横屏" : "横屏 · 切换竖屏");
    }

    private void stopOutput() {
        splitActive = splitChoosing = false;
        restorePrimaryDensity();
        Log.i(TAG, "stop displayId=" + virtualDisplayId);
        densityRefreshGeneration++;
        reconnectOutputAfterDisplayChange = false;
        displayRecoveryHandler.removeCallbacks(recoverExternalDisplay);
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
        captureBlockedDisplay = false;
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
            lastHideRescheduleAt = 0;
            invalidate();
        };
        private final Paint pointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint panelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint screenshotImagePaint = new Paint(
                Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        private int panelDensityDpi = densityDpi;
        private final Path pointerShape = new Path();
        private float pointerX;
        private float pointerY;
        private float virtualPointerX;
        private float virtualPointerY;
        private float lastRawX;
        private float lastRawY;
        private boolean hasRawPosition;
        private boolean preservePointerPositionWhenRebasing;
        private boolean snapPointerToRawPositionWhenRebasing;
        private boolean touchGestureActive;
        private long touchGestureDownTime;
        private long lastHideRescheduleAt;
        private boolean pointerVisible;
        private boolean forwarding;
        private boolean statusBarVisible;
        private boolean quickPanelVisible;
        private boolean pointerOutside;
        private boolean panelPointerDown;
        private boolean panelDragFromBar;
        private boolean panelDragOpened;
        private float panelDownY;
        private int panelPressedControl;
        private boolean screenshotPreviewPointerDown;
        private String cachedStatusBarLabel;
        private long statusBarLabelUpdatedAt;

        private final Runnable revealStatusBar = () -> {
            if (forwarding && pointerY <= statusBarRevealZone()) {
                statusBarVisible = true;
                invalidate();
            }
        };
        private final Runnable hideStatusBar = () -> {
            if (pointerOutside || (pointerY > statusBarHeight()
                    && (!quickPanelVisible || pointerY > statusBarHeight() + quickPanelHeight()))) {
                statusBarVisible = false;
                quickPanelVisible = false;
                pointerOutside = false;
                invalidate();
            }
        };

        private static final int PANEL_NONE = 0;
        private static final int PANEL_SPLIT = 1;
        private long splitInputDownTime = Long.MIN_VALUE;
        private boolean splitInputTouchMode;
        private boolean splitKillMenuVisible;
        private int splitKillMenuPressed = -1;
        private boolean splitKillLongPressTriggered;
        private boolean splitKillAwaitRelease;
        private final Runnable splitKillLongPress = () -> {
            if (!splitActive || splitChoosing) return;
            // A real drag cancels the menu: once the divider has been moved, the press means
            // "resize", not "open the close-app menu".
            float axisNow = portraitMode ? virtualPointerY : virtualPointerX;
            if (Math.abs(axisNow - this.splitDownAxis) > panelDp(6)) return;
            // The menu replaces the drag: the handle goes back to the current split ratio and
            // the rest of this press is ignored until the button is released.
            splitDrag = false;
            splitKillLongPressTriggered = true;
            splitKillAwaitRelease = true;
            splitKillMenuVisible = true;
            splitKillMenuPressed = -1;
            invalidate();
        };

        private boolean splitDrag;
        /**
         * Very light by design: the handle only hints at the split. The dark rim stays dimmer
         * than the white core so it does not read as a grey smudge over light content.
         */
        private static final float SPLIT_HANDLE_RIM_OPACITY = 0.08f;
        private static final float SPLIT_HANDLE_CORE_OPACITY = 0.25f;
        private boolean splitLayoutChanging;

        /** The handle sits high on the divider so dialogs and floating IMEs rarely reach it. */
        private float splitHandleAlong() {
            return (portraitMode ? getWidth() : getHeight()) * 0.18f;
        }

        private Bitmap splitPreviousFrame;
        private int splitFreshFrames;
        private final Runnable splitFrameTimeout = this::finishSplitLayoutChange;
        private final Runnable splitLayoutWatchdog = () -> {
            if (!splitLayoutChanging) return;
            Log.w(TAG, "split layout watchdog cleared a stuck change");
            finishSplitLayoutChange();
        };
        private float splitDownAxis;
        private int splitPickerOffset;
        private ArrayList<ResolveInfo> splitPickerCache;
        private boolean splitPickerPressed;
        private static final int PANEL_VOLUME_SLIDER = 3;
        private static final int PANEL_APPS = 4;
        private static final int PANEL_CLOSE_APP = 5;
        private static final int PANEL_APP_BASE = 10;
        private static final int PANEL_RECENT_APP_BASE = 100;
        private static final int PANEL_DPI_SLIDER = 6;
        private static final int PANEL_ORIENTATION = 7;
        private static final int PANEL_INPUT_MODE = 8;
        private static final int PANEL_DENSITY_COMPAT = 9;

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
            panelPaint.setStyle(Paint.Style.FILL);
            panelTextPaint.setColor(0xffffffff);
            panelTextPaint.setTextAlign(Paint.Align.CENTER);
            panelTextPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
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
            if (!enabled) cancelTouchGesture();
            forwarding = enabled;
            pointerVisible = false;
            lastHideRescheduleAt = 0;
            statusBarVisible = false;
            quickPanelVisible = false;
            pointerOutside = false;
            panelPointerDown = false;
            hasRawPosition = false;
            preservePointerPositionWhenRebasing = false;
            snapPointerToRawPositionWhenRebasing = false;
            handler.removeCallbacks(hidePointer);
            handler.removeCallbacks(revealStatusBar);
            handler.removeCallbacks(hideStatusBar);
            setPointerIcon(PointerIcon.getSystemIcon(getContext(), enabled
                    ? PointerIcon.TYPE_NULL : PointerIcon.TYPE_ARROW));
            if (presentation != null && presentation.getWindow() != null) {
                presentation.getWindow().getDecorView().setPointerIcon(PointerIcon.getSystemIcon(
                        getContext(), enabled ? PointerIcon.TYPE_NULL : PointerIcon.TYPE_ARROW));
            }
            invalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if ((!forwarding && screenshotPreviewBitmap == null)
                    || !event.isFromSource(InputDevice.SOURCE_MOUSE)) return false;
            if (transitionFramesRemaining > 0) return true;
            updateVirtualPointer(event.getRawX(), event.getRawY());
            showPointer();
            if (splitActive && splitLayoutChanging) return true;
            updateStatusBarDwell();
            if (handleScreenshotPreviewTouch(event)) return true;
            if (handleQuickPanelTouch(event)) return true;
            if (handleSplitKillMenuTouch(event)) return true;
            if (handleSplitTouch(event)) return true;
            if (splitActive && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                splitLastFirst = isPointerInFirstSplitPane();
            }
            if (usesTouchMode(event)) forwardTouchModeEvent(event);
            else forwardMouseEvent(event);
            return true;
        }

        @Override
        public boolean onGenericMotionEvent(MotionEvent event) {
            if ((!forwarding && screenshotPreviewBitmap == null)
                    || !event.isFromSource(InputDevice.SOURCE_MOUSE)) return false;
            int action = event.getActionMasked();
            if (action != MotionEvent.ACTION_HOVER_ENTER
                    && action != MotionEvent.ACTION_HOVER_MOVE
                    && action != MotionEvent.ACTION_HOVER_EXIT
                    && action != MotionEvent.ACTION_SCROLL
                    && action != MotionEvent.ACTION_BUTTON_PRESS
                    && action != MotionEvent.ACTION_BUTTON_RELEASE) return false;
            if (transitionFramesRemaining > 0) return true;
            updateVirtualPointer(event.getRawX(), event.getRawY());
            showPointer();
            if (splitActive && splitLayoutChanging) return true;
            updateStatusBarDwell();
            if (screenshotPreviewBitmap != null && handleScreenshotPreviewTouch(event)) return true;
            if (action == MotionEvent.ACTION_HOVER_EXIT) {
                pointerOutside = true;
                handler.removeCallbacks(revealStatusBar);
                if (statusBarVisible || quickPanelVisible) {
                    handler.removeCallbacks(hideStatusBar);
                    handler.postDelayed(hideStatusBar, 500);
                }
            }
            if (splitKillMenuVisible) return true;
            if ((quickPanelVisible || statusBarVisible) && isOverQuickUi(virtualPointerX, virtualPointerY)) {
                if (action == MotionEvent.ACTION_SCROLL) adjustVolume(event.getAxisValue(MotionEvent.AXIS_VSCROLL) > 0 ? 1 : -1);
                return true;
            }
            if (splitActive && !splitChoosing
                    && Math.abs((portraitMode ? virtualPointerY : virtualPointerX)
                    - splitRatio * (portraitMode ? getHeight() : getWidth())) <= panelDp(20)) {
                return true;
            }
            if (splitChoosing && overSplitPicker()) {
                if (action == MotionEvent.ACTION_SCROLL) {
                    int direction = event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0 ? 1 : -1;
                    splitPickerOffset = Math.max(0, Math.min(Math.max(0, (splitPickerApps().size() - 1)
                                    / splitPickerColumns() * splitPickerColumns()),
                            splitPickerOffset + direction * splitPickerColumns()));
                    invalidate();
                }
                return true;
            }
            if (action == MotionEvent.ACTION_HOVER_EXIT) {
                if (!usesTouchMode(event)) forwardMouseEvent(event);
                if (portraitMode && event.getRawX() <= 2f) {
                    pointerOutside = false;
                    handler.removeCallbacks(hidePointer);
                    pointerVisible = true;
                    lastHideRescheduleAt = 0;
                    Log.i(TAG, "preserving pointer at portrait left edge rawX=" + event.getRawX());
                    invalidate();
                } else {
                    handler.removeCallbacks(hidePointer);
                    pointerVisible = false;
                    lastHideRescheduleAt = 0;
                    invalidate();
                }
            } else if (usesTouchMode(event)) {
                if (action == MotionEvent.ACTION_SCROLL) forwardMouseEvent(event);
                else forwardTouchModeEvent(event);
            } else {
                forwardMouseEvent(event);
            }
            return true;
        }

        private boolean isPointerInFirstSplitPane() {
            float axis = portraitMode ? virtualPointerY : virtualPointerX;
            int extent = portraitMode ? getHeight() : getWidth();
            return axis < splitRatio * extent;
        }

        private boolean usesTouchMode(MotionEvent event) {
            if (!splitActive) return isTouchMode(currentVirtualPackage);
            String pkg = isPointerInFirstSplitPane() ? splitFirstPackage : splitSecondPackage;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_MOVE
                    || action == MotionEvent.ACTION_HOVER_EXIT || action == MotionEvent.ACTION_SCROLL) {
                return isTouchMode(pkg);
            }
            // Generic button events and touch dispatch can belong to the same mouse gesture.
            if (splitInputDownTime != event.getDownTime()) {
                splitInputDownTime = event.getDownTime();
                splitInputTouchMode = isTouchMode(pkg);
            }
            return splitInputTouchMode;
        }

        private void forwardTouchModeEvent(MotionEvent source) {
            int action = source.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // Only the primary button starts a touch gesture. Any other first
                    // button also arrives as ACTION_DOWN, so without this guard right
                    // and middle clicks would behave exactly like left clicks.
                    if ((source.getButtonState() & MotionEvent.BUTTON_PRIMARY) != 0) {
                        beginTouchGesture(source);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (touchGestureActive) injectTouchAction(source, MotionEvent.ACTION_MOVE);
                    break;
                case MotionEvent.ACTION_UP:
                    finishTouchGesture(source, MotionEvent.ACTION_UP);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    finishTouchGesture(source, MotionEvent.ACTION_CANCEL);
                    break;
                case MotionEvent.ACTION_BUTTON_PRESS:
                    if (source.getActionButton() == MotionEvent.BUTTON_PRIMARY
                            || (source.getButtonState() & MotionEvent.BUTTON_PRIMARY) != 0) {
                        beginTouchGesture(source);
                    }
                    break;
                case MotionEvent.ACTION_BUTTON_RELEASE:
                    int button = source.getActionButton();
                    if (button == MotionEvent.BUTTON_SECONDARY && !touchGestureActive) {
                        sendVirtualKey("BACK");
                    } else if (button == MotionEvent.BUTTON_TERTIARY && !touchGestureActive) {
                        sendVirtualKey("MENU");
                    } else if (touchGestureActive
                            && (button == MotionEvent.BUTTON_PRIMARY
                            || (source.getButtonState() & MotionEvent.BUTTON_PRIMARY) == 0)) {
                        finishTouchGesture(source, MotionEvent.ACTION_UP);
                    }
                    break;
                default:
                    break;
            }
        }

        private final java.util.concurrent.ExecutorService virtualInputExecutor =
                java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "virtual-input");
                    t.setDaemon(true);
                    return t;
                });
        private volatile long lastKeyInjectAt;
        // OPPO's key interceptor moves the target display to top and waits for its
        // animation on every injected key, so rapid right-clicks used to starve the
        // main screen's focus for seconds (pointer frozen). Throttle keys hard.
        private static final long KEY_INJECT_MIN_INTERVAL_MS = 250;

        private void sendVirtualKey(String keyCode) {
            final IDisplayService service = displayService;
            final int virtualId = virtualDisplayId;
            if (service == null || virtualId < 0) return;
            // No app on the virtual display means no focused window to receive the
            // key. Injecting into that state fails on system_server (no focused
            // window) while its OPPO key interceptor still moves the display to top
            // and waits for the animation, starving the main screen's focus. Skip.
            // Check live, not just currentVirtualPackage, so self-exited apps count.
            final boolean hasTask;
            try {
                hasTask = service.hasVirtualDisplayTask();
            } catch (RemoteException | RuntimeException e) {
                return;
            }
            if (!hasTask) return;
            int code = "BACK".equals(keyCode) ? android.view.KeyEvent.KEYCODE_BACK
                    : "MENU".equals(keyCode) ? android.view.KeyEvent.KEYCODE_MENU : 0;
            if (code == 0) return;
            final long now = android.os.SystemClock.uptimeMillis();
            if (now - lastKeyInjectAt < KEY_INJECT_MIN_INTERVAL_MS) return;
            lastKeyInjectAt = now;
            // system_server may block inside the injection for seconds while a
            // display transition waits for animations, so never run this on the
            // main thread: a single background thread also keeps down/up order.
            virtualInputExecutor.execute(() -> {
                try {
                    if (service.injectKey(code, true)) service.injectKey(code, false);
                } catch (RemoteException | RuntimeException e) {
                    Log.w(TAG, "failed to inject " + keyCode + " to virtual display", e);
                }
            });
        }

        private void beginTouchGesture(MotionEvent source) {
            if (touchGestureActive) return;
            touchGestureDownTime = source.getEventTime();
            touchGestureActive = true;
            injectTouchAction(source, MotionEvent.ACTION_DOWN);
        }

        private void finishTouchGesture(MotionEvent source, int action) {
            if (!touchGestureActive) return;
            injectTouchAction(source, action);
            touchGestureActive = false;
        }

        void cancelTouchGesture() {
            splitInputDownTime = Long.MIN_VALUE;
            if (!touchGestureActive) return;
            injectTouchAction(null, MotionEvent.ACTION_CANCEL);
            touchGestureActive = false;
        }

        private void injectTouchAction(MotionEvent source, int action) {
            if (displayService == null || virtualDisplayId < 0 || transitionFramesRemaining > 0) return;
            if (baseWidth <= 0 || baseHeight <= 0) return;
            long eventTime = source == null ? android.os.SystemClock.uptimeMillis() : source.getEventTime();
            MotionEvent.PointerProperties properties = new MotionEvent.PointerProperties();
            properties.id = 0;
            properties.toolType = MotionEvent.TOOL_TYPE_FINGER;
            MotionEvent.PointerCoords coordinates = new MotionEvent.PointerCoords();
            coordinates.x = virtualPointerX;
            coordinates.y = virtualPointerY;
            coordinates.pressure = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL ? 0f : 1f;
            coordinates.size = 1f;
            MotionEvent touch = MotionEvent.obtain(touchGestureDownTime, eventTime, action,
                    1, new MotionEvent.PointerProperties[]{properties},
                    new MotionEvent.PointerCoords[]{coordinates},
                    source == null ? 0 : source.getMetaState(), 0, 1f, 1f, 0, 0,
                    InputDevice.SOURCE_TOUCHSCREEN, 0);
            final MotionEvent touchEvent = touch;
            final IDisplayService service = displayService;
            final int displayId = virtualDisplayId;
            virtualInputExecutor.execute(() -> {
                try {
                    if (!service.injectInputEvent(touchEvent, displayId)) {
                        Log.w(TAG, "InputManager rejected synthetic touch display=" + displayId
                                + " action=" + action + " x=" + virtualPointerX + " y=" + virtualPointerY);
                    }
                } catch (RemoteException | RuntimeException e) {
                    Log.e(TAG, "Unable to inject synthetic touch to virtual display", e);
                } finally {
                    touchEvent.recycle();
                }
            });
        }

        private void updateVirtualPointer(float x, float y) {
            pointerOutside = false;
            int virtualWidth = portraitMode ? baseHeight : baseWidth;
            int virtualHeight = portraitMode ? baseWidth : baseHeight;
            float scale = Math.max(virtualWidth / (float) Math.max(1, baseWidth),
                    virtualHeight / (float) Math.max(1, baseHeight));
            if (hasRawPosition) {
                virtualPointerX += (x - lastRawX) * scale;
                virtualPointerY += (y - lastRawY) * scale;
            } else if (snapPointerToRawPositionWhenRebasing) {
                // Returning from portrait to landscape also returns to the host's raw
                // coordinate frame. Start from the mouse's current absolute position so
                // the remaining travel to either edge is not shortened by the old frame.
                virtualPointerX = x * scale;
                virtualPointerY = y * scale;
                snapPointerToRawPositionWhenRebasing = false;
                preservePointerPositionWhenRebasing = false;
                Log.i(TAG, "pointer raw baseline restored at " + x + "," + y
                        + " -> virtual=" + virtualPointerX + "," + virtualPointerY);
            } else if (!preservePointerPositionWhenRebasing) {
                virtualPointerX = x * scale;
                virtualPointerY = y * scale;
            } else {
                // The virtual coordinates were already remapped for the new orientation.
                // Seed the raw input baseline without replacing that remapped position.
                preservePointerPositionWhenRebasing = false;
            }
            lastRawX = x;
            lastRawY = y;
            hasRawPosition = true;
            virtualPointerX = Math.max(0f, Math.min(virtualWidth - 1f, virtualPointerX));
            virtualPointerY = Math.max(0f, Math.min(virtualHeight - 1f, virtualPointerY));
        }

        void remapVirtualPointerPosition(int oldWidth, int oldHeight, int newWidth, int newHeight) {
            if (oldWidth <= 0 || oldHeight <= 0 || newWidth <= 0 || newHeight <= 0) return;
            virtualPointerX = Math.max(0f, Math.min(newWidth - 1f,
                    virtualPointerX / oldWidth * newWidth));
            virtualPointerY = Math.max(0f, Math.min(newHeight - 1f,
                    virtualPointerY / oldHeight * newHeight));
            pointerX = virtualPointerX;
            pointerY = virtualPointerY;
            hasRawPosition = false;
            snapPointerToRawPositionWhenRebasing = oldWidth < oldHeight && newWidth > newHeight;
            preservePointerPositionWhenRebasing = !snapPointerToRawPositionWhenRebasing;
            Log.i(TAG, "pointer position remapped " + oldWidth + "x" + oldHeight + " -> "
                    + newWidth + "x" + newHeight + "; snapToRaw="
                    + snapPointerToRawPositionWhenRebasing);
        }

        private int panelDp(int value) {
            return Math.round(value * panelDensityDpi / 160f);
        }

        void applyPanelDensity() {
            if (panelDensityDpi == densityDpi) return;
            panelDensityDpi = densityDpi;
            panelDisplayLabelCache.clear();
            invalidate();
        }

        private float statusBarHeight() { return panelDp(48); }
        private float statusBarRevealZone() { return panelDp(3); }

        private float quickPanelHeight() {
            int appCount = quickPanelApps().size();
            int columns = quickPanelColumnCount(appCount);
            int rows = (appCount + columns - 1) / columns;
            float contentHeight = panelDp(120) + rows * panelDp(58)
                    + Math.max(0, rows - 1) * panelDp(8) + panelDp(12);
            return Math.min(Math.max(0, getHeight() - statusBarHeight()), contentHeight);
        }

        private int quickPanelColumnCount(int appCount) {
            if (appCount <= 0) return 1;
            float availableWidth = getWidth() - panelDp(32);
            float gap = panelDp(8);
            int columnsByWidth = Math.max(1, (int) ((availableWidth + gap) / (panelDp(128) + gap)));
            return Math.min(appCount, columnsByWidth);
        }

        private float quickPanelAppsTop() { return statusBarHeight() + panelDp(120); }

        private float dpiSliderLeft() { return panelDp(118); }

        private float dpiSliderRight() { return Math.max(dpiSliderLeft() + 1, getWidth() - panelDp(164)); }

        private void updateStatusBarDwell() {
            if (!forwarding) return;
            if (virtualPointerY <= statusBarRevealZone()) {
                handler.removeCallbacks(hideStatusBar);
                if (!statusBarVisible) {
                    handler.removeCallbacks(revealStatusBar);
                    handler.postDelayed(revealStatusBar, 500);
                }
            } else {
                handler.removeCallbacks(revealStatusBar);
                boolean insideVisiblePanel = quickPanelVisible
                        && virtualPointerY <= statusBarHeight() + quickPanelHeight();
                boolean insideVisibleBar = statusBarVisible && virtualPointerY <= statusBarHeight();
                if (statusBarVisible && !insideVisiblePanel && !insideVisibleBar) {
                    handler.removeCallbacks(hideStatusBar);
                    handler.postDelayed(hideStatusBar, 500);
                } else if (insideVisiblePanel || insideVisibleBar) {
                    handler.removeCallbacks(hideStatusBar);
                }
            }
        }

        private boolean isOverQuickUi(float x, float y) {
            return (statusBarVisible && y <= statusBarHeight())
                    || (quickPanelVisible && y <= statusBarHeight() + quickPanelHeight());
        }

        private boolean handleQuickPanelTouch(MotionEvent event) {
            int action = event.getActionMasked();
            float x = virtualPointerX;
            float y = virtualPointerY;
            if (action == MotionEvent.ACTION_DOWN) {
                panelPointerDown = false;
                panelDragOpened = false;
                panelDragFromBar = false;
                if (quickPanelVisible && y <= statusBarHeight() + quickPanelHeight()) {
                    panelPointerDown = true;
                    panelDownY = y;
                    panelPressedControl = hitQuickControl(x, y);
                    if (panelPressedControl == PANEL_DPI_SLIDER) {
                        Log.i(TAG, "DPI slider down x=" + x + " y=" + y
                                + " dpi=" + densityDpi + " portrait=" + portraitMode);
                    }
                    if (panelPressedControl == PANEL_VOLUME_SLIDER) setStatusVolumeFromPointer(x);
                    else if (panelPressedControl == PANEL_DPI_SLIDER) setDensityFromPointer(x, false);
                    return true;
                }
                if (statusBarVisible && y <= statusBarHeight()) {
                    panelPointerDown = true;
                    panelPressedControl = hitStatusBarControl(x);
                    panelDragFromBar = panelPressedControl == PANEL_APPS;
                    panelDownY = y;
                    if (panelPressedControl == PANEL_VOLUME_SLIDER) setStatusVolumeFromPointer(x);
                    return true;
                }
                if (quickPanelVisible) {
                    // Consume both DOWN and its matching UP when dismissing the panel.
                    // Otherwise the UP leaks through to the virtual display without a DOWN.
                    panelPointerDown = true;
                    panelPressedControl = PANEL_NONE;
                    quickPanelVisible = false;
                    statusBarVisible = false;
                    invalidate();
                    return true;
                }
                return false;
            }
            if (!panelPointerDown) return false;
            if (action == MotionEvent.ACTION_MOVE) {
                if (panelDragFromBar && y - panelDownY >= panelDp(56)) {
                    panelDragOpened = true;
                    quickPanelVisible = true;
                    statusBarVisible = true;
                }
                if (panelPressedControl == PANEL_VOLUME_SLIDER) setStatusVolumeFromPointer(x);
                else if (panelPressedControl == PANEL_DPI_SLIDER) setDensityFromPointer(x, false);
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                if (panelPressedControl == PANEL_DPI_SLIDER) {
                    Log.i(TAG, "DPI slider up x=" + x + " y=" + y + " dpi=" + densityDpi);
                    setDensityFromPointer(x, true);
                }
                if (panelDragFromBar && y - panelDownY >= panelDp(56)) {
                    quickPanelVisible = true;
                    statusBarVisible = true;
                } else if (!panelDragOpened && panelPressedControl != PANEL_DPI_SLIDER) {
                    int releasedControl = hitQuickControl(x, y);
                    if (releasedControl == panelPressedControl) activateQuickControl(releasedControl, x);
                }
                panelPointerDown = false;
                panelDragFromBar = false;
                panelPressedControl = PANEL_NONE;
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                panelPointerDown = false;
                panelDragFromBar = false;
                panelPressedControl = PANEL_NONE;
                return true;
            }
            return true;
        }

        private int hitQuickControl(float x, float y) {
            float width = getWidth();
            float barHeight = statusBarHeight();
            if (y <= barHeight) return hitStatusBarControl(x);
            if (!quickPanelVisible || y > barHeight + quickPanelHeight()) return PANEL_NONE;
            float controlsTop = barHeight + panelDp(8);
            float controlsBottom = barHeight + panelDp(64);
            if (y >= controlsTop && y <= controlsBottom) {
                if (x >= getWidth() - panelDp(48) && x <= getWidth() - panelDp(12)) {
                    return PANEL_SCREENSHOT;
                }
                if (x >= getWidth() - panelDp(94) && x <= getWidth() - panelDp(58)) {
                    return PANEL_ORIENTATION;
                }
                if (x >= getWidth() - panelDp(152) && x <= getWidth() - panelDp(106)) {
                    return PANEL_INPUT_MODE;
                }
                if (x >= dpiSliderLeft() && x <= dpiSliderRight()) return PANEL_DPI_SLIDER;
                return PANEL_NONE;
            }
            float compatCenterY = barHeight + panelDp(91);
            if (!splitActive && currentVirtualPackage != null && !captureBlockedDisplay
                    && x >= width - panelDp(72) && x <= width - panelDp(6)
                    && Math.abs(y - compatCenterY) <= panelDp(18)) return PANEL_SPLIT;
            if (y >= compatCenterY - panelDp(17) && y <= compatCenterY + panelDp(17)) {
                return PANEL_DENSITY_COMPAT;
            }
            float appsTop = quickPanelAppsTop();
            float tileHeight = panelDp(58);
            float gap = panelDp(8);
            if (y >= appsTop) {
                int row = (int) ((y - appsTop) / (tileHeight + gap));
                ArrayList<ResolveInfo> apps = quickPanelApps();
                int columns = quickPanelColumnCount(apps.size());
                float horizontalPadding = panelDp(16);
                float tileGap = panelDp(8);
                float tileWidth = (width - horizontalPadding * 2f - tileGap * (columns - 1)) / columns;
                float offsetX = x - horizontalPadding;
                if (offsetX < 0 || tileWidth <= 0) return PANEL_NONE;
                int column = (int) (offsetX / (tileWidth + tileGap));
                if (column >= columns || offsetX - column * (tileWidth + tileGap) > tileWidth) return PANEL_NONE;
                int index = row * columns + column;
                if (index >= 0 && index < apps.size()) return PANEL_APP_BASE + index;
            }
            return PANEL_NONE;
        }

        private void activateQuickControl(int control, float x) {
            if (control == PANEL_SPLIT) {
                quickPanelVisible = statusBarVisible = false;
                splitPickerOffset = 0;
                splitPickerCache = null;
                if (splitActive) expandCurrentSplit();
                else updateSplit(null);
            } else if (control == PANEL_VOLUME_SLIDER) setStatusVolumeFromPointer(x);
            else if (control == PANEL_DPI_SLIDER) setDensityFromPointer(x, true);
            else if (control == PANEL_ORIENTATION) setPortraitMode(!portraitMode);
            else if (control == PANEL_INPUT_MODE) toggleCurrentInputMode();
            else if (control == PANEL_DENSITY_COMPAT) toggleDensityCompat();
            else if (control == PANEL_SCREENSHOT) {
                Log.i(TAG, "quick panel screenshot button activated x=" + x);
                captureVirtualScreen();
            }
            else if (control == PANEL_APPS) {
                quickPanelVisible = true;
                statusBarVisible = true;
            } else if (control == PANEL_CLOSE_APP) {
                closeCurrentVirtualApp();
            } else if (control >= PANEL_RECENT_APP_BASE) {
                launchRecentApp(control - PANEL_RECENT_APP_BASE);
            } else if (control >= PANEL_APP_BASE) {
                launchQuickPanelApp(control - PANEL_APP_BASE);
            }
            invalidate();
        }

        private void adjustVolume(int direction) {
            AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audio == null) return;
            int current = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audio.setStreamVolume(AudioManager.STREAM_MUSIC,
                    Math.max(0, Math.min(max, current + direction)), 0);
            invalidate();
        }

        private void setStatusVolumeFromPointer(float x) {
            StatusBarLayout layout = statusBarLayout();
            AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audio == null || layout.sliderRight <= layout.sliderLeft) return;
            float ratio = Math.max(0f, Math.min(1f, (x - layout.sliderLeft)
                    / (layout.sliderRight - layout.sliderLeft)));
            int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(max * ratio), 0);
            invalidate();
        }

        private void setDensityFromPointer(float x, boolean finished) {
            float left = dpiSliderLeft();
            float right = dpiSliderRight();
            float ratio = Math.max(0f, Math.min(1f, (x - left) / (right - left)));
            int step = Math.round(ratio * 46f);
            int selectedDensity = 140 + step * 10;
            if (finished) {
                Log.i(TAG, "DPI slider commit x=" + x + " track=" + left + ".." + right
                        + " selected=" + selectedDensity + " current=" + densityDpi
                        + " portrait=" + portraitMode);
            }
            if (selectedDensity != densityDpi) updateDensitySetting(selectedDensity, true);
            if (finished) {
                densityResizeHandler.removeCallbacks(applyPendingDensity);
                applyPanelDensity();
                syncPrimaryDensityForPackage(currentVirtualPackage);
                refreshVirtualDisplayAfterDensityCommit();
            }
            invalidate();
        }

        private final class StatusBarLayout {
            final String label;
            final ArrayList<ResolveInfo> apps;
            final float sliderLeft;
            final float sliderRight;
            final float appsLeft;
            final float closeLeft;

            StatusBarLayout(String label, ArrayList<ResolveInfo> apps, float sliderLeft,
                            float sliderRight, float appsLeft, float closeLeft) {
                this.label = label;
                this.apps = apps;
                this.sliderLeft = sliderLeft;
                this.sliderRight = sliderRight;
                this.appsLeft = appsLeft;
                this.closeLeft = closeLeft;
            }
        }

        private StatusBarLayout statusBarLayout() {
            panelTextPaint.setTextSize(panelDp(15));
            String label = statusBarLabel();
            ArrayList<ResolveInfo> apps = recentStatusApps();
            float width = getWidth();
            float closeLeft = width - panelDp(44);
            float appsLeft = closeLeft - panelDp(8) - apps.size() * panelDp(42);
            float sliderLeft = panelDp(16) + panelTextPaint.measureText(label) + panelDp(14);
            float sliderRight = Math.min(sliderLeft + panelDp(150), appsLeft - panelDp(12));
            return new StatusBarLayout(label, apps, sliderLeft, sliderRight, appsLeft, closeLeft);
        }

        private String statusBarLabel() {
            long now = android.os.SystemClock.uptimeMillis();
            if (cachedStatusBarLabel != null && now - statusBarLabelUpdatedAt < 1000) {
                return cachedStatusBarLabel;
            }
            String time = new SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new Date());
            BatteryManager battery = (BatteryManager) getSystemService(BATTERY_SERVICE);
            int percent = battery == null ? -1
                    : battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            cachedStatusBarLabel = percent < 0 ? time : time + "   " + percent + "%";
            statusBarLabelUpdatedAt = now;
            return cachedStatusBarLabel;
        }

        private ArrayList<ResolveInfo> recentStatusApps() {
            ArrayList<ResolveInfo> apps = new ArrayList<>();
            for (String packageName : recentAppPackages) {
                if (packageName.equals(currentVirtualPackage)) continue;
                ResolveInfo app = launcherByPackage.get(packageName);
                if (app != null) apps.add(app);
                if (apps.size() >= 3) break;
            }
            return apps;
        }

        private int hitStatusBarControl(float x) {
            StatusBarLayout layout = statusBarLayout();
            if (x >= layout.closeLeft) return PANEL_CLOSE_APP;
            float appsRight = layout.closeLeft - panelDp(8);
            if (x >= layout.appsLeft && x < appsRight) {
                int index = (int) ((x - layout.appsLeft) / panelDp(42));
                if (index >= 0 && index < layout.apps.size()) return PANEL_RECENT_APP_BASE + index;
            }
            if (x >= layout.sliderLeft && x <= layout.sliderRight) return PANEL_VOLUME_SLIDER;
            return PANEL_APPS;
        }

        private void launchRecentApp(int index) {
            ArrayList<ResolveInfo> apps = recentStatusApps();
            if (index < 0 || index >= apps.size()) return;
            ResolveInfo app = apps.get(index);
            quickPanelVisible = false;
            statusBarVisible = false;
            launchPackage(app.activityInfo.packageName, app.activityInfo.name);
        }

        private void closeCurrentVirtualApp() {
            if (splitOpening) return;
            if (displayService == null || virtualDisplayId < 0) return;
            if (mousePointerView != null) mousePointerView.cancelTouchGesture();
            try {
                boolean removed = displayService.closeTopTaskOnVirtualDisplay();
                Log.i(TAG, "close foreground virtual display task result=" + removed
                        + " package=" + currentVirtualPackage);
                if (removed) {
                    splitActive = splitChoosing = false;
                    restorePrimaryDensity();
                    currentVirtualPackage = null;
                    // Closing the last app leaves the virtual display blank; show the
                    // status bar and quick panel right away as the next affordance.
                    // The regular dwell logic hides them again once the pointer leaves
                    // their area, so mouse behaviour stays unchanged.
                    quickPanelVisible = true;
                    statusBarVisible = true;
                } else {
                    status("虚拟屏上没有可关闭的应用任务");
                }
            } catch (RemoteException | RuntimeException e) {
                Log.e(TAG, "unable to close foreground app on virtual display", e);
                status("关闭虚拟屏应用失败：" + e.getMessage());
            }
            invalidate();
        }

        private ArrayList<ResolveInfo> quickPanelApps() {
            if (quickPanelAppCache != null) return quickPanelAppCache;
            ArrayList<ResolveInfo> candidates = new ArrayList<>();
            for (ResolveInfo app : launcherByPackage.values()) {
                String packageName = app.activityInfo.packageName;
                if (packageName.equals(getPackageName())) continue;
                int flags = app.activityInfo.applicationInfo.flags;
                boolean system = (flags & (android.content.pm.ApplicationInfo.FLAG_SYSTEM
                        | android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
                if (system && !showSystemApps) continue;
                candidates.add(app);
            }
            java.util.Comparator<ResolveInfo> byLabel = (a, b) ->
                    launcherLabel(a).compareToIgnoreCase(launcherLabel(b));
            ArrayList<ResolveInfo> ordered = new ArrayList<>();
            HashSet<String> added = new HashSet<>();
            ArrayList<ResolveInfo> favorites = new ArrayList<>();
            for (ResolveInfo app : candidates) {
                if (favoritePackages.contains(app.activityInfo.packageName)) favorites.add(app);
            }
            Collections.sort(favorites, byLabel);
            for (ResolveInfo app : favorites) {
                ordered.add(app);
                added.add(app.activityInfo.packageName);
            }
            for (String packageName : recentAppPackages) {
                ResolveInfo app = launcherByPackage.get(packageName);
                if (app != null && !showSystemApps) {
                    int flags = app.activityInfo.applicationInfo.flags;
                    if ((flags & (android.content.pm.ApplicationInfo.FLAG_SYSTEM
                            | android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) app = null;
                }
                if (app != null && added.add(packageName)) ordered.add(app);
            }
            Collections.sort(candidates, byLabel);
            for (ResolveInfo app : candidates) {
                if (added.add(app.activityInfo.packageName)) ordered.add(app);
            }
            quickPanelAppCache = ordered.size() > 12
                    ? new ArrayList<>(ordered.subList(0, 12)) : ordered;
            return quickPanelAppCache;
        }

        private String launcherLabel(ResolveInfo app) {
            return getLauncherLabel(app);
        }

        private android.graphics.drawable.Drawable launcherIcon(ResolveInfo app) {
            return getLauncherIcon(app);
        }

        private String panelDisplayLabel(ResolveInfo app, float width) {
            String key = app.activityInfo.packageName + ":" + Math.round(width);
            String label = panelDisplayLabelCache.get(key);
            if (label == null) {
                label = android.text.TextUtils.ellipsize(launcherLabel(app),
                        new android.text.TextPaint(panelTextPaint), width - panelDp(16),
                        android.text.TextUtils.TruncateAt.END).toString();
                panelDisplayLabelCache.put(key, label);
            }
            return label;
        }

        private void launchQuickPanelApp(int index) {
            ArrayList<ResolveInfo> apps = quickPanelApps();
            if (index < 0 || index >= apps.size()) return;
            ResolveInfo app = apps.get(index);
            quickPanelVisible = false;
            statusBarVisible = false;
            launchPackage(app.activityInfo.packageName, app.activityInfo.name);
        }

        private void drawQuickUi(Canvas canvas) {
            float width = getWidth();
            float barHeight = statusBarHeight();
            panelPaint.setColor(0xb8171a20);
            panelPaint.setStyle(Paint.Style.FILL);
            panelTextPaint.setColor(0xe6ffffff);
            if (quickPanelVisible) {
                float bottom = barHeight + quickPanelHeight();
                canvas.drawRect(0, 0, width, bottom, panelPaint);

                ArrayList<ResolveInfo> apps = quickPanelApps();
                float controlsTop = barHeight + panelDp(8);
                float controlsBottom = barHeight + panelDp(64);
                float controlsCenterY = barHeight + panelDp(36);
                panelPaint.setColor(0x6630343d);
                canvas.drawRect(width - panelDp(152), controlsTop + panelDp(4), width - panelDp(8),
                        controlsBottom - panelDp(4), panelPaint);
                panelPaint.setColor(inputSettingsPackage() == null || isTouchMode(inputSettingsPackage())
                        ? 0x997a5b32 : 0x664b6470);
                canvas.drawRect(width - panelDp(152), controlsTop + panelDp(8), width - panelDp(106),
                        controlsBottom - panelDp(8), panelPaint);
                panelPaint.setColor(0x664b6470);
                canvas.drawRect(width - panelDp(94), controlsTop + panelDp(8), width - panelDp(58),
                        controlsBottom - panelDp(8), panelPaint);
                panelPaint.setColor(0x664b6470);
                canvas.drawRect(width - panelDp(48), controlsTop + panelDp(8), width - panelDp(12),
                        controlsBottom - panelDp(8), panelPaint);
                panelTextPaint.setColor(0xe6ffffff);
                panelTextPaint.setTextSize(panelDp(14));
                panelTextPaint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("DPI " + densityDpi, panelDp(16), controlsCenterY + panelDp(5), panelTextPaint);
                float sliderLeft = dpiSliderLeft();
                float sliderRight = dpiSliderRight();
                float densityRatio = (densityDpi - 140) / 460f;
                panelPaint.setColor(0x9977808d);
                canvas.drawRoundRect(sliderLeft, controlsCenterY - panelDp(3), sliderRight,
                        controlsCenterY + panelDp(3), panelDp(4), panelDp(4), panelPaint);
                panelPaint.setColor(0xdd80cbc4);
                canvas.drawRoundRect(sliderLeft, controlsCenterY - panelDp(3),
                        sliderLeft + (sliderRight - sliderLeft) * densityRatio,
                        controlsCenterY + panelDp(3), panelDp(4), panelDp(4), panelPaint);
                canvas.drawCircle(sliderLeft + (sliderRight - sliderLeft) * densityRatio,
                        controlsCenterY, panelDp(8), panelPaint);
                drawInputModeIcon(canvas, width - panelDp(129), controlsCenterY,
                        inputSettingsPackage() == null || isTouchMode(inputSettingsPackage()));
                drawOrientationIcon(canvas, width - panelDp(76), controlsCenterY);
                drawPanelVectorIcon(canvas, R.drawable.ic_screenshot,
                        width - panelDp(30), controlsCenterY, panelDp(26));

                float compatY = barHeight + panelDp(91);
                // Split/fullscreen action sits at the far right edge so it never separates the
                // "主屏 DPI 同步" label from its switch.
                // Maximising a pane is handled by the divider menu, so the panel only offers the
                // one action that the menu cannot: entering split mode.
                if (!splitActive && currentVirtualPackage != null && !captureBlockedDisplay) {
                    panelPaint.setColor(0x99607888);
                    canvas.drawRoundRect(width - panelDp(68), compatY - panelDp(17),
                            width - panelDp(10), compatY + panelDp(17), panelDp(6), panelDp(6), panelPaint);
                    panelTextPaint.setColor(0xe6ffffff);
                    panelTextPaint.setTextSize(panelDp(13));
                    panelTextPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("分屏", width - panelDp(39), compatY + panelDp(5), panelTextPaint);
                }
                boolean compat = currentVirtualPackage != null && densityCompatPackages.contains(currentVirtualPackage);
                panelTextPaint.setTextSize(panelDp(13));
                panelTextPaint.setTextAlign(Paint.Align.LEFT);
                panelTextPaint.setColor(0xe6ffffff);
                String compatLabel = "主屏 DPI 同步 · 当前应用";
                canvas.drawText(compatLabel, panelDp(16), compatY - panelDp(2), panelTextPaint);
                panelTextPaint.setTextSize(panelDp(10));
                panelTextPaint.setColor(0xaaffffff);
                canvas.drawText("手机主屏显示大小会变化", panelDp(16), compatY + panelDp(12), panelTextPaint);
                // Switch hugs the label instead of floating at the panel edge.
                panelTextPaint.setTextSize(panelDp(13));
                float compatLabelRight = panelDp(16) + panelTextPaint.measureText(compatLabel);
                float compatSwitchLeft = compatLabelRight + panelDp(12);
                float compatSwitchRight = compatSwitchLeft + panelDp(40);
                panelPaint.setColor(compat ? 0xcc80cbc4 : 0x9977808d);
                canvas.drawRoundRect(compatSwitchLeft, compatY - panelDp(10),
                        compatSwitchRight, compatY + panelDp(10), panelDp(10), panelDp(10), panelPaint);
                panelPaint.setColor(0xffeeeeee);
                canvas.drawCircle(compat ? compatSwitchRight - panelDp(10)
                        : compatSwitchLeft + panelDp(10), compatY, panelDp(7), panelPaint);

                float tileTop = quickPanelAppsTop();
                float tileHeight = panelDp(58);
                float gap = panelDp(8);
                float horizontalPadding = panelDp(16);
                float tileGap = panelDp(8);
                int columns = quickPanelColumnCount(apps.size());
                float tileWidth = (width - horizontalPadding * 2f - tileGap * (columns - 1)) / columns;
                panelPaint.setColor(0x9930343d);
                panelTextPaint.setTextSize(panelDp(14));
                panelTextPaint.setTextAlign(Paint.Align.CENTER);
                for (int i = 0; i < apps.size(); i++) {
                    int row = i / columns;
                    int column = i % columns;
                    float left = horizontalPadding + column * (tileWidth + tileGap);
                    float top = tileTop + row * (tileHeight + gap);
                    canvas.drawRect(left, top, left + tileWidth, top + tileHeight, panelPaint);
                    canvas.drawText(panelDisplayLabel(apps.get(i), tileWidth),
                            left + tileWidth / 2f, top + tileHeight / 2f + panelDp(5), panelTextPaint);
                }
            }

            if (statusBarVisible && !quickPanelVisible) {
                panelPaint.setColor(0xb8171a20);
                canvas.drawRect(0, 0, width, barHeight, panelPaint);
            }
            if (statusBarVisible) {
                StatusBarLayout layout = statusBarLayout();
                panelTextPaint.setTextAlign(Paint.Align.LEFT);
                panelTextPaint.setTextSize(panelDp(15));
                canvas.drawText(layout.label, panelDp(16), barHeight * 0.66f, panelTextPaint);
                AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
                if (audio != null && layout.sliderRight > layout.sliderLeft) {
                    float center = barHeight / 2f;
                    float ratio = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                            / (float) Math.max(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                    panelPaint.setColor(0xb877808d);
                    canvas.drawRoundRect(layout.sliderLeft, center - panelDp(3), layout.sliderRight,
                            center + panelDp(3), panelDp(4), panelDp(4), panelPaint);
                    panelPaint.setColor(0xdd80cbc4);
                    canvas.drawRoundRect(layout.sliderLeft, center - panelDp(3),
                            layout.sliderLeft + (layout.sliderRight - layout.sliderLeft) * ratio,
                            center + panelDp(3), panelDp(4), panelDp(4), panelPaint);
                    canvas.drawCircle(layout.sliderLeft + (layout.sliderRight - layout.sliderLeft) * ratio,
                            center, panelDp(8), panelPaint);
                }
                for (int i = 0; i < layout.apps.size(); i++) {
                    float left = layout.appsLeft + i * panelDp(42);
                    float top = (barHeight - panelDp(32)) / 2f;
                    panelPaint.setColor(0x665f6875);
                    canvas.drawRoundRect(left + panelDp(5), top, left + panelDp(37), top + panelDp(32),
                            panelDp(7), panelDp(7), panelPaint);
                    try {
                        android.graphics.drawable.Drawable icon = launcherIcon(layout.apps.get(i));
                        canvas.save();
                        canvas.clipRect(left + panelDp(7), top + panelDp(2), left + panelDp(35), top + panelDp(30));
                        icon.setBounds((int) (left + panelDp(7)), (int) (top + panelDp(2)),
                                (int) (left + panelDp(35)), (int) (top + panelDp(30)));
                        icon.draw(canvas);
                        canvas.restore();
                    } catch (RuntimeException ignored) { }
                }
                float closeCenterX = layout.closeLeft + panelDp(22);
                float closeCenterY = barHeight / 2f;
                panelPaint.setColor(0x885f6875);
                canvas.drawCircle(closeCenterX, closeCenterY, panelDp(15), panelPaint);
                panelTextPaint.setTextAlign(Paint.Align.CENTER);
                panelTextPaint.setTextSize(panelDp(22));
                canvas.drawText("×", closeCenterX, closeCenterY + panelDp(8), panelTextPaint);
            }
        }

        private void drawInputModeIcon(Canvas canvas, float cx, float cy, boolean touchMode) {
            if (touchMode) {
                drawPanelVectorIcon(canvas, R.drawable.ic_touch_app, cx, cy, panelDp(27));
            } else {
                panelPaint.setColor(0xe6ffffff);
                panelPaint.setStyle(Paint.Style.STROKE);
                panelPaint.setStrokeWidth(panelDp(2));
                panelPaint.setStrokeCap(Paint.Cap.ROUND);
                panelPaint.setStrokeJoin(Paint.Join.ROUND);
                canvas.drawRoundRect(cx - panelDp(8), cy - panelDp(12), cx + panelDp(8), cy + panelDp(12),
                        panelDp(7), panelDp(7), panelPaint);
                canvas.drawLine(cx, cy - panelDp(11), cx, cy - panelDp(4), panelPaint);
                canvas.drawLine(cx, cy - panelDp(9), cx, cy - panelDp(6), panelPaint);
            }
            panelPaint.setStyle(Paint.Style.FILL);
        }

        private void drawOrientationIcon(Canvas canvas, float cx, float cy) {
            drawPanelVectorIcon(canvas, R.drawable.ic_screen_orientation, cx, cy, panelDp(28));
        }

        private void drawPanelVectorIcon(Canvas canvas, int resourceId, float cx, float cy, float size) {
            android.graphics.drawable.Drawable icon = getContext().getDrawable(resourceId);
            if (icon == null) return;
            int save = canvas.save();
            int left = Math.round(cx - size / 2f);
            int top = Math.round(cy - size / 2f);
            icon.setBounds(left, top, Math.round(cx + size / 2f), Math.round(cy + size / 2f));
            icon.setTint(0xe6ffffff);
            icon.draw(canvas);
            canvas.restoreToCount(save);
        }

        private void showPointer() {
            boolean wasVisible = pointerVisible;
            pointerX = virtualPointerX;
            pointerY = virtualPointerY;
            pointerVisible = true;
            long now = android.os.SystemClock.uptimeMillis();
            if (!wasVisible || now - lastHideRescheduleAt >= 250) {
                handler.removeCallbacks(hidePointer);
                handler.postDelayed(hidePointer, 1800);
                lastHideRescheduleAt = now;
            }
            invalidate();
        }

        void hideQuickPanel() {
            quickPanelVisible = false;
            statusBarVisible = false;
            invalidate();
        }

        private void forwardMouseEvent(MotionEvent source) {
            if (displayService == null || virtualDisplayId < 0 || externalDisplay == null) return;
            if (transitionFramesRemaining > 0) return;
            if (baseWidth <= 0 || baseHeight <= 0) return;
            final int virtualWidth = portraitMode ? baseHeight : baseWidth;
            final int virtualHeight = portraitMode ? baseWidth : baseHeight;
            final MotionEvent mapped = MotionEvent.obtain(source);
            final IDisplayService service = displayService;
            final int displayId = virtualDisplayId;
            virtualInputExecutor.execute(() -> {
                try {
                    if (portraitMode) {
                        float scaleX = virtualWidth / (float) baseWidth;
                        float scaleY = virtualHeight / (float) baseHeight;
                        Matrix transform = new Matrix();
                        transform.setValues(new float[]{
                                0f, -scaleX, baseWidth * scaleX,
                                scaleY, 0f, 0f,
                                0f, 0f, 1f
                        });
                        mapped.transform(transform);
                    }
                    mapped.setLocation(virtualPointerX, virtualPointerY);
                    if (!service.injectInputEvent(mapped, displayId)) {
                        Log.w(TAG, "InputManager rejected mouse event display=" + displayId
                                + " action=" + mapped.getActionMasked() + " x=" + mapped.getX()
                                + " y=" + mapped.getY());
                    }
                } catch (RemoteException | RuntimeException e) {
                    Log.e(TAG, "Unable to forward mouse event to virtual display", e);
                } finally {
                    mapped.recycle();
                }
            });
        }

        private RectF screenshotThumbnailBounds() {
            float width = panelDp(300);
            float height = panelDp(176);
            return new RectF(panelDp(18), getHeight() - height - panelDp(18),
                    panelDp(18) + width, getHeight() - panelDp(18));
        }

        private RectF screenshotExpandedBounds() {
            float margin = panelDp(24);
            return new RectF(margin, margin, getWidth() - margin, getHeight() - margin);
        }

        private RectF fitBitmap(RectF bounds, Bitmap bitmap) {
            float scale = Math.min(bounds.width() / bitmap.getWidth(),
                    bounds.height() / bitmap.getHeight());
            float width = bitmap.getWidth() * scale;
            float height = bitmap.getHeight() * scale;
            float left = bounds.centerX() - width / 2f;
            float top = bounds.centerY() - height / 2f;
            return new RectF(left, top, left + width, top + height);
        }

        private boolean handleScreenshotPreviewTouch(MotionEvent event) {
            if (screenshotPreviewBitmap == null) return false;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_BUTTON_PRESS) {
                if (event.getActionButton() != MotionEvent.BUTTON_PRIMARY) return true;
                action = MotionEvent.ACTION_DOWN;
            } else if (action == MotionEvent.ACTION_BUTTON_RELEASE) {
                if (event.getActionButton() != MotionEvent.BUTTON_PRIMARY) return true;
                action = MotionEvent.ACTION_UP;
            }
            float x = virtualPointerX;
            float y = virtualPointerY;
            if (action == MotionEvent.ACTION_DOWN) {
                RectF bounds = screenshotPreviewExpanded
                        ? screenshotExpandedBounds() : screenshotThumbnailBounds();
                screenshotPreviewPointerDown = bounds.contains(x, y);
                Log.d(TAG, "preview pointer down x=" + x + " y=" + y + " hit=" + screenshotPreviewPointerDown);
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                screenshotPreviewPointerDown = false;
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                boolean wasPressed = screenshotPreviewPointerDown;
                screenshotPreviewPointerDown = false;
                if (wasPressed && !screenshotPreviewExpanded) {
                    screenshotPreviewExpanded = true;
                    screenshotPreviewHandler.removeCallbacks(hideScreenshotPreview);
                    Log.i(TAG, "screenshot preview expanded");
                } else if (wasPressed) {
                    float buttonY = getHeight() - panelDp(70);
                    if (y >= buttonY && x < getWidth() * 0.36f) {
                        Uri uri = screenshotPreviewUri;
                        if (uri != null) getContentResolver().delete(uri, null, null);
                        dismissScreenshotPreview();
                    } else if (y >= buttonY && x > getWidth() * 0.64f) {
                        openScreenshotInGallery();
                    } else {
                        dismissScreenshotPreview();
                    }
                }
                invalidate();
                return true;
            }
            return true;
        }

        private void drawScreenshotPreview(Canvas canvas) {
            Bitmap bitmap = screenshotPreviewBitmap;
            if (bitmap == null || bitmap.isRecycled()) return;
            panelPaint.setColor(0xe6202329);
            panelTextPaint.setColor(0xffffffff);
            panelTextPaint.setTextAlign(Paint.Align.CENTER);
            if (screenshotPreviewExpanded) {
                RectF bounds = screenshotExpandedBounds();
                canvas.drawRect(0, 0, getWidth(), getHeight(), panelPaint);
                RectF imageBounds = fitBitmap(new RectF(bounds.left + panelDp(12), bounds.top + panelDp(12),
                        bounds.right - panelDp(12), bounds.bottom - panelDp(84)), bitmap);
                canvas.drawBitmap(bitmap, null, imageBounds, screenshotImagePaint);
                float buttonY = getHeight() - panelDp(70);
                panelPaint.setColor(0xff39434b);
                canvas.drawRoundRect(panelDp(24), buttonY, getWidth() * 0.36f,
                        buttonY + panelDp(48), panelDp(8), panelDp(8), panelPaint);
                canvas.drawRoundRect(getWidth() * 0.64f, buttonY, getWidth() - panelDp(24),
                        buttonY + panelDp(48), panelDp(8), panelDp(8), panelPaint);
                panelTextPaint.setTextSize(panelDp(16));
                canvas.drawText("删除", (panelDp(24) + getWidth() * 0.36f) / 2f,
                        buttonY + panelDp(31), panelTextPaint);
                canvas.drawText("图库打开", (getWidth() * 0.64f + getWidth() - panelDp(24)) / 2f,
                        buttonY + panelDp(31), panelTextPaint);
                float closeX = getWidth() / 2f;
                float closeY = buttonY + panelDp(24);
                panelPaint.setColor(0xff39434b);
                canvas.drawCircle(closeX, closeY, panelDp(20), panelPaint);
                panelPaint.setColor(0xffffffff);
                panelPaint.setStyle(Paint.Style.STROKE);
                panelPaint.setStrokeWidth(panelDp(2));
                canvas.drawLine(closeX - panelDp(6), closeY - panelDp(6),
                        closeX + panelDp(6), closeY + panelDp(6), panelPaint);
                canvas.drawLine(closeX + panelDp(6), closeY - panelDp(6),
                        closeX - panelDp(6), closeY + panelDp(6), panelPaint);
                panelPaint.setStyle(Paint.Style.FILL);
            } else {
                RectF card = screenshotThumbnailBounds();
                canvas.drawRoundRect(card, panelDp(10), panelDp(10), panelPaint);
                RectF imageBounds = fitBitmap(new RectF(card.left + panelDp(8), card.top + panelDp(8),
                        card.right - panelDp(8), card.bottom - panelDp(36)), bitmap);
                canvas.drawBitmap(bitmap, null, imageBounds, screenshotImagePaint);
                panelTextPaint.setTextSize(panelDp(12));
                canvas.drawText("截图已保存 · 点击预览", card.centerX(),
                        card.bottom - panelDp(12), panelTextPaint);
            }
            if (screenshotPreviewMessage != null) {
                panelPaint.setColor(0xee20252b);
                RectF message = new RectF(panelDp(24), getHeight() - panelDp(116),
                        getWidth() - panelDp(24), getHeight() - panelDp(24));
                canvas.drawRoundRect(message, panelDp(12), panelDp(12), panelPaint);
                panelTextPaint.setTextSize(panelDp(14));
                panelTextPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(screenshotPreviewMessage, message.centerX(), message.centerY(), panelTextPaint);
            }
        }

        private RectF splitPickerBounds() {
            if (portraitMode) {
                float divider = getHeight() * splitRatio;
                return splitPickerFirst ? new RectF(0, 0, getWidth(), divider)
                        : new RectF(0, divider, getWidth(), getHeight());
            }
            float divider = getWidth() * splitRatio;
            return splitPickerFirst ? new RectF(0, 0, divider, getHeight())
                    : new RectF(divider, 0, getWidth(), getHeight());
        }

        private boolean overSplitPicker() {
            return splitPickerBounds().contains(virtualPointerX, virtualPointerY);
        }

        private ArrayList<ResolveInfo> splitPickerApps() {
            if (splitPickerCache != null) return splitPickerCache;
            java.util.LinkedHashMap<String, ResolveInfo> ordered = new java.util.LinkedHashMap<>();
            // Apps the user opened on BoxScreen belong at the top of this picker.
            for (String pkg : recentAppPackages) {
                ResolveInfo app = launcherByPackage.get(pkg);
                if (app != null) ordered.put(pkg, app);
            }
            for (ResolveInfo app : quickPanelApps()) ordered.putIfAbsent(app.activityInfo.packageName, app);
            ArrayList<ResolveInfo> others = new ArrayList<>(launcherByPackage.values());
            others.sort((a, b) -> launcherLabel(a).compareToIgnoreCase(launcherLabel(b)));
            for (ResolveInfo app : others) {
                int flags = app.activityInfo.applicationInfo.flags;
                if (!showSystemApps && (flags & (android.content.pm.ApplicationInfo.FLAG_SYSTEM
                        | android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) continue;
                ordered.putIfAbsent(app.activityInfo.packageName, app);
            }
            ordered.remove(getPackageName());
            ordered.remove(currentVirtualPackage);
            ordered.remove(splitFirstPackage);
            ordered.remove(splitSecondPackage);
            ordered.remove("com.netease.uuremote");
            splitPickerCache = new ArrayList<>(ordered.values());
            return splitPickerCache;
        }

        private int splitPickerColumns() {
            return Math.max(1, (int) (splitPickerBounds().width() / panelDp(88)));
        }

        private boolean handleSplitTouch(MotionEvent event) {
            if (!splitActive) return false;
            int action = event.getActionMasked();
            float axis = portraitMode ? virtualPointerY : virtualPointerX;
            float extent = portraitMode ? getHeight() : getWidth();
            if (splitChoosing) {
                if (action == MotionEvent.ACTION_DOWN) splitPickerPressed = overSplitPicker();
                if (!splitPickerPressed) return false;
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    splitPickerPressed = false;
                    if (action == MotionEvent.ACTION_UP && overSplitPicker()) {
                        RectF area = splitPickerBounds();
                        int columns = splitPickerColumns();
                        int col = (int) ((virtualPointerX - area.left) / (area.width() / columns));
                        int row = (int) Math.floor((virtualPointerY - area.top - panelDp(52)) / panelDp(74));
                        ArrayList<ResolveInfo> apps = splitPickerApps();
                        int index = splitPickerOffset + row * columns + col;
                        if (row >= 0 && col >= 0 && col < columns && index < apps.size()) updateSplit(apps.get(index));
                    }
                }
                return true;
            }
            // Only the handle near the middle of the divider can be grabbed. Presses elsewhere
            // on the line are still swallowed (so they never turn into a long-press inside the
            // app underneath) but do nothing.
            float along = portraitMode ? virtualPointerX : virtualPointerY;
            boolean nearLine = Math.abs(axis - splitRatio * extent) <= panelDp(20);
            if (action == MotionEvent.ACTION_DOWN) {
                // Only the press decides whether this gesture is a handle drag. Once the drag
                // is running the pointer is deliberately far from the old divider, so the
                // remaining MOVE/UP events must not be re-tested against it.
                // The visual handle is 12x64px; the grab area has to be at least that big,
                // with margin, or aiming at its end misses the gesture entirely.
                boolean nearHandle = Math.abs(axis - splitRatio * extent) <= panelDp(16)
                        && Math.abs(along - splitHandleAlong()) <= panelDp(56);
                splitDrag = nearHandle;
                splitDownAxis = axis;
                splitKillLongPressTriggered = false;
                if (nearHandle && !splitKillMenuVisible) {
                    handler.postDelayed(splitKillLongPress, 500);
                }
            }
            if (!splitDrag) {
                if (!nearLine) return false;
                // Swallow the stray press on the divider line, but keep the handle put.
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (Math.abs(axis - splitDownAxis) > panelDp(6)) {
                    handler.removeCallbacks(splitKillLongPress);
                }
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                splitDrag = false;
                handler.removeCallbacks(splitKillLongPress);
                if (splitKillLongPressTriggered) {
                    splitKillLongPressTriggered = false;
                    invalidate();
                    return true;
                }
                if (action == MotionEvent.ACTION_UP) {
                    boolean swap = Math.abs(axis - splitDownAxis) < panelDp(4);
                    float ratio = swap ? splitRatio : SplitGeometry.clamp(axis / extent, getWidth(), getHeight());
                    if (swap || Math.abs(ratio - splitRatio) * extent >= 1f) {
                        changeSplitLayout(ratio, swap);
                    }
                }
            }
            invalidate();
            return true;
        }

        private void changeSplitLayout(float ratio, boolean swap) {
            if (splitLayoutChanging || displayService == null) {
                return;
            }
            splitLayoutChanging = true;
            handler.removeCallbacks(splitLayoutWatchdog);
            handler.postDelayed(splitLayoutWatchdog, 1500);
            IDisplayService service = displayService;
            int targetDisplay = virtualDisplayId;
            Runnable apply = () -> applySplitLayout(service, targetDisplay, ratio, swap);
            TextureView output = outputSurface;
            if (!swap || output == null || !output.isAvailable() || getWidth() < 1 || getHeight() < 1) {
                apply.run();
                return;
            }
            Bitmap previous = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Surface source = new Surface(output.getSurfaceTexture());
            try {
                PixelCopy.request(source, previous, result -> {
                    source.release();
                    if (result == PixelCopy.SUCCESS && splitActive && mousePointerView == this
                            && outputSurface == output && virtualDisplayId == targetDisplay) {
                        splitPreviousFrame = previous;
                        invalidate();
                        postOnAnimation(apply);
                    } else {
                        previous.recycle();
                        apply.run();
                    }
                }, handler);
            } catch (RuntimeException e) {
                source.release();
                previous.recycle();
                apply.run();
            }
        }

        private void applySplitLayout(IDisplayService service, int targetDisplay, float ratio, boolean swap) {
            if (!splitActive || mousePointerView != this || virtualDisplayId != targetDisplay
                    || displayService != service) {
                finishSplitLayoutChange();
                return;
            }
            new Thread(() -> {
                Exception failure = null;
                try {
                    service.adjustSplit(ratio, swap);
                } catch (Exception e) {
                    failure = e;
                }
                Exception result = failure;
                if (result != null) Log.w(TAG, "adjustSplit failed", result);
                handler.post(() -> {
                    if (mousePointerView != this || virtualDisplayId != targetDisplay
                            || displayService != service || !splitActive || result != null) {
                        if (result != null) status("分屏调整失败：" + result.getMessage());
                        finishSplitLayoutChange();
                        return;
                    }
                    splitRatio = ratio;
                    if (swap) splitLastFirst = !splitLastFirst;
                    refreshSplitState();
                    refreshVirtualDisplayLayout(swap ? "swap split panes" : "resize split panes", () -> {
                        if (splitPreviousFrame == null) {
                            finishSplitLayoutChange();
                        } else {
                            splitFreshFrames = 2;
                            handler.postDelayed(splitFrameTimeout, 220);
                        }
                    });
                    invalidate();
                });
            }, "boxscreen-split-layout").start();
        }

        private void onSplitOutputFrame() {
            if (splitFreshFrames > 0 && --splitFreshFrames == 0) finishSplitLayoutChange();
        }

        private void finishSplitLayoutChange() {
            handler.removeCallbacks(splitFrameTimeout);
            splitFreshFrames = 0;
            splitLayoutChanging = false;
            // Drop the reference; the renderer may still hold the bitmap from the last frame.
            splitPreviousFrame = null;
            invalidate();
        }

        @Override
        protected void onDetachedFromWindow() {
            finishSplitLayoutChange();
            super.onDetachedFromWindow();
        }

        /**
         * One cluster of three actions per pane, placed on that pane's own side of the divider:
         * left/right of the vertical divider in landscape, above/below the horizontal divider in
         * portrait. Keeping each cluster clear of the divider stops the buttons from sitting on
         * the black split line.
         */
        private RectF[] splitKillClusters() {
            float gap = panelDp(18);
            float height = panelDp(46);
            float maxWidth = panelDp(204);
            float extent = portraitMode ? getHeight() : getWidth();
            float divider = Math.round(splitRatio * extent);
            RectF first;
            RectF second;
            if (portraitMode) {
                float width = Math.min(maxWidth, getWidth() - panelDp(16));
                float cx = splitHandleAlong();
                float topY = Math.max(panelDp(8), divider - gap - height);
                float bottomY = Math.min(getHeight() - height - panelDp(8), divider + gap);
                first = new RectF(cx - width / 2f, topY, cx + width / 2f, topY + height);
                second = new RectF(cx - width / 2f, bottomY, cx + width / 2f, bottomY + height);
            } else {
                float cy = splitHandleAlong();
                float available = Math.min(divider - gap - panelDp(8),
                        getWidth() - (divider + gap) - panelDp(8));
                float width = Math.min(maxWidth, Math.max(panelDp(120), available));
                float leftStart = Math.max(panelDp(8), divider - gap - width);
                float rightStart = divider + gap;
                first = new RectF(leftStart, cy - height / 2f, leftStart + width, cy + height / 2f);
                second = new RectF(rightStart, cy - height / 2f, rightStart + width, cy + height / 2f);
            }
            return new RectF[]{first, second};
        }

        private static final String[] SPLIT_KILL_ACTIONS = {"最小化", "最大化", "关闭"};

        private void drawSplitKillMenu(Canvas canvas) {
            RectF[] clusters = splitKillClusters();
            panelTextPaint.setTextAlign(Paint.Align.CENTER);
            for (int side = 0; side < 2; side++) {
                RectF cluster = clusters[side];
                String packageName = side == 0 ? splitFirstPackage : splitSecondPackage;
                panelPaint.setColor(0xf01b2028);
                canvas.drawRoundRect(cluster, panelDp(10), panelDp(10), panelPaint);
                float cellWidth = cluster.width() / 3f;
                for (int col = 0; col < 3; col++) {
                    int index = side * 3 + col;
                    boolean enabled = packageName != null;
                    float cellLeft = cluster.left + col * cellWidth;
                    if (splitKillMenuPressed == index && enabled) {
                        panelPaint.setColor(0x55304a56);
                        canvas.drawRoundRect(cellLeft + panelDp(3), cluster.top + panelDp(4),
                                cellLeft + cellWidth - panelDp(3), cluster.bottom - panelDp(4),
                                panelDp(8), panelDp(8), panelPaint);
                    }
                    // Symbols instead of words: a bar (minimise), a square (maximise) and a cross
                    // (close), matching the familiar window-control trio.
                    float cx = cellLeft + cellWidth / 2f;
                    float cy = cluster.centerY();
                    float half = panelDp(8);
                    panelPaint.setStyle(Paint.Style.STROKE);
                    panelPaint.setStrokeWidth(panelDp(2));
                    panelPaint.setColor(enabled ? 0xe6ffffff : 0x55ffffff);
                    if (col == 0) {
                        canvas.drawLine(cx - half, cy + panelDp(2), cx + half, cy + panelDp(2), panelPaint);
                    } else if (col == 1) {
                        canvas.drawRect(cx - half, cy - half + panelDp(2), cx + half,
                                cy + half + panelDp(2), panelPaint);
                    } else {
                        float d = half * 0.85f;
                        canvas.drawLine(cx - d, cy + panelDp(2) - d, cx + d, cy + panelDp(2) + d, panelPaint);
                        canvas.drawLine(cx + d, cy + panelDp(2) - d, cx - d, cy + panelDp(2) + d, panelPaint);
                    }
                    panelPaint.setStyle(Paint.Style.FILL);
                }
            }
        }

        private int splitKillMenuItemAt(float x, float y) {
            RectF[] clusters = splitKillClusters();
            for (int side = 0; side < 2; side++) {
                RectF cluster = clusters[side];
                if (x < cluster.left || x > cluster.right
                        || y < cluster.top || y > cluster.bottom) continue;
                int col = (int) ((x - cluster.left) / (cluster.width() / 3f));
                if (col < 0) col = 0;
                if (col > 2) col = 2;
                return side * 3 + col;
            }
            return -1;
        }

        /** Returns true when the touch belongs to the "close pane app" menu. */
        private boolean handleSplitKillMenuTouch(MotionEvent event) {
            if (!splitKillMenuVisible) return false;
            int action = event.getActionMasked();
            if (splitKillAwaitRelease) {
                // The press that opened the menu must not also pick an item: wait for the
                // button to be released, then the next click chooses.
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    splitKillAwaitRelease = false;
                }
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN) {
                splitKillMenuPressed = splitKillMenuItemAt(virtualPointerX, virtualPointerY);
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                int item = splitKillMenuItemAt(virtualPointerX, virtualPointerY);
                if (item != splitKillMenuPressed) {
                    splitKillMenuPressed = item;
                    invalidate();
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                int item = splitKillMenuItemAt(virtualPointerX, virtualPointerY);
                boolean confirmed = item >= 0 && item == splitKillMenuPressed;
                splitKillMenuVisible = false;
                splitKillMenuPressed = -1;
                invalidate();
                if (confirmed) runSplitKillAction(item / 3 == 0, item % 3);
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                splitKillMenuVisible = false;
                splitKillMenuPressed = -1;
                invalidate();
                return true;
            }
            return true;
        }

        private void runSplitKillAction(boolean firstSide, int action) {
            final IDisplayService service = displayService;
            final int targetDisplay = virtualDisplayId;
            if (service == null || targetDisplay < 0) return;
            new Thread(() -> {
                String name = null;
                String failure = null;
                try {
                    if (action == 0) {
                        name = service.hideSplitSide(firstSide) ? "" : null;
                    } else if (action == 1) {
                        name = service.expandSplit(firstSide);
                    } else {
                        name = service.killSplitSide(firstSide);
                    }
                } catch (RemoteException | RuntimeException e) {
                    Log.e(TAG, "split pane action failed", e);
                    failure = e.getMessage();
                }
                final String resultName = name;
                final String resultFailure = failure;
                runOnUiThread(() -> {
                    if (displayService != service) return;
                    refreshSplitState();
                    applyDensitySync();
                    refreshVirtualDisplayLayout("split pane action");
                    if (resultFailure != null) {
                        status("操作失败：" + resultFailure);
                    } else if (resultName == null) {
                        status(action == 0 ? "该栏没有运行中的应用" : "该栏没有运行中的应用");
                    } else if (action == 1) {
                        splitActive = splitChoosing = false;
                        currentVirtualPackage = resultName;
                        status("已放大 " + resultName);
                    } else if (action == 0) {
                        status("已最小化，可选择其他应用");
                    } else {
                        status("已关闭 " + resultName);
                    }
                    invalidate();
                });
            }, "boxscreen-split-action").start();
        }

        private void drawSplitUi(Canvas canvas) {
            float divider = Math.round(splitRatio * (portraitMode ? getHeight() : getWidth()));
            // The panes are already separated by a system gap, so drawing our own full-height
            // black band here only covered system windows (share sheet, floating IME) that span
            // the split. Keep the gap as-is and draw just the handle below.
            if (splitChoosing) {
                RectF area = splitPickerBounds();
                panelPaint.setColor(0xff1b2028);
                canvas.drawRect(area, panelPaint);
                panelTextPaint.setColor(0xe6ffffff);
                panelTextPaint.setTextAlign(Paint.Align.CENTER);
                panelTextPaint.setTextSize(panelDp(15));
                String side = portraitMode ? (splitPickerFirst ? "上方" : "下方")
                        : (splitPickerFirst ? "左侧" : "右侧");
                canvas.drawText("选择" + side + "应用", area.centerX(),
                        area.top + panelDp(32), panelTextPaint);
                ArrayList<ResolveInfo> apps = splitPickerApps();
                int columns = splitPickerColumns();
                float cellWidth = area.width() / columns;
                float rowHeight = panelDp(74);
                float top = area.top + panelDp(52);
                canvas.save();
                canvas.clipRect(area);
                for (int i = splitPickerOffset; i < apps.size(); i++) {
                    int slot = i - splitPickerOffset;
                    float y = top + slot / columns * rowHeight;
                    if (y >= area.bottom) break;
                    float x = area.left + (slot % columns + 0.5f) * cellWidth;
                    ResolveInfo app = apps.get(i);
                    android.graphics.drawable.Drawable icon = launcherIcon(app);
                    int half = Math.round(panelDp(16));
                    icon.setBounds((int) x - half, (int) y, (int) x + half, (int) y + 2 * half);
                    icon.draw(canvas);
                    panelTextPaint.setTextSize(panelDp(12));
                    canvas.drawText(panelDisplayLabel(app, cellWidth), x, y + panelDp(52), panelTextPaint);
                }
                canvas.restore();
                return;
            }
            float extent = portraitMode ? getHeight() : getWidth();
            float axis = splitRatio * extent;
            if (splitDrag) {
                axis = extent * SplitGeometry.clamp(
                        (portraitMode ? virtualPointerY : virtualPointerX) / extent,
                        getWidth(), getHeight());
                // Landing line: a 3px white core with a 1px dark edge on each side, so it
                // stays readable over both bright and dark content.
                if (portraitMode) {
                    panelPaint.setColor(0xcc000000);
                    canvas.drawRect(0, axis - 2.5f, getWidth(), axis + 2.5f, panelPaint);
                    panelPaint.setColor(0xf2ffffff);
                    canvas.drawRect(0, axis - 1.5f, getWidth(), axis + 1.5f, panelPaint);
                } else {
                    panelPaint.setColor(0xcc000000);
                    canvas.drawRect(axis - 2.5f, 0, axis + 2.5f, getHeight(), panelPaint);
                    panelPaint.setColor(0xf2ffffff);
                    canvas.drawRect(axis - 1.5f, 0, axis + 1.5f, getHeight(), panelPaint);
                }
            }
            // Sausage handle: 12px across, 64px along, black rim with a white core, placed
            // high on the divider so floating IMEs and centred dialogs stay clear of it.
            // Only drawn while the pointer is near it (or while dragging): the rest of the
            // time the virtual display has no overlay at all.
            float along = splitHandleAlong();
            boolean handleHovered = forwarding && pointerVisible
                    && Math.abs(axis - splitRatio * extent) <= panelDp(16)
                    && Math.abs(along - splitHandleAlong()) <= panelDp(56);
            if (!splitDrag && !handleHovered) return;
            float cx = portraitMode ? along : axis;
            float cy = portraitMode ? axis : along;
            float hw = portraitMode ? 32 : 6;
            float hh = portraitMode ? 6 : 32;
            float radius = Math.min(hw, hh);
            panelPaint.setColor(0xff000000);
            panelPaint.setAlpha(Math.round(255 * SPLIT_HANDLE_RIM_OPACITY));
            canvas.drawRoundRect(cx - hw, cy - hh, cx + hw, cy + hh, radius, radius, panelPaint);
            panelPaint.setColor(0xffffffff);
            panelPaint.setAlpha(Math.round(255 * SPLIT_HANDLE_CORE_OPACITY));
            canvas.drawRoundRect(cx - hw + 2, cy - hh + 2, cx + hw - 2, cy + hh - 2,
                    radius - 1, radius - 1, panelPaint);
            panelPaint.setAlpha(255);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (splitActive && splitPreviousFrame != null) {
                canvas.drawBitmap(splitPreviousFrame, null, new RectF(0, 0, getWidth(), getHeight()),
                        screenshotImagePaint);
            }
            if (splitActive) drawSplitUi(canvas);
            if (splitKillMenuVisible) drawSplitKillMenu(canvas);
            if (statusBarVisible || quickPanelVisible) drawQuickUi(canvas);
            drawScreenshotPreview(canvas);
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
            lastHideRescheduleAt = 0;
            invalidate();
        }

        void resetPointerPosition() {
            hidePointer();
        }
    }

    private void releaseVirtualDisplay() {
        splitActive = splitChoosing = false;
        if (displayService != null && virtualDisplayId >= 0) {
            try { displayService.releaseDisplay(); } catch (RemoteException ignored) { }
        }
        virtualDisplayId = -1;
    }

    private void addLaunchableApps() {
        refreshLaunchableApps();
        appGrid = new GridLayout(this);
        appGrid.setColumnCount(1);
        appGrid.setMinimumHeight(dp(1));
        appGrid.setUseDefaultMargins(false);
        appGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        content.addView(appGrid, fullWidth());
        appGrid.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            scheduleAppGridUpdate(right - left);
        });
        // An empty GridLayout can be measured to zero height and may not receive a
        // useful size-change callback on every Android build. Populate it after its
        // first layout as well, once the available width is known.
        appGrid.post(() -> updateAppGridColumns(appGrid.getWidth()));
        appGrid.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                int width = appGrid.getWidth();
                if (width <= 0) return;
                appGrid.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                scheduleAppGridUpdate(width);
            }
        });
    }

    private void refreshLaunchableApps() {
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        appEntries = getPackageManager().queryIntentActivities(launcher, 0);
        launcherByPackage.clear();
        launcherLabelCache.clear();
        launcherIconCache.clear();
        panelDisplayLabelCache.clear();
        quickPanelAppCache = null;
        for (ResolveInfo app : appEntries) {
            if (app.activityInfo != null) {
                launcherByPackage.putIfAbsent(app.activityInfo.packageName, app);
            }
        }
        if (appGrid != null) rebuildAppsGrid();
        if (mousePointerView != null) mousePointerView.invalidate();
    }

    private String getLauncherLabel(ResolveInfo app) {
        String packageName = app.activityInfo.packageName;
        String label = launcherLabelCache.get(packageName);
        if (label == null) {
            label = String.valueOf(app.loadLabel(getPackageManager()));
            launcherLabelCache.put(packageName, label);
        }
        return label;
    }

    private android.graphics.drawable.Drawable getLauncherIcon(ResolveInfo app) {
        String packageName = app.activityInfo.packageName;
        android.graphics.drawable.Drawable icon = launcherIconCache.get(packageName);
        if (icon == null) {
            icon = app.loadIcon(getPackageManager());
            launcherIconCache.put(packageName, icon);
        }
        return icon;
    }

    private void scheduleAppGridUpdate(int widthPx) {
        if (appGrid == null || widthPx <= 0) return;
        // Do not add or remove GridLayout children from a layout callback. Defer
        // population until that traversal finishes so the very first frame draws it.
        appGrid.postOnAnimation(() -> updateAppGridColumns(appGrid.getWidth()));
    }

    private void updateAppGridColumns(int widthPx) {
        if (appGrid == null || widthPx <= 0) {
            scheduleAppGridRetry();
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        int availableDp = Math.max(1, Math.round(widthPx / density));
        int columns = Math.max(2, Math.min(8, availableDp / 80));
        boolean densityChanged = density != appGridDensity;
        boolean columnsChanged = columns != appColumns || appGrid.getColumnCount() != columns;
        if (columnsChanged || densityChanged) {
            // GridLayout validates the current children against the new column count.
            // During rotation, old children may still occupy columns beyond the new count.
            // A DPI sync also changes how every tile measures, so the children are rebuilt.
            appGrid.removeAllViews();
            appColumns = columns;
            appGridDensity = density;
            appGrid.setColumnCount(columns);
        }
        if (columnsChanged || densityChanged || appGrid.getChildCount() == 0) {
            rebuildAppsGrid();
            Log.i(TAG, "app grid populated width=" + widthPx + " columns=" + columns
                    + " entries=" + appEntries.size() + " children=" + appGrid.getChildCount());
        }
    }

    private void rebuildAppsGrid() {
        if (appGrid == null) return;
        if (appGrid.getWidth() <= 0 || appColumns <= 0) {
            // Between layouts (rotation, density change): keep whatever is on screen and
            // retry once the grid has a real size. Never clear children we cannot replace.
            scheduleAppGridRetry();
            return;
        }
        appGrid.removeAllViews();
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
        int cellWidth = appGrid.getWidth() > 0 && appColumns > 0
                ? appGrid.getWidth() / appColumns : 0;
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
            if (cellWidth > 0) cell.width = cellWidth;
            cell.height = -2;
            cell.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            appGrid.addView(tile, cell);
            tile.setOnClickListener(v -> launchPackage(packageName, app.activityInfo.name));
            favorite.setOnCheckedChangeListener((button, checked) -> {
                if (checked) favoritePackages.add(packageName); else favoritePackages.remove(packageName);
                getPreferences(MODE_PRIVATE).edit().putStringSet("favorite_apps", new HashSet<>(favoritePackages)).apply();
                quickPanelAppCache = null;
                if (mousePointerView != null) mousePointerView.invalidate();
            });
        }
    }

    private void updateSplit(ResolveInfo second) {
        if (splitOpening || virtualDisplayId < 0 || displayService == null) return;
        if (second == null && (currentVirtualPackage == null || captureBlockedDisplay || splitActive)) {
            status("请先打开支持分屏的应用");
            return;
        }
        IDisplayService service = displayService;
        int targetDisplay = virtualDisplayId;
        String firstPackage = currentVirtualPackage;
        boolean targetFirst = splitPickerFirst;
        splitOpening = true;
        new Thread(() -> {
            Exception failure = null;
            try {
                if (second == null) service.beginSplit(firstPackage);
                else service.fillSplit(second.activityInfo.packageName, second.activityInfo.name, targetFirst);
            } catch (Exception e) {
                failure = e;
            }
            Exception result = failure;
            runOnUiThread(() -> {
                splitOpening = false;
                if (virtualDisplayId != targetDisplay || displayService != service) return;
                if (result != null) {
                    Log.e(TAG, "split update failed", result);
                    status("分屏操作失败：" + result.getMessage());
                    return;
                }
                splitActive = true;
                splitChoosing = second == null;
                if (splitChoosing) {
                    splitRatio = 0.5f;
                    splitLastFirst = true;
                    splitPickerFirst = false;
                    splitFirstPackage = firstPackage;
                    splitSecondPackage = null;
                } else {
                    currentVirtualPackage = null;
                    splitLastFirst = targetFirst;
                    refreshVirtualDisplayLayout("fill split pane");
                }
                refreshSplitState();
                applyDensitySync();
                scheduleSplitCheck();
            });
        }, "boxscreen-split").start();
    }

    private final Handler splitCheckHandler = new Handler(android.os.Looper.getMainLooper());
    private final Runnable splitCheck = this::scheduleSplitCheck;

    private void scheduleSplitCheck() {
        splitCheckHandler.removeCallbacks(splitCheck);
        if (!splitActive || virtualDisplayId < 0 || displayService == null) return;
        if (!splitOpening) refreshSplitState();
        if (splitActive) splitCheckHandler.postDelayed(splitCheck, 850);
    }

    /**
     * The split itself lives in the shell service, so the pane state outlives this activity
     * (activity restart, new build install, low-memory kill). Adopt whatever the service
     * reports instead of trusting the in-memory flag only.
     */
    private void syncSplitState() {
        if (displayService == null || virtualDisplayId < 0) return;
        try {
            String[] pair = displayService.getSplitPackages();
            if (pair == null || pair.length != 2) {
                if (splitActive) {
                    splitActive = splitChoosing = false;
                    if (mousePointerView != null) mousePointerView.invalidate();
                }
                return;
            }
            boolean changed = !java.util.Objects.equals(splitFirstPackage, pair[0])
                    || !java.util.Objects.equals(splitSecondPackage, pair[1]);
            splitFirstPackage = pair[0];
            splitSecondPackage = pair[1];
            splitActive = true;
            splitChoosing = pair[0] == null || pair[1] == null;
            splitPickerFirst = pair[0] == null;
            if (changed && mousePointerView != null) {
                mousePointerView.splitPickerOffset = 0;
                mousePointerView.splitPickerCache = null;
            }
            if (mousePointerView != null) mousePointerView.invalidate();
            scheduleSplitCheck();
        } catch (RemoteException | RuntimeException e) {
            Log.w(TAG, "could not sync split state", e);
        }
    }

    private void refreshSplitState() {
        if (!splitActive || displayService == null) return;
        try {
            String[] pair = displayService.getSplitPackages();
            if (pair == null || pair.length != 2) {
                splitActive = splitChoosing = false;
                if (mousePointerView != null) mousePointerView.invalidate();
                return;
            }
            boolean changed = !java.util.Objects.equals(splitFirstPackage, pair[0])
                    || !java.util.Objects.equals(splitSecondPackage, pair[1]);
            splitFirstPackage = pair[0];
            splitSecondPackage = pair[1];
            splitChoosing = pair[0] == null || pair[1] == null;
            splitPickerFirst = pair[0] == null;
            if (changed && mousePointerView != null) {
                mousePointerView.splitPickerOffset = 0;
                mousePointerView.splitPickerCache = null;
                mousePointerView.invalidate();
            }
            if (changed) applyDensitySync();
        } catch (RemoteException | RuntimeException e) {
            Log.w(TAG, "could not read split state", e);
        }
    }

    private void expandCurrentSplit() {
        if (!splitActive || splitOpening || displayService == null) return;
        try {
            currentVirtualPackage = displayService.expandSplit(splitLastFirst);
            splitActive = splitChoosing = false;
            splitCheckHandler.removeCallbacks(splitCheck);
            applyDensitySync();
            refreshVirtualDisplayLayout("expand split");
            if (mousePointerView != null) mousePointerView.invalidate();
            status("已退出分屏，正在刷新全屏画面");
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "could not expand split", e);
            status("切换全屏失败：" + e.getMessage());
        }
    }

    private void launchPackage(String packageName, String activityName) {
        if (splitOpening) return;
        autoMouseSuppressed = false;
        if (displayService == null || virtualDisplayId < 0) {
            pendingLaunchPackage = packageName;
            pendingLaunchActivity = activityName;
            requestShizukuAndStart();
            return;
        }
        boolean needsCaptureBlocked = "com.netease.uuremote".equals(packageName);
        if (captureBlockedDisplay != needsCaptureBlocked) {
            pendingLaunchPackage = packageName;
            pendingLaunchActivity = activityName;
            if (outputSurface == null || !outputSurface.isAvailable()) {
                status("外接屏画面尚未准备好，无法切换虚拟屏");
                return;
            }
            try {
                if (mousePointerView != null) mousePointerView.cancelTouchGesture();
                displayService.releaseDisplay();
                virtualDisplayId = -1;
                createVirtualDisplay(outputSurface.getSurfaceTexture());
            } catch (RemoteException | RuntimeException e) {
                Log.e(TAG, "switch virtual display failed", e);
                status("切换虚拟屏失败：" + e.getMessage());
            }
            return;
        }
        try {
            if (mousePointerView != null) mousePointerView.cancelTouchGesture();
            if (!syncPrimaryDensityForPackage(packageName)) return;
            autoEnableMouseIfAvailable();
            Log.i(TAG, "launch=" + packageName + "/" + activityName + " display=" + virtualDisplayId);
            displayService.launchActivity(packageName, activityName);
            splitActive = splitChoosing = false;
            currentVirtualPackage = packageName;
            recentAppPackages.remove(packageName);
            recentAppPackages.add(0, packageName);
            while (recentAppPackages.size() > 8) recentAppPackages.remove(recentAppPackages.size() - 1);
            quickPanelAppCache = null;
            getPreferences(MODE_PRIVATE).edit()
                    .putString("recent_apps", android.text.TextUtils.join("\n", recentAppPackages))
                    .apply();
            if (mousePointerView != null) mousePointerView.invalidate();
            if (mouseBound) dimAndBlackoutPhoneScreen();
            status("已请求在虚拟屏中打开 " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "launch failed=" + packageName + ": " + e.getMessage(), e);
            status("启动应用失败：" + e.getMessage());
        }
    }

    private String inputSettingsPackage() {
        if (!splitActive) return currentVirtualPackage;
        return splitLastFirst ? splitFirstPackage : splitSecondPackage;
    }

    private boolean isTouchMode(String packageName) {
        return packageName != null && !mouseModePackages.contains(packageName);
    }

    private boolean restorePrimaryDensity() {
        android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        if (!prefs.getBoolean("primary_density_restore_pending", false)) return true;
        if (displayService == null) return false;
        try {
            displayService.setPrimaryDensity(prefs.getInt("primary_density_original_override", 0));
            prefs.edit().remove("primary_density_restore_pending")
                    .remove("primary_density_original_override").remove("primary_density_applied").commit();
            Log.i(TAG, "restored primary display density");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "failed to restore primary display density", e);
            status("恢复主屏 DPI 失败，请确认 Shizuku 已连接");
            return false;
        }
    }

    /**
     * Package whose main-screen DPI sync should currently be in effect. Inside a split the
     * synced app can be either pane, and it must stay synced when the other pane's app does
     * not need it - otherwise a split that adds the sensitive app last renders incorrectly.
     */
    private String densitySyncTarget() {
        if (!splitActive) return currentVirtualPackage;
        if (splitFirstPackage != null && densityCompatPackages.contains(splitFirstPackage)) {
            return splitFirstPackage;
        }
        if (splitSecondPackage != null && densityCompatPackages.contains(splitSecondPackage)) {
            return splitSecondPackage;
        }
        return null;
    }

    private void applyDensitySync() {
        syncPrimaryDensityForPackage(densitySyncTarget());
    }

    private boolean syncPrimaryDensityForPackage(String packageName) {
        if (packageName == null || !densityCompatPackages.contains(packageName)) return restorePrimaryDensity();
        if (displayService == null) return false;
        android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        try {
            if (!prefs.getBoolean("primary_density_restore_pending", false)) {
                String output = displayService.getPrimaryDensity();
                if (!output.contains("Physical density:")) {
                    throw new IllegalStateException("无法读取主屏 DPI");
                }
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("Override density:\\s*(\\d+)").matcher(output);
                int originalOverride = matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
                if (!prefs.edit().putInt("primary_density_original_override", originalOverride)
                        .putBoolean("primary_density_restore_pending", true).commit()) {
                    throw new IllegalStateException("无法保存主屏 DPI 恢复信息");
                }
            }
            if (prefs.getInt("primary_density_applied", -1) != densityDpi) {
                displayService.setPrimaryDensity(densityDpi);
                prefs.edit().putInt("primary_density_applied", densityDpi).commit();
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "failed to synchronize primary display density", e);
            status("同步主屏 DPI 失败：" + e.getMessage());
            return false;
        }
    }

    private void toggleDensityCompat() {
        String target = inputSettingsPackage();
        if (target == null) {
            status("先打开应用，再设置主屏 DPI 同步");
            return;
        }
        boolean enabled = !densityCompatPackages.contains(target);
        if (enabled) densityCompatPackages.add(target);
        else densityCompatPackages.remove(target);
        getPreferences(MODE_PRIVATE).edit().putStringSet("density_compat_apps",
                new HashSet<>(densityCompatPackages)).apply();
        applyDensitySync();
        if (densityCompatPackages.contains(target) == enabled) {
            status(enabled ? "已为当前应用同步主屏 DPI，手机显示大小会变化" : "已关闭当前应用的主屏 DPI 同步");
        }
    }

    private void toggleCurrentInputMode() {
        String packageName = inputSettingsPackage();
        if (packageName == null) {
            status("先在虚拟屏启动应用，再切换输入模式");
            return;
        }
        if (mousePointerView != null) mousePointerView.cancelTouchGesture();
        boolean touchMode;
        if (isTouchMode(packageName)) {
            mouseModePackages.add(packageName);
            touchMode = false;
        } else {
            mouseModePackages.remove(packageName);
            touchMode = true;
        }
        getPreferences(MODE_PRIVATE).edit()
                .putStringSet("mouse_mode_apps", new HashSet<>(mouseModePackages))
                .apply();
        status((touchMode ? "触摸模式" : "鼠标模式") + "已为当前应用单独保存");
        if (mousePointerView != null) mousePointerView.invalidate();
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
                    || display.getDisplayId() == virtualDisplayId
                    || (display.getFlags() & Display.FLAG_PRESENTATION) == 0) continue;
            if (best == null || display.getMode().getPhysicalWidth() * display.getMode().getPhysicalHeight()
                    > best.getMode().getPhysicalWidth() * best.getMode().getPhysicalHeight()) best = display;
        }
        return best;
    }

    private boolean refreshExternalDisplayGeometry() {
        if (externalDisplay == null) return false;
        Display.Mode mode = externalDisplay.getMode();
        int width = mode.getPhysicalWidth();
        int height = mode.getPhysicalHeight();
        if (width < height) {
            int swap = width;
            width = height;
            height = swap;
        }
        if (width == baseWidth && height == baseHeight) return false;
        Log.i(TAG, "external display geometry changed " + baseWidth + "x" + baseHeight
                + " -> " + width + "x" + height);
        baseWidth = width;
        baseHeight = height;
        return true;
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
