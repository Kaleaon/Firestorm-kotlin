package com.firestorm.llimage

object ImageConstants {
    const val MIN_IMAGE_MIP = 2
    const val MAX_IMAGE_MIP = 12
    const val MAX_DISCARD_LEVEL = 5
    const val MIN_IMAGE_SIZE = 1 shl MIN_IMAGE_MIP
    const val MAX_IMAGE_SIZE = 1 shl MAX_IMAGE_MIP
    const val MIN_IMAGE_AREA = MIN_IMAGE_SIZE * MIN_IMAGE_SIZE
    const val MAX_IMAGE_AREA = MAX_IMAGE_SIZE * MAX_IMAGE_SIZE
    const val MAX_IMAGE_COMPONENTS = 8
    const val MAX_IMAGE_DATA_SIZE = MAX_IMAGE_AREA * MAX_IMAGE_COMPONENTS
    const val FIRST_PACKET_SIZE = 600
    const val MAX_IMG_PACKET_SIZE = 1000
    const val HTTP_PACKET_SIZE = 1496

    const val TYPE_NORMAL = 0
    const val TYPE_AVATAR_BAKE = 1

    // JPEG2000 size / quality constraints (from llimage.h)
    const val MAX_DECOMPOSITION_LEVELS = 32   // JPEG2000 spec maximum
    const val MIN_DECOMPOSITION_LEVELS = 5    // SL viewer minimum (crash below this)
    const val MAX_PRECINCT_SIZE = 4096        // bounded by MAX_IMAGE_SIZE
    const val MIN_PRECINCT_SIZE = 4           // must be >= MIN_BLOCK_SIZE
    const val MAX_BLOCK_SIZE = 64             // 64×64 max square block (4096 total)
    const val MIN_BLOCK_SIZE = 4              // JPEG2000 spec minimum
    const val MIN_LAYER_SIZE = 2000           // first quality layer size (> FIRST_PACKET_SIZE)
    const val MAX_NB_LAYERS = 64             // practical SL layer limit
}

// ---------------------------------------------------------------------------
// LLImage — library-level singleton (corresponds to the C++ class LLImage)
// ---------------------------------------------------------------------------

/**
 * Library-wide initialisation state for the image subsystem.
 *
 * Not a Kotlin `object` singleton because image operations are used on worker
 * threads — matches the C++ comment "do not convert to LLSingleton".
 */
object LLImage {
    @Volatile var useNewByteRange: Boolean = false
        private set
    @Volatile var reverseByteRangePercent: Int = 75
        private set

    private val lastThreadError = ThreadLocal<String>()

    fun initClass(useNewByteRange: Boolean = false, minimalReverseByteRangePercent: Int = 75) {
        this.useNewByteRange = useNewByteRange
        this.reverseByteRangePercent = minimalReverseByteRangePercent
    }

    fun cleanupClass() {
        useNewByteRange = false
        reverseByteRangePercent = 75
    }

    fun getLastThreadError(): String = lastThreadError.get() ?: ""
    fun setLastError(message: String) { lastThreadError.set(message) }
}

// ---------------------------------------------------------------------------
// ImageFormatted — abstract base for codec-specific formatted images
// (corresponds to LLImageFormatted in the C++ source)
// ---------------------------------------------------------------------------

/**
 * Abstract base for images that carry a specific compressed encoding.
 *
 * Subclasses implement [encode], [decode], and [updateData].
 * Factory helpers in the companion object mirror the static
 * `LLImageFormatted::createFromType` / `createFromExtension` methods.
 *
 * @param codec The [ImageCodecType] that identifies this subclass.
 */
abstract class ImageFormatted(val codec: ImageCodecType) {

    // ---- raw byte buffer for the compressed stream ---------------------------

    /** Compressed bytes as read from disk or a network packet. */
    var data: ByteArray = ByteArray(0)
        protected set
    val dataSize: Int get() = data.size

    // ---- resolution tracking ------------------------------------------------

    /**
     * Current discard level being worked on.
     *  - 0 = full resolution
     *  - 1 = half resolution (each dimension halved)
     *  - [ImageConstants.MAX_DISCARD_LEVEL] = minimum resolution
     */
    var discardLevel: Byte = -1

    /**
     * Total number of resolution levels in the stream.
     * 0 means unknown (not yet parsed).
     */
    var levels: Byte = 0

    // ---- decode state -------------------------------------------------------

    var isDecoding: Boolean = false
        protected set
    var isDecoded: Boolean = false
        protected set

    // ---- global stats -------------------------------------------------------

    // ---- abstract interface -------------------------------------------------

    /** File extension for this format, lower-case without leading dot. */
    abstract val extension: String

    /**
     * Parse stream metadata (dimensions, components, level count) from [data].
     * Must be called before [decode] or any size queries.
     */
    abstract fun updateData(): Boolean

    /**
     * Decode the first 4 channels of [data] into [rawImage].
     *
     * @param decodeTime  Maximum seconds to spend; 0 = unlimited.
     */
    abstract fun decode(rawImage: ImageRaw, decodeTime: Float = 0f): Boolean

