package com.firestorm.llcorehttp

import com.firestorm.llcommon.LLSD
import com.firestorm.llcommon.LLSDSerialize
import com.firestorm.llcommon.llinfos
import com.firestorm.llcommon.llwarns
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

const val HTTP_REQUEST_EXPIRY_SECS: Float = 60.0f

typealias CompletionCallback = (Map<String, Any?>) -> Unit
typealias BoolSettingQuery = (String) -> Boolean
typealias BoolSettingUpdate = (String, Boolean, String) -> Unit

private const val HTTP_LOGBODY_KEY = "HTTPLogBodyOnError"

private var boolSettingGet: BoolSettingQuery? = null
private var boolSettingPut: BoolSettingUpdate? = null

/** Shared HTTP client used for all blocking requests. */
private val httpClient: HttpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
    .build()

// ── LLSD ↔ Map conversion helpers ──────────────────────────────────────────

private fun llsdToAny(sd: LLSD): Any? = when (sd) {
    is LLSD.Undefined      -> null
    is LLSD.LLSDBoolean    -> sd.value
    is LLSD.LLSDInteger    -> sd.value
    is LLSD.LLSDReal       -> sd.value
    is LLSD.LLSDString     -> sd.value
    is LLSD.LLSDUUID       -> sd.value.toString()
    is LLSD.LLSDDate       -> sd.value.toISOString()
    is LLSD.LLSDURI        -> sd.value.asString()
    is LLSD.LLSDBinary     -> sd.value
    is LLSD.LLSDMap        -> sd.value.mapValues { llsdToAny(it.value) }
    is LLSD.LLSDArray      -> sd.value.map { llsdToAny(it) }
}

private fun anyToLLSD(v: Any?): LLSD = when (v) {
    null            -> LLSD.Undefined
    is Boolean      -> LLSD.LLSDBoolean(v)
    is Int          -> LLSD.LLSDInteger(v)
    is Long         -> LLSD.LLSDInteger(v.toInt())
    is Double       -> LLSD.LLSDReal(v)
    is Float        -> LLSD.LLSDReal(v.toDouble())
    is String       -> LLSD.LLSDString(v)
    is Map<*, *>    -> LLSD.LLSDMap((v as Map<String, Any?>).mapValues { anyToLLSD(it.value) })
    is List<*>      -> LLSD.LLSDArray(v.map { anyToLLSD(it) })
    else            -> LLSD.LLSDString(v.toString())
}

fun setPropertyMethods(queryfn: BoolSettingQuery, updatefn: BoolSettingUpdate) {
    boolSettingGet = queryfn
    boolSettingPut = updatefn
    updatefn(HTTP_LOGBODY_KEY, false, "Log the entire HTTP body in the case of an HTTP error.")
}

fun responseToLLSD(body: ByteArray, log: Boolean): Map<String, Any?>? {
    if (body.isEmpty()) return null
    return try {
        val xml = body.toString(Charsets.UTF_8)
        val sd = LLSDSerialize.fromXML(xml)
        @Suppress("UNCHECKED_CAST")
        llsdToAny(sd) as? Map<String, Any?>
    } catch (e: Exception) {
        if (log) llwarns("CoreHttpUtil") { "responseToLLSD: parse failed: ${e.message}" }
        null
    }
}

fun responseToString(body: ByteArray?): String {
    if (body == null || body.isEmpty()) return "[Empty]"
    return try {
        val llsd = LLSDSerialize.fromXML(body.toString(Charsets.UTF_8))
        LLSDSerialize.toXML(llsd).take(1024)
    } catch (e: Exception) {
        body.toString(Charsets.UTF_8).take(1024)
    }
}

private fun llsdBodyPublisher(body: Map<String, Any?>): HttpRequest.BodyPublisher {
    val sd = anyToLLSD(body)
    val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<llsd>${LLSDSerialize.toXML(sd)}</llsd>"
    return HttpRequest.BodyPublishers.ofString(xml)
}

