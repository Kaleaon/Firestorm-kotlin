package com.firestorm.newview

import java.util.UUID

object FSPrimfeedConnect {

    enum class ConnectionState {
        PRIMFEED_DISCONNECTED,
        PRIMFEED_CONNECTING,
        PRIMFEED_CONNECTED,
        PRIMFEED_POSTING,
        PRIMFEED_POSTED,
        PRIMFEED_POST_FAILED,
        PRIMFEED_DISCONNECTING
    }

    private var mPostCallback: ((success: Boolean, url: String) -> Unit)? = null
    private var mConnectionState: ConnectionState = ConnectionState.PRIMFEED_DISCONNECTED

    fun uploadPhoto(
        params: Map<String, Any>,
        imageData: ByteArray,
        imageCodec: String,
        callback: (success: Boolean, url: String) -> Unit
    ) {
        if (!FSPrimfeedAuth.isAuthorized()) {
            callback(false, "")
            return
        }
        mPostCallback = callback
        Thread { uploadPhotoCoro(params, imageData, imageCodec) }.start()
    }

    private fun uploadPhotoCoro(params: Map<String, Any>, imageData: ByteArray, imageCodec: String) {
        setConnectionState(ConnectionState.PRIMFEED_POSTING)

        val fmt = if (imageCodec == "jpg") "jpg" else "png"
        val boundary = "----------------------------0123456789abcdef"
        val sep = "\n"
        val dash = "--$boundary"

        val body = StringBuilder()
        fun addPart(name: String, value: String) {
            body.append("$dash$sep")
            body.append("Content-Disposition: form-data; name=\"$name\"$sep$sep")
            body.append("$value$sep")
        }

        addPart("commercial", if (params["commercial"] as? Boolean == true) "true" else "false")
        addPart("rating", params["rating"]?.toString() ?: "")
        addPart("content", params["content"]?.toString() ?: "")
        addPart("publicGallery", if (params["post_to_public_gallery"] as? Boolean == true) "true" else "false")

        val location = params["location"]?.toString()
        if (!location.isNullOrEmpty()) {
            addPart("location", location)
        }

        body.append("$dash$sep")
        body.append("Content-Disposition: form-data; name=\"image\"; filename=\"snapshot.$fmt\"$sep")
        body.append("Content-Type: image/$fmt$sep$sep")

        val multipartPrefix = body.toString().toByteArray(Charsets.UTF_8)
        val multipartSuffix = "$sep$dash--$sep".toByteArray(Charsets.UTF_8)
        val fullBody = multipartPrefix + imageData + multipartSuffix

        System.err.println("FSPrimfeedConnect: uploadPhotoCoro not yet implemented")

        // After real HTTP call:
        // val success = httpStatus == 200
        // val url = if (success) responseJson["url"] else ""
        // mPostCallback?.invoke(success, url)
        // setConnectionState(if (success) ConnectionState.PRIMFEED_POSTED else ConnectionState.PRIMFEED_POST_FAILED)
    }

    fun setConnectionState(state: ConnectionState) {
        mConnectionState = state
    }

    fun getConnectionState(): ConnectionState = mConnectionState

    fun isTransactionOngoing(): Boolean =
        mConnectionState == ConnectionState.PRIMFEED_CONNECTING ||
        mConnectionState == ConnectionState.PRIMFEED_POSTING ||
        mConnectionState == ConnectionState.PRIMFEED_DISCONNECTING
}
