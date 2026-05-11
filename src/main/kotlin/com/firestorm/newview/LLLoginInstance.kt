package com.firestorm.newview

import java.util.UUID

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private const val LOGIN_MAX_RETRIES = 0
private const val LOGIN_SRV_TIMEOUT_MIN = 10.0f
private const val LOGIN_SRV_TIMEOUT_MAX = 180.0f
private const val LOGIN_DNS_TIMEOUT_FACTOR = 0.9f

private const val TOS_REPLY_PUMP = "lllogininstance_tos_callback"
private const val TOS_LISTENER_NAME = "lllogininstance_tos"

// ---------------------------------------------------------------------------
// LLLoginInstance  (singleton → object)
// ---------------------------------------------------------------------------

object LLLoginInstance {

    // Marker interface – callers may subclass for disposable resources attached
    // to the login lifecycle.
    interface Disposable

    private var mLoginState: String = "offline"
    private var mRequestData: MutableMap<String, Any> = mutableMapOf()
    private var mResponseData: MutableMap<String, Any> = mutableMapOf()
    private var mAttemptComplete: Boolean = false
    private var mSaveMFA: Boolean = true
    private var mTransferRate: Double = 0.0

    private var mSerialNumber: String = ""
    private var mLastExecEvent: Int = 0
    private var mLastExecDuration: Int = 0
    private var mLastAgentSessionId: UUID = UUID(0L, 0L)
    private var mPlatform: String = ""
    private var mPlatformVersion: String = ""
    private var mPlatformVersionName: String = ""

    // Dispatcher: maps change-type string to handler
    private val mDispatcher: MutableMap<String, (Map<String, Any>) -> Unit> = mutableMapOf()

    init {
        mDispatcher["fail.login"]    = { event -> handleLoginFailure(event) }
        mDispatcher["connect"]       = { event -> handleLoginSuccess(event) }
        mDispatcher["disconnect"]    = { event -> handleDisconnect(event) }
        mDispatcher["indeterminate"] = { event -> handleIndeterminate(event) }
    }

    // -----------------------------------------------------------------------
    // Setters
    // -----------------------------------------------------------------------

