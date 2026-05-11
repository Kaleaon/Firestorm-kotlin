package com.firestorm.android.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay

private const val TAG = "FirestormGLSurfaceView"

private const val EGL_CONTEXT_MAJOR_VERSION = 0x3098
private const val EGL_CONTEXT_MINOR_VERSION = 0x30FB
private const val EGL_OPENGL_ES3_BIT = 0x00000040

class FirestormGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    init {
        setEGLContextFactory(Es32ContextFactory())
        setEGLConfigChooser(Es32ConfigChooser(red = 8, green = 8, blue = 8, alpha = 8, depth = 24, stencil = 8))
        setRenderer(FirestormRenderer())
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }

    private class Es32ContextFactory : EGLContextFactory {
        override fun createContext(egl: EGL10, display: EGLDisplay, config: EGLConfig): EGLContext {
            val attribs32 = intArrayOf(
                EGL_CONTEXT_MAJOR_VERSION, 3,
                EGL_CONTEXT_MINOR_VERSION, 2,
                EGL10.EGL_NONE
            )
            var ctx = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attribs32)
            if (ctx == null || ctx === EGL10.EGL_NO_CONTEXT) {
                Log.w(TAG, "ES 3.2 context unavailable (egl error 0x${egl.eglGetError().toString(16)}); falling back to ES 3.0")
                val attribs30 = intArrayOf(EGL_CONTEXT_MAJOR_VERSION, 3, EGL10.EGL_NONE)
                ctx = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attribs30)
            }
            checkNotNull(ctx) { "eglCreateContext returned null" }
            return ctx
        }

        override fun destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext) {
            if (!egl.eglDestroyContext(display, context)) {
                Log.w(TAG, "eglDestroyContext failed: 0x${egl.eglGetError().toString(16)}")
            }
        }
    }

    private class Es32ConfigChooser(
        private val red: Int,
        private val green: Int,
        private val blue: Int,
        private val alpha: Int,
        private val depth: Int,
        private val stencil: Int
    ) : EGLConfigChooser {

        private val tmp = IntArray(1)

        override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
            val spec = intArrayOf(
                EGL10.EGL_RED_SIZE, red,
                EGL10.EGL_GREEN_SIZE, green,
                EGL10.EGL_BLUE_SIZE, blue,
                EGL10.EGL_ALPHA_SIZE, alpha,
                EGL10.EGL_DEPTH_SIZE, depth,
                EGL10.EGL_STENCIL_SIZE, stencil,
                EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
                EGL10.EGL_NONE
            )

            val count = IntArray(1)
            require(egl.eglChooseConfig(display, spec, null, 0, count)) {
                "eglChooseConfig (count) failed: 0x${egl.eglGetError().toString(16)}"
            }
            require(count[0] > 0) { "No ES3-capable EGL configs match the request" }

            val configs = arrayOfNulls<EGLConfig>(count[0])
            require(egl.eglChooseConfig(display, spec, configs, count[0], count)) {
                "eglChooseConfig (fetch) failed: 0x${egl.eglGetError().toString(16)}"
            }

            return pickBestConfig(egl, display, configs)
                ?: error("No suitable EGL config among ${count[0]} candidates")
        }

        private fun pickBestConfig(egl: EGL10, display: EGLDisplay, configs: Array<EGLConfig?>): EGLConfig? {
            for (cfg in configs) {
                cfg ?: continue
                val d = attrib(egl, display, cfg, EGL10.EGL_DEPTH_SIZE)
                val s = attrib(egl, display, cfg, EGL10.EGL_STENCIL_SIZE)
                if (d < depth || s < stencil) continue
                val r = attrib(egl, display, cfg, EGL10.EGL_RED_SIZE)
                val g = attrib(egl, display, cfg, EGL10.EGL_GREEN_SIZE)
                val b = attrib(egl, display, cfg, EGL10.EGL_BLUE_SIZE)
                val a = attrib(egl, display, cfg, EGL10.EGL_ALPHA_SIZE)
                if (r == red && g == green && b == blue && a == alpha) return cfg
            }
            return configs.firstOrNull()
        }

        private fun attrib(egl: EGL10, display: EGLDisplay, config: EGLConfig, attr: Int): Int {
            return if (egl.eglGetConfigAttrib(display, config, attr, tmp)) tmp[0] else 0
        }
    }
}
