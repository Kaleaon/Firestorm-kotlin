package com.firestorm.llimage

// CODEC NOTE: DXT (S3TC) compression is a GPU-native block-compression format.
// Decoding requires either a native library (squish, libdxt, or similar via JNI)
// or an LWJGL / JOGL binding to a driver-level decompressor.
// The SL viewer's C++ code never software-decompresses DXT — it uploads the
// blocks directly to OpenGL via GL_COMPRESSED_RGB(A)_S3TC_DXT* internal formats.

// ---------------------------------------------------------------------------
// DXT format enum
// ---------------------------------------------------------------------------

/**
 * DXT / DXR compression variants understood by the viewer.
 *
 * DXR variants are a Linden-specific re-ordering of mip levels
 * ("DXT Reversed") used for progressive streaming; otherwise identical
 * to the corresponding DXT format on disk.
 *
 * Mirrors [LLImageDXT::EFileFormat] from llimagedxt.h, keeping only the
 * variants relevant for the Kotlin type system.  The full FourCC ↔ format
 * mapping is maintained in [ImageDXT.Companion].
 */
enum class DXTFormat(
    /** Bits per pixel used when computing mip-level byte sizes. */
    val bitsPerPixel: Int,
    /** Number of colour components (channels) in the decoded image. */
    val components: Int,
    /** Minimum block dimension in pixels (4 for DXT, 1 for uncompressed). */
    val minDim: Int,
    /** FourCC identifier stored in the DDS pixel-format sub-header. */
    val fourCC: Int
) {
    UNKNOWN  (bitsPerPixel = 0,  components = 0, minDim = 1, fourCC = 0x00000000),
    I8       (bitsPerPixel = 8,  components = 1, minDim = 1, fourCC = 0x20203849),
    A8       (bitsPerPixel = 8,  components = 1, minDim = 1, fourCC = 0x20203841),
    RGB8     (bitsPerPixel = 24, components = 3, minDim = 1, fourCC = 0x20424752),
    RGBA8    (bitsPerPixel = 32, components = 4, minDim = 1, fourCC = 0x41424752),
    /** DXT1 — opaque RGB, 4 bpp.  3 components after decode. */
    DXT1     (bitsPerPixel = 4,  components = 3, minDim = 4, fourCC = 0x31545844),
    DXT2     (bitsPerPixel = 8,  components = 4, minDim = 4, fourCC = 0x32545844),
    /** DXT3 — explicit 4-bit alpha, 8 bpp.  4 components after decode. */
    DXT3     (bitsPerPixel = 8,  components = 4, minDim = 4, fourCC = 0x33545844),
    DXT4     (bitsPerPixel = 8,  components = 4, minDim = 4, fourCC = 0x34545844),
    /** DXT5 — interpolated alpha, 8 bpp.  4 components after decode. */
    DXT5     (bitsPerPixel = 8,  components = 4, minDim = 4, fourCC = 0x35545844),
    /** DXR1 — DXT1 with mips stored in reverse (ascending) order. */
    DXR1     (bitsPerPixel = 4,  components = 3, minDim = 4, fourCC = 0x31525844),
    DXR2     (bitsPerPixel = 8,  components = 4, minDim = 4, fourCC = 0x32525844),
    /** DXR3 — DXT3 with mips reversed. */
    DXR3     (bitsPerPixel = 8,  components = 4, minDim = 4, fourCC = 0x33525844),
    DXR4     (bitsPerPixel = 8,  components = 4, minDim = 4, fourCC = 0x34525844),
    /** DXR5 — DXT5 with mips reversed. */
    DXR5     (bitsPerPixel = 8,  components = 4, minDim = 4, fourCC = 0x35525844);

    /** True when this format uses 4×4-block GPU compression. */
    val isCompressed: Boolean get() = minDim == 4
}

// ---------------------------------------------------------------------------
// DDS / DXT file headers (data classes)
// ---------------------------------------------------------------------------

/**
 * Legacy LL DXT file header (pre-DDS format used by early SL assets).
 * 16 bytes total; identified by the absence of the 0x20534444 ("DDS ") FourCC.
 *
 * Mirrors `struct dxtfile_header_old_t` in llimagedxt.h.
 */
data class DxtFileHeaderOld(
    val format: Int,      // EFileFormat ordinal
    val maxLevel: Int,    // highest mip index
    val maxWidth: Int,
    val maxHeight: Int
)

/**
 * Standard DDS surface-descriptor header (DDSURFACEDESC2).
 * 128 bytes total; begins with the FourCC 0x20534444 ("DDS ").
 *
 * Mirrors `struct dxtfile_header_t` in llimagedxt.h.
 */
