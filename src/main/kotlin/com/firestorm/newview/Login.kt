package com.firestorm.newview

enum class LoginConnectionState {
    OFFLINE,
    AUTHENTICATING,
    DOWNLOADING,
    ONLINE
}

data class LoginProgressEvent(
    val state: String,
    val change: String,
    val progress: Float = 0f,
    val transferRate: Float = 0f,
    val data: Map<String, Any?> = emptyMap()
)

class Login {

    private val eventListeners: MutableList<(LoginProgressEvent) -> Unit> = mutableListOf()

    fun connect(uri: String, credentials: Map<String, Any?>) {
        Thread {
            loginCoroutine(uri, credentials)
        }.also {
            it.isDaemon = true
            it.name = "Login"
            it.start()
        }
    }

    fun disconnect() {
        sendProgressEvent("offline", "disconnect")
    }

    fun addEventListener(listener: (LoginProgressEvent) -> Unit) {
        eventListeners.add(listener)
    }

    fun removeEventListener(listener: (LoginProgressEvent) -> Unit) {
        eventListeners.remove(listener)
    }

    private fun sendProgressEvent(state: String, change: String, data: Map<String, Any?> = emptyMap()) {
        val event = LoginProgressEvent(
            state = state,
            change = change,
            data = data
        )
        for (listener in eventListeners) {
            listener(event)
        }
    }

    private fun loginCoroutine(uri: String, credentials: Map<String, Any?>) {
        val printableParams = credentials.mapValues { (k, v) ->
            if (k == "passwd") "*******" else v
        }

        var attempts = 0
        var requestUri = uri
        var method = credentials["method"]?.toString() ?: "login_to_simulator"

        try {
            while (true) {
                attempts++
                sendProgressEvent(
                    "offline", "authenticating",
                    mapOf("attempt" to attempts, "request" to printableParams)
                )

                val response = performXmlRpcLogin(requestUri, method, credentials)
                    ?: run {
                        sendProgressEvent("offline", "fail.login", mapOf("reason" to "CURLError", "message" to "Connection failed"))
                        return
                    }

                val status = response["status"]?.toString() ?: "OtherError"

                if (status == "Downloading") {
                    sendProgressEvent("offline", "downloading")
                    continue
                }

                if (status == "Complete") {
                    val loginResult = response["responses"] as? Map<*, *>
                    val loginOutcome = loginResult?.get("login")?.toString()

                    if (loginOutcome == "indeterminate") {
                        sendProgressEvent("offline", "indeterminate", loginResult?.let { mapOf("responses" to it) } ?: emptyMap())
                        requestUri = loginResult?.get("next_url")?.toString() ?: break
                        method = loginResult?.get("next_method")?.toString() ?: method
                        continue
                    }

                    if (loginOutcome == "true") {
                        sendProgressEvent("online", "connect", loginResult?.let { mapOf("responses" to it) } ?: emptyMap())
                    } else {
                        sendProgressEvent("offline", "fail.login", loginResult?.let { mapOf("responses" to it) } ?: emptyMap())
                    }
                    return
                }

                val errorResponse = mapOf(
                    "reason"    to status,
                    "errorcode" to (response["errorcode"] ?: ""),
                    "message"   to (response["error"] ?: "")
                )
                sendProgressEvent("offline", "fail.login", errorResponse)
                return
            }
        } catch (e: Exception) {
            sendProgressEvent("offline", "fail.login", mapOf("reason" to "OtherError", "message" to (e.message ?: "")))
        }
    }

    private fun performXmlRpcLogin(
        uri: String,
        method: String,
        credentials: Map<String, Any?>
    ): Map<String, Any?>? {
        TODO("APR: use JVM equivalent - send XMLRPC login request to $uri using method $method")
    }
}
