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

        TODO("GPU: glGenFramebuffers; if depth attach depth texture; addColorAttachment(colorFmt)")
    }

    fun resize(resx: UInt, resy: UInt) {
        resX = resx
        resY = resy
        TODO("GPU: rebind each tex in mTex with setManualImage at new resolution; update depth texture if present; update sBytesAllocated")
    }

    fun setColorAttachment(texName: UInt = 0u) {
        TODO("GPU: glGenFramebuffers if needed; glFramebufferTexture2D for GL_COLOR_ATTACHMENT0; push texName into tex list")
    }

    fun releaseColorAttachment() {
        TODO("GPU: glFramebufferTexture2D detach; tex.clear()")
    }

    fun addColorAttachment(colorFmt: UInt): Boolean {
        if (colorFmt == 0u) return true
        val offset = tex.size
        if (offset >= 4) return false
        TODO("GPU: generateTextures; setManualImage; set filtering; glFramebufferTexture2D; push to tex/internalFormat lists; update sBytesAllocated")
    }

    fun allocateDepth(): Boolean {
        TODO("GPU: generateTextures into depth; setManualImage GL_DEPTH_COMPONENT24; setTextureFilteringOption POINT; update sBytesAllocated")
    }

    fun shareDepthBuffer(target: RenderTarget) {
        check(fbo != 0u && target.fbo != 0u) { "Cannot share depth buffer between non-FBO render targets" }
        check(target.depth == 0u) { "Attempting to override existing depth buffer" }
        check(!target.useDepth) { "Attempting to override existing shared depth buffer" }
        if (depth != 0u) {
            TODO("GPU: glBindFramebuffer target.fbo; glFramebufferTexture2D depth attachment; target.useDepth = true")
        }
    }

    fun release() {
        TODO("GPU: deleteTextures(depth); detach shared depth; detach extra color attachments; glDeleteFramebuffers; deleteTextures primary; clear lists; reset resX/resY")
    }

    fun bindTarget() {
        check(fbo != 0u) { "FBO not allocated" }
        check(!isBoundInStack()) { "RenderTarget already bound in stack" }
        TODO("GPU: glBindFramebuffer; glDrawBuffers; glReadBuffer; glViewport; push sBoundTarget stack")
    }

    fun clear(mask: UInt = 0xFFFFFFFFu) {
        check(fbo != 0u)
        val clearMask = GL_COLOR_BUFFER_BIT or (if (useDepth) GL_DEPTH_BUFFER_BIT else 0u)
        TODO("GPU: check_framebuffer_status; glClear(clearMask and mask)")
    }

    fun flush() {
        check(fbo != 0u)
        check(currentBoundTarget == this)
        if (generateMipMaps == TextureMipGeneration.AUTO) {
            TODO("GPU: bindTexture(0,0,TFO_TRILINEAR); glGenerateMipmap GL_TEXTURE_2D")
        }
        TODO("GPU: restore previousRT via bindTarget() or unbind to default framebuffer; restore gGLViewport")
    }

    fun bindTexture(index: UInt, channel: Int, filterOptions: TextureFilterOptions = TextureFilterOptions.BILINEAR) {
        TODO("GPU: bindManual(usage, getTexture(index), useMips); setTextureFilteringOption")
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
