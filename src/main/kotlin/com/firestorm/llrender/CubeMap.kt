package com.firestorm.llrender

private const val DEFAULT_RESOLUTION: UShort = 64u

class CubeMap(val isSrgb: Boolean) {

    private var textureStage: Int = 0
    private var matrixStage: Int = 0

    val targets: IntArray = intArrayOf(
        GL.TEXTURE_CUBE_MAP_NEGATIVE_X,
        GL.TEXTURE_CUBE_MAP_POSITIVE_X,
        GL.TEXTURE_CUBE_MAP_NEGATIVE_Y,
        GL.TEXTURE_CUBE_MAP_POSITIVE_Y,
        GL.TEXTURE_CUBE_MAP_NEGATIVE_Z,
        GL.TEXTURE_CUBE_MAP_POSITIVE_Z
    )

    private val images: Array<ImageGL?> = arrayOfNulls(6)
    private val rawImages: Array<ImageRaw?> = arrayOfNulls(6)

    private val textureMatrix: FloatArray = FloatArray(16) { if (it % 5 == 0) 1f else 0f }

    fun initGl() {
        if (!sUseCubeMaps) {
            System.err.println("Using cube map without extension!")
            return
        }
        if (images[0] != null) return

        val gl = GpuBackend.current
        val name = gl.genTextures(1)[0]
        val internalFormat = if (isSrgb) GL.SRGB8_ALPHA8 else GL.RGBA8
        for (i in 0 until 6) {
            val raw = ImageRaw(DEFAULT_RESOLUTION.toInt(), DEFAULT_RESOLUTION.toInt(), 4)
            rawImages[i] = raw
            val img = ImageGL(raw.width, raw.height, 4, useMipMaps = false)
            img.internalFormat = internalFormat
            img.target = GL.TEXTURE_CUBE_MAP
            img.textureType = TextureType.CUBE_MAP
            img.setTexName(name.toUInt())
            images[i] = img
        }
        gl.bindTexture(GL.TEXTURE_CUBE_MAP, name)
        for (i in 0 until 6) {
            gl.texImage2D(targets[i], 0, internalFormat,
                DEFAULT_RESOLUTION.toInt(), DEFAULT_RESOLUTION.toInt(),
                GL.RGBA, GL.UNSIGNED_BYTE, null)
        }
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_WRAP_R, GL.CLAMP_TO_EDGE)
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_MIN_FILTER, GL.LINEAR)
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_MAG_FILTER, GL.LINEAR)
    }

    fun initRawData(rawImages: List<ImageRaw>) {
        val flipX  = booleanArrayOf(false, true,  false, false, true,  false)
        val flipY  = booleanArrayOf(true,  true,  true,  false, true,  true)
        val transpose = booleanArrayOf(false, false, false, false, true,  true)

        for (i in 0 until 6) {
            val src = rawImages[i].data
            val dst = this.rawImages[i]!!.data
            var offset = 0
            for (y in 0 until 64) {
                for (x in 0 until 64) {
                    var sx = x
                    var sy = if (flipY[i]) 63 - y else y
                    if (flipX[i]) sx = 63 - x
                    if (transpose[i]) { val t = sx; sx = sy; sy = t }
                    var so = (64 * sy + sx) * 4
                    dst[offset++] = src[so++]
                    dst[offset++] = src[so++]
                    dst[offset++] = src[so++]
                    dst[offset++] = src[so]
                }
            }
        }
    }

    fun initGlData() {
        val gl = GpuBackend.current
        val texName = images[0]?.getTexName()?.toInt() ?: return
        gl.bindTexture(GL.TEXTURE_CUBE_MAP, texName)
        for (i in 0 until 6) {
            val raw = rawImages[i] ?: continue
            gl.texSubImage2D(targets[i], 0, 0, 0,
                DEFAULT_RESOLUTION.toInt(), DEFAULT_RESOLUTION.toInt(),
                GL.RGBA, GL.UNSIGNED_BYTE, raw.data)
        }
    }

    fun init(rawImages: List<ImageRaw>) {
        initGl()
        initRawData(rawImages)
        initGlData()
    }

    fun initReflectionMap(resolution: UInt, components: UInt = 3u) {
        val gl = GpuBackend.current
        val name = gl.genTextures(1)[0]
        val img = ImageGL(resolution.toInt(), resolution.toInt(), components.toInt(), useMipMaps = true)
        img.target = GL.TEXTURE_CUBE_MAP
        img.textureType = TextureType.CUBE_MAP
        img.setTexName(name.toUInt())
        images[0] = img
        gl.bindTexture(GL.TEXTURE_CUBE_MAP, name)
        for (face in targets) {
            gl.texImage2D(face, 0, GL.RGBA8, resolution.toInt(), resolution.toInt(), GL.RGBA, GL.UNSIGNED_BYTE, null)
        }
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_WRAP_R, GL.CLAMP_TO_EDGE)
    }

    fun initEnvironmentMap(rawImages: List<ImageRaw>) {
        require(rawImages.size == 6)
        val gl = GpuBackend.current
        val name = gl.genTextures(1)[0]
        val resolution = rawImages[0].width
        val img = ImageGL(resolution, resolution, 4, useMipMaps = true)
        img.target = GL.TEXTURE_CUBE_MAP
        img.textureType = TextureType.CUBE_MAP
        img.setTexName(name.toUInt())
        images[0] = img
        gl.bindTexture(GL.TEXTURE_CUBE_MAP, name)
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_WRAP_R, GL.CLAMP_TO_EDGE)
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_MIN_FILTER, GL.LINEAR_MIPMAP_LINEAR)
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_MAG_FILTER, GL.LINEAR)
        for (i in 0 until 6) {
            gl.texImage2D(targets[i], 0, GL.RGBA8, resolution, resolution, GL.RGBA, GL.UNSIGNED_BYTE, rawImages[i].data)
        }
        gl.enable(GL.TEXTURE_CUBE_MAP_SEAMLESS)
        gl.generateMipmap(GL.TEXTURE_CUBE_MAP)
    }

    fun generateMipMaps() {
        val img = images[0] ?: return
        img.setUseMipMaps(true)
        img.setHasMipMaps(true)
        val gl = GpuBackend.current
        gl.bindTexture(GL.TEXTURE_CUBE_MAP, img.getTexName().toInt())
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_MIN_FILTER, GL.LINEAR_MIPMAP_LINEAR)
        gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_MAG_FILTER, GL.LINEAR)
        gl.generateMipmap(GL.TEXTURE_CUBE_MAP)
    }

    fun getGlName(): UInt = images[0]?.getTexName() ?: 0u

    fun getResolution(): UInt = images[0]?.getWidth(0)?.toUInt() ?: 0u

    fun bind() {
        val gl = GpuBackend.current
        gl.activeTexture(GL.TEXTURE0 + textureStage)
        gl.bindTexture(GL.TEXTURE_CUBE_MAP, getGlName().toInt())
    }

    fun enable(stage: Int) = enableTexture(stage)

    fun enableTexture(stage: Int) {
        textureStage = stage
        if (stage >= 0 && sUseCubeMaps) {
            GpuBackend.current.activeTexture(GL.TEXTURE0 + stage)
            GpuBackend.current.bindTexture(GL.TEXTURE_CUBE_MAP, getGlName().toInt())
        }
    }

    fun disable() = disableTexture()

    fun disableTexture() {
        if (textureStage >= 0 && sUseCubeMaps) {
            val gl = GpuBackend.current
            gl.activeTexture(GL.TEXTURE0 + textureStage)
            gl.bindTexture(GL.TEXTURE_CUBE_MAP, 0)
            if (textureStage == 0) {
                gl.bindTexture(GL.TEXTURE_2D, 0)
            }
        }
    }

    fun setMatrix(stage: Int) {
        matrixStage = stage
        if (matrixStage < 0) return
        // Extract the rotation-only 3x3 from the current modelview, transpose
        // it (we want the inverse rotation for sampling), and stash it as a
        // texture matrix. The actual upload is done by whatever shader binds
        // this CubeMap — they read `textureMatrix` directly.
        val mv = gGLModelView
        for (i in 0..2) for (j in 0..2) {
            textureMatrix[j * 4 + i] = mv[i * 4 + j]
        }
        textureMatrix[3] = 0f; textureMatrix[7] = 0f; textureMatrix[11] = 0f
        textureMatrix[12] = 0f; textureMatrix[13] = 0f; textureMatrix[14] = 0f
        textureMatrix[15] = 1f
    }

    fun restoreMatrix() {
        if (matrixStage < 0) return
        for (i in textureMatrix.indices) textureMatrix[i] = if (i % 5 == 0) 1f else 0f
    }

    fun destroyGL() {
        val gl = GpuBackend.current
        val toDelete = images.mapNotNull { it?.getTexName()?.toInt()?.takeIf { name -> name != 0 } }.distinct()
        if (toDelete.isNotEmpty()) gl.deleteTextures(toDelete.toIntArray())
        for (i in 0 until 6) images[i] = null
    }

    /** Texture matrix exposed for shader binders (no fixed-function in ES). */
    fun getTextureMatrix(): FloatArray = textureMatrix

    companion object {
        var sUseCubeMaps: Boolean = true

        /** Current modelview matrix used by [setMatrix]; renderer code updates this each frame. */
        var gGLModelView: FloatArray = FloatArray(16) { if (it % 5 == 0) 1f else 0f }
    }
}

