package com.firestorm.newview

import java.security.MessageDigest

data class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

data class Credential(
    val identifier: Map<String, String>,
    val authenticator: Map<String, String>
) {
    fun userID(): String = when (identifier["type"]) {
        "agent" -> "${identifier["first_name"]}_${identifier["last_name"]}".lowercase()
        "account" -> identifier["account_name"] ?: ""
        else -> ""
    }
    fun identifierType(): String = identifier["type"] ?: ""
}

class PanelLogin(
    private val rect: Rect,
    private val callback: (option: Int, userData: Any?) -> Unit,
    private val callbackData: Any?
) {
    private var passwordModified: Boolean = false
    private var showFavorites: Boolean = false
    private var firstLoginThisInstall: Boolean = false
    private var usernameLength: UInt = 0u
    private var passwordLength: UInt = 0u
    private var locationLength: UInt = 0u
    private var alertNotif: Boolean = false

    init {
        sInstance = this
        firstLoginThisInstall = savedSettings.getBool("FirstLoginThisInstall")
        reshape(rect.width, rect.height)
    }

    fun addFavoritesToStartLocation() {
        if (firstLoginThisInstall) {
            val name = getUsername()
            usernameLength = name.length.toUInt()
            updateLoginButtons()
            return
        }

        val username = getUsername().trim().lowercase()
        usernameLength = username.length.toUInt()
        updateLoginButtons()

        val adjustedName = run {
            val idx = username.indexOfFirst { it == ' ' || it == '.' || it == '_' }
            if (idx == -1) username
            else {
                val first = username.substring(0, idx)
                val last = username.substring(idx + 1)
                if (last == "resident") first else "$first $last"
            }
        }

        TODO("APR: use JVM equivalent - load favorites from stored_favorites XML for $adjustedName")
    }

    fun setFocus(b: Boolean) {
        if (b) giveFocus() else TODO("GPU: unfocus panel")
    }

    private fun updateLoginButtons() {
        val enabled = usernameLength != 0u && passwordLength != 0u && !alertNotif
        TODO("GPU: set login button enabled = $enabled")
    }

    private fun populateUserList(credential: Credential?) {
        TODO("APR: use JVM equivalent - populate username combo from credential store")
    }

    private fun onSelectServer() {
        TODO("APR: use JVM equivalent - apply selected grid and reload favorites/login page")
    }

    private fun onLocationSLURL() {
        TODO("APR: use JVM equivalent - read location combo and call setStartSLURL")
    }

    private fun onUpdateNotification(notify: Map<String, Any>): Boolean {
        val sigtype = notify["sigtype"] as? String
        if (notify["name"] == "PromptOptionalUpdate") {
            when (sigtype) {
                "add" -> alertNotif = true
                "delete" -> alertNotif = false
            }
            updateLoginButtons()
        }
        return false
    }

    private fun getUsername(): String {
        TODO("GPU: read username combo value")
    }

    companion object {
        var sInstance: PanelLogin? = null
        var sCapslockDidNotification: Boolean = false
        var sCredentialSet: Boolean = false

        private val savedSettings = object {
            fun getBool(key: String): Boolean = false
            fun getString(key: String): String = ""
        }

        fun show(rect: Rect, callback: (Int, Any?) -> Unit, callbackData: Any?) {
            if (sInstance == null) {
                PanelLogin(rect, callback, callbackData)
            }
            TODO("GPU: ensure keyboard focus on the login panel")
        }

        fun reshapePanel() {
            sInstance?.let {
                it.reshape(it.rect.width, it.rect.height)
            }
        }

        fun populateFields(credential: Credential?, rememberUser: Boolean, rememberPassword: Boolean) {
            val inst = sInstance ?: return
            TODO("GPU: set remember_name checkbox = $rememberUser, populate user list")
        }

        fun resetFields() {
            val inst = sInstance ?: return
            TODO("APR: use JVM equivalent - reload credential from SecAPI and repopulate list")
        }

        fun getFields(credential: Credential?, rememberUser: Boolean, rememberPassword: Boolean) {
            val inst = sInstance ?: return
            TODO("APR: use JVM equivalent - build Credential from UI field values including MD5 hash")
        }

        fun isCredentialSet(): Boolean = sCredentialSet

        fun areCredentialFieldsDirty(): Boolean {
            val inst = sInstance ?: return false
            TODO("GPU: check if username combo or password field is dirty")
        }

        fun setLocation(slurl: String) {
            TODO("APR: use JVM equivalent - call setStartSLURL")
        }

        fun autologinToLocation(slurl: String) {
            TODO("APR: use JVM equivalent - setStartSLURL then trigger connect")
        }

        fun updateLocationSelectorsVisibility() {
            val inst = sInstance ?: return
            val showServer = savedSettings.getBool("ForceShowGrid")
            TODO("GPU: set server_combo visibility = $showServer")
        }

        fun closePanel() {
            sInstance?.let {
                TODO("GPU: remove panel from parent and delete")
            }
            sInstance = null
        }

        fun loadLoginPage() {
            val inst = sInstance ?: return
            TODO("APR: use JVM equivalent - build login URI with params (lang, version, channel, grid, os, sourceid) and navigate browser")
        }

        fun giveFocus() {
            val inst = sInstance ?: return
            TODO("GPU: focus username combo or password field depending on which is empty")
        }

        fun setAlwaysRefresh(refresh: Boolean) {
            TODO("GPU: set always-refresh on login_html web browser control")
        }

        fun updateServer() {
            val inst = sInstance ?: return
            TODO("APR: use JVM equivalent - reload credential, update links visibility, reload login page")
        }

        fun onUpdateStartSLURL(newStartSlurl: String) {
            val inst = sInstance ?: return
            TODO("APR: use JVM equivalent - parse slurl type and update location combo and grid selector")
        }

        fun getShowFavorites(): Boolean {
            TODO("APR: use JVM equivalent - return gSavedPerAccountSettings ShowFavoritesOnLogin")
        }

        fun getUserName(cred: Credential?): String {
            if (cred == null) return "unknown"
            val ident = cred.identifier
            return when (ident["type"]) {
                "agent" -> {
                    val last = ident["last_name"] ?: ""
                    if (last.equals("resident", ignoreCase = true)) {
                        ident["first_name"] ?: "unknown"
                    } else {
                        "${ident["first_name"]} $last"
                    }
                }
                "account" -> cleanFullName(ident["account_name"] ?: "")
                else -> "unknown"
            }
        }

        private fun setFields(credential: Credential?) {
            val inst = sInstance ?: return
            sCredentialSet = true
            TODO("GPU: populate username combo and password filler from credential identifier/authenticator")
        }

        private fun onClickConnect(commitFields: Boolean = true) {
            val inst = sInstance ?: return
            if (inst.alertNotif) return

            TODO("APR: use JVM equivalent - validate grid choice, username, password, then invoke callback(0, callbackData)")
        }

        private fun onClickForgotPassword() {
            TODO("APR: use JVM equivalent - open forgot_password_url in external browser")
        }

        private fun onClickSignUp() {
            TODO("APR: use JVM equivalent - open sign_up_url in external browser")
        }

        private fun onUserNameTextEntry() {
            val inst = sInstance ?: return
            inst.passwordModified = true
            inst.passwordLength = 0u
            inst.addFavoritesToStartLocation()
        }

        private fun onUserListCommit() {
            val inst = sInstance ?: return
            TODO("APR: use JVM equivalent - load credential for selected username key and setFields")
        }

        private fun onRememberUserCheck() {
            val inst = sInstance ?: return
            TODO("GPU: sync remember_password checkbox enabled state with remember_name checkbox value")
        }

        private fun onRememberPasswordCheck() {
            val inst = sInstance ?: return
            TODO("APR: use JVM equivalent - mark UpdateRememberPasswordSetting and persist grid/user prefs")
        }

        private fun onPassKey(passwordText: String) {
            val inst = sInstance ?: return
            inst.passwordModified = true
            inst.passwordLength = passwordText.length.toUInt()
            inst.updateLoginButtons()
        }

        private fun md5Hex(input: String): String {
            val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        private fun cleanFullName(accountName: String): String =
            accountName.replace('.', ' ').trim().split(" ")
                .joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
    }

    fun reshape(width: Int, height: Int) {
        TODO("GPU: resize panel to ${width}x${height}")
    }
}
