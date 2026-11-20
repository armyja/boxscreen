package com.armyja.boxscreen;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Presents the virtual display one frame later than it is produced.
 *
 * <p>The direct {@link SurfaceView} path hands the composed frame straight to the external
 * display, which composes that display last in the same SurfaceFlinger pass: whenever the
 * earlier virtual-display composition runs long, the picture misses the vsync and the display
 * repeats the previous frame. Owning the frame for one extra vsync (buffer here, present it
 * with a presentation time one vsync later) removes that dependency at the cost of ~16 ms of
 * latency, which is irrelevant for a television-style output.
 */
final class BufferedOutputView extends FrameLayout {

    interface Listener {
        /** The input surface is ready and can be handed to the virtual display. */
        void onOutputReady(BufferedOutputView view);

        /** The display surface went away and the virtual display must be detached. */
        void onOutputLost(BufferedOutputView view);
    }

    private static final String TAG = "BoxScreenBuffered";

    private final SurfaceView display;
    private final HandlerThread renderThread = new HandlerThread("boxscreen-buffered-out");
    private Handler renderHandler;
    private Listener listener;
    private volatile boolean released;

    private EGL14Holder egl;
    private SurfaceTexture inputTexture;
    private Surface inputSurface;
    private volatile int viewportWidth = 1;
    private volatile int viewportHeight = 1;
    private volatile boolean inputReady;
    private int bufferWidth;
    private int bufferHeight;