private fun buildResult(url: String, resp: HttpResponse<ByteArray>): Map<String, Any?> {
    val status = HttpStatus(0, resp.statusCode())
    val result = mutableMapOf<String, Any?>()
    val body = resp.body()
    if (status.success) {
        val parsed = responseToLLSD(body, false)
        if (parsed != null) result.putAll(parsed)
        else result[HttpCoroutineAdapter.HTTP_RESULTS_CONTENT] = body.toString(Charsets.UTF_8)
    }
    HttpCoroHandler.writeStatusCodes(status, url, result)
    return result
}

fun requestPostWithLLSD(
    url: String,
    body: Map<String, Any?>,
    headers: Map<String, String> = emptyMap(),
    options: Map<String, Any?> = emptyMap(),
    handler: ((Map<String, Any?>) -> Unit)? = null
): Long {
    Thread {
        try {
            var reqBuilder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/llsd+xml")
                .POST(llsdBodyPublisher(body))
                .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
            headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
            val resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
            handler?.invoke(buildResult(url, resp))
        } catch (e: Exception) {
            llwarns("CoreHttpUtil") { "requestPostWithLLSD failed for $url: ${e.message}" }
        }
    }.also { it.isDaemon = true }.start()
    return 0L
}

fun requestPutWithLLSD(
    url: String,
    body: Map<String, Any?>,
    headers: Map<String, String> = emptyMap(),
    options: Map<String, Any?> = emptyMap(),
    handler: ((Map<String, Any?>) -> Unit)? = null
): Long {
    Thread {
        try {
            var reqBuilder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/llsd+xml")
                .PUT(llsdBodyPublisher(body))
                .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
            headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
            val resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
            handler?.invoke(buildResult(url, resp))
        } catch (e: Exception) {
            llwarns("CoreHttpUtil") { "requestPutWithLLSD failed for $url: ${e.message}" }
        }
    }.also { it.isDaemon = true }.start()
    return 0L
}

fun requestPatchWithLLSD(
    url: String,
    body: Map<String, Any?>,
    headers: Map<String, String> = emptyMap(),
    options: Map<String, Any?> = emptyMap(),
    handler: ((Map<String, Any?>) -> Unit)? = null
): Long {
    Thread {
        try {
            var reqBuilder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/llsd+xml")
                .method("PATCH", llsdBodyPublisher(body))
                .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
            headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
            val resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
            handler?.invoke(buildResult(url, resp))
        } catch (e: Exception) {
            llwarns("CoreHttpUtil") { "requestPatchWithLLSD failed for $url: ${e.message}" }
        }
    }.also { it.isDaemon = true }.start()
    return 0L
}

data class HttpStatus(
    val type: Int,
    val status: Int,
    val message: String = "",
    val success: Boolean = status in 200..299
)

abstract class HttpCoroHandler(val replyPump: (Map<String, Any?>) -> Unit) {

    fun onCompleted(handle: Long, responseBody: ByteArray?, responseStatus: HttpStatus, requestUrl: String, responseHeaders: Map<String, String>) {
        val result = mutableMapOf<String, Any?>()
        if (!responseStatus.success) {
            if (responseStatus.type in 400..499 && responseBody != null) {
                val parsed = responseToLLSD(responseBody, false)
                if (parsed != null) {
                    if (parsed is Map<*, *>) result.putAll(parsed as Map<String, Any?>)
                    else result[HttpCoroutineAdapter.HTTP_RESULTS_CONTENT] = parsed
                }
            }
        } else {
            handleSuccess(responseBody, responseStatus, result)
        }
        buildStatusEntry(requestUrl, responseStatus, responseHeaders, result)
        if (!responseStatus.success) {
            val httpStatus = result[HttpCoroutineAdapter.HTTP_RESULTS] as? MutableMap<String, Any?>
            httpStatus?.set("error_body", responseBody?.toString(Charsets.UTF_8) ?: "")
        }
        replyPump(result)
    }

    protected abstract fun handleSuccess(body: ByteArray?, status: HttpStatus, result: MutableMap<String, Any?>)

