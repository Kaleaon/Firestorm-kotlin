package com.firestorm.llimage

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

object ImageRawJPEG {
    fun encode(image: ImageRaw, quality: Float = 0.75f): ByteArray {
        val w = image.width; val h = image.height
        val bi = BufferedImage(w, h, if (image.components == 4) BufferedImage.TYPE_4BYTE_ABGR else BufferedImage.TYPE_3BYTE_BGR)
        val data = image.data
        for (y in 0 until h) for (x in 0 until w) {
            val off = (y * w + x) * image.components
            val rgb = when (image.components) {
                4 -> ((data[off + 3].toInt() and 0xFF) shl 24) or
                     ((data[off].toInt() and 0xFF) shl 16) or
                     ((data[off + 1].toInt() and 0xFF) shl 8) or
                     (data[off + 2].toInt() and 0xFF)
                3 -> (0xFF shl 24) or
                     ((data[off].toInt() and 0xFF) shl 16) or
                     ((data[off + 1].toInt() and 0xFF) shl 8) or
                     (data[off + 2].toInt() and 0xFF)
                else -> (0xFF shl 24) or
                        ((data[off].toInt() and 0xFF) shl 16) or
                        ((data[off].toInt() and 0xFF) shl 8) or
                        (data[off].toInt() and 0xFF)
            }
            bi.setRGB(x, y, rgb)
        }
        val out = ByteArrayOutputStream()
        val writers = ImageIO.getImageWritersByFormatName("jpeg")
        val writer = writers.next()
        val param = writer.defaultWriteParam
        param.compressionMode = ImageWriteParam.MODE_EXPLICIT
        param.compressionQuality = quality
        writer.output = ImageIO.createImageOutputStream(out)
        writer.write(null, IIOImage(bi, null, null), param)
        writer.dispose()
        return out.toByteArray()
    }

    fun decode(data: ByteArray): ImageRaw? = runCatching {
        val bi = ImageIO.read(ByteArrayInputStream(data)) ?: return null
        bufferedImageToLLImage(bi)
    }.getOrNull()
}

internal fun bufferedImageToLLImage(bi: BufferedImage): ImageRaw {
    val hasAlpha = bi.colorModel.hasAlpha()
    val components = if (hasAlpha) 4 else 3
    val raw = ByteArray(bi.width * bi.height * components)
    for (y in 0 until bi.height) for (x in 0 until bi.width) {
        val rgb = bi.getRGB(x, y)
        val off = (y * bi.width + x) * components
        raw[off]     = ((rgb shr 16) and 0xFF).toByte()
        raw[off + 1] = ((rgb shr 8) and 0xFF).toByte()
        raw[off + 2] = (rgb and 0xFF).toByte()
        if (hasAlpha) raw[off + 3] = ((rgb shr 24) and 0xFF).toByte()
    }
    return ImageRaw(bi.width, bi.height, components, raw)
}
