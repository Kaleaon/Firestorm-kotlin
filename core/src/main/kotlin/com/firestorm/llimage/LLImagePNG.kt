package com.firestorm.llimage

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object ImageRawPNG {
    fun encode(image: ImageRaw): ByteArray {
        val w = image.width; val h = image.height
        val type = if (image.components == 4) BufferedImage.TYPE_4BYTE_ABGR else BufferedImage.TYPE_3BYTE_BGR
        val bi = BufferedImage(w, h, type)
        val data = image.data
        for (y in 0 until h) for (x in 0 until w) {
            val off = (y * w + x) * image.components
            val rgb = when (image.components) {
                4 -> ((data[off + 3].toInt() and 0xFF) shl 24) or
                     ((data[off].toInt() and 0xFF) shl 16) or
                     ((data[off + 1].toInt() and 0xFF) shl 8) or
                     (data[off + 2].toInt() and 0xFF)
                else -> (0xFF shl 24) or
                        ((data[off].toInt() and 0xFF) shl 16) or
                        ((data[off + 1].toInt() and 0xFF) shl 8) or
                        (data[off + 2].toInt() and 0xFF)
            }
            bi.setRGB(x, y, rgb)
        }
        val out = ByteArrayOutputStream()
        ImageIO.write(bi, "PNG", out)
        return out.toByteArray()
    }

    fun decode(data: ByteArray): ImageRaw? = runCatching {
        val bi = ImageIO.read(ByteArrayInputStream(data)) ?: return null
        bufferedImageToLLImage(bi)
    }.getOrNull()
}
