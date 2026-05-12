package com.firestorm.llrender

import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

abstract class ImageDecodeResponder {
    abstract fun completed(success: Boolean, errorMessage: String, raw: Any?, aux: Any?, requestId: UInt)
}

// ---------------------------------------------------------------------------
// Platform-neutral image I/O helpers using reflection so that the file
// compiles cleanly on Android (no javax.imageio / java.awt in the SDK).
// On JVM these resolve to the real ImageIO / BufferedImage at runtime;
// on Android isAvailable() returns false and every decode yields null.
// ---------------------------------------------------------------------------
private object JvmImageIo {
    private val imageIoClass: Class<*>? = runCatching {
        Class.forName("javax.imageio.ImageIO")
    }.getOrNull()
    private val bufferedImageClass: Class<*>? = runCatching {
        Class.forName("java.awt.image.BufferedImage")
    }.getOrNull()

    fun isAvailable(): Boolean = imageIoClass != null && bufferedImageClass != null

    /** Read an image from a ByteArray or a file-path String. Returns null on failure. */
    fun read(source: Any): Any? {
        val io = imageIoClass ?: return null
        return runCatching {
            when (source) {
                is ByteArray -> {
                    val baisClass = Class.forName("java.io.ByteArrayInputStream")
                    val bais = baisClass.getConstructor(ByteArray::class.java).newInstance(source)
                    io.getMethod("read", Class.forName("java.io.InputStream")).invoke(null, bais)
                }
                is String -> {
                    val fileClass = Class.forName("java.io.File")
                    val file = fileClass.getConstructor(String::class.java).newInstance(source)
                    io.getMethod("read", fileClass).invoke(null, file)
                }
                else -> null
            }
        }.getOrNull()
    }

    fun getWidth(image: Any): Int =
        (bufferedImageClass?.getMethod("getWidth")?.invoke(image) as? Int) ?: 0

    fun getHeight(image: Any): Int =
        (bufferedImageClass?.getMethod("getHeight")?.invoke(image) as? Int) ?: 0

    fun getRGB(image: Any, x: Int, y: Int, w: Int, h: Int): IntArray? =
        runCatching {
            bufferedImageClass
                ?.getMethod(
                    "getRGB",
                    Int::class.java, Int::class.java,
                    Int::class.java, Int::class.java,
                    IntArray::class.java, Int::class.java, Int::class.java
                )
                ?.invoke(image, x, y, w, h, null, 0, w) as? IntArray
        }.getOrNull()
}

/** Downsample a pixel array by 2× (box filter) — no AWT dependency. */
private fun downsample(pixels: IntArray, srcW: Int, srcH: Int): Triple<Int, Int, IntArray> {
    val dstW = (srcW / 2).coerceAtLeast(1)
    val dstH = (srcH / 2).coerceAtLeast(1)
    val dst = IntArray(dstW * dstH)
    for (dy in 0 until dstH) {
        for (dx in 0 until dstW) {
            var a = 0; var r = 0; var g = 0; var b = 0; var n = 0
            for (ky in 0..1) for (kx in 0..1) {
                val px = dx * 2 + kx; val py = dy * 2 + ky
                if (px < srcW && py < srcH) {
                    val p = pixels[py * srcW + px]
                    a += (p ushr 24) and 0xFF
                    r += (p ushr 16) and 0xFF
                    g += (p ushr 8)  and 0xFF
                    b +=  p          and 0xFF
                    n++
                }
            }
            dst[dy * dstW + dx] = if (n > 0)
                ((a / n) shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
            else 0
        }
    }
    return Triple(dstW, dstH, dst)
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
        // Decode through the JVM's ImageIO via reflection so this file compiles
        // on Android (where javax.imageio is absent).  Callers pass either a
        // ByteArray (encoded blob) or a path String.
        try {
            if (!JvmImageIo.isAvailable()) {
                errorString = "ImageIO not available on this platform"
                return false
            }
            val decoded = JvmImageIo.read(formattedImage) ?: run {
                errorString = when (formattedImage) {
                    is ByteArray, is String -> "ImageIO returned null (unrecognised format)"
                    else -> "Unsupported formattedImage type ${formattedImage?.javaClass?.simpleName}"
                }
                return false
            }

            // Retrieve pixels from the decoded image.
            var w = JvmImageIo.getWidth(decoded)
            var h = JvmImageIo.getHeight(decoded)
            var pixels = JvmImageIo.getRGB(decoded, 0, 0, w, h) ?: run {
                errorString = "getRGB returned null"
                return false
            }

            // Down-sample by discardLevel halvings (matches the C++ J2K discard semantics).
            repeat(discardLevel.coerceAtLeast(0)) {
                val (nw, nh, np) = downsample(pixels, w, h)
                w = nw; h = nh; pixels = np
            }

            val rgba = ByteArray(pixels.size * 4)
            for (i in pixels.indices) {
                val p = pixels[i]
                rgba[i * 4]     = ((p ushr 16) and 0xFF).toByte()
                rgba[i * 4 + 1] = ((p ushr 8)  and 0xFF).toByte()
                rgba[i * 4 + 2] = ( p           and 0xFF).toByte()
                rgba[i * 4 + 3] = ((p ushr 24)  and 0xFF).toByte()
            }
            decodedImageRaw = Triple(w, h, rgba)
            decodedRaw = true

            if (needsAux) {
                val alpha = ByteArray(pixels.size)
                for (i in pixels.indices) alpha[i] = ((pixels[i] ushr 24) and 0xFF).toByte()
                decodedImageAux = Triple(w, h, alpha)
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