    /**
     * Encode [rawImage] into this format, storing the result in [data].
     *
     * @param encodeTime  Maximum seconds to spend; 0 = unlimited.
     */
    abstract fun encode(rawImage: ImageRaw, encodeTime: Float = 0f): Boolean

    // ---- optional overrides with default behaviour --------------------------

    /** Maximum byte size of the stream header; 0 = no header (read whole file). */
    open fun calcHeaderSize(): Int = 0

    /**
     * Bytes required to reach [discardLevel] (including header).
     * Default: fall back to total [dataSize].
     */
    open fun calcDataSize(discardLevel: Int = 0): Int = dataSize

    /**
     * Smallest valid discard level achievable with [bytes] of data.
     * Default: always returns 0 (full resolution required).
     */
    open fun calcDiscardLevelBytes(bytes: Int): Int = 0

    /**
     * Raw implementation-level discard level.
     * Default delegates to [discardLevel]; overridden by [ImageJ2C].
     */
    open fun getRawDiscardLevel(): Byte = discardLevel

    // ---- error handling (may be overridden for DLL thread-safety) -----------

    open fun resetLastError()                                           { LLImage.setLastError("") }
    open fun setLastError(message: String, filename: String = "")      { LLImage.setLastError(message) }

    // ---- data injection -----------------------------------------------------

    fun setData(newData: ByteArray) {
        data = newData
        updateData()
    }

    fun appendData(extra: ByteArray) {
        data = data + extra
        updateData()
    }

    /** Load compressed data from [filename]. */
    fun load(filename: String, loadSize: Int = 0): Boolean =
        runCatching {
            val bytes = java.io.File(filename).let { f ->
                if (loadSize > 0) f.readBytes().copyOf(loadSize.coerceAtMost(f.length().toInt()))
                else f.readBytes()
            }
            setData(bytes)
            true
        }.getOrElse { e ->
            setLastError(e.message ?: "load failed", filename)
            false
        }

    /** Save compressed [data] to [filename]. */
    fun save(filename: String): Boolean =
        runCatching {
            java.io.File(filename).writeBytes(data)
            true
        }.getOrElse { e ->
            setLastError(e.message ?: "save failed", filename)
            false
        }

    // ---- factory ------------------------------------------------------------

    companion object {
        /** Global count of bytes held in all formatted image instances. */
        var globalFormattedMemory: Int = 0

        fun createFromType(codec: ImageCodecType): ImageFormatted? = when (codec) {
            ImageCodecType.J2C  -> ImageJ2C()
            ImageCodecType.TGA  -> ImageTGA()
            ImageCodecType.PNG  -> PNGWrapper().asImageFormatted()
            else                -> null
        }

        fun createFromExtension(path: String): ImageFormatted? {
            val ext = path.substringAfterLast('.', "").lowercase()
            return createFromType(ImageCodecType.fromExtension(ext))
        }
    }
}

enum class ImageCodecType(val id: Int, val extension: String) {
    INVALID(0, ""),
    RGB(1, "rgb"),
    J2C(2, "j2c"),
    BMP(3, "bmp"),
    TGA(4, "tga"),
    JPEG(5, "jpg"),
    DXT(6, "dxt"),
    PNG(7, "png");

    companion object {
        fun fromExtension(ext: String): ImageCodecType =
            entries.firstOrNull { it.extension == ext.lowercase().trimStart('.') } ?: INVALID
    }
}

