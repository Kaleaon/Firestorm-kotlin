package com.firestorm.llrender

import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

abstract class ImageDecodeResponder {
    abstract fun completed(success: Boolean, errorMessage: String, raw: Any?, aux: Any?, requestId: UInt)
}

private class ImageRequest(
    private val formattedImage: Any?,
    private val discardLevel: Int,
    private val needsAux: Boolean,
    private val responder: ImageDecodeResponder?,
    private val requestId: UInt
) {
    private var decodedImageRaw: Any? = null
    private var decodedImageAux: Any? = null
    private var decodedRaw: Boolean = false
    private var decodedAux: Boolean = false
    private var errorString: String = ""

    fun processRequest(): Boolean {
        if (formattedImage == null) return true
        // Decode through the JVM's ImageIO. Callers pass either a ByteArray
        // (encoded blob) or a path String; anything else is reported as a
        // decode failure. The aux channel — used for grayscale alpha
        // separation in J2K assets — is read from the alpha component when
        // requested.
        try {
            val input = when (formattedImage) {
                is ByteArray -> javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(formattedImage))
                is String -> javax.imageio.ImageIO.read(java.io.File(formattedImage))
                else -> {
                    errorString = "Unsupported formattedImage type ${formattedImage::class.simpleName}"
                    return false
                }
            }
            if (input == null) {
                errorString = "ImageIO returned null (unrecognised format)"
                return false
            }
            // Down-sample by discardLevel halvings (matches the C++ J2K
            // discard semantics).
            var image = input
            repeat(discardLevel.coerceAtLeast(0)) {
                val w = (image.width / 2).coerceAtLeast(1)
                val h = (image.height / 2).coerceAtLeast(1)
                val resized = java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                val g = resized.createGraphics()
                g.drawImage(image, 0, 0, w, h, null)
                g.dispose()
                image = resized
            }
            val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
            val rgba = ByteArray(pixels.size * 4)
            for (i in pixels.indices) {
                val p = pixels[i]
                rgba[i * 4]     = ((p ushr 16) and 0xFF).toByte()
                rgba[i * 4 + 1] = ((p ushr 8) and 0xFF).toByte()
                rgba[i * 4 + 2] = (p and 0xFF).toByte()
                rgba[i * 4 + 3] = ((p ushr 24) and 0xFF).toByte()
            }
            decodedImageRaw = Triple(image.width, image.height, rgba)
            decodedRaw = true
            if (needsAux) {
                val alpha = ByteArray(pixels.size)
                for (i in pixels.indices) alpha[i] = ((pixels[i] ushr 24) and 0xFF).toByte()
                decodedImageAux = Triple(image.width, image.height, alpha)
                decodedAux = true
            }
            return true
        } catch (e: Exception) {
            errorString = e.message ?: e::class.simpleName.orEmpty()
            return false
        }
    }

    fun finishRequest(completed: Boolean) {
        val success = completed && decodedRaw && (!needsAux || decodedAux)
        responder?.completed(success, errorString, decodedImageRaw, decodedImageAux, requestId)
    }
}

class ImageDecodeThread(threaded: Boolean = true) {

    private val decodeCount = AtomicInteger(0)

    private val queue = LinkedBlockingQueue<Runnable>()
    private val threadPool: ThreadPoolExecutor = ThreadPoolExecutor(
        8, 8, 0L, TimeUnit.MILLISECONDS, queue,
        Executors.defaultThreadFactory()
    )

    init {
        if (!threaded) {
            threadPool.corePoolSize = 1
            threadPool.maximumPoolSize = 1
        }
    }

    fun decodeImage(
        image: Any?,
        discard: Int,
        needsAux: Boolean,
        responder: ImageDecodeResponder?
    ): UInt {
        var id = decodeCount.incrementAndGet().toUInt()
        if (id == 0u) id = decodeCount.incrementAndGet().toUInt()

        val req = ImageRequest(image, discard, needsAux, responder, id)
        val posted = try {
            threadPool.execute {
                val done = req.processRequest()
                req.finishRequest(done)
            }
            true
        } catch (e: Exception) {
            false
        }

        return if (posted) id else 0u
    }

    fun getPending(): Int = queue.size

    fun update(maxTimeMs: Float): Int = getPending()

    fun getTotalDecodeCount(): Int = decodeCount.get()

    fun shutdown() {
        threadPool.shutdown()
    }
}
