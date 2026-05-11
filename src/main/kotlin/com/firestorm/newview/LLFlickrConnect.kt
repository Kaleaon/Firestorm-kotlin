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
        TODO("APR: launch coroutine flickrConnectCoro(requestToken, oauthVerifier) on a background thread/coroutine")
    }

    fun disconnectFromFlickr() {
        TODO("APR: launch coroutine flickrDisconnectCoro() on a background thread/coroutine")
    }

    fun checkConnectionToFlickr(autoConnect: Boolean = false) {
        TODO("APR: launch coroutine flickrConnectedCoro(autoConnect) on a background thread/coroutine")
    }

    fun loadFlickrInfo() {
        if (refreshInfo) {
            TODO("APR: launch coroutine flickrInfoCoro() on a background thread/coroutine")
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
        TODO("APR: launch coroutine flickrShareCoro(body) on a background thread/coroutine")
    }

    fun uploadPhoto(image: Any, title: String, description: String, tags: String, safetyLevel: Int) {
        setConnectionState(EConnectionState.FLICKR_POSTING)
        TODO("APR: launch coroutine flickrShareImageCoro(image, title, description, tags, safetyLevel) on a background thread/coroutine")
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
        TODO("APR: open the Flickr auth URL in an embedded web floater and set keyboard focus to it")
    }

    private fun getFlickrConnectURL(route: String = "", includeReadFromMaster: Boolean = false): String {
        TODO("APR: obtain FlickrConnect capability URL from current viewer region; append route and optional ?read_from_master=true")
    }

    private fun flickrConnectCoro(requestToken: String, oauthVerifier: String) {
        setConnectionState(EConnectionState.FLICKR_CONNECTION_IN_PROGRESS)
        val body: MutableMap<String, Any?> = mutableMapOf()
        if (requestToken.isNotEmpty()) body["request_token"] = requestToken
        if (oauthVerifier.isNotEmpty()) body["oauth_verifier"] = oauthVerifier

        TODO("APR: HTTP PUT to getFlickrConnectURL(\"/connection\") with body; on HTTP_FOUND redirect call openFlickrWeb; on success call setConnectionState(FLICKR_CONNECTED); on other error call setConnectionState(FLICKR_CONNECTION_FAILED)")
    }

    private fun testShareStatus(result: Map<String, Any?>): Boolean {
        TODO("APR: extract HTTP status from result; return true on 2xx; on HTTP_FOUND open redirect URL; on HTTP_NOT_FOUND call connectToFlickr(); on other error set FLICKR_POST_FAILED")
    }

    private fun flickrShareCoro(share: Map<String, Any?>) {
        TODO("APR: HTTP POST to getFlickrConnectURL(\"/share/photo\", true) with share body; on success toast user and set FLICKR_POSTED")
    }

    private fun flickrShareImageCoro(image: Any, title: String, description: String, tags: String, safetyLevel: Int) {
        TODO("APR: determine image format (PNG/JPEG); build multipart/form-data body with title, description, tags, safety_level, image bytes; HTTP POST to getFlickrConnectURL(\"/share/photo\", true) with timeout FLICKR_CONNECT_TIMEOUT and retries=0; on success toast user and set FLICKR_POSTED")
    }

    private fun flickrDisconnectCoro() {
        setConnectionState(EConnectionState.FLICKR_DISCONNECTING)
        TODO("APR: HTTP DELETE to getFlickrConnectURL(\"/connection\"); on success call clearInfo() and set FLICKR_NOT_CONNECTED; on failure set FLICKR_DISCONNECT_FAILED (ignore HTTP_NOT_FOUND as success)")
    }

    private fun flickrConnectedCoro(autoConnect: Boolean) {
        setConnectionState(EConnectionState.FLICKR_CONNECTION_IN_PROGRESS)
        TODO("APR: HTTP GET to getFlickrConnectURL(\"/connection\", true); on HTTP_NOT_FOUND either call connectToFlickr() if autoConnect else set FLICKR_NOT_CONNECTED; on 2xx set FLICKR_CONNECTED; on other error set FLICKR_CONNECTION_FAILED")
    }

    private fun flickrInfoCoro() {
        TODO("APR: HTTP GET to getFlickrConnectURL(\"/info\", true); on HTTP_FOUND open redirect; on 2xx call storeInfo(result); on error log warning")
    }

    private fun logFlickrConnectError(request: String, status: Int, reason: String, code: String?, description: String?) {
        if (status != 302) {
            TODO("APR: log warning \"$request request failed with $status $reason. Reason: $code ($description)\"")
        }
    }

    private fun toastUserForFlickrSuccess() {
        TODO("APR: LLNotificationsUtil::add(\"FlickrConnect\", args=[\"MESSAGE\": LLTrans.getString(\"flickr_post_success\")])")
    }
}
