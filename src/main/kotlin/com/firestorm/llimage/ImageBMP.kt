package com.firestorm.llimage

// CODEC NOTE: The C++ implementation hand-parses the BMP file format without
// any external library.  A JVM replacement can use javax.imageio.ImageIO
// (which supports BMP natively via the built-in com.sun.imageio.plugins.bmp
// plugin) or parse the binary header manually to match the C++ behaviour
// exactly (including the bottom-up / top-down origin flag).

// ---------------------------------------------------------------------------
// BMP file-format data classes
// ---------------------------------------------------------------------------

/**
 * Windows BMP file header — the first 14 bytes of every BMP file.
 *
 * Mirrors the manually-parsed "Part 1: File Header" block in llimagebmp.cpp.
 *
 * @param signature   2-byte magic: 0x4D42 ("BM") for Windows BMP.
 * @param fileSize    Total file size in bytes (little-endian in the file).
 * @param reserved    Always 0 in well-formed files.
 * @param dataOffset  Byte offset from the start of the file to the pixel array.
 */
data class BmpHeader(
    val signature: Int,   // 'B'=0x42, 'M'=0x4D → stored as little-endian UInt16
    val fileSize: Int,
    val reserved: Int,
    val dataOffset: Int
)

/**
 * Windows BITMAPINFOHEADER — the 40-byte DIB (Device-Independent Bitmap) header
 * that immediately follows [BmpHeader] in a Windows 3.x / NT BMP file.
 *
 * Mirrors `struct LLBMPHeader` in llimagebmp.cpp.
 *
 * @param size              Size of this structure in bytes (40 for BITMAPINFOHEADER).
 * @param width             Image width in pixels.
 * @param height            Image height in pixels; negative → origin at top.
 * @param planes            Number of colour planes; must be 1.
 * @param bitCount          Bits per pixel: 1, 4, 8, 16, 24, or 32.
 * @param compression       Compression type: 0 = BI_RGB, 3 = BI_BITFIELDS.
 * @param imageSize         Size of the pixel array in bytes; 0 is valid for BI_RGB.
 * @param horzPelsPerMeter  Horizontal resolution of the target device (pixels/metre).
 * @param vertPelsPerMeter  Vertical resolution of the target device (pixels/metre).
 * @param numColors         Number of colours in the colour table (0 = 2^bitCount).
 * @param numColorsImportant Number of colours that are important; 0 = all.
 */
data class DIBHeader(
    val size: Int,
    val width: Int,
    val height: Int,
    val planes: Short,
    val bitCount: Short,
    val compression: Int,
    val imageSize: Int,
    val horzPelsPerMeter: Int,
    val vertPelsPerMeter: Int,
    val numColors: Int,
    val numColorsImportant: Int
)

// ---------------------------------------------------------------------------
// ImageBMP
// ---------------------------------------------------------------------------

/**
 * Windows BMP image codec — corresponds to LLImageBMP in the C++ viewer source.
 *
 * Supports reading and writing uncompressed Windows BMP files with 8-bit
 * (indexed colour), 24-bit (true-colour), and 32-bit (colour-mask) pixel
 * formats.  RLE-compressed variants and OS/2 BMP are not supported, matching
 * the C++ implementation.
 *
 * Internal state mirrors the protected fields of LLImageBMP:
 *  - [colorPaletteColors] / [colorPalette] — indexed-colour look-up table
 *  - [bitmapOffset]        — byte offset of the pixel array within [data]
 *  - [bitsPerPixel]        — source colour depth (8, 24, or 32)
 *  - [bitfieldMask]        — RGBA channel masks used in 16/32-bit colour-mask mode
 *  - [originAtTop]         — true when the height field in the header is negative
 */
class ImageBMP : ImageFormatted(ImageCodecType.BMP) {

    // ---- internal parsing state (mirrors C++ protected fields) --------------

    /** Number of entries in [colorPalette]; 0 when no palette is present. */
    private var colorPaletteColors: Int = 0

    /**
     * Raw colour palette bytes.  Each entry is 4 bytes: Blue, Green, Red, 0x00
     * (BGRx order, as stored in the BMP file).  Null when no palette is present.
     */
    private var colorPalette: ByteArray? = null

    /** Byte offset within [data] at which the pixel array begins. */
    private var bitmapOffset: Int = 0

    /** Source colour depth in bits per pixel. */
    private var bitsPerPixel: Int = 0

    /**
     * Bitfield channel masks for 16/32-bit colour-mask mode (BI_BITFIELDS).
     * Index 0 = red, 1 = green, 2 = blue, 3 = alpha (alpha is informational only).
     * Stored as signed Int but treated as unsigned 32-bit values during decoding.
     */
    private val bitfieldMask: IntArray = IntArray(4)

    /**
     * True when the BMP height field is negative, indicating that the first
     * row in the pixel array corresponds to the top of the image.
     * The default (positive height) stores rows bottom-to-top.
     */
    private var originAtTop: Boolean = false

