package com.firestorm.llrender

private const val DEFAULT_RESOLUTION: UShort = 64u

class CubeMap(val isSrgb: Boolean) {

    private var textureStage: Int = 0
    private var matrixStage: Int = 0

    val targets: IntArray = intArrayOf(
        GL_TEXTURE_CUBE_MAP_NEGATIVE_X,
        GL_TEXTURE_CUBE_MAP_POSITIVE_X,
        GL_TEXTURE_CUBE_MAP_NEGATIVE_Y,
        GL_TEXTURE_CUBE_MAP_POSITIVE_Y,
        GL_TEXTURE_CUBE_MAP_NEGATIVE_Z,
        GL_TEXTURE_CUBE_MAP_POSITIVE_Z
    )

    private val images: Array<ImageGL?> = arrayOfNulls(6)
    private val rawImages: Array<ImageRaw?> = arrayOfNulls(6)

    fun initGl() {
        if (!sUseCubeMaps) {
            System.err.println("Using cube map without extension!")
            return
        }
        if (images[0] != null) return
        // no-op
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
        // no-op
    }

    fun init(rawImages: List<ImageRaw>) {
        initGl()
        initRawData(rawImages)
        initGlData()
    }

    fun initReflectionMap(resolution: UInt, components: UInt = 3u) {
        // no-op
    }

    fun initEnvironmentMap(rawImages: List<ImageRaw>) {
        require(rawImages.size == 6)
        // no-op
    }

    fun generateMipMaps() {
        // no-op
    }

    fun getGlName(): UInt {
        return 0u
    }

    fun getResolution(): UInt {
        return 0u
    }

    fun bind() {
        // no-op
    }

    fun enable(stage: Int) = enableTexture(stage)

    fun enableTexture(stage: Int) {
        textureStage = stage
        if (stage >= 0 && sUseCubeMaps) {
            // no-op
        }
    }

    fun disable() = disableTexture()

    fun disableTexture() {
        if (textureStage >= 0 && sUseCubeMaps) {
            // no-op
        }
    }

    fun setMatrix(stage: Int) {
        matrixStage = stage
        if (matrixStage < 0) return
        // no-op
    }

    fun restoreMatrix() {
        if (matrixStage < 0) return
        // no-op
    }

    fun destroyGL() {
        for (i in 0 until 6) images[i] = null
    }

    companion object {
        var sUseCubeMaps: Boolean = true

        private const val GL_TEXTURE_CUBE_MAP_NEGATIVE_X = 0x8516
        private const val GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515
        private const val GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 0x8518
        private const val GL_TEXTURE_CUBE_MAP_POSITIVE_Y = 0x8517
        private const val GL_TEXTURE_CUBE_MAP_NEGATIVE_Z = 0x851A
        private const val GL_TEXTURE_CUBE_MAP_POSITIVE_Z = 0x8519
    }
}

class ImageGL(val width: Int, val height: Int, val components: Int, val useMipMaps: Boolean) {
    private var texName: UInt = 0u
    fun getTexName(): UInt = texName
    fun setTexName(name: UInt) { texName = name }
    fun getWidth(mip: Int = 0): Int = width shr mip
    fun getHeight(mip: Int = 0): Int = height shr mip
    fun getComponents(): Int = components
    fun getUseMipMaps(): Boolean = useMipMaps
    fun setUseMipMaps(v: Boolean) { /* no-op */ }
    fun setHasMipMaps(v: Boolean) { /* no-op */ }
    fun setTarget(target: Int, texType: TextureType) { /* no-op */ }
    fun setAddressMode(mode: AddressMode) { /* no-op */ }
    fun setFilteringOption(opt: TextureFilterOptions) { /* no-op */ }
    fun setSubImage(raw: ImageRaw, x: Int, y: Int, w: UShort, h: UShort) { /* no-op */ }
    fun createGLTexture(mipLevel: Int, raw: ImageRaw, texName: UInt): Boolean { return false }
}

class ImageRaw(val width: Int, val height: Int, val components: Int) {
    val data: ByteArray = ByteArray(width * height * components)
}

enum class AddressMode { CLAMP, MIRROR, WRAP }
