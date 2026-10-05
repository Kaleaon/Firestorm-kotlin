package com.firestorm.llrender

import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max

class RenderTarget {

    var resX: UInt = 0u
        private set
    var resY: UInt = 0u
        private set

    private val tex: MutableList<UInt> = mutableListOf()
    private val internalFormat: MutableList<UInt> = mutableListOf()
    private var fbo: UInt = 0u
    private var previousRT: RenderTarget? = null

    private var depth: UInt = 0u
    private var useDepth: Boolean = false
    private var generateMipMaps: TextureMipGeneration = TextureMipGeneration.NONE
    private var mipLevels: UInt = 0u
    private var usage: TextureType = TextureType.TEXTURE

    fun allocate(
        resx: UInt,
        resy: UInt,
        colorFmt: UInt,
        depth: Boolean = false,
        usage: TextureType = TextureType.TEXTURE,
        generateMipMaps: TextureMipGeneration = TextureMipGeneration.NONE
    ): Boolean {
        if (resX == resx && resY == resy && this.usage == usage && depth == useDepth && this.generateMipMaps == generateMipMaps) {
            return true
        }

        val clampedX = minOf(resx, MAX_TEXTURE_SIZE)
        val clampedY = minOf(resy, MAX_TEXTURE_SIZE)

        release()

        resX = clampedX
        resY = clampedY
        this.usage = usage
        useDepth = depth
        this.generateMipMaps = generateMipMaps

        if (this.generateMipMaps != TextureMipGeneration.NONE) {
            mipLevels = (1u + floor(log10(max(resX, resY).toFloat()) / log10(2f)).toUInt())
        }

        if (depth) {
            if (!allocateDepth()) return false
        }

        // no-op
        return false
    }

    fun resize(resx: UInt, resy: UInt) {
        resX = resx
        resY = resy
        // no-op
    }

    fun setColorAttachment(texName: UInt = 0u) {
        // no-op
    }

    fun releaseColorAttachment() {
        // no-op
    }

    fun addColorAttachment(colorFmt: UInt): Boolean {
        if (colorFmt == 0u) return true
        val offset = tex.size
        if (offset >= 4) return false
        // no-op
        return false
    }

    fun allocateDepth(): Boolean {
        // no-op
        return false
    }

    fun shareDepthBuffer(target: RenderTarget) {
        check(fbo != 0u && target.fbo != 0u) { "Cannot share depth buffer between non-FBO render targets" }
        check(target.depth == 0u) { "Attempting to override existing depth buffer" }
        check(!target.useDepth) { "Attempting to override existing shared depth buffer" }
        if (depth != 0u) {
            // no-op
        }
    }

    fun release() {
        // no-op
    }

    fun bindTarget() {
        check(fbo != 0u) { "FBO not allocated" }
        check(!isBoundInStack()) { "RenderTarget already bound in stack" }
        // no-op
    }

    fun clear(mask: UInt = 0xFFFFFFFFu) {
        check(fbo != 0u)
        val clearMask = GL_COLOR_BUFFER_BIT or (if (useDepth) GL_DEPTH_BUFFER_BIT else 0u)
        // no-op
    }

    fun flush() {
        check(fbo != 0u)
        check(currentBoundTarget == this)
        if (generateMipMaps == TextureMipGeneration.AUTO) {
            // no-op
        }
        // no-op
    }

    fun bindTexture(index: UInt, channel: Int, filterOptions: TextureFilterOptions = TextureFilterOptions.BILINEAR) {
        // no-op
    }

    fun getTexture(attachment: UInt = 0u): UInt {
        require(attachment < tex.size.toUInt()) { "Invalid attachment index $attachment for size ${tex.size}" }
        return tex[attachment.toInt()]
    }

    fun getNumTextures(): UInt = tex.size.toUInt()

    fun getDepth(): UInt = depth

    fun getUsage(): TextureType = usage

    fun getWidth(): UInt = resX

    fun getHeight(): UInt = resY

    fun getViewport(viewport: IntArray) {
        viewport[0] = 0
        viewport[1] = 0
        viewport[2] = resX.toInt()
        viewport[3] = resY.toInt()
    }

    fun isComplete(): Boolean = tex.isNotEmpty() || depth != 0u

    fun isBoundInStack(): Boolean {
        var cur = currentBoundTarget
        while (cur != null && cur !== this) {
            cur = cur.previousRT
        }
        return cur === this
    }

    fun swapFboRefs(other: RenderTarget) {
        check(fbo != 0u && other.fbo != 0u)
        check(!isBoundInStack() && !other.isBoundInStack())
        val tmpFbo = fbo; fbo = other.fbo; other.fbo = tmpFbo
        val tmpTex = tex.toMutableList(); tex.clear(); tex.addAll(other.tex); other.tex.clear(); other.tex.addAll(tmpTex)
    }

    companion object {
        var useFBO: Boolean = false
        var bytesAllocated: UInt = 0u
        var curFBO: UInt = 0u
        var curResX: UInt = 0u
        var curResY: UInt = 0u
        var currentBoundTarget: RenderTarget? = null

        private val MAX_TEXTURE_SIZE: UInt = 16384u

        private val GL_COLOR_BUFFER_BIT: UInt = 0x00004000u
        private val GL_DEPTH_BUFFER_BIT: UInt = 0x00000100u
    }
}

enum class TextureType { TEXTURE, RECT_TEXTURE, CUBE_MAP, CUBE_MAP_ARRAY }
enum class TextureMipGeneration { NONE, AUTO, MANUAL }
enum class TextureFilterOptions { NEAREST, BILINEAR, TRILINEAR, ANISOTROPIC, POINT }
