package com.firestorm.newview

import java.util.UUID

data class LLRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = top - bottom
}

class FSPanelLogin(
    rect: LLRect,
    private val callback: ((option: Int, userData: Any?) -> Unit)?,
    private val callbackData: Any?
) {

    private var passwordModified: Boolean = false
    private var showPassword: Boolean = false
    private var showFavorites: Boolean = false
    private var initialized: Boolean = false

    private var gridListChangedCallbackConnection: (() -> Unit)? = null

    private var usernameLength: Int = 0
    private var passwordLength: Int = 0
    private var locationLength: Int = 0

    private var previousUsername: String = ""

    init {
        sInstance = this
        buildPanel(rect)
        initialized = true
    }

    private fun buildPanel(rect: LLRect) {
        TODO("GPU: build login panel from skin XML (panel_fs_nui_login.xml or panel_fs_login.xml), " +
            "wire all callbacks for mode_combo, password_edit, server_combo, username_combo, " +
            "location_combo, and action buttons")
    }

    open fun setFocus(b: Boolean) {
        if (b) {
            giveFocus()
        } else {
            TODO("GPU: delegate setFocus(false) to parent LLPanel")
        }
    }

    fun showLoginWidgets() {
        TODO("GPU: make login_widgets visible and navigate login_html to splash screen URL")
    }

    fun handleMediaEvent(self: Any?, event: Int) {
        // Media events on the login splash page do not require action here.
    }

    fun gridListChanged(success: Boolean) {
        TODO("APR: use JVM equivalent for grid list change callback after addGrid completes")
    }

    private fun addFavoritesToStartLocation() {
        TODO("APR: use JVM equivalent for reading stored_favorites.xml and populating start_location_combo")
    }

    private fun addUsersToCombo(showServer: Boolean) {
        TODO("APR: use JVM equivalent for reading saved credentials and populating username_combo")
    }

    private fun onSelectUser() {
        TODO("GPU: load saved credentials for selected user and populate fields")
    }

    private fun onModeChange(originalValue: Any?, newValue: Any?) {
        TODO("GPU: confirm mode change via notification before applying new session settings file")
    }

    private fun onModeChangeConfirm(
        originalValue: Any?,
        newValue: Any?,
        notification: Map<String, Any>,
        response: Map<String, Any>
    ) {
        TODO("GPU: apply confirmed mode change; restart or reload session settings file")
    }

    private fun onSelectServer() {
        TODO("GPU: apply grid selection from server_combo; handle unknown grid URI via addGrid flow")
    }

    private fun onLocationSLURL() {
        TODO("APR: use JVM equivalent for LLStartUp::setStartSLURL from location_combo value")
    }

    private fun onUsernameTextChanged() {
        usernameLength = getUsernameComboText().length
        updateLoginButtons()
    }

    private fun syncShowHidePasswordButton() {
        TODO("GPU: show/hide password_show_btn and password_hide_btn based on mShowPassword; " +
            "update password_edit drawAsterisks flag")
    }

    private fun updateLoginButtons() {
        TODO("GPU: enable connect_btn only when usernameLength != 0 && passwordLength != 0")
    }

    private fun getUsernameComboText(): String {
        TODO("GPU: return current text from username_combo UI control")
    }

    companion object {
        var sInstance: FSPanelLogin? = null
            private set

        var sCapslockDidNotification: Boolean = false
        var sCredentialSet: Boolean = false
        var sPassword: String = ""
        var sPendingNewGridURI: String = ""

        fun show(
            rect: LLRect,
            callback: (option: Int, userData: Any?) -> Unit,
            callbackData: Any?
        ) {
            if (sInstance == null) {
                FSPanelLogin(rect, callback, callbackData)
            }
            TODO("GPU: ensure keyboard focus goes to the login panel")
        }

        fun reshapePanel() {
            sInstance?.let {
                TODO("GPU: reshape the panel to its current rect dimensions")
            }
        }

        fun setFields(credential: Any?, fromStartup: Boolean = false) {
            val instance = sInstance ?: return
            if (instance.initialized) sCredentialSet = true
            TODO("GPU: populate username_combo and password_edit from credential object; " +
                "handle remember-password checkbox; call addFavoritesToStartLocation")
        }

        fun getFields(credential: Any?, remember: Boolean) {
            val instance = sInstance ?: return
            TODO("APR: use JVM equivalent for credential construction: " +
                "parse username_combo for first/last or account form; " +
                "MD5-hash password if modified; set remember from checkbox")
        }

        fun isCredentialSet(): Boolean = sCredentialSet

        fun areCredentialFieldsDirty(): Boolean {
            val instance = sInstance ?: return false
            TODO("GPU: return true if username_combo or password_edit are dirty")
        }

        fun setLocation(slurl: Any?) {
            TODO("APR: use JVM equivalent for LLStartUp::setStartSLURL(slurl)")
        }

        fun autologinToLocation(slurl: Any?) {
            setLocation(slurl)
            sInstance?.let { onClickConnect(null) }
        }

        fun updateLocationSelectorsVisibility() {
            sInstance?.let {
                TODO("GPU: show/hide start_location_panel and grid_panel based on settings; " +
                    "call addUsersToCombo")
            }
        }

        fun closePanel() {
            sInstance?.let { instance ->
                TODO("GPU: remove panel from parent view hierarchy and delete sInstance")
            }
            sInstance = null
        }

        fun loadLoginPage() {
            val instance = sInstance ?: return
            TODO("APR: use JVM equivalent for building the login page LLURI with query params " +
                "(lang, version, channel, grid, os, sourceid, login_content_version, skin, " +
                "splash screen flags) and navigating login_html to the resulting URL")
        }

        fun giveFocus() {
            sInstance?.let {
                TODO("GPU: move keyboard focus to password_edit if username filled, else username_combo")
            }
        }

        fun setAlwaysRefresh(refresh: Boolean) {
            sInstance?.let {
                TODO("GPU: call setAlwaysRefresh($refresh) on login_html media control")
            }
        }

        fun updateServer() {
            sInstance?.let {
                TODO("APR: use JVM equivalent for loading credentials for new grid, " +
                    "updating server_combo label, navigating splash screen, " +
                    "updating password max-length for grid type")
            }
        }

        fun onUpdateStartSLURL(newStartSlurl: Any?) {
            val instance = sInstance ?: return
            TODO("GPU: update start_location_combo and possibly server_combo from the new SLURL type")
        }

        fun getShowFavorites(): Boolean {
            return sInstance?.showFavorites ?: false
        }

        fun clearPassword() {
            sPassword = ""
        }

        // -------------------------------------------------------------------------
        // Static action callbacks
        // -------------------------------------------------------------------------

        private fun onClickConnect(unused: Any?) {
            val instance = sInstance ?: return
            TODO("APR: use JVM equivalent for: validate non-empty username/password, " +
                "set grid choice from server_combo, build credential via getFields, " +
                "check allowed credential types, call mCallback(0, mCallbackData)")
        }

        private fun onClickNewAccount(unused: Any?) {
            TODO("APR: use JVM equivalent for LLWeb::loadURLInternal(create_account_url) " +
                "or grid-specific registration URL on OpenSim")
        }

        private fun onClickVersion(unused: Any?) {
            TODO("GPU: show the 'sl_about' floater")
        }

        private fun onClickForgotPassword(unused: Any?) {
            TODO("APR: use JVM equivalent for opening the forgot-password URL " +
                "via LLWeb::loadURLExternal or grid-specific URL on OpenSim")
        }

        private fun onClickHelp(unused: Any?) {
            TODO("GPU: show the pre-login help topic via LLViewerHelp")
        }

        private fun onPassKey(caller: Any?, userData: Any?) {
            val self = userData as? FSPanelLogin ?: return
            self.passwordModified = true
            self.passwordLength = getPasswordEditLength(self)
            self.updateLoginButtons()
            TODO("APR: check caps-lock state for notification (sCapslockDidNotification guard)")
        }

        private fun getPasswordEditLength(instance: FSPanelLogin): Int {
            TODO("GPU: return current text length of password_edit control")
        }

        private fun updateServerCombo() {
            TODO("GPU: update server_combo label and selection to reflect the current grid manager state")
        }

        private fun onClickRemove(unused: Any?) {
            TODO("APR: show confirmation notification; on confirm remove selected user credential")
        }

        private fun onRemoveCallback(notification: Map<String, Any>, response: Map<String, Any>) {
            TODO("APR: use JVM equivalent for deleting credential and refreshing username_combo")
        }

        private fun onClickGridMgrHelp(unused: Any?) {
            TODO("APR: use JVM equivalent for opening the grid manager help URL")
        }

        private fun onClickGridBuilder(unused: Any?) {
            TODO("APR: use JVM equivalent for opening the grid builder URL (OpenSim only)")
        }

        private fun onShowHidePasswordClick(unused: Any?) {
            val instance = sInstance ?: return
            instance.showPassword = !instance.showPassword
            instance.syncShowHidePasswordButton()
        }

        private fun credentialName(): String {
            TODO("APR: use JVM equivalent for LLGridManager::getInstance()->getCredentialIdentifier() " +
                "to produce the per-grid credential store key")
        }

        private fun canonicalizeUsername(name: String): String {
            var cname = name
            val arobase = cname.indexOf('@')
            if (arobase > 0) {
                cname = cname.substring(0, arobase)
            }
            val separatorIndex = cname.indexOfFirst { it == ' ' || it == '.' || it == '_' }
            val first = if (separatorIndex >= 0) cname.substring(0, separatorIndex) else cname
            val last = if (separatorIndex >= 0 && separatorIndex + 1 < cname.length) {
                cname.substring(separatorIndex + 1)
            } else {
                "Resident"
            }
            val displayLast = last.ifEmpty { "Resident" }
            return "$first $displayLast"
        }
    }
}