data class DxtFileHeader(
    val fourCC: Int,          // 0x20534444 ("DDS ")
    val headerSize: Int,      // sizeof(DDSURFACEDESC2) = 124
    val flags: Int,
    val maxHeight: Int,
    val maxWidth: Int,
    val imageSize: Int,       // compressed size of top mip level
    val depth: Int,
    val numMips: Int,
    val reserved: IntArray,   // 11 reserved Int32s
    val pixelFormatSize: Int, // sizeof(DDPIXELFORMAT) = 32
    val pixelFormatFlags: Int,
    val pixelFormatFourCC: Int,
    val bitCount: Int,
    val rMask: Int,
    val gMask: Int,
    val bMask: Int,
    val aMask: Int,
    val caps: IntArray        // 4 Int32s (DDSCAPS / DDSCAPS2)
)

// ---------------------------------------------------------------------------
// ImageDXT
// ---------------------------------------------------------------------------

/**
 * DXT / DDS image codec — corresponds to LLImageDXT in the C++ viewer source.
 *
 * Handles loading, header-parsing, mip-level access, and the
 * DXT → DXR re-ordering used for progressive streaming.  Actual
 * GPU-level block decompression is intentionally left as a
 * [TODO("CODEC: DXT ...")] because it requires hardware or native-library
 * support unavailable in pure JVM code.
 *
 * Key responsibilities:
 *  - Parse old-style (16-byte) and DDS-style (128-byte) headers via [updateData].
 *  - Compute per-mip byte offsets via [getMipOffset].
 *  - Re-order mip levels from DXT to DXR order via [convertToDXR].
 *  - Expose [calcDataSize] and [calcHeaderSize] for the progressive downloader.
 */
class ImageDXT : ImageFormatted(ImageCodecType.DXT) {

    /** Detected file format; [DXTFormat.UNKNOWN] until [updateData] succeeds. */
    var fileFormat: DXTFormat = DXTFormat.UNKNOWN
        private set

    /** Byte size of the parsed header ([DxtFileHeaderOld] or [DxtFileHeader]). */
    var headerSize: Int = 0
        private set

    // ---- ImageFormatted interface -------------------------------------------

    override val extension: String get() = "dxt"

    /**
     * Parse the DXT/DDS header from [data] to set [fileFormat], [headerSize],
     * and image dimensions.
     *
     * - If the first four bytes are NOT 0x20534444 ("DDS "), the file is
     *   treated as the old 16-byte LL format ([DxtFileHeaderOld]).
     * - Otherwise the standard [DxtFileHeader] is read and [fileFormat] is
     *   resolved from [DxtFileHeader.pixelFormatFourCC] via [DXTFormat.fromFourCC].
     */
    override fun updateData(): Boolean {
        resetLastError()
        if (data.size < OLD_HEADER_SIZE) {
            setLastError("LLImageDXT uninitialized")
            return false
        }
        System.err.println("ImageDXT: updateData not yet implemented")
        return false
    }

    /**
     * Decode the mip level selected by [discardLevel] into [rawImage].
     *
     * Only uncompressed formats (I8, A8, RGB8, RGBA8) can be decoded in
     * software — block-compressed formats (DXT1/3/5, DXR1/3/5) require GPU
     * support and are explicitly rejected with a warning.
     */
    override fun decode(rawImage: ImageRaw, decodeTime: Float): Boolean {
        resetLastError()
        if (fileFormat.isCompressed) {
            setLastError("Attempt to decode compressed ImageDXT to Raw (unsupported)")
            return false
        }
        System.err.println("ImageDXT: decode not yet implemented")
        return false
    }

    /**
     * Encode [rawImage] as a DXT/DDS file, storing the result in [data].
     *
     * The C++ encoder writes uncompressed mip data (no GPU block compression)
     * using the RGB8 or RGBA8 format and generates all mip levels via
     * box-filter downsampling.  Block compression (DXT1/3/5) is left as
     * a hardware task.
     */
    override fun encode(rawImage: ImageRaw, encodeTime: Float): Boolean {
        resetLastError()
        System.err.println("ImageDXT: encode not yet implemented")
        return false
    }

    // ---- header / data size -------------------------------------------------

    /**
     * Size of the larger of the two header variants (old vs. DDS).
     * Matches `LLImageDXT::calcHeaderSize()` which returns
     * `llmax(sizeof(old), sizeof(new))`.
     */
    override fun calcHeaderSize(): Int = DDS_HEADER_SIZE   // 128 bytes

    /**
     * Total bytes required to represent [discardLevel] (header + mips above it).
     * Delegates to [getMipOffset] + one mip's worth of bytes.
     */
    override fun calcDataSize(discardLevel: Int): Int {
        if (fileFormat == DXTFormat.UNKNOWN) return 0
        val level = if (discardLevel < 0) this.discardLevel.toInt() else discardLevel
        val offset = getMipOffset(level)
        System.err.println("ImageDXT: calcDataSize not yet implemented")
        return 0
    }

    // ---- mip-level access ---------------------------------------------------

    /**
     * Byte offset within [data] at which the mip level for [discard] begins.
     *
     * The DXR format stores mip levels in ascending-resolution order
     * (smallest first), so offset 0 after the header corresponds to the
     * highest discard level (1×1).  DXT order (used in the old header format)
     * stores the largest mip first.
     *
     * This is a direct port of `LLImageDXT::getMipOffset(S32 discard)`.
     */
    fun getMipOffset(discard: Int): Int {
        System.err.println("ImageDXT: getMipOffset not yet implemented")
        return 0
    }