    private fun buildStatusEntry(url: String, status: HttpStatus, headers: Map<String, String>, result: MutableMap<String, Any?>) {
        val httpResults = mutableMapOf<String, Any?>()
        httpResults[HttpCoroutineAdapter.HTTP_RESULTS_SUCCESS] = status.success
        httpResults[HttpCoroutineAdapter.HTTP_RESULTS_TYPE] = status.type
        httpResults[HttpCoroutineAdapter.HTTP_RESULTS_STATUS] = status.status
        httpResults[HttpCoroutineAdapter.HTTP_RESULTS_MESSAGE] = status.message
        httpResults[HttpCoroutineAdapter.HTTP_RESULTS_URL] = url
        httpResults[HttpCoroutineAdapter.HTTP_RESULTS_HEADERS] = headers.toMap()
        result[HttpCoroutineAdapter.HTTP_RESULTS] = httpResults
    }

    companion object {
        fun writeStatusCodes(status: HttpStatus, url: String, result: MutableMap<String, Any?>) {
            result[HttpCoroutineAdapter.HTTP_RESULTS_SUCCESS] = status.success
            result[HttpCoroutineAdapter.HTTP_RESULTS_TYPE] = status.type
            result[HttpCoroutineAdapter.HTTP_RESULTS_STATUS] = status.status
            result[HttpCoroutineAdapter.HTTP_RESULTS_MESSAGE] = status.message
            result[HttpCoroutineAdapter.HTTP_RESULTS_URL] = url
        }
    }
}

