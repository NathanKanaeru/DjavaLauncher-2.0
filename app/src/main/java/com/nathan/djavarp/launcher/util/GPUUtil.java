package com.nathan.djavarp.launcher.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLES20;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class GPUUtil {

    public enum GPU_TYPE {
        ADRENO,
        MALI,
        POWERVR,
        UNKNOWN
    }

    public static String getRenderer(Context context) {
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        int[] version = new int[2];
        egl.eglInitialize(display, version);

        int[] configAttribs = {
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfig = new int[1];
        egl.eglChooseConfig(display, configAttribs, configs, 1, numConfig);
        EGLConfig config = configs[0];

        EGLContext contextEgl = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, new int[]{0x3098, 2, EGL10.EGL_NONE});
        EGLSurface surface = egl.eglCreatePbufferSurface(display, config, new int[]{EGL10.EGL_WIDTH, 1, EGL10.EGL_HEIGHT, 1, EGL10.EGL_NONE});

        egl.eglMakeCurrent(display, surface, surface, contextEgl);

        String renderer = GLES20.glGetString(GLES20.GL_RENDERER);

        egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(display, surface);
        egl.eglDestroyContext(display, contextEgl);
        egl.eglTerminate(display);

        return renderer;
    }

    public static GPU_TYPE getGpuType(String renderer) {
        if (renderer == null) return GPU_TYPE.UNKNOWN;
        String r = renderer.toLowerCase();
        if (r.contains("adreno")) return GPU_TYPE.ADRENO;
        if (r.contains("mali")) return GPU_TYPE.MALI;
        if (r.contains("powervr") || r.contains("rogue")) return GPU_TYPE.POWERVR;
        return GPU_TYPE.UNKNOWN;
    }
}
