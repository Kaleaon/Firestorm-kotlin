package com.firestorm.newview

class DrawPoolSimple : RenderPass(DrawPool.PoolType.SIMPLE.value.toUInt()) {

    companion object {
        const val VERTEX_DATA_MASK: UInt = 0u or
            VertexBufferFlags.MAP_VERTEX or
            VertexBufferFlags.MAP_NORMAL or
            VertexBufferFlags.MAP_TEXCOORD0 or
            VertexBufferFlags.MAP_COLOR
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    override fun getNumDeferredPasses(): Int = 1

    override fun renderDeferred(pass: Int) {
        // no-op
    }
}


class DrawPoolGrass : RenderPass(DrawPool.PoolType.GRASS.value.toUInt()) {

    companion object {
        const val VERTEX_DATA_MASK: UInt = 0u or
            VertexBufferFlags.MAP_VERTEX or
            VertexBufferFlags.MAP_NORMAL or
            VertexBufferFlags.MAP_TEXCOORD0 or
            VertexBufferFlags.MAP_COLOR
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    override fun getNumDeferredPasses(): Int = 1

    override fun renderDeferred(pass: Int) {
        // no-op
    }
}


class DrawPoolAlphaMask : RenderPass(DrawPool.PoolType.ALPHA_MASK.value.toUInt()) {

    companion object {
        const val VERTEX_DATA_MASK: UInt = 0u or
            VertexBufferFlags.MAP_VERTEX or
            VertexBufferFlags.MAP_NORMAL or
            VertexBufferFlags.MAP_TEXCOORD0 or
            VertexBufferFlags.MAP_COLOR
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    override fun getNumDeferredPasses(): Int = 1

    override fun renderDeferred(pass: Int) {
        // no-op
    }
}


class DrawPoolFullbrightAlphaMask : RenderPass(DrawPool.PoolType.FULLBRIGHT_ALPHA_MASK.value.toUInt()) {

    companion object {
        const val VERTEX_DATA_MASK: UInt = 0u or
            VertexBufferFlags.MAP_VERTEX or
            VertexBufferFlags.MAP_TEXCOORD0 or
            VertexBufferFlags.MAP_COLOR
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    override fun getNumPostDeferredPasses(): Int = 1

    override fun renderPostDeferred(pass: Int) {
        // no-op
    }
}


class DrawPoolFullbright : RenderPass(DrawPool.PoolType.FULLBRIGHT.value.toUInt()) {

    companion object {
        const val VERTEX_DATA_MASK: UInt = 0u or
            VertexBufferFlags.MAP_VERTEX or
            VertexBufferFlags.MAP_TEXCOORD0 or
            VertexBufferFlags.MAP_COLOR
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    override fun getNumPostDeferredPasses(): Int = 1

    override fun renderPostDeferred(pass: Int) {
        // no-op
    }
}


class DrawPoolGlow : RenderPass(DrawPool.PoolType.GLOW.value.toUInt()) {

    companion object {
        const val VERTEX_DATA_MASK: UInt = 0u or
            VertexBufferFlags.MAP_VERTEX or
            VertexBufferFlags.MAP_TEXCOORD0 or
            VertexBufferFlags.MAP_EMISSIVE
    }

    override fun getVertexDataMask(): UInt = VERTEX_DATA_MASK

    override fun getNumPostDeferredPasses(): Int = 1

    override fun renderPostDeferred(pass: Int) {
        // no-op
    }
}


// Vertex buffer attribute bit flags matching LLVertexBuffer constants.
object VertexBufferFlags {
    const val MAP_VERTEX: UInt = 0x0001u
    const val MAP_NORMAL: UInt = 0x0002u
    const val MAP_TEXCOORD0: UInt = 0x0004u
    const val MAP_TEXCOORD1: UInt = 0x0008u
    const val MAP_TEXCOORD2: UInt = 0x0010u
    const val MAP_TEXCOORD3: UInt = 0x0020u
    const val MAP_COLOR: UInt = 0x0040u
    const val MAP_EMISSIVE: UInt = 0x0080u
    const val MAP_TANGENT: UInt = 0x0100u
    const val MAP_WEIGHT: UInt = 0x0200u
    const val MAP_WEIGHT4: UInt = 0x0400u
    const val MAP_CLOTHWEIGHT: UInt = 0x0800u
    const val MAP_TEXTURE_INDEX: UInt = 0x1000u
}
