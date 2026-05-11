package com.firestorm.android

import android.content.Context
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES surface configured for ES 3.2 rendering.
 */
class FirestormGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    init {
        // Request a GLES 3.x context and prefer 8-bit RGBA + depth/stencil.
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 24, 8)
        preserveEGLContextOnPause = true
        setRenderer(FirestormRenderer())
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    private class FirestormRenderer : Renderer {
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES32.glClearColor(0.05f, 0.06f, 0.10f, 1.0f)
            GLES32.glEnable(GLES32.GL_DEPTH_TEST)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES32.glViewport(0, 0, width, height)
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT or GLES32.GL_DEPTH_BUFFER_BIT)
        }
    }
}
