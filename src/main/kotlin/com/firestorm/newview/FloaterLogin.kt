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
        TODO("GPU: build panel from $layoutFile, add to login_panel_holder, reshape to ${rect.width}x${rect.height}")

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
        if (b) giveFocusInternal() else TODO("GPU: unfocus panel")
    }

    fun showLoginWidgets() {
        TODO("GPU: set login_widgets visible=true and navigate login_html to grid login page")
    }

    fun reshape(width: Int, height: Int) {
        TODO("GPU: resize panel to ${width}x${height}")
    }

    fun gridListChanged(success: Boolean) {
        updateServer()
    }

    private fun updateLoginButtons() {
        val enabled = usernameLength > 0 && passwordLength > 0
        TODO("GPU: set connect_btn enabled = $enabled")
    }

    private fun addFavoritesToStartLocation() {
        showFavorites = false
        val username = getUsername().trim()
        val canonical = canonicalizeUsername(username)
        val currentGrid = getCurrentGrid()
        val currentUser = "$canonical @ $currentGrid"
        usernameLength = currentUser.length
        updateLoginButtons()

        TODO("APR: use JVM equivalent - read stored_favorites.xml, add separator + entries matching $currentUser to start_location_combo")
    }

    private fun addUsersToCombo(showServer: Boolean) {
        TODO("APR: use JVM equivalent - populate username_combo from credential store for current grid")
    }

    private fun onSelectUser() {
        TODO("APR: use JVM equivalent - load credential for selected username and call setFields")
    }

    private fun onSelectServer() {
        TODO("APR: use JVM equivalent - update GridManager grid choice and reload login page")
    }

    private fun onLocationSLURL() {
        TODO("APR: use JVM equivalent - read start_location_combo and set StartSLURL")
    }

    private fun onUsernameTextChanged() {
        passwordModified = true
        passwordLength = 0
        addFavoritesToStartLocation()
    }

    private fun syncShowHidePasswordButton() {
        TODO("GPU: toggle password_show_btn / password_hide_btn based on showPassword=$showPassword")
    }

    private fun giveFocusInternal() {
        val username = getUsername()
        val pass     = getPasswordField()
        TODO("GPU: focus ${if (username.isNotEmpty() && pass.isEmpty()) "password_edit" else "username_combo"}")
    }

    private fun getUsername(): String = TODO("GPU: return username_combo value")
    private fun getPasswordField(): String = TODO("GPU: return password_edit value")
    private fun getCurrentGrid(): String = TODO("GPU: return server_combo simple value")

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
            TODO("GPU: ensure keyboard focus on the login panel")
        }

        fun reshapePanel() {
            sInstance?.let { it.reshape(it.rect.width, it.rect.height) }
        }

        fun setFields(credential: LoginCredential?, fromStartup: Boolean = false) {
            val inst = sInstance ?: return
            sCredentialSet = true
            TODO("GPU: populate username_combo and password_edit from credential; set sPassword")
        }

        fun getFields(credential: LoginCredential?, remember: Boolean) {
            val inst = sInstance ?: return
            TODO("APR: use JVM equivalent - build LoginCredential from UI fields; MD5-hash password to sPassword")
        }

        fun isCredentialSet(): Boolean = sCredentialSet

        fun areCredentialFieldsDirty(): Boolean {
            val inst = sInstance ?: return false
            TODO("GPU: check if username_combo or password_edit is dirty vs saved values")
        }

        fun setLocation(slurl: String) {
            TODO("APR: use JVM equivalent - parse slurl and update start_location_combo")
        }

        fun autologinToLocation(slurl: String) {
            TODO("APR: use JVM equivalent - setStartSLURL($slurl) then trigger connect")
        }

        fun updateLocationSelectorsVisibility() {
            val showServer = savedSettings("ForceShowGrid") as? Boolean ?: false
            TODO("GPU: set server_combo visibility = $showServer; show/hide related controls")
        }

        fun closePanel() {
            sInstance?.let {
                TODO("GPU: remove panel from parent view")
            }
            sInstance = null
        }

        fun loadLoginPage() {
            val inst = sInstance ?: return
            TODO("APR: use JVM equivalent - build login page URI (lang, version, channel, grid, os, sourceid) and navigate login_html")
        }

        fun giveFocus() {
            sInstance?.giveFocusInternal()
        }

        fun setAlwaysRefresh(refresh: Boolean) {
            TODO("GPU: set always-refresh on login_html browser control")
        }

        fun updateServer() {
            val inst = sInstance ?: return
            TODO("APR: use JVM equivalent - reload credential, update server combo, reload login page")
        }

        fun onUpdateStartSLURL(newStartSlurl: String) {
            val inst = sInstance ?: return
            TODO("APR: use JVM equivalent - parse slurl type and update start_location_combo and server_combo")
        }

        fun getShowFavorites(): Boolean {
            TODO("APR: use JVM equivalent - return gSavedPerAccountSettings ShowFavoritesOnLogin")
        }

        fun clearPassword() {
            sPassword = ""
        }

        fun credentialName(): String {
            TODO("APR: use JVM equivalent - return username_combo@server_combo string")
        }

        private fun onClickConnect() {
            val inst = sInstance ?: return
            TODO("APR: use JVM equivalent - validate grid/username/password, then invoke callback(0, callbackData)")
        }

        private fun onClickNewAccount() {
            TODO("APR: use JVM equivalent - open create_account_url in external browser")
        }

        private fun onClickForgotPassword() {
            TODO("APR: use JVM equivalent - open forgot_password_url in external browser")
        }

        private fun onClickRemove() {
            TODO("APR: use JVM equivalent - show remove-user confirmation dialog")
        }

        private fun onClickGridMgrHelp() {
            TODO("APR: use JVM equivalent - open grid manager help URL in external browser")
        }

        private fun onClickGridBuilder() {
            TODO("APR: use JVM equivalent - open FloaterGridBuilder")
        }

        private fun onShowHidePasswordClick() {
            val inst = sInstance ?: return
            inst.showPassword = !inst.showPassword
            inst.syncShowHidePasswordButton()
            TODO("GPU: toggle password_edit echo mode based on showPassword")
        }

        private fun onPassKey(passwordText: String) {
            val inst = sInstance ?: return
            inst.passwordModified = true
            inst.passwordLength = passwordText.length
            inst.updateLoginButtons()
        }

        private fun updateServerCombo() {
            TODO("APR: use JVM equivalent - rebuild server_combo from GridManager grid list")
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

        private fun savedSettings(key: String): Any? = TODO("APR: use JVM equivalent - gSavedSettings[$key]")
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