    fun setSerialNumber(sn: String) { mSerialNumber = sn }
    fun setLastExecEvent(lee: Int) { mLastExecEvent = lee }
    fun setLastExecDuration(duration: Int) { mLastExecDuration = duration }
    fun setLastAgentSessionId(id: UUID) { mLastAgentSessionId = id }
    fun setPlatformInfo(platform: String, platformVersion: String, platformName: String) {
        mPlatform = platform
        mPlatformVersion = platformVersion
        mPlatformVersionName = platformName
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    fun authFailure(): Boolean = mAttemptComplete && mLoginState == "offline"
    fun authSuccess(): Boolean = mAttemptComplete && mLoginState == "online"
    fun getLoginState(): String = mLoginState
    fun saveMFA(): Boolean = mSaveMFA
    fun getLastTransferRateBPS(): Double = mTransferRate

    fun getResponse(): MutableMap<String, Any> = mResponseData
    fun getResponse(key: String): Any? = mResponseData[key]
    fun hasResponse(key: String): Boolean = mResponseData.containsKey(key)

    // -----------------------------------------------------------------------
    // Connection management
    // -----------------------------------------------------------------------

    fun connect(credentials: Any?) {
        TODO("APR: use JVM equivalent – obtain login URIs from LLGridManager and call connect(uri, credentials)")
    }

    fun connect(uri: String, credentials: Any?) {
        mAttemptComplete = false
        constructAuthParams(credentials)
        TODO("APR: use JVM equivalent – mLoginModule.connect(uri, mRequestData)")
    }

    fun reconnect() {
        TODO("APR: use JVM equivalent – obtain login URIs and call mLoginModule.connect(uri, mRequestData); show progress window")
    }

    fun disconnect() {
        mAttemptComplete = false
        mRequestData.clear()
        TODO("APR: use JVM equivalent – mLoginModule.disconnect()")
    }

    // -----------------------------------------------------------------------
    // Auth parameter construction
    // -----------------------------------------------------------------------

    private fun constructAuthParams(userCredentials: Any?) {
        val requestedOptions = mutableListOf(
            "inventory-root",
            "inventory-skeleton",
            "inventory-lib-root",
            "inventory-lib-owner",
            "inventory-skel-lib",
            "initial-outfit",
            "gestures",
            "display_names",
            "event_categories",
            "event_notifications",
            "classified_categories",
            "adult_compliant",
            "buddy-list",
            "newuser-config",
            "ui-config",
            "advanced-mode",
            "max-agent-groups",
            "map-server-url",
            "voice-config",
            "tutorial_setting",
            "login-flags",
            "global-textures"
        )

        TODO("APR: use JVM equivalent – build request_params map (start string, mac hash, version, channel, platform, mfa_hash, etc.) and set mRequestData")
    }

    // -----------------------------------------------------------------------
    // Event handling
    // -----------------------------------------------------------------------

    fun handleLoginEvent(event: Map<String, Any>): Boolean {
        if (!event.containsKey("state") || !event.containsKey("change") || !event.containsKey("progress")) {
            error("Unknown message from LLLogin: $event")
        }

        mLoginState = event["state"] as? String ?: "offline"
        @Suppress("UNCHECKED_CAST")
        mResponseData = (event["data"] as? MutableMap<String, Any>) ?: mutableMapOf()

        (event["transfer_rate"] as? Double)?.let { mTransferRate = it }

        val change = event["change"] as? String ?: return false
        mDispatcher[change]?.invoke(event)
        return false
    }

    private fun handleLoginFailure(event: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val response = event["data"] as? Map<String, Any> ?: emptyMap()
        val reasonResponse = response["reason"] as? String ?: ""
        val messageResponse = response["message"] as? String ?: ""

        if (response.containsKey("mfa_hash")) {
            (mRequestData["params"] as? MutableMap<String, Any>)?.let {
                it["mfa_hash"] = response["mfa_hash"]!!
                it["token"] = ""
            }
            saveMFAHash(response)
        }

        when (reasonResponse) {
            "tos" -> {
                TODO("APR: show TOS floater with messageResponse; listen on TOS_REPLY_PUMP for handleTOSResponse(v, 'agree_to_tos')")
            }
            "critical" -> {
                TODO("APR: show critical message floater with messageResponse; listen on TOS_REPLY_PUMP for handleTOSResponse(v, 'read_critical')")
            }
            "update" -> {
                val loginVersion = ((response["message_args"] as? Map<*, *>)?.get("VERSION") as? String) ?: ""
                TODO("APR: show RequiredUpdate notification with loginVersion and release notes URL; on dismiss call handleLoginDisallowed")
            }
            "mfa_challenge" -> {
                TODO("APR: show MFA token prompt dialog; on response call handleMFAChallenge")
            }
            "key", "presence", "connect" -> {
                attemptComplete()
            }
            else -> {
                if (messageResponse.isNotEmpty() || (response["message_id"] as? String)?.isNotEmpty() == true) {
                    attemptComplete()
                } else {
                    TODO("APR: show LoginFailedUnknown notification; on dismiss call handleLoginDisallowed")
                }
            }
        }
    }

    private fun handleLoginDisallowed(notification: Map<String, Any>, response: Map<String, Any>) {
        attemptComplete()
    }

    private fun handleLoginSuccess(event: Map<String, Any>) {
        attemptComplete()
        mRequestData.clear()
    }

    private fun handleDisconnect(event: Map<String, Any>) {
        // placeholder – no action required on plain disconnect
    }

    private fun handleIndeterminate(event: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val data = event["data"] as? Map<String, Any> ?: return
        val message = data["message"] as? String ?: return
        TODO("APR: post progress update with desc=$message to LLProgressView event pump")
    }

    private fun handleTOSResponse(accepted: Boolean, key: String): Boolean {
        if (accepted) {
            (mRequestData["params"] as? MutableMap<String, Any>)?.set(key, true)

            val token = ((mRequestData["params"] as? Map<*, *>)?.get("token") as? String) ?: ""
            if (token.isNotEmpty()) {
                TODO("APR: show MFA challenge again because the old token likely expired mid-TOS flow")
            } else {
                reconnect()
            }
        } else {
            attemptComplete()
        }
        TODO("APR: stop listening on TOS_REPLY_PUMP / TOS_LISTENER_NAME")
        return true
    }

    private fun showMFAChallenge(message: String) {
        TODO("APR: show PromptMFAToken(WithSave) notification with message; on response call handleMFAChallenge")
    }

    private fun handleMFAChallenge(notif: Map<String, Any>, response: Map<String, Any>): Boolean {
        val continueClicked = response["continue"] as? Boolean ?: false
        var token = response["token"] as? String ?: ""

        // Strip whitespace so users can paste tokens with spaces
        token = token.replace(Regex("\\s"), "")

        return if (continueClicked && token.isNotEmpty()) {
            (mRequestData["params"] as? MutableMap<String, Any>)?.set("token", token)
            mSaveMFA = (response["ignore"] as? Boolean) ?: false
            reconnect()
            true
        } else {
            attemptComplete()
            true
        }
    }

    fun saveMFAHash(response: Map<String, Any>) {
        TODO("APR: use JVM equivalent – if response has mfa_hash and user wants to be remembered and saveMFA(), store/remove mfa_hash in secure store for (grid, userId)")
    }

    private fun attemptComplete() {
        mAttemptComplete = true
    }
}

// ---------------------------------------------------------------------------
// Free function: construct_start_string
// ---------------------------------------------------------------------------

fun constructStartString(): String {
    TODO("APR: use JVM equivalent – inspect LLStartUp.getStartSLURL() type and build 'uri:region&x&y&z', 'home', or 'last'")
}
