package com.armyja.boxscreen;

import android.app.Activity;
import android.app.Presentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.pm.ActivityInfo;
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
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.Surface;
import android.view.SurfaceControl;
import android.graphics.SurfaceTexture;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.InputDevice;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.PixelCopy;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.net.Uri;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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
    private static final String GUIDE_ASSET_NAME = "operation_guide_zh_cn.html";
    private static final String GUIDE_BASE_URL = "https://boxscreen.local/";
    private static final int PANEL_SCREENSHOT = 2;
    private static final int SHIZUKU_REQUEST = 410;
    private static final int MAX_DISPLAY_RECOVERY_ATTEMPTS = 12;
    private static final long DISPLAY_RECOVERY_DELAY_MS = 250;

    private static final class SmartisanPalette {
        final boolean isDark;
        final int pageBg;
        final int titleBarTop;
        final int titleBarBottom;
        final int titleBarBorder;
        final int titleBarShadowStart;
        final int cardBg;
        final int appsCardBg;
        final int cardBorder;
        final int cardShadow;
        final int divider;
        final int textPrimary;
        final int textSecondary;
        final int textMuted;
        final int textSection;
        final int textDisabled;
        final int chipBg;
        final int chipBorder;
        final int accentRedTop;
        final int accentRedBottom;
        final int accentRedBorder;
        final int accentRedPressedTop;
        final int accentRedPressedBottom;
        final int accentRedPressedBorder;
        final int softRedTop;
        final int softRedBottom;
        final int softRedBorder;
        final int softRedPressedTop;
        final int softRedPressedBottom;
        final int softRedText;
        final int accentBlue;
        final int accentBlueSoftBg;
        final int accentBlueSoftBorder;
        final int accentBlueText;
        final int amberSoftBg;
        final int amberSoftBorder;
        final int amberText;
        final int btnTop;
        final int btnBottom;
        final int btnBorder;
        final int btnPressedTop;
        final int btnPressedBottom;
        final int btnPressedBorder;
        final int btnShadow;
        final int btnDisabledBg;
        final int btnDisabledBorder;
        final int searchBgTop;
        final int searchBgBottom;
        final int searchBorder;
        final int tilePressedBg;
        final int tileFavEditBg;
        final int tileFavEditBorder;
        final int sliderTrackTop;
        final int sliderTrackBottom;
        final int sliderTrackBorder;
        final int sliderThumbTop;
        final int sliderThumbBottom;
        final int sliderThumbBorder;
        final int sliderThumbGrip;
        final int switchOffTrack;
        final int switchOffBorder;
        final int switchOnTrack;
        final int switchOnBorder;
        final int webBg;

        private SmartisanPalette(boolean dark) {
            this.isDark = dark;
            if (dark) {
                pageBg = 0xff25282d;
                titleBarTop = 0xff2d3036;
                titleBarBottom = 0xff292c31;
                titleBarBorder = 0xff1c1e22;
                titleBarShadowStart = 0x44000000;
                cardBg = 0xff34373c;
                appsCardBg = 0xff34373c;
                cardBorder = 0xff23262b;
                cardShadow = 0x40000000;
                divider = 0xff404348;
                textPrimary = 0xfff2f2f3;
                textSecondary = 0xffc8cacd;
                textMuted = 0xff85878a;
                textSection = 0xff9fa1a4;
                textDisabled = 0xff717377;
                chipBg = 0xff2a2d32;
                chipBorder = 0xff42464d;
                accentRedTop = 0xffe45c53;
                accentRedBottom = 0xffcb433b;
                accentRedBorder = 0xff962c26;
                accentRedPressedTop = 0xffb3362f;
                accentRedPressedBottom = 0xffc44038;
                accentRedPressedBorder = 0xff82241f;
                softRedTop = 0xff463235;
                softRedBottom = 0xff3d2b2e;
                softRedBorder = 0xff6b4247;
                softRedPressedTop = 0xff352427;
                softRedPressedBottom = 0xff3a282b;
                softRedText = 0xfff4908a;
                accentBlue = 0xff5c83dc;
                accentBlueSoftBg = 0xff2e3748;
                accentBlueSoftBorder = 0xff465875;
                accentBlueText = 0xff96b5f8;
                amberSoftBg = 0xff3f3526;
                amberSoftBorder = 0xff675334;
                amberText = 0xfff5bc63;
                btnTop = 0xff454a52;
                btnBottom = 0xff3b4048;
                btnBorder = 0xff25282e;
                btnPressedTop = 0xff31353c;
                btnPressedBottom = 0xff373c43;
                btnPressedBorder = 0xff1e2025;
                btnShadow = 0x42000000;
                btnDisabledBg = 0xff303338;
                btnDisabledBorder = 0xff292c31;
                searchBgTop = 0xff292c32;
                searchBgBottom = 0xff2f3339;
                searchBorder = 0xff1e2126;
                tilePressedBg = 0xff41464e;
                tileFavEditBg = 0xff453235;
                tileFavEditBorder = 0xff7e484d;
                sliderTrackTop = 0xff1e2126;
                sliderTrackBottom = 0xff282c32;
                sliderTrackBorder = 0xff15171b;
                sliderThumbTop = 0xff555b65;
                sliderThumbBottom = 0xff41464f;
                sliderThumbBorder = 0xff202328;
                sliderThumbGrip = 0xff7a818c;
                switchOffTrack = 0xff292c32;
                switchOffBorder = 0xff1c1e22;
                switchOnTrack = 0xff5378ce;
                switchOnBorder = 0xff3c5ca6;
                webBg = 0xff25282d;
            } else {
                pageBg = 0xffeff0f2;
                titleBarTop = 0xffffffff;
                titleBarBottom = 0xfff8f8f8;
                titleBarBorder = 0xffdcdcdc;
                titleBarShadowStart = 0x1c000000;
                cardBg = 0xffffffff;
                appsCardBg = 0xfff6f7f9;
                cardBorder = 0xffdcdcdc;
                cardShadow = 0x14000000;
                divider = 0xffebebeb;
                textPrimary = 0xff262626;
                textSecondary = 0xff6d7075;
                textMuted = 0xff909398;
                textSection = 0xff787b80;
                textDisabled = 0xffb3b5ba;
                chipBg = 0xfff5f6f7;
                chipBorder = 0xffdedfe2;
                accentRedTop = 0xffe85e55;
                accentRedBottom = 0xffd2463d;
                accentRedBorder = 0xffb43830;
                accentRedPressedTop = 0xffc33d34;
                accentRedPressedBottom = 0xffd1473e;
                accentRedPressedBorder = 0xff9e2e27;
                softRedTop = 0xfffef4f3;
                softRedBottom = 0xfffbe9e7;
                softRedBorder = 0xffecc5c2;
                softRedPressedTop = 0xfff2d7d4;
                softRedPressedBottom = 0xfff7e2e0;
                softRedText = 0xffce453d;
                accentBlue = 0xff4f73c7;
                accentBlueSoftBg = 0xffedf3fc;
                accentBlueSoftBorder = 0xffc7d7f4;
                accentBlueText = 0xff3e60b0;
                amberSoftBg = 0xfffef6e8;
                amberSoftBorder = 0xffedd4a7;
                amberText = 0xffb46b14;
                btnTop = 0xffffffff;
                btnBottom = 0xfff3f4f6;
                btnBorder = 0xffd2d4d8;
                btnPressedTop = 0xffe4e6ea;
                btnPressedBottom = 0xffededf0;
                btnPressedBorder = 0xffc2c5ca;
                btnShadow = 0x16000000;
                btnDisabledBg = 0xfff4f4f5;
                btnDisabledBorder = 0xffe3e4e6;
                searchBgTop = 0xffe8eaed;
                searchBgBottom = 0xfff2f3f5;
                searchBorder = 0xffd4d6dc;
                tilePressedBg = 0xffeaecef;
                tileFavEditBg = 0xfffdf2f1;
                tileFavEditBorder = 0xffe6a9a4;
                sliderTrackTop = 0xffd2d5dc;
                sliderTrackBottom = 0xffe7e9ed;
                sliderTrackBorder = 0xffbec2ca;
                sliderThumbTop = 0xffffffff;
                sliderThumbBottom = 0xffeaedf0;
                sliderThumbBorder = 0xffbcbfc6;
                sliderThumbGrip = 0xffcfd2d8;
                switchOffTrack = 0xffd7dade;
                switchOffBorder = 0xffc1c4ca;
                switchOnTrack = 0xff5378ce;
                switchOnBorder = 0xff3e61b5;
                webBg = 0xfff2f5f4;
            }
        }

        static SmartisanPalette of(boolean dark) {
            return new SmartisanPalette(dark);
        }
    }

    private static final int THEME_MODE_SYSTEM = 0;
    private static final int THEME_MODE_LIGHT = 1;
    private static final int THEME_MODE_DARK = 2;
    private static final String PREF_THEME_MODE = "theme_mode";
    private static final int LAYOUT_CONFIG_MASK = ActivityInfo.CONFIG_ORIENTATION
            | ActivityInfo.CONFIG_SCREEN_SIZE
            | ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE
            | ActivityInfo.CONFIG_SCREEN_LAYOUT
            | ActivityInfo.CONFIG_DENSITY
            | ActivityInfo.CONFIG_FONT_SCALE
            | ActivityInfo.CONFIG_LAYOUT_DIRECTION
            | ActivityInfo.CONFIG_UI_MODE;
    private Configuration lastConfiguration;
    private int themeMode = THEME_MODE_SYSTEM;
    private boolean systemNightMode;
    private boolean darkMode;
    private SmartisanPalette theme = SmartisanPalette.of(false);
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
    private volatile int virtualDisplayId = -1;
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
    private LinearLayout rootShell;
    private LinearLayout topBarContainer;
    private TextView topBarTitle;
    private TextView versionChipView;
    private View topBarDivider;
    private View topBarShadow;
    private ScrollView mainScroll;
    private Button guideButton;
    private View blackScreenOverlay;
    private TextView statusView;
    private TextView sectionControlTitle;
    private LinearLayout controlCard;
    private View cardDivider1;
    private View cardDivider2;
    private View monitorStatusDot;
    private TextView monitorView;
    private TextView monitorMetaView;
    private TextView shizukuBadge;
    private Button blackoutButton;
    private Button stopButton;
    private TextView densityTitleView;
    private TextView densityRangeView;
    private TextView densityView;
    private SmartisanSliderView densitySlider;
    private View cardDivider3;
    private TextView themeTitleView;
    private LinearLayout themeSegmentTray;
    private TextView themeModeSystemBtn;
    private TextView themeModeLightBtn;
    private TextView themeModeDarkBtn;
    private TextView appsSectionTitle;
    private TextView systemAppsLabel;
    private LinearLayout searchContainer;
    private ImageView searchIconView;
    private LinearLayout appsCard;
    private GridLayout appGrid;
    private EditText appSearch;
    private TextView searchClearButton;
    private TextView appsCountView;
    private TextView appsEmptyView;
    private Switch systemAppsSwitch;
    private android.app.Dialog guideDialog;
    private WebView guideWebView;
    private LinearLayout guideContainer;
    private LinearLayout guideTopBar;
    private TextView guideTitleView;
    private Button guideCloseButton;
    private View guideTopDivider;
    private View guideTopShadow;
    /**
     * Large outputs (above roughly 1200p) hold each frame for one vsync before presenting:
     * at those sizes the direct path loses ~1 frame in 8. Smaller outputs compose fast
     * enough to stay direct, which keeps latency and CPU at their minimum.
     */
    private static final long BUFFERED_OUTPUT_MIN_PIXELS = 2_500_000L;
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
    private final java.util.HashMap<String, android.graphics.drawable.Drawable> panelLauncherIconCache = new java.util.HashMap<>();
    private final java.util.HashMap<String, android.graphics.drawable.Drawable> recentIconCache = new java.util.HashMap<>();
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
    private View outputSurface;
    private View previousOutputSurface;
    private MousePointerView mousePointerView;
    private Presentation presentation;
    // The instance holding the physical presentation owns the running virtual screen.
    private static MainActivity outputOwner;
    private static final Set<MainActivity> controlActivities = new HashSet<>();
    private boolean returningControls;
    private boolean blackoutWhenOnPrimary;
    private boolean screenshotInProgress;
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
    private volatile IDisplayService displayService;
    private Shizuku.UserServiceArgs shizukuServiceArgs;
    private final ServiceConnection shizukuServiceConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            displayService = IDisplayService.Stub.asInterface(binder);
            if (virtualDisplayId < 0) restorePrimaryDensity();
            updateShizukuBadge();
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
            updateShizukuBadge();
            status("Shizuku 服务已断开");
        }
    };

    private final Shizuku.OnRequestPermissionResultListener permissionListener = (requestCode, grantResult) -> {
        updateShizukuBadge();
        if (requestCode == SHIZUKU_REQUEST && grantResult == PackageManager.PERMISSION_GRANTED) {
            bindShizukuService();
        } else if (requestCode == SHIZUKU_REQUEST) {
            status("未授予 Shizuku 权限");
        }
    };

    private final Shizuku.OnBinderReceivedListener binderReceivedListener =
            () -> runOnUiThread(this::updateShizukuBadge);

    private final Shizuku.OnBinderDeadListener binderDeadListener =
            () -> runOnUiThread(this::updateShizukuBadge);

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
        controlActivities.add(this);
        android.content.SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        themeMode = preferences.getInt(PREF_THEME_MODE, THEME_MODE_SYSTEM);
        if (themeMode < THEME_MODE_SYSTEM || themeMode > THEME_MODE_DARK) {
            themeMode = THEME_MODE_SYSTEM;
        }
        systemNightMode = isNightConfig(currentSystemConfiguration());
        lastConfiguration = new Configuration(currentSystemConfiguration());
        darkMode = resolveDarkMode();
        theme = SmartisanPalette.of(darkMode);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        applySystemBarTheme(getWindow(), theme);
        displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        displayManager.registerDisplayListener(displayListener, new Handler());
        inputManager = (InputManager) getSystemService(INPUT_SERVICE);
        if (inputManager != null) {
            inputManager.registerInputDeviceListener(inputDeviceListener, new Handler());
        }
        Shizuku.addRequestPermissionResultListener(permissionListener);
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
        externalDisplay = findExternalDisplay();
        showSystemApps = preferences.getBoolean("show_system_apps", false);
        favoritePackages = new HashSet<>(preferences.getStringSet("favorite_apps", Collections.emptySet()));
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
        if (outputOwner == null && preferences.getBoolean("primary_density_restore_pending", false)
                && Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            bindShizukuService();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        systemNightMode = isNightConfig(currentSystemConfiguration());
        int savedMode = getPreferences(MODE_PRIVATE).getInt(PREF_THEME_MODE, THEME_MODE_SYSTEM);
        if (savedMode < THEME_MODE_SYSTEM || savedMode > THEME_MODE_DARK) savedMode = THEME_MODE_SYSTEM;
        int prevMode = themeMode;
        themeMode = savedMode;
        boolean savedDark = resolveDarkMode();
        if (savedMode != prevMode || savedDark != darkMode) {
            applyThemeModeState(savedMode);
        }
        applyPendingPrimaryBlackout();
        externalDisplay = findExternalDisplay();
        if (monitorView != null) updateMonitorLabel();
        updateShizukuBadge();
        if (appGrid != null) refreshLaunchableApps();
        syncSplitState();
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int diff = lastConfiguration == null ? ~0 : lastConfiguration.diff(newConfig);
        lastConfiguration = new Configuration(newConfig);
        syncSystemNightAcrossInstances(isNightConfig(newConfig));
        if ((diff & LAYOUT_CONFIG_MASK) != 0) {
            applyPendingPrimaryBlackout();
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
        }
        Log.i(TAG, "phone configuration changed diff=0x" + Integer.toHexString(diff)
                + " orientation=" + newConfig.orientation
                + " dark=" + darkMode + " virtualDisplay=" + virtualDisplayId);
    }

    @Override protected void onDestroy() {
        controlActivities.remove(this);
        if (outputOwner == this) outputOwner = null;
        dismissScreenshotPreview();
        if (outputOwner == null) restorePrimaryDensity();
        densityResizeHandler.removeCallbacks(applyPendingDensity);
        displayRecoveryHandler.removeCallbacks(recoverExternalDisplay);
        splitCheckHandler.removeCallbacks(splitCheck);
        splitCheckExecutor.shutdown();
        if (displayService != null && virtualDisplayId >= 0) {
            try { displayService.releaseDisplay(); }
            catch (RemoteException | RuntimeException e) {
                Log.w(TAG, "failed to release display on activity destroy", e);
            }
            virtualDisplayId = -1;
        }
        dismissGuideDialog();
        restorePhoneScreen();
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        Shizuku.removeBinderReceivedListener(binderReceivedListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);
        if (presentation != null) presentation.dismiss();
        if (displayManager != null) displayManager.unregisterDisplayListener(displayListener);
        if (inputManager != null) inputManager.unregisterInputDeviceListener(inputDeviceListener);
        if (shizukuServiceArgs != null) {
            try { Shizuku.unbindUserService(shizukuServiceArgs, shizukuServiceConnection, outputOwner == null); }
            catch (RuntimeException ignored) { }
        }
        super.onDestroy();
    }

    private void showRoleScreen() {
        rootLayout = new FrameLayout(this);
        rootShell = new LinearLayout(this);
        rootShell.setOrientation(LinearLayout.VERTICAL);

        topBarContainer = new LinearLayout(this);
        topBarContainer.setOrientation(LinearLayout.VERTICAL);
        topBarContainer.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset;
            if (Build.VERSION.SDK_INT >= 30) {
                topInset = insets.getInsets(WindowInsets.Type.statusBars()).top;
            } else {
                topInset = insets.getSystemWindowInsetTop();
            }
            view.setPadding(0, topInset, 0, 0);
            return insets;
        });

        FrameLayout barInner = new FrameLayout(this);
        barInner.setPadding(dp(14), 0, dp(14), 0);
        topBarContainer.addView(barInner, new LinearLayout.LayoutParams(-1, dp(52)));

        LinearLayout leftBrand = new LinearLayout(this);
        leftBrand.setOrientation(LinearLayout.HORIZONTAL);
        leftBrand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView brandIcon = new ImageView(this);
        brandIcon.setImageResource(R.drawable.ic_launcher);
        LinearLayout.LayoutParams brandIconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        brandIconParams.setMargins(0, 0, dp(6), 0);
        leftBrand.addView(brandIcon, brandIconParams);

        String verName = "1.0";
        try {
            verName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) { }
        versionChipView = text("v" + verName, 10, true, theme.textSecondary);
        versionChipView.setPadding(dp(6), dp(2), dp(6), dp(2));
        leftBrand.addView(versionChipView, new LinearLayout.LayoutParams(-2, -2));
        barInner.addView(leftBrand, new FrameLayout.LayoutParams(-2, -2, Gravity.START | Gravity.CENTER_VERTICAL));

        topBarTitle = text("BoxScreen", 18, true, theme.textPrimary);
        topBarTitle.setLetterSpacing(0.02f);
        barInner.addView(topBarTitle, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));

        guideButton = new Button(this);
        guideButton.setText("操作图解");
        guideButton.setAllCaps(false);
        guideButton.setTextSize(11.5f);
        guideButton.setTypeface(null, android.graphics.Typeface.BOLD);
        guideButton.setStateListAnimator(null);
        guideButton.setElevation(0f);
        guideButton.setMinHeight(dp(32));
        guideButton.setMinimumHeight(dp(32));
        guideButton.setMinWidth(0);
        guideButton.setMinimumWidth(0);
        guideButton.setPadding(dp(10), 0, dp(11), 0);
        barInner.addView(guideButton, new FrameLayout.LayoutParams(-2, dp(32), Gravity.END | Gravity.CENTER_VERTICAL));
        guideButton.setOnClickListener(v -> showOperationGuideDialog());

        topBarDivider = new View(this);
        topBarContainer.addView(topBarDivider, new LinearLayout.LayoutParams(-1, Math.max(1, dp(1))));
        rootShell.addView(topBarContainer, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout bodyFrame = new FrameLayout(this);
        mainScroll = new ScrollView(this);
        mainScroll.setClipToPadding(false);
        mainScroll.setVerticalScrollBarEnabled(false);
        mainScroll.setHorizontalScrollBarEnabled(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(12), dp(14), dp(18));
        mainScroll.addView(content, new FrameLayout.LayoutParams(-1, -2));
        mainScroll.setOnApplyWindowInsetsListener((view, insets) -> {
            int bottomInset;
            if (Build.VERSION.SDK_INT >= 30) {
                bottomInset = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
            } else {
                bottomInset = insets.getSystemWindowInsetBottom();
            }
            content.setPadding(dp(14), dp(12), dp(14), dp(20) + bottomInset);
            return insets;
        });
        bodyFrame.addView(mainScroll, new FrameLayout.LayoutParams(-1, -1));

        topBarShadow = new View(this);
        bodyFrame.addView(topBarShadow, new FrameLayout.LayoutParams(-1, dp(4), Gravity.TOP));

        rootShell.addView(bodyFrame, new LinearLayout.LayoutParams(-1, 0, 1f));
        rootLayout.addView(rootShell, new FrameLayout.LayoutParams(-1, -1));
        setContentView(rootLayout);

        statusView = text("", 12, false, theme.accentBlueText);
        statusView.setPadding(dp(12), dp(8), dp(12), dp(8));
        statusView.setVisibility(View.GONE);
        content.addView(statusView, margins(0, 0, 0, 10));

        showHostControls();
        applySmartisanTheme();
    }

    private void showHostControls() {
        sectionControlTitle = text("外接显示器与控制", 12, true, theme.textSection);
        content.addView(sectionControlTitle, margins(6, 0, 6, 6));

        controlCard = new LinearLayout(this);
        controlCard.setOrientation(LinearLayout.VERTICAL);
        controlCard.setPadding(dp(14), dp(13), dp(14), dp(12));
        content.addView(controlCard, margins(0, 0, 0, 14));

        LinearLayout monitorHeader = new LinearLayout(this);
        monitorHeader.setOrientation(LinearLayout.HORIZONTAL);
        monitorHeader.setGravity(Gravity.CENTER_VERTICAL);
        controlCard.addView(monitorHeader, fullWidth());

        monitorStatusDot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(9), dp(9));
        dotParams.setMargins(0, 0, dp(10), 0);
        monitorHeader.addView(monitorStatusDot, dotParams);

        LinearLayout monitorCol = new LinearLayout(this);
        monitorCol.setOrientation(LinearLayout.VERTICAL);
        monitorHeader.addView(monitorCol, new LinearLayout.LayoutParams(0, -2, 1f));

        monitorView = text("", 14, true, theme.textPrimary);
        monitorCol.addView(monitorView, fullWidth());

        monitorMetaView = text("", 11.5f, false, theme.textSecondary);
        monitorCol.addView(monitorMetaView, margins(0, 2, 0, 0));

        shizukuBadge = text("", 11, true, theme.textSecondary);
        shizukuBadge.setPadding(dp(9), dp(4), dp(9), dp(4));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(-2, -2);
        badgeParams.setMargins(dp(8), 0, 0, 0);
        monitorHeader.addView(shizukuBadge, badgeParams);

        cardDivider1 = new View(this);
        LinearLayout.LayoutParams div1Params = new LinearLayout.LayoutParams(-1, Math.max(1, dp(1)));
        div1Params.setMargins(0, dp(11), 0, dp(11));
        controlCard.addView(cardDivider1, div1Params);

        LinearLayout primaryActions = new LinearLayout(this);
        primaryActions.setOrientation(LinearLayout.HORIZONTAL);
        controlCard.addView(primaryActions, margins(0, 0, 0, 8));
        mouseRouteButton = button("开启鼠标");
        LinearLayout.LayoutParams actionCell = weighted();
        actionCell.setMargins(0, 0, dp(4), 0);
        primaryActions.addView(mouseRouteButton, actionCell);
        mouseRouteButton.setOnClickListener(v -> toggleMouseRoute());

        modeButton = button(portraitMode ? "竖屏 · 切换横屏" : "横屏 · 切换竖屏");
        LinearLayout.LayoutParams modeCell = weighted();
        modeCell.setMargins(dp(4), 0, 0, 0);
        primaryActions.addView(modeButton, modeCell);
        modeButton.setOnClickListener(v -> setPortraitMode(!portraitMode));

        LinearLayout secondaryActions = new LinearLayout(this);
        secondaryActions.setOrientation(LinearLayout.HORIZONTAL);
        controlCard.addView(secondaryActions, margins(0, 0, 0, 0));

        blackoutButton = button("手机黑屏");
        LinearLayout.LayoutParams blackoutCell = weighted();
        blackoutCell.setMargins(0, 0, dp(4), 0);
        secondaryActions.addView(blackoutButton, blackoutCell);
        blackoutButton.setOnClickListener(v -> dimAndBlackoutPhoneScreen());

        stopButton = button("停止输出");
        LinearLayout.LayoutParams stopCell = weighted();
        stopCell.setMargins(dp(4), 0, 0, 0);
        secondaryActions.addView(stopButton, stopCell);
        stopButton.setOnClickListener(v -> stopOutput());

        cardDivider2 = new View(this);
        LinearLayout.LayoutParams div2Params = new LinearLayout.LayoutParams(-1, Math.max(1, dp(1)));
        div2Params.setMargins(0, dp(11), 0, dp(10));
        controlCard.addView(cardDivider2, div2Params);

        LinearLayout densityRow = new LinearLayout(this);
        densityRow.setOrientation(LinearLayout.HORIZONTAL);
        densityRow.setGravity(Gravity.CENTER_VERTICAL);
        controlCard.addView(densityRow, margins(0, 0, 0, 4));

        densityTitleView = text("虚拟屏 DPI", 13, true, theme.textPrimary);
        densityRow.addView(densityTitleView, new LinearLayout.LayoutParams(-2, -2));

        densityView = text(densityDpi + " DPI", 11, true, theme.accentBlueText);
        densityView.setPadding(dp(7), dp(2), dp(7), dp(2));
        LinearLayout.LayoutParams densityChipParams = new LinearLayout.LayoutParams(-2, -2);
        densityChipParams.setMargins(dp(8), 0, 0, 0);
        densityRow.addView(densityView, densityChipParams);

        View densitySpacer = new View(this);
        densityRow.addView(densitySpacer, new LinearLayout.LayoutParams(0, dp(1), 1f));

        densityRangeView = text("140 — 600", 11, false, theme.textMuted);
        densityRow.addView(densityRangeView, new LinearLayout.LayoutParams(-2, -2));

        densitySlider = new SmartisanSliderView(this);
        densitySlider.setMax(46);
        densitySlider.setProgress(Math.max(0, Math.min(46, (densityDpi - 140) / 10)));
        controlCard.addView(densitySlider, margins(0, 4, 0, 0));
        densitySlider.setListener(new SmartisanSliderView.OnSliderChangeListener() {
            @Override public void onProgressChanged(int progress, boolean fromUser) {
                updateDensitySetting(140 + progress * 10, fromUser);
            }
            @Override public void onStopTrackingTouch() {
                densityResizeHandler.removeCallbacks(applyPendingDensity);
                if (mousePointerView != null) mousePointerView.applyPanelDensity();
                resizeVirtualDisplay();
            }
        });

        cardDivider3 = new View(this);
        LinearLayout.LayoutParams div3Params = new LinearLayout.LayoutParams(-1, Math.max(1, dp(1)));
        div3Params.setMargins(0, dp(12), 0, dp(10));
        controlCard.addView(cardDivider3, div3Params);

        LinearLayout themeRow = new LinearLayout(this);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        themeRow.setGravity(Gravity.CENTER_VERTICAL);
        controlCard.addView(themeRow, fullWidth());

        themeTitleView = text("外观主题", 13, true, theme.textPrimary);
        themeRow.addView(themeTitleView, new LinearLayout.LayoutParams(-2, -2));

        View themeSpacer = new View(this);
        themeRow.addView(themeSpacer, new LinearLayout.LayoutParams(0, dp(1), 1f));

        themeSegmentTray = new LinearLayout(this);
        themeSegmentTray.setOrientation(LinearLayout.HORIZONTAL);
        themeSegmentTray.setGravity(Gravity.CENTER_VERTICAL);
        themeSegmentTray.setPadding(dp(2), dp(2), dp(2), dp(2));
        themeModeSystemBtn = buildThemeSegment("跟随系统", THEME_MODE_SYSTEM);
        themeModeLightBtn = buildThemeSegment("浅色", THEME_MODE_LIGHT);
        themeModeDarkBtn = buildThemeSegment("暗色", THEME_MODE_DARK);
        themeSegmentTray.addView(themeModeSystemBtn, new LinearLayout.LayoutParams(-2, -1));
        themeSegmentTray.addView(themeModeLightBtn, new LinearLayout.LayoutParams(-2, -1));
        themeSegmentTray.addView(themeModeDarkBtn, new LinearLayout.LayoutParams(-2, -1));
        themeRow.addView(themeSegmentTray, new LinearLayout.LayoutParams(-2, dp(32)));

        LinearLayout appsHeader = new LinearLayout(this);
        appsHeader.setOrientation(LinearLayout.HORIZONTAL);
        appsHeader.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(appsHeader, margins(6, 0, 2, 8));

        appsSectionTitle = text("可启动的应用", 12.5f, true, theme.textSection);
        appsHeader.addView(appsSectionTitle, new LinearLayout.LayoutParams(-2, -2));

        appsCountView = text("0", 10.5f, true, theme.textSecondary);
        appsCountView.setPadding(dp(7), dp(2), dp(7), dp(2));
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(-2, -2);
        countParams.setMargins(dp(7), 0, 0, 0);
        appsHeader.addView(appsCountView, countParams);

        View appsSpacer = new View(this);
        appsHeader.addView(appsSpacer, new LinearLayout.LayoutParams(0, dp(1), 1f));

        systemAppsLabel = text("系统应用", 12, false, theme.textSecondary);
        LinearLayout.LayoutParams sysLabelParams = new LinearLayout.LayoutParams(-2, -2);
        sysLabelParams.setMargins(0, 0, dp(6), 0);
        appsHeader.addView(systemAppsLabel, sysLabelParams);

        systemAppsSwitch = new Switch(this);
        systemAppsSwitch.setShowText(false);
        systemAppsSwitch.setChecked(showSystemApps);
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(-2, -2);
        switchParams.setMargins(0, 0, dp(10), 0);
        appsHeader.addView(systemAppsSwitch, switchParams);
        systemAppsLabel.setOnClickListener(v -> systemAppsSwitch.toggle());

        editFavoritesButton = button("编辑收藏");
        editFavoritesButton.setTextSize(11.5f);
        editFavoritesButton.setMinHeight(dp(30));
        editFavoritesButton.setMinimumHeight(dp(30));
        editFavoritesButton.setMinWidth(0);
        editFavoritesButton.setMinimumWidth(0);
        editFavoritesButton.setPadding(dp(10), 0, dp(10), 0);
        appsHeader.addView(editFavoritesButton, new LinearLayout.LayoutParams(-2, dp(30)));
        editFavoritesButton.setOnClickListener(v -> {
            editFavorites = !editFavorites;
            editFavoritesButton.setText(editFavorites ? "完成" : "编辑收藏");
            updateEditFavoritesButtonStyle();
            rebuildAppsGrid();
        });
        systemAppsSwitch.setOnCheckedChangeListener((button, checked) -> {
            showSystemApps = checked;
            getPreferences(MODE_PRIVATE).edit().putBoolean("show_system_apps", checked).apply();
            quickPanelAppCache = null;
            if (mousePointerView != null) mousePointerView.invalidate();
            rebuildAppsGrid();
        });

        searchContainer = new LinearLayout(this);
        searchContainer.setOrientation(LinearLayout.HORIZONTAL);
        searchContainer.setGravity(Gravity.CENTER_VERTICAL);
        searchContainer.setMinimumHeight(dp(40));
        searchContainer.setPadding(dp(11), 0, dp(4), 0);
        content.addView(searchContainer, margins(0, 0, 0, 10));

        searchIconView = new ImageView(this);
        LinearLayout.LayoutParams searchIconParams = new LinearLayout.LayoutParams(dp(15), dp(15));
        searchIconParams.setMargins(0, 0, dp(8), 0);
        searchContainer.addView(searchIconView, searchIconParams);

        appSearch = new EditText(this);
        appSearch.setSingleLine(true);
        appSearch.setBackground(null);
        appSearch.setTextSize(13.5f);
        appSearch.setHint("搜索应用名称或包名…");
        appSearch.setPadding(0, dp(8), 0, dp(8));
        appSearch.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        searchContainer.addView(appSearch, new LinearLayout.LayoutParams(0, -2, 1f));

        searchClearButton = text("×", 16, true, theme.textSecondary);
        searchClearButton.setGravity(Gravity.CENTER);
        searchClearButton.setVisibility(View.GONE);
        searchContainer.addView(searchClearButton, new LinearLayout.LayoutParams(dp(30), dp(30)));
        searchClearButton.setOnClickListener(v -> appSearch.setText(""));

        appSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchClearButton != null) {
                    searchClearButton.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                }
                rebuildAppsGrid();
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        appsCard = new LinearLayout(this);
        appsCard.setOrientation(LinearLayout.VERTICAL);
        appsCard.setPadding(dp(6), dp(8), dp(6), dp(10));
        content.addView(appsCard, margins(0, 0, 0, 4));

        appsEmptyView = text("没有找到匹配的应用，换个关键词试试", 13, false, theme.textMuted);
        appsEmptyView.setGravity(Gravity.CENTER);
        appsEmptyView.setPadding(0, dp(24), 0, dp(24));
        appsEmptyView.setVisibility(View.GONE);
        appsCard.addView(appsEmptyView, fullWidth());

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
            updateShizukuBadge();
            status("请先在本机启动 Shizuku");
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
                    .version(39);
            Shizuku.bindUserService(shizukuServiceArgs, shizukuServiceConnection);
            status("正在连接 Shizuku 主机服务…");
        } catch (RuntimeException e) {
            status("Shizuku 连接失败：" + e.getMessage());
        }
    }

    private void showExternalOutput() {
        if (outputOwner != null && outputOwner != this) return;
        if (presentation != null) {
            if (virtualDisplayId >= 0) {
                launchPendingApp();
                return;
            }
            if (outputSurface != null) {
                attachOutputSurface();
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
            updateDensityLabel();
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
            outputSurface = newOutputView();
            frame.addView(outputSurface);
            mousePointerView = new MousePointerView(this);
            frame.addView(mousePointerView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            presentation.setContentView(frame);
            updateOutputLayout();
            presentation.show();
            outputOwner = this;
            status("外接输出已启动：" + baseWidth + "×" + baseHeight + "，" + densityDpi + " dpi");
        } catch (RuntimeException e) {
            status("无法在外接显示器上显示：" + e.getMessage());
        }
    }

    private View newOutputView() {
        int bufferWidth = portraitMode ? baseHeight : baseWidth;
        int bufferHeight = portraitMode ? baseWidth : baseHeight;
        long pixels = (long) bufferWidth * bufferHeight;
        boolean useBuffered = pixels > BUFFERED_OUTPUT_MIN_PIXELS
                && (!portraitMode || Build.VERSION.SDK_INT >= 36);
        if (useBuffered) {
            BufferedOutputView buffered = new BufferedOutputView(this, portraitMode);
            if (portraitMode) watchPortraitSurfaceGeometry(buffered);
            Log.i(TAG, "output pipeline=buffered buffer=" + bufferWidth + "x" + bufferHeight
                    + " pixels=" + pixels);
            buffered.setListener(new BufferedOutputView.Listener() {
                @Override public void onOutputReady(BufferedOutputView view) {
                    if (outputSurface == view) attachOutputSurface();
                }

                @Override public void onOutputLost(BufferedOutputView view) {
                    if (outputSurface == view) detachOutputSurfaceForRecreation();
                }
            });
            return buffered;
        }
        if (!portraitMode || Build.VERSION.SDK_INT >= 36) {
            boolean rotate = portraitMode;
            SurfaceView view = new SurfaceView(this) {
                @Override public boolean gatherTransparentRegion(Region region) {
                    // The rotated mouse/panel View draws above the surface hole. Do not
                    // report that hole as wholly transparent for compositor occlusion.
                    return !rotate && super.gatherTransparentRegion(region);
                }
            };
            view.getHolder().setFixedSize(rotate ? baseHeight : baseWidth,
                    rotate ? baseWidth : baseHeight);
            if (rotate) watchPortraitSurfaceGeometry(view);
            view.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override public void surfaceCreated(SurfaceHolder holder) {
                    if (outputSurface == view) attachOutputSurface();
                }

                @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

                @Override public void surfaceDestroyed(SurfaceHolder holder) {
                    if (outputSurface == view) detachOutputSurfaceForRecreation();
                }
            });
            return view;
        }

        // Older Android versions keep the established View rotation path for portrait.
        TextureView view = new TextureView(this);
        view.setOpaque(true);
        view.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                if (outputSurface == view) attachOutputSurface();
            }

            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {}

            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                if (outputSurface == view) detachOutputSurfaceForRecreation();
                return true;
            }

            @Override public void onSurfaceTextureUpdated(SurfaceTexture texture) {
                if (outputSurface != view) return;
                if (mousePointerView != null) mousePointerView.onSplitOutputFrame();
                if (transitionFramesRemaining > 0 && --transitionFramesRemaining == 0
                        && mousePointerView != null) {
                    mousePointerView.invalidate();
                }
            }
        });
        return view;
    }

    private void watchPortraitSurfaceGeometry(SurfaceView view) {
        view.addOnLayoutChangeListener((v, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> {
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                schedulePortraitSurfaceRotation(view);
            }
        });
        view.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder holder) {}

            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                schedulePortraitSurfaceRotation(view);
            }

            @Override public void surfaceDestroyed(SurfaceHolder holder) {}
        });
    }

    private void schedulePortraitSurfaceRotation(SurfaceView view) {
        if (view != outputSurface || !view.getViewTreeObserver().isAlive()) return;
        // SurfaceView writes its default layer geometry during traversal. Rotate after
        // that frame is committed, with a delayed retry in case the first attempt lands
        // before the layer exists.
        view.getViewTreeObserver().registerFrameCommitCallback(
                () -> view.post(() -> rotatePortraitSurface(view)));
        view.invalidate();
        view.postDelayed(() -> rotatePortraitSurface(view), 400);
    }

    @SuppressWarnings("deprecation")
    private void rotatePortraitSurface(SurfaceView view) {
        if (view != outputSurface || !portraitMode) return;
        SurfaceControl layer = view.getSurfaceControl();
        if (layer == null || !layer.isValid()) return;
        Rect portrait = new Rect(0, 0, baseHeight, baseWidth);
        try (SurfaceControl.Transaction tx = new SurfaceControl.Transaction()) {
            // setGeometry transforms the container and its BLAST child. Its native
            // transform uses the inverse quarter-turn for the required View rotation.
            tx.setGeometry(layer, portrait, portrait, SurfaceControl.BUFFER_TRANSFORM_ROTATE_270)
                    .setPosition(layer, baseWidth, 0).apply();
        }
    }

    private void removePreviousOutput() {
        View previous = previousOutputSurface;
        previousOutputSurface = null;
        if (previous != null && previous.getParent() instanceof FrameLayout frame) {
            frame.removeView(previous);
        }
        releaseBufferedOutput(previous);
    }

    private boolean isOutputAvailable(View view) {
        if (view instanceof BufferedOutputView buffered) return buffered.isReady();
        if (view instanceof SurfaceView surfaceView) return surfaceView.getHolder().getSurface().isValid();
        return view instanceof TextureView textureView && textureView.isAvailable();
    }

    private void setOutputBufferSize(int width, int height) {
        if (outputSurface instanceof BufferedOutputView buffered) {
            buffered.setBufferSize(width, height);
        } else if (outputSurface instanceof SurfaceView surfaceView) {
            surfaceView.getHolder().setFixedSize(width, height);
        } else if (outputSurface instanceof TextureView textureView && textureView.isAvailable()) {
            textureView.getSurfaceTexture().setDefaultBufferSize(width, height);
        }
    }

    /**
     * Surface to read pixels from. The buffered path feeds the virtual display from an
     * off-screen texture, so captures have to come from the on-screen surface instead.
     */
    private Surface captureSurface(View view) {
        if (view instanceof BufferedOutputView buffered) return buffered.displaySurface();
        return acquireOutputSurface(view);
    }

    private Surface acquireOutputSurface(View view) {
        if (view instanceof BufferedOutputView buffered) return buffered.inputSurface();
        if (view instanceof SurfaceView surfaceView) return surfaceView.getHolder().getSurface();
        return new Surface(((TextureView) view).getSurfaceTexture());
    }

    private void releaseOutputSurface(View view, Surface surface) {
        if (view instanceof TextureView) surface.release();
    }

    private void releaseBufferedOutput(View view) {
        if (view instanceof BufferedOutputView buffered) buffered.release();
    }

    private void createVirtualDisplay() {
        View view = outputSurface;
        if (displayService == null || !isOutputAvailable(view)) return;
        int width = portraitMode ? baseHeight : baseWidth;
        int height = portraitMode ? baseWidth : baseHeight;
        try {
            setOutputBufferSize(width, height);
            Log.i(TAG, "create buffer=" + width + "x" + height + " dpi=" + densityDpi);
            Surface surface = acquireOutputSurface(view);
            try {
                boolean blockCapture = pendingLaunchPackage == null ? captureBlockedDisplay
                        : "com.netease.uuremote".equals(pendingLaunchPackage);
                virtualDisplayId = displayService.createDisplay("BoxScreen", width, height,
                        densityDpi, surface, blockCapture);
                captureBlockedDisplay = blockCapture;
                removePreviousOutput();
            } finally {
                releaseOutputSurface(view, surface);
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

    private void attachOutputSurface() {
        View view = outputSurface;
        if (displayService == null || !isOutputAvailable(view)) return;
        int width = portraitMode ? baseHeight : baseWidth;
        int height = portraitMode ? baseWidth : baseHeight;
        setOutputBufferSize(width, height);
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
                createVirtualDisplay();
                return;
            }
            Surface surface = acquireOutputSurface(view);
            try {
                displayService.setDisplaySurface(surface);
            } catch (RemoteException | RuntimeException e) {
                Log.e(TAG, "could not re-attach the existing virtual display", e);
                status("无法接回运行中的虚拟屏：" + e.getMessage());
                return;
            } finally {
                releaseOutputSurface(view, surface);
            }
            virtualDisplayId = existing;
            removePreviousOutput();
            Log.i(TAG, "re-attached existing virtual display id=" + existing);
            status("已接回运行中的虚拟屏 display ID=" + existing);
            syncSplitState();
            applyDensitySync();
            refreshVirtualDisplayLayout("re-attach virtual display");
            return;
        }

        Surface surface = acquireOutputSurface(view);
        try {
            displayService.setDisplaySurface(surface);
            removePreviousOutput();
            Log.i(TAG, "reattached output surface to virtual display=" + virtualDisplayId);
            updateOutputLayout();
            if (mouseBound && boundMouseDescriptor != null) rebindMouseToExternalDisplay();
            status("外接屏已恢复，继续使用虚拟屏 ID=" + virtualDisplayId);
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "failed to reattach surface; recreating virtual display", e);
            virtualDisplayId = -1;
            createVirtualDisplay();
        } finally {
            releaseOutputSurface(view, surface);
        }
    }

    private void captureVirtualScreen() {
        if (screenshotInProgress) return;
        View source = outputSurface;
        if (!isOutputAvailable(source) || virtualDisplayId < 0) {
            Toast.makeText(this, "虚拟屏当前没有可截图的画面", Toast.LENGTH_SHORT).show();
            return;
        }
        int width = portraitMode ? baseHeight : baseWidth;
        int height = portraitMode ? baseWidth : baseHeight;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Surface surface = captureSurface(source);
        int captureDisplayId = virtualDisplayId;
        screenshotInProgress = true;
        Log.i(TAG, "screenshot requested display=" + captureDisplayId + " size=" + width + "x" + height);
        try {
            PixelCopy.request(surface, bitmap, result -> {
                releaseOutputSurface(source, surface);
                if (result != PixelCopy.SUCCESS) {
                    screenshotInProgress = false;
                    bitmap.recycle();
                    Log.e(TAG, "PixelCopy failed result=" + result);
                    if (!isDestroyed()) {
                        Toast.makeText(this, "读取虚拟屏画面失败（" + result + "）", Toast.LENGTH_LONG).show();
                    }
                    return;
                }
                new Thread(() -> saveVirtualScreenScreenshot(bitmap, captureDisplayId),
                        "boxscreen-save-screenshot").start();
            }, screenshotPreviewHandler);
        } catch (RuntimeException e) {
            releaseOutputSurface(source, surface);
            screenshotInProgress = false;
            bitmap.recycle();
            Log.w(TAG, "could not request screenshot", e);
            Toast.makeText(this, "读取虚拟屏画面失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveVirtualScreenScreenshot(Bitmap bitmap, int captureDisplayId) {
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
        } catch (Exception e) {
            if (uri != null) {
                try {
                    getContentResolver().delete(uri, null, null);
                } catch (RuntimeException cleanupFailure) {
                    e.addSuppressed(cleanupFailure);
                }
            }
            bitmap.recycle();
            Log.e(TAG, "virtual display screenshot failed", e);
            runOnUiThread(() -> {
                screenshotInProgress = false;
                if (!isDestroyed()) {
                    Toast.makeText(this, "截图失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            return;
        }
        Uri saved = uri;
        runOnUiThread(() -> {
            screenshotInProgress = false;
            if (isDestroyed() || isFinishing() || virtualDisplayId != captureDisplayId) {
                bitmap.recycle();
                return;
            }
            Toast.makeText(this, "截图已保存", Toast.LENGTH_SHORT).show();
            showScreenshotPreview(saved, bitmap);
        });
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
            if (splitActive) {
                splitActive = splitChoosing = false;
                splitFirstPackage = splitSecondPackage = null;
                splitCheckHandler.removeCallbacks(splitCheck);
                refreshVirtualDisplayLayout("launch gallery from split");
            }
            currentVirtualPackage = gallery.activityInfo.packageName;
            applyDensitySync();
            if (mousePointerView != null) mousePointerView.invalidateRecentTasks();
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
        outputSurface = previousOutputSurface = null;
        transitionFramesRemaining = 0;
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
            setOutputBufferSize(width, height);
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
                // Refresh app bounds without resizing the rotated SurfaceView.
                // A holder resize lets SurfaceView overwrite the compositor rotation.
                if (!expectedPortrait || !(outputSurface instanceof SurfaceView)) {
                    setOutputBufferSize(nudgeWidth, nudgeHeight);
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
                    setOutputBufferSize(width, height);
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
        if (statusView == null || baseWidth <= 0 || baseHeight <= 0) return;
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
        updateDensityLabel();
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
        // The direct portrait layer rotates inside a landscape-sized host.
        boolean rotatedLayer = portraitMode && outputSurface instanceof SurfaceView;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                rotatedLayer ? baseWidth : width, rotatedLayer ? baseHeight : height, Gravity.CENTER);
        outputSurface.setLayoutParams(params);
        setOutputBufferSize(width, height);
        if (outputSurface instanceof TextureView) outputSurface.setRotation(90f);
        if (mousePointerView != null) {
            FrameLayout.LayoutParams pointerParams = new FrameLayout.LayoutParams(width, height, Gravity.CENTER);
            mousePointerView.setLayoutParams(pointerParams);
            mousePointerView.setRotation(portraitMode ? 90f : 0f);
        }
        if (modeButton != null) {
            modeButton.setText(portraitMode ? "竖屏 · 切换横屏" : "横屏 · 切换竖屏");
            updateModeButtonStyle();
        }
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
        updateDensityLabel();
        if (densitySlider != null) densitySlider.setProgress((densityDpi - 140) / 10);
        updateDensityStatus();
        Log.i(TAG, "orientation=" + (portrait ? "portrait" : "landscape"));
        View oldOutput = outputSurface;
        if (oldOutput != null && oldOutput.getParent() instanceof FrameLayout frame) {
            View nextOutput = newOutputView();
            transitionFramesRemaining = 2;
            if (mousePointerView != null) mousePointerView.hidePointer();
            if (previousOutputSurface == null) {
                previousOutputSurface = oldOutput;
            } else {
                frame.removeView(oldOutput);
                releaseBufferedOutput(oldOutput);
            }
            outputSurface = nextOutput;
            frame.addView(nextOutput, 1);
            nextOutput.postDelayed(() -> {
                if (outputSurface != nextOutput || transitionFramesRemaining == 0) return;
                transitionFramesRemaining = 0;
                if (mousePointerView != null) mousePointerView.invalidate();
            }, 180);
        }
        if (virtualDisplayId >= 0) resizeVirtualDisplay();
        else updateOutputLayout();
        if (modeButton != null) {
            modeButton.setText(portraitMode ? "竖屏 · 切换横屏" : "横屏 · 切换竖屏");
            updateModeButtonStyle();
        }
    }

    private void stopOutput() {
        if (outputOwner != null && outputOwner != this) {
            outputOwner.stopOutput();
            return;
        }
        blackoutWhenOnPrimary = false;
        if (outputOwner == this) outputOwner = null;
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
        outputSurface = previousOutputSurface = null;
        transitionFramesRemaining = 0;
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

    private boolean onPrimaryDisplay() {
        Display display = getWindow().getDecorView().getDisplay();
        return display == null || display.getDisplayId() == Display.DEFAULT_DISPLAY;
    }

    private void applyPendingPrimaryBlackout() {
        if (!blackoutWhenOnPrimary || !onPrimaryDisplay()) return;
        blackoutWhenOnPrimary = false;
        dimAndBlackoutPhoneScreen();
    }

    private void dimAndBlackoutPhoneScreen() {
        dismissGuideDialog();
        if (!onPrimaryDisplay()) {
            blackoutWhenOnPrimary = true;
            return;
        }
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
        applySystemBarTheme(getWindow(), theme);
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
        if (mouseRouteButton == null || theme == null) return;
        boolean available = displayService != null && virtualDisplayId >= 0;
        mouseRouteButton.setEnabled(available);
        mouseRouteButton.setText(mouseBound ? "鼠标已开 · 点击关闭" : "开启鼠标");
        if (!available) {
            styleFlatDisabledButton(mouseRouteButton, theme, 9);
        } else if (mouseBound) {
            styleAccentRedButton(mouseRouteButton, theme, 9);
        } else {
            styleNeutralBevelButton(mouseRouteButton, theme, 9);
        }
    }

    private final class SmartisanSliderView extends View {
        interface OnSliderChangeListener {
            void onProgressChanged(int progress, boolean fromUser);
            void onStopTrackingTouch();
        }

        private int max = 46;
        private int progress = 11;
        private boolean dragging;
        private OnSliderChangeListener listener;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        SmartisanSliderView(Context context) {
            super(context);
            setClickable(true);
        }

        void setMax(int max) {
            this.max = Math.max(1, max);
            if (progress > this.max) progress = this.max;
            invalidate();
        }

        int getProgress() {
            return progress;
        }

        void setProgress(int value) {
            int clamped = Math.max(0, Math.min(max, value));
            if (clamped != progress) {
                progress = clamped;
                invalidate();
            }
        }

        void setListener(OnSliderChangeListener listener) {
            this.listener = listener;
        }

        void applyTheme() {
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int desiredH = dp(44);
            int w = MeasureSpec.getSize(widthMeasureSpec);
            int h = resolveSize(desiredH, heightMeasureSpec);
            setMeasuredDimension(w, h);
        }

        private float grooveLeft() {
            return dp(12);
        }

        private float grooveRight() {
            return Math.max(grooveLeft() + dp(20), getWidth() - dp(12));
        }

        private float thumbTravelLeft() {
            return grooveLeft() + dp(10);
        }

        private float thumbTravelRight() {
            return Math.max(thumbTravelLeft() + 1f, grooveRight() - dp(10));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            SmartisanPalette t = theme != null ? theme : SmartisanPalette.of(false);
            float w = getWidth();
            float cy = getHeight() / 2f;
            if (w <= dp(32)) return;

            // 1. Sculpted recessed outer housing tray (Smartisan Music bottom fader bezel)
            float trayHalfH = dp(14);
            rect.set(dp(1), cy - trayHalfH, w - dp(1), cy + trayHalfH);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xffffffff);
            paint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom,
                    t.isDark ? 0xff272a30 : 0xfffafafb,
                    t.isDark ? 0xff2e3238 : 0xffffffff,
                    Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, dp(6), dp(6), paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, dp(1)));
            paint.setColor(t.isDark ? 0xff1c1f24 : 0xffe4e6ea);
            canvas.drawRoundRect(rect, dp(6), dp(6), paint);

            // 2. Inner recessed groove
            float gLeft = grooveLeft();
            float gRight = grooveRight();
            float gHalfH = dp(3.5f);
            rect.set(gLeft, cy - gHalfH, gRight, cy + gHalfH);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xffffffff);
            paint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom,
                    t.isDark ? 0xff1b1e23 : 0xffeaedf0,
                    t.isDark ? 0xff24282e : 0xfff3f5f7,
                    Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, dp(4), dp(4), paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, dp(1) * 0.8f));
            paint.setColor(t.isDark ? 0xff14161a : 0xffd9dce1);
            canvas.drawRoundRect(rect, dp(4), dp(4), paint);

            // 3. Neutral architectural brushed-grey filled track
            float ratio = max <= 0 ? 0f : progress / (float) max;
            float minX = thumbTravelLeft();
            float maxX = thumbTravelRight();
            float thumbCx = minX + (maxX - minX) * ratio;
            if (thumbCx > gLeft + dp(2)) {
                rect.set(gLeft, cy - gHalfH, Math.min(gRight, thumbCx), cy + gHalfH);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xffffffff);
                paint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom,
                        t.isDark ? 0xff868c97 : 0xff93969b,
                        t.isDark ? 0xff6d737e : 0xffa4a7ac,
                        Shader.TileMode.CLAMP));
                canvas.drawRoundRect(rect, dp(4), dp(4), paint);
                paint.setShader(null);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(1f, dp(1) * 0.8f));
                paint.setColor(t.isDark ? 0xff555a63 : 0xff808389);
                canvas.drawRoundRect(rect, dp(4), dp(4), paint);
            }

            // 4. Sculpted raised rectangular fader knob overhanging the housing tray
            float kHalfW = dp(12);
            float kHalfH = dp(18);
            float kRadius = dp(6);
            paint.setStyle(Paint.Style.FILL);
            // Multi-pass soft drop shadow under knob
            int[] shadowAlphas = t.isDark ? new int[]{0x22000000, 0x34000000, 0x48000000}
                    : new int[]{0x0c000000, 0x13000000, 0x1b000000};
            for (int i = 0; i < 3; i++) {
                float expand = (2 - i) * dp(1) * 0.6f;
                float dy = (3 - i) * dp(1);
                rect.set(thumbCx - kHalfW - expand, cy - kHalfH + dy,
                        thumbCx + kHalfW + expand, cy + kHalfH + dy);
                paint.setColor(shadowAlphas[i]);
                canvas.drawRoundRect(rect, kRadius, kRadius, paint);
            }

            // 100% solid opaque beveled knob body (reset Paint alpha before LinearGradient)
            rect.set(thumbCx - kHalfW, cy - kHalfH, thumbCx + kHalfW, cy + kHalfH);
            paint.setColor(0xffffffff);
            int kTop = t.isDark
                    ? (dragging ? 0xff4d535d : 0xff5b616c)
                    : (dragging ? 0xfff4f5f7 : 0xffffffff);
            int kBot = t.isDark
                    ? (dragging ? 0xff3d424b : 0xff474d57)
                    : (dragging ? 0xffeceef0 : 0xfffafafb);
            paint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom, kTop, kBot, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, kRadius, kRadius, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, dp(1)));
            paint.setColor(t.isDark ? 0xff21242a : (dragging ? 0xffc7cbd1 : 0xffdfe1e6));
            canvas.drawRoundRect(rect, kRadius, kRadius, paint);

            if (t.isDark) {
                paint.setStrokeWidth(Math.max(1f, dp(1) * 0.8f));
                paint.setColor(0x20ffffff);
                canvas.drawLine(rect.left + dp(3), rect.top + dp(1), rect.right - dp(3), rect.top + dp(1), paint);
            }

            // Machined concave finger-indent well inside knob
            float wellHalfW = dp(5);
            float wellHalfH = dp(10);
            rect.set(thumbCx - wellHalfW, cy - wellHalfH, thumbCx + wellHalfW, cy + wellHalfH);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xffffffff);
            paint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom,
                    t.isDark ? 0xff3b4049 : 0xfff1f2f4,
                    t.isDark ? 0xff434952 : 0xfff7f8f9,
                    Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, dp(3), dp(3), paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, dp(1) * 0.8f));
            paint.setColor(t.isDark ? 0xff31353d : 0xffe9ebee);
            canvas.drawRoundRect(rect, dp(3), dp(3), paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void updateFromTouchX(float x) {
            float minX = thumbTravelLeft();
            float maxX = thumbTravelRight();
            float ratio = Math.max(0f, Math.min(1f, (x - minX) / Math.max(1f, maxX - minX)));
            int next = Math.max(0, Math.min(max, Math.round(ratio * max)));
            if (next != progress) {
                progress = next;
                invalidate();
                if (listener != null) listener.onProgressChanged(progress, true);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!isEnabled()) return false;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                dragging = true;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                updateFromTouchX(event.getX());
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (dragging) updateFromTouchX(event.getX());
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (dragging) {
                    dragging = false;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                    if (action == MotionEvent.ACTION_UP) updateFromTouchX(event.getX());
                    invalidate();
                    if (listener != null) listener.onStopTrackingTouch();
                }
                return true;
            }
            return super.onTouchEvent(event);
        }
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
        private PanelAppDrag panelAppDrag;
        private boolean screenshotPreviewPointerDown;
        private String cachedStatusBarLabel;
        private long statusBarLabelUpdatedAt;
        private final Path statusClipPath = new Path();
        private final RectF statusIconRect = new RectF();
        private final Rect savedIconBounds = new Rect();
        private final RectF panelRect = new RectF();
        private final RectF thumbRect = new RectF();

        private final Runnable revealStatusBar = () -> {
            if (forwarding && pointerY <= statusBarRevealZone()) {
                statusBarVisible = true;
                invalidate();
            }
        };
        private final Runnable hideStatusBar = () -> {
            if (panelPointerDown && panelAppDrag != null && panelAppDrag.fromStatusBar) return;
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
        private static final float SPLIT_HANDLE_RIM_OPACITY = 0.32f;
        private static final float SPLIT_HANDLE_CORE_OPACITY = 0.62f;
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
            handler.removeCallbacks(delayedRecentTaskRefresh);
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
                    handler.postDelayed(hideStatusBar, STATUS_BAR_DWELL_MS);
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

        boolean isPointerInFirstPane() {
            return isPointerInFirstSplitPane();
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
        private long lastKeyInjectAt;
        private boolean keyInjectionPending;
        // OPPO's key interceptor moves the target display to top and waits for its
        // animation on every injected key, so rapid right-clicks used to starve the
        // main screen's focus for seconds (pointer frozen). Throttle keys hard.
        private static final long KEY_INJECT_MIN_INTERVAL_MS = 250;

        private void sendVirtualKey(String keyCode) {
            final IDisplayService service = displayService;
            final int virtualId = virtualDisplayId;
            if (service == null || virtualId < 0) return;
            int code = "BACK".equals(keyCode) ? android.view.KeyEvent.KEYCODE_BACK
                    : "MENU".equals(keyCode) ? android.view.KeyEvent.KEYCODE_MENU : 0;
            if (code == 0) return;
            long now = android.os.SystemClock.uptimeMillis();
            if (keyInjectionPending || now - lastKeyInjectAt < KEY_INJECT_MIN_INTERVAL_MS) return;
            lastKeyInjectAt = now;
            keyInjectionPending = true;
            // A live task probe can run dumpsys, and key injection can wait on a window
            // transition. Keep both off the UI and motion queues.
            new Thread(() -> {
                try {
                    if (displayService != service || virtualDisplayId != virtualId
                            || !service.hasVirtualDisplayTask()
                            || displayService != service || virtualDisplayId != virtualId) return;
                    if (service.injectKey(code, true)) service.injectKey(code, false);
                } catch (RemoteException | RuntimeException e) {
                    Log.w(TAG, "failed to inject " + keyCode + " to virtual display", e);
                } finally {
                    handler.post(() -> keyInjectionPending = false);
                }
            }, "boxscreen-virtual-key").start();
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

        private float panelDp(float value) {
            return value * panelDensityDpi / 160f;
        }

        void applyPanelDensity() {
            if (panelDensityDpi == densityDpi) return;
            panelDensityDpi = densityDpi;
            panelDisplayLabelCache.clear();
            invalidate();
        }

        private static final long STATUS_BAR_DWELL_MS = 100;
        private static final float PANEL_EDGE_MARGIN_DP = 12f;
        private static final float STATUS_ITEM_SIZE_DP = 32f;
        private static final float STATUS_ITEM_GAP_DP = PANEL_EDGE_MARGIN_DP;
        private static final float STATUS_ITEM_PITCH_DP = STATUS_ITEM_SIZE_DP + STATUS_ITEM_GAP_DP; // 44dp
        private static final float ACTION_BTN_WIDTH_DP = 44f;
        private static final float ACTION_BTN_GAP_DP = 8f;
        private static final float SPLIT_BTN_WIDTH_DP = 56f;

        private float panelEdgeMargin() { return panelDp(PANEL_EDGE_MARGIN_DP); }
        private float screenshotBtnRight() { return getWidth() - panelEdgeMargin(); }
        private float screenshotBtnLeft() { return screenshotBtnRight() - panelDp(ACTION_BTN_WIDTH_DP); }
        private float orientationBtnRight() { return screenshotBtnLeft() - panelDp(ACTION_BTN_GAP_DP); }
        private float orientationBtnLeft() { return orientationBtnRight() - panelDp(ACTION_BTN_WIDTH_DP); }
        private float inputModeBtnRight() { return orientationBtnLeft() - panelDp(ACTION_BTN_GAP_DP); }
        private float inputModeBtnLeft() { return inputModeBtnRight() - panelDp(ACTION_BTN_WIDTH_DP); }
        private float splitBtnRight() { return getWidth() - panelEdgeMargin(); }
        private float splitBtnLeft() { return splitBtnRight() - panelDp(SPLIT_BTN_WIDTH_DP); }

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
            float availableWidth = getWidth() - panelEdgeMargin() * 2f;
            float gap = panelDp(8);
            int columnsByWidth = Math.max(1, (int) ((availableWidth + gap) / (panelDp(128) + gap)));
            return Math.min(appCount, columnsByWidth);
        }

        private float quickPanelAppsTop() { return statusBarHeight() + panelDp(120); }

        private float dpiSliderLeft() { return panelDp(114f); }

        private float dpiSliderRight() { return Math.max(dpiSliderLeft() + 1, inputModeBtnLeft() - panelDp(12f)); }

        private void updateStatusBarDwell() {
            if (!forwarding) return;
            if (panelPointerDown && panelAppDrag != null && panelAppDrag.fromStatusBar) {
                handler.removeCallbacks(revealStatusBar);
                handler.removeCallbacks(hideStatusBar);
                return;
            }
            if (virtualPointerY <= statusBarRevealZone()) {
                handler.removeCallbacks(hideStatusBar);
                if (!statusBarVisible) {
                    handler.removeCallbacks(revealStatusBar);
                    handler.postDelayed(revealStatusBar, STATUS_BAR_DWELL_MS);
                }
            } else {
                handler.removeCallbacks(revealStatusBar);
                boolean insideVisiblePanel = quickPanelVisible
                        && virtualPointerY <= statusBarHeight() + quickPanelHeight();
                boolean insideVisibleBar = statusBarVisible && virtualPointerY <= statusBarHeight();
                if (statusBarVisible && !insideVisiblePanel && !insideVisibleBar) {
                    handler.removeCallbacks(hideStatusBar);
                    handler.postDelayed(hideStatusBar, STATUS_BAR_DWELL_MS);
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
                panelAppDrag = null;
                if (quickPanelVisible && y <= statusBarHeight() + quickPanelHeight()) {
                    panelPointerDown = true;
                    panelDownY = y;
                    panelPressedControl = hitQuickControl(x, y);
                    panelAppDrag = capturePanelAppDrag(panelPressedControl, x, y);
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
                    panelAppDrag = capturePanelAppDrag(panelPressedControl, x, y);
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
                boolean wasStatusBarAppDrag = panelAppDrag != null && panelAppDrag.fromStatusBar;
                boolean appDropped = performPanelAppDrop(x, y);
                if (panelPressedControl == PANEL_DPI_SLIDER) {
                    Log.i(TAG, "DPI slider up x=" + x + " y=" + y + " dpi=" + densityDpi);
                    setDensityFromPointer(x, true);
                }
                if (panelDragFromBar && y - panelDownY >= panelDp(56)) {
                    quickPanelVisible = true;
                    statusBarVisible = true;
                } else if (!appDropped && !panelDragOpened
                        && panelPressedControl != PANEL_DPI_SLIDER) {
                    int releasedControl = hitQuickControl(x, y);
                    if (releasedControl == panelPressedControl) activateQuickControl(releasedControl, x);
                }
                panelPointerDown = false;
                panelDragFromBar = false;
                panelPressedControl = PANEL_NONE;
                panelAppDrag = null;
                if (wasStatusBarAppDrag && !appDropped) updateStatusBarDwell();
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                boolean wasStatusBarAppDrag = panelAppDrag != null && panelAppDrag.fromStatusBar;
                panelPointerDown = false;
                panelDragFromBar = false;
                panelPressedControl = PANEL_NONE;
                panelAppDrag = null;
                if (wasStatusBarAppDrag) updateStatusBarDwell();
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
                if (x >= screenshotBtnLeft() && x <= screenshotBtnRight()) {
                    return PANEL_SCREENSHOT;
                }
                if (x >= orientationBtnLeft() && x <= orientationBtnRight()) {
                    return PANEL_ORIENTATION;
                }
                if (x >= inputModeBtnLeft() && x <= inputModeBtnRight()) {
                    return PANEL_INPUT_MODE;
                }
                if (x >= dpiSliderLeft() && x <= dpiSliderRight()) return PANEL_DPI_SLIDER;
                return PANEL_NONE;
            }
            float compatCenterY = barHeight + panelDp(91);
            if (!splitActive && currentVirtualPackage != null && !captureBlockedDisplay
                    && x >= splitBtnLeft() - panelDp(4f) && x <= splitBtnRight() + panelDp(4f)
                    && Math.abs(y - compatCenterY) <= panelDp(18f)) return PANEL_SPLIT;
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
                float horizontalPadding = panelEdgeMargin();
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

        private final class RecentVirtualTask {
            final int taskId;
            final String packageName;
            final String secondPackageName;

            RecentVirtualTask(int taskId, String packageName) {
                this(taskId, packageName, null);
            }

            RecentVirtualTask(int taskId, String packageName, String secondPackageName) {
                this.taskId = taskId;
                this.packageName = packageName;
                this.secondPackageName = secondPackageName;
            }

            boolean isSplitPair() {
                return secondPackageName != null;
            }
        }

        private final class PanelAppDrag {
            final String packageName;
            final String className;
            final int taskId;
            final boolean fromStatusBar;
            final boolean splitPair;
            final float left;
            final float top;
            final float right;
            final float bottom;

            PanelAppDrag(String packageName, String className, int taskId, boolean fromStatusBar,
                    boolean splitPair, float left, float top, float right, float bottom) {
                this.packageName = packageName;
                this.className = className;
                this.taskId = taskId;
                this.fromStatusBar = fromStatusBar;
                this.splitPair = splitPair;
                this.left = left;
                this.top = top;
                this.right = right;
                this.bottom = bottom;
            }
        }

        private final ArrayList<RecentVirtualTask> recentVirtualTasks = new ArrayList<>();
        private long lastRecentTaskRefreshAt;
        private boolean recentTaskRefreshInFlight;
        private final Runnable delayedRecentTaskRefresh = () -> {
            lastRecentTaskRefreshAt = 0;
            refreshRecentStatusTasks();
        };

        void invalidateRecentTasks() {
            lastRecentTaskRefreshAt = 0;
            refreshRecentStatusTasks();
            handler.removeCallbacks(delayedRecentTaskRefresh);
            handler.postDelayed(delayedRecentTaskRefresh, 350);
        }

        private final class StatusBarLayout {
            final String label;
            final ArrayList<RecentVirtualTask> apps;
            final float sliderLeft;
            final float sliderRight;
            final float appsLeft;
            final float closeLeft;

            StatusBarLayout(String label, ArrayList<RecentVirtualTask> apps, float sliderLeft,
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
            ArrayList<RecentVirtualTask> apps = recentStatusTasks();
            float width = getWidth();
            float closeLeft = width - panelEdgeMargin() - panelDp(STATUS_ITEM_SIZE_DP);
            float appsLeft = closeLeft - apps.size() * panelDp(STATUS_ITEM_PITCH_DP);
            float sliderLeft = panelEdgeMargin() + panelTextPaint.measureText(label) + panelDp(14f);
            float sliderRight = Math.min(sliderLeft + panelDp(150f), appsLeft - panelDp(12f));
            return new StatusBarLayout(label, apps, sliderLeft, sliderRight, appsLeft, closeLeft);
        }

        private PanelAppDrag capturePanelAppDrag(int control, float x, float y) {
            if (control >= PANEL_RECENT_APP_BASE) {
                StatusBarLayout layout = statusBarLayout();
                int index = control - PANEL_RECENT_APP_BASE;
                if (index < 0 || index >= layout.apps.size()) return null;
                float itemSize = panelDp(STATUS_ITEM_SIZE_DP);
                float halfGap = panelDp(STATUS_ITEM_GAP_DP / 2f);
                float pillLeft = layout.appsLeft + index * panelDp(STATUS_ITEM_PITCH_DP);
                float pillTop = (statusBarHeight() - itemSize) / 2f;
                float pillRight = pillLeft + itemSize;
                float pillBottom = pillTop + itemSize;
                if (x < pillLeft - halfGap || x > pillRight + halfGap || y < 0 || y > statusBarHeight()) return null;
                RecentVirtualTask task = layout.apps.get(index);
                return new PanelAppDrag(task.packageName, null, task.taskId, true, task.isSplitPair(),
                        pillLeft, pillTop, pillRight, pillBottom);
            }
            if (control < PANEL_APP_BASE || control >= PANEL_RECENT_APP_BASE) return null;
            ArrayList<ResolveInfo> apps = quickPanelApps();
            int index = control - PANEL_APP_BASE;
            if (index < 0 || index >= apps.size()) return null;
            int columns = quickPanelColumnCount(apps.size());
            float horizontalPadding = panelEdgeMargin();
            float gap = panelDp(8);
            float tileWidth = (getWidth() - horizontalPadding * 2f - gap * (columns - 1)) / columns;
            float left = horizontalPadding + (index % columns) * (tileWidth + gap);
            float top = quickPanelAppsTop() + (index / columns) * (panelDp(58) + panelDp(8));
            float right = left + tileWidth;
            float bottom = top + panelDp(58);
            if (x < left || x > right || y < top || y > bottom) return null;
            ResolveInfo app = apps.get(index);
            return new PanelAppDrag(app.activityInfo.packageName, app.activityInfo.name, -1, false, false,
                    left, top, right, bottom);
        }

        private boolean performPanelAppDrop(float x, float y) {
            PanelAppDrag drag = panelAppDrag;
            if (drag == null) return false;
            if (drag.splitPair) {
                if (drag.fromStatusBar && x >= drag.left && x <= drag.right && y < drag.top) {
                    ungroupSplitPairFromStatusBar();
                    return true;
                }
                if (drag.fromStatusBar && x >= drag.left && x <= drag.right && y > drag.bottom) {
                    closeSplitPairFromStatusBar();
                    return true;
                }
                return false;
            }
            if (drag.fromStatusBar && x < drag.left && y < drag.top) {
                restartDisplayedApp(drag.packageName);
                return true;
            }
            if (drag.fromStatusBar && x >= drag.left && x <= drag.right && y > drag.bottom) {
                closeDisplayedApp(drag.packageName);
                return true;
            }
            if (y < drag.top || y > drag.bottom) return false;
            boolean dropFirst = x < drag.left;
            if (!dropFirst && x <= drag.right) return false;
            dropAppIntoSplit(drag, dropFirst);
            return true;
        }

        private void dropAppIntoSplit(PanelAppDrag drag, boolean firstSide) {
            if (splitOpening || displayService == null || virtualDisplayId < 0) return;
            IDisplayService service = displayService;
            int targetDisplay = virtualDisplayId;
            boolean wasSplit = splitActive;
            quickPanelVisible = false;
            statusBarVisible = false;
            splitOpening = true;
            invalidate();
            new Thread(() -> {
                String[] result = null;
                Exception failure = null;
                try {
                    result = service.dropAppIntoSplit(drag.packageName, drag.className,
                            drag.taskId, firstSide, currentVirtualPackage);
                } catch (Exception error) {
                    failure = error;
                }
                String[] resultPair = result;
                Exception resultError = failure;
                runOnUiThread(() -> {
                    splitOpening = false;
                    if (targetDisplay != virtualDisplayId || service != displayService) return;
                    if (resultError != null || resultPair == null || resultPair.length == 0) {
                        Log.e(TAG, "drop app into split failed package=" + drag.packageName, resultError);
                        status("分屏拖放失败：" + (resultError == null ? "无返回状态" : resultError.getMessage()));
                        return;
                    }
                    if (resultPair.length == 1) {
                        splitActive = splitChoosing = false;
                        splitFirstPackage = splitSecondPackage = null;
                        currentVirtualPackage = resultPair[0];
                        splitCheckHandler.removeCallbacks(splitCheck);
                    } else {
                        splitActive = true;
                        splitChoosing = resultPair[0] == null || resultPair[1] == null;
                        splitFirstPackage = resultPair[0];
                        splitSecondPackage = resultPair[1];
                        currentVirtualPackage = resultPair[0] == null
                                ? resultPair[1] : resultPair[1] == null ? resultPair[0] : null;
                        splitLastFirst = firstSide;
                        splitPickerFirst = resultPair[0] == null;
                        if (!wasSplit) splitRatio = 0.5f;
                    }
                    applyDensitySync();
                    refreshVirtualDisplayLayout("drop app into split");
                    refreshSplitState();
                    scheduleSplitCheck();
                    invalidateRecentTasks();
                    invalidate();
                });
            }, "boxscreen-split-drop").start();
        }

        private void restartDisplayedApp(String packageName) {
            if (splitOpening || displayService == null || virtualDisplayId < 0) return;
            IDisplayService service = displayService;
            int targetDisplay = virtualDisplayId;
            boolean wasSplit = splitActive;
            boolean restorePanel = quickPanelVisible;
            quickPanelVisible = false;
            statusBarVisible = false;
            splitOpening = true;
            invalidate();
            new Thread(() -> {
                String[] result = null;
                Exception failure = null;
                try {
                    result = service.restartDisplayedApp(packageName, currentVirtualPackage);
                } catch (Exception error) {
                    failure = error;
                }
                String[] resultState = result;
                Exception resultError = failure;
                runOnUiThread(() -> {
                    splitOpening = false;
                    if (targetDisplay != virtualDisplayId || service != displayService) return;
                    if (resultError != null) {
                        quickPanelVisible = restorePanel;
                        statusBarVisible = true;
                        Log.e(TAG, "restart displayed app failed package=" + packageName, resultError);
                        status("重启前台应用失败：" + resultError.getMessage());
                        invalidate();
                        return;
                    }
                    if (resultState == null || resultState.length == 0) {
                        quickPanelVisible = restorePanel;
                        statusBarVisible = true;
                        status("该应用当前未显示在虚拟屏前台");
                        invalidate();
                        return;
                    }
                    if (resultState.length == 2) {
                        splitActive = true;
                        splitChoosing = resultState[0] == null || resultState[1] == null;
                        splitFirstPackage = resultState[0];
                        splitSecondPackage = resultState[1];
                        currentVirtualPackage = null;
                        if (!wasSplit) splitRatio = 0.5f;
                    } else {
                        splitActive = splitChoosing = false;
                        splitFirstPackage = splitSecondPackage = null;
                        currentVirtualPackage = resultState[0];
                        splitCheckHandler.removeCallbacks(splitCheck);
                    }
                    applyDensitySync();
                    refreshVirtualDisplayLayout("restart displayed app");
                    refreshSplitState();
                    scheduleSplitCheck();
                    invalidateRecentTasks();
                    invalidate();
                });
            }, "boxscreen-restart-displayed-app").start();
        }

        private void closeDisplayedApp(String packageName) {
            if (splitOpening || displayService == null || virtualDisplayId < 0) return;
            IDisplayService service = displayService;
            int targetDisplay = virtualDisplayId;
            boolean restorePanel = quickPanelVisible;
            quickPanelVisible = false;
            statusBarVisible = false;
            splitOpening = true;
            invalidate();
            new Thread(() -> {
                String[] result = null;
                Exception failure = null;
                try {
                    result = service.closeDisplayedApp(packageName, currentVirtualPackage);
                } catch (Exception error) {
                    failure = error;
                }
                String[] resultState = result;
                Exception resultError = failure;
                runOnUiThread(() -> {
                    splitOpening = false;
                    if (targetDisplay != virtualDisplayId || service != displayService) return;
                    if (resultError != null || resultState == null) {
                        quickPanelVisible = restorePanel;
                        statusBarVisible = true;
                        Log.e(TAG, "close displayed app failed package=" + packageName, resultError);
                        status(resultError == null ? "该应用当前未显示在虚拟屏前台"
                                : "关闭前台应用失败：" + resultError.getMessage());
                        invalidate();
                        return;
                    }
                    if (resultState.length == 2) {
                        splitActive = true;
                        splitChoosing = resultState[0] == null || resultState[1] == null;
                        splitFirstPackage = resultState[0];
                        splitSecondPackage = resultState[1];
                        splitPickerFirst = resultState[0] == null;
                        currentVirtualPackage = null;
                    } else {
                        splitActive = splitChoosing = false;
                        splitFirstPackage = splitSecondPackage = null;
                        currentVirtualPackage = resultState.length == 0 ? null : resultState[0];
                        splitCheckHandler.removeCallbacks(splitCheck);
                        if (currentVirtualPackage == null) restorePrimaryDensity();
                    }
                    applyDensitySync();
                    refreshVirtualDisplayLayout("close displayed app");
                    refreshSplitState();
                    invalidateRecentTasks();
                    scheduleSplitCheck();
                    invalidate();
                });
            }, "boxscreen-close-displayed-app").start();
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

        private ArrayList<RecentVirtualTask> recentStatusTasks() {
            refreshRecentStatusTasks();
            if (recentVirtualTasks.isEmpty()) return new ArrayList<>();
            panelTextPaint.setTextSize(panelDp(15));
            float sliderLeft = panelEdgeMargin() + panelTextPaint.measureText(statusBarLabel())
                    + panelDp(14f);
            float closeLeft = getWidth() - panelEdgeMargin() - panelDp(STATUS_ITEM_SIZE_DP);
            float roomForIcons = closeLeft - sliderLeft - panelDp(12f + 72f);
            int slots = Math.max(0, (int) (roomForIcons / panelDp(STATUS_ITEM_PITCH_DP)));
            slots = Math.min(16, slots);
            return new ArrayList<>(recentVirtualTasks.subList(
                    0, Math.min(slots, recentVirtualTasks.size())));
        }

        private void refreshRecentStatusTasks() {
            IDisplayService service = displayService;
            int display = virtualDisplayId;
            long now = android.os.SystemClock.uptimeMillis();
            if (service == null || display < 0 || recentTaskRefreshInFlight
                    || now - lastRecentTaskRefreshAt < 750) return;
            recentTaskRefreshInFlight = true;
            lastRecentTaskRefreshAt = now;
            new Thread(() -> {
                ArrayList<RecentVirtualTask> tasks = new ArrayList<>();
                HashSet<String> seenPackages = new HashSet<>();
                boolean seenSplitPair = false;
                try {
                    String[] rows = service.getVirtualDisplayRecentTasks();
                    if (rows != null) {
                        for (String row : rows) {
                            if (row == null) continue;
                            if (row.startsWith("split\t")) {
                                String[] parts = row.split("\t", -1);
                                if (!seenSplitPair && parts.length == 3
                                        && !parts[1].isEmpty() && !parts[2].isEmpty()) {
                                    seenSplitPair = true;
                                    tasks.add(new RecentVirtualTask(-1, parts[1], parts[2]));
                                }
                                continue;
                            }
                            int separator = row.indexOf('\t');
                            if (separator <= 0 || separator == row.length() - 1) continue;
                            try {
                                int taskId = Integer.parseInt(row.substring(0, separator));
                                String packageName = row.substring(separator + 1);
                                if (!packageName.equals(getPackageName())
                                        && seenPackages.add(packageName)) {
                                    tasks.add(new RecentVirtualTask(taskId, packageName));
                                }
                            } catch (NumberFormatException ignored) { }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "could not read virtual display recent tasks", e);
                }
                runOnUiThread(() -> {
                    recentTaskRefreshInFlight = false;
                    if (display != virtualDisplayId || service != displayService) return;
                    boolean changed = tasks.size() != recentVirtualTasks.size();
                    if (!changed) {
                        for (int i = 0; i < tasks.size(); i++) {
                            RecentVirtualTask old = recentVirtualTasks.get(i);
                            RecentVirtualTask next = tasks.get(i);
                            if (old.taskId != next.taskId
                                    || !old.packageName.equals(next.packageName)
                                    || !java.util.Objects.equals(old.secondPackageName, next.secondPackageName)) {
                                changed = true;
                                break;
                            }
                        }
                    }
                    if (changed) {
                        recentVirtualTasks.clear();
                        recentVirtualTasks.addAll(tasks);
                        invalidate();
                    }
                });
            }, "boxscreen-recent-tasks").start();
        }

        private int hitStatusBarControl(float x) {
            StatusBarLayout layout = statusBarLayout();
            float halfGap = panelDp(STATUS_ITEM_GAP_DP / 2f);
            if (x >= layout.closeLeft - halfGap) return PANEL_CLOSE_APP;
            if (!layout.apps.isEmpty() && x >= layout.appsLeft - halfGap && x < layout.closeLeft - halfGap) {
                int index = (int) Math.floor((x - layout.appsLeft + halfGap) / panelDp(STATUS_ITEM_PITCH_DP));
                if (index >= 0 && index < layout.apps.size()) return PANEL_RECENT_APP_BASE + index;
            }
            if (x >= layout.sliderLeft && x <= layout.sliderRight) return PANEL_VOLUME_SLIDER;
            return PANEL_APPS;
        }

        private void launchRecentApp(int index) {
            ArrayList<RecentVirtualTask> apps = recentStatusTasks();
            if (index < 0 || index >= apps.size() || splitOpening) return;
            RecentVirtualTask task = apps.get(index);
            if (task.isSplitPair()) {
                activateSplitPairFromStatusBar();
                return;
            }
            IDisplayService service = displayService;
            int display = virtualDisplayId;
            if (service == null || display < 0) return;
            if (!syncPrimaryDensityForPackage(task.packageName)) return;
            quickPanelVisible = false;
            statusBarVisible = false;
            splitOpening = true;
            new Thread(() -> {
                String packageName = null;
                Exception failure = null;
                try {
                    packageName = service.activateVirtualDisplayTask(task.taskId);
                } catch (Exception e) {
                    failure = e;
                }
                String activatedPackage = packageName;
                Exception result = failure;
                runOnUiThread(() -> {
                    splitOpening = false;
                    if (display != virtualDisplayId || service != displayService) return;
                    if (result != null || activatedPackage == null) {
                        Log.w(TAG, "could not activate virtual task=" + task.taskId, result);
                        status("无法切换到该最近任务");
                        return;
                    }
                    boolean wasSplit = splitActive;
                    splitActive = splitChoosing = false;
                    currentVirtualPackage = activatedPackage;
                    splitFirstPackage = splitSecondPackage = null;
                    splitCheckHandler.removeCallbacks(splitCheck);
                    syncPrimaryDensityForPackage(activatedPackage);
                    if (wasSplit) refreshVirtualDisplayLayout("activate recent virtual task");
                    invalidateRecentTasks();
                    invalidate();
                });
            }, "boxscreen-activate-recent-task").start();
        }

        private void activateSplitPairFromStatusBar() {
            if (splitOpening || displayService == null || virtualDisplayId < 0) return;
            quickPanelVisible = false;
            statusBarVisible = false;
            if (splitActive && !splitChoosing) {
                invalidate();
                return;
            }
            IDisplayService service = displayService;
            int display = virtualDisplayId;
            splitOpening = true;
            invalidate();
            new Thread(() -> {
                String[] pair = null;
                Exception failure = null;
                try {
                    pair = service.activateSplitPair();
                } catch (Exception e) {
                    failure = e;
                }
                String[] restored = pair;
                Exception result = failure;
                runOnUiThread(() -> {
                    splitOpening = false;
                    if (display != virtualDisplayId || service != displayService) return;
                    if (result != null || restored == null || restored.length != 2
                            || restored[0] == null || restored[1] == null) {
                        Log.w(TAG, "could not activate split pair", result);
                        status(result == null ? "分屏组合已失效" : "无法恢复分屏组合：" + result.getMessage());
                        invalidateRecentTasks();
                        invalidate();
                        return;
                    }
                    splitActive = true;
                    splitChoosing = false;
                    splitFirstPackage = restored[0];
                    splitSecondPackage = restored[1];
                    currentVirtualPackage = null;
                    splitPickerFirst = false;
                    applyDensitySync();
                    refreshVirtualDisplayLayout("activate split pair");
                    scheduleSplitCheck();
                    invalidateRecentTasks();
                    invalidate();
                });
            }, "boxscreen-activate-split-pair").start();
        }

        private void closeSplitPairFromStatusBar() {
            if (splitOpening || displayService == null || virtualDisplayId < 0) return;
            IDisplayService service = displayService;
            int display = virtualDisplayId;
            boolean restorePanel = quickPanelVisible;
            quickPanelVisible = false;
            statusBarVisible = false;
            splitOpening = true;
            invalidate();
            new Thread(() -> {
                String[] outcome = null;
                Exception failure = null;
                try {
                    outcome = service.closeSplitPair();
                } catch (Exception e) {
                    failure = e;
                }
                String[] resultState = outcome;
                Exception resultError = failure;
                runOnUiThread(() -> {
                    splitOpening = false;
                    if (display != virtualDisplayId || service != displayService) return;
                    if (resultError != null || resultState == null || resultState.length == 0) {
                        quickPanelVisible = restorePanel;
                        statusBarVisible = true;
                        Log.e(TAG, "close split pair failed", resultError);
                        status(resultError == null ? "当前没有可关闭的分屏组合"
                                : "关闭分屏组合失败：" + resultError.getMessage());
                        invalidateRecentTasks();
                        invalidate();
                        return;
                    }
                    if ("foreground".equals(resultState[0])) {
                        splitActive = splitChoosing = false;
                        splitFirstPackage = splitSecondPackage = null;
                        currentVirtualPackage = resultState.length > 1 ? resultState[1] : null;
                        splitCheckHandler.removeCallbacks(splitCheck);
                        if (currentVirtualPackage == null) restorePrimaryDensity();
                        else syncPrimaryDensityForPackage(currentVirtualPackage);
                        refreshVirtualDisplayLayout("close split pair");
                        status("已关闭分屏组合");
                    } else {
                        status("已关闭后台分屏组合");
                    }
                    invalidateRecentTasks();
                    invalidate();
                });
            }, "boxscreen-close-split-pair").start();
        }

        private void ungroupSplitPairFromStatusBar() {
            if (splitOpening || displayService == null || virtualDisplayId < 0) return;
            IDisplayService service = displayService;
            int display = virtualDisplayId;
            boolean preferFirst = splitLastFirst;
            boolean restorePanel = quickPanelVisible;
            quickPanelVisible = false;
            statusBarVisible = false;
            splitOpening = true;
            invalidate();
            new Thread(() -> {
                String[] outcome = null;
                Exception failure = null;
                try {
                    outcome = service.ungroupSplitPair(preferFirst);
                } catch (Exception e) {
                    failure = e;
                }
                String[] resultState = outcome;
                Exception resultError = failure;
                runOnUiThread(() -> {
                    splitOpening = false;
                    if (display != virtualDisplayId || service != displayService) return;
                    if (resultError != null || resultState == null || resultState.length == 0) {
                        quickPanelVisible = restorePanel;
                        statusBarVisible = true;
                        Log.e(TAG, "ungroup split pair failed", resultError);
                        status(resultError == null ? "当前没有可拆散的分屏组合"
                                : "拆散分屏组合失败：" + resultError.getMessage());
                        invalidateRecentTasks();
                        invalidate();
                        return;
                    }
                    if ("foreground".equals(resultState[0])) {
                        splitActive = splitChoosing = false;
                        splitFirstPackage = splitSecondPackage = null;
                        currentVirtualPackage = resultState.length > 1 ? resultState[1] : null;
                        splitCheckHandler.removeCallbacks(splitCheck);
                        applyDensitySync();
                        refreshVirtualDisplayLayout("ungroup split pair");
                        status("已拆散分屏组合");
                    } else {
                        status("已拆散后台分屏组合");
                    }
                    invalidateRecentTasks();
                    invalidate();
                });
            }, "boxscreen-ungroup-split-pair").start();
        }

        private void closeCurrentVirtualApp() {
            if (splitOpening) return;
            if (displayService == null || virtualDisplayId < 0) return;
            if (mousePointerView != null) mousePointerView.cancelTouchGesture();
            try {
                MainActivity entry = findForegroundControlsOnOutput();
                if (entry != null) {
                    returnControlsToPrimary(entry, null);
                    return;
                }
                boolean removed = displayService.closeTopTaskOnVirtualDisplay();
                Log.i(TAG, "close foreground virtual display task result=" + removed
                        + " package=" + currentVirtualPackage);
                if (removed) {
                    String[] pair = displayService.getSplitPackages();
                    if (pair != null && pair.length == 2
                            && (pair[0] != null || pair[1] != null)) {
                        splitActive = true;
                        splitChoosing = pair[0] == null || pair[1] == null;
                        splitFirstPackage = pair[0];
                        splitSecondPackage = pair[1];
                        splitPickerFirst = pair[0] == null;
                        currentVirtualPackage = null;
                        applyDensitySync();
                        refreshVirtualDisplayLayout("restore split pair after close");
                        scheduleSplitCheck();
                        invalidateRecentTasks();
                        quickPanelVisible = false;
                        statusBarVisible = true;
                    } else {
                        splitActive = splitChoosing = false;
                        splitFirstPackage = splitSecondPackage = null;
                        splitCheckHandler.removeCallbacks(splitCheck);
                        currentVirtualPackage = displayService.getVirtualDisplayForegroundPackage();
                        if (currentVirtualPackage == null) restorePrimaryDensity();
                        else syncPrimaryDensityForPackage(currentVirtualPackage);
                        invalidateRecentTasks();
                        // Keep the next foreground task as current, so it retains the split action.
                        // If the virtual display is empty, the panel is the next affordance.
                        // The regular dwell logic hides them again once the pointer leaves
                        // their area, so mouse behaviour stays unchanged.
                        quickPanelVisible = true;
                        statusBarVisible = true;
                    }
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

        private android.graphics.drawable.Drawable recentTaskIcon(String packageName) {
            android.graphics.drawable.Drawable cached = recentIconCache.get(packageName);
            if (cached != null) return cached;
            try {
                android.graphics.drawable.Drawable icon = getPackageManager().getApplicationIcon(packageName);
                if (icon != null) recentIconCache.put(packageName, icon);
                return icon;
            } catch (android.content.pm.PackageManager.NameNotFoundException | RuntimeException e) {
                Log.w(TAG, "could not load recent task icon for " + packageName, e);
                return null;
            }
        }

        private android.graphics.drawable.Drawable launcherIcon(ResolveInfo app) {
            if (app == null || app.activityInfo == null) return null;
            String packageName = app.activityInfo.packageName;
            Drawable icon = panelLauncherIconCache.get(packageName);
            if (icon == null) {
                Drawable base = getLauncherIcon(app);
                Drawable.ConstantState state = base != null ? base.getConstantState() : null;
                icon = state != null ? state.newDrawable(getResources()).mutate()
                        : app.loadIcon(getPackageManager());
                if (icon != null) panelLauncherIconCache.put(packageName, icon);
            }
            return icon;
        }

        private void drawClippedPanelIcon(Canvas canvas, Drawable icon,
                float left, float top, float right, float bottom, float rx) {
            if (icon == null) return;
            SmartisanPalette t = activePanelTheme();
            if (!t.isDark) {
                panelPaint.setShader(null);
                panelPaint.setStyle(Paint.Style.FILL);
                panelPaint.setColor(0x14000000);
                panelRect.set(left, top + Math.max(1f, panelDp(1)), right, bottom + Math.max(1f, panelDp(1)));
                canvas.drawRoundRect(panelRect, rx, rx, panelPaint);
                panelPaint.setColor(0xffffffff);
                panelRect.set(left, top, right, bottom);
                canvas.drawRoundRect(panelRect, rx, rx, panelPaint);
            }
            icon.copyBounds(savedIconBounds);
            canvas.save();
            try {
                statusIconRect.set(left, top, right, bottom);
                statusClipPath.reset();
                statusClipPath.addRoundRect(statusIconRect, rx, rx, Path.Direction.CW);
                canvas.clipPath(statusClipPath);
                icon.setBounds(Math.round(left), Math.round(top),
                        Math.round(right), Math.round(bottom));
                icon.draw(canvas);
            } finally {
                canvas.restore();
                icon.setBounds(savedIconBounds);
            }
            panelPaint.setShader(null);
            panelPaint.setStyle(Paint.Style.STROKE);
            panelPaint.setStrokeWidth(Math.max(1f, panelDp(0.75f)));
            panelPaint.setColor(t.isDark ? 0x1cffffff : 0x22000000);
            panelRect.set(left, top, right, bottom);
            canvas.drawRoundRect(panelRect, rx, rx, panelPaint);
            panelPaint.setStyle(Paint.Style.FILL);
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

        private SmartisanPalette activePanelTheme() {
            return theme != null ? theme : SmartisanPalette.of(false);
        }

        private void drawSmartisanRect(Canvas canvas, float left, float top, float right, float bottom,
                float rx, int topColor, int bottomColor, int borderColor, int shadowColor) {
            if ((shadowColor >>> 24) != 0) {
                panelPaint.setShader(null);
                panelPaint.setStyle(Paint.Style.FILL);
                panelPaint.setColor(shadowColor);
                float lip = Math.max(1f, panelDp(1));
                panelRect.set(left, top + lip, right, bottom + lip);
                canvas.drawRoundRect(panelRect, rx, rx, panelPaint);
            }
            panelRect.set(left, top, right, bottom);
            panelPaint.setStyle(Paint.Style.FILL);
            if (topColor == bottomColor) {
                panelPaint.setShader(null);
                panelPaint.setColor(topColor);
            } else {
                panelPaint.setColor(0xffffffff);
                panelPaint.setShader(new LinearGradient(0, top, 0, bottom,
                        topColor, bottomColor, Shader.TileMode.CLAMP));
            }
            canvas.drawRoundRect(panelRect, rx, rx, panelPaint);
            panelPaint.setShader(null);
            if ((borderColor >>> 24) != 0) {
                panelPaint.setStyle(Paint.Style.STROKE);
                panelPaint.setStrokeWidth(Math.max(1f, panelDp(1)));
                panelPaint.setColor(borderColor);
                canvas.drawRoundRect(panelRect, rx, rx, panelPaint);
                panelPaint.setStyle(Paint.Style.FILL);
            }
        }

        private void drawSmartisanCanvasSlider(Canvas canvas, SmartisanPalette t,
                float left, float right, float centerY, float ratio, boolean compact) {
            float clampedRatio = Math.max(0f, Math.min(1f, ratio));
            float trayHalfH = compact ? panelDp(10) : panelDp(13);
            float trayRx = panelDp(5);
            drawSmartisanRect(canvas, left, centerY - trayHalfH, right, centerY + trayHalfH, trayRx,
                    t.isDark ? 0xff272a30 : 0xfffafafb,
                    t.isDark ? 0xff2e3238 : 0xffffffff,
                    t.isDark ? 0xff1c1f24 : 0xffe4e6ea, 0);

            float inset = compact ? panelDp(7) : panelDp(9);
            float gLeft = left + inset;
            float gRight = Math.max(gLeft + panelDp(10), right - inset);
            float gHalfH = compact ? panelDp(2.5f) : panelDp(3f);
            float gRx = panelDp(3);
            drawSmartisanRect(canvas, gLeft, centerY - gHalfH, gRight, centerY + gHalfH, gRx,
                    t.isDark ? 0xff1b1e23 : 0xffeaedf0,
                    t.isDark ? 0xff24282e : 0xfff3f5f7,
                    t.isDark ? 0xff14161a : 0xffd9dce1, 0);

            float kHalfW = compact ? panelDp(7) : panelDp(9);
            float kHalfH = compact ? panelDp(12) : panelDp(16);
            float kRadius = panelDp(5);
            float travelLeft = gLeft + kHalfW * 0.7f;
            float travelRight = Math.max(travelLeft + 1f, gRight - kHalfW * 0.7f);
            float thumbCx = travelLeft + (travelRight - travelLeft) * clampedRatio;

            if (thumbCx > gLeft + panelDp(2)) {
                drawSmartisanRect(canvas, gLeft, centerY - gHalfH,
                        Math.min(gRight, thumbCx), centerY + gHalfH, gRx,
                        t.isDark ? 0xff868c97 : 0xff93969b,
                        t.isDark ? 0xff6d737e : 0xffa4a7ac,
                        t.isDark ? 0xff555a63 : 0xff808389, 0);
            }

            panelPaint.setShader(null);
            panelPaint.setStyle(Paint.Style.FILL);
            float shadowDy = Math.max(1f, panelDp(2));
            thumbRect.set(thumbCx - kHalfW - panelDp(1) * 0.5f, centerY - kHalfH + shadowDy,
                    thumbCx + kHalfW + panelDp(1) * 0.5f, centerY + kHalfH + shadowDy);
            panelPaint.setColor(t.isDark ? 0x44000000 : 0x1b000000);
            canvas.drawRoundRect(thumbRect, kRadius, kRadius, panelPaint);

            drawSmartisanRect(canvas, thumbCx - kHalfW, centerY - kHalfH,
                    thumbCx + kHalfW, centerY + kHalfH, kRadius,
                    t.isDark ? 0xff5b616c : 0xffffffff,
                    t.isDark ? 0xff474d57 : 0xfffafafb,
                    t.isDark ? 0xff21242a : 0xffdfe1e6, 0);

            float wellHalfW = compact ? panelDp(2.5f) : panelDp(3.5f);
            float wellHalfH = compact ? panelDp(6) : panelDp(8.5f);
            drawSmartisanRect(canvas, thumbCx - wellHalfW, centerY - wellHalfH,
                    thumbCx + wellHalfW, centerY + wellHalfH, panelDp(2),
                    t.isDark ? 0xff3b4049 : 0xfff1f2f4,
                    t.isDark ? 0xff434952 : 0xfff7f8f9,
                    t.isDark ? 0xff31353d : 0xffe9ebee, 0);
        }

        private void drawSmartisanCanvasSwitch(Canvas canvas, SmartisanPalette t,
                float left, float right, float centerY, boolean checked) {
            float halfH = panelDp(11);
            drawSmartisanRect(canvas, left, centerY - halfH, right, centerY + halfH, halfH,
                    checked ? t.switchOnTrack : t.switchOffTrack,
                    checked ? t.switchOnTrack : t.switchOffTrack,
                    checked ? t.switchOnBorder : t.switchOffBorder, 0);
            float knobR = panelDp(8.5f);
            float cx = checked ? right - panelDp(11) : left + panelDp(11);
            panelPaint.setShader(null);
            panelPaint.setStyle(Paint.Style.FILL);
            panelPaint.setColor(t.isDark ? 0x44000000 : 0x22000000);
            canvas.drawCircle(cx, centerY + Math.max(1f, panelDp(1)), knobR, panelPaint);
            panelPaint.setColor(0xffffffff);
            panelPaint.setShader(new LinearGradient(0, centerY - knobR, 0, centerY + knobR,
                    t.sliderThumbTop, t.sliderThumbBottom, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, centerY, knobR, panelPaint);
            panelPaint.setShader(null);
            panelPaint.setStyle(Paint.Style.STROKE);
            panelPaint.setStrokeWidth(Math.max(1f, panelDp(1)));
            panelPaint.setColor(t.sliderThumbBorder);
            canvas.drawCircle(cx, centerY, knobR, panelPaint);
            panelPaint.setStyle(Paint.Style.FILL);
        }

        private void drawQuickUi(Canvas canvas) {
            SmartisanPalette t = activePanelTheme();
            float width = getWidth();
            float barHeight = statusBarHeight();
            float oneDp = Math.max(1f, panelDp(1));

            if (quickPanelVisible) {
                float bottom = barHeight + quickPanelHeight();
                float radius = panelDp(8);
                panelPaint.setShader(null);
                panelPaint.setStyle(Paint.Style.FILL);
                panelPaint.setColor(t.isDark ? 0x55000000 : 0x1c000000);
                panelRect.set(0, -radius, width, bottom + panelDp(5));
                canvas.drawRoundRect(panelRect, radius, radius, panelPaint);

                int panelFill = t.isDark ? 0xf525282d : 0xf7eff0f2;
                drawSmartisanRect(canvas, 0, -radius, width, bottom, radius,
                        panelFill, panelFill, t.cardBorder, 0);

                ArrayList<ResolveInfo> apps = quickPanelApps();
                float controlsTop = barHeight + panelDp(8);
                float controlsBottom = barHeight + panelDp(64);
                float controlsCenterY = barHeight + panelDp(36);
                float btnTop = controlsTop + panelDp(8);
                float btnBottom = controlsBottom - panelDp(8);
                float btnRx = panelDp(8);
                boolean touchMode = inputSettingsPackage() == null || isTouchMode(inputSettingsPackage());
                boolean pressInput = panelPointerDown && panelPressedControl == PANEL_INPUT_MODE;
                boolean pressOrient = panelPointerDown && panelPressedControl == PANEL_ORIENTATION;
                boolean pressShot = panelPointerDown && panelPressedControl == PANEL_SCREENSHOT;

                // Left DPI label + embossed blue badge
                panelTextPaint.setColor(t.textSecondary);
                panelTextPaint.setTextSize(panelDp(13));
                panelTextPaint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("DPI", panelEdgeMargin(), controlsCenterY + panelDp(5), panelTextPaint);

                drawSmartisanRect(canvas, panelDp(42), controlsCenterY - panelDp(12),
                        panelDp(100), controlsCenterY + panelDp(12), panelDp(7),
                        t.accentBlueSoftBg, t.accentBlueSoftBg, t.accentBlueSoftBorder, 0);
                panelTextPaint.setColor(t.accentBlueText);
                panelTextPaint.setTextSize(panelDp(12));
                panelTextPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(String.valueOf(densityDpi), panelDp(71),
                        controlsCenterY + panelDp(4.5f), panelTextPaint);

                // Mechanical DPI fader slider
                float sliderLeft = dpiSliderLeft();
                float sliderRight = dpiSliderRight();
                float densityRatio = (densityDpi - 140) / 460f;
                drawSmartisanCanvasSlider(canvas, t, sliderLeft, sliderRight,
                        controlsCenterY, densityRatio, false);

                // Uniform 44dp wide action buttons (touch/mouse, orientation, screenshot)
                float inputLeft = inputModeBtnLeft();
                float inputRight = inputModeBtnRight();
                if (touchMode) {
                    drawSmartisanRect(canvas, inputLeft, btnTop, inputRight, btnBottom, btnRx,
                            pressInput ? t.btnPressedTop : t.accentBlueSoftBg,
                            pressInput ? t.btnPressedBottom : t.accentBlueSoftBg,
                            t.accentBlueSoftBorder, t.btnShadow);
                    drawInputModeIcon(canvas, (inputLeft + inputRight) / 2f, controlsCenterY, true, t.accentBlueText);
                } else {
                    drawSmartisanRect(canvas, inputLeft, btnTop, inputRight, btnBottom, btnRx,
                            pressInput ? t.softRedPressedTop : t.softRedTop,
                            pressInput ? t.softRedPressedBottom : t.softRedBottom,
                            t.softRedBorder, t.btnShadow);
                    drawInputModeIcon(canvas, (inputLeft + inputRight) / 2f, controlsCenterY, false, t.softRedText);
                }

                float orientLeft = orientationBtnLeft();
                float orientRight = orientationBtnRight();
                drawSmartisanRect(canvas, orientLeft, btnTop, orientRight, btnBottom, btnRx,
                        pressOrient ? t.btnPressedTop : t.btnTop,
                        pressOrient ? t.btnPressedBottom : t.btnBottom,
                        pressOrient ? t.btnPressedBorder : t.btnBorder, t.btnShadow);
                drawOrientationIcon(canvas, (orientLeft + orientRight) / 2f, controlsCenterY, t.textPrimary);

                float shotLeft = screenshotBtnLeft();
                float shotRight = screenshotBtnRight();
                drawSmartisanRect(canvas, shotLeft, btnTop, shotRight, btnBottom, btnRx,
                        pressShot ? t.btnPressedTop : t.btnTop,
                        pressShot ? t.btnPressedBottom : t.btnBottom,
                        pressShot ? t.btnPressedBorder : t.btnBorder, t.btnShadow);
                drawPanelVectorIcon(canvas, R.drawable.ic_screenshot,
                        (shotLeft + shotRight) / 2f, controlsCenterY, panelDp(21), t.textPrimary);

                // Hairline divider separating row 1 and row 2
                panelPaint.setShader(null);
                panelPaint.setColor(t.divider);
                canvas.drawRect(panelEdgeMargin(), barHeight + panelDp(68),
                        width - panelEdgeMargin(), barHeight + panelDp(68) + oneDp, panelPaint);

                float compatY = barHeight + panelDp(91);
                // Split button: authentic Smartisan Crimson Red beveled CTA
                if (!splitActive && currentVirtualPackage != null && !captureBlockedDisplay) {
                    boolean pressSplit = panelPointerDown && panelPressedControl == PANEL_SPLIT;
                    float sLeft = splitBtnLeft();
                    float sRight = splitBtnRight();
                    drawSmartisanRect(canvas, sLeft, compatY - panelDp(16),
                            sRight, compatY + panelDp(16), panelDp(8),
                            pressSplit ? t.accentRedPressedTop : t.accentRedTop,
                            pressSplit ? t.accentRedPressedBottom : t.accentRedBottom,
                            pressSplit ? t.accentRedPressedBorder : t.accentRedBorder, t.btnShadow);
                    panelTextPaint.setColor(0xffffffff);
                    panelTextPaint.setTextSize(panelDp(13));
                    panelTextPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("分屏", (sLeft + sRight) / 2f, compatY + panelDp(4.5f), panelTextPaint);
                }

                boolean compat = currentVirtualPackage != null && densityCompatPackages.contains(currentVirtualPackage);
                panelTextPaint.setTextSize(panelDp(13));
                panelTextPaint.setTextAlign(Paint.Align.LEFT);
                panelTextPaint.setColor(t.textPrimary);
                String compatLabel = "主屏 DPI 同步 · 当前应用";
                canvas.drawText(compatLabel, panelEdgeMargin(), compatY - panelDp(2), panelTextPaint);
                panelTextPaint.setTextSize(panelDp(10));
                panelTextPaint.setColor(t.textMuted);
                canvas.drawText("手机主屏显示大小会变化", panelEdgeMargin(), compatY + panelDp(12), panelTextPaint);

                panelTextPaint.setTextSize(panelDp(13));
                float compatLabelRight = panelEdgeMargin() + panelTextPaint.measureText(compatLabel);
                float compatSwitchLeft = compatLabelRight + panelDp(12);
                float compatSwitchRight = compatSwitchLeft + panelDp(40);
                drawSmartisanCanvasSwitch(canvas, t, compatSwitchLeft, compatSwitchRight, compatY, compat);

                // Shortcut app grid cards with real app icons
                float tileTop = quickPanelAppsTop();
                float tileHeight = panelDp(58);
                float gap = panelDp(8);
                float horizontalPadding = panelEdgeMargin();
                float tileGap = panelDp(8);
                int columns = quickPanelColumnCount(apps.size());
                float tileWidth = (width - horizontalPadding * 2f - tileGap * (columns - 1)) / columns;
                float iconSize = panelDp(30);
                float iconRx = panelDp(7);
                for (int i = 0; i < apps.size(); i++) {
                    int row = i / columns;
                    int column = i % columns;
                    float left = horizontalPadding + column * (tileWidth + tileGap);
                    float top = tileTop + row * (tileHeight + gap);
                    boolean pressedTile = panelPointerDown && panelPressedControl == PANEL_APP_BASE + i;
                    drawSmartisanRect(canvas, left, top, left + tileWidth, top + tileHeight, panelDp(8),
                            pressedTile ? t.btnPressedTop : (t.isDark ? t.cardBg : 0xfff8f9fb),
                            pressedTile ? t.btnPressedBottom : (t.isDark ? 0xff2f3237 : 0xfff1f3f6),
                            t.cardBorder, t.cardShadow);

                    ResolveInfo app = apps.get(i);
                    float iLeft = left + panelDp(10);
                    float iTop = top + (tileHeight - iconSize) / 2f;
                    try {
                        drawClippedPanelIcon(canvas, launcherIcon(app),
                                iLeft, iTop, iLeft + iconSize, iTop + iconSize, iconRx);
                    } catch (RuntimeException ignored) { }

                    panelTextPaint.setColor(t.textPrimary);
                    panelTextPaint.setTextSize(panelDp(13));
                    panelTextPaint.setTextAlign(Paint.Align.LEFT);
                    String label = panelDisplayLabel(app, tileWidth - panelDp(42));
                    canvas.drawText(label, left + panelDp(48),
                            top + tileHeight / 2f + panelDp(4.5f), panelTextPaint);

                    if (favoritePackages.contains(app.activityInfo.packageName)) {
                        panelPaint.setShader(null);
                        panelPaint.setColor(t.accentRedTop);
                        canvas.drawCircle(left + tileWidth - panelDp(9), top + panelDp(9),
                                panelDp(3), panelPaint);
                    }
                }
            }

            if (statusBarVisible) {
                if (!quickPanelVisible) {
                    panelPaint.setStyle(Paint.Style.FILL);
                    panelPaint.setColor(0xffffffff);
                    panelPaint.setShader(new LinearGradient(0, barHeight, 0, barHeight + panelDp(4),
                            t.titleBarShadowStart, 0x00000000, Shader.TileMode.CLAMP));
                    canvas.drawRect(0, barHeight, width, barHeight + panelDp(4), panelPaint);
                    panelPaint.setShader(null);
                }
                int barTop = t.isDark ? 0xf52e3137 : 0xf7ffffff;
                int barBot = t.isDark ? 0xf5282b30 : 0xf7f7f8fa;
                drawSmartisanRect(canvas, 0, 0, width, barHeight, 0f, barTop, barBot, 0, 0);
                panelPaint.setShader(null);
                panelPaint.setColor(t.titleBarBorder);
                canvas.drawRect(0, barHeight - oneDp, width, barHeight, panelPaint);

                StatusBarLayout layout = statusBarLayout();
                panelTextPaint.setColor(t.textPrimary);
                panelTextPaint.setTextAlign(Paint.Align.LEFT);
                panelTextPaint.setTextSize(panelDp(14.5f));
                canvas.drawText(layout.label, panelEdgeMargin(), barHeight * 0.65f, panelTextPaint);

                AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
                if (audio != null && layout.sliderRight > layout.sliderLeft) {
                    float center = barHeight / 2f;
                    float ratio = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                            / (float) Math.max(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                    drawSmartisanCanvasSlider(canvas, t, layout.sliderLeft, layout.sliderRight,
                            center, ratio, true);
                }

                float itemSize = panelDp(STATUS_ITEM_SIZE_DP);
                float pitch = panelDp(STATUS_ITEM_PITCH_DP);
                for (int i = 0; i < layout.apps.size(); i++) {
                    RecentVirtualTask task = layout.apps.get(i);
                    float pillLeft = layout.appsLeft + i * pitch;
                    float pillTop = (barHeight - itemSize) / 2f;
                    float pillRight = pillLeft + itemSize;
                    float pillBottom = pillTop + itemSize;
                    drawSmartisanRect(canvas, pillLeft, pillTop, pillRight, pillBottom, panelDp(8),
                            t.cardBg, t.isDark ? 0xff2d3035 : 0xfff5f6f8,
                            task.isSplitPair() ? t.accentBlue : t.cardBorder, t.cardShadow);
                    float iconLeft = pillLeft + panelDp(2);
                    float iconTop = pillTop + panelDp(2);
                    float iconRight = pillRight - panelDp(2);
                    float iconBottom = pillBottom - panelDp(2);
                    if (task.isSplitPair()) {
                        drawSplitPairStatusIcon(canvas, task, pillLeft, pillTop, pillRight, pillBottom,
                                iconLeft, iconTop, iconRight, iconBottom);
                    } else {
                        try {
                            drawClippedPanelIcon(canvas, recentTaskIcon(task.packageName),
                                    iconLeft, iconTop, iconRight, iconBottom, panelDp(6));
                        } catch (RuntimeException ignored) { }
                    }
                }

                float closeR = panelDp(STATUS_ITEM_SIZE_DP / 2f);
                float closeCenterX = layout.closeLeft + closeR;
                float closeCenterY = barHeight / 2f;
                boolean pressClose = panelPointerDown && panelPressedControl == PANEL_CLOSE_APP;
                drawSmartisanRect(canvas, closeCenterX - closeR, closeCenterY - closeR,
                        closeCenterX + closeR, closeCenterY + closeR, closeR,
                        pressClose ? t.softRedPressedTop : t.softRedTop,
                        pressClose ? t.softRedPressedBottom : t.softRedBottom,
                        t.softRedBorder, t.btnShadow);
                float arm = panelDp(5.2f);
                panelPaint.setStyle(Paint.Style.STROKE);
                panelPaint.setStrokeCap(Paint.Cap.ROUND);
                panelPaint.setStrokeWidth(Math.max(1.5f, panelDp(1.7f)));
                panelPaint.setColor(t.softRedText);
                canvas.drawLine(closeCenterX - arm, closeCenterY - arm,
                        closeCenterX + arm, closeCenterY + arm, panelPaint);
                canvas.drawLine(closeCenterX + arm, closeCenterY - arm,
                        closeCenterX - arm, closeCenterY + arm, panelPaint);
                panelPaint.setStyle(Paint.Style.FILL);
            }
        }

        private void drawSplitPairStatusIcon(Canvas canvas, RecentVirtualTask task,
                float pillLeft, float pillTop, float pillRight, float pillBottom,
                float iconLeft, float iconTop, float iconRight, float iconBottom) {
            SmartisanPalette t = activePanelTheme();
            float subSize = panelDp(18);
            float subRadius = panelDp(4);
            try {
                android.graphics.drawable.Drawable firstIcon = recentTaskIcon(task.packageName);
                if (firstIcon != null) {
                    canvas.save();
                    statusIconRect.set(iconLeft, iconTop, iconLeft + subSize, iconTop + subSize);
                    statusClipPath.reset();
                    statusClipPath.addRoundRect(statusIconRect, subRadius, subRadius, Path.Direction.CW);
                    canvas.clipPath(statusClipPath);
                    firstIcon.setBounds(Math.round(statusIconRect.left), Math.round(statusIconRect.top),
                            Math.round(statusIconRect.right), Math.round(statusIconRect.bottom));
                    firstIcon.draw(canvas);
                    canvas.restore();
                }
            } catch (RuntimeException ignored) { }

            float gap = Math.max(1f, panelDp(1));
            canvas.save();
            statusIconRect.set(pillLeft, pillTop, pillRight, pillBottom);
            statusClipPath.reset();
            statusClipPath.addRoundRect(statusIconRect, panelDp(8), panelDp(8), Path.Direction.CW);
            canvas.clipPath(statusClipPath);
            panelPaint.setShader(null);
            panelPaint.setColor(t.cardBg);
            canvas.drawRoundRect(iconRight - subSize - gap, iconBottom - subSize - gap,
                    iconRight + gap, iconBottom + gap, panelDp(5), panelDp(5), panelPaint);
            canvas.restore();

            try {
                android.graphics.drawable.Drawable secondIcon = recentTaskIcon(task.secondPackageName);
                if (secondIcon != null) {
                    canvas.save();
                    statusIconRect.set(iconRight - subSize, iconBottom - subSize, iconRight, iconBottom);
                    statusClipPath.reset();
                    statusClipPath.addRoundRect(statusIconRect, subRadius, subRadius, Path.Direction.CW);
                    canvas.clipPath(statusClipPath);
                    secondIcon.setBounds(Math.round(statusIconRect.left), Math.round(statusIconRect.top),
                            Math.round(statusIconRect.right), Math.round(statusIconRect.bottom));
                    secondIcon.draw(canvas);
                    canvas.restore();
                }
            } catch (RuntimeException ignored) { }
        }

        private void drawInputModeIcon(Canvas canvas, float cx, float cy, boolean touchMode, int tintColor) {
            drawPanelVectorIcon(canvas,
                    touchMode ? R.drawable.ic_touch_app : R.drawable.ic_input_mouse,
                    cx, cy, panelDp(21), tintColor);
        }

        private void drawOrientationIcon(Canvas canvas, float cx, float cy, int tintColor) {
            drawPanelVectorIcon(canvas, R.drawable.ic_screen_orientation, cx, cy, panelDp(21), tintColor);
        }

        private void drawPanelVectorIcon(Canvas canvas, int resourceId, float cx, float cy,
                float size, int tintColor) {
            android.graphics.drawable.Drawable icon = getContext().getDrawable(resourceId);
            if (icon == null) return;
            Drawable tinted = icon.mutate();
            int save = canvas.save();
            int left = Math.round(cx - size / 2f);
            int top = Math.round(cy - size / 2f);
            tinted.setBounds(left, top, Math.round(cx + size / 2f), Math.round(cy + size / 2f));
            tinted.setTint(tintColor);
            tinted.draw(canvas);
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
            SmartisanPalette t = activePanelTheme();
            panelTextPaint.setColor(t.textPrimary);
            panelTextPaint.setTextAlign(Paint.Align.CENTER);
            if (screenshotPreviewExpanded) {
                RectF bounds = screenshotExpandedBounds();
                panelPaint.setShader(null);
                panelPaint.setStyle(Paint.Style.FILL);
                panelPaint.setColor(t.isDark ? 0xee1c1f24 : 0xebedeeef);
                canvas.drawRect(0, 0, getWidth(), getHeight(), panelPaint);
                drawSmartisanRect(canvas, bounds.left, bounds.top, bounds.right, bounds.bottom, panelDp(14),
                        t.cardBg, t.cardBg, t.cardBorder, t.cardShadow);
                RectF imageBounds = fitBitmap(new RectF(bounds.left + panelDp(14), bounds.top + panelDp(14),
                        bounds.right - panelDp(14), bounds.bottom - panelDp(84)), bitmap);
                canvas.save();
                statusClipPath.reset();
                statusClipPath.addRoundRect(imageBounds, panelDp(8), panelDp(8), Path.Direction.CW);
                canvas.clipPath(statusClipPath);
                canvas.drawBitmap(bitmap, null, imageBounds, screenshotImagePaint);
                canvas.restore();

                float buttonY = getHeight() - panelDp(72);
                drawSmartisanRect(canvas, panelDp(24), buttonY, getWidth() * 0.36f,
                        buttonY + panelDp(44), panelDp(9),
                        t.softRedTop, t.softRedBottom, t.softRedBorder, t.btnShadow);
                drawSmartisanRect(canvas, getWidth() * 0.64f, buttonY, getWidth() - panelDp(24),
                        buttonY + panelDp(44), panelDp(9),
                        t.btnTop, t.btnBottom, t.btnBorder, t.btnShadow);
                panelTextPaint.setTextSize(panelDp(15));
                panelTextPaint.setColor(t.softRedText);
                canvas.drawText("删除", (panelDp(24) + getWidth() * 0.36f) / 2f,
                        buttonY + panelDp(28), panelTextPaint);
                panelTextPaint.setColor(t.textPrimary);
                canvas.drawText("图库打开", (getWidth() * 0.64f + getWidth() - panelDp(24)) / 2f,
                        buttonY + panelDp(28), panelTextPaint);
                float closeX = getWidth() / 2f;
                float closeY = buttonY + panelDp(22);
                float cr = panelDp(19);
                drawSmartisanRect(canvas, closeX - cr, closeY - cr, closeX + cr, closeY + cr, cr,
                        t.btnTop, t.btnBottom, t.btnBorder, t.btnShadow);
                panelPaint.setColor(t.textPrimary);
                panelPaint.setStyle(Paint.Style.STROKE);
                panelPaint.setStrokeCap(Paint.Cap.ROUND);
                panelPaint.setStrokeWidth(panelDp(2));
                canvas.drawLine(closeX - panelDp(6), closeY - panelDp(6),
                        closeX + panelDp(6), closeY + panelDp(6), panelPaint);
                canvas.drawLine(closeX + panelDp(6), closeY - panelDp(6),
                        closeX - panelDp(6), closeY + panelDp(6), panelPaint);
                panelPaint.setStyle(Paint.Style.FILL);
            } else {
                RectF card = screenshotThumbnailBounds();
                drawSmartisanRect(canvas, card.left, card.top, card.right, card.bottom, panelDp(12),
                        t.cardBg, t.cardBg, t.cardBorder, t.cardShadow);
                RectF imageBounds = fitBitmap(new RectF(card.left + panelDp(8), card.top + panelDp(8),
                        card.right - panelDp(8), card.bottom - panelDp(34)), bitmap);
                canvas.save();
                statusClipPath.reset();
                statusClipPath.addRoundRect(imageBounds, panelDp(6), panelDp(6), Path.Direction.CW);
                canvas.clipPath(statusClipPath);
                canvas.drawBitmap(bitmap, null, imageBounds, screenshotImagePaint);
                canvas.restore();
                panelTextPaint.setColor(t.textSecondary);
                panelTextPaint.setTextSize(panelDp(12));
                canvas.drawText("截图已保存 · 点击预览", card.centerX(),
                        card.bottom - panelDp(11), panelTextPaint);
            }
            if (screenshotPreviewMessage != null) {
                RectF message = new RectF(panelDp(24), getHeight() - panelDp(116),
                        getWidth() - panelDp(24), getHeight() - panelDp(24));
                drawSmartisanRect(canvas, message.left, message.top, message.right, message.bottom, panelDp(12),
                        t.cardBg, t.cardBg, t.cardBorder, t.cardShadow);
                panelTextPaint.setColor(t.textPrimary);
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
            View output = outputSurface;
            if (!swap || !isOutputAvailable(output) || getWidth() < 1 || getHeight() < 1) {
                apply.run();
                return;
            }
            Bitmap previous = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Surface source = captureSurface(output);
            try {
                PixelCopy.request(source, previous, result -> {
                    releaseOutputSurface(output, source);
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
                releaseOutputSurface(output, source);
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
                    if (swap) invalidateRecentTasks();
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
            handler.removeCallbacks(delayedRecentTaskRefresh);
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
            SmartisanPalette t = activePanelTheme();
            RectF[] clusters = splitKillClusters();
            panelTextPaint.setTextAlign(Paint.Align.CENTER);
            float oneDp = Math.max(1f, panelDp(1));
            for (int side = 0; side < 2; side++) {
                RectF cluster = clusters[side];
                // getSplitPackages() already returns visual order: 0 = left/top pane.
                String packageName = side == 0 ? splitFirstPackage : splitSecondPackage;
                panelPaint.setShader(null);
                panelPaint.setStyle(Paint.Style.FILL);
                panelPaint.setColor(t.isDark ? 0x55000000 : 0x22000000);
                panelRect.set(cluster.left, cluster.top + panelDp(2),
                        cluster.right, cluster.bottom + panelDp(3));
                canvas.drawRoundRect(panelRect, panelDp(11), panelDp(11), panelPaint);

                drawSmartisanRect(canvas, cluster.left, cluster.top, cluster.right, cluster.bottom,
                        panelDp(11), t.btnTop, t.btnBottom, t.btnBorder, 0);
                float cellWidth = cluster.width() / 3f;
                for (int col = 1; col < 3; col++) {
                    float sepX = cluster.left + col * cellWidth;
                    panelPaint.setColor(t.divider);
                    canvas.drawRect(sepX, cluster.top + panelDp(8),
                            sepX + oneDp, cluster.bottom - panelDp(8), panelPaint);
                }
                for (int col = 0; col < 3; col++) {
                    int index = side * 3 + col;
                    boolean enabled = packageName != null;
                    float cellLeft = cluster.left + col * cellWidth;
                    if (splitKillMenuPressed == index && enabled) {
                        drawSmartisanRect(canvas, cellLeft + panelDp(3), cluster.top + panelDp(3),
                                cellLeft + cellWidth - panelDp(3), cluster.bottom - panelDp(3),
                                panelDp(8), t.btnPressedTop, t.btnPressedBottom, t.btnPressedBorder, 0);
                    }
                    float cx = cellLeft + cellWidth / 2f;
                    float cy = cluster.centerY();
                    float half = panelDp(7.5f);
                    panelPaint.setStyle(Paint.Style.STROKE);
                    panelPaint.setStrokeCap(Paint.Cap.ROUND);
                    panelPaint.setStrokeWidth(panelDp(2));
                    int symColor = !enabled ? t.textDisabled
                            : (col == 2 ? t.softRedText : t.textPrimary);
                    panelPaint.setColor(symColor);
                    if (col == 0) {
                        canvas.drawLine(cx - half, cy, cx + half, cy, panelPaint);
                    } else if (col == 1) {
                        canvas.drawRoundRect(cx - half, cy - half, cx + half, cy + half,
                                panelDp(2), panelDp(2), panelPaint);
                    } else {
                        float d = half * 0.85f;
                        canvas.drawLine(cx - d, cy - d, cx + d, cy + d, panelPaint);
                        canvas.drawLine(cx + d, cy - d, cx - d, cy + d, panelPaint);
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
                        splitFirstPackage = splitSecondPackage = null;
                        splitCheckHandler.removeCallbacks(splitCheck);
                        currentVirtualPackage = resultName;
                        status("已放大 " + resultName);
                    } else if (action == 0) {
                        status("已最小化，可选择其他应用");
                    } else {
                        status("已关闭 " + resultName);
                    }
                    invalidateRecentTasks();
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
                SmartisanPalette t = activePanelTheme();
                RectF area = splitPickerBounds();
                panelPaint.setShader(null);
                panelPaint.setStyle(Paint.Style.FILL);
                panelPaint.setColor(t.pageBg);
                canvas.drawRect(area, panelPaint);

                float headerHeight = panelDp(46);
                drawSmartisanRect(canvas, area.left, area.top, area.right, area.top + headerHeight, 0f,
                        t.titleBarTop, t.titleBarBottom, 0, 0);
                panelPaint.setColor(t.titleBarBorder);
                canvas.drawRect(area.left, area.top + headerHeight - Math.max(1f, panelDp(1)),
                        area.right, area.top + headerHeight, panelPaint);

                panelTextPaint.setColor(t.textPrimary);
                panelTextPaint.setTextAlign(Paint.Align.CENTER);
                panelTextPaint.setTextSize(panelDp(15));
                String side = portraitMode ? (splitPickerFirst ? "上方" : "下方")
                        : (splitPickerFirst ? "左侧" : "右侧");
                canvas.drawText("选择" + side + "应用", area.centerX(),
                        area.top + panelDp(29), panelTextPaint);

                ArrayList<ResolveInfo> apps = splitPickerApps();
                int columns = splitPickerColumns();
                float cellWidth = area.width() / columns;
                float rowHeight = panelDp(74);
                float top = area.top + panelDp(52);
                canvas.save();
                canvas.clipRect(area);
                float iconHalf = Math.round(panelDp(17));
                float iconRx = panelDp(7);
                for (int i = splitPickerOffset; i < apps.size(); i++) {
                    int slot = i - splitPickerOffset;
                    float cellLeft = area.left + (slot % columns) * cellWidth;
                    float y = top + slot / columns * rowHeight;
                    if (y >= area.bottom) break;
                    drawSmartisanRect(canvas, cellLeft + panelDp(4), y - panelDp(2),
                            cellLeft + cellWidth - panelDp(4), y + rowHeight - panelDp(6), panelDp(10),
                            t.cardBg, t.isDark ? 0xff2f3237 : 0xfff8f9fa, t.cardBorder, t.cardShadow);
                    float x = cellLeft + cellWidth * 0.5f;
                    ResolveInfo app = apps.get(i);
                    try {
                        drawClippedPanelIcon(canvas, launcherIcon(app),
                                x - iconHalf, y + panelDp(4),
                                x + iconHalf, y + panelDp(4) + 2 * iconHalf, iconRx);
                    } catch (RuntimeException ignored) { }
                    panelTextPaint.setColor(t.textPrimary);
                    panelTextPaint.setTextSize(panelDp(11.5f));
                    canvas.drawText(panelDisplayLabel(app, cellWidth - panelDp(8)),
                            x, y + panelDp(56), panelTextPaint);
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
            if (transitionFramesRemaining > 0) {
                canvas.drawColor(0xff000000);
                return;
            }
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
        appsCard.addView(appGrid, fullWidth());
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
        panelLauncherIconCache.clear();
        recentIconCache.clear();
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
            return getLauncherLabel(a).compareToIgnoreCase(getLauncherLabel(b));
        });
        if (appsCountView != null) appsCountView.setText(String.valueOf(visible.size()));
        if (appsEmptyView != null) appsEmptyView.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
        int cellWidth = appGrid.getWidth() > 0 && appColumns > 0
                ? appGrid.getWidth() / appColumns : 0;
        appGrid.setClipChildren(false);
        appGrid.setClipToPadding(false);
        final int plateRadius = dp(11);
        final android.view.ViewOutlineProvider iconOutlineProvider = new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View v, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), plateRadius);
            }
        };
        for (ResolveInfo app : visible) {
            String packageName = app.activityInfo.packageName;
            String appLabel = getLauncherLabel(app);
            boolean isFavorite = favoritePackages.contains(packageName);

            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
            tile.setMinimumHeight(dp(90));
            tile.setPadding(dp(4), dp(8), dp(4), dp(8));
            tile.setClipChildren(false);
            tile.setClipToPadding(false);
            tile.setBackground(buildTileBackground(theme, editFavorites && isFavorite));

            FrameLayout iconBox = new FrameLayout(this);
            iconBox.setClipChildren(false);
            iconBox.setClipToPadding(false);

            FrameLayout iconPlate = new FrameLayout(this);
            iconPlate.setBackground(theme.isDark ? null : roundedCard(0xffffffff, 0, 11));
            iconPlate.setOutlineProvider(iconOutlineProvider);
            iconPlate.setClipToOutline(true);
            iconPlate.setForeground(buildIconHairlineOutline(theme.isDark));
            iconPlate.setElevation(theme.isDark ? dp(1.5f) : dp(2.2f));
            if (Build.VERSION.SDK_INT >= 28) {
                iconPlate.setOutlineAmbientShadowColor(theme.isDark ? 0x55000000 : 0x26000000);
                iconPlate.setOutlineSpotShadowColor(theme.isDark ? 0x66000000 : 0x36000000);
            }

            ImageView icon = new ImageView(this);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setImageDrawable(getLauncherIcon(app));
            iconPlate.addView(icon, new FrameLayout.LayoutParams(-1, -1));
            iconBox.addView(iconPlate, new FrameLayout.LayoutParams(dp(46), dp(46), Gravity.CENTER));

            TextView badgeView = null;
            if (editFavorites) {
                badgeView = createFavoriteBadgeView(true, isFavorite);
                badgeView.setElevation(dp(4));
                FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.TOP | Gravity.END);
                badgeParams.setMargins(0, 0, 0, 0);
                iconBox.addView(badgeView, badgeParams);
            } else if (isFavorite) {
                badgeView = createFavoriteBadgeView(false, true);
                badgeView.setElevation(dp(4));
                FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(dp(16), dp(16), Gravity.TOP | Gravity.END);
                badgeParams.setMargins(0, 0, dp(1), 0);
                iconBox.addView(badgeView, badgeParams);
            }
            tile.addView(iconBox, new LinearLayout.LayoutParams(dp(58), dp(54)));

            TextView label = text(appLabel, 11.5f, false, theme.textPrimary);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
            labelParams.setMargins(0, dp(4), 0, 0);
            tile.addView(label, labelParams);

            GridLayout.LayoutParams cell = new GridLayout.LayoutParams();
            if (cellWidth > 0) cell.width = cellWidth;
            cell.height = -2;
            cell.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            appGrid.addView(tile, cell);

            if (editFavorites) {
                TextView editBadge = badgeView;
                tile.setOnClickListener(v -> {
                    boolean nowChecked = !favoritePackages.contains(packageName);
                    if (nowChecked) favoritePackages.add(packageName);
                    else favoritePackages.remove(packageName);
                    getPreferences(MODE_PRIVATE).edit()
                            .putStringSet("favorite_apps", new HashSet<>(favoritePackages)).apply();
                    quickPanelAppCache = null;
                    if (mousePointerView != null) mousePointerView.invalidate();
                    tile.setBackground(buildTileBackground(theme, nowChecked));
                    if (editBadge != null) updateFavoriteBadgeView(editBadge, true, nowChecked);
                });
            } else {
                tile.setOnClickListener(v -> launchPackage(packageName, app.activityInfo.name));
            }
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
        boolean targetFirst = splitPickerFirst;
        splitOpening = true;
        new Thread(() -> {
            Exception failure = null;
            String openedFirstPackage = null;
            try {
                if (second == null) openedFirstPackage = service.beginSplit();
                else service.fillSplit(second.activityInfo.packageName, second.activityInfo.name, targetFirst);
            } catch (Exception e) {
                failure = e;
            }
            Exception result = failure;
            String firstPackage = openedFirstPackage;
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
                    currentVirtualPackage = firstPackage;
                    splitFirstPackage = firstPackage;
                    splitSecondPackage = null;
                } else {
                    currentVirtualPackage = null;
                    splitLastFirst = targetFirst;
                }
                refreshVirtualDisplayLayout(splitChoosing ? "begin split" : "fill split pane");
                refreshSplitState();
                applyDensitySync();
                scheduleSplitCheck();
                if (mousePointerView != null) mousePointerView.invalidateRecentTasks();
            });
        }, "boxscreen-split").start();
    }

    private final Handler splitCheckHandler = new Handler(android.os.Looper.getMainLooper());
    private final ExecutorService splitCheckExecutor = Executors.newSingleThreadExecutor(
            task -> new Thread(task, "boxscreen-split-check"));
    private final Runnable splitCheck = this::pollSplitState;
    private int splitStateGeneration;
    private boolean splitCheckInFlight;

    private void scheduleSplitCheck() {
        splitCheckHandler.removeCallbacks(splitCheck);
        if (splitActive && virtualDisplayId >= 0 && displayService != null) {
            splitCheckHandler.postDelayed(splitCheck, 850);
        }
    }

    private void pollSplitState() {
        IDisplayService service = displayService;
        int displayId = virtualDisplayId;
        if (!splitActive || displayId < 0 || service == null || isDestroyed()) return;
        if (splitOpening || splitCheckInFlight
                || (mousePointerView != null && mousePointerView.splitLayoutChanging)) {
            scheduleSplitCheck();
            return;
        }
        int generation = splitStateGeneration;
        splitCheckInFlight = true;
        splitCheckExecutor.execute(() -> {
            try {
                String[] pair = service.getSplitPackages();
                splitCheckHandler.post(() -> {
                    if (!isDestroyed() && splitActive && !splitOpening
                            && displayService == service && virtualDisplayId == displayId
                            && generation == splitStateGeneration
                            && (mousePointerView == null || !mousePointerView.splitLayoutChanging)) {
                        applySplitState(pair);
                    }
                });
            } catch (RemoteException | RuntimeException e) {
                Log.w(TAG, "could not poll split state", e);
            } finally {
                splitCheckHandler.post(() -> {
                    splitCheckInFlight = false;
                    if (!isDestroyed()) scheduleSplitCheck();
                });
            }
        });
    }

    /**
     * The split itself lives in the shell service, so the pane state outlives this activity
     * (activity restart, new build install, low-memory kill). Adopt whatever the service
     * reports instead of trusting the in-memory flag only.
     */
    private void syncSplitState() {
        splitStateGeneration++;
        if (displayService == null || virtualDisplayId < 0) return;
        try {
            String[] pair = displayService.getSplitPackages();
            if (pair == null || pair.length != 2) {
                if (splitActive) {
                    splitActive = splitChoosing = false;
                    splitFirstPackage = splitSecondPackage = null;
                    splitCheckHandler.removeCallbacks(splitCheck);
                    if (mousePointerView != null) mousePointerView.invalidate();
                }
                if (currentVirtualPackage == null) {
                    currentVirtualPackage = displayService.getVirtualDisplayForegroundPackage();
                }
                if (mousePointerView != null) mousePointerView.invalidateRecentTasks();
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
            if (mousePointerView != null) {
                mousePointerView.invalidateRecentTasks();
                mousePointerView.invalidate();
            }
            scheduleSplitCheck();
        } catch (RemoteException | RuntimeException e) {
            Log.w(TAG, "could not sync split state", e);
        }
    }

    private void refreshSplitState() {
        splitStateGeneration++;
        if (!splitActive || displayService == null) return;
        try {
            applySplitState(displayService.getSplitPackages());
        } catch (RemoteException | RuntimeException e) {
            Log.w(TAG, "could not read split state", e);
        }
    }

    private void applySplitState(String[] pair) {
        if (pair == null || pair.length != 2) {
            boolean wasActive = splitActive;
            splitActive = splitChoosing = false;
            splitFirstPackage = splitSecondPackage = null;
            splitCheckHandler.removeCallbacks(splitCheck);
            if (wasActive && mousePointerView != null) {
                mousePointerView.invalidateRecentTasks();
                mousePointerView.invalidate();
            }
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
            mousePointerView.invalidateRecentTasks();
            mousePointerView.invalidate();
        }
        if (changed) applyDensitySync();
    }

    /** Visual side under the mouse, falling back to the last active pane. */
    private boolean isPointerInFirstSplitPaneForPanel() {
        if (mousePointerView != null) return mousePointerView.isPointerInFirstPane();
        return splitLastFirst;
    }

    private void expandCurrentSplit() {
        if (!splitActive || splitOpening || displayService == null) return;
        try {
            // expandSplit() takes the visual side: true = left/top pane.
            currentVirtualPackage = displayService.expandSplit(isPointerInFirstSplitPaneForPanel());
            splitActive = splitChoosing = false;
            splitFirstPackage = splitSecondPackage = null;
            splitCheckHandler.removeCallbacks(splitCheck);
            applyDensitySync();
            refreshVirtualDisplayLayout("expand split");
            if (mousePointerView != null) {
                mousePointerView.invalidateRecentTasks();
                mousePointerView.invalidate();
            }
            status("已退出分屏，正在刷新全屏画面");
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "could not expand split", e);
            status("切换全屏失败：" + e.getMessage());
        }
    }

    private MainActivity findForegroundControlsOnOutput() throws RemoteException {
        for (MainActivity activity : controlActivities) {
            if (activity.onPrimaryDisplay() || activity.isFinishing()) continue;
            // Most close clicks target another app. Only inspect the task stack when a
            // BoxScreen window is actually attached to the output display.
            int foreground = displayService.getForegroundBoxScreenTaskId();
            for (MainActivity controls : controlActivities) {
                if (controls.getTaskId() == foreground && !controls.onPrimaryDisplay()
                        && !controls.isFinishing()) {
                    return controls;
                }
            }
            break;
        }
        return null;
    }

    private void launchPackage(String packageName, String activityName) {
        MainActivity owner = outputOwner;
        if (owner != null && owner.virtualDisplayId >= 0
                && (!onPrimaryDisplay() || (this != owner && getTaskId() == owner.getTaskId()))) {
            owner.returnControlsToPrimary(this,
                    () -> owner.launchPackageOnOutput(packageName, activityName));
            return;
        }
        launchPackageOnOutput(packageName, activityName);
    }

    private void returnControlsToPrimary(MainActivity entry, Runnable continuation) {
        if (returningControls || splitOpening || displayService == null || virtualDisplayId < 0) return;
        IDisplayService service = displayService;
        int display = virtualDisplayId;
        int task = getTaskId();
        boolean checkTaskOnOutput = !onPrimaryDisplay() || !entry.onPrimaryDisplay()
                || (entry != this && entry.getTaskId() == task);
        Runnable complete = () -> {
            if (displayService != service || virtualDisplayId != display || isFinishing()) return;
            for (MainActivity activity : new ArrayList<>(controlActivities)) {
                if (activity != this && (activity == entry || activity.getTaskId() == task)) {
                    activity.finish();
                }
            }
            if (continuation != null) continuation.run();
        };
        if (!checkTaskOnOutput) {
            complete.run();
            return;
        }
        returningControls = true;
        new Thread(() -> {
            try {
                service.returnBoxScreenTaskToPrimary(task);
                runOnUiThread(() -> {
                    returningControls = false;
                    complete.run();
                });
            } catch (RemoteException | RuntimeException e) {
                runOnUiThread(() -> {
                    returningControls = false;
                    Log.e(TAG, "could not return BoxScreen task to the primary display", e);
                    entry.status("无法把 BoxScreen 带回主屏：" + e.getMessage());
                });
            }
        }, "boxscreen-return-controls").start();
    }

    private void launchPackageOnOutput(String packageName, String activityName) {
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
            if (outputSurface == null || !isOutputAvailable(outputSurface)) {
                status("外接屏画面尚未准备好，无法切换虚拟屏");
                return;
            }
            try {
                if (mousePointerView != null) mousePointerView.cancelTouchGesture();
                displayService.releaseDisplay();
                virtualDisplayId = -1;
                createVirtualDisplay();
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
            boolean leavingSplit = splitActive;
            displayService.launchActivity(packageName, activityName);
            splitActive = splitChoosing = false;
            splitFirstPackage = splitSecondPackage = null;
            splitCheckHandler.removeCallbacks(splitCheck);
            currentVirtualPackage = packageName;
            if (leavingSplit) refreshVirtualDisplayLayout("launch fullscreen from split");
            recentAppPackages.remove(packageName);
            recentAppPackages.add(0, packageName);
            while (recentAppPackages.size() > 8) recentAppPackages.remove(recentAppPackages.size() - 1);
            quickPanelAppCache = null;
            getPreferences(MODE_PRIVATE).edit()
                    .putString("recent_apps", android.text.TextUtils.join("\n", recentAppPackages))
                    .apply();
            if (mousePointerView != null) {
                mousePointerView.invalidateRecentTasks();
                mousePointerView.invalidate();
            }
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
        launchPackageOnOutput(packageName, activityName);
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

    private Configuration currentSystemConfiguration() {
        android.content.Context app = getApplicationContext();
        return app != null ? app.getResources().getConfiguration() : getResources().getConfiguration();
    }

    private boolean isNightConfig(Configuration config) {
        return config != null
                && (config.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private boolean resolveDarkMode() {
        if (themeMode == THEME_MODE_LIGHT) return false;
        if (themeMode == THEME_MODE_DARK) return true;
        return systemNightMode;
    }

    private ArrayList<MainActivity> activeThemeTargets() {
        ArrayList<MainActivity> targets = new ArrayList<>(controlActivities);
        if (outputOwner != null && !targets.contains(outputOwner)) targets.add(outputOwner);
        return targets;
    }

    private void syncSystemNightAcrossInstances(boolean nextSysNight) {
        for (MainActivity activity : activeThemeTargets()) {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                activity.syncSystemNightState(nextSysNight);
            }
        }
    }

    private void syncSystemNightState(boolean nextSysNight) {
        if (systemNightMode == nextSysNight) return;
        systemNightMode = nextSysNight;
        boolean nextDark = resolveDarkMode();
        if (nextDark != darkMode) {
            darkMode = nextDark;
            theme = SmartisanPalette.of(darkMode);
            applySmartisanTheme();
        }
    }

    private void setThemeMode(int mode) {
        int normalized = (mode >= THEME_MODE_SYSTEM && mode <= THEME_MODE_DARK) ? mode : THEME_MODE_SYSTEM;
        getPreferences(MODE_PRIVATE).edit().putInt(PREF_THEME_MODE, normalized).apply();
        for (MainActivity activity : activeThemeTargets()) {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                activity.applyThemeModeState(normalized);
            }
        }
    }

    private void applyThemeModeState(int mode) {
        this.themeMode = (mode >= THEME_MODE_SYSTEM && mode <= THEME_MODE_DARK) ? mode : THEME_MODE_SYSTEM;
        darkMode = resolveDarkMode();
        theme = SmartisanPalette.of(darkMode);
        applySmartisanTheme();
    }

    private TextView buildThemeSegment(String label, int mode) {
        TextView btn = text(label, 11.5f, false, theme.textSecondary);
        btn.setGravity(Gravity.CENTER);
        btn.setMinHeight(dp(28));
        btn.setPadding(dp(10), 0, dp(10), 0);
        btn.setClickable(true);
        btn.setFocusable(true);
        btn.setOnClickListener(v -> setThemeMode(mode));
        return btn;
    }

    private void styleThemeSegmentButton(TextView btn, boolean selected, SmartisanPalette t) {
        if (btn == null) return;
        int pl = btn.getPaddingLeft();
        int pt = btn.getPaddingTop();
        int pr = btn.getPaddingRight();
        int pb = btn.getPaddingBottom();
        if (selected) {
            int top = t.isDark ? 0xff4c525c : t.btnTop;
            int bot = t.isDark ? 0xff3f444d : t.btnBottom;
            int border = t.isDark ? 0xff262930 : t.btnBorder;
            btn.setBackground(buildBeveledFace(top, bot, border, t.btnShadow, 7, false));
            btn.setTextColor(t.textPrimary);
            btn.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            btn.setBackground(null);
            btn.setTextColor(t.textSecondary);
            btn.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        btn.setPadding(pl, pt, pr, pb);
    }

    private void updateThemeSegmentStyles(SmartisanPalette t) {
        if (themeSegmentTray != null) {
            GradientDrawable trayBg = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{t.searchBgTop, t.searchBgBottom});
            trayBg.setCornerRadius(dp(9));
            trayBg.setStroke(Math.max(1, dp(1)), t.searchBorder);
            themeSegmentTray.setBackground(trayBg);
        }
        styleThemeSegmentButton(themeModeSystemBtn, themeMode == THEME_MODE_SYSTEM, t);
        styleThemeSegmentButton(themeModeLightBtn, themeMode == THEME_MODE_LIGHT, t);
        styleThemeSegmentButton(themeModeDarkBtn, themeMode == THEME_MODE_DARK, t);
    }

    @SuppressWarnings("deprecation")
    private void applySystemBarTheme(Window window, SmartisanPalette t) {
        if (window == null || t == null) return;
        window.setStatusBarColor(t.titleBarBottom);
        window.setNavigationBarColor(t.pageBg);
        View decor = window.getDecorView();
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(t.isDark ? 0 : mask, mask);
            }
        } else {
            int flags = decor.getSystemUiVisibility();
            int lightMask = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | (Build.VERSION.SDK_INT >= 26 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
            decor.setSystemUiVisibility(t.isDark ? (flags & ~lightMask) : (flags | lightMask));
        }
    }

    private void applySmartisanTheme() {
        SmartisanPalette t = theme;
        if (t == null) return;
        applySystemBarTheme(getWindow(), t);
        if (rootLayout != null) rootLayout.setBackgroundColor(t.pageBg);
        if (rootShell != null) rootShell.setBackgroundColor(t.pageBg);
        if (mainScroll != null) mainScroll.setBackgroundColor(t.pageBg);
        if (content != null) content.setBackgroundColor(t.pageBg);
        if (topBarContainer != null) {
            GradientDrawable barBg = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{t.titleBarTop, t.titleBarBottom});
            topBarContainer.setBackground(barBg);
        }
        if (topBarTitle != null) topBarTitle.setTextColor(t.textPrimary);
        if (versionChipView != null) {
            versionChipView.setTextColor(t.textSecondary);
            versionChipView.setBackground(roundedCard(t.chipBg, t.chipBorder, 6));
        }
        if (topBarDivider != null) topBarDivider.setBackgroundColor(t.titleBarBorder);
        if (topBarShadow != null) {
            topBarShadow.setBackground(new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{t.titleBarShadowStart, 0x00000000}));
        }
        if (guideButton != null) {
            Drawable helpDrawable = getDrawable(R.drawable.ic_help_guide);
            if (helpDrawable != null) {
                Drawable tintedHelp = helpDrawable.mutate();
                tintedHelp.setTint(t.textPrimary);
                tintedHelp.setBounds(0, 0, dp(15), dp(15));
                guideButton.setCompoundDrawables(tintedHelp, null, null, null);
                guideButton.setCompoundDrawablePadding(dp(5));
            }
            styleNeutralBevelButton(guideButton, t, 8);
        }
        if (statusView != null) {
            statusView.setTextColor(t.accentBlueText);
            statusView.setBackground(roundedCard(t.accentBlueSoftBg, t.accentBlueSoftBorder, 9));
        }
        if (sectionControlTitle != null) sectionControlTitle.setTextColor(t.textSection);
        if (controlCard != null) controlCard.setBackground(buildSmartisanCard(t, 12));
        if (cardDivider1 != null) cardDivider1.setBackgroundColor(t.divider);
        if (cardDivider2 != null) cardDivider2.setBackgroundColor(t.divider);
        if (monitorView != null) monitorView.setTextColor(t.textPrimary);
        if (monitorMetaView != null) monitorMetaView.setTextColor(t.textSecondary);
        updateMonitorLabel();
        updateShizukuBadge();
        updateMouseRouteButton();
        updateModeButtonStyle();
        if (blackoutButton != null) styleNeutralBevelButton(blackoutButton, t, 9);
        if (stopButton != null) styleSoftRedButton(stopButton, t, 9);
        if (densityTitleView != null) densityTitleView.setTextColor(t.textPrimary);
        if (densityView != null) {
            densityView.setTextColor(t.accentBlueText);
            densityView.setBackground(roundedCard(t.accentBlueSoftBg, t.accentBlueSoftBorder, 7));
        }
        if (densityRangeView != null) densityRangeView.setTextColor(t.textMuted);
        if (densitySlider != null) densitySlider.applyTheme();
        if (cardDivider3 != null) cardDivider3.setBackgroundColor(t.divider);
        if (themeTitleView != null) themeTitleView.setTextColor(t.textPrimary);
        updateThemeSegmentStyles(t);
        if (appsSectionTitle != null) appsSectionTitle.setTextColor(t.textSection);
        if (appsCountView != null) {
            appsCountView.setTextColor(t.textSecondary);
            appsCountView.setBackground(roundedCard(t.chipBg, t.chipBorder, 7));
        }
        if (systemAppsLabel != null) systemAppsLabel.setTextColor(t.textSecondary);
        if (systemAppsSwitch != null) {
            systemAppsSwitch.setTrackTintList(null);
            systemAppsSwitch.setThumbTintList(null);
            systemAppsSwitch.setTrackDrawable(buildSmartisanSwitchTrack(t));
            systemAppsSwitch.setThumbDrawable(buildSmartisanSwitchThumb(t));
        }
        updateEditFavoritesButtonStyle();
        if (searchContainer != null) {
            GradientDrawable searchBg = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{t.searchBgTop, t.searchBgBottom});
            searchBg.setCornerRadius(dp(10));
            searchBg.setStroke(Math.max(1, dp(1)), t.searchBorder);
            searchContainer.setBackground(searchBg);
        }
        if (searchIconView != null) {
            Drawable searchDrawable = getDrawable(R.drawable.ic_search);
            if (searchDrawable != null) {
                Drawable tintedSearch = searchDrawable.mutate();
                tintedSearch.setTint(t.textMuted);
                searchIconView.setImageDrawable(tintedSearch);
            }
        }
        if (appSearch != null) {
            appSearch.setTextColor(t.textPrimary);
            appSearch.setHintTextColor(t.textMuted);
        }
        if (searchClearButton != null) {
            searchClearButton.setTextColor(t.textSecondary);
            searchClearButton.setBackground(buildTileBackground(t, false));
        }
        if (appsCard != null) {
            appsCard.setBackground(buildSmartisanCard(t.appsCardBg, t.cardBorder, t.cardShadow, 12));
        }
        if (appsEmptyView != null) appsEmptyView.setTextColor(t.textMuted);
        if (appGrid != null && appGrid.getWidth() > 0 && appColumns > 0) rebuildAppsGrid();
        if (mousePointerView != null) mousePointerView.invalidate();
        applyGuideDialogTheme();
    }

    private void applyGuideDialogTheme() {
        SmartisanPalette t = theme;
        if (guideDialog == null || t == null) return;
        applySystemBarTheme(guideDialog.getWindow(), t);
        if (guideContainer != null) guideContainer.setBackgroundColor(t.pageBg);
        if (guideTopBar != null) {
            guideTopBar.setBackground(new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{t.titleBarTop, t.titleBarBottom}));
        }
        if (guideTitleView != null) guideTitleView.setTextColor(t.textPrimary);
        if (guideCloseButton != null) styleNeutralBevelButton(guideCloseButton, t, 8);
        if (guideTopDivider != null) guideTopDivider.setBackgroundColor(t.titleBarBorder);
        if (guideTopShadow != null) {
            guideTopShadow.setBackground(new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{t.titleBarShadowStart, 0x00000000}));
        }
        if (guideWebView != null) {
            guideWebView.setBackgroundColor(t.webBg);
            guideWebView.evaluateJavascript(
                    "document.documentElement.classList.toggle('dark'," + t.isDark
                            + ");document.documentElement.classList.toggle('light'," + (!t.isDark) + ");", null);
        }
    }

    private void updateMonitorLabel() {
        if (monitorView == null || theme == null) return;
        externalDisplay = findExternalDisplay();
        if (monitorStatusDot != null) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            if (externalDisplay != null) {
                dot.setColor(0xff38b26f);
                dot.setStroke(Math.max(1, dp(1)), 0xff227a48);
            } else {
                dot.setColor(0xffe0922b);
                dot.setStroke(Math.max(1, dp(1)), 0xffa86716);
            }
            monitorStatusDot.setBackground(dot);
        }
        if (externalDisplay == null) {
            monitorView.setText("未检测到外接显示器");
            if (monitorMetaView != null) {
                monitorMetaView.setText("连接支持视频输出的外接显示器后自动识别");
            }
            return;
        }
        Display.Mode mode = externalDisplay.getMode();
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        externalDisplay.getRealMetrics(metrics);
        monitorView.setText("外接屏 " + externalDisplay.getName() + " · "
                + mode.getPhysicalWidth() + "×" + mode.getPhysicalHeight()
                + " @ " + Math.round(mode.getRefreshRate()) + " Hz");
        if (monitorMetaView != null) {
            monitorMetaView.setText("系统 ID " + externalDisplay.getDisplayId() + " · 物理密度 " + metrics.densityDpi + " dpi");
        }
    }

    private void updateShizukuBadge() {
        if (shizukuBadge == null || theme == null) return;
        if (displayService != null) {
            shizukuBadge.setText("Shizuku 已连接");
            shizukuBadge.setTextColor(theme.accentBlueText);
            shizukuBadge.setBackground(roundedCard(theme.accentBlueSoftBg, theme.accentBlueSoftBorder, 8));
        } else {
            boolean binderReady = false;
            boolean granted = false;
            try {
                binderReady = Shizuku.pingBinder();
                if (binderReady) granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
            } catch (RuntimeException ignored) { }
            if (binderReady && granted) {
                shizukuBadge.setText("Shizuku 就绪");
                shizukuBadge.setTextColor(theme.accentBlueText);
                shizukuBadge.setBackground(roundedCard(theme.accentBlueSoftBg, theme.accentBlueSoftBorder, 8));
            } else if (binderReady) {
                shizukuBadge.setText("待授权 Shizuku");
                shizukuBadge.setTextColor(theme.amberText);
                shizukuBadge.setBackground(roundedCard(theme.amberSoftBg, theme.amberSoftBorder, 8));
            } else {
                shizukuBadge.setText("需启动 Shizuku");
                shizukuBadge.setTextColor(theme.amberText);
                shizukuBadge.setBackground(roundedCard(theme.amberSoftBg, theme.amberSoftBorder, 8));
            }
        }
    }

    private void updateDensityLabel() {
        if (densityView != null) densityView.setText(densityDpi + " DPI");
    }

    private void updateModeButtonStyle() {
        if (modeButton == null || theme == null) return;
        if (portraitMode) {
            styleBevelButton(modeButton,
                    theme.accentBlueSoftBg, theme.accentBlueSoftBg, theme.accentBlueSoftBorder,
                    theme.btnPressedTop, theme.btnPressedBottom, theme.accentBlueSoftBorder,
                    theme.btnDisabledBg, theme.btnDisabledBorder, theme.btnShadow,
                    theme.accentBlueText, 9);
        } else {
            styleNeutralBevelButton(modeButton, theme, 9);
        }
    }

    private void updateEditFavoritesButtonStyle() {
        if (editFavoritesButton == null || theme == null) return;
        if (editFavorites) {
            styleAccentRedButton(editFavoritesButton, theme, 8);
        } else {
            styleNeutralBevelButton(editFavoritesButton, theme, 8);
        }
    }

    private void showOperationGuideDialog() {
        if (isFinishing() || isDestroyed()) return;
        if (guideDialog != null && guideDialog.isShowing()) return;
        String html = readGuideAssetHtml();
        if (html == null) {
            Toast.makeText(this, "无法读取内置操作图解", Toast.LENGTH_SHORT).show();
            return;
        }
        int dialogTheme = theme != null && theme.isDark
                ? android.R.style.Theme_Material_NoActionBar
                : android.R.style.Theme_Material_Light_NoActionBar;
        android.app.Dialog dialog = new android.app.Dialog(this, dialogTheme);
        Window dialogWindow = dialog.getWindow();
        if (dialogWindow != null) {
            dialogWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }

        guideContainer = new LinearLayout(this);
        guideContainer.setOrientation(LinearLayout.VERTICAL);

        guideTopBar = new LinearLayout(this);
        guideTopBar.setOrientation(LinearLayout.VERTICAL);
        guideTopBar.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset;
            if (Build.VERSION.SDK_INT >= 30) {
                topInset = insets.getInsets(WindowInsets.Type.statusBars()).top;
            } else {
                topInset = insets.getSystemWindowInsetTop();
            }
            view.setPadding(0, topInset, 0, 0);
            return insets;
        });

        FrameLayout topBarInner = new FrameLayout(this);
        topBarInner.setPadding(dp(14), 0, dp(14), 0);
        guideTopBar.addView(topBarInner, new LinearLayout.LayoutParams(-1, dp(52)));

        guideTitleView = text("BoxScreen 操作图解", 17, true, theme.textPrimary);
        guideTitleView.setLetterSpacing(0.02f);
        topBarInner.addView(guideTitleView, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));

        guideCloseButton = button("关闭");
        guideCloseButton.setTextSize(12);
        guideCloseButton.setMinHeight(dp(32));
        guideCloseButton.setMinimumHeight(dp(32));
        guideCloseButton.setMinWidth(0);
        guideCloseButton.setMinimumWidth(0);
        guideCloseButton.setPadding(dp(13), 0, dp(13), 0);
        guideCloseButton.setOnClickListener(v -> dialog.dismiss());
        topBarInner.addView(guideCloseButton, new FrameLayout.LayoutParams(-2, dp(32), Gravity.END | Gravity.CENTER_VERTICAL));

        guideTopDivider = new View(this);
        guideTopBar.addView(guideTopDivider, new LinearLayout.LayoutParams(-1, Math.max(1, dp(1))));
        guideContainer.addView(guideTopBar, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout webFrame = new FrameLayout(this);
        webFrame.setOnApplyWindowInsetsListener((view, insets) -> {
            int bottomInset;
            if (Build.VERSION.SDK_INT >= 30) {
                bottomInset = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
            } else {
                bottomInset = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(0, 0, 0, bottomInset);
            return insets;
        });

        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request == null ? null : request.getUrl();
                return url == null || !url.toString().startsWith(GUIDE_BASE_URL);
            }
        });
        boolean useDark = theme != null && theme.isDark;
        html = html.replaceFirst("<html lang=\"zh-CN\">",
                useDark ? "<html lang=\"zh-CN\" class=\"dark\">" : "<html lang=\"zh-CN\" class=\"light\">");
        webView.loadDataWithBaseURL(GUIDE_BASE_URL, html, "text/html", "utf-8", null);
        webFrame.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        guideTopShadow = new View(this);
        webFrame.addView(guideTopShadow, new FrameLayout.LayoutParams(-1, dp(4), Gravity.TOP));
        guideContainer.addView(webFrame, new LinearLayout.LayoutParams(-1, 0, 1f));

        dialog.setContentView(guideContainer);
        dialog.setCancelable(true);
        dialog.setOnDismissListener(d -> {
            if (guideWebView != null) {
                if (guideWebView.getParent() instanceof android.view.ViewGroup parent) {
                    parent.removeView(guideWebView);
                }
                guideWebView.stopLoading();
                guideWebView.loadUrl("about:blank");
                guideWebView.destroy();
                guideWebView = null;
            }
            if (guideDialog == d) {
                guideDialog = null;
                guideContainer = null;
                guideTopBar = null;
                guideTitleView = null;
                guideCloseButton = null;
                guideTopDivider = null;
                guideTopShadow = null;
            }
        });
        guideDialog = dialog;
        guideWebView = webView;
        applyGuideDialogTheme();
        dialog.show();
    }

    private void dismissGuideDialog() {
        if (guideDialog != null) {
            try { guideDialog.dismiss(); }
            catch (RuntimeException ignored) { }
            guideDialog = null;
        }
    }

    private String readGuideAssetHtml() {
        try (InputStream in = getAssets().open(GUIDE_ASSET_NAME);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            Log.e(TAG, "failed to load guide asset " + GUIDE_ASSET_NAME, e);
            return null;
        }
    }

    private boolean hasShizukuPackage() {
        try { getPackageManager().getPackageInfo("moe.shizuku.privileged.api", 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    private void status(String text) {
        if (statusView == null) return;
        if (text == null || text.isEmpty()) {
            statusView.setText("");
            statusView.setVisibility(View.GONE);
        } else {
            statusView.setText(text);
            statusView.setVisibility(View.VISIBLE);
        }
    }

    private TextView text(String value, int size, boolean bold) {
        return text(value, (float) size, bold, theme.textPrimary);
    }

    private TextView text(String value, float size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private GradientDrawable roundedCard(int fill, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if ((stroke >>> 24) != 0) d.setStroke(Math.max(1, dp(1)), stroke);
        return d;
    }

    private Drawable buildSmartisanCard(SmartisanPalette t, int radiusDp) {
        return buildSmartisanCard(t.cardBg, t.cardBorder, t.cardShadow, radiusDp);
    }

    private Drawable buildSmartisanCard(int bgColor, int borderColor, int shadowColor, int radiusDp) {
        GradientDrawable shadow = new GradientDrawable();
        shadow.setColor(shadowColor);
        shadow.setCornerRadius(dp(radiusDp));
        GradientDrawable body = roundedCard(bgColor, borderColor, radiusDp);
        LayerDrawable layer = new LayerDrawable(new Drawable[]{shadow, body});
        int lip = Math.max(1, dp(1));
        layer.setLayerInset(0, 0, lip, 0, 0);
        layer.setLayerInset(1, 0, 0, 0, lip);
        return layer;
    }

    private Drawable buildIconHairlineOutline(boolean dark) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(0x00000000);
        d.setCornerRadius(dp(11));
        d.setStroke(Math.max(1, Math.round(dp(0.75f))), dark ? 0x18ffffff : 0x1b000000);
        return d;
    }

    private Drawable buildBeveledFace(int top, int bottom, int border, int shadowColor, int radiusDp, boolean pressed) {
        GradientDrawable face = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
        face.setCornerRadius(dp(radiusDp));
        face.setStroke(Math.max(1, dp(1)), border);
        if (pressed || (shadowColor >>> 24) == 0) {
            return new InsetDrawable(face, 0, dp(1), 0, Math.max(1, dp(1)));
        }
        GradientDrawable shadow = new GradientDrawable();
        shadow.setColor(shadowColor);
        shadow.setCornerRadius(dp(radiusDp));
        LayerDrawable layer = new LayerDrawable(new Drawable[]{shadow, face});
        int lip = Math.max(1, dp(1));
        layer.setLayerInset(0, 0, lip, 0, 0);
        layer.setLayerInset(1, 0, 0, 0, lip);
        return layer;
    }

    private void styleBevelButton(Button b,
            int normalTop, int normalBottom, int normalBorder,
            int pressedTop, int pressedBottom, int pressedBorder,
            int disabledBg, int disabledBorder, int shadowColor,
            int textColor, int radiusDp) {
        int pl = b.getPaddingLeft();
        int pt = b.getPaddingTop();
        int pr = b.getPaddingRight();
        int pb = b.getPaddingBottom();
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled},
                roundedCard(disabledBg, disabledBorder, radiusDp));
        states.addState(new int[]{android.R.attr.state_pressed},
                buildBeveledFace(pressedTop, pressedBottom, pressedBorder, shadowColor, radiusDp, true));
        states.addState(new int[0],
                buildBeveledFace(normalTop, normalBottom, normalBorder, shadowColor, radiusDp, false));
        b.setBackground(states);
        b.setPadding(pl, pt, pr, pb);
        b.setTextColor(textColor);
        b.setStateListAnimator(null);
        b.setElevation(0f);
    }

    private void styleNeutralBevelButton(Button b, SmartisanPalette t, int radiusDp) {
        styleBevelButton(b, t.btnTop, t.btnBottom, t.btnBorder,
                t.btnPressedTop, t.btnPressedBottom, t.btnPressedBorder,
                t.btnDisabledBg, t.btnDisabledBorder, t.btnShadow,
                t.textPrimary, radiusDp);
    }

    private void styleFlatDisabledButton(Button b, SmartisanPalette t, int radiusDp) {
        int pl = b.getPaddingLeft();
        int pt = b.getPaddingTop();
        int pr = b.getPaddingRight();
        int pb = b.getPaddingBottom();
        b.setBackground(roundedCard(t.btnDisabledBg, t.btnDisabledBorder, radiusDp));
        b.setPadding(pl, pt, pr, pb);
        b.setTextColor(t.textDisabled);
        b.setStateListAnimator(null);
        b.setElevation(0f);
    }

    private void styleAccentRedButton(Button b, SmartisanPalette t, int radiusDp) {
        styleBevelButton(b, t.accentRedTop, t.accentRedBottom, t.accentRedBorder,
                t.accentRedPressedTop, t.accentRedPressedBottom, t.accentRedPressedBorder,
                t.btnDisabledBg, t.btnDisabledBorder, t.btnShadow,
                0xffffffff, radiusDp);
    }

    private void styleSoftRedButton(Button b, SmartisanPalette t, int radiusDp) {
        styleBevelButton(b, t.softRedTop, t.softRedBottom, t.softRedBorder,
                t.softRedPressedTop, t.softRedPressedBottom, t.softRedBorder,
                t.btnDisabledBg, t.btnDisabledBorder, t.btnShadow,
                t.softRedText, radiusDp);
    }

    private Drawable buildSmartisanSwitchTrack(SmartisanPalette t) {
        GradientDrawable on = new GradientDrawable();
        on.setSize(dp(42), dp(24));
        on.setColor(t.switchOnTrack);
        on.setCornerRadius(dp(12));
        on.setStroke(Math.max(1, dp(1)), t.switchOnBorder);

        GradientDrawable off = new GradientDrawable();
        off.setSize(dp(42), dp(24));
        off.setColor(t.switchOffTrack);
        off.setCornerRadius(dp(12));
        off.setStroke(Math.max(1, dp(1)), t.switchOffBorder);

        StateListDrawable track = new StateListDrawable();
        track.addState(new int[]{android.R.attr.state_checked}, on);
        track.addState(new int[0], off);
        return track;
    }

    private Drawable buildSmartisanSwitchThumb(SmartisanPalette t) {
        GradientDrawable knob = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{t.sliderThumbTop, t.sliderThumbBottom});
        knob.setSize(dp(18), dp(18));
        knob.setShape(GradientDrawable.OVAL);
        knob.setStroke(Math.max(1, dp(1)), t.sliderThumbBorder);
        return new InsetDrawable(knob, dp(3), dp(3), dp(3), dp(3));
    }

    private Drawable buildTileBackground(SmartisanPalette t, boolean selectedInEdit) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed},
                roundedCard(t.tilePressedBg, 0x00000000, 10));
        if (selectedInEdit) {
            states.addState(new int[0], roundedCard(t.tileFavEditBg, t.tileFavEditBorder, 10));
        } else {
            states.addState(new int[0], roundedCard(0x00000000, 0x00000000, 10));
        }
        return states;
    }

    private TextView createFavoriteBadgeView(boolean editMode, boolean checked) {
        TextView badge = new TextView(this);
        badge.setGravity(Gravity.CENTER);
        badge.setTextSize(editMode ? 10 : 9);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        updateFavoriteBadgeView(badge, editMode, checked);
        return badge;
    }

    private void updateFavoriteBadgeView(TextView badge, boolean editMode, boolean checked) {
        if (badge == null || theme == null) return;
        if (checked) {
            GradientDrawable red = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{theme.accentRedTop, theme.accentRedBottom});
            red.setShape(GradientDrawable.OVAL);
            red.setStroke(Math.max(1, dp(1)), theme.cardBg);
            badge.setBackground(red);
            badge.setTextColor(0xffffffff);
            badge.setText(editMode ? "✓" : "★");
        } else {
            GradientDrawable empty = new GradientDrawable();
            empty.setShape(GradientDrawable.OVAL);
            empty.setColor(theme.isDark ? 0x661e2025 : 0xd0ffffff);
            empty.setStroke(Math.max(1, dp(1)), theme.textMuted);
            badge.setBackground(empty);
            badge.setText("");
        }
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTypeface(null, android.graphics.Typeface.BOLD);
        b.setMinHeight(dp(42));
        b.setMinimumHeight(dp(42));
        b.setPadding(dp(10), 0, dp(10), 0);
        if (theme != null) styleNeutralBevelButton(b, theme, 9);
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

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }

}
