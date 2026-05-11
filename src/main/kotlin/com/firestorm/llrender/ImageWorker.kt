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
        TODO("APR: use JVM equivalent — decode formattedImage into decodedImageRaw; if needsAux decode aux channel into decodedImageAux; populate errorString on failure")
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
