package com.firestorm.llrender

/**
 * RAII-style depth-test scope that snapshots the previous depth state on
 * construction and restores it on [restore]. All actual GL state changes are
 * routed through [GpuBackend.current].
 */
class GLDepthTest(depthEnabled: Boolean, writeEnabled: Boolean = true, depthFunc: Int = GL_LEQUAL) {

    companion object {
        const val GL_LEQUAL: Int = 0x0203
        const val GL_LESS: Int = 0x0201
        const val GL_TRUE: Boolean = true
        const val GL_FALSE: Boolean = false

        private var sDepthEnabled: Boolean = false
        private var sDepthFunc: Int = GL_LESS
        private var sWriteEnabled: Boolean = true
    }

    val prevDepthEnabled: Boolean = sDepthEnabled
    val prevDepthFunc: Int = sDepthFunc
    val prevWriteEnabled: Boolean = sWriteEnabled

    init {
        sDepthEnabled = depthEnabled
        sDepthFunc = depthFunc
        sWriteEnabled = writeEnabled
        apply(depthEnabled, depthFunc, writeEnabled)
    }

    fun restore() {
        sDepthEnabled = prevDepthEnabled
        sDepthFunc = prevDepthFunc
        sWriteEnabled = prevWriteEnabled
        apply(prevDepthEnabled, prevDepthFunc, prevWriteEnabled)
    }

    fun checkState() {
        val gl = GpuBackend.current
        check(gl.isEnabled(com.firestorm.llrender.GL.DEPTH_TEST) == sDepthEnabled) {
            "GL depth test state mismatch: enabled=${gl.isEnabled(com.firestorm.llrender.GL.DEPTH_TEST)}, expected=$sDepthEnabled"
        }
    }

    private fun apply(enabled: Boolean, func: Int, writeEnabled: Boolean) {
        val gl = GpuBackend.current
        if (enabled) gl.enable(com.firestorm.llrender.GL.DEPTH_TEST) else gl.disable(com.firestorm.llrender.GL.DEPTH_TEST)
        gl.depthFunc(func)
        gl.depthMask(writeEnabled)
    }
}

class GLSDefault {
    private val blend = GLDisable(GL_BLEND)
    private val cullFace = GLDisable(GL_CULL_FACE)

    fun restore() {
        blend.restore()
        cullFace.restore()
    }

    companion object {
        const val GL_BLEND: Int = 0x0BE2
        const val GL_CULL_FACE: Int = 0x0B44
    }
}

class GLSObjectSelect {
    private val blend = GLDisable(GLSDefault.GL_BLEND)
    private val cullFace = GLEnable(GLSDefault.GL_CULL_FACE)

    fun restore() {
        blend.restore()
        cullFace.restore()
    }
}

class GLSUIDefault {
    private val blend = GLEnable(GLSDefault.GL_BLEND)
    private val cullFace = GLDisable(GLSDefault.GL_CULL_FACE)
    private val depthTest = GLDepthTest(false, writeEnabled = true, depthFunc = GLDepthTest.GL_LEQUAL)

    fun restore() {
        blend.restore()
        cullFace.restore()
        depthTest.restore()
    }
}

class GLSPipeline {
    private val cullFace = GLEnable(GLSDefault.GL_CULL_FACE)
    private val depthTest = GLDepthTest(true, writeEnabled = true, depthFunc = GLDepthTest.GL_LEQUAL)

    fun restore() {
        cullFace.restore()
        depthTest.restore()
    }
}

class GLSPipelineAlpha {
    private val blend = GLEnable(GLSDefault.GL_BLEND)

    fun restore() {
        blend.restore()
    }
}

class GLSPipelineSelection {
    private val cullFace = GLDisable(GLSDefault.GL_CULL_FACE)

    fun restore() {
        cullFace.restore()
    }
}

/**
 * Sky-box helper that "squashes" depth to the far clip so the box always sits
 * at maximum depth. Restores the prior depth range on [restore].
 */
open class GLSPipelineSkyBox {
    private val cullFace = GLDisable(GLSDefault.GL_CULL_FACE)
    private val prevNear = sNearRange
    private val prevFar = sFarRange

    init {
        sNearRange = 1.0f
        sFarRange = 1.0f
        GpuBackend.current.depthRangef(1.0f, 1.0f)
    }

    fun restore() {
        cullFace.restore()
        sNearRange = prevNear
        sFarRange = prevFar
        GpuBackend.current.depthRangef(prevNear, prevFar)
    }

    companion object {
        private var sNearRange: Float = 0.0f
        private var sFarRange: Float = 1.0f
    }
}

open class GLSPipelineDepthTestSkyBox(depthTest: Boolean, depthWrite: Boolean) : GLSPipelineSkyBox() {
    val depth = GLDepthTest(depthTest, writeEnabled = depthWrite, depthFunc = GLDepthTest.GL_LEQUAL)
}

class GLSPipelineBlendSkyBox(depthTest: Boolean, depthWrite: Boolean) : GLSPipelineDepthTestSkyBox(depthTest, depthWrite) {
    val blend = GLEnable(GLSDefault.GL_BLEND)
}

class GLSTracker {
    private val cullFace = GLEnable(GLSDefault.GL_CULL_FACE)
    private val blend = GLEnable(GLSDefault.GL_BLEND)

    fun restore() {
        cullFace.restore()
        blend.restore()
    }
}

/**
 * Specular-material scope. Fixed-function `glMaterialfv` is gone in ES; we
 * keep the snapshot semantics and stash the specular tint where shaders can
 * read it as a default uniform.
 */
class GLSSpecular(val color: FloatArray, val shininess: Float) {
    private val prevColor: FloatArray = sSpecularColor.copyOf()
    private val prevExponent: Int = sSpecularExponent

    init {
        if (shininess > 0f) {
            val shiny = (shininess * 128f).toInt().coerceIn(0, 128)
            sSpecularColor = color.copyOf()
            sSpecularExponent = shiny
        }
    }

    fun restore() {
        if (shininess > 0f) {
            sSpecularColor = prevColor.copyOf()
            sSpecularExponent = prevExponent
        }
    }

    companion object {
        var sSpecularColor: FloatArray = floatArrayOf(0f, 0f, 0f, 1f)
            private set
        var sSpecularExponent: Int = 0
            private set
    }
}

/**
 * RAII enable: queries the cap's current state, enables it on construction,
 * and restores the prior state on [restore].
 */
class GLEnable(val cap: Int) {
    private val prevEnabled: Boolean = GpuBackend.current.isEnabled(cap)

    init {
        if (!prevEnabled) GpuBackend.current.enable(cap)
    }

    fun restore() {
        val gl = GpuBackend.current
        if (prevEnabled) gl.enable(cap) else gl.disable(cap)
    }
}

/**
 * RAII disable: mirror of [GLEnable] that turns the cap off on construction.
 */
class GLDisable(val cap: Int) {
    private val prevEnabled: Boolean = GpuBackend.current.isEnabled(cap)

    init {
        if (prevEnabled) GpuBackend.current.disable(cap)
    }

    fun restore() {
        val gl = GpuBackend.current
        if (prevEnabled) gl.enable(cap) else gl.disable(cap)
    }
}