class ImageGL(val width: Int, val height: Int, val components: Int, val useMipMaps: Boolean) {
    private var texName: UInt = 0u
    private var hasMipMaps: Boolean = false
    private var addressMode: AddressMode = AddressMode.CLAMP
    private var filtering: TextureFilterOptions = TextureFilterOptions.BILINEAR

    var target: Int = GL.TEXTURE_2D
    var textureType: TextureType = TextureType.TEXTURE
    var internalFormat: Int = GL.RGBA8

    fun getTexName(): UInt = texName
    fun setTexName(name: UInt) { texName = name }
    fun getWidth(mip: Int = 0): Int = width shr mip
    fun getHeight(mip: Int = 0): Int = height shr mip
    fun getComponents(): Int = components
    fun getUseMipMaps(): Boolean = useMipMaps

    fun setUseMipMaps(v: Boolean) { hasMipMaps = v }
    fun setHasMipMaps(v: Boolean) { hasMipMaps = v }

    fun setTarget(target: Int, texType: TextureType) {
        this.target = target
        this.textureType = texType
    }

    fun setAddressMode(mode: AddressMode) {
        addressMode = mode
        val wrap = when (mode) {
            AddressMode.CLAMP -> GL.CLAMP_TO_EDGE
            AddressMode.MIRROR -> GL.MIRRORED_REPEAT
            AddressMode.WRAP -> GL.REPEAT
        }
        val gl = GpuBackend.current
        gl.texParameteri(target, GL.TEXTURE_WRAP_S, wrap)
        gl.texParameteri(target, GL.TEXTURE_WRAP_T, wrap)
        if (target == GL.TEXTURE_CUBE_MAP || target == GL.TEXTURE_3D) {
            gl.texParameteri(target, GL.TEXTURE_WRAP_R, wrap)
        }
    }

