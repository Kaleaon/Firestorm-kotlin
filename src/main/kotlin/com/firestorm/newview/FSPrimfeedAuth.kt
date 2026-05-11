package com.firestorm.newview

import java.util.concurrent.atomic.AtomicBoolean

typealias AuthorizedCallback = (success: Boolean, response: LLSD) -> Unit

/*
 * Primfeed OAuth login flow (https://docs.primfeed.com/api/third-party-viewers):
 *
 *  1. POST create-login-request  → get requestId
 *  2. Open browser to oauth/viewer?r=<requestId>&v=<apiKey>
 *  3. User approves; in-world chat delivers "#PRIMFEED_OAUTH: <token>"
 *  4. POST validate-request with Bearer token + requestId
 *  5. GET user  → persist plan/username/link; fire callback
 */
class FSPrimfeedAuth private constructor(private val callback: AuthorizedCallback) {

    private var oauthToken: String = ""
    private var requestId: String = ""
    private var chatMessageConnection: (() -> Unit)? = null

    init {
        chatMessageConnection = LLNotificationsUI.LLNotificationManager
            .instance()
            .getChatHandler()
            .addNewChatCallback { message -> onChatMessage(message) }
    }

    fun destroy() {
        try {
            chatMessageConnection?.invoke()
        } catch (_: Exception) {}
        chatMessageConnection = null
    }

    fun onOauthTokenReceived(oauthToken: String) {
        if (oauthToken.isEmpty()) {
            callback(false, LLSD())
            return
        }
        this.oauthToken = oauthToken
        validateRequest()
    }

    fun onChatMessage(message: LLSD) {
        val prefix = "#PRIMFEED_OAUTH: "
        val msg = message["message"].asString()
        if (msg.startsWith(prefix)) {
            val token = msg.removePrefix(prefix)
            onOauthTokenReceived(token)
        }
    }

    fun beginLoginRequest() {
        val viewerApiKey = gSavedSettings.getString("FSPrimfeedViewerApiKey")
        val userUuid     = gAgent.getId().toString()
        val url          = "https://api.primfeed.com/pf/viewer/create-login-request"

        val headers = mapOf(
            "User-Agent"       to FS_PF_USER_AGENT,
            "pf-viewer-api-key" to viewerApiKey,
            "pf-user-uuid"     to userUuid
        )

        TODO("APR: use JVM equivalent")
        // FSCoreHttpUtil.callbackHttpPostRaw(url, "", headers, PRIMFEED_CONNECT_TIMEOUT,
        //     onSuccess = { data -> handleHttpResponse(data) { success, resp -> gotRequestId(success, resp) } },
        //     onFailure = { data -> handleHttpResponse(data) { success, resp -> gotRequestId(success, resp) } }
        // )
    }

    fun checkUserStatus() {
        val viewerApiKey = gSavedSettings.getString("FSPrimfeedViewerApiKey")
        val url          = "https://api.primfeed.com/pf/viewer/user"

        val headers = mapOf(
            "User-Agent"        to FS_PF_USER_AGENT,
            "Authorization"     to "Bearer $oauthToken",
            "pf-viewer-api-key" to viewerApiKey
        )

        TODO("APR: use JVM equivalent")
        // FSCoreHttpUtil.callbackHttpGetRaw(url, headers, PRIMFEED_CONNECT_TIMEOUT,
        //     onSuccess = { data -> handleHttpResponse(data) { success, resp -> gotUserStatus(success, resp) } },
        //     onFailure = { data -> handleHttpResponse(data) { success, resp -> gotUserStatus(success, resp) } }
        // )
    }

    private fun gotRequestId(success: Boolean, response: LLSD) {
        if (!success) {
            LLNotificationsUtil.add("PrimfeedLoginRequestFailed")
            callback(false, LLSD())
            return
        }
        requestId = response["requestId"].asString()
        if (requestId.isEmpty()) {
            LLNotificationsUtil.add("PrimfeedLoginRequestFailed")
            callback(false, LLSD())
            return
        }
        val viewerApiKey = gSavedSettings.getString("FSPrimfeedViewerApiKey")
        val authUrl = "https://www.primfeed.com/oauth/viewer?r=$requestId&v=$viewerApiKey"
        TODO("APR: use JVM equivalent")
        // gViewerWindow.getWindow().spawnWebBrowser(authUrl, true)
    }

