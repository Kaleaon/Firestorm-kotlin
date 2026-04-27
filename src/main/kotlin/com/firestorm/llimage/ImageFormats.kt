package com.firestorm.llimage

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

interface ImageCodec {
    val extension: String
    fun encode(raw: ImageRaw): ByteArray
    fun decode(data: ByteArray): ImageRaw?
}

private fun BufferedImage.toImageRaw(): ImageRaw {
    val hasAlpha = colorModel.hasAlpha()
    val components = if (hasAlpha) 4 else 3
    val result = ByteArray(width * height * components)
    var idx = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = getRGB(x, y)
            result[idx++] = ((pixel shr 16) and 0xFF).toByte()
            result[idx++] = ((pixel shr 8) and 0xFF).toByte()
            result[idx++] = (pixel and 0xFF).toByte()
            if (hasAlpha) result[idx++] = ((pixel shr 24) and 0xFF).toByte()
        }
    }
    return ImageRaw(width, height, components, result)
}

private fun ImageRaw.toBufferedImage(): BufferedImage {
    val type = if (components == 4) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
    val img = BufferedImage(width, height, type)
    var idx = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val r = data.getOrElse(idx) { 0 }.toInt() and 0xFF
            val g = data.getOrElse(idx + 1) { 0 }.toInt() and 0xFF
            val b = data.getOrElse(idx + 2) { 0 }.toInt() and 0xFF
            val a = if (components == 4) data.getOrElse(idx + 3) { -1 }.toInt() and 0xFF else 0xFF
            img.setRGB(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
            idx += components
        }
    }
    return img
}

class PngImage : ImageCodec {
    override val extension: String = "png"

    override fun encode(raw: ImageRaw): ByteArray {
        val img = raw.toBufferedImage()
        val out = ByteArrayOutputStream()
        ImageIO.write(img, "png", out)
        return out.toByteArray()
    }

    override fun decode(data: ByteArray): ImageRaw? = runCatching {
        val img = ImageIO.read(ByteArrayInputStream(data)) ?: return null
        img.toImageRaw()
    }.getOrNull()
}

class JpegImage(private val quality: Int = 75) : ImageCodec {
    override val extension: String = "jpg"

    override fun encode(raw: ImageRaw): ByteArray {
        val src = if (raw.components == 4) raw.convertToType(3) else raw
        val img = src.toBufferedImage()
        val writers = ImageIO.getImageWritersByFormatName("jpeg")
        if (!writers.hasNext()) error("No JPEG writer found")
        val writer = writers.next()
        val param: ImageWriteParam = writer.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = quality / 100f
        }
        val out = ByteArrayOutputStream()
        writer.output = ImageIO.createImageOutputStream(out)
        writer.write(null, IIOImage(img, null, null), param)
        writer.dispose()
        return out.toByteArray()
    }

    override fun decode(data: ByteArray): ImageRaw? = runCatching {
        val img = ImageIO.read(ByteArrayInputStream(data)) ?: return null
        img.toImageRaw()
    }.getOrNull()

    fun setEncodeQuality(q: Int): JpegImage = JpegImage(q.coerceIn(1, 100))
}

class BmpImage : ImageCodec {
    override val extension: String = "bmp"

    override fun encode(raw: ImageRaw): ByteArray {
        val src = when (raw.components) {
            4 -> raw.convertToType(3)
            1 -> raw.convertToType(3)
            else -> raw
        }
        val img = src.toBufferedImage()
        val out = ByteArrayOutputStream()
        ImageIO.write(img, "bmp", out)
        return out.toByteArray()
    }

    override fun decode(data: ByteArray): ImageRaw? = runCatching {
        val img = ImageIO.read(ByteArrayInputStream(data)) ?: return null
        img.toImageRaw()
    }.getOrNull()
}

class TgaImage : ImageCodec {
    override val extension: String = "tga"

    override fun encode(raw: ImageRaw): ByteArray {
        val out = ByteArrayOutputStream()
        val hasAlpha = raw.components == 4
        val pixelDepth = if (hasAlpha) 32 else 24

        // TGA header (18 bytes)
        out.write(0)             // ID length
        out.write(0)             // Color map type (none)
        out.write(2)             // Image type: uncompressed true-color
        out.write(ByteArray(5))  // Color map spec (unused)
        out.write(0); out.write(0)   // X origin
        out.write(0); out.write(0)   // Y origin
        out.write(raw.width and 0xFF); out.write((raw.width shr 8) and 0xFF)
        out.write(raw.height and 0xFF); out.write((raw.height shr 8) and 0xFF)
        out.write(pixelDepth)    // Bits per pixel
        out.write(if (hasAlpha) 0x08 else 0x00)  // Attribute bits (alpha depth)

        // TGA stores bottom-to-top by default; flip row order
        val rowSize = raw.width * raw.components
        for (row in raw.height - 1 downTo 0) {
            val base = row * rowSize
            for (x in 0 until raw.width) {
                val px = base + x * raw.components
                val r = raw.data.getOrElse(px) { 0 }.toInt() and 0xFF
                val g = raw.data.getOrElse(px + 1) { 0 }.toInt() and 0xFF
                val b = raw.data.getOrElse(px + 2) { 0 }.toInt() and 0xFF
                // TGA stores BGR(A)
                out.write(b); out.write(g); out.write(r)
                if (hasAlpha) out.write(raw.data.getOrElse(px + 3) { -1 }.toInt() and 0xFF)
            }
        }
        return out.toByteArray()
    }

    override fun decode(data: ByteArray): ImageRaw? = runCatching {
        if (data.size < 18) return null
        val idLength = data[0].toInt() and 0xFF
        val imageType = data[2].toInt() and 0xFF
        if (imageType != 2) return null  // only uncompressed true-color supported

        val width = ((data[12].toInt() and 0xFF) or ((data[13].toInt() and 0xFF) shl 8))
        val height = ((data[14].toInt() and 0xFF) or ((data[15].toInt() and 0xFF) shl 8))
        val bpp = data[16].toInt() and 0xFF
        val components = when (bpp) { 32 -> 4; 24 -> 3; else -> return null }

        val headerSize = 18 + idLength
        val pixels = ByteArray(width * height * components)
        var src = headerSize
        for (row in height - 1 downTo 0) {
            for (x in 0 until width) {
                val dst = (row * width + x) * components
                val b = data.getOrElse(src++) { 0 }.toInt() and 0xFF
                val g = data.getOrElse(src++) { 0 }.toInt() and 0xFF
                val r = data.getOrElse(src++) { 0 }.toInt() and 0xFF
                pixels[dst] = r.toByte()
                pixels[dst + 1] = g.toByte()
                pixels[dst + 2] = b.toByte()
                if (components == 4) pixels[dst + 3] = data.getOrElse(src++) { -1 }
            }
        }
        ImageRaw(width, height, components, pixels)
    }.getOrNull()
}

object ImageCodecRegistry {
    private val codecs: Map<String, ImageCodec> = mapOf(
        "png"  to PngImage(),
        "jpg"  to JpegImage(),
        "jpeg" to JpegImage(),
        "bmp"  to BmpImage(),
        "tga"  to TgaImage()
    )

    fun get(extension: String): ImageCodec? = codecs[extension.lowercase().trimStart('.')]

    fun encode(raw: ImageRaw, extension: String): ByteArray =
        get(extension)?.encode(raw) ?: error("No codec for extension: $extension")

    fun decode(data: ByteArray, extension: String): ImageRaw? =
        get(extension)?.decode(data)
}
