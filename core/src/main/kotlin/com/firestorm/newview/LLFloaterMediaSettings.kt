package com.firestorm.newview

import java.util.UUID

class LLFloaterMediaSettings(key: LLSD) : LLFloater(key) {

    var mIdenticalHasMediaInfo: Boolean = true
    var mMultipleMedia: Boolean = false
    var mMultipleValidMedia: Boolean = false

    protected var mOKBtn: LLButton? = null
    protected var mCancelBtn: LLButton? = null
    protected var mApplyBtn: LLButton? = null

    protected var mTabContainer: LLTabContainer? = null
    protected var mPanelMediaSettingsGeneral: LLPanelMediaSettingsGeneral? = null
    protected var mPanelMediaSettingsSecurity: LLPanelMediaSettingsSecurity? = null
    protected var mPanelMediaSettingsPermissions: LLPanelMediaSettingsPermissions? = null

    private var mInitialValues: LLSD = LLSD()

    override fun postBuild(): Boolean {
        mApplyBtn = getChild<LLButton>("Apply")
        mApplyBtn?.setClickedCallback { onBtnApply(this) }

        mCancelBtn = getChild<LLButton>("Cancel")
        mCancelBtn?.setClickedCallback { onBtnCancel(this) }

        mOKBtn = getChild<LLButton>("OK")
        mOKBtn?.setClickedCallback { onBtnOK(this) }

        mTabContainer = getChild<LLTabContainer>("tab_container")

        mPanelMediaSettingsGeneral = LLPanelMediaSettingsGeneral()
        mTabContainer?.addTabPanel(LLTabContainer.TabPanelParams().panel(mPanelMediaSettingsGeneral!!))
        mPanelMediaSettingsGeneral?.setParent(this)

        // "permissions" tab is labelled "Controls" in UI; naming kept consistent with server-side convention
        mPanelMediaSettingsPermissions = LLPanelMediaSettingsPermissions()
        mTabContainer?.addTabPanel(LLTabContainer.TabPanelParams().panel(mPanelMediaSettingsPermissions!!))

        mPanelMediaSettingsSecurity = LLPanelMediaSettingsSecurity()
        mTabContainer?.addTabPanel(LLTabContainer.TabPanelParams().panel(mPanelMediaSettingsSecurity!!))
        mPanelMediaSettingsSecurity?.setParent(this)

        val lastTab = gSavedSettings.getS32("LastMediaSettingsTab")
        if (mTabContainer?.selectTab(lastTab) == false) {
            mTabContainer?.selectFirstTab()
        }

        sInstance = this
        return true
    }

    override fun onOpen(key: LLSD) {
        mPanelMediaSettingsGeneral?.updateMediaPreview()
    }

    override fun onClose(appQuitting: Boolean) {
        mPanelMediaSettingsGeneral?.onClose(appQuitting)
        LLFloaterReg.hideInstance("whitelist_entry")
    }

    override fun draw() {
        mApplyBtn?.setEnabled(haveValuesChanged())
        super.draw()
    }

    fun getPanelSecurity(): LLPanelMediaSettingsSecurity? = mPanelMediaSettingsSecurity

    fun getHomeUrl(): String = mPanelMediaSettingsGeneral?.getHomeUrl() ?: ""

    private fun commitFields() {
        if (hasFocus()) {
            val curFocus = gFocusMgr.getKeyboardFocus() as? LLUICtrl
            if (curFocus != null && curFocus.acceptsTextInput()) {
                curFocus.onCommit()
            }
        }
    }

    private fun haveValuesChanged(): Boolean {
        val settings = LLSD()
        sInstance?.mPanelMediaSettingsGeneral?.getValues(settings)
        sInstance?.mPanelMediaSettingsSecurity?.getValues(settings)
        sInstance?.mPanelMediaSettingsPermissions?.getValues(settings)
        for ((key, value) in settings.asMap()) {
            if (!llsdEquals(value, mInitialValues[key])) return true
        }
        return false
    }

    override fun onDestroy() {
        mPanelMediaSettingsGeneral = null
        mPanelMediaSettingsSecurity = null
        mPanelMediaSettingsPermissions = null
        sInstance = null
        super.onDestroy()
    }

