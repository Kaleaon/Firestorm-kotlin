package com.firestorm.llcorehttp

class HttpOptions {
    var retries: Int = 5
    var maxRetries: Int = 8
    var retryOnStatus: Boolean = true
    var timeout: Int = 30
    var transferTimeout: Int = 0
    var useLLSD: Boolean = false
    var followRedirects: Boolean = false
    var dnsCacheTimeout: Int = -1
    var useRetryAfter: Boolean = true
    var pipelining: Boolean = false
    var wantHeaders: Boolean = false
    var headersOnly: Boolean = false
    var verifyPeer: Boolean = true
    var verifyHost: Boolean = false
    var minBackoffMs: Long = 1_000_000L
    var maxBackoffMs: Long = 5_000_000L
    var trace: Int = 0
    var lastModified: Long = 0L
}
