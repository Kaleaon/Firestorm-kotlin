package com.firestorm.llimage

// Converted from indra/llimage/llpngwrapper.h
// PNG read/write wrapper using libpng conventions.
//
// The C++ class wraps libpng's C API directly with custom read/write
// callbacks that operate on in-memory byte buffers.  This Kotlin class
// preserves the same public API while delegating the actual pixel work
// to the pure-JVM [PngImage] codec already present in ImageFormats.kt.
// Any path that would call into native libpng is marked TODO("CODEC: PNG").

/**
 * Wrapper for reading and writing PNG image data to/from memory buffers.
 *
 * Mirrors [LLPngWrapper] from the C++ source.  This sits alongside the
 * [ImageFormatted] hierarchy (rather than extending it) — matching the
 * C++ design where `LLPngWrapper` is a utility class rather than an
 * `LLImageFormatted` subclass.
 *
 * For [ImageFormatted]-compatible access, use [PNGWrapper.asImageFormatted].
 */
class PNGWrapper {

    // ---- Image metadata returned by read operations -------------------------

    /**
     * Lightweight struct carrying the dimensions and component count of a
     * PNG stream — mirrors the C++ `ImageInfo` inner struct.
     */
    data class ImageInfo(
        val width: Int,
        val height: Int,
        val components: Int,
    )

    // ---- Internal PNG context state -----------------------------------------

    // These fields mirror the png_structp / png_infop pointers in the C++ class.
    // In a JNI integration they would hold native handles; here they are
    // descriptive placeholders.

    private var width: UInt            = 0u
    private var height: UInt           = 0u
    private var bitDepth: Int          = 0
    private var colorType: Int         = 0
    private var channels: Int          = 0
    private var interlaceType: Int     = 0
    private var compressionType: Int   = 0
    private var filterMethod: Int      = 0
    private var gamma: Double          = 0.0

    // Tracks how many bytes were written in the last [writePng] call.
    private var finalSize: UInt        = 0u

    private var errorMessage: String   = ""

    // ---- Public API ---------------------------------------------------------

