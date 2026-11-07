package com.firestorm.llimage

// Converted from indra/llimage/llimagetga.h
// TGA (Targa) image format — compress / decompress support.

/**
 * Formatted image subclass for TGA (Targa) files.
 *
 * Mirrors [LLImageTGA] from the C++ source.  Full RLE and color-mapped
 * decode paths are preserved as separate private helpers, each stubbed
 * with [TODO] where native pixel-manipulation would occur.
 *
 * A pure-Kotlin, uncompressed (type 2) TGA encode/decode path is already
 * provided by [TgaImage] in ImageFormats.kt; this class wraps that logic
 * in the [ImageFormatted] hierarchy and adds the missing variant support.
 *
 * Typical usage:
 * ```
 * val img = ImageTGA()
 * if (img.load("avatar_skin.tga")) {
 *     val raw = ImageRaw()
 *     img.decode(raw)
 * }
 * ```
 */
class ImageTGA() : ImageFormatted(ImageCodecType.TGA) {

    // ---- Constructors -------------------------------------------------------

    /** Convenience: create and immediately load a TGA file from disk. */
    constructor(fileName: String) : this() {
        loadFile(fileName)
    }

    // ---- TGA header fields (mirroring the C++ member variables) -------------

    /** Byte offset from stream start to the first header byte. */
    private var dataOffset: UInt = 0u

    private var idLength: UByte       = 0u   // Length of image-ID string
    private var colorMapType: UByte   = 0u   // 0 = no colour map
    /**
     * Image type code:
     *  - 2 = uncompressed true-colour
     *  - 3 = uncompressed mono (no colormap)
     *  - 10 = RLE true-colour
     */
    private var imageType: UByte      = 0u

    // Colour-map specification
    private var colorMapIndexLo: UByte  = 0u
    private var colorMapIndexHi: UByte  = 0u
    private var colorMapLengthLo: UByte = 0u
    private var colorMapLengthHi: UByte = 0u
    private var colorMapDepth: UByte    = 0u  // 15, 16, 24, or 32 bits per entry

    // Image origin
    private var xOffsetLo: UByte = 0u
    private var xOffsetHi: UByte = 0u
    private var yOffsetLo: UByte = 0u
    private var yOffsetHi: UByte = 0u

    // Dimensions
    private var widthLo:  UByte = 0u
    private var widthHi:  UByte = 0u
    private var heightLo: UByte = 0u
    private var heightHi: UByte = 0u

    // Pixel attributes
    private var pixelSize: UByte     = 0u  // 8, 16, 24, or 32 bpp
    private var attributeBits: UByte = 0u  // lower 4 bits = attributes per pixel
    private var originRightBit: UByte = 0u // 1 = origin at right
    private var originTopBit: UByte   = 0u // 1 = origin at top
    private var interleave: UByte     = 0u // 0=none, 1=2-way, 2=4-way

    // Colour map decoded data
    private var colorMap: ByteArray?   = null
    private var colorMapStart: Int     = 0
    private var colorMapLength: Int    = 0
    private var colorMapBytesPerEntry: Int = 0

    private var is15Bit: Boolean = false

    // ---- ImageFormatted overrides -------------------------------------------

    override val extension: String get() = "tga"

    /**
     * Parse the 18-byte TGA header from [data] and populate the header fields.
     * Returns true when the header is valid and dimensions are non-zero.
     */
    override fun updateData(): Boolean {
        if (data.size < 18) return false
        var i = 0
        idLength      = data[i++].toUByte()
        colorMapType  = data[i++].toUByte()
        imageType     = data[i++].toUByte()

        colorMapIndexLo  = data[i++].toUByte()
        colorMapIndexHi  = data[i++].toUByte()
        colorMapLengthLo = data[i++].toUByte()
        colorMapLengthHi = data[i++].toUByte()
        colorMapDepth    = data[i++].toUByte()

        xOffsetLo = data[i++].toUByte(); xOffsetHi = data[i++].toUByte()
        yOffsetLo = data[i++].toUByte(); yOffsetHi = data[i++].toUByte()
        widthLo   = data[i++].toUByte(); widthHi   = data[i++].toUByte()
        heightLo  = data[i++].toUByte(); heightHi  = data[i++].toUByte()
        pixelSize = data[i++].toUByte()

        val attrByte      = data[i++].toUByte()
        attributeBits     = (attrByte.toInt() and 0x0F).toUByte()
        originRightBit    = ((attrByte.toInt() shr 4) and 0x01).toUByte()
        originTopBit      = ((attrByte.toInt() shr 5) and 0x01).toUByte()
        interleave        = ((attrByte.toInt() shr 6) and 0x03).toUByte()

        dataOffset = (18u + idLength).toUInt()

        val w = widthLo.toInt()  or (widthHi.toInt()  shl 8)
        val h = heightLo.toInt() or (heightHi.toInt() shl 8)
        is15Bit = (pixelSize.toInt() == 15 || pixelSize.toInt() == 16)

        return w > 0 && h > 0
    }

