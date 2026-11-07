package com.firestorm.llrender

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
        // no-op
    }

    fun restore() {
        sDepthEnabled = prevDepthEnabled
        sDepthFunc = prevDepthFunc
        sWriteEnabled = prevWriteEnabled
        // no-op
    }

    fun checkState() {
        // no-op
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

open class GLSPipelineSkyBox {
    private val cullFace = GLDisable(GLSDefault.GL_CULL_FACE)

    init {
        // no-op
    }

    fun restore() {
        cullFace.restore()
        // no-op
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

class GLSSpecular(color: FloatArray, val shininess: Float) {
    init {
        if (shininess > 0f) {
            val shiny = (shininess * 128f).toInt().coerceIn(0, 128)
            // no-op
        }
    }

    fun restore() {
        if (shininess > 0f) {
            // no-op
        }
    }
}

class GLEnable(val cap: Int) {
    private var prevEnabled: Boolean = false

    init {
        // no-op
    }

    fun restore() {
        // no-op
    }
}

class GLDisable(val cap: Int) {
    private var prevEnabled: Boolean = false

    init {
        // no-op
    }

    fun restore() {
        // no-op
    }
}
