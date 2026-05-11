package com.firestorm.newview

import com.firestorm.llcorehttp.HttpResponse

abstract class HttpHandler {

    abstract fun onCompleted(handle: Long, response: HttpResponse)
}
