package com.firestorm.llcorehttp

class HttpResponse {

    data class HttpStatus(val type: Int, val status: Int) {
        companion object {
            const val EXT_CURL_EASY = 0
            const val EXT_CURL_MULTI = 1
            const val LLCORE = 2
        }

        fun isOk(): Boolean = status == 0

        fun isHttpStatus(): Boolean = type in 100..999

        fun toTerseString(): String {
            val prefix = when (type) {
                EXT_CURL_EASY -> "Easy"
                EXT_CURL_MULTI -> "Multi"
                LLCORE -> "Core"
                in 100..999 -> "Http"
                else -> "Unknown"
            }
            return "${prefix}_${status.toUInt()}"
        }
    }

    data class TransferStats(
        var sizeDownload: Double = 0.0,
        var totalTime: Double = 0.0,
        var speedDownload: Double = 0.0
    )

    var status: HttpStatus = HttpStatus(HttpStatus.LLCORE, 0)
    var body: ByteArray? = null
    var headers: HttpHeaders = HttpHeaders()
    var contentType: String = ""
    var retryAfter: Float = 0f
    var replyOffset: UInt = 0u
    var replyLength: UInt = 0u
    var replyFullLength: UInt = 0u
    var retries: UInt = 0u
    var retries503: UInt = 0u
    var requestUrl: String = ""
    var requestMethod: String = ""
    var transferStats: TransferStats? = null
}
