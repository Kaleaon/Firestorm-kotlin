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

        System.err.println("PanelLogin: addFavoritesToStartLocation not yet implemented")
    }

    fun setFocus(b: Boolean) {
        if (b) giveFocus() else System.err.println("PanelLogin: unfocus panel not yet implemented")
    }

    private fun updateLoginButtons() {
        val enabled = usernameLength != 0u && passwordLength != 0u && !alertNotif
        System.err.println("PanelLogin: set login button enabled = $enabled not yet implemented")
    }

    private fun populateUserList(credential: Credential?) {
        System.err.println("PanelLogin: populate username combo from credential store not yet implemented")
    }

    private fun onSelectServer() {
        System.err.println("PanelLogin: apply selected grid and reload favorites/login page not yet implemented")
    }

    private fun onLocationSLURL() {
        System.err.println("PanelLogin: read location combo and call setStartSLURL not yet implemented")
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
        System.err.println("PanelLogin: read username combo value not yet implemented")
        return ""
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
            System.err.println("PanelLogin: ensure keyboard focus on the login panel not yet implemented")
        }

        fun reshapePanel() {
            sInstance?.let {
                it.reshape(it.rect.width, it.rect.height)
            }
        }

        fun populateFields(credential: Credential?, rememberUser: Boolean, rememberPassword: Boolean) {
            val inst = sInstance ?: return
            System.err.println("PanelLogin: set remember_name checkbox = $rememberUser, populate user list not yet implemented")
        }

        fun resetFields() {
            val inst = sInstance ?: return
            System.err.println("PanelLogin: reload credential from SecAPI and repopulate list not yet implemented")
        }

        fun getFields(credential: Credential?, rememberUser: Boolean, rememberPassword: Boolean) {
            val inst = sInstance ?: return
            System.err.println("PanelLogin: build Credential from UI field values including MD5 hash not yet implemented")
        }

        fun isCredentialSet(): Boolean = sCredentialSet

        fun areCredentialFieldsDirty(): Boolean {
            val inst = sInstance ?: return false
            System.err.println("PanelLogin: check if username combo or password field is dirty not yet implemented")
            return false
        }

        fun setLocation(slurl: String) {
            System.err.println("PanelLogin: call setStartSLURL not yet implemented")
        }

        fun autologinToLocation(slurl: String) {
            System.err.println("PanelLogin: setStartSLURL then trigger connect not yet implemented")
        }

        fun updateLocationSelectorsVisibility() {
            val inst = sInstance ?: return
            val showServer = savedSettings.getBool("ForceShowGrid")
            System.err.println("PanelLogin: set server_combo visibility = $showServer not yet implemented")
        }

        fun closePanel() {
            sInstance?.let {
                System.err.println("PanelLogin: remove panel from parent and delete not yet implemented")
            }
            sInstance = null
        }

        fun loadLoginPage() {
            val inst = sInstance ?: return
            System.err.println("PanelLogin: build login URI with params (lang, version, channel, grid, os, sourceid) and navigate browser not yet implemented")
        }

        fun giveFocus() {
            val inst = sInstance ?: return
            System.err.println("PanelLogin: focus username combo or password field depending on which is empty not yet implemented")
        }

        fun setAlwaysRefresh(refresh: Boolean) {
            System.err.println("PanelLogin: set always-refresh on login_html web browser control not yet implemented")
        }

        fun updateServer() {
            val inst = sInstance ?: return
            System.err.println("PanelLogin: reload credential, update links visibility, reload login page not yet implemented")
        }

        fun onUpdateStartSLURL(newStartSlurl: String) {
            val inst = sInstance ?: return
            System.err.println("PanelLogin: parse slurl type and update location combo and grid selector not yet implemented")
        }

        fun getShowFavorites(): Boolean {
            System.err.println("PanelLogin: return gSavedPerAccountSettings ShowFavoritesOnLogin not yet implemented")
            return false
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
            System.err.println("PanelLogin: populate username combo and password filler from credential identifier/authenticator not yet implemented")
        }

        private fun onClickConnect(commitFields: Boolean = true) {
            val inst = sInstance ?: return
            if (inst.alertNotif) return

            System.err.println("PanelLogin: validate grid choice, username, password, then invoke callback(0, callbackData) not yet implemented")
        }

        private fun onClickForgotPassword() {
            System.err.println("PanelLogin: open forgot_password_url in external browser not yet implemented")
        }

        private fun onClickSignUp() {
            System.err.println("PanelLogin: open sign_up_url in external browser not yet implemented")
        }

        private fun onUserNameTextEntry() {
            val inst = sInstance ?: return
            inst.passwordModified = true
            inst.passwordLength = 0u
            inst.addFavoritesToStartLocation()
        }

        private fun onUserListCommit() {
            val inst = sInstance ?: return
            System.err.println("PanelLogin: load credential for selected username key and setFields not yet implemented")
        }

        private fun onRememberUserCheck() {
            val inst = sInstance ?: return
            System.err.println("PanelLogin: sync remember_password checkbox enabled state with remember_name checkbox value not yet implemented")
        }

        private fun onRememberPasswordCheck() {
            val inst = sInstance ?: return
            System.err.println("PanelLogin: mark UpdateRememberPasswordSetting and persist grid/user prefs not yet implemented")
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
        System.err.println("PanelLogin: resize panel to ${width}x${height} not yet implemented")
    }
}
