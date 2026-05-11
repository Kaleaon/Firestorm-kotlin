package com.firestorm.newview

import kotlin.math.min

private const val HTTP_INTERNAL_ERROR = 499

private fun isHttpServerErrorStatus(status: Int): Boolean {
    if (status == HTTP_INTERNAL_ERROR) return true
    return status in 500..599
}

abstract class LLHTTPRetryPolicy {
    abstract fun onSuccess()
    abstract fun onFailure(status: Int, headers: Map<String, String>)
    abstract fun shouldRetry(): Pair<Boolean, Float>
    abstract fun reset()
}

class LLAdaptiveRetryPolicy(
    private val minDelay: Float,
    private val maxDelay: Float,
    private val backoffFactor: Float,
    private val maxRetries: UInt,
    private val retryOn4xx: Boolean = false
) : LLHTTPRetryPolicy() {

    private var delay: Float = minDelay
    private var retryCount: UInt = 0u
    private var shouldRetryFlag: Boolean = true
    private var retryReadyAtMs: Long = 0L

    init {
        init()
    }

    private fun init() {
        delay = minDelay
        retryCount = 0u
        shouldRetryFlag = true
        retryReadyAtMs = 0L
    }

    override fun reset() {
        init()
    }

    override fun onSuccess() {
        init()
    }

    override fun onFailure(status: Int, headers: Map<String, String>) {
        val retryHeaderTime = getRetryAfterFromHeaders(headers)
        onFailureCommon(status, retryHeaderTime != null, retryHeaderTime ?: 0f)
    }

    fun onFailureWithResponse(statusCode: Int, responseHeaders: Map<String, String>) {
        onFailure(statusCode, responseHeaders)
    }

    override fun shouldRetry(): Pair<Boolean, Float> {
        if (retryCount == 0u) {
            return Pair(false, Float.MAX_VALUE)
        }
        val remaining = if (shouldRetryFlag) {
            val nowMs = System.currentTimeMillis()
            val waitMs = (retryReadyAtMs - nowMs).coerceAtLeast(0L)
            waitMs / 1000f
        } else {
            Float.MAX_VALUE
        }
        return Pair(shouldRetryFlag, remaining)
    }

    private fun getRetryAfterFromHeaders(headers: Map<String, String>): Float? {
        val value = headers["retry-after"] ?: headers["Retry-After"] ?: return null
        return getSecondsUntilRetryAfter(value)
    }

    private fun onFailureCommon(status: Int, hasRetryHeaderTime: Boolean, retryHeaderTime: Float) {
        if (!shouldRetryFlag) return
        if (retryCount > 0u) {
            delay = delay.times(backoffFactor).coerceIn(minDelay, maxDelay)
        }
        val waitTime = if (hasRetryHeaderTime) retryHeaderTime else delay
        if (retryCount >= maxRetries) {
            shouldRetryFlag = false
        }
        if (!retryOn4xx && !isHttpServerErrorStatus(status)) {
            shouldRetryFlag = false
        }
        if (shouldRetryFlag) {
            retryReadyAtMs = System.currentTimeMillis() + (waitTime * 1000).toLong()
        }
        retryCount++
    }

    companion object {
        fun getSecondsUntilRetryAfter(retryAfter: String): Float? {
            val asSeconds = retryAfter.trim().toDoubleOrNull()
            if (asSeconds != null) return asSeconds.toFloat()

            // Parse RFC 1123 date via JVM
            return try {
                @Suppress("DEPRECATION")
                val date = java.util.Date(retryAfter)
                val secondsUntil = (date.time - System.currentTimeMillis()) / 1000.0
                secondsUntil.toFloat()
            } catch (e: Exception) {
                null
            }
        }
    }
}
