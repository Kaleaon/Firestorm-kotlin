package com.firestorm.newview

import java.util.UUID

typealias RenderTarget = Any

abstract class ViewerTexture(
    val fullWidth: Int,
    val fullHeight: Int,
    val components: Int,
    val useMipMaps: Boolean
) {
    companion object {
        const val DYNAMIC_TEXTURE = 4
        const val INVALID_TEXTURE_TYPE = 255
    }

    open fun getFullWidth(): Int = fullWidth
    open fun getFullHeight(): Int = fullHeight

    open fun generateGLTexture() { // no-op
    }
    open fun createGLTexture(discardLevel: Int, rawImage: Any?, mipLevel: Int, usable: Boolean, texType: Int) {
        // no-op
    }
}

enum class EOrder(val value: Int) {
    ORDER_FIRST(0),
    ORDER_MIDDLE(1),
    ORDER_LAST(2),
    ORDER_RESET(3);

    companion object {
        const val ORDER_COUNT = 4
    }
}

data class CoordGL(var x: Int = 0, var y: Int = 0) {
    fun set(nx: Int, ny: Int) { x = nx; y = ny }
}

abstract class ViewerDynamicTexture(
    width: Int,
    height: Int,
    components: Int,
    private val order: EOrder,
    protected val clamp: Boolean
) : ViewerTexture(width, height, components, false) {

    companion object {
        const val LL_VIEWER_DYNAMIC_TEXTURE = ViewerTexture.DYNAMIC_TEXTURE
        const val LL_TEX_LAYER_SET_BUFFER   = ViewerTexture.INVALID_TEXTURE_TYPE + 1
        const val LL_VISUAL_PARAM_HINT      = ViewerTexture.INVALID_TEXTURE_TYPE + 2
        const val LL_VISUAL_PARAM_RESET     = ViewerTexture.INVALID_TEXTURE_TYPE + 3
        const val LL_PREVIEW_ANIMATION      = ViewerTexture.INVALID_TEXTURE_TYPE + 4
        const val LL_IMAGE_PREVIEW_SCULPTED = ViewerTexture.INVALID_TEXTURE_TYPE + 5
        const val LL_IMAGE_PREVIEW_AVATAR   = ViewerTexture.INVALID_TEXTURE_TYPE + 6
        const val INVALID_DYNAMIC_TEXTURE   = ViewerTexture.INVALID_TEXTURE_TYPE + 7

        val sInstances: Array<MutableSet<ViewerDynamicTexture>> =
            Array(EOrder.ORDER_COUNT) { mutableSetOf() }

        var sNumRenders: Int = 0

        fun updateAllInstances(): Boolean {
            return false
        }

        fun destroyGL() {
            for (order in 0 until EOrder.ORDER_COUNT) {
                for (tex in sInstances[order]) {
                    tex.destroyGLTexture()
                }
            }
        }

        fun restoreGL() {
            // no-op
        }
    }

    protected val origin = CoordGL()
    protected var boundTarget: RenderTarget? = null

    init {
        require(components in 1..4) { "components must be 1–4, got $components" }
        generateGLTexture()
        sInstances[order.value].add(this)
    }

    fun getOriginX(): Int = origin.x
    fun getOriginY(): Int = origin.y
    fun getSize(): Int = fullWidth * fullHeight * components

    open fun getType(): Byte = ViewerTexture.DYNAMIC_TEXTURE.toByte()

    open fun needsRender(): Boolean = true

    open fun preRender(clearDepth: Boolean = true) {
        origin.set(0, 0)
        // no-op
    }

    open fun render(): Boolean = false

    open fun postRender(success: Boolean) {
        // no-op
    }

    open fun restoreGLTexture() {}
    open fun destroyGLTexture() {}

    fun setBoundTarget(target: RenderTarget?) { boundTarget = target }

    override fun generateGLTexture() {
        super.generateGLTexture()
        generateGLTexture(-1, 0, 0, false)
    }

    fun generateGLTexture(internalFormat: Int, primaryFormat: Int, typeFormat: Int, swapBytes: Boolean) {
        if (components < 1 || components > 4) error("Bad component count: $components")
        // no-op
    }

    protected fun finalize() {
        for (o in 0 until EOrder.ORDER_COUNT) {
            sInstances[o].remove(this)
        }
    }
}