    // ---- ImageFormatted interface -------------------------------------------

    override val extension: String get() = "bmp"

    /**
     * Parse the BMP file and DIB headers from [data], populating image
     * dimensions, [bitsPerPixel], [bitmapOffset], and [colorPalette].
     *
     * Rejects OS/2 1.x (12-byte header), OS/2 2.x (12–64-byte header),
     * and all RLE-compressed variants.  Accepts Windows 3.x (40-byte),
     * Windows NT (40-byte + BI_BITFIELDS), BITMAPV4HEADER (108-byte), and
     * BITMAPV5HEADER (>108-byte) files.
     */
    override fun updateData(): Boolean {
        resetLastError()
        if (data.size < FILE_HEADER_SIZE) {
            setLastError("Uninitialized instance of ImageBMP")
            return false
        }

        // -- Part 1: File Header (14 bytes) -----------------------------------
        if (data[0] != 'B'.code.toByte() || data[1] != 'M'.code.toByte()) {
            val sig1 = data[0] == 'B'.code.toByte()
            val sig2 = data[1] == 'A'.code.toByte()
            setLastError(
                if (sig1 && sig2) "OS/2 bitmap array BMP files are not supported"
                else "Does not appear to be a bitmap file"
            )
            return false
        }

        bitmapOffset = readInt32LE(data, 10)

        // -- Part 2: DIB / Bitmap Header (40 bytes) ---------------------------
        if (data.size < FILE_HEADER_SIZE + BITMAP_HEADER_SIZE) {
            setLastError("BMP file too small to contain a bitmap header")
            return false
        }
        val headerSize = readInt32LE(data, FILE_HEADER_SIZE)

        val winNT = headerSize == 40 && readInt32LE(data, FILE_HEADER_SIZE + 16) == 3
        val win95 = headerSize >= 108

        when {
            headerSize == 12 ->
                { setLastError("Windows 2.x and OS/2 1.x BMP files are not supported"); return false }
            headerSize in 13..64 ->
                { setLastError("OS/2 2.x BMP files are not supported"); return false }
        }

        val rawWidth  = readInt32LE(data, FILE_HEADER_SIZE + 4)
        val rawHeight = readInt32LE(data, FILE_HEADER_SIZE + 8)
        originAtTop   = rawHeight < 0
        val width  = rawWidth
        val height = if (rawHeight < 0) -rawHeight else rawHeight

        bitsPerPixel = readInt16LE(data, FILE_HEADER_SIZE + 14).toInt()
        val compression = readInt32LE(data, FILE_HEADER_SIZE + 16)

        val components = when (bitsPerPixel) {
            8       -> 1
            24, 32  -> 3
            1, 4, 16 -> { setLastError("Unsupported bit depth"); return false }
            else    -> { setLastError("Unrecognized bit depth"); return false }
        }

        when (compression) {
            0    -> { /* BI_RGB — uncompressed */ }
            1    -> { setLastError("8 bit RLE compression not supported."); return false }
            2    -> { setLastError("4 bit RLE compression not supported."); return false }
            3    -> { /* BI_BITFIELDS — Windows NT or Win95 */ }
            else -> { setLastError("Unsupported compression format."); return false }
        }

        // -- Part 3: Bitfield Masks -------------------------------------------
        val extensionSize: Int
        val afterBitmapHeader = FILE_HEADER_SIZE + BITMAP_HEADER_SIZE
        if (winNT) {
            if (bitsPerPixel != 16 && bitsPerPixel != 32) {
                setLastError("Bitfield encoding requires 16 or 32 bits per pixel."); return false
            }
            extensionSize = 12  // 3 × UInt32
            if (data.size >= afterBitmapHeader + extensionSize) {
                for (i in 0..2) bitfieldMask[i] = readInt32LE(data, afterBitmapHeader + i * 4)
            }
        } else if (win95) {
            extensionSize = 68  // sizeof Win95BmpHeaderExtension
            if (compression == 3 && data.size >= afterBitmapHeader + 16) {
                for (i in 0..3) bitfieldMask[i] = readInt32LE(data, afterBitmapHeader + i * 4)
            }
        } else {
            extensionSize = 0
        }

        // -- Part 4: Colour Palette -------------------------------------------
        colorPaletteColors = when {
            bitsPerPixel >= 16 -> 0
            else -> {
                val numColors = readInt32LE(data, FILE_HEADER_SIZE + 32)
                if (numColors == 0) (1 shl bitsPerPixel) else numColors
            }
        }
        if (colorPaletteColors > 0) {
            val paletteOffset = afterBitmapHeader + extensionSize
            val paletteBytes  = colorPaletteColors * 4
            if (data.size >= paletteOffset + paletteBytes) {
                colorPalette = data.copyOfRange(paletteOffset, paletteOffset + paletteBytes)
            }
        }

        // Mirror C++ setSize(width, height, components)
        // (ImageFormatted subclasses track dimensions separately; stub here)
        System.err.println("ImageBMP: updateData not yet implemented")
        return false
    }