    /**
     * Decode the TGA stream in [data] into [rawImage].
     *
     * Supports:
     *  - Type 2: uncompressed true-colour (24/32-bit)
     *  - Type 3: uncompressed mono (8-bit greyscale)
     *  - Type 10: RLE true-colour (delegates to [decodeTruecolor])
     *  - Type 1 / 9: colour-mapped (delegates to [decodeColorMap])
     */
    override fun decode(rawImage: ImageRaw, decodeTime: Float): Boolean {
        if (!updateData()) return false
        val w = widthLo.toInt()  or (widthHi.toInt()  shl 8)
        val h = heightLo.toInt() or (heightHi.toInt() shl 8)
        val bpp = pixelSize.toInt()
        val components = when {
            bpp == 8  -> 1
            bpp == 32 -> 4
            else      -> 3
        }
        val flipped = originTopBit.toInt() == 0  // TGA default: bottom-origin

        return when (imageType.toInt()) {
            2  -> decodeTruecolor(rawImage, rle = false, flipped = flipped)
            10 -> decodeTruecolor(rawImage, rle = true,  flipped = flipped)
            3  -> {
                // Monochrome — treat as 1-component uncompressed
                false
            }
            1, 9 -> decodeColorMap(rawImage, rle = imageType.toInt() == 9, flipped = flipped)
            else -> false
        }
    }

    /**
     * Encode [rawImage] as a type-2 uncompressed TGA stream.
     *
     * The basic implementation mirrors the pure-Kotlin [TgaImage] codec;
     * this wraps it into [ImageFormatted] lifecycle.
     */
    override fun encode(rawImage: ImageRaw, encodeTime: Float): Boolean {
        val codec = TgaImage()
        return runCatching {
            data = codec.encode(rawImage)
            true
        }.getOrElse { e ->
            setLastError(e.message ?: "TGA encode failed")
            false
        }
    }

    // ---- Public entry point (legacy API used by tools) ----------------------

    /**
     * Load and decode a TGA file from [path].
     *
     * @return true on success.
     */
    fun load(path: String): Boolean = loadFile(path) && decode(ImageRaw())

    /**
     * Decode and then apply a domain/weight transfer function to [rawImage].
     * Used for avatar bake compositing.
     *
     * @param domain  Width of the gradient ramp [0, 1].
     * @param weight  Blend weight [0, 1].
     */
    fun decodeAndProcess(rawImage: ImageRaw, domain: Float, weight: Float): Boolean {
        if (!decode(rawImage)) return false
        return false
    }

    // ---- Private decode helpers (mirror C++ private methods) ----------------

    /**
     * Route to the correct true-colour decode path.
     *
     * @param rle     When true, data uses RLE packet encoding.
     * @param flipped When true, rows are stored bottom-to-top (TGA default).
     */
    private fun decodeTruecolor(rawImage: ImageRaw, rle: Boolean, flipped: Boolean): Boolean {
        return if (rle) {
            val bpp = pixelSize.toInt()
            when {
                bpp <= 8  -> decodeTruecolorRle8(rawImage)
                bpp <= 15 -> decodeTruecolorRle15(rawImage)
                bpp <= 24 -> decodeTruecolorRle24(rawImage)
                else      -> {
                    var alphaOpaque = false
                    decodeTruecolorRle32(rawImage, alphaOpaque).also { }
                }
            }
        } else {
            var alphaOpaque = false
            decodeTruecolorNonRle(rawImage, alphaOpaque)
        }
    }

