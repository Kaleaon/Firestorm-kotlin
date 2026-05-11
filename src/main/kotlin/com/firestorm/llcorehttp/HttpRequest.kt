package com.firestorm.llcorehttp

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest as JHttpRequest
import java.net.http.HttpResponse as JHttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

typealias HttpHandle = Long

class HttpRequest {

    enum class Policy {
        DEFAULT_POLICY_ID,
        TEXTURE_INIT,
        TEXTURE_FETCH,
        MESH_FETCH,
        LARGE_FETCH
    }

    enum class Priority { LOW, NORMAL, HIGH, URGENT }

    companion object {
        val INVALID_HANDLE: HttpHandle = 0L

        private val client: HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    private val handleCounter = AtomicLong(1L)
    private val completionQueue = ConcurrentLinkedQueue<() -> Unit>()

    private fun nextHandle(): HttpHandle = handleCounter.getAndIncrement()

    private fun applyHeaders(builder: JHttpRequest.Builder, headers: HttpHeaders?) {
        headers?.getAll()?.forEach { (name, value) ->
            builder.header(name, value)
        }
    }

    private fun buildResponse(
        jresp: JHttpResponse<ByteArray>,
        options: HttpOptions?
    ): HttpResponse {
        val resp = HttpResponse()
        val code = jresp.statusCode()
        resp.status = HttpResponse.HttpStatus(
            code,
            if (code in 200..299) 0 else 1
        )
        resp.body = jresp.body()
        val hdrs = HttpHeaders()
        jresp.headers().map().forEach { (name, values) ->
            values.forEach { v -> hdrs.append(name, v) }
        }
        resp.headers = hdrs
        resp.contentType = jresp.headers().firstValue("content-type").orElse("")
        return resp
    }

    private fun enqueueAsync(
        handle: HttpHandle,
        requestBuilder: JHttpRequest.Builder,
        options: HttpOptions?,
        handler: HttpHandler?
    ): HttpHandle {
        val req = requestBuilder.build()
        client.sendAsync(req, JHttpResponse.BodyHandlers.ofByteArray())
            .thenAccept { jresp ->
                val resp = buildResponse(jresp, options)
                completionQueue.add { handler?.onCompleted(handle, resp) }
            }
            .exceptionally { ex ->
                val resp = HttpResponse()
                resp.status = HttpResponse.HttpStatus(HttpResponse.HttpStatus.LLCORE, -1)
                resp.requestUrl = req.uri().toString()
                completionQueue.add { handler?.onCompleted(handle, resp) }
                null
            }
        return handle
    }

    fun requestGet(
        policyId: Policy,
        priority: Priority,
        url: String,
        options: HttpOptions?,
        headers: HttpHeaders?,
        handler: HttpHandler?
    ): HttpHandle {
        val handle = nextHandle()
        val timeout = Duration.ofSeconds((options?.timeout ?: 30).toLong())
        val builder = JHttpRequest.newBuilder(URI.create(url))
            .GET()
            .timeout(timeout)
            .header("Accept", "*/*")
            .header("Connection", "keep-alive")
        applyHeaders(builder, headers)
        return enqueueAsync(handle, builder, options, handler)
    }

    fun requestPost(
        policyId: Policy,
        priority: Priority,
        url: String,
        body: ByteArray,
        options: HttpOptions?,
        headers: HttpHeaders?,
        handler: HttpHandler?
    ): HttpHandle {
        val handle = nextHandle()
        val timeout = Duration.ofSeconds((options?.timeout ?: 30).toLong())
        val builder = JHttpRequest.newBuilder(URI.create(url))
            .POST(JHttpRequest.BodyPublishers.ofByteArray(body))
            .timeout(timeout)
            .header("Accept", "*/*")
            .header("Connection", "keep-alive")
            .header("Content-Type", "application/x-www-form-urlencoded")
        applyHeaders(builder, headers)
        return enqueueAsync(handle, builder, options, handler)
    }

    fun requestPut(
        policyId: Policy,
        priority: Priority,
        url: String,
        body: ByteArray,
        options: HttpOptions?,
        headers: HttpHeaders?,
        handler: HttpHandler?
    ): HttpHandle {
        val handle = nextHandle()
        val timeout = Duration.ofSeconds((options?.timeout ?: 30).toLong())
        val builder = JHttpRequest.newBuilder(URI.create(url))
            .PUT(JHttpRequest.BodyPublishers.ofByteArray(body))
            .timeout(timeout)
            .header("Accept", "*/*")
            .header("Connection", "keep-alive")
        applyHeaders(builder, headers)
        return enqueueAsync(handle, builder, options, handler)
    }

    fun requestDelete(
        policyId: Policy,
        priority: Priority,
        url: String,
        options: HttpOptions?,
        headers: HttpHeaders?,
        handler: HttpHandler?
    ): HttpHandle {
        val handle = nextHandle()
        val timeout = Duration.ofSeconds((options?.timeout ?: 30).toLong())
        val builder = JHttpRequest.newBuilder(URI.create(url))
            .DELETE()
            .timeout(timeout)
            .header("Accept", "*/*")
            .header("Connection", "keep-alive")
        applyHeaders(builder, headers)
        return enqueueAsync(handle, builder, options, handler)
    }

    fun requestGetByteRange(
        policyId: Policy,
        priority: Priority,
        url: String,
        offset: Long,
        len: Long,
        options: HttpOptions?,
        headers: HttpHeaders?,
        handler: HttpHandler?
    ): HttpHandle {
        val handle = nextHandle()
        val timeout = Duration.ofSeconds((options?.timeout ?: 30).toLong())
        val builder = JHttpRequest.newBuilder(URI.create(url))
            .GET()
            .timeout(timeout)
            .header("Accept", "*/*")
            .header("Connection", "keep-alive")
        if (offset > 0 || len > 0) {
            val end = if (len > 0) (offset + len - 1).toString() else ""
            builder.header("Range", "bytes=$offset-$end")
        }
        applyHeaders(builder, headers)
        return enqueueAsync(handle, builder, options, handler)
    }

    fun update(usecs: Long): Int {
        var count = 0
        val deadline = if (usecs == 0L) Long.MAX_VALUE else System.nanoTime() + usecs * 1000L
        while (System.nanoTime() < deadline) {
            val cb = completionQueue.poll() ?: break
            cb()
            count++
        }
        return count
    }
}
