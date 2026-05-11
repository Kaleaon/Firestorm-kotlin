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
        TODO("GPU: LLImageGL.generateTextures(1); for each face create ImageGL/ImageRaw at DEFAULT_RESOLUTION; set sRGB format if isSrgb; setTarget(TT_CUBE_MAP); createGLTexture; setAddressMode(TAM_CLAMP); disable tex unit")
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
        TODO("GPU: for each face call setSubImage on images[i] using rawImages[i] at DEFAULT_RESOLUTION")
    }

    fun init(rawImages: List<ImageRaw>) {
        initGl()
        initRawData(rawImages)
        initGlData()
    }

    fun initReflectionMap(resolution: UInt, components: UInt = 3u) {
        TODO("GPU: generateTextures(1); create images[0] at resolution; setTexName; setTarget TT_CUBE_MAP; bindManual; setAddressMode TAM_CLAMP")
    }

    fun initEnvironmentMap(rawImages: List<ImageRaw>) {
        require(rawImages.size == 6)
        TODO("GPU: generateTextures(1); for each face create ImageGL; createGLTexture; bindManual; setAddressMode; setSubImage; enableTexture(0); bind; setFilteringOption ANISOTROPIC; glEnable GL_TEXTURE_CUBE_MAP_SEAMLESS; glGenerateMipmap; disable")
    }

    fun generateMipMaps() {
        TODO("GPU: setUseMipMaps/setHasMipMaps on images[0]; enableTexture(0); bind; setFilteringOption BILINEAR; glGenerateMipmap GL_TEXTURE_CUBE_MAP; disable")
    }

    fun getGlName(): UInt {
        TODO("GPU: return images[0].getTexName()")
    }

    fun getResolution(): UInt {
        TODO("GPU: return images[0]?.getWidth(0) ?: 0u")
    }

    fun bind() {
        TODO("GPU: gGL.getTexUnit(textureStage).bind(this)")
    }

    fun enable(stage: Int) = enableTexture(stage)

    fun enableTexture(stage: Int) {
        textureStage = stage
        if (stage >= 0 && sUseCubeMaps) {
            TODO("GPU: gGL.getTexUnit(stage).enable(TT_CUBE_MAP)")
        }
    }

    fun disable() = disableTexture()

    fun disableTexture() {
        if (textureStage >= 0 && sUseCubeMaps) {
            TODO("GPU: gGL.getTexUnit(textureStage).disable(); if stage==0 re-enable TT_TEXTURE")
        }
    }

    fun setMatrix(stage: Int) {
        matrixStage = stage
        if (matrixStage < 0) return
        TODO("GPU: activate tex unit; extract rotation 3x3 from gGLModelView; transpose; push as texture matrix")
    }

    fun restoreMatrix() {
        if (matrixStage < 0) return
        TODO("GPU: activate tex unit matrixStage; gGL.matrixMode(MM_TEXTURE); gGL.popMatrix(); gGL.matrixMode(MM_MODELVIEW)")
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
    fun setUseMipMaps(v: Boolean) { TODO("GPU: store mip flag") }
    fun setHasMipMaps(v: Boolean) { TODO("GPU: store hasMipMaps flag") }
    fun setTarget(target: Int, texType: TextureType) { TODO("GPU: store GL target and texture type") }
    fun setAddressMode(mode: AddressMode) { TODO("GPU: glTexParameteri wrap mode") }
    fun setFilteringOption(opt: TextureFilterOptions) { TODO("GPU: glTexParameteri filter") }
    fun setSubImage(raw: ImageRaw, x: Int, y: Int, w: UShort, h: UShort) { TODO("GPU: glTexSubImage2D") }
    fun createGLTexture(mipLevel: Int, raw: ImageRaw, texName: UInt): Boolean { TODO("GPU: bind and upload texture data") }
}

class ImageRaw(val width: Int, val height: Int, val components: Int) {
    val data: ByteArray = ByteArray(width * height * components)
}

enum class AddressMode { CLAMP, MIRROR, WRAP }