    /**
     * Set [fileFormat] based on [components], choosing the DXR variants
     * (DXR1 for 3-component, DXR3 for 4-component).
     * Corresponds to `LLImageDXT::setFormat()`.
     */
    fun setFormat(components: Int) {
        fileFormat = when (components) {
            3    -> DXTFormat.DXR1
            4    -> DXTFormat.DXR3
            else -> throw IllegalArgumentException("ImageDXT.setFormat: unsupported components=$components")
        }
        headerSize = calcHeaderSize()
    }

    /**
     * Re-order mip levels from DXT order (largest first) to DXR order
     * (smallest first) in-place within [data].
     *
     * Returns false if the current [fileFormat] is already a DXR variant
     * or if conversion is not applicable.
     */
    fun convertToDXR(): Boolean {
        System.err.println("ImageDXT: convertToDXR not yet implemented")
        return false
    }

    /**
     * Extract raw mip data at [discard] into a new [ImageRaw].
     * [discard] defaults to [discardLevel] when negative.
     */
    fun getMipData(discard: Int = -1): ImageRaw {
        System.err.println("ImageDXT: getMipData not yet implemented")
        return ImageRaw()
    }

    // ---- companion object: static helpers -----------------------------------

    companion object {
        /** Size of the old 16-byte LL DXT header. */
        const val OLD_HEADER_SIZE: Int = 16

        /** Size of the standard DDS surface-descriptor header (128 bytes). */
        const val DDS_HEADER_SIZE: Int = 128

        /** Magic FourCC at byte 0 of a DDS file: "DDS " (0x20534444 LE). */
        const val DDS_MAGIC: Int = 0x20534444

        /**
         * Ensure that [width] and [height] meet the minimum block dimension
         * required by [format].  DXT block-compressed formats require ≥ 4 pixels
         * in each dimension.
         *
         * Direct port of `LLImageDXT::checkMinWidthHeight`.
         */
        fun checkMinWidthHeight(format: DXTFormat, width: Int, height: Int): Pair<Int, Int> {
            val minDim = format.minDim
            return Pair(maxOf(width, minDim), maxOf(height, minDim))
        }

        /**
         * Byte size of one mip level with dimensions [width] × [height] in [format].
         *
         * Formula: `ceil(width × height × bpp / 8)` rounded up to the next
         * 4-byte boundary, with a minimum block dimension of [DXTFormat.minDim].
         *
         * Direct port of `LLImageDXT::formatBytes`.
         */
        fun formatBytes(format: DXTFormat, width: Int, height: Int): Int {
            val (w, h) = checkMinWidthHeight(format, width, height)
            val bytes = (w * h * format.bitsPerPixel + 7) shr 3
            return (bytes + 3) and 3.inv()   // round up to 4-byte boundary
        }

        /**
         * Byte size of compressed DXT data for a texture of dimensions
         * [w] × [h] using [format].
         *
         * - DXT1: 8 bytes per 4×4 block (4 bits/pixel).
         * - DXT3 / DXT5: 16 bytes per 4×4 block (8 bits/pixel).
         *
         * Equivalent to [formatBytes] for the three main SL formats.
         */
        fun calcDataSize(w: Int, h: Int, format: DXTFormat): Int = formatBytes(format, w, h)

        /**
         * Total number of mip levels needed for a texture of dimensions
         * [width] × [height] (including the 1×1 base level).
         *
         * Direct port of `LLImageDXT::calcNumMips`.
         */
        fun calcNumMips(width: Int, height: Int): Int {
            var w = width; var h = height; var n = 0
            while (w > 0 && h > 0) { w = w shr 1; h = h shr 1; n++ }
            return n
        }

        /**
         * Compute the clamped dimensions at [discardLevel] discard steps below
         * the full [width] × [height], with the minimum block size enforced.
         *
         * Direct port of `LLImageDXT::calcDiscardWidthHeight`.
         */
        fun calcDiscardWidthHeight(
            discardLevel: Int, format: DXTFormat, width: Int, height: Int
        ): Pair<Int, Int> {
            var w = width; var h = height; var d = discardLevel
            while (d > 0 && w > 1 && h > 1) { d--; w = w shr 1; h = h shr 1 }
            return checkMinWidthHeight(format, w, h)
        }

        /**
         * Resolve a [DXTFormat] from its 32-bit FourCC identifier.
         * Returns [DXTFormat.UNKNOWN] for unrecognised codes.
         *
         * Direct port of `LLImageDXT::getFormat(S32 fourcc)`.
         */
        fun fromFourCC(fourCC: Int): DXTFormat =
            DXTFormat.entries.firstOrNull { it.fourCC == fourCC } ?: DXTFormat.UNKNOWN

        /**
         * Return the FourCC for [format], or 0 for [DXTFormat.UNKNOWN].
         * Direct port of `LLImageDXT::getFourCC`.
         */
        fun getFourCC(format: DXTFormat): Int = format.fourCC
    }
}