class HttpCoroutineAdapter(
    private val adapterName: String,
    private val policyId: Int
) {
    companion object {
        const val HTTP_RESULTS         = "http_result"
        const val HTTP_RESULTS_SUCCESS = "success"
        const val HTTP_RESULTS_TYPE    = "type"
        const val HTTP_RESULTS_STATUS  = "status"
        const val HTTP_RESULTS_MESSAGE = "message"
        const val HTTP_RESULTS_URL     = "url"
        const val HTTP_RESULTS_HEADERS = "headers"
        const val HTTP_RESULTS_CONTENT = "content"
        const val HTTP_RESULTS_RAW     = "raw"

        fun getStatusFromLLSD(httpResults: Map<String, Any?>): HttpStatus {
            val type = (httpResults[HTTP_RESULTS_TYPE] as? Number)?.toInt() ?: 0
            val status = (httpResults[HTTP_RESULTS_STATUS] as? Number)?.toInt() ?: 0
            val message = httpResults[HTTP_RESULTS_MESSAGE] as? String ?: ""
            val success = httpResults[HTTP_RESULTS_SUCCESS] as? Boolean ?: (status in 200..299)
            return HttpStatus(type, status, message, success)
        }

        fun callbackHttpGet(
            url: String,
            policyId: Int = 0,
            success: CompletionCallback? = null,
            failure: CompletionCallback? = null
        ) {
            Thread {
                try {
                    val req = HttpRequest.newBuilder(URI.create(url))
                        .GET()
                        .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
                        .build()
                    val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray())
                    val result = buildResultStatic(url, resp)
                    val status = HttpStatus(0, resp.statusCode())
                    if (status.success) success?.invoke(result) else failure?.invoke(result)
                } catch (e: Exception) {
                    llwarns("CoreHttpUtil") { "callbackHttpGet failed for $url: ${e.message}" }
                    failure?.invoke(mapOf(HTTP_RESULTS_URL to url, HTTP_RESULTS_SUCCESS to false, HTTP_RESULTS_MESSAGE to (e.message ?: "")))
                }
            }.also { it.isDaemon = true }.start()
        }

        fun callbackHttpPost(
            url: String,
            policyId: Int = 0,
            postData: Map<String, Any?>,
            success: CompletionCallback? = null,
            failure: CompletionCallback? = null
        ) {
            Thread {
                try {
                    val req = HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "application/llsd+xml")
                        .POST(llsdBodyPublisher(postData))
                        .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
                        .build()
                    val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray())
                    val result = buildResultStatic(url, resp)
                    val status = HttpStatus(0, resp.statusCode())
                    if (status.success) success?.invoke(result) else failure?.invoke(result)
                } catch (e: Exception) {
                    llwarns("CoreHttpUtil") { "callbackHttpPost failed for $url: ${e.message}" }
                    failure?.invoke(mapOf(HTTP_RESULTS_URL to url, HTTP_RESULTS_SUCCESS to false, HTTP_RESULTS_MESSAGE to (e.message ?: "")))
                }
            }.also { it.isDaemon = true }.start()
        }

        fun callbackHttpDel(
            url: String,
            policyId: Int = 0,
            success: CompletionCallback? = null,
            failure: CompletionCallback? = null
        ) {
            Thread {
                try {
                    val req = HttpRequest.newBuilder(URI.create(url))
                        .DELETE()
                        .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
                        .build()
                    val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray())
                    val result = buildResultStatic(url, resp)
                    val status = HttpStatus(0, resp.statusCode())
                    if (status.success) success?.invoke(result) else failure?.invoke(result)
                } catch (e: Exception) {
                    llwarns("CoreHttpUtil") { "callbackHttpDel failed for $url: ${e.message}" }
                    failure?.invoke(mapOf(HTTP_RESULTS_URL to url, HTTP_RESULTS_SUCCESS to false, HTTP_RESULTS_MESSAGE to (e.message ?: "")))
                }
            }.also { it.isDaemon = true }.start()
        }

        fun messageHttpGet(url: String, success: String = "", failure: String = "") {
            Thread {
                try {
                    val req = HttpRequest.newBuilder(URI.create(url)).GET()
                        .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong())).build()
                    val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray())
                    if (resp.statusCode() in 200..299) {
                        if (success.isNotEmpty()) llinfos("CoreHttpUtil") { "GET $url: $success" }
                    } else {
                        if (failure.isNotEmpty()) llwarns("CoreHttpUtil") { "GET $url failed (${resp.statusCode()}): $failure" }
                    }
                } catch (e: Exception) {
                    if (failure.isNotEmpty()) llwarns("CoreHttpUtil") { "GET $url exception: ${e.message} — $failure" }
                }
            }.also { it.isDaemon = true }.start()
        }

        fun messageHttpPost(url: String, postData: Map<String, Any?>, success: String, failure: String) {
            Thread {
                try {
                    val req = HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "application/llsd+xml")
                        .POST(llsdBodyPublisher(postData))
                        .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong())).build()
                    val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray())
                    if (resp.statusCode() in 200..299) {
                        if (success.isNotEmpty()) llinfos("CoreHttpUtil") { "POST $url: $success" }
                    } else {
                        if (failure.isNotEmpty()) llwarns("CoreHttpUtil") { "POST $url failed (${resp.statusCode()}): $failure" }
                    }
                } catch (e: Exception) {
                    if (failure.isNotEmpty()) llwarns("CoreHttpUtil") { "POST $url exception: ${e.message} — $failure" }
                }
            }.also { it.isDaemon = true }.start()
        }

        private fun buildResultStatic(url: String, resp: HttpResponse<ByteArray>): MutableMap<String, Any?> {
            val status = HttpStatus(0, resp.statusCode())
            val result = mutableMapOf<String, Any?>()
            val body = resp.body()
            if (status.success) {
                val parsed = responseToLLSD(body, false)
                if (parsed != null) result.putAll(parsed)
                else result[HTTP_RESULTS_CONTENT] = body.toString(Charsets.UTF_8)
            }
            writeStatusCodes(status, url, result)
            return result
        }
    }

    // ── Instance HTTP methods (blocking, mirror C++ coroutine-adapter API) ──────

    private fun sendBlocking(req: HttpRequest): Map<String, Any?> {
        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray())
        return buildResult(req.uri().toString(), resp)
    }

    fun postAndSuspend(
        url: String,
        body: Map<String, Any?>,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/llsd+xml")
            .POST(llsdBodyPublisher(body))
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        return sendBlocking(reqBuilder.build())
    }

    fun postAndSuspend(
        url: String,
        rawBody: ByteArray,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofByteArray(rawBody))
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        return sendBlocking(reqBuilder.build())
    }

    fun postRawAndSuspend(
        url: String,
        rawBody: ByteArray,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofByteArray(rawBody))
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        val resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
        val result = buildResult(url, resp).toMutableMap()
        result[HttpCoroutineAdapter.HTTP_RESULTS_RAW] = resp.body()
        return result
    }

    fun postFileAndSuspend(
        url: String,
        fileName: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        val fileBytes = File(fileName).readBytes()
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        return sendBlocking(reqBuilder.build())
    }

    fun postJsonAndSuspend(
        url: String,
        body: Map<String, Any?>,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        // Simple JSON serialization without an external library
        val json = mapToJson(body)
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        return sendBlocking(reqBuilder.build())
    }

    fun putAndSuspend(
        url: String,
        body: Map<String, Any?>,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/llsd+xml")
            .PUT(llsdBodyPublisher(body))
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        return sendBlocking(reqBuilder.build())
    }

    fun putJsonAndSuspend(
        url: String,
        body: Map<String, Any?>,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        val json = mapToJson(body)
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        return sendBlocking(reqBuilder.build())
    }

    fun getAndSuspend(
        url: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .GET()
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        return sendBlocking(reqBuilder.build())
    }

    fun getRawAndSuspend(
        url: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .GET()
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        val resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
        val result = buildResult(url, resp).toMutableMap()
        result[HTTP_RESULTS_RAW] = resp.body()
        return result
    }

    fun getJsonAndSuspend(
        url: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        val resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
        val result = buildResult(url, resp).toMutableMap()
        if (HttpStatus(0, resp.statusCode()).success) {
            result[HTTP_RESULTS_CONTENT] = resp.body().toString(Charsets.UTF_8)
        }
        return result
    }

    fun deleteAndSuspend(
        url: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .DELETE()
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        return sendBlocking(reqBuilder.build())
    }

    fun deleteJsonAndSuspend(
        url: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .header("Accept", "application/json")
            .DELETE()
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        val resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
        val result = buildResult(url, resp).toMutableMap()
        if (HttpStatus(0, resp.statusCode()).success) {
            result[HTTP_RESULTS_CONTENT] = resp.body().toString(Charsets.UTF_8)
        }
        return result
    }

    fun patchAndSuspend(
        url: String,
        body: Map<String, Any?>,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/llsd+xml")
            .method("PATCH", llsdBodyPublisher(body))
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        return sendBlocking(reqBuilder.build())
    }

    fun copyAndSuspend(
        url: String,
        dest: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .header("Destination", dest)
            .method("COPY", HttpRequest.BodyPublishers.noBody())
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        return sendBlocking(reqBuilder.build())
    }

    fun moveAndSuspend(
        url: String,
        dest: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        var reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .header("Destination", dest)
            .method("MOVE", HttpRequest.BodyPublishers.noBody())
            .timeout(Duration.ofSeconds(HTTP_REQUEST_EXPIRY_SECS.toLong()))
        headers.forEach { (k, v) -> reqBuilder = reqBuilder.header(k, v) }
        return sendBlocking(reqBuilder.build())
    }

    fun cancelSuspendedOperation() {
        // Blocking requests cannot be cancelled mid-flight without a thread interrupt.
        // Signal the owning thread to stop if it is waiting; best-effort only.
        // A future refactor to CompletableFuture/coroutines would enable clean cancellation.
    }
}

// ── Minimal JSON serializer (no external library required) ──────────────────

private fun mapToJson(map: Map<String, Any?>): String = buildString {
    append('{')
    map.entries.forEachIndexed { idx, (k, v) ->
        if (idx > 0) append(',')
        append('"').append(k.replace("\"", "\\\"")).append("\":")
        append(anyToJson(v))
    }
    append('}')
}

private fun anyToJson(v: Any?): String = when (v) {
    null           -> "null"
    is Boolean     -> v.toString()
    is Number      -> v.toString()
    is String      -> '"' + v.replace("\\", "\\\\").replace("\"", "\\\"") + '"'
    is Map<*, *>   -> mapToJson(@Suppress("UNCHECKED_CAST") (v as Map<String, Any?>))
    is List<*>     -> "[${v.joinToString(",") { anyToJson(it) }}]"
    else           -> '"' + v.toString().replace("\"", "\\\"") + '"'
}
