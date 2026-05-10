package com.firestorm.newview

class FSFloaterContactSetConfiguration(targetSet: LLSD) : LLFloater(targetSet) {

    private var contactSet: String = targetSet.asString()

    private var notificationCheckBox: LLCheckBoxCtrl? = null
    private var sortByOnlineStatusCheckBox: LLCheckBoxCtrl? = null
    private var autoresponseBusyEnabledCheckBox: LLCheckBoxCtrl? = null
    private var autoresponseBusyEditor: LLTextEditor? = null
    private var autoresponseModeEnabledCheckBox: LLCheckBoxCtrl? = null
    private var autoresponseModeEditor: LLTextEditor? = null
    private var autoresponseNonFriendsEnabledCheckBox: LLCheckBoxCtrl? = null
    private var autoresponseNonFriendsEditor: LLTextEditor? = null
    private var setSwatch: LLColorSwatchCtrl? = null
    private var globalSwatch: LLColorSwatchCtrl? = null
    private var setName: LLLineEditor? = null
    private var renameButton: LLButton? = null

    private var frustumOrigin: LLHandle<LLView>? = null

    private var contextConeOpacity: Float = 0f
    private val contextConeInAlpha: Float = CONTEXT_CONE_IN_ALPHA
    private val contextConeOutAlpha: Float = CONTEXT_CONE_OUT_ALPHA
    private val contextConeFadeTime: Float = CONTEXT_CONE_FADE_TIME

    override fun postBuild(): Boolean {
        updateTitle()

        setName = getChild<LLLineEditor>("set_name_editor").also { it.setText(contactSet) }
        renameButton = getChild<LLButton>("rename_btn").also {
            it.setCommitCallback { onRenameSet() }
        }

        setSwatch = getChild<LLColorSwatchCtrl>("set_swatch").also {
            it.setCommitCallback { onCommitSetColor() }
        }
        globalSwatch = getChild<LLColorSwatchCtrl>("global_swatch").also {
            it.setCommitCallback { onCommitDefaultColor() }
        }

        notificationCheckBox = getChild<LLCheckBoxCtrl>("show_set_notifications").also {
            it.setCommitCallback { onCommitSetNotifications() }
        }
        sortByOnlineStatusCheckBox = getChild<LLCheckBoxCtrl>("sort_by_online_status").also {
            it.setCommitCallback { onCommitSetSortByOnlineStatus() }
        }

        autoresponseBusyEnabledCheckBox = getChild<LLCheckBoxCtrl>("set_autoresponse_busy_enabled").also {
            it.setCommitCallback { onCommitSetAutoresponseBusy() }
        }
        autoresponseBusyEditor = getChild<LLTextEditor>("set_autoresponse_busy").also {
            it.setCommitCallback { onCommitSetAutoresponseBusy() }
        }

        autoresponseModeEnabledCheckBox = getChild<LLCheckBoxCtrl>("set_autoresponse_mode_enabled").also {
            it.setCommitCallback { onCommitSetAutoresponseMode() }
        }
        autoresponseModeEditor = getChild<LLTextEditor>("set_autoresponse_mode").also {
            it.setCommitCallback { onCommitSetAutoresponseMode() }
        }

        autoresponseNonFriendsEnabledCheckBox = getChild<LLCheckBoxCtrl>("set_autoresponse_nonfriends_enabled").also {
            it.setCommitCallback { onCommitSetAutoresponseNonFriends() }
        }
        autoresponseNonFriendsEditor = getChild<LLTextEditor>("set_autoresponse_nonfriends").also {
            it.setCommitCallback { onCommitSetAutoresponseNonFriends() }
        }

        return true
    }

    override fun draw() {
        val maxOpacity = gSavedSettings.getFloat("PickerContextOpacity", 0.4f)
        drawConeToOwner(contextConeOpacity, maxOpacity, frustumOrigin?.get(), contextConeFadeTime, contextConeInAlpha, contextConeOutAlpha)
        super.draw()
    }