class ImageRaw(
    var width: Int,
    var height: Int,
    var components: Int,
    val data: ByteArray = ByteArray(width * height * components)
) {
    constructor() : this(0, 0, 0, ByteArray(0))
    constructor(width: Int, height: Int, components: Int) :
        this(width, height, components, ByteArray(width * height * components))

    val dataSize: Int get() = data.size
    var comment: String = ""

    fun isValid(): Boolean = width > 0 && height > 0 && components > 0 && data.size == width * height * components

    fun clear(r: Byte = 0, g: Byte = 0, b: Byte = 0, a: Byte = -1) {
        var i = 0
        while (i < data.size) {
            when (components) {
                1 -> data[i] = r
                2 -> { data[i] = r; data[i + 1] = a }
                3 -> { data[i] = r; data[i + 1] = g; data[i + 2] = b }
                4 -> { data[i] = r; data[i + 1] = g; data[i + 2] = b; data[i + 3] = a }
            }
            i += components
        }
    }

    fun verticalFlip(): ImageRaw {
        val result = ByteArray(data.size)
        val rowSize = width * components
        for (row in 0 until height) {
            val srcRow = (height - 1 - row) * rowSize
            val dstRow = row * rowSize
            data.copyInto(result, dstRow, srcRow, srcRow + rowSize)
        }
        return ImageRaw(width, height, components, result)
    }

    fun scale(newWidth: Int, newHeight: Int): ImageRaw {
        if (newWidth == width && newHeight == height) return ImageRaw(width, height, components, data.copyOf())
        val dest = ByteArray(newWidth * newHeight * components)
        val xRatio = width.toFloat() / newWidth
        val yRatio = height.toFloat() / newHeight
        for (y in 0 until newHeight) {
            val srcY = (y * yRatio).toInt().coerceIn(0, height - 1)
            for (x in 0 until newWidth) {
                val srcX = (x * xRatio).toInt().coerceIn(0, width - 1)
                val srcIdx = (srcY * width + srcX) * components
                val dstIdx = (y * newWidth + x) * components
                for (c in 0 until components) dest[dstIdx + c] = data[srcIdx + c]
            }
        }
        return ImageRaw(newWidth, newHeight, components, dest)
    }

    fun crop(x: Int, y: Int, cropWidth: Int, cropHeight: Int): ImageRaw {
        val clampX = x.coerceIn(0, width - 1)
        val clampY = y.coerceIn(0, height - 1)
        val clampW = cropWidth.coerceAtMost(width - clampX)
        val clampH = cropHeight.coerceAtMost(height - clampY)
        val dest = ByteArray(clampW * clampH * components)
        for (row in 0 until clampH) {
            val srcOffset = ((clampY + row) * width + clampX) * components
            val dstOffset = row * clampW * components
            data.copyInto(dest, dstOffset, srcOffset, srcOffset + clampW * components)
        }
        return ImageRaw(clampW, clampH, components, dest)
    }

    fun compositeScaled(src: ImageRaw): ImageRaw {
        val scaled = src.scale(width, height)
        return composite(scaled)
    }

    fun composite(src: ImageRaw): ImageRaw {
        require(src.width == width && src.height == height)
        val result = data.copyOf()
        if (src.components == 4 && components == 3) {
            var si = 0; var di = 0
            while (si < src.data.size) {
                val a = (src.data[si + 3].toInt() and 0xFF) / 255f
                val ia = 1f - a
                result[di] = ((src.data[si].toInt() and 0xFF) * a + (result[di].toInt() and 0xFF) * ia).toInt().toByte()
                result[di + 1] = ((src.data[si + 1].toInt() and 0xFF) * a + (result[di + 1].toInt() and 0xFF) * ia).toInt().toByte()
                result[di + 2] = ((src.data[si + 2].toInt() and 0xFF) * a + (result[di + 2].toInt() and 0xFF) * ia).toInt().toByte()
                si += 4; di += 3
            }
        } else {
            src.data.copyInto(result, 0, 0, minOf(src.data.size, result.size))
        }
        return ImageRaw(width, height, components, result)
    }

    fun convertToType(newComponents: Int): ImageRaw {
        if (newComponents == components) return ImageRaw(width, height, components, data.copyOf())
        val dest = ByteArray(width * height * newComponents)
        val pixels = width * height
        for (i in 0 until pixels) {
            val srcBase = i * components
            val dstBase = i * newComponents
            val r = if (components > 0) data.getOrElse(srcBase) { 0 } else 0
            val g = if (components > 1) data.getOrElse(srcBase + 1) { r } else r
            val b = if (components > 2) data.getOrElse(srcBase + 2) { r } else r
            val a = if (components > 3) data.getOrElse(srcBase + 3) { -1 } else -1
            when (newComponents) {
                1 -> dest[dstBase] = r
                2 -> { dest[dstBase] = r; dest[dstBase + 1] = a.toByte() }
                3 -> { dest[dstBase] = r; dest[dstBase + 1] = g; dest[dstBase + 2] = b }
                4 -> { dest[dstBase] = r; dest[dstBase + 1] = g; dest[dstBase + 2] = b; dest[dstBase + 3] = a.toByte() }
            }
        }
        return ImageRaw(width, height, newComponents, dest)
    }

    fun checkHasTransparentPixels(): Boolean {
        if (components < 4) return false
        var i = 3
        while (i < data.size) {
            if (data[i] != (-1).toByte()) return true
            i += components
        }
        return false
    }

    fun resize(newWidth: Int, newHeight: Int, newComponents: Int): ImageRaw =
        scale(newWidth, newHeight).let {
            if (newComponents != components) it.convertToType(newComponents) else it
        }

    companion object {
        fun biasedDimToPowerOfTwo(dim: Int, maxDim: Int = ImageConstants.MAX_IMAGE_SIZE): Int {
            var result = 1
            while (result < dim && result < maxDim) result = result shl 1
            return if (result > maxDim) maxDim else result
        }

        fun expandDimToPowerOfTwo(dim: Int, maxDim: Int = ImageConstants.MAX_IMAGE_SIZE): Int {
            var result = 1
            while (result < dim && result < maxDim) result = result shl 1
            return result
        }

        fun contractDimToPowerOfTwo(dim: Int, minDim: Int = ImageConstants.MIN_IMAGE_SIZE): Int {
            var result = dim
            while (result > minDim && result % 2 == 0) result = result shr 1
            return result.coerceAtLeast(minDim)
        }

        fun calcDownloadPriority(virtualSize: Float, visibleArea: Float, bytesSent: Int): Float {
            if (virtualSize <= 0f || visibleArea <= 0f) return 0f
            return (virtualSize / visibleArea) * (1f - bytesSent.toFloat() / ImageConstants.MAX_IMAGE_DATA_SIZE)
        }
    }
}