    companion object {
        var sInstance: LLFloaterMediaSettings? = null
            private set

        fun getInstance(): LLFloaterMediaSettings {
            if (sInstance == null) {
                sInstance = LLFloaterReg.getTypedInstance<LLFloaterMediaSettings>("media_settings")
            }
            return sInstance!!
        }

        fun instanceExists(): Boolean =
            LLFloaterReg.findTypedInstance<LLFloaterMediaSettings>("media_settings") != null

        fun apply() {
            val inst = sInstance ?: return
            if (!inst.haveValuesChanged()) return

            val settings = LLSD()
            inst.mPanelMediaSettingsGeneral?.preApply()
            inst.mPanelMediaSettingsGeneral?.getValues(settings, false)
            inst.mPanelMediaSettingsSecurity?.preApply()
            inst.mPanelMediaSettingsSecurity?.getValues(settings, false)
            inst.mPanelMediaSettingsPermissions?.preApply()
            inst.mPanelMediaSettingsPermissions?.getValues(settings, false)

            LLSelectMgr.getInstance().selectionSetMedia(LLTextureEntry.MF_HAS_MEDIA, settings)

            inst.mPanelMediaSettingsGeneral?.postApply()
            inst.mPanelMediaSettingsSecurity?.postApply()
            inst.mPanelMediaSettingsPermissions?.postApply()
        }

        fun initValues(
            mediaSettings: LLSD,
            editable: Boolean,
            hasMediaInfo: Boolean,
            multipleMedia: Boolean,
            multipleValidMedia: Boolean
        ) {
            val inst = sInstance ?: return
            inst.mIdenticalHasMediaInfo = hasMediaInfo
            inst.mMultipleMedia = multipleMedia
            inst.mMultipleValidMedia = multipleValidMedia
            if (inst.hasFocus()) return

            inst.mPanelMediaSettingsGeneral?.clearValues(inst.mPanelMediaSettingsGeneral!!, editable, false)
            inst.mPanelMediaSettingsSecurity?.clearValues(inst.mPanelMediaSettingsSecurity!!, editable)
            inst.mPanelMediaSettingsPermissions?.clearValues(inst.mPanelMediaSettingsPermissions!!, editable)

            inst.mPanelMediaSettingsGeneral?.initValues(inst.mPanelMediaSettingsGeneral!!, mediaSettings, editable)
            inst.mPanelMediaSettingsSecurity?.initValues(inst.mPanelMediaSettingsSecurity!!, mediaSettings, editable)
            inst.mPanelMediaSettingsPermissions?.initValues(inst.mPanelMediaSettingsPermissions!!, mediaSettings, editable)

            inst.mInitialValues = LLSD()
            inst.mPanelMediaSettingsGeneral?.getValues(inst.mInitialValues)
            inst.mPanelMediaSettingsSecurity?.getValues(inst.mInitialValues)
            inst.mPanelMediaSettingsPermissions?.getValues(inst.mInitialValues)

            inst.mApplyBtn?.setEnabled(editable)
            inst.mOKBtn?.setEnabled(editable)
        }

        fun clearValues(editable: Boolean) {
            val inst = sInstance ?: return
            inst.mPanelMediaSettingsGeneral?.clearValues(inst.mPanelMediaSettingsGeneral!!, editable)
            inst.mPanelMediaSettingsSecurity?.clearValues(inst.mPanelMediaSettingsSecurity!!, editable)
            inst.mPanelMediaSettingsPermissions?.clearValues(inst.mPanelMediaSettingsPermissions!!, editable)
        }

        fun onBtnOK(self: LLFloaterMediaSettings) {
            self.commitFields()
            apply()
            self.closeFloater()
        }

        fun onBtnApply(self: LLFloaterMediaSettings) {
            self.commitFields()
            apply()
            val inst = sInstance ?: return
            inst.mInitialValues = LLSD()
            inst.mPanelMediaSettingsGeneral?.getValues(inst.mInitialValues)
            inst.mPanelMediaSettingsSecurity?.getValues(inst.mInitialValues)
            inst.mPanelMediaSettingsPermissions?.getValues(inst.mInitialValues)
        }

        fun onBtnCancel(self: LLFloaterMediaSettings) {
            self.closeFloater()
        }

        fun onTabChanged(tabContainer: LLTabContainer) {
            gSavedSettings.setS32("LastMediaSettingsTab", tabContainer.getCurrentPanelIndex())
        }
    }
}
