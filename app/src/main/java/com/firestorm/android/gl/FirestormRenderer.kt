package com.firestorm.android.gl

import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import android.util.Log
import com.firestorm.llrender.GLSLShader
import com.firestorm.llrender.GpuBackend
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val TAG = "FirestormRenderer"

/**
 * @param onVersionReady Called on the GL thread after the ES version is
 *   detected; the implementation should post back to the UI thread if needed.
 * @param debugBuild Pass [BuildConfig.DEBUG] to enable synchronous GL debug
 *   output. Synchronous callbacks are useful for pinpointing the exact draw
 *   call that raised an error, but they add latency on every GL message.
 */
class FirestormRenderer(
    private val onVersionReady: ((major: Int, minor: Int) -> Unit)? = null,
    private val debugBuild: Boolean = false
) : GLSurfaceView.Renderer {

    @Volatile var actualMajor: Int = 0
        private set

    @Volatile var actualMinor: Int = 0
        private set

    @Volatile var supportsEs32: Boolean = false
        private set

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        detectVersion()
        logCapabilities()
        val backend = Es32GpuBackend()
        GpuBackend.install(backend)
        // Enable GL timer-query profiling only when the EXT_disjoint_timer_query
        // extension is present; without it glBeginQuery(GL_TIME_ELAPSED) is a
        // no-op that silently produces zeroed stats every frame.
        GLSLShader.canProfile = backend.extensionSupported("GL_EXT_disjoint_timer_query")
        installDebugCallback()
        applyPipelineDefaults()
        onVersionReady?.invoke(actualMajor, actualMinor)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glScissor(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_STENCIL_BUFFER_BIT)
    }

    private fun detectVersion() {
        val major = IntArray(1)
        val minor = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, major, 0)
        GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, minor, 0)
        actualMajor = major[0]
        actualMinor = minor[0]
        supportsEs32 = actualMajor > 3 || (actualMajor == 3 && actualMinor >= 2)
    }

    private fun logCapabilities() {
        Log.i(TAG, "GL_VENDOR=${GLES20.glGetString(GLES20.GL_VENDOR)}")
        Log.i(TAG, "GL_RENDERER=${GLES20.glGetString(GLES20.GL_RENDERER)}")
        Log.i(TAG, "GL_VERSION=${GLES20.glGetString(GLES20.GL_VERSION)}")
        Log.i(TAG, "GL_SHADING_LANGUAGE_VERSION=${GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION)}")
        Log.i(TAG, "Detected ES context: $actualMajor.$actualMinor (supportsEs32=$supportsEs32)")
    }

    private fun installDebugCallback() {
        if (!supportsEs32) return
        GLES32.glEnable(GLES32.GL_DEBUG_OUTPUT)
        // GL_DEBUG_OUTPUT_SYNCHRONOUS makes the driver block until the callback
        // returns for every message — very useful for debugging but causes a
        // measurable perf hit on some drivers. Enable only in debug builds.
        if (debugBuild) {
            GLES32.glEnable(GLES32.GL_DEBUG_OUTPUT_SYNCHRONOUS)
        }
        GLES32.glDebugMessageCallback(object : GLES32.DebugProc {
            override fun onMessage(
                source: Int,
                type: Int,
                id: Int,
                severity: Int,
                message: String?
            ) {
                val level = when (severity) {
                    GLES32.GL_DEBUG_SEVERITY_HIGH -> Log.ERROR
                    GLES32.GL_DEBUG_SEVERITY_MEDIUM -> Log.WARN
                    GLES32.GL_DEBUG_SEVERITY_LOW -> Log.INFO
                    else -> Log.DEBUG
                }
                Log.println(level, TAG, "GL[src=$source type=$type id=$id]: ${message ?: ""}")
            }
        })
    }

    private fun applyPipelineDefaults() {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClearDepthf(1f)
        GLES20.glClearStencil(0)

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDepthMask(true)

        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glFrontFace(GLES20.GL_CCW)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFuncSeparate(
            GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA,
            GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA
        )

        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glPixelStorei(GLES30.GL_PACK_ALIGNMENT, 1)

        if (supportsEs32) {
            GLES32.glEnable(GLES32.GL_PRIMITIVE_RESTART_FIXED_INDEX)
        }
    }
}