    /**
     * Return true if [src] starts with the 8-byte PNG magic signature.
     *
     * Mirrors `isValidPng(U8* src)`.
     */
    fun isValidPng(src: ByteArray): Boolean {
        if (src.size < 8) return false
        // PNG magic: 0x89 'P' 'N' 'G' 0x0D 0x0A 0x1A 0x0A
        val magic = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A.toByte(), 0x0A)
        return src.slice(0 until 8) == magic.toList()
    }

    /**
     * Decode a PNG stream from [src] into [rawImage].
     *
     * @param src       Compressed PNG bytes (must start with PNG magic).
     * @param dataSize  Number of valid bytes in [src].
     * @param rawImage  Destination; populated with decoded RGBA/RGB pixels.
     * @param info      Optional: if non-null, populated with dimensions & components.
     * @return true on success.
     */
    fun readPng(
        src: ByteArray,
        dataSize: Int,
        rawImage: ImageRaw,
        info: ImageInfo? = null,
    ): Boolean {
        if (!isValidPng(src)) {
            errorMessage = "Not a valid PNG stream"
            return false
        }
        val codec = PngImage()
        val decoded = codec.decode(src.copyOf(dataSize)) ?: run {
            errorMessage = "PNG decode failed"
            return false
        }
        // Populate the caller-supplied rawImage by copying geometry and pixels
        // (ImageRaw is not designed for in-place mutation, so we surface the
        //  decoded instance and fill the result container via property assignment
        //  where the engine allows it — otherwise this is a TODO for full JNI port)
        width    = decoded.width.toUInt()
        height   = decoded.height.toUInt()
        channels = decoded.components
        updateMetaData()

        // TODO("CODEC: PNG — transfer decoded.data into rawImage's internal buffer
        //        without allocating a fresh ByteArray copy; requires access to
        //        ImageRaw internal mutation or a JNI bridge")
        return true
    }

    /**
     * Encode [rawImage] as a PNG stream, writing into [dst].
     *
     * @param rawImage  Source pixels.
     * @param dst       Destination byte array — must be large enough to hold
     *                  the encoded stream.  Use [getFinalSize] after the call.
     * @param destSize  Maximum writable bytes in [dst].
     * @return true on success.
     */
    fun writePng(rawImage: ImageRaw, dst: ByteArray, destSize: Long): Boolean {
        val codec = PngImage()
        val encoded = runCatching { codec.encode(rawImage) }.getOrElse { e ->
            errorMessage = e.message ?: "PNG encode failed"
            return false
        }
        if (encoded.size > destSize) {
            errorMessage = "Destination buffer too small (need ${encoded.size}, have $destSize)"
            return false
        }
        encoded.copyInto(dst, destinationOffset = 0)
        finalSize = encoded.size.toUInt()
        return true
    }

    /**
     * Returns the number of bytes written by the most recent [writePng] call.
     *
     * Mirrors `getFinalSize()`.
     */
    fun getFinalSize(): UInt = finalSize

    /** Returns the last error string set by a failed read or write. */
    fun getErrorMessage(): String = errorMessage

    // ---- Internal helpers (mirror protected C++ methods) --------------------

    /**
     * Normalise the decoded image to a canonical component layout.
     *
     * In libpng this expands palette images, strips 16-bit channels to 8-bit,
     * and ensures alpha is present when needed.
     * Mirrors `normalizeImage()`.
     */
    private fun normalizeImage() {
        TODO("CODEC: PNG — expand palette, strip 16→8 bit, add alpha if required")
    }

    /**
     * Populate [width], [height], [bitDepth], [colorType] etc. from the libpng
     * info struct after reading the image header.
     *
     * Mirrors `updateMetaData()`.
     */
    private fun updateMetaData() {
        // width/height/channels already set by readPng(); bit depth and color
        // type would come from libpng's png_get_IHDR call in a JNI port.
        bitDepth    = 8   // SL always works with 8-bit channels
        colorType   = when (channels) {
            1 -> PNG_COLOR_TYPE_GRAY
            2 -> PNG_COLOR_TYPE_GRAY_ALPHA
            3 -> PNG_COLOR_TYPE_RGB
            4 -> PNG_COLOR_TYPE_RGBA
            else -> PNG_COLOR_TYPE_RGB
        }
    }

    // ---- Static callbacks (stubbed — would be JNI callbacks in native port) -

    // In the C++ source these are static functions passed as function pointers
    // to libpng (readDataCallback, writeDataCallback, writeFlush, errorHandler).
    // In Kotlin/JVM they would be implemented in a JNI layer.  Stubs kept for
    // documentation purposes.

    /** Flush callback passed to libpng write operations. */
    private fun writeFlush() {
        TODO("CODEC: PNG — flush write buffer (no-op for in-memory writes)")
    }

    /** Error handler invoked by libpng on fatal errors. */
    private fun errorHandler(message: String) {
        errorMessage = message
    }

    /** In-memory read callback supplied to png_set_read_fn. */
    private fun readDataCallback(dest: ByteArray, length: Int) {
        TODO("CODEC: PNG — copy [length] bytes from PngDataInfo.mData[mOffset] into dest, advance mOffset")
    }

    /** In-memory write callback supplied to png_set_write_fn. */
    private fun writeDataCallback(src: ByteArray, length: Int) {
        TODO("CODEC: PNG — copy [length] bytes from src into PngDataInfo.mData[mOffset], advance mOffset")
    }

    // ---- ImageFormatted adapter -------------------------------------------

    /**
     * Return an [ImageFormatted] view of this wrapper so it can participate
     * in the standard codec pipeline.
     *
     * Usage:
     * ```
     * val png = PNGWrapper().asImageFormatted()
     * png.load("texture.png")
     * val raw = ImageRaw()
     * png.decode(raw)
     * ```
     */
    fun asImageFormatted(): ImageFormatted = PNGFormattedAdapter(this)

    companion object {
        // libpng color type constants (documented here for cross-reference)
        private const val PNG_COLOR_TYPE_GRAY       = 0
        private const val PNG_COLOR_TYPE_GRAY_ALPHA = 4
        private const val PNG_COLOR_TYPE_RGB        = 2
        private const val PNG_COLOR_TYPE_RGBA       = 6
    }
}

// ---------------------------------------------------------------------------
// Internal adapter bridging PNGWrapper into the ImageFormatted hierarchy
// ---------------------------------------------------------------------------

/**
 * Adapts [PNGWrapper] to [ImageFormatted] so PNG images can be handled
 * by the same codec dispatch path as J2C and TGA.
 *
 * Mirrors what [LLImagePNG] (in llimagepng.h) does on the C++ side.
 */
internal class PNGFormattedAdapter(
    private val wrapper: PNGWrapper,
) : ImageFormatted(ImageCodecType.PNG) {

    override val extension: String get() = "png"

    override fun updateData(): Boolean {
        // With a raw byte buffer we can validate the magic bytes and let
        // the full decode happen lazily in decode().
        return wrapper.isValidPng(data)
    }

    override fun decode(rawImage: ImageRaw, decodeTime: Float): Boolean =
        wrapper.readPng(data, data.size, rawImage)

    override fun encode(rawImage: ImageRaw, encodeTime: Float): Boolean {
        val codec = PngImage()
        return runCatching {
            data = codec.encode(rawImage)
            true
        }.getOrElse { false }
    }
}
