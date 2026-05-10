package com.firestorm.llcorehttp

const val HTTP_REQUEST_EXPIRY_SECS: Float = 60.0f

typealias CompletionCallback = (Map<String, Any?>) -> Unit
typealias BoolSettingQuery = (String) -> Boolean
typealias BoolSettingUpdate = (String, Boolean, String) -> Unit

private const val HTTP_LOGBODY_KEY = "HTTPLogBodyOnError"

private var boolSettingGet: BoolSettingQuery? = null
private var boolSettingPut: BoolSettingUpdate? = null

fun setPropertyMethods(queryfn: BoolSettingQuery, updatefn: BoolSettingUpdate) {
    boolSettingGet = queryfn
    boolSettingPut = updatefn
    updatefn(HTTP_LOGBODY_KEY, false, "Log the entire HTTP body in the case of an HTTP error.")
}

fun responseToLLSD(body: ByteArray, log: Boolean): Map<String, Any?>? {
    if (body.isEmpty()) return null
    TODO("APR: use JVM XML parser to deserialise LLSD body into Map<String,Any?>")
}

fun responseToString(body: ByteArray?): String {
    if (body == null || body.isEmpty()) return "[Empty]"
    TODO("APR: attempt LLSD parse, fall back to raw string truncated at 1024 chars")
}

fun requestPostWithLLSD(
    url: String,
    body: Map<String, Any?>,
    headers: Map<String, String> = emptyMap(),
    options: Map<String, Any?> = emptyMap(),
    handler: ((Map<String, Any?>) -> Unit)? = null
): Long {
    TODO("APR: use JVM HttpClient — POST LLSD-serialised body to $url")
}

fun requestPutWithLLSD(
    url: String,
    body: Map<String, Any?>,
    headers: Map<String, String> = emptyMap(),
    options: Map<String, Any?> = emptyMap(),
    handler: ((Map<String, Any?>) -> Unit)? = null
): Long {
    TODO("APR: use JVM HttpClient — PUT LLSD-serialised body to $url")
}

fun requestPatchWithLLSD(
    url: String,
    body: Map<String, Any?>,
    headers: Map<String, String> = emptyMap(),
    options: Map<String, Any?> = emptyMap(),
    handler: ((Map<String, Any?>) -> Unit)? = null
): Long {
    TODO("APR: use JVM HttpClient — PATCH LLSD-serialised body to $url")
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
            TODO("APR: use JVM coroutine — async GET $url, call success/failure callback")
        }

        fun callbackHttpPost(
            url: String,
            policyId: Int = 0,
            postData: Map<String, Any?>,
            success: CompletionCallback? = null,
            failure: CompletionCallback? = null
        ) {
            TODO("APR: use JVM coroutine — async POST postData to $url, call success/failure callback")
        }

        fun callbackHttpDel(
            url: String,
            policyId: Int = 0,
            success: CompletionCallback? = null,
            failure: CompletionCallback? = null
        ) {
            TODO("APR: use JVM coroutine — async DELETE $url, call success/failure callback")
        }

        fun messageHttpGet(url: String, success: String = "", failure: String = "") {
            TODO("APR: use JVM coroutine — GET $url, log result at INFO/WARN level")
        }

        fun messageHttpPost(url: String, postData: Map<String, Any?>, success: String, failure: String) {
            TODO("APR: use JVM coroutine — POST to $url, log result at INFO/WARN level")
        }
    }

    fun postAndSuspend(
        url: String,
        body: Map<String, Any?>,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — POST LLSD body to $url and return decorated result map")
    }

    fun postAndSuspend(
        url: String,
        rawBody: ByteArray,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — POST raw bytes to $url and return decorated result map")
    }

    fun postRawAndSuspend(
        url: String,
        rawBody: ByteArray,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — POST raw body to $url, return result with HTTP_RESULTS_RAW bytes")
    }

    fun postFileAndSuspend(
        url: String,
        fileName: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — read $fileName and POST bytes to $url")
    }

    fun postJsonAndSuspend(
        url: String,
        body: Map<String, Any?>,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — POST JSON-serialised body to $url")
    }

    fun putAndSuspend(
        url: String,
        body: Map<String, Any?>,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — PUT LLSD body to $url")
    }

    fun putJsonAndSuspend(
        url: String,
        body: Map<String, Any?>,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — PUT JSON body to $url")
    }

    fun getAndSuspend(
        url: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — GET $url and return decorated result map")
    }

    fun getRawAndSuspend(
        url: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — GET $url, return result with HTTP_RESULTS_RAW bytes")
    }

    fun getJsonAndSuspend(
        url: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — GET $url, parse JSON response into result map")
    }

    fun deleteAndSuspend(
        url: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — DELETE $url and return decorated result map")
    }

    fun deleteJsonAndSuspend(
        url: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — DELETE $url, parse JSON response")
    }

    fun patchAndSuspend(
        url: String,
        body: Map<String, Any?>,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — PATCH LLSD body to $url")
    }

    fun copyAndSuspend(
        url: String,
        dest: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — COPY $url with Destination: $dest header")
    }

    fun moveAndSuspend(
        url: String,
        dest: String,
        options: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): Map<String, Any?> {
        TODO("APR: use JVM coroutine/suspend — MOVE $url with Destination: $dest header")
    }

    fun cancelSuspendedOperation() {
        TODO("APR: cancel the in-flight JVM HTTP request associated with this adapter")
    }
}
