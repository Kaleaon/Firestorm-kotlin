package com.firestorm.llimage

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object ImageRawBMP {
    fun encode(image: ImageRaw): ByteArray {
        val bi = BufferedImage(image.width, image.height, BufferedImage.TYPE_3BYTE_BGR)
        val data = image.data
        for (y in 0 until image.height) for (x in 0 until image.width) {
            val off = (y * image.width + x) * image.components
            val rgb = (0xFF shl 24) or
                      ((data[off].toInt() and 0xFF) shl 16) or
                      ((data[off + 1].toInt() and 0xFF) shl 8) or
                      (data[off + 2].toInt() and 0xFF)
            bi.setRGB(x, y, rgb)
        }
        val out = ByteArrayOutputStream()
        ImageIO.write(bi, "BMP", out)
        return out.toByteArray()
    }

    fun decode(data: ByteArray): ImageRaw? = runCatching {
        val bi = ImageIO.read(ByteArrayInputStream(data)) ?: return null
        bufferedImageToLLImage(bi)
    }.getOrNull()
}