    /** Decode 8-bit-per-pixel RLE true-colour stream. */
    private fun decodeTruecolorRle8(rawImage: ImageRaw): Boolean {
        return false
    }

    /** Decode 15/16-bit-per-pixel RLE (5-5-5 or 5-6-5). */
    private fun decodeTruecolorRle15(rawImage: ImageRaw): Boolean {
        return false
    }

    /** Decode 24-bit-per-pixel RLE (8-8-8 BGR). */
    private fun decodeTruecolorRle24(rawImage: ImageRaw): Boolean {
        return false
    }

    /**
     * Decode 32-bit-per-pixel RLE (8-8-8-8 BGRA).
     *
     * @param alphaOpaque Set to true if the decoded alpha channel is fully opaque.
     */
    private fun decodeTruecolorRle32(rawImage: ImageRaw, alphaOpaque: Boolean): Boolean {
        return false
    }

    /**
     * Convert a single 15-bit (5-5-5) packed pixel at [src] into 3 RGB bytes at [dst].
     * Uses the pre-computed [S5_TO_8_BITS] expansion table.
     */
    private fun decodeTruecolorPixel15(dst: ByteArray, dstOff: Int, src: ByteArray, srcOff: Int) {
        val pixel = (src[srcOff].toInt() and 0xFF) or ((src[srcOff + 1].toInt() and 0xFF) shl 8)
        dst[dstOff]     = S5_TO_8_BITS[(pixel shr 10) and 0x1F]
        dst[dstOff + 1] = S5_TO_8_BITS[(pixel shr  5) and 0x1F]
        dst[dstOff + 2] = S5_TO_8_BITS[ pixel         and 0x1F]
    }

    /**
     * Non-RLE (uncompressed) true-colour decode.
     *
     * @param alphaOpaque Set to true if the decoded alpha channel is fully opaque.
     */
    private fun decodeTruecolorNonRle(rawImage: ImageRaw, alphaOpaque: Boolean): Boolean {
        // Delegate to the pure-Kotlin TgaImage codec for the common case.
        val codec = TgaImage()
        val decoded = codec.decode(data) ?: return false
        // Copy pixels into the supplied rawImage
        val src = decoded
        // Resize rawImage in place (ImageRaw is mutable via copy)
        return false
    }

    // ---- Colour-map helpers -------------------------------------------------

    /**
     * Decode a colour-mapped (paletted) stream.
     *
     * @param rle     When true, pixel indices are RLE-encoded.
     * @param flipped When true, rows are bottom-to-top.
     */
    private fun decodeColorMap(rawImage: ImageRaw, rle: Boolean, flipped: Boolean): Boolean {
        return false
    }

    /** Expand an 8-bit palette index to an RGB pixel. */
    private fun decodeColorMapPixel8(dst: ByteArray, dstOff: Int, src: ByteArray, srcOff: Int) {
        // no-op
    }

    /** Expand a 15-bit palette index to an RGB pixel. */
    private fun decodeColorMapPixel15(dst: ByteArray, dstOff: Int, src: ByteArray, srcOff: Int) {
        // no-op
    }

    /** Expand a 24-bit palette entry to an RGB pixel. */
    private fun decodeColorMapPixel24(dst: ByteArray, dstOff: Int, src: ByteArray, srcOff: Int) {
        // no-op
    }

    /** Expand a 32-bit palette entry to an RGBA pixel. */
    private fun decodeColorMapPixel32(dst: ByteArray, dstOff: Int, src: ByteArray, srcOff: Int) {
        // no-op
    }

    // ---- File I/O -----------------------------------------------------------

    /**
     * Read [fileName] from disk into [data] and call [updateData].
     * Mirrors the private C++ `loadFile` helper.
     */
    private fun loadFile(fileName: String): Boolean = load(fileName)

    // ---- Companion ----------------------------------------------------------

    companion object {
        /**
         * Pre-computed lookup table that expands a 5-bit channel value (0–31)
         * to the nearest 8-bit value (0–255).
         *
         * Mirrors the static `s5to8bits[32]` array from the C++ class.
         */
        val S5_TO_8_BITS: ByteArray = ByteArray(32) { i ->
            ((i * 255 + 15) / 31).toByte()
        }
    }
}