    BufferedOutputView(Context context) {
        super(context);
        display = new SurfaceView(context);
        display.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder holder) {
                startRenderer(holder.getSurface());
            }

            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                viewportWidth = width;
                viewportHeight = height;
            }

            @Override public void surfaceDestroyed(SurfaceHolder holder) {
                stopRenderer();
            }
        });
        addView(display, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setBackgroundColor(0xff000000);
        renderThread.start();
        renderHandler = new Handler(renderThread.getLooper());
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    boolean isReady() {
        return inputReady && inputSurface != null && inputSurface.isValid();
    }

    Surface inputSurface() {
        return inputSurface;
    }

    /** The surface the monitor actually shows; used for screenshots and frame capture. */
    Surface displaySurface() {
        return display.getHolder().getSurface();
    }

    void setBufferSize(int width, int height) {
        bufferWidth = width;
        bufferHeight = height;
        display.getHolder().setFixedSize(width, height);
        Handler handler = renderHandler;
        if (handler != null) handler.post(() -> {
            if (inputTexture != null && bufferWidth > 0 && bufferHeight > 0) {
                inputTexture.setDefaultBufferSize(bufferWidth, bufferHeight);
            }
        });
    }

    private void startRenderer(Surface window) {
        Handler handler = renderHandler;
        if (handler == null || released) return;
        handler.post(() -> {
            if (released) return;
            try {
                if (egl == null) egl = new EGL14Holder();
                egl.attachWindow(window);
                if (inputTexture == null) {
                    int[] textures = new int[1];
                    GLES20.glGenTextures(1, textures, 0);
                    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
                    inputTexture = new SurfaceTexture(textures[0]);
                    inputTexture.setOnFrameAvailableListener(t -> {
                        Handler h = renderHandler;
                        if (h != null) h.post(this::drawOneFrame);
                    }, renderHandler);
                    if (bufferWidth > 0 && bufferHeight > 0) {
                        inputTexture.setDefaultBufferSize(bufferWidth, bufferHeight);
                    }
                    inputSurface = new Surface(inputTexture);
                }
                inputReady = true;
                Listener current = listener;
                if (current != null) post(() -> current.onOutputReady(this));
            } catch (RuntimeException e) {
                Log.e(TAG, "could not start the buffered renderer", e);
            }
        });
    }

    private void stopRenderer() {
        Handler handler = renderHandler;
        if (handler == null) return;
        handler.post(() -> {
            inputReady = false;
            Listener current = listener;
            if (current != null) post(() -> current.onOutputLost(this));
            if (egl != null) egl.detachWindow();
        });
    }

    private void drawOneFrame() {
        EGL14Holder holder = egl;
        if (holder == null || !holder.makeCurrent() || inputTexture == null) return;
        try {
            inputTexture.updateTexImage();
            holder.draw(inputTexture, viewportWidth, viewportHeight);
            // Present with the producer's own timestamp. The frame is already one step
            // behind (the virtual display produced it, we drew it, then the display
            // composes it), so the external display always finds a finished frame waiting.
            // Deliberately adding another vsync here starves the display: when the virtual
            // display is itself late, the queue backs up and only ~1 frame in 3 is shown.
            long present = inputTexture.getTimestamp();
            EGLExt.eglPresentationTimeANDROID(holder.display, holder.surface, present);
            holder.swapBuffers();
        } catch (RuntimeException e) {
            Log.w(TAG, "frame dropped by the buffered renderer", e);
        }
    }

    void release() {
        released = true;
        Handler handler = renderHandler;
        if (handler == null) return;
        handler.post(() -> {
            inputReady = false;
            if (inputSurface != null) {
                inputSurface.release();
                inputSurface = null;
            }
            if (inputTexture != null) {
                inputTexture.release();
                inputTexture = null;
            }
            if (egl != null) {
                egl.detachWindow();
                egl.release();
                egl = null;
            }
            renderThread.quitSafely();
        });
    }

    /** Minimal EGL + GLES plumbing: one external texture drawn onto the display surface. */
    private static final class EGL14Holder {
        private static final String VERTEX_SHADER = ""
                + "attribute vec2 aPos;\n"
                + "attribute vec2 aTex;\n"
                + "uniform mat4 uMvp;\n"

                + "varying vec2 vTex;\n"
                + "void main() {\n"
                + "  gl_Position = vec4(aPos, 0.0, 1.0);\n"
                + "  vTex = (uMvp * vec4(aTex, 0.0, 1.0)).xy;\n"
                + "}\n";

        private static final String FRAGMENT_SHADER = ""
                + "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES uTex;\n"
                + "varying vec2 vTex;\n"
                + "void main() { gl_FragColor = texture2D(uTex, vTex); }\n";

        private static final float[] QUAD = {
                -1f, -1f, 0f, 0f,
                1f, -1f, 1f, 0f,
                -1f, 1f, 0f, 1f,
                1f, 1f, 1f, 1f,
        };

        android.opengl.EGLDisplay display = EGL14.EGL_NO_DISPLAY;
        android.opengl.EGLContext context = EGL14.EGL_NO_CONTEXT;
        android.opengl.EGLSurface surface = EGL14.EGL_NO_SURFACE;
        private android.opengl.EGLConfig config;
        private int program;
        private int uMvp;
        private int aPos;
        private int aTex;
        private FloatBuffer quad;
        private final float[] matrix = new float[16];

        EGL14Holder() {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] version = new int[2];
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
                throw new IllegalStateException("eglInitialize failed");
            }
            int[] attribs = {
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_NONE,
            };
            android.opengl.EGLConfig[] configs = new android.opengl.EGLConfig[1];
            int[] count = new int[1];
            if (!EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, count, 0) || count[0] == 0) {
                throw new IllegalStateException("no EGL config");
            }
            config = configs[0];
            context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT,
                    new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE}, 0);
            if (context == EGL14.EGL_NO_CONTEXT) throw new IllegalStateException("no EGL context");
            quad = ByteBuffer.allocateDirect(QUAD.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            quad.put(QUAD).position(0);
        }

        void attachWindow(Surface window) {
            detachWindow();
            surface = EGL14.eglCreateWindowSurface(display, config, window,
                    new int[]{EGL14.EGL_NONE}, 0);
            if (surface == EGL14.EGL_NO_SURFACE) throw new IllegalStateException("no EGL window");
            makeCurrent();
            ensureProgram();
        }

        void detachWindow() {
            if (surface != null && surface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT);
                EGL14.eglDestroySurface(display, surface);
            }
            surface = EGL14.EGL_NO_SURFACE;
        }

        boolean makeCurrent() {
            if (surface == null || surface == EGL14.EGL_NO_SURFACE) return false;
            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) return false;
            return true;
        }

        void swapBuffers() {
            EGL14.eglSwapBuffers(display, surface);
        }

        private void ensureProgram() {
            if (program != 0) return;
            int vs = compile(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fs = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vs);
            GLES20.glAttachShader(program, fs);
            GLES20.glLinkProgram(program);
            GLES20.glDeleteShader(vs);
            GLES20.glDeleteShader(fs);
            aPos = GLES20.glGetAttribLocation(program, "aPos");
            aTex = GLES20.glGetAttribLocation(program, "aTex");
            uMvp = GLES20.glGetUniformLocation(program, "uMvp");
        }

        private int compile(int type, String source) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            return shader;
        }

        void draw(SurfaceTexture texture, int width, int height) {
            GLES20.glViewport(0, 0, Math.max(1, width), Math.max(1, height));
            GLES20.glClearColor(0f, 0f, 0f, 1f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            if (program == 0) return;
            GLES20.glUseProgram(program);
            texture.getTransformMatrix(matrix);
            GLES20.glUniformMatrix4fv(uMvp, 1, false, matrix, 0);
            quad.position(0);
            GLES20.glEnableVertexAttribArray(aPos);
            GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 16, quad);
            quad.position(2);
            GLES20.glEnableVertexAttribArray(aTex);
            GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, 16, quad);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glDisableVertexAttribArray(aPos);
            GLES20.glDisableVertexAttribArray(aTex);
        }

        void release() {
            if (program != 0) {
                GLES20.glDeleteProgram(program);
                program = 0;
            }
            if (display != null && display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglTerminate(display);
            }
            display = EGL14.EGL_NO_DISPLAY;
            context = EGL14.EGL_NO_CONTEXT;
        }
    }
}