    private fun validateRequest() {
        val viewerApiKey = gSavedSettings.getString("FSPrimfeedViewerApiKey")
        val url          = "https://api.primfeed.com/pf/viewer/validate-request"

        val headers = mapOf(
            "User-Agent"              to FS_PF_USER_AGENT,
            "Authorization"           to "Bearer $oauthToken",
            "pf-viewer-api-key"       to viewerApiKey,
            "pf-viewer-request-id"    to requestId
        )

        try {
            TODO("APR: use JVM equivalent")
            // FSCoreHttpUtil.callbackHttpPostRaw(url, "", headers, PRIMFEED_CONNECT_TIMEOUT,
            //     onSuccess = { data -> handleHttpResponse(data) { success, resp -> gotValidateResponse(success, resp) } },
            //     onFailure = { data -> handleHttpResponse(data) { success, resp -> gotValidateResponse(success, resp) } }
            // )
        } catch (e: Exception) {
            LLLog.warn("Primfeed", "Primfeed validation failed: ${e.message}")
        }
    }

    private fun gotValidateResponse(success: Boolean, response: LLSD) {
        if (!success) {
            LLNotificationsUtil.add("PrimfeedValidateFailed")
            callback(false, response)
            return
        }
        checkUserStatus()
    }

    private fun gotUserStatus(success: Boolean, response: LLSD) {
        if (success &&
            response.has("plan") &&
            response.has("username") &&
            response.has("link")
        ) {
            gSavedPerAccountSettings.setString("FSPrimfeedOAuthToken",   oauthToken)
            gSavedPerAccountSettings.setString("FSPrimfeedPlan",         response["plan"].asString())
            gSavedPerAccountSettings.setString("FSPrimfeedProfileLink",  response["link"].asString())
            gSavedPerAccountSettings.setString("FSPrimfeedUsername",     response["username"].asString())
            FSPrimfeedConnect.instance().setConnectionState(FSPrimfeedConnect.PRIMFEED_CONNECTED)

            val eventData = response.deepCopy().apply {
                put("responseType", "primfeed_user_info")
            }
            sPrimfeedAuthPump.post(eventData)
            callback(true, response)
            return
        }
        LLNotificationsUtil.add("PrimfeedUserStatusFailed")
        FSPrimfeedConnect.instance().setConnectionState(FSPrimfeedConnect.PRIMFEED_DISCONNECTED)
        callback(false, response)
    }

    companion object {
        private var sPrimfeedAuth: FSPrimfeedAuth? = null

        // Event pump used to broadcast auth outcomes to other subsystems.
        val sPrimfeedAuthPump: LLEventStream = LLEventStream("PrimfeedAuthResponse")

        private val sAuthorisationInProgress = AtomicBoolean(false)

        private const val PRIMFEED_CONNECT_TIMEOUT: UInt = 300u

        fun create(callback: AuthorizedCallback): FSPrimfeedAuth {
            sPrimfeedAuth?.let { return it }

            val auth = FSPrimfeedAuth(callback)
            FSPrimfeedConnect.instance().setConnectionState(FSPrimfeedConnect.PRIMFEED_CONNECTING)

            auth.oauthToken = gSavedPerAccountSettings.getString("FSPrimfeedOAuthToken")
            if (auth.oauthToken.isEmpty()) {
                auth.beginLoginRequest()
            } else {
                auth.checkUserStatus()
            }
            sPrimfeedAuth = auth
            return auth
        }

        fun isPendingAuth(): Boolean = sPrimfeedAuth != null

        fun isAuthorized(): Boolean =
            gSavedPerAccountSettings.getString("FSPrimfeedOAuthToken").isNotEmpty()

        fun initiateAuthRequest() {
            if (sPrimfeedAuth != null) {
                LLNotificationsUtil.add("PrimfeedAuthorizationAlreadyInProgress")
                return
            }
            sPrimfeedAuth = create { success, response ->
                val eventData = response.deepCopy().apply {
                    put("responseType", "primfeed_auth_response")
                    put("success", success)
                }
                sPrimfeedAuthPump.post(eventData)
                sPrimfeedAuth?.destroy()
                sPrimfeedAuth = null
            }
            FSPrimfeedConnect.instance().setConnectionState(FSPrimfeedConnect.PRIMFEED_CONNECTING)
        }

        fun resetAuthStatus() {
            sPrimfeedAuth?.destroy()
            sPrimfeedAuth = null
            gSavedPerAccountSettings.setString("FSPrimfeedOAuthToken",  "")
            gSavedPerAccountSettings.setString("FSPrimfeedProfileLink", "")
            gSavedPerAccountSettings.setString("FSPrimfeedPlan",        "")
            gSavedPerAccountSettings.setString("FSPrimfeedUsername",    "")
            val eventData = LLSD.map().apply {
                put("responseType", "primfeed_auth_reset")
                put("success", false)
            }
            sPrimfeedAuthPump.post(eventData)
            FSPrimfeedConnect.instance().setConnectionState(FSPrimfeedConnect.PRIMFEED_DISCONNECTED)
        }
    }
}