    override fun onOpen(targetSet: LLSD) {
        val cs = LGGContactSets.getInstance()
        setSwatch?.set(cs.getSetColor(contactSet), true)
        globalSwatch?.set(cs.getDefaultColor(), true)
        notificationCheckBox?.set(cs.getNotifyForSet(contactSet))
        sortByOnlineStatusCheckBox?.set(cs.getSortByOnlineStatusForSet(contactSet))

        var enabled: Boolean
        var response: String

        cs.getAutoresponseForSet(contactSet, ContactSetAutoresponseMode.BUSY).also { (e, r) ->
            enabled = e; response = r
        }
        autoresponseBusyEnabledCheckBox?.set(enabled)
        autoresponseBusyEditor?.setText(response)

        cs.getAutoresponseForSet(contactSet, ContactSetAutoresponseMode.AUTORESPONSE).also { (e, r) ->
            enabled = e; response = r
        }
        autoresponseModeEnabledCheckBox?.set(enabled)
        autoresponseModeEditor?.setText(response)

        cs.getAutoresponseForSet(contactSet, ContactSetAutoresponseMode.AUTORESPONSE_NONFRIENDS).also { (e, r) ->
            enabled = e; response = r
        }
        autoresponseNonFriendsEnabledCheckBox?.set(enabled)
        autoresponseNonFriendsEditor?.setText(response)

        updateAutoresponseEditorEnabledState()
    }

    private fun onCommitSetColor() {
        LGGContactSets.getInstance().setSetColor(contactSet, setSwatch!!.get())
    }

    private fun onCommitSetNotifications() {
        LGGContactSets.getInstance().setNotifyForSet(contactSet, notificationCheckBox!!.getValue().asBoolean())
    }

    private fun onCommitSetSortByOnlineStatus() {
        LGGContactSets.getInstance().setSortByOnlineStatusForSet(contactSet, sortByOnlineStatusCheckBox!!.getValue().asBoolean())
    }

    private fun onCommitDefaultColor() {
        LGGContactSets.getInstance().setDefaultColor(globalSwatch!!.get())
    }

    private fun onCommitSetAutoresponseBusy() {
        LGGContactSets.getInstance().setAutoresponseForSet(
            contactSet,
            ContactSetAutoresponseMode.BUSY,
            autoresponseBusyEnabledCheckBox!!.getValue().asBoolean(),
            autoresponseBusyEditor!!.getText()
        )
        updateAutoresponseEditorEnabledState()
    }

    private fun onCommitSetAutoresponseMode() {
        LGGContactSets.getInstance().setAutoresponseForSet(
            contactSet,
            ContactSetAutoresponseMode.AUTORESPONSE,
            autoresponseModeEnabledCheckBox!!.getValue().asBoolean(),
            autoresponseModeEditor!!.getText()
        )
        updateAutoresponseEditorEnabledState()
    }

    private fun onCommitSetAutoresponseNonFriends() {
        LGGContactSets.getInstance().setAutoresponseForSet(
            contactSet,
            ContactSetAutoresponseMode.AUTORESPONSE_NONFRIENDS,
            autoresponseNonFriendsEnabledCheckBox!!.getValue().asBoolean(),
            autoresponseNonFriendsEditor!!.getText()
        )
        updateAutoresponseEditorEnabledState()
    }

    private fun onRenameSet() {
        val newName = setName!!.getText()
        if (newName.isNotEmpty() && LGGContactSets.getInstance().renameSet(contactSet, newName)) {
            key = LLSD(newName)
            contactSet = newName
            updateTitle()
        } else {
            LLNotificationsUtil.add(
                "RenameContactSetFailure",
                mapOf("SET" to contactSet, "NEW_NAME" to newName)
            )
        }
    }

    private fun updateTitle() {
        setTitle(getString("title").replace("{NAME}", contactSet))
    }

    fun setFrustumOrigin(origin: LLView?) {
        if (origin != null) {
            frustumOrigin = origin.getHandle()
        }
    }

    private fun updateAutoresponseEditorEnabledState() {
        autoresponseBusyEditor?.setEnabled(autoresponseBusyEnabledCheckBox!!.getValue().asBoolean())
        autoresponseModeEditor?.setEnabled(autoresponseModeEnabledCheckBox!!.getValue().asBoolean())
        autoresponseNonFriendsEditor?.setEnabled(autoresponseNonFriendsEnabledCheckBox!!.getValue().asBoolean())
    }
}
