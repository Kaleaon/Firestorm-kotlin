package com.firestorm.newview

const val FLICKR_CONNECT_TIMEOUT: UInt = 600u

object LLFlickrConnect {

    enum class EConnectionState {
        FLICKR_NOT_CONNECTED,
        FLICKR_CONNECTION_IN_PROGRESS,
        FLICKR_CONNECTED,
        FLICKR_CONNECTION_FAILED,
        FLICKR_POSTING,
        FLICKR_POSTED,
        FLICKR_POST_FAILED,
        FLICKR_DISCONNECTING,
        FLICKR_DISCONNECT_FAILED
    }

    private var connectionState: EConnectionState = EConnectionState.FLICKR_NOT_CONNECTED
    private var connected: Boolean = false
    private var info: MutableMap<String, Any?> = mutableMapOf()
    private var refreshInfo: Boolean = false
    private var readFromMaster: Boolean = false

    val stateListeners: MutableList<(Map<String, Any?>) -> Unit> = mutableListOf()
    val infoListeners: MutableList<(Map<String, Any?>) -> Unit> = mutableListOf()

    fun isConnected(): Boolean = connected

    fun isTransactionOngoing(): Boolean =
        connectionState == EConnectionState.FLICKR_CONNECTION_IN_PROGRESS ||
        connectionState == EConnectionState.FLICKR_POSTING ||
        connectionState == EConnectionState.FLICKR_DISCONNECTING

    fun getConnectionState(): EConnectionState = connectionState

    fun connectToFlickr(requestToken: String = "", oauthVerifier: String = "") {
        System.err.println("LLFlickrConnect: connectToFlickr not yet implemented")
    }

    fun disconnectFromFlickr() {
        System.err.println("LLFlickrConnect: disconnectFromFlickr not yet implemented")
    }

    fun checkConnectionToFlickr(autoConnect: Boolean = false) {
        System.err.println("LLFlickrConnect: checkConnectionToFlickr not yet implemented")
    }

    fun loadFlickrInfo() {
        if (refreshInfo) {
            System.err.println("LLFlickrConnect: loadFlickrInfo not yet implemented")
        }
    }

    fun uploadPhoto(imageUrl: String, title: String, description: String, tags: String, safetyLevel: Int) {
        val body: MutableMap<String, Any?> = mutableMapOf(
            "image" to imageUrl,
            "title" to title,
            "description" to description,
            "tags" to tags,
            "safety_level" to safetyLevel
        )
        setConnectionState(EConnectionState.FLICKR_POSTING)
        System.err.println("LLFlickrConnect: uploadPhoto (url) not yet implemented")
    }

    fun uploadPhoto(image: Any, title: String, description: String, tags: String, safetyLevel: Int) {
        setConnectionState(EConnectionState.FLICKR_POSTING)
        System.err.println("LLFlickrConnect: uploadPhoto (image) not yet implemented")
    }

    fun storeInfo(newInfo: Map<String, Any?>) {
        info = newInfo.toMutableMap()
        refreshInfo = false
        infoListeners.forEach { it(info) }
    }

    fun getInfo(): Map<String, Any?> = info

    fun clearInfo() {
        info = mutableMapOf()
    }

    fun setDataDirty() {
        refreshInfo = true
    }

    fun setConnectionState(state: EConnectionState) {
        when (state) {
            EConnectionState.FLICKR_CONNECTED -> {
                readFromMaster = true
                setConnected(true)
                setDataDirty()
            }
            EConnectionState.FLICKR_NOT_CONNECTED -> {
                setConnected(false)
            }
            EConnectionState.FLICKR_POSTED -> {
                readFromMaster = false
            }
            else -> Unit
        }

        if (connectionState != state) {
            connectionState = state
            val stateInfo: Map<String, Any?> = mapOf("enum" to state)
            stateListeners.forEach { it(stateInfo) }
        }
    }

    fun setConnected(isConnected: Boolean) {
        connected = isConnected
    }

    fun openFlickrWeb(url: String) {
        System.err.println("LLFlickrConnect: openFlickrWeb not yet implemented")
    }

    private fun getFlickrConnectURL(route: String = "", includeReadFromMaster: Boolean = false): String {
        return ""
    }

    private fun flickrConnectCoro(requestToken: String, oauthVerifier: String) {
        setConnectionState(EConnectionState.FLICKR_CONNECTION_IN_PROGRESS)
        val body: MutableMap<String, Any?> = mutableMapOf()
        if (requestToken.isNotEmpty()) body["request_token"] = requestToken
        if (oauthVerifier.isNotEmpty()) body["oauth_verifier"] = oauthVerifier

        System.err.println("LLFlickrConnect: flickrConnectCoro not yet implemented")
    }

    private fun testShareStatus(result: Map<String, Any?>): Boolean {
        return false
    }

    private fun flickrShareCoro(share: Map<String, Any?>) {
        System.err.println("LLFlickrConnect: flickrShareCoro not yet implemented")
    }

    private fun flickrShareImageCoro(image: Any, title: String, description: String, tags: String, safetyLevel: Int) {
        System.err.println("LLFlickrConnect: flickrShareImageCoro not yet implemented")
    }

    private fun flickrDisconnectCoro() {
        setConnectionState(EConnectionState.FLICKR_DISCONNECTING)
        System.err.println("LLFlickrConnect: flickrDisconnectCoro not yet implemented")
    }

    private fun flickrConnectedCoro(autoConnect: Boolean) {
        setConnectionState(EConnectionState.FLICKR_CONNECTION_IN_PROGRESS)
        System.err.println("LLFlickrConnect: flickrConnectedCoro not yet implemented")
    }

    private fun flickrInfoCoro() {
        System.err.println("LLFlickrConnect: flickrInfoCoro not yet implemented")
    }

    private fun logFlickrConnectError(request: String, status: Int, reason: String, code: String?, description: String?) {
        if (status != 302) {
            System.err.println("LLFlickrConnect: logFlickrConnectError not yet implemented")
        }
    }

    private fun toastUserForFlickrSuccess() {
        System.err.println("LLFlickrConnect: toastUserForFlickrSuccess not yet implemented")
    }
}
