package com.firestorm.newview

import java.security.MessageDigest

data class PanelRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int  get() = right - left
    val height: Int get() = bottom - top
}

class FloaterLogin(
    private val rect: PanelRect,
    private val callback: (option: Int, userData: Any?) -> Unit,
    private val callbackData: Any?
) {
    private var passwordModified: Boolean = false
    private var showPassword: Boolean = false
    private var showFavorites: Boolean = false
    private var initialized: Boolean = false
    private var previousUsername: String = ""

    private var usernameLength: Int = 0
    private var passwordLength: Int = 0
    private var locationLength: Int = 0

    init {
        sInstance = this

        val useLegacy = savedSettings("FSUseLegacyLoginPanel") as? Boolean ?: false
        val layoutFile = if (useLegacy) "panel_fs_login.xml" else "panel_fs_nui_login.xml"
        System.err.println("FloaterLogin: build panel from $layoutFile not yet implemented")

        val startSlurl = Startup.getStartSLURL()
        if (startSlurl.isEmpty()) {
            val defaultStart = savedSettings("LoginLocation") as? String ?: ""
            if (defaultStart.isNotEmpty()) {
                Startup.setStartSLURL(defaultStart)
            } else {
                Startup.setStartSLURL("home")
            }
        }

        loadLoginPage()
        initialized = true
    }

    fun setFocus(b: Boolean) {
        if (b) giveFocusInternal() else System.err.println("FloaterLogin: unfocus panel not yet implemented")
    }

    fun showLoginWidgets() {
        System.err.println("FloaterLogin: showLoginWidgets not yet implemented")
    }

    fun reshape(width: Int, height: Int) {
        System.err.println("FloaterLogin: reshape not yet implemented")
    }

    fun gridListChanged(success: Boolean) {
        updateServer()
    }

    private fun updateLoginButtons() {
        val enabled = usernameLength > 0 && passwordLength > 0
        System.err.println("FloaterLogin: updateLoginButtons not yet implemented")
    }

    private fun addFavoritesToStartLocation() {
        showFavorites = false
        val username = getUsername().trim()
        val canonical = canonicalizeUsername(username)
        val currentGrid = getCurrentGrid()
        val currentUser = "$canonical @ $currentGrid"
        usernameLength = currentUser.length
        updateLoginButtons()

        System.err.println("FloaterLogin: addFavoritesToStartLocation not yet implemented")
    }

    private fun addUsersToCombo(showServer: Boolean) {
        System.err.println("FloaterLogin: addUsersToCombo not yet implemented")
    }

    private fun onSelectUser() {
        System.err.println("FloaterLogin: onSelectUser not yet implemented")
    }

    private fun onSelectServer() {
        System.err.println("FloaterLogin: onSelectServer not yet implemented")
    }

    private fun onLocationSLURL() {
        System.err.println("FloaterLogin: onLocationSLURL not yet implemented")
    }

    private fun onUsernameTextChanged() {
        passwordModified = true
        passwordLength = 0
        addFavoritesToStartLocation()
    }

    private fun syncShowHidePasswordButton() {
        System.err.println("FloaterLogin: syncShowHidePasswordButton not yet implemented")
    }

    private fun giveFocusInternal() {
        val username = getUsername()
        val pass     = getPasswordField()
        System.err.println("FloaterLogin: giveFocusInternal not yet implemented")
    }

    private fun getUsername(): String {
        System.err.println("FloaterLogin: getUsername not yet implemented")
        return ""
    }
    private fun getPasswordField(): String {
        System.err.println("FloaterLogin: getPasswordField not yet implemented")
        return ""
    }
    private fun getCurrentGrid(): String {
        System.err.println("FloaterLogin: getCurrentGrid not yet implemented")
        return ""
    }

    companion object {
        var sInstance: FloaterLogin? = null
            private set

        var sCapslockDidNotification: Boolean = false
        var sCredentialSet: Boolean = false

        private var sPassword: String = ""
        private var sPendingNewGridUri: String = ""

        const val MAX_PASSWORD_SL: Int = 16
        const val MAX_PASSWORD_OPENSIM: Int = 255

        fun show(rect: PanelRect, callback: (Int, Any?) -> Unit, callbackData: Any?) {
            if (sInstance == null) {
                FloaterLogin(rect, callback, callbackData)
            }
            System.err.println("FloaterLogin: show not yet implemented")
        }

        fun reshapePanel() {
            sInstance?.let { it.reshape(it.rect.width, it.rect.height) }
        }

        fun setFields(credential: LoginCredential?, fromStartup: Boolean = false) {
            val inst = sInstance ?: return
            sCredentialSet = true
            System.err.println("FloaterLogin: setFields not yet implemented")
        }

        fun getFields(credential: LoginCredential?, remember: Boolean) {
            val inst = sInstance ?: return
            System.err.println("FloaterLogin: getFields not yet implemented")
        }

        fun isCredentialSet(): Boolean = sCredentialSet

        fun areCredentialFieldsDirty(): Boolean {
            val inst = sInstance ?: return false
            System.err.println("FloaterLogin: areCredentialFieldsDirty not yet implemented")
            return false
        }

        fun setLocation(slurl: String) {
            System.err.println("FloaterLogin: setLocation not yet implemented")
        }

        fun autologinToLocation(slurl: String) {
            System.err.println("FloaterLogin: autologinToLocation not yet implemented")
        }

        fun updateLocationSelectorsVisibility() {
            val showServer = savedSettings("ForceShowGrid") as? Boolean ?: false
            System.err.println("FloaterLogin: updateLocationSelectorsVisibility not yet implemented")
        }

        fun closePanel() {
            sInstance?.let {
                System.err.println("FloaterLogin: closePanel not yet implemented")
            }
            sInstance = null
        }

        fun loadLoginPage() {
            val inst = sInstance ?: return
            System.err.println("FloaterLogin: loadLoginPage not yet implemented")
        }

        fun giveFocus() {
            sInstance?.giveFocusInternal()
        }

        fun setAlwaysRefresh(refresh: Boolean) {
            System.err.println("FloaterLogin: setAlwaysRefresh not yet implemented")
        }

        fun updateServer() {
            val inst = sInstance ?: return
            System.err.println("FloaterLogin: updateServer not yet implemented")
        }

        fun onUpdateStartSLURL(newStartSlurl: String) {
            val inst = sInstance ?: return
            System.err.println("FloaterLogin: onUpdateStartSLURL not yet implemented")
        }

        fun getShowFavorites(): Boolean {
            System.err.println("FloaterLogin: getShowFavorites not yet implemented")
            return false
        }

        fun clearPassword() {
            sPassword = ""
        }

        fun credentialName(): String {
            System.err.println("FloaterLogin: credentialName not yet implemented")
            return ""
        }

        private fun onClickConnect() {
            val inst = sInstance ?: return
            System.err.println("FloaterLogin: onClickConnect not yet implemented")
        }

        private fun onClickNewAccount() {
            System.err.println("FloaterLogin: onClickNewAccount not yet implemented")
        }

        private fun onClickForgotPassword() {
            System.err.println("FloaterLogin: onClickForgotPassword not yet implemented")
        }

        private fun onClickRemove() {
            System.err.println("FloaterLogin: onClickRemove not yet implemented")
        }

        private fun onClickGridMgrHelp() {
            System.err.println("FloaterLogin: onClickGridMgrHelp not yet implemented")
        }

        private fun onClickGridBuilder() {
            System.err.println("FloaterLogin: onClickGridBuilder not yet implemented")
        }

        private fun onShowHidePasswordClick() {
            val inst = sInstance ?: return
            inst.showPassword = !inst.showPassword
            inst.syncShowHidePasswordButton()
            System.err.println("FloaterLogin: onShowHidePasswordClick not yet implemented")
        }

        private fun onPassKey(passwordText: String) {
            val inst = sInstance ?: return
            inst.passwordModified = true
            inst.passwordLength = passwordText.length
            inst.updateLoginButtons()
        }

        private fun updateServerCombo() {
            System.err.println("FloaterLogin: updateServerCombo not yet implemented")
        }

        fun canonicalizeUsername(name: String): String {
            val trimmed = name.trim()
            val parts   = trimmed.split(Regex("[. ]+"), limit = 2)
            val first   = parts[0].replaceFirstChar { it.uppercase() }
            val last    = if (parts.size > 1) parts[1].replaceFirstChar { it.uppercase() } else "Resident"
            return "$first $last"
        }

        private fun md5Hex(input: String): String {
            val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        private fun savedSettings(key: String): Any? {
            System.err.println("FloaterLogin: savedSettings not yet implemented")
            return null
        }
    }
}

object LocationLoginAutoHandler {
    fun handle(tokens: List<String>, queryMap: Map<String, String>): Boolean {
        if (Startup.startupState >= StartupState.STATE_LOGIN_CLEANUP) return true
        if (tokens.isEmpty() || tokens.size > 4) return false

        val region = tokens[0]

        val slurl = when (tokens.size) {
            1, 2 -> region
            3 -> {
                val x = tokens[1].toFloatOrNull() ?: 0f
                val y = tokens[2].toFloatOrNull() ?: 0f
                "$region/$x/$y/0"
            }
            4 -> {
                val x = tokens[1].toFloatOrNull() ?: 0f
                val y = tokens[2].toFloatOrNull() ?: 0f
                val z = tokens[3].toFloatOrNull() ?: 0f
                "$region/$x/$y/$z"
            }
            else -> return false
        }

        FloaterLogin.autologinToLocation(slurl)
        return true
    }
}
