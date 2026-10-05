package com.firestorm.newview

/**
 * Floater that lets the user remove saved login credentials (and optionally
 * on-disk user data) for one or more accounts.
 *
 * The original C++ source is guarded by `#if 0 … #endif` – i.e. the feature
 * is compiled out.  The Kotlin port preserves the full logic so it can be
 * re-enabled when the surrounding infrastructure is ported.
 */
class LLFloaterForgetUser(key: LLSD) : LLFloater("floater_forget_user") {

    private var mScrollList: LLScrollListCtrl? = null
    private var mLoginPanelDirty: Boolean = false

    // Maps a user-id string to the number of grids that still carry credentials
    // for that id.  Used to warn the user when deleting data shared across grids.
    private val mUserGridsCount: MutableMap<String, Int> = mutableMapOf()

    override fun onClose(appQuitting: Boolean) {
        if (mLoginPanelDirty) {
            LLPanelLogin.resetFields()
        }
        super.onClose(appQuitting)
    }

    override fun postBuild(): Boolean {
        mScrollList = getChild<LLScrollListCtrl>("user_list")

        var showGridMarks = gSavedSettings.getBool("ForceShowGrid") ?: false
        showGridMarks = showGridMarks || !LLGridManager.getInstance().isInProductionGrid()

        val knownGrids: MutableMap<String, String> = LLGridManager.getInstance().getKnownGrids()

        if (!showGridMarks) {
            for ((gridKey, _) in knownGrids) {
                if (gridKey.isNotEmpty() && gridKey != MAINGRID) {
                    if (!gSecAPIHandler.emptyCredentialMap("login_list", gridKey)) {
                        showGridMarks = true
                        break
                    }
                    // Legacy single-credential support
                    val cred = gSecAPIHandler.loadCredential(gridKey)
                    if (cred != null) {
                        val ident = cred.getIdentifier()
                        if (ident.isMap() && ident.has("type")) {
                            showGridMarks = true
                            break
                        }
                    }
                }
            }
        }

        mUserGridsCount.clear()
        if (!showGridMarks) {
            loadGridToList(MAINGRID, false)
        } else {
            for ((gridKey, _) in knownGrids) {
                if (gridKey.isNotEmpty()) {
                    loadGridToList(gridKey, true)
                }
            }
        }

        mScrollList?.selectFirstItem()
        val enableButton = (mScrollList?.getFirstSelectedIndex() ?: -1) != -1
        val chkBox = getChild<LLCheckBoxCtrl>("delete_data")
        chkBox?.setEnabled(enableButton)
        chkBox?.set(false)
        val button = getChild<LLButton>("forget")
        button?.setEnabled(enableButton)
        button?.setCommitCallback { _, _ -> onForgetClicked() }

        return true
    }

    fun onForgetClicked() {
        val scrollList = getChild<LLScrollListCtrl>("user_list") ?: return
        val userData = scrollList.getSelectedValue()
        val userId = userData["user_id"].asString()

        val deleteData = getChild<LLCheckBoxCtrl>("delete_data")?.getValue() ?: false

        // Warn if the data file is shared with more than one grid
        if (deleteData && (mUserGridsCount[userId] ?: 0) > 1) {
            LLNotificationsUtil.add("LoginRemoveMultiGridUserData", LLSD(), LLSD()) { notif, resp ->
                onConfirmForget(notif, resp)
            }
            return
        }

        processForgetUser()
    }

    private fun onConfirmForget(notification: LLSD, response: LLSD): Boolean {
        if (LLNotificationsUtil.getSelectedOption(notification, response) == 0) {
            processForgetUser()
        }
        return false
    }

    private fun processForgetUser() {
        val scrollList = getChild<LLScrollListCtrl>("user_list") ?: return
        val chkBox     = getChild<LLCheckBoxCtrl>("delete_data")
        val deleteData = chkBox?.getValue() ?: false
        val userData   = scrollList.getSelectedValue()
        val userId     = userData["user_id"].asString()
        val grid       = userData["grid"].asString()
        val userName   = userData["label"].asString()

        // Cannot delete data for the currently-logged-in user without quitting
        if (deleteData && userId == LLStartUp.getUserId() && LLStartUp.getStartupState() > STATE_LOGIN_WAIT) {
            LLNotificationsUtil.add("LoginCantRemoveCurUsername", LLSD(), LLSD()) { notif, resp ->
                onConfirmLogout(notif, resp, userName, grid)
            }
            return
        }

        forgetUser(userId, userName, grid, deleteData)
        mLoginPanelDirty = true

        if (deleteData) {
            mUserGridsCount[userId] = 0
        } else {
            mUserGridsCount[userId] = (mUserGridsCount[userId] ?: 1) - 1
        }

        scrollList.deleteSelectedItems()
        scrollList.selectFirstItem()
        if (scrollList.getFirstSelectedIndex() == -1) {
            getChild<LLButton>("forget")?.setEnabled(false)
            chkBox?.setEnabled(false)
        }
    }

