package com.firestorm.llcorehttp

interface HttpHandler {
    fun onCompleted(handle: Long, response: HttpResponse)
}