    /**
     * Decode the BMP pixel data from [data] into [rawImage].
     *
     * The C++ implementation calls one of four specialised helpers depending
     * on [bitsPerPixel]:
     *  - 8-bit  → [decodeColorTable8]
     *  - 16-bit → [decodeColorMask16]
     *  - 24-bit → [decodeTruecolor24]
     *  - 32-bit → [decodeColorMask32]
     *
     * After decoding, rows are flipped when [originAtTop] is true (i.e. when
     * the height field in the DIB header was negative).
     *
     * JVM alternative: `ImageIO.read(ByteArrayInputStream(data))`.
     */
    override fun decode(rawImage: ImageRaw, decodeTime: Float): Boolean {
        resetLastError()
        if (data.isEmpty()) {
            setLastError("ImageBMP trying to decode an image with no data!")
            return false
        }
        System.err.println("ImageBMP: decode not yet implemented")
        return false
    }

    /**
     * Encode [rawImage] as a Windows BMP, storing the result in [data].
     *
     * The C++ implementation:
     *  1. Computes line padding to align each row to a 4-byte boundary.
     *  2. Writes a 14-byte file header followed by a 40-byte BITMAPINFOHEADER.
     *  3. For 8-bit (greyscale) output, appends a 256-entry greyscale palette.
     *  4. Writes pixel rows bottom-to-top with BGR(x) byte order.
     *
     * JVM alternative: `ImageIO.write(bufferedImage, "bmp", outputStream)`.
     */
    override fun encode(rawImage: ImageRaw, encodeTime: Float): Boolean {
        resetLastError()
        System.err.println("ImageBMP: encode not yet implemented")
        return false
    }

    // ---- private decode helpers (correspond to C++ protected methods) -------

    /**
     * Decode an 8-bit indexed BMP row using [colorPalette].
     * Each source byte is an index into a 256-entry BGRx colour table.
     * Output is written as 3-component RGB (alpha is discarded).
     *
     * Rows are padded to the next 4-byte boundary in the source.
     */
    private fun decodeColorTable8(dst: ByteArray, src: ByteArray, srcOffset: Int): Boolean {
        System.err.println("ImageBMP: decodeColorTable8 not yet implemented")
        return false
    }

    /**
     * Decode a 16-bit colour-mask BMP using [bitfieldMask].
     * Defaults to 5-5-5 (RGB555) if no masks are provided.
     */
    private fun decodeColorMask16(dst: ByteArray, src: ByteArray, srcOffset: Int): Boolean {
        System.err.println("ImageBMP: decodeColorMask16 not yet implemented")
        return false
    }

    /**
     * Decode a 24-bit true-colour BMP.
     * Source bytes are stored as BGR; output is written as RGB.
     * Rows are padded to the next 4-byte boundary in the source.
     */
    private fun decodeTruecolor24(dst: ByteArray, src: ByteArray, srcOffset: Int): Boolean {
        System.err.println("ImageBMP: decodeTruecolor24 not yet implemented")
        return false
    }

    /**
     * Decode a 32-bit colour-mask BMP using [bitfieldMask].
     * Defaults to 0x00FF0000 / 0x0000FF00 / 0x000000FF if no masks are provided.
     * Alpha channel is not carried through (matches C++ comment "alpha is not supported").
     */
    private fun decodeColorMask32(dst: ByteArray, src: ByteArray, srcOffset: Int): Boolean {
        System.err.println("ImageBMP: decodeColorMask32 not yet implemented")
        return false
    }

    /**
     * Count trailing zero bits in [m] — used to determine the shift required
     * to align a bitfield mask value.  Direct port of `LLImageBMP::countTrailingZeros`.
     */
    private fun countTrailingZeros(m: Int): Int {
        var mask = m
        var count = 0
        while (mask and 1 == 0 && mask != 0) { count++; mask = mask ushr 1 }
        return count
    }

    // ---- little-endian binary reading helpers -------------------------------

    private fun readInt32LE(buf: ByteArray, offset: Int): Int =
        (buf[offset].toInt() and 0xFF) or
        ((buf[offset + 1].toInt() and 0xFF) shl 8) or
        ((buf[offset + 2].toInt() and 0xFF) shl 16) or
        ((buf[offset + 3].toInt() and 0xFF) shl 24)

    private fun readInt16LE(buf: ByteArray, offset: Int): Short =
        ((buf[offset].toInt() and 0xFF) or ((buf[offset + 1].toInt() and 0xFF) shl 8)).toShort()

    // ---- companion ----------------------------------------------------------

    companion object {
        private const val FILE_HEADER_SIZE   = 14
        private const val BITMAP_HEADER_SIZE = 40
    }
}
