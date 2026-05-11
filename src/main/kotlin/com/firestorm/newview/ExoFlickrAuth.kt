package com.firestorm.newview

typealias AuthorizedCallback = (success: Boolean, params: Map<String, Any?>) -> Unit

class ExoFlickrAuth(private val callback: AuthorizedCallback) {

    companion object {
        @Volatile
        private var authorisationInProgress: Boolean = false

        fun create(callback: AuthorizedCallback): ExoFlickrAuth? {
            if (authorisationInProgress) return null
            return ExoFlickrAuth(callback)
        }
    }

    private var authenticating: Boolean = true

    init {
        authorisationInProgress = true
        val token = SavedPerAccountSettings.getString("ExodusFlickrToken")
        val secret = SavedPerAccountSettings.getString("ExodusFlickrTokenSecret")
        if (token.isEmpty() || secret.isEmpty()) {
            beginAuthorisation()
        } else {
            checkAuthorisation()
        }
    }

    private fun finish() {
        if (authenticating) {
            authenticating = false
            authorisationInProgress = false
        }
    }

    private fun checkAuthorisation() {
        ExoFlickr.request("flickr.test.login", emptyMap()) { success, response ->
            checkResult(success, response)
        }
    }

    private fun checkResult(success: Boolean, response: Map<String, Any?>) {
        if (!success || response["stat"]?.toString() != "ok") {
            beginAuthorisation()
        } else {
            callback(true, emptyMap())
            finish()
        }
    }

    private fun beginAuthorisation() {
        TODO("APR: show 'ExodusFlickrVerificationExplanation' notification dialog; on user response call explanationCallback(selectedOption)")
    }

    private fun explanationCallback(option: Int) {
        if (option == 0) {
            SavedPerAccountSettings.setString("ExodusFlickrToken", "")
            SavedPerAccountSettings.setString("ExodusFlickrTokenSecret", "")

            val params = mutableMapOf<String, Any?>("oauth_callback" to "oob")
            ExoFlickr.signRequest(params, "GET", "https://www.flickr.com/services/oauth/request_token")
            val url = buildGetUrl("https://www.flickr.com/services/oauth/request_token", params)
            TODO("APR: HTTP GET $url; parse query-string response via parseQueryString; call gotRequestToken(statusOk, parsedMap)")
        } else {
            callback(false, emptyMap())
            finish()
        }
    }

    private fun gotRequestToken(success: Boolean, params: Map<String, Any?>) {
        if (!success) {
            callback(false, emptyMap())
            finish()
            return
        }
        val token = params["oauth_token"]?.toString() ?: ""
        val secret = params["oauth_token_secret"]?.toString() ?: ""
        SavedPerAccountSettings.setString("ExodusFlickrToken", token)
        SavedPerAccountSettings.setString("ExodusFlickrTokenSecret", secret)

        TODO("APR: spawnWebBrowser 'https://www.flickr.com/services/oauth/authorize?perms=write&oauth_token=$token'; show 'ExodusFlickrVerificationPrompt' notification; on response call gotVerifier(selectedOption, response[\"oauth_verifier\"].toString())")
    }

    private fun gotVerifier(option: Int, verifier: String) {
        if (option == 1) {
            callback(false, emptyMap())
            finish()
            return
        }
        val params = mutableMapOf<String, Any?>("oauth_verifier" to verifier)
        ExoFlickr.signRequest(params, "GET", "https://www.flickr.com/services/oauth/access_token")
        val url = buildGetUrl("https://www.flickr.com/services/oauth/access_token", params)
        TODO("APR: HTTP GET $url; parse query-string response via parseQueryString; call gotAccessToken(statusOk, parsedMap)")
    }

    private fun gotAccessToken(success: Boolean, params: Map<String, Any?>) {
        if (success) {
            SavedPerAccountSettings.setString("ExodusFlickrToken", params["oauth_token"]?.toString() ?: "")
            SavedPerAccountSettings.setString("ExodusFlickrTokenSecret", params["oauth_token_secret"]?.toString() ?: "")
            SavedPerAccountSettings.setString("ExodusFlickrFullName", params["fullname"]?.toString() ?: "")
            SavedPerAccountSettings.setString("ExodusFlickrNSID", params["user_nsid"]?.toString() ?: "")
            SavedPerAccountSettings.setString("ExodusFlickrUsername", params["username"]?.toString() ?: "")
            callback(true, params)
        } else {
            TODO("APR: show 'ExodusFlickrVerificationFailed' notification")
            callback(false, params)
        }
        finish()
    }

    private fun buildGetUrl(base: String, params: Map<String, Any?>): String {
        val query = params.entries.joinToString("&") { (k, v) ->
            "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v.toString(), "UTF-8")}"
        }
        return "$base?$query"
    }
}

private fun parseQueryString(raw: String): Map<String, String> =
    raw.split("&").mapNotNull { pair ->
        val idx = pair.indexOf('=')
        if (idx < 0) null
        else pair.substring(0, idx) to java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
    }.toMap()
