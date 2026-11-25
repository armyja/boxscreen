package com.armyja.boxscreen;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

/**
 * Physical optical Liquid Glass engine modeled after the Lens design system
 * (ruiqichenbiec/design-systems) and Android RuntimeShader/RenderEffect compositing:
 * two-stage backdrop Gaussian blur + Snell's law refraction (air 1.0 -> glass 1.45) +
 * sharp beveled edge chromatic dispersion + Schlick Fresnel environment reflection +
 * narrow directional rim specular & opposite caustic + bottom bevel thickness occlusion.
 *
 * Optical RuntimeShader / RenderEffect passes run on Android 13+ (API 33+) with tasteful
 * translucent fallbacks on API 29-32.
 */
final class LiquidGlassEngine {

    private LiquidGlassEngine() {}

    static boolean isOpticalSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }

    static float[] uniformRadii(float r) {
        return new float[]{r, r, r, r, r, r, r, r};
    }

    static float[] uniformShaderRadii(float r) {
        return new float[]{r, r, r, r};
    }

    static float[] sheetBottomRadii(float r) {
        return new float[]{0f, 0f, r, r};
    }

    static final class LiquidGlassParams {
        final float blurDp;
        final float refraction;
        final float dispersion;
        final float darkMode;
        final float tintAlpha;
        final int outerBorderColor;
        final int fallbackTopColor;
        final int fallbackBottomColor;
        final int fallbackRimColor;
        final boolean dockTop;
        final boolean dockLeft;
        final boolean dockRight;

        LiquidGlassParams(
                float blurDp,
                float refraction,
                float dispersion,
                float darkMode,
                float tintAlpha,
                int outerBorderColor,
                int fallbackTopColor,
                int fallbackBottomColor,
                int fallbackRimColor,
                boolean dockTop,
                boolean dockLeft,
                boolean dockRight) {
            this.blurDp = blurDp;
            this.refraction = refraction;
            this.dispersion = dispersion;
            this.darkMode = darkMode;
            this.tintAlpha = tintAlpha;
            this.outerBorderColor = outerBorderColor;
            this.fallbackTopColor = fallbackTopColor;
            this.fallbackBottomColor = fallbackBottomColor;
            this.fallbackRimColor = fallbackRimColor;
            this.dockTop = dockTop;
            this.dockLeft = dockLeft;
            this.dockRight = dockRight;
        }

        static LiquidGlassParams externalPanel(boolean dark) {
            if (dark) {
                return new LiquidGlassParams(
                        6.5f, 0.88f, 0.65f, 1.0f, 0.58f,
                        0x32000000,
                        0xc8121822, 0xd40c1118, 0x55ffffff,
                        true, true, true);
            }
            return new LiquidGlassParams(
                    6.5f, 0.88f, 0.65f, 0.0f, 0.24f,
                    0x18000000,
                    0xcbf7fbff, 0xbeeaf1fa, 0xb8ffffff,
                    true, true, true);
        }

        static LiquidGlassParams floatingCard(boolean dark, boolean modal) {
            if (dark) {
                return new LiquidGlassParams(
                        modal ? 12.0f : 9.0f,
                        modal ? 0.72f : 0.85f,
                        modal ? 0.45f : 0.58f,
                        1.0f,
                        modal ? 0.62f : 0.48f,
                        0x3c000000,
                        0xdd181e28, 0xe411161e, 0x58ffffff,
                        false, false, false);
            }
            return new LiquidGlassParams(
                    modal ? 12.0f : 9.0f,
                    modal ? 0.72f : 0.85f,
                    modal ? 0.45f : 0.58f,
                    0.0f,
                    modal ? 0.48f : 0.36f,
                    0x1e000000,
                    0xe4ffffff, 0xd8f2f6fc, 0xc4ffffff,
                    false, false, false);
        }

        static LiquidGlassParams phoneTopBar(boolean dark) {
            if (dark) {
                return new LiquidGlassParams(
                        7.5f, 0.85f, 0.60f, 1.0f, 0.44f,
                        0x30000000,
                        0xd4111620, 0xda0c1018, 0x48ffffff,
                        true, true, true);
            }
            return new LiquidGlassParams(
                    7.5f, 0.85f, 0.60f, 0.0f, 0.26f,
                    0x16000000,
                    0xdcffffff, 0xcef2f6fc, 0xa8ffffff,
                    true, true, true);
        }
    }

    /** Multi-bloom gallery backdrop (Lens pastel cyan/blue/peach in Light; deep sapphire/teal/twilight in Dark). */
    static final class LiquidAuroraBackdropDrawable extends Drawable {
        private final boolean dark;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int cachedWidth = -1;
        private int cachedHeight = -1;
        private Shader baseShader;
        private Shader bloomA;
        private Shader bloomB;
        private Shader bloomC;

        LiquidAuroraBackdropDrawable(boolean dark) {
            this.dark = dark;
        }

        private void ensureShaders(int w, int h) {
            if (w <= 0 || h <= 0) return;
            if (w == cachedWidth && h == cachedHeight && baseShader != null) return;
            cachedWidth = w;
            cachedHeight = h;
            float maxDim = Math.max(w, h);
            if (dark) {
                baseShader = new LinearGradient(0f, 0f, w, h,
                        new int[]{0xff0b0f17, 0xff090c12, 0xff0d121c},
                        new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
                bloomA = new RadialGradient(w * 0.16f, h * 0.14f, maxDim * 0.58f,
                        new int[]{0x582e6de0, 0x1e204da6, 0x00090c12},
                        new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
                bloomB = new RadialGradient(w * 0.88f, h * 0.42f, maxDim * 0.54f,
                        new int[]{0x4a1692ad, 0x180e6275, 0x00090c12},
                        new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
                bloomC = new RadialGradient(w * 0.24f, h * 0.84f, maxDim * 0.52f,
                        new int[]{0x425842c0, 0x163d2c8d, 0x00090c12},
                        new float[]{0f, 0.58f, 1f}, Shader.TileMode.CLAMP);
            } else {
                baseShader = new LinearGradient(0f, 0f, w, h,
                        new int[]{0xfff7f9fc, 0xfff5f7fa, 0xffeef2f8},
                        new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
                bloomA = new RadialGradient(w * 0.14f, h * 0.12f, maxDim * 0.58f,
                        new int[]{0x4a64c8f5, 0x1c7cd3f8, 0x00f5f7fa},
                        new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
                bloomB = new RadialGradient(w * 0.88f, h * 0.44f, maxDim * 0.55f,
                        new int[]{0x3e7ea8f8, 0x1896b8fa, 0x00f5f7fa},
                        new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
                bloomC = new RadialGradient(w * 0.22f, h * 0.84f, maxDim * 0.52f,
                        new int[]{0x38f6b99e, 0x14f8ccb8, 0x00f5f7fa},
                        new float[]{0f, 0.58f, 1f}, Shader.TileMode.CLAMP);
            }
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            int w = bounds.width();
            int h = bounds.height();
            if (w <= 0 || h <= 0) return;
            ensureShaders(w, h);
            canvas.save();
            canvas.translate(bounds.left, bounds.top);
            paint.setShader(baseShader);
            canvas.drawRect(0f, 0f, w, h, paint);
            paint.setShader(bloomA);
            canvas.drawRect(0f, 0f, w, h, paint);
            paint.setShader(bloomB);
            canvas.drawRect(0f, 0f, w, h, paint);
            paint.setShader(bloomC);
            canvas.drawRect(0f, 0f, w, h, paint);
            paint.setShader(null);
            canvas.restore();
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter colorFilter) {}
        @Override @SuppressWarnings("deprecation") public int getOpacity() { return PixelFormat.OPAQUE; }
    }

    /** Translucent optical surface with narrow Lens-style directional rim specular for Phone UI controls. */
    static final class LiquidGlassSurfaceDrawable extends Drawable {
        private final boolean dark;
        private final float radiusPx;
        private final int topColor;
        private final int bottomColor;
        private final int outerStrokeColor;
        private final int specularColor;
        private final float strokeWidthPx;
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint specularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Path clipPath = new Path();
        private Object specularHolder;

        LiquidGlassSurfaceDrawable(
                boolean dark,
                float radiusPx,
                int topColor,
                int bottomColor,
                int outerStrokeColor,
                int specularColor,
                float strokeWidthPx) {
            this.dark = dark;
            this.radiusPx = radiusPx;
            this.topColor = topColor;
            this.bottomColor = bottomColor;
            this.outerStrokeColor = outerStrokeColor;
            this.specularColor = specularColor;
            this.strokeWidthPx = Math.max(1f, strokeWidthPx);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float w = b.width();
            float h = b.height();
            if (w <= 1f || h <= 1f) return;
            float rx = Math.min(radiusPx, Math.min(w, h) * 0.5f);
            canvas.save();
            canvas.translate(b.left, b.top);
            rect.set(0f, 0f, w, h);

            if (topColor == bottomColor) {
                fillPaint.setShader(null);
                fillPaint.setColor(topColor);
            } else {
                fillPaint.setColor(0xffffffff);
                fillPaint.setShader(new LinearGradient(0f, 0f, 0f, h,
                        topColor, bottomColor, Shader.TileMode.CLAMP));
            }
            canvas.drawRoundRect(rect, rx, rx, fillPaint);
            fillPaint.setShader(null);

            if ((specularColor >>> 24) != 0) {
                if (isOpticalSupported()) {
                    if (specularHolder == null) specularHolder = Api33.createSpecularHolder();
                    Api33.drawSpecularRim(specularHolder, canvas, rect, uniformShaderRadii(rx),
                            specularPaint, strokeWidthPx * 1.35f, specularColor);
                } else {
                    clipPath.reset();
                    clipPath.addRoundRect(rect, rx, rx, Path.Direction.CW);
                    canvas.save();
                    canvas.clipPath(clipPath);
                    rimPaint.setStyle(Paint.Style.STROKE);
                    rimPaint.setStrokeWidth(strokeWidthPx * 1.25f);
                    rimPaint.setShader(new LinearGradient(0f, 0f, w * 0.65f, h,
                            specularColor, dark ? 0x16ffffff : 0x28ffffff, Shader.TileMode.CLAMP));
                    canvas.drawRoundRect(rect, rx, rx, rimPaint);
                    rimPaint.setShader(null);
                    canvas.restore();
                }
            }

            if ((outerStrokeColor >>> 24) != 0) {
                rimPaint.setShader(null);
                rimPaint.setStyle(Paint.Style.STROKE);
                rimPaint.setStrokeWidth(Math.max(1f, strokeWidthPx * 0.6f));
                rimPaint.setColor(outerStrokeColor);
                float halfStroke = rimPaint.getStrokeWidth() * 0.5f;
                rect.inset(halfStroke, halfStroke);
                canvas.drawRoundRect(rect, Math.max(0f, rx - halfStroke), Math.max(0f, rx - halfStroke), rimPaint);
            }
            canvas.restore();
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter colorFilter) {}
        @Override @SuppressWarnings("deprecation") public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** Optical Liquid Glass renderer for External Display Canvas panels and clear glass slider thumbs. */
    static final class CanvasGlassNode {
        private final String nodeName;
        private Object api33State;
        private final Paint fallbackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint specularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path clipPath = new Path();
        private final RectF localRect = new RectF();
        private final RectF fallbackRect = new RectF();

        CanvasGlassNode(String nodeName) {
            this.nodeName = nodeName;
        }

        void draw(
                Canvas canvas,
                Bitmap backdrop,
                RectF backdropBounds,
                float left,
                float top,
                float right,
                float bottom,
                float[] shaderRadii4,
                LiquidGlassParams params,
                float densityScale,
                float pointerX,
                float pointerY,
                boolean hasPointer) {
            float w = right - left;
            float h = bottom - top;
            if (w <= 1f || h <= 1f) return;

            float[] radii8 = new float[]{
                    shaderRadii4[0], shaderRadii4[0],
                    shaderRadii4[1], shaderRadii4[1],
                    shaderRadii4[2], shaderRadii4[2],
                    shaderRadii4[3], shaderRadii4[3]
            };

            boolean drewOptical = false;
            if (isOpticalSupported() && canvas.isHardwareAccelerated()
                    && backdrop != null && !backdrop.isRecycled() && backdropBounds != null) {
                if (api33State == null) api33State = Api33.createGlassNodeState(nodeName);
                drewOptical = Api33.drawCanvasBackdropNode(
                        api33State, canvas, backdrop, backdropBounds,
                        left, top, right, bottom, radii8, shaderRadii4, params, densityScale,
                        pointerX - left, pointerY - top, hasPointer);
            }

            if (!drewOptical) {
                fallbackRect.set(left, top, right, bottom);
                fallbackPaint.setStyle(Paint.Style.FILL);
                fallbackPaint.setColor(0xffffffff);
                fallbackPaint.setShader(new LinearGradient(0f, top, 0f, bottom,
                        params.fallbackTopColor, params.fallbackBottomColor, Shader.TileMode.CLAMP));
                clipPath.reset();
                clipPath.addRoundRect(fallbackRect, radii8, Path.Direction.CW);
                canvas.drawPath(clipPath, fallbackPaint);
                fallbackPaint.setShader(null);
                drawSpecularOverlay(canvas, left, top, right, bottom, radii8, shaderRadii4,
                        params.fallbackRimColor, Math.max(1f, 1.15f * densityScale),
                        params.outerBorderColor, Math.max(1f, 0.7f * densityScale));
            } else if ((params.outerBorderColor >>> 24) != 0) {
                drawOuterHairlineBorder(canvas, left, top, right, bottom, radii8,
                        params.outerBorderColor, Math.max(1f, 0.7f * densityScale));
            }
        }

        void drawSpecularOverlay(
                Canvas canvas,
                float left,
                float top,
                float right,
                float bottom,
                float[] radii8,
                float[] shaderRadii4,
                int specularColor,
                float specularStrokePx,
                int outerBorderColor,
                float outerStrokePx) {
            float w = right - left;
            float h = bottom - top;
            if (w <= 1f || h <= 1f) return;

            if ((specularColor >>> 24) != 0) {
                if (isOpticalSupported()) {
                    if (api33State == null) api33State = Api33.createGlassNodeState(nodeName);
                    canvas.save();
                    canvas.translate(left, top);
                    localRect.set(0f, 0f, w, h);
                    Api33.drawSpecularRimFromState(api33State, canvas, localRect, radii8, shaderRadii4,
                            specularPaint, specularStrokePx, specularColor);
                    canvas.restore();
                } else {
                    clipPath.reset();
                    localRect.set(left, top, right, bottom);
                    clipPath.addRoundRect(localRect, radii8, Path.Direction.CW);
                    canvas.save();
                    canvas.clipPath(clipPath);
                    borderPaint.setStyle(Paint.Style.STROKE);
                    borderPaint.setStrokeWidth(Math.max(1f, specularStrokePx));
                    borderPaint.setShader(new LinearGradient(left, top, right * 0.7f + left * 0.3f, bottom,
                            specularColor, (specularColor & 0x00ffffff) | 0x22000000, Shader.TileMode.CLAMP));
                    canvas.drawPath(clipPath, borderPaint);
                    borderPaint.setShader(null);
                    canvas.restore();
                }
            }

            if ((outerBorderColor >>> 24) != 0) {
                drawOuterHairlineBorder(canvas, left, top, right, bottom, radii8, outerBorderColor, outerStrokePx);
            }
        }

        private void drawOuterHairlineBorder(
                Canvas canvas,
                float left,
                float top,
                float right,
                float bottom,
                float[] radii8,
                int outerBorderColor,
                float outerStrokePx) {
            float half = outerStrokePx * 0.5f;
            localRect.set(left + half, top + half, right - half, bottom - half);
            float[] insetRadii = new float[]{
                    Math.max(0f, radii8[0] - half), Math.max(0f, radii8[1] - half),
                    Math.max(0f, radii8[2] - half), Math.max(0f, radii8[3] - half),
                    Math.max(0f, radii8[4] - half), Math.max(0f, radii8[5] - half),
                    Math.max(0f, radii8[6] - half), Math.max(0f, radii8[7] - half)
            };
            clipPath.reset();
            clipPath.addRoundRect(localRect, insetRadii, Path.Direction.CW);
            borderPaint.setShader(null);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(outerStrokePx);
            borderPaint.setColor(outerBorderColor);
            canvas.drawPath(clipPath, borderPaint);
        }
    }

    static void drawCanvasSpecularOverlay(
            CanvasGlassNode helperNode,
            Canvas canvas,
            float left,
            float top,
            float right,
            float bottom,
            float rx,
            int specularColor,
            int borderColor,
            float densityScale) {
        helperNode.drawSpecularOverlay(
                canvas, left, top, right, bottom,
                uniformRadii(rx), uniformShaderRadii(rx),
                specularColor, Math.max(1.1f, 1.25f * densityScale),
                borderColor, Math.max(1f, 0.7f * densityScale));
    }

    /**
     * Single-body physical water-droplet lens thumb renderer (C-infinity dome, IOR 1.333 track
     * magnification & dispersion, overhead-left crown highlight, lower-right caustic bounce,
     * and smooth analytical drop shadow — zero nested bevel rings or hard outlines).
     */
    static final class DropletThumbRenderer {
        private Object shaderHolder;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        void draw(
                Canvas canvas,
                float thumbCx,
                float centerY,
                float kHalfW,
                float kHalfH,
                float trackLeft,
                float trackRight,
                float trackHalfH,
                boolean hasActiveFill,
                float fillEndX,
                boolean darkMode,
                boolean active,
                float densityScale) {
            if (kHalfW <= 1f || kHalfH <= 1f) return;
            float scale = Math.max(0.5f, densityScale);
            if (isOpticalSupported() && canvas.isHardwareAccelerated()) {
                if (shaderHolder == null) shaderHolder = Api33.createDropletHolder();
                if (Api33.drawDropletThumb(
                        shaderHolder, canvas, paint,
                        thumbCx, centerY, kHalfW, kHalfH,
                        trackLeft, trackRight, trackHalfH,
                        hasActiveFill, fillEndX, darkMode, active, scale)) {
                    return;
                }
            }

            // API 29-32 translucent water-droplet fallback
            float radius = kHalfH;
            rect.set(thumbCx - kHalfW - scale * 0.4f, centerY - kHalfH + scale * 1.4f,
                    thumbCx + kHalfW + scale * 0.4f, centerY + kHalfH + scale * 1.4f);
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(darkMode ? 0x36000000 : 0x18081426);
            canvas.drawRoundRect(rect, radius, radius, paint);

            rect.set(thumbCx - kHalfW, centerY - kHalfH, thumbCx + kHalfW, centerY + kHalfH);
            paint.setColor(0xffffffff);
            paint.setShader(new LinearGradient(rect.left, rect.top, rect.right * 0.35f + rect.left * 0.65f, rect.bottom,
                    darkMode ? 0x628098b8 : 0x72ffffff,
                    darkMode ? 0x36364860 : 0x42eaf2fc,
                    Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setShader(null);

            if (hasActiveFill && fillEndX > trackLeft) {
                paint.setShader(new RadialGradient(
                        thumbCx - kHalfW * 0.1f, centerY, Math.max(radius, 4f),
                        darkMode ? 0x664c96ff : 0x662875ed, 0x002875ed, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(rect, radius, radius, paint);
                paint.setShader(null);
            }

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, scale * 0.85f));
            paint.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                    darkMode ? 0x88ffffff : 0xd8ffffff,
                    darkMode ? 0x32ffffff : 0x48a8b8ce,
                    Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    /**
     * Sibling live backdrop blur + Lens optical refraction View for floating Top Bars on Phone UI.
     */
    static final class LiquidGlassBackdropView extends View {
        private View backdropSource;
        private View attachedSource;
        private ViewTreeObserver attachedVto;
        private boolean glassEnabled;
        private boolean darkMode;
        private boolean recording;
        private Object api33State;
        private final Paint fallbackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ViewTreeObserver.OnScrollChangedListener scrollListener = this::invalidateBackdrop;
        private final View.OnLayoutChangeListener sourceLayoutListener =
                (v, l, t, r, b, ol, ot, or2, ob) -> invalidateBackdrop();

        LiquidGlassBackdropView(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        void bindBackdropSource(View source) {
            if (this.backdropSource == source) return;
            detachListeners();
            this.backdropSource = source;
            syncListeners();
        }

        void setGlassStyle(boolean enabled, boolean dark) {
            this.glassEnabled = enabled;
            this.darkMode = dark;
            setVisibility(enabled ? VISIBLE : GONE);
            syncListeners();
            if (enabled) invalidateBackdrop();
        }

        void invalidateBackdrop() {
            if (glassEnabled && getVisibility() == VISIBLE) {
                invalidate();
            }
        }

        private void detachListeners() {
            if (attachedVto != null && attachedVto.isAlive()) {
                attachedVto.removeOnScrollChangedListener(scrollListener);
            }
            attachedVto = null;
            if (attachedSource != null) {
                attachedSource.removeOnLayoutChangeListener(sourceLayoutListener);
            }
            attachedSource = null;
        }

        private void syncListeners() {
            boolean shouldAttach = glassEnabled && backdropSource != null && isAttachedToWindow();
            if (!shouldAttach) {
                detachListeners();
                return;
            }
            if (attachedSource != backdropSource) {
                detachListeners();
                attachedSource = backdropSource;
                attachedSource.addOnLayoutChangeListener(sourceLayoutListener);
                ViewTreeObserver vto = getViewTreeObserver();
                if (vto.isAlive()) {
                    vto.addOnScrollChangedListener(scrollListener);
                    attachedVto = vto;
                }
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            syncListeners();
        }

        @Override
        protected void onDetachedFromWindow() {
            detachListeners();
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (!glassEnabled) return;
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float density = getResources().getDisplayMetrics().density;
            LiquidGlassParams params = LiquidGlassParams.phoneTopBar(darkMode);
            canvas.save();
            canvas.clipRect(0f, 0f, w, h);
            boolean drewOptical = false;
            if (!recording && isOpticalSupported() && canvas.isHardwareAccelerated()
                    && backdropSource != null && backdropSource.getWidth() > 0 && backdropSource.getHeight() > 0) {
                if (api33State == null) api33State = Api33.createGlassNodeState("boxscreen-topbar");
                recording = true;
                try {
                    drewOptical = Api33.drawSiblingBackdropNode(
                            api33State, canvas, this, backdropSource, w, h, params, density);
                } finally {
                    recording = false;
                }
            }

            if (!drewOptical) {
                fallbackPaint.setShader(new LinearGradient(0f, 0f, 0f, h,
                        params.fallbackTopColor, params.fallbackBottomColor, Shader.TileMode.CLAMP));
                canvas.drawRect(0f, 0f, w, h, fallbackPaint);
                fallbackPaint.setShader(null);
            }

            // Whisper-thin hairline edge anchor below floating header
            float lineH = Math.max(1f, density * 0.75f);
            edgePaint.setColor(params.outerBorderColor);
            canvas.drawRect(0f, h - lineH, w, h, edgePaint);
            canvas.restore();
        }
    }

    /**
     * Lens optical Liquid Glass card container for Phone UI (Home & Settings).
     */
    static final class LiquidGlassCardLayout extends LinearLayout {
        private boolean glassEnabled;
        private boolean darkMode;
        private float cornerRadiusDp = 15f;
        private Object specularState;
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint specularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path outlinePath = new Path();
        private final RectF cardRect = new RectF();
        private final RectF localRect = new RectF();
        private final ViewOutlineProvider cardOutlineProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                float rx = cornerRadiusDp * view.getResources().getDisplayMetrics().density;
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), rx);
                outline.setAlpha(0.85f);
            }
        };

        LiquidGlassCardLayout(Context context) {
            super(context);
        }

        void configureGlass(boolean enabled, boolean dark, float radiusDp) {
            this.glassEnabled = enabled;
            this.darkMode = dark;
            this.cornerRadiusDp = radiusDp;
            float density = getResources().getDisplayMetrics().density;
            if (enabled) {
                setWillNotDraw(false);
                setBackground(null);
                setClipToOutline(false);
                setOutlineProvider(cardOutlineProvider);
                setElevation(3.5f * density);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    setOutlineAmbientShadowColor(dark ? 0x52000000 : 0x1a0a182c);
                    setOutlineSpotShadowColor(dark ? 0x62000000 : 0x200a182c);
                }
                invalidateOutline();
            } else {
                setElevation(0f);
                setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!glassEnabled) return;
            int w = getWidth();
            int h = getHeight();
            if (w <= 2 || h <= 2) return;

            float density = getResources().getDisplayMetrics().density;
            float rx = cornerRadiusDp * density;
            cardRect.set(0f, 0f, w, h);
            outlinePath.reset();
            outlinePath.addRoundRect(cardRect, rx, rx, Path.Direction.CW);

            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(0xffffffff);
            fillPaint.setShader(new LinearGradient(0f, 0f, 0f, h,
                    darkMode ? 0x841e2634 : 0xd4ffffff,
                    darkMode ? 0x72131924 : 0xb8f2f6fc,
                    Shader.TileMode.CLAMP));
            canvas.drawPath(outlinePath, fillPaint);
            fillPaint.setShader(null);

            canvas.save();
            canvas.clipPath(outlinePath);
            sheenPaint.setStyle(Paint.Style.FILL);
            sheenPaint.setColor(0xffffffff);
            sheenPaint.setShader(new LinearGradient(0f, 0f, w * 0.72f, h * 0.85f,
                    darkMode ? new int[]{0x16ffffff, 0x00ffffff, 0x08ffffff}
                            : new int[]{0x28ffffff, 0x00ffffff, 0x12ffffff},
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawRect(cardRect, sheenPaint);
            sheenPaint.setShader(null);
            canvas.restore();

            float[] radii8 = uniformRadii(rx);
            float[] radii4 = uniformShaderRadii(rx);
            int specColor = darkMode ? 0x68ffffff : 0xe8ffffff;
            if (isOpticalSupported()) {
                if (specularState == null) specularState = Api33.createGlassNodeState("boxscreen-card");
                localRect.set(0f, 0f, w, h);
                Api33.drawSpecularRimFromState(specularState, canvas, localRect, radii8, radii4,
                        specularPaint, 1.3f * density, specColor);
            } else {
                canvas.save();
                canvas.clipPath(outlinePath);
                borderPaint.setStyle(Paint.Style.STROKE);
                borderPaint.setStrokeWidth(1.2f * density);
                borderPaint.setShader(new LinearGradient(0f, 0f, w * 0.7f, h,
                        specColor, (specColor & 0x00ffffff) | 0x1c000000, Shader.TileMode.CLAMP));
                canvas.drawPath(outlinePath, borderPaint);
                borderPaint.setShader(null);
                canvas.restore();
            }

            float halfStroke = 0.4f * density;
            localRect.set(halfStroke, halfStroke, w - halfStroke, h - halfStroke);
            borderPaint.setShader(null);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(0.8f * density);
            borderPaint.setColor(darkMode ? 0x38000000 : 0x18000000);
            canvas.drawRoundRect(localRect, Math.max(0f, rx - halfStroke), Math.max(0f, rx - halfStroke), borderPaint);
        }
    }

    /** API 33+ RuntimeShader + RenderEffect implementation modeled directly on Lens optical physics. */
    @TargetApi(33)
    private static final class Api33 {
        private static final String LENS_OPTICAL_AGSL = ""
                + "uniform shader content;\n"
                + "uniform float2 size;\n"
                + "uniform float2 offset;\n"
                + "uniform float2 viewSize;\n"
                + "uniform float4 cornerRadii;\n"
                + "uniform float scale;\n"
                + "uniform float refraction;\n"
                + "uniform float dispersion;\n"
                + "uniform float darkMode;\n"
                + "uniform float tintAlpha;\n"
                + "uniform float2 pointerLocal;\n"
                + "uniform float hasPointer;\n"
                + "float radiusAt(float2 coord, float4 radii) {\n"
                + "    if (coord.x >= 0.0) {\n"
                + "        if (coord.y <= 0.0) return radii.y;\n"
                + "        else return radii.z;\n"
                + "    } else {\n"
                + "        if (coord.y <= 0.0) return radii.x;\n"
                + "        else return radii.w;\n"
                + "    }\n"
                + "}\n"
                + "float sdRoundedRect(float2 coord, float2 halfSize, float radius) {\n"
                + "    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));\n"
                + "    float outside = length(max(cornerCoord, 0.0)) - radius;\n"
                + "    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);\n"
                + "    return outside + inside;\n"
                + "}\n"
                + "float2 safeNormalize(float2 value, float2 fallback) {\n"
                + "    float len = length(value);\n"
                + "    if (len > 0.0001) return value / len;\n"
                + "    return fallback;\n"
                + "}\n"
                + "float3 snellRefract(float3 I, float3 N, float eta) {\n"
                + "    float NdotI = dot(N, I);\n"
                + "    float k = 1.0 - eta * eta * (1.0 - NdotI * NdotI);\n"
                + "    if (k < 0.0) return I;\n"
                + "    return eta * I - (eta * NdotI + sqrt(k)) * N;\n"
                + "}\n"
                + "half4 main(float2 coord) {\n"
                + "    float2 halfSize = max(size * 0.5, float2(1.0));\n"
                + "    float2 centeredCoord = (coord + offset) - halfSize;\n"
                + "    float radius = clamp(radiusAt(centeredCoord, cornerRadii), 0.0, min(halfSize.x, halfSize.y));\n"
                + "    float sd = sdRoundedRect(centeredCoord, halfSize, radius);\n"
                + "    float eps = max(0.7 * scale, 0.5);\n"
                + "    float sdX1 = sdRoundedRect(centeredCoord + float2(eps, 0.0), halfSize, clamp(radiusAt(centeredCoord + float2(eps, 0.0), cornerRadii), 0.0, min(halfSize.x, halfSize.y)));\n"
                + "    float sdX0 = sdRoundedRect(centeredCoord - float2(eps, 0.0), halfSize, clamp(radiusAt(centeredCoord - float2(eps, 0.0), cornerRadii), 0.0, min(halfSize.x, halfSize.y)));\n"
                + "    float sdY1 = sdRoundedRect(centeredCoord + float2(0.0, eps), halfSize, clamp(radiusAt(centeredCoord + float2(0.0, eps), cornerRadii), 0.0, min(halfSize.x, halfSize.y)));\n"
                + "    float sdY0 = sdRoundedRect(centeredCoord - float2(0.0, eps), halfSize, clamp(radiusAt(centeredCoord - float2(0.0, eps), cornerRadii), 0.0, min(halfSize.x, halfSize.y)));\n"
                + "    float2 n2 = safeNormalize(float2(sdX1 - sdX0, sdY1 - sdY0), float2(0.0, 1.0));\n"
                + "    float bevel = max(min(min(halfSize.x, halfSize.y) * 0.48, 19.0 * scale), 0.001);\n"
                + "    float edge = clamp(1.0 + sd / bevel, 0.0, 1.0);\n"
                + "    float slope = pow(edge, 1.65) * 0.94;\n"
                + "    float2 faceNormal = clamp(centeredCoord / halfSize, float2(-1.0), float2(1.0)) * 0.045 * (1.0 - edge);\n"
                + "    float2 normalXY = n2 * slope + faceNormal;\n"
                + "    float3 normal = float3(normalXY, sqrt(max(1.0 - dot(normalXY, normalXY), 0.001)));\n"
                + "    float3 ray = snellRefract(float3(0.0, 0.0, -1.0), normal, 1.0 / 1.45);\n"
                + "    float thickness = (18.0 + refraction * 57.0) * scale;\n"
                + "    float2 disp = ray.xy / max(-ray.z, 0.05) * thickness * refraction - centeredCoord * 0.016 * refraction;\n"
                + "    float2 maxUV = max(viewSize - float2(0.5), float2(1.0));\n"
                + "    float2 at = clamp(coord + disp, float2(0.5), maxUV);\n"
                + "    half3 c = content.eval(at).rgb;\n"
                + "    if (dispersion > 0.001) {\n"
                + "        float2 split = n2 * pow(edge, 2.8) * dispersion * 2.6 * scale;\n"
                + "        c.r += (content.eval(clamp(at + split, float2(0.5), maxUV)).r - c.r) * half(0.8);\n"
                + "        c.b += (content.eval(clamp(at - split, float2(0.5), maxUV)).b - c.b) * half(0.8);\n"
                + "    }\n"
                + "    half luma = dot(c, half3(0.2126, 0.7152, 0.0722));\n"
                + "    c = mix(half3(luma), c, half(1.12));\n"
                + "    half3 tint = mix(half3(0.97, 0.985, 1.0), half3(0.07, 0.10, 0.15), half(darkMode));\n"
                + "    float localTint;\n"
                + "    if (darkMode < 0.5) {\n"
                + "        localTint = mix(tintAlpha, min(tintAlpha + 0.44, 0.72), smoothstep(0.55, 0.08, float(luma)));\n"
                + "    } else {\n"
                + "        localTint = mix(min(tintAlpha, 0.30), max(tintAlpha, 0.60), smoothstep(0.38, 0.86, float(luma)));\n"
                + "    }\n"
                + "    c = mix(c, tint, half(localTint));\n"
                + "    float fresnel = 0.04 + 0.96 * pow(1.0 - normal.z, 5.0);\n"
                + "    half3 env = content.eval(clamp(coord + n2 * 34.0 * scale, float2(0.5), maxUV)).rgb;\n"
                + "    c = mix(c, env, half(fresnel * 0.14));\n"
                + "    float2 baseLight = float2(-0.47, -0.88);\n"
                + "    float2 light = (hasPointer > 0.5)\n"
                + "        ? safeNormalize(baseLight + clamp((pointerLocal - halfSize) / max(halfSize, float2(1.0)), float2(-1.0), float2(1.0)) * 0.14, baseLight)\n"
                + "        : baseLight;\n"
                + "    float directional = pow(max(dot(n2, light), 0.0), 7.0);\n"
                + "    float opposite = pow(max(dot(n2, -light), 0.0), 7.0) * 0.52;\n"
                + "    float outline = 1.0 - smoothstep(0.12 * scale, 1.15 * scale, abs(sd + 0.65 * scale));\n"
                + "    float edgeLight = (directional + opposite) * pow(edge, 4.0);\n"
                + "    float spec = edgeLight * 0.075 + outline * (0.02 + directional * 0.38 + opposite * 0.16) + fresnel * 0.04;\n"
                + "    c += mix(half3(1.0), env, half(0.18)) * half(spec);\n"
                + "    float caustic = exp(-pow((sd + 3.0 * scale) / (1.65 * scale), 2.0));\n"
                + "    c += half3(caustic * opposite * 0.045);\n"
                + "    float2 face = centeredCoord / halfSize;\n"
                + "    float broadRefl = exp(-pow((face.x * 0.55 - face.y * 0.8 - light.x * 0.4) * 1.6, 2.0));\n"
                + "    half3 ambient = content.eval(clamp(float2(viewSize.x * 0.25, viewSize.y * 0.25), float2(0.5), maxUV)).rgb;\n"
                + "    c = mix(c, clamp(ambient + half3(0.14), half3(0.0), half3(1.0)), half(broadRefl * 0.05));\n"
                + "    if (hasPointer > 0.5) {\n"
                + "        float glint = exp(-length((coord - pointerLocal) / (float2(180.0, 150.0) * scale)) * 2.0);\n"
                + "        c += half3(glint * 0.028);\n"
                + "    }\n"
                + "    c -= half3(pow(edge, 8.0) * max(dot(n2, float2(0.45, 0.89)), 0.0) * 0.085);\n"
                + "    return half4(clamp(c, half3(0.0), half3(1.0)), 1.0);\n"
                + "}\n";

        /** Narrow directional rim specular + opposite secondary caustic matching Lens optics (no uniform white outline). */
        private static final String SPECULAR_HIGHLIGHT_AGSL = ""
                + "uniform float2 size;\n"
                + "uniform float4 cornerRadii;\n"
                + "layout(color) uniform half4 color;\n"
                + "float radiusAt(float2 coord, float4 radii) {\n"
                + "    if (coord.x >= 0.0) {\n"
                + "        if (coord.y <= 0.0) return radii.y;\n"
                + "        else return radii.z;\n"
                + "    } else {\n"
                + "        if (coord.y <= 0.0) return radii.x;\n"
                + "        else return radii.w;\n"
                + "    }\n"
                + "}\n"
                + "float safeSign(float value) {\n"
                + "    return value < 0.0 ? -1.0 : 1.0;\n"
                + "}\n"
                + "float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {\n"
                + "    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));\n"
                + "    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {\n"
                + "        float2 outside = max(cornerCoord, 0.0);\n"
                + "        float outsideLength = length(outside);\n"
                + "        if (outsideLength > 0.001) {\n"
                + "            return float2(safeSign(coord.x), safeSign(coord.y)) * (outside / outsideLength);\n"
                + "        }\n"
                + "        float useX = step(cornerCoord.y, cornerCoord.x);\n"
                + "        return float2(useX * safeSign(coord.x), (1.0 - useX) * safeSign(coord.y));\n"
                + "    } else {\n"
                + "        float gradX = step(cornerCoord.y, cornerCoord.x);\n"
                + "        return float2(safeSign(coord.x), safeSign(coord.y)) * float2(gradX, 1.0 - gradX);\n"
                + "    }\n"
                + "}\n"
                + "half4 main(float2 coord) {\n"
                + "    float2 halfSize = max(size * 0.5, float2(1.0));\n"
                + "    float2 centeredCoord = coord - halfSize;\n"
                + "    float radius = clamp(radiusAt(centeredCoord, cornerRadii), 0.0, min(halfSize.x, halfSize.y));\n"
                + "    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));\n"
                + "    float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);\n"
                + "    float2 light = float2(-0.47, -0.88);\n"
                + "    float directional = pow(max(dot(grad, light), 0.0), 7.0);\n"
                + "    float opposite = pow(max(dot(grad, -light), 0.0), 7.0) * 0.52;\n"
                + "    float intensity = clamp(0.06 + directional * 0.94 + opposite * 0.45, 0.0, 1.0);\n"
                + "    return color * intensity;\n"
                + "}\n";

        /**
         * Single-body optical water-droplet capsule shader: C-infinity curved dome, Snell IOR 1.333
         * magnification & rim chromatic dispersion of the slider track underneath, overhead-left crown
         * specular + opposite caustic bounce, and feathery exterior contact/ambient shadow.
         */
        private static final String DROPLET_THUMB_AGSL = ""
                + "uniform float2 halfSize;\n"
                + "uniform float2 center;\n"
                + "uniform float4 trackBounds;\n"
                + "uniform float fillEndX;\n"
                + "uniform float hasActiveFill;\n"
                + "uniform float darkMode;\n"
                + "uniform float active;\n"
                + "uniform float scale;\n"
                + "float3 snellRefract(float3 I, float3 N, float eta) {\n"
                + "    float NdotI = dot(N, I);\n"
                + "    float k = 1.0 - eta * eta * (1.0 - NdotI * NdotI);\n"
                + "    if (k < 0.0) return I;\n"
                + "    return eta * I - (eta * NdotI + sqrt(k)) * N;\n"
                + "}\n"
                + "float trackCov(float2 q, float4 tb, float radiusT, float s) {\n"
                + "    float2 tc = float2(clamp(q.x, tb.x + radiusT, tb.z - radiusT), 0.0);\n"
                + "    return smoothstep(0.65 * s, -0.65 * s, length(q - tc) - radiusT);\n"
                + "}\n"
                + "half4 main(float2 coord) {\n"
                + "    float2 p = coord - center;\n"
                + "    float radius = max(halfSize.y, 1.0);\n"
                + "    float spineX = max(halfSize.x - radius, 0.0);\n"
                + "    float2 delta = p - float2(clamp(p.x, -spineX, spineX), 0.0);\n"
                + "    float distSpine = length(delta);\n"
                + "    float sd = distSpine - radius;\n"
                + "    float2 radial = (distSpine > 0.0001) ? (delta / max(radius, 0.001)) : float2(0.0, 0.0);\n"
                + "    float r = clamp(distSpine / radius, 0.0, 1.0);\n"
                + "    float h = sqrt(max(1.0 - r * r, 0.0));\n"
                + "    float2 deltaSh = (p - float2(0.0, 1.55 * scale)) - float2(clamp(p.x, -spineX, spineX), 0.0);\n"
                + "    float sdSh = length(deltaSh) - radius;\n"
                + "    float sh = clamp(\n"
                + "        exp(-max(sdSh, 0.0) / (1.25 * scale)) * (darkMode > 0.5 ? 0.32 : 0.15)\n"
                + "        + exp(-max(sdSh, 0.0) / (3.8 * scale)) * (darkMode > 0.5 ? 0.20 : 0.08),\n"
                + "        0.0, 0.50) * smoothstep(5.8 * scale, 1.8 * scale, max(sdSh, 0.0));\n"
                + "    half4 shadowPremul = half4(half(0.02 * sh), half(0.05 * sh), half(0.12 * sh), half(sh));\n"
                + "    float2 nXY = radial * 0.82;\n"
                + "    float3 N = float3(nXY, sqrt(max(1.0 - dot(nXY, nXY), 0.04)));\n"
                + "    float3 ray = snellRefract(float3(0.0, 0.0, -1.0), N, 0.75);\n"
                + "    float heightAboveTrack = radius * (0.35 + 0.75 * h);\n"
                + "    float2 refractedP = p + (ray.xy / max(-ray.z, 0.25)) * heightAboveTrack;\n"
                + "    float radiusT = max((trackBounds.w - trackBounds.y) * 0.5, 0.5);\n"
                + "    float2 split = radial * pow(r, 2.4) * 0.75 * scale;\n"
                + "    float covR = trackCov(refractedP + split, trackBounds, radiusT, scale);\n"
                + "    float covG = trackCov(refractedP, trackBounds, radiusT, scale);\n"
                + "    float covB = trackCov(refractedP - split, trackBounds, radiusT, scale);\n"
                + "    float activeMask = (hasActiveFill > 0.5) ? smoothstep(fillEndX + 0.65 * scale, fillEndX - 0.65 * scale, refractedP.x) : 0.0;\n"
                + "    float3 inactiveRgb = mix(float3(0.62, 0.68, 0.76), float3(0.75, 0.81, 0.89), darkMode);\n"
                + "    float3 activeRgb = mix(float3(0.18, 0.50, 0.96), float3(0.32, 0.62, 1.0), darkMode);\n"
                + "    float3 trackRgb = mix(inactiveRgb, activeRgb, activeMask);\n"
                + "    float trackOpacity = mix(mix(0.36, 0.26, darkMode), 0.82, activeMask);\n"
                + "    float3 trackLayer = float3(trackRgb.r * covR, trackRgb.g * covG, trackRgb.b * covB) * trackOpacity;\n"
                + "    float trackA = clamp(max(max(covR, covG), covB) * trackOpacity, 0.0, 1.0);\n"
                + "    half3 veilRgb = mix(half3(0.98, 0.99, 1.0), half3(0.84, 0.91, 1.0), half(darkMode));\n"
                + "    float veilA = mix(mix(0.30, 0.18, active), mix(0.22, 0.14, active), darkMode);\n"
                + "    half3 premulRgb = half3(trackLayer * (1.0 - veilA)) + veilRgb * half(veilA);\n"
                + "    float alpha = trackA * (1.0 - veilA) + veilA;\n"
                + "    float3 L = normalize(float3(-0.34, -0.72, 0.60));\n"
                + "    float3 H = normalize(L + float3(0.0, 0.0, 1.0));\n"
                + "    float crownSpec = pow(max(dot(N, H), 0.0), 40.0) * 0.88;\n"
                + "    float domeSheen = pow(max(dot(N, H), 0.0), 7.5) * 0.18;\n"
                + "    float3 Lb = normalize(float3(0.28, 0.76, 0.58));\n"
                + "    float bounce = pow(max(dot(N, Lb), 0.0), 7.5) * pow(r, 1.5) * (darkMode > 0.5 ? 0.52 : 0.44);\n"
                + "    float fresnel = pow(1.0 - N.z, 2.8);\n"
                + "    float fresnelDir = fresnel * (\n"
                + "        0.28\n"
                + "        + 0.48 * pow(max(dot(radial, float2(-0.42, -0.90)), 0.0), 2.5)\n"
                + "        + 0.34 * pow(max(dot(radial, float2(0.38, 0.92)), 0.0), 2.5));\n"
                + "    float whiteGlow = crownSpec + domeSheen + bounce + fresnelDir * (darkMode > 0.5 ? 0.52 : 0.68);\n"
                + "    premulRgb += half3(whiteGlow);\n"
                + "    alpha = clamp(alpha + whiteGlow, 0.0, 1.0);\n"
                + "    float meniscusShade = smoothstep(0.86, 0.99, r) * max(radial.y, 0.0) * (darkMode > 0.5 ? 0.06 : 0.11);\n"
                + "    premulRgb *= half(1.0 - meniscusShade);\n"
                + "    alpha = clamp(alpha + meniscusShade * 0.45, 0.0, 1.0);\n"
                + "    premulRgb = min(premulRgb, half3(alpha));\n"
                + "    float insideAlpha = smoothstep(0.65, -0.65, sd);\n"
                + "    half4 dropletPremul = half4(clamp(premulRgb, half3(0.0), half3(1.0)), half(alpha));\n"
                + "    return mix(shadowPremul, dropletPremul, half(insideAlpha));\n"
                + "}\n";

        static final class GlassNodeState {
            final RenderNode node;
            RuntimeShader lensShader;
            RuntimeShader specularShader;
            final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            final Path clipPath = new Path();
            final RectF rect = new RectF();
            final int[] srcLoc = new int[2];
            final int[] hostLoc = new int[2];
            RenderEffect cachedBlurEffect;
            float cachedBlurPx = -1f;
            long lastBlurCreatedAt = 0L;

            GlassNodeState(String name) {
                this.node = new RenderNode(name);
                this.node.setClipToBounds(true);
            }
        }

        static Object createGlassNodeState(String name) {
            return new GlassNodeState(name);
        }

        static Object createSpecularHolder() {
            return new RuntimeShader(SPECULAR_HIGHLIGHT_AGSL);
        }

        static Object createDropletHolder() {
            return new RuntimeShader(DROPLET_THUMB_AGSL);
        }

        static boolean drawDropletThumb(
                Object shaderObj,
                Canvas canvas,
                Paint paint,
                float thumbCx,
                float centerY,
                float kHalfW,
                float kHalfH,
                float trackLeft,
                float trackRight,
                float trackHalfH,
                boolean hasActiveFill,
                float fillEndX,
                boolean darkMode,
                boolean active,
                float scale) {
            if (!(shaderObj instanceof RuntimeShader shader)) return false;
            try {
                float pad = (float) Math.ceil(6.0f * scale);
                float rectLeft = thumbCx - kHalfW - pad;
                float rectTop = centerY - kHalfH - pad;
                float rectW = kHalfW * 2f + pad * 2f;
                float rectH = kHalfH * 2f + pad * 2f;

                shader.setFloatUniform("halfSize", kHalfW, kHalfH);
                shader.setFloatUniform("center", kHalfW + pad, kHalfH + pad);
                shader.setFloatUniform("trackBounds",
                        trackLeft - thumbCx, -trackHalfH,
                        trackRight - thumbCx, trackHalfH);
                shader.setFloatUniform("fillEndX", fillEndX - thumbCx);
                shader.setFloatUniform("hasActiveFill", hasActiveFill ? 1.0f : 0.0f);
                shader.setFloatUniform("darkMode", darkMode ? 1.0f : 0.0f);
                shader.setFloatUniform("active", active ? 1.0f : 0.0f);
                shader.setFloatUniform("scale", scale);

                canvas.save();
                canvas.translate(rectLeft, rectTop);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xffffffff);
                paint.setShader(shader);
                canvas.drawRect(0f, 0f, rectW, rectH, paint);
                paint.setShader(null);
                canvas.restore();
                return true;
            } catch (RuntimeException e) {
                Log.w("BoxScreenGlass", "drawDropletThumb failed", e);
                return false;
            }
        }

        private static RuntimeShader ensureLensShader(GlassNodeState s) {
            if (s.lensShader == null) s.lensShader = new RuntimeShader(LENS_OPTICAL_AGSL);
            return s.lensShader;
        }

        private static RuntimeShader ensureSpecularShader(GlassNodeState s) {
            if (s.specularShader == null) s.specularShader = new RuntimeShader(SPECULAR_HIGHLIGHT_AGSL);
            return s.specularShader;
        }

        private static void applyLensEffect(
                GlassNodeState s,
                int w,
                int h,
                float[] shaderRadii4,
                LiquidGlassParams params,
                float densityScale,
                float pointerLocalX,
                float pointerLocalY,
                boolean hasPointer) {
            RuntimeShader lens = ensureLensShader(s);
            float padUnit = Math.max(4f, Math.min(Math.min(w, h) * 0.45f, 19.0f * densityScale));
            float padTop = params.dockTop ? padUnit : 0f;
            float padLeft = params.dockLeft ? padUnit : 0f;
            float padRight = params.dockRight ? padUnit : 0f;

            lens.setFloatUniform("size", w + padLeft + padRight, h + padTop);
            lens.setFloatUniform("offset", padLeft, padTop);
            lens.setFloatUniform("viewSize", (float) w, (float) h);
            lens.setFloatUniform("cornerRadii",
                    shaderRadii4[0], shaderRadii4[1], shaderRadii4[2], shaderRadii4[3]);
            lens.setFloatUniform("scale", Math.max(0.5f, densityScale));
            lens.setFloatUniform("refraction", params.refraction);
            lens.setFloatUniform("dispersion", params.dispersion);
            lens.setFloatUniform("darkMode", params.darkMode);
            lens.setFloatUniform("tintAlpha", params.tintAlpha);
            lens.setFloatUniform("pointerLocal", pointerLocalX, pointerLocalY);
            lens.setFloatUniform("hasPointer", hasPointer ? 1.0f : 0.0f);

            RenderEffect lensEffect = RenderEffect.createRuntimeShaderEffect(lens, "content");
            float blurPx = Math.max(0f, params.blurDp * densityScale);
            if (blurPx > 0.5f) {
                long now = android.os.SystemClock.uptimeMillis();
                if (s.cachedBlurEffect == null || Math.abs(s.cachedBlurPx - blurPx) > 0.5f
                        || now - s.lastBlurCreatedAt > 1000L) {
                    s.cachedBlurEffect = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP);
                    s.cachedBlurPx = blurPx;
                    s.lastBlurCreatedAt = now;
                }
                s.node.setRenderEffect(RenderEffect.createChainEffect(lensEffect, s.cachedBlurEffect));
            } else {
                s.node.setRenderEffect(lensEffect);
            }
        }

        static boolean drawCanvasBackdropNode(
                Object stateObj,
                Canvas canvas,
                Bitmap backdrop,
                RectF backdropBounds,
                float left,
                float top,
                float right,
                float bottom,
                float[] radii8,
                float[] shaderRadii4,
                LiquidGlassParams params,
                float densityScale,
                float pointerLocalX,
                float pointerLocalY,
                boolean hasPointer) {
            if (!(stateObj instanceof GlassNodeState s)) return false;
            int w = Math.max(1, Math.round(right - left));
            int h = Math.max(1, Math.round(bottom - top));
            try {
                s.node.setPosition(0, 0, w, h);
                Canvas rec = s.node.beginRecording(w, h);
                try {
                    rec.translate(-left, -top);
                    rec.drawBitmap(backdrop, null, backdropBounds, s.bitmapPaint);
                } finally {
                    s.node.endRecording();
                }
                applyLensEffect(s, w, h, shaderRadii4, params, densityScale,
                        pointerLocalX, pointerLocalY, hasPointer);
                s.rect.set(left, top, right, bottom);
                s.clipPath.reset();
                s.clipPath.addRoundRect(s.rect, radii8, Path.Direction.CW);
                canvas.save();
                canvas.clipPath(s.clipPath);
                canvas.translate(left, top);
                canvas.drawRenderNode(s.node);
                canvas.restore();
                return true;
            } catch (RuntimeException e) {
                Log.w("BoxScreenGlass", "drawCanvasBackdropNode failed", e);
                return false;
            }
        }

        static boolean drawSiblingBackdropNode(
                Object stateObj,
                Canvas canvas,
                View host,
                View source,
                int w,
                int h,
                LiquidGlassParams params,
                float densityScale) {
            if (!(stateObj instanceof GlassNodeState s)) return false;
            try {
                s.node.setPosition(0, 0, w, h);
                source.getLocationInWindow(s.srcLoc);
                host.getLocationInWindow(s.hostLoc);
                Canvas rec = s.node.beginRecording(w, h);
                try {
                    rec.translate(-(s.hostLoc[0] - s.srcLoc[0]), -(s.hostLoc[1] - s.srcLoc[1]));
                    source.draw(rec);
                } finally {
                    s.node.endRecording();
                }
                applyLensEffect(s, w, h, uniformShaderRadii(0f), params, densityScale,
                        w * 0.25f, h * 0.2f, false);
                canvas.drawRenderNode(s.node);
                return true;
            } catch (RuntimeException e) {
                Log.w("BoxScreenGlass", "drawSiblingBackdropNode failed", e);
                return false;
            }
        }

        static void drawSpecularRimFromState(
                Object stateObj,
                Canvas canvas,
                RectF localBounds,
                float[] radii8,
                float[] shaderRadii4,
                Paint paint,
                float strokeWidthPx,
                int color) {
            if (!(stateObj instanceof GlassNodeState s)) return;
            try {
                RuntimeShader shader = ensureSpecularShader(s);
                drawSpecularRimWithShader(shader, s.clipPath, canvas, localBounds, radii8, shaderRadii4,
                        paint, strokeWidthPx, color);
            } catch (RuntimeException ignored) { }
        }

        static void drawSpecularRim(
                Object shaderObj,
                Canvas canvas,
                RectF localBounds,
                float[] shaderRadii4,
                Paint paint,
                float strokeWidthPx,
                int color) {
            if (!(shaderObj instanceof RuntimeShader shader)) return;
            try {
                float[] radii8 = new float[]{
                        shaderRadii4[0], shaderRadii4[0],
                        shaderRadii4[1], shaderRadii4[1],
                        shaderRadii4[2], shaderRadii4[2],
                        shaderRadii4[3], shaderRadii4[3]
                };
                drawSpecularRimWithShader(shader, new Path(), canvas, localBounds, radii8, shaderRadii4,
                        paint, strokeWidthPx, color);
            } catch (RuntimeException ignored) { }
        }

        private static void drawSpecularRimWithShader(
                RuntimeShader shader,
                Path path,
                Canvas canvas,
                RectF localBounds,
                float[] radii8,
                float[] shaderRadii4,
                Paint paint,
                float strokeWidthPx,
                int color) {
            float w = localBounds.width();
            float h = localBounds.height();
            if (w <= 1f || h <= 1f) return;
            shader.setFloatUniform("size", w, h);
            shader.setFloatUniform("cornerRadii",
                    shaderRadii4[0], shaderRadii4[1], shaderRadii4[2], shaderRadii4[3]);
            shader.setColorUniform("color", color);

            path.reset();
            path.addRoundRect(localBounds, radii8, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(path);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidthPx);
            paint.setShader(shader);
            paint.setBlendMode(BlendMode.PLUS);
            canvas.drawPath(path, paint);
            paint.setBlendMode(null);
            paint.setShader(null);
            canvas.restore();
        }
    }
}