    private fun loadGridToList(grid: String, showGridName: Boolean) {
        val gridLabel = if (showGridName) LLGridManager.getInstance().getGridId(grid) else ""

        if (gSecAPIHandler.hasCredentialMap("login_list", grid)) {
            val credentials: MutableMap<String, LLCredential?> = mutableMapOf()
            gSecAPIHandler.loadCredentialMap("login_list", grid, credentials)

            for ((credKey, cred) in credentials) {
                cred ?: continue
                val userLabel = LLPanelLogin.getUserName(cred)
                val userData  = LLSD()
                userData["user_id"] = credKey
                userData["label"]   = userLabel
                userData["grid"]    = grid

                val displayLabel = if (showGridName) "$userLabel ($gridLabel)" else userLabel

                val itemParams = LLScrollListItem.Params()
                itemParams.value(userData)
                itemParams.columns.add().value(displayLabel).column("user")
                    .font(LLFontGL.getFontSansSerifSmall())
                mScrollList?.addRow(itemParams, ADD_BOTTOM)

                mUserGridsCount[credKey] = (mUserGridsCount[credKey] ?: 0) + 1
            }
        } else {
            // Legacy single-credential fallback
            val cred = gSecAPIHandler.loadCredential(grid) ?: return
            val ident = cred.getIdentifier()
            if (!ident.isMap() || !ident.has("type")) return

            val userLabel = LLPanelLogin.getUserName(cred)
            val userData  = LLSD()
            userData["user_id"] = cred.userID()
            userData["label"]   = userLabel
            userData["grid"]    = grid

            val displayLabel = if (showGridName) "$userLabel ($gridLabel)" else userLabel

            val itemParams = LLScrollListItem.Params()
            itemParams.value(userData)
            itemParams.columns.add().value(displayLabel).column("user")
                .font(LLFontGL.getFontSansSerifSmall())
            mScrollList?.addRow(itemParams, ADD_BOTTOM)

            mUserGridsCount[cred.userID()] = (mUserGridsCount[cred.userID()] ?: 0) + 1
        }
    }

    companion object {
        fun onConfirmLogout(notification: LLSD, response: LLSD, favId: String, grid: String): Boolean {
            if (LLNotificationsUtil.getSelectedOption(notification, response) != 0) return false

            var gridId = LLGridManager.getInstance().getGridId(grid)
            if (gridId.isEmpty()) gridId = grid

            gSecAPIHandler.removeFromProtectedMap("mfa_hash", gridId, LLStartUp.getUserId())
            gSecAPIHandler.removeFromCredentialMap("login_list", grid, LLStartUp.getUserId())

            val cred = gSecAPIHandler.loadCredential(grid)
            if (cred != null && cred.userID() == LLStartUp.getUserId()) {
                gSecAPIHandler.deleteCredential(cred)
            }

            LLFavoritesOrderStorage.removeFavoritesRecordOfUser(favId, grid)
            LLAppViewer.instance().purgeUserDataOnExit()
            LLAppViewer.instance().requestQuit()
            return false
        }

        fun forgetUser(userid: String, favId: String, grid: String, deleteData: Boolean) {
            var gridId = LLGridManager.getInstance().getGridId(grid)
            if (gridId.isEmpty()) gridId = grid

            gSecAPIHandler.removeFromProtectedMap("mfa_hash", gridId, userid)
            gSecAPIHandler.removeFromCredentialMap("login_list", grid, userid)

            val cred = gSecAPIHandler.loadCredential(grid)
            if (cred != null && cred.userID() == userid) {
                gSecAPIHandler.deleteCredential(cred)
            }

            if (deleteData) {
                val userPath = gDirUtilp.getOSUserAppDir() + gDirUtilp.getDirDelimiter() + userid
                System.err.println("LLFloaterForgetUser: deleteUserDirectory not yet implemented")

                LLFavoritesOrderStorage.removeFavoritesRecordOfUser(favId, grid)
            }
        }
    }
}
