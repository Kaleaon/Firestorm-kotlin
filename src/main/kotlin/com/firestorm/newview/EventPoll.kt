package com.firestorm.newview

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private const val EVENT_POLL_ERROR_RETRY_SECONDS      = 1f
private const val EVENT_POLL_ERROR_RETRY_SECONDS_INC  = 3f
private const val MAX_EVENT_POLL_ERRORS               = 15
private const val MIN_SECONDS_PASSED                  = 10.0

private val sNextCounter = AtomicInteger(1)

private class EventPollImpl(senderIp: String) {

    private val done = AtomicBoolean(false)
    private val counter = sNextCounter.getAndIncrement()
    private val senderIp: String = senderIp
    private var pollJob: Job? = null

    fun start(url: String) {
        if (url.isEmpty()) return
        pollJob = CoroutineScope(Dispatchers.IO).launch { eventPollCoro(url) }
    }

    fun stop() {
        done.set(true)
        pollJob?.cancel()
    }

    private suspend fun eventPollCoro(url: String) {
        var errorCount = 0
        var acknowledge: Any? = null

        while (!done.get()) {
            val requestStart = System.currentTimeMillis()

            val result = postAndSuspend(url, acknowledge) ?: run {
                done.set(true)
                break
            }

            val elapsedSeconds = (System.currentTimeMillis() - requestStart) / 1000.0

            val httpStatus = extractHttpStatus(result)
            if (!httpStatus.isSuccess) {
                if (httpStatus.isTimeout || httpStatus.isNoEvents) {
                    if (elapsedSeconds < MIN_SECONDS_PASSED) {
                        // Response too fast — treat as soft error and fall through to retry logic
                    } else {
                        errorCount = 0
                        continue
                    }
                } else if (httpStatus.isCanceled || httpStatus.isNotFound) {
                    break
                }

                if (errorCount < MAX_EVENT_POLL_ERRORS) {
                    val waitToRetry = EVENT_POLL_ERROR_RETRY_SECONDS +
                            errorCount * EVENT_POLL_ERROR_RETRY_SECONDS_INC
                    errorCount++
                    delay((waitToRetry * 1000).toLong())
                    if (done.get()) break
                    continue
                } else {
                    done.set(true)
                    if (isMainRegion(senderIp)) {
                        TODO("APR: use JVM equivalent — force disconnect (agent lost connection to main region)")
                    }
                    break
                }
            }

            errorCount = 0

            if (!resultHasEvents(result)) continue

            acknowledge = extractAck(result)
            val events = extractEvents(result)

            for (event in events) {
                if (eventHasMessage(event)) {
                    dispatchMessage(event)
                }
            }
        }
    }

    private suspend fun postAndSuspend(url: String, acknowledge: Any?): Any? {
        TODO("APR: use JVM equivalent — POST LLSD request {ack, done} to $url and suspend until response")
    }

    private fun extractHttpStatus(result: Any): HttpStatus {
        TODO("APR: use JVM equivalent — extract HTTP status from coroutine result")
    }

    private fun resultHasEvents(result: Any): Boolean {
        TODO("APR: use JVM equivalent — check result is a map with 'events' array and 'id'")
    }

    private fun extractAck(result: Any): Any? {
        TODO("APR: use JVM equivalent — extract 'id' field from result")
    }

    private fun extractEvents(result: Any): List<Any> {
        TODO("APR: use JVM equivalent — extract 'events' array from result")
    }

    private fun eventHasMessage(event: Any): Boolean {
        TODO("APR: use JVM equivalent — check event map has 'message' key")
    }

    private fun dispatchMessage(event: Any) {
        TODO("APR: use JVM equivalent — extract msg_name and body, call MessageSystem.dispatch on main thread")
    }

    private fun isMainRegion(ip: String): Boolean {
        TODO("APR: use JVM equivalent — compare ip against gAgent.getRegion().getHost()")
    }
}

private data class HttpStatus(
    val isSuccess: Boolean,
    val isTimeout: Boolean = false,
    val isNoEvents: Boolean = false,
    val isCanceled: Boolean = false,
    val isNotFound: Boolean = false
)

class EventPoll(pollUrl: String, senderIp: String) {

    private val impl: EventPollImpl = EventPollImpl(senderIp)

    init {
        impl.start(pollUrl)
    }

    fun stop() {
        impl.stop()
    }
}
