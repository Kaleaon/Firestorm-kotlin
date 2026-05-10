package com.firestorm.llrender

class RenderTarget {
    var width: Int = 0
    var height: Int = 0
    var hasDepth: Boolean = false

    fun allocate(
        width: Int,
        height: Int,
        colorFormat: UInt = 0x8058u,
        depth: Boolean = false,
        useFBO: Boolean = true,
    ) {
        this.width = width
        this.height = height
        this.hasDepth = depth
        TODO("GPU: allocate FBO/textures")
    }

    fun release() {
        width = 0
        height = 0
        hasDepth = false
        TODO("GPU: release FBO/textures")
    }

    fun bindTarget() { TODO("GPU: bind FBO") }

    fun flush(sync: Boolean = false) { TODO("GPU: flush FBO") }

    @Suppress("LongParameterList")
    fun copyContents(
        src: RenderTarget,
        srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int,
        dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int,
        mask: UInt,
        filter: UInt,
    ) { TODO("GPU: blit framebuffer") }

    fun isComplete(): Boolean = width > 0 && height > 0

    companion object {
        val sCurFBO: RenderTarget? = null
    }
}
