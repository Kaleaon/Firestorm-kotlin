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

        val gl = GpuBackend.current
        fbo = gl.genFramebuffers(1)[0].toUInt()
        gl.bindFramebuffer(GL.FRAMEBUFFER, fbo.toInt())
        if (depth && this.depth != 0u) {
            gl.framebufferTexture2D(GL.FRAMEBUFFER, GL.DEPTH_ATTACHMENT, GL.TEXTURE_2D, this.depth.toInt(), 0)
        }
        val added = addColorAttachment(colorFmt)
        gl.bindFramebuffer(GL.FRAMEBUFFER, 0)
        return added
    }

    fun resize(resx: UInt, resy: UInt) {
        if (resX == resx && resY == resy) return
        val oldBytes = bytesPerPixelFor(usage, useDepth)
        bytesAllocated -= (resX * resY).toULong().toUInt() * oldBytes.toUInt()
        resX = resx
        resY = resy
        val gl = GpuBackend.current
        gl.bindFramebuffer(GL.FRAMEBUFFER, fbo.toInt())
        for ((i, name) in tex.withIndex()) {
            gl.bindTexture(GL.TEXTURE_2D, name.toInt())
            val fmt = internalFormat.getOrElse(i) { GL.RGBA8.toUInt() }
            gl.texImage2D(GL.TEXTURE_2D, 0, fmt.toInt(), resx.toInt(), resy.toInt(), GL.RGBA, GL.UNSIGNED_BYTE, null)
        }
        if (depth != 0u) {
            gl.bindTexture(GL.TEXTURE_2D, depth.toInt())
            gl.texImage2D(GL.TEXTURE_2D, 0, GL.DEPTH_COMPONENT24, resx.toInt(), resy.toInt(), GL.DEPTH_COMPONENT, GL.UNSIGNED_INT, null)
        }
        gl.bindFramebuffer(GL.FRAMEBUFFER, 0)
        bytesAllocated += (resX * resY).toULong().toUInt() * bytesPerPixelFor(usage, useDepth).toUInt()
    }

    fun setColorAttachment(texName: UInt = 0u) {
        val gl = GpuBackend.current
        if (fbo == 0u) fbo = gl.genFramebuffers(1)[0].toUInt()
        gl.bindFramebuffer(GL.FRAMEBUFFER, fbo.toInt())
        gl.framebufferTexture2D(GL.FRAMEBUFFER, GL.COLOR_ATTACHMENT0, GL.TEXTURE_2D, texName.toInt(), 0)
        tex.add(texName)
        // Record a format entry so internalFormat stays in sync with tex.
        // The texture is externally owned so bytesAllocated is not incremented;
        // callers must invoke releaseColorAttachment() before release() to avoid
        // attempting to delete externally-owned textures.
        internalFormat.add(GL.RGBA8.toUInt())
        gl.bindFramebuffer(GL.FRAMEBUFFER, 0)
    }

    fun releaseColorAttachment() {
        if (fbo == 0u) {
            tex.clear()
            return
        }
        val gl = GpuBackend.current
        gl.bindFramebuffer(GL.FRAMEBUFFER, fbo.toInt())
        for (i in tex.indices) {
            gl.framebufferTexture2D(GL.FRAMEBUFFER, GL.COLOR_ATTACHMENT0 + i, GL.TEXTURE_2D, 0, 0)
        }
        gl.bindFramebuffer(GL.FRAMEBUFFER, 0)
        tex.clear()
    }

    fun addColorAttachment(colorFmt: UInt): Boolean {
        if (colorFmt == 0u) return true
        val offset = tex.size
        if (offset >= 4) return false

        val gl = GpuBackend.current
        val texName = gl.genTextures(1)[0]
        gl.bindTexture(GL.TEXTURE_2D, texName)
        gl.texImage2D(GL.TEXTURE_2D, 0, colorFmt.toInt(), resX.toInt(), resY.toInt(), GL.RGBA, GL.UNSIGNED_BYTE, null)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)

        if (fbo == 0u) fbo = gl.genFramebuffers(1)[0].toUInt()
        gl.bindFramebuffer(GL.FRAMEBUFFER, fbo.toInt())
        gl.framebufferTexture2D(GL.FRAMEBUFFER, GL.COLOR_ATTACHMENT0 + offset, GL.TEXTURE_2D, texName, 0)
        gl.bindFramebuffer(GL.FRAMEBUFFER, 0)

        tex.add(texName.toUInt())
        internalFormat.add(colorFmt)
        bytesAllocated += (resX * resY).toULong().toUInt() * 4u
        return true
    }

    fun allocateDepth(): Boolean {
        val gl = GpuBackend.current
        val name = gl.genTextures(1)[0]
        gl.bindTexture(GL.TEXTURE_2D, name)
        gl.texImage2D(GL.TEXTURE_2D, 0, GL.DEPTH_COMPONENT24, resX.toInt(), resY.toInt(), GL.DEPTH_COMPONENT, GL.UNSIGNED_INT, null)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.NEAREST)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.NEAREST)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)
        depth = name.toUInt()
        bytesAllocated += (resX * resY).toULong().toUInt() * 3u
        return true
    }

    fun shareDepthBuffer(target: RenderTarget) {
        check(fbo != 0u && target.fbo != 0u) { "Cannot share depth buffer between non-FBO render targets" }
        check(target.depth == 0u) { "Attempting to override existing depth buffer" }
        check(!target.useDepth) { "Attempting to override existing shared depth buffer" }
        if (depth != 0u) {
            val gl = GpuBackend.current
            gl.bindFramebuffer(GL.FRAMEBUFFER, target.fbo.toInt())
            gl.framebufferTexture2D(GL.FRAMEBUFFER, GL.DEPTH_ATTACHMENT, GL.TEXTURE_2D, depth.toInt(), 0)
            gl.bindFramebuffer(GL.FRAMEBUFFER, 0)
            target.useDepth = true
        }
    }

    fun release() {
        val gl = GpuBackend.current
        if (depth != 0u) {
            gl.deleteTextures(intArrayOf(depth.toInt()))
            bytesAllocated -= (resX * resY).toULong().toUInt() * 3u
            depth = 0u
        }
        if (fbo != 0u) {
            gl.bindFramebuffer(GL.FRAMEBUFFER, fbo.toInt())
            for (i in tex.indices) {
                gl.framebufferTexture2D(GL.FRAMEBUFFER, GL.COLOR_ATTACHMENT0 + i, GL.TEXTURE_2D, 0, 0)
            }
            gl.bindFramebuffer(GL.FRAMEBUFFER, 0)
            gl.deleteFramebuffers(intArrayOf(fbo.toInt()))
            fbo = 0u
        }
        if (tex.isNotEmpty()) {
            gl.deleteTextures(tex.map { it.toInt() }.toIntArray())
            bytesAllocated -= (resX * resY).toULong().toUInt() * (4u * tex.size.toUInt())
            tex.clear()
        }
        internalFormat.clear()
        useDepth = false
        resX = 0u
        resY = 0u
    }

    fun bindTarget() {
        check(fbo != 0u) { "FBO not allocated" }
        check(!isBoundInStack()) { "RenderTarget already bound in stack" }
        val gl = GpuBackend.current
        previousRT = currentBoundTarget
        currentBoundTarget = this
        curFBO = fbo
        curResX = resX
        curResY = resY
        gl.bindFramebuffer(GL.FRAMEBUFFER, fbo.toInt())
        if (tex.isNotEmpty()) {
            gl.drawBuffers(IntArray(tex.size) { GL.COLOR_ATTACHMENT0 + it })
        } else {
            gl.drawBuffers(intArrayOf(GL.NONE))
        }
        gl.readBuffer(if (tex.isNotEmpty()) GL.COLOR_ATTACHMENT0 else GL.NONE)
        gl.viewport(0, 0, resX.toInt(), resY.toInt())
    }

    fun clear(mask: UInt = 0xFFFFFFFFu) {
        check(fbo != 0u)
        val gl = GpuBackend.current
        val status = gl.checkFramebufferStatus(GL.FRAMEBUFFER)
        check(status == GL.FRAMEBUFFER_COMPLETE) { "Framebuffer is not complete: 0x${status.toString(16)}" }
        val clearMask = GL_COLOR_BUFFER_BIT or (if (useDepth) GL_DEPTH_BUFFER_BIT else 0u)
        gl.clear((clearMask and mask).toInt())
    }

    fun flush() {
        check(fbo != 0u)
        check(currentBoundTarget == this)
        val gl = GpuBackend.current
        if (generateMipMaps == TextureMipGeneration.AUTO && tex.isNotEmpty()) {
            gl.bindTexture(GL.TEXTURE_2D, tex[0].toInt())
            gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR_MIPMAP_LINEAR)
            gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR)
            gl.generateMipmap(GL.TEXTURE_2D)
        }
        val prior = previousRT
        currentBoundTarget = prior
        if (prior != null) {
            curFBO = prior.fbo
            curResX = prior.resX
            curResY = prior.resY
            gl.bindFramebuffer(GL.FRAMEBUFFER, prior.fbo.toInt())
            gl.viewport(0, 0, prior.resX.toInt(), prior.resY.toInt())
        } else {
            curFBO = 0u
            gl.bindFramebuffer(GL.FRAMEBUFFER, 0)
        }
        previousRT = null
    }

    fun bindTexture(index: UInt, channel: Int, filterOptions: TextureFilterOptions = TextureFilterOptions.BILINEAR) {
        require(index < tex.size.toUInt()) { "Invalid texture index $index" }
        val gl = GpuBackend.current
        gl.activeTexture(GL.TEXTURE0 + channel)
        gl.bindTexture(GL.TEXTURE_2D, tex[index.toInt()].toInt())
        val (minFilter, magFilter) = when (filterOptions) {
            TextureFilterOptions.NEAREST, TextureFilterOptions.POINT -> GL.NEAREST to GL.NEAREST
            TextureFilterOptions.BILINEAR -> GL.LINEAR to GL.LINEAR
            TextureFilterOptions.TRILINEAR -> GL.LINEAR_MIPMAP_LINEAR to GL.LINEAR
            TextureFilterOptions.ANISOTROPIC -> GL.LINEAR_MIPMAP_LINEAR to GL.LINEAR
        }
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, minFilter)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, magFilter)
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

    private fun bytesPerPixelFor(usage: TextureType, depth: Boolean): Int {
        val color = if (tex.isEmpty()) 0 else 4 * tex.size
        val depthBytes = if (depth) 3 else 0
        return color + depthBytes
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