    fun setFilteringOption(opt: TextureFilterOptions) {
        filtering = opt
        val gl = GpuBackend.current
        val (minF, magF) = when (opt) {
            TextureFilterOptions.NEAREST, TextureFilterOptions.POINT -> GL.NEAREST to GL.NEAREST
            TextureFilterOptions.BILINEAR -> GL.LINEAR to GL.LINEAR
            TextureFilterOptions.TRILINEAR -> GL.LINEAR_MIPMAP_LINEAR to GL.LINEAR
            TextureFilterOptions.ANISOTROPIC -> GL.LINEAR_MIPMAP_LINEAR to GL.LINEAR
        }
        gl.texParameteri(target, GL.TEXTURE_MIN_FILTER, minF)
        gl.texParameteri(target, GL.TEXTURE_MAG_FILTER, magF)
        if (opt == TextureFilterOptions.ANISOTROPIC) {
            gl.texParameterf(target, GL.TEXTURE_MAX_ANISOTROPY, 16f)
        }
    }

    fun setSubImage(raw: ImageRaw, x: Int, y: Int, w: UShort, h: UShort) {
        val gl = GpuBackend.current
        gl.bindTexture(target, texName.toInt())
        gl.texSubImage2D(target, 0, x, y, w.toInt(), h.toInt(),
            GL.RGBA, GL.UNSIGNED_BYTE, raw.data)
    }

    fun createGLTexture(mipLevel: Int, raw: ImageRaw, texName: UInt): Boolean {
        this.texName = texName
        val gl = GpuBackend.current
        gl.bindTexture(target, texName.toInt())
        gl.texImage2D(target, mipLevel, internalFormat, raw.width, raw.height,
            GL.RGBA, GL.UNSIGNED_BYTE, raw.data)
        return true
    }
}

class ImageRaw(val width: Int, val height: Int, val components: Int) {
    val data: ByteArray = ByteArray(width * height * components)
}

enum class AddressMode { CLAMP, MIRROR, WRAP }
