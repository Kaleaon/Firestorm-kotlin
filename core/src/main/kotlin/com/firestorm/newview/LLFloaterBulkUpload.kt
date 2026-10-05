package com.firestorm.newview

import java.util.UUID

class LLFloaterBulkUpload(key: LLSD) : LLModalDialog(key, true) {

    private var mCheckboxUpload2K: LLUICtrl? = null
    private var mCountLabel: LLTextBox? = null
    private var mCostLabel: LLTextBox? = null
    private var mCheckboxPanel: LLPanel? = null
    private var mLinkPanel: LLPanel? = null
    private var mWarningPanel: LLPanel? = null

    private val mFiles: MutableList<String> = mutableListOf()
    private var mAllow2kTextures: Boolean = true
    private var mHas2kTextures: Boolean = false
    private var mDestinationFolderId: UUID = UUID.randomUUID()
    private var mUploadCost: Int = 0
    private var mUploadCount: Int = 0

    init {
        mUploadCost = key["upload_cost"].asInteger()
        mUploadCount = key["upload_count"].asInteger()
        mHas2kTextures = key["has_2k_textures"].asBoolean()
        mDestinationFolderId = key["dest"].asUUID()
        if (key["files"].isArray()) {
            val files = key["files"]
            for (entry in files.asArray()) {
                mFiles.add(entry.asString())
            }
        }
    }

    override fun postBuild(): Boolean {
        childSetAction("upload_btn") { onClickUpload() }
        childSetAction("cancel_btn") { onClickCancel() }

        mCountLabel = getChild<LLTextBox>("number_of_items", true)
        mCostLabel = getChild<LLTextBox>("upload_cost", true)

        mCheckboxPanel = getChild<LLPanel>("checkbox_panel", true)
        mLinkPanel = getChild<LLPanel>("link_panel", true)
        mWarningPanel = getChild<LLPanel>("warning_panel", true)

        mCheckboxUpload2K = getChild<LLUICtrl>("upload_2k")
        mCheckboxUpload2K!!.setCommitCallback { onUpload2KCheckBox() }

        mAllow2kTextures = gSavedSettings.getBool("BulkUpload2KTextures")
        mCheckboxUpload2K!!.setValue(!mAllow2kTextures)

        if (!mAllow2kTextures && mHas2kTextures) {
            // Provided cost was for 2K textures; recalculate with current allow flag.
            getBulkUploadExpectedCost(mFiles, mAllow2kTextures)
        }

        update()

        return super.postBuild()
    }

    fun update() {
        mCountLabel?.setTextArg("[COUNT]", mUploadCount.toString())
        mCostLabel?.setTextArg("[COST]", mUploadCost.toString())

        mCheckboxPanel?.setVisible(mHas2kTextures)
        mLinkPanel?.setVisible(mHas2kTextures)
        mWarningPanel?.setVisible(mHas2kTextures)

        var newHeight = MAX_HEIGHT
        if (!mHas2kTextures) {
            newHeight -= mCheckboxPanel?.getRect()?.getHeight() ?: 0
            newHeight -= mLinkPanel?.getRect()?.getHeight() ?: 0
            newHeight -= mWarningPanel?.getRect()?.getHeight() ?: 0
        }
        reshape(getRect().getWidth(), newHeight, false)
    }

    protected fun onUpload2KCheckBox() {
        mAllow2kTextures = !(mCheckboxUpload2K!!.getValue().asBoolean())
        gSavedSettings.setBool("BulkUpload2KTextures", mAllow2kTextures)

        // keep old value of mHas2kTextures so the checkbox remains visible
        getBulkUploadExpectedCost(mFiles, mAllow2kTextures)

        update()
    }

    protected fun onClickUpload() {
        doBulkUpload(mFiles, mAllow2kTextures, mDestinationFolderId)
        closeFloater()
    }

    protected fun onClickCancel() {
        closeFloater()
    }

    private fun getBulkUploadExpectedCost(files: MutableList<String>, allow2k: Boolean) {
        System.err.println("LLFloaterBulkUpload: getBulkUploadExpectedCost not yet implemented")
    }

    private fun doBulkUpload(files: MutableList<String>, allow2k: Boolean, destFolderId: UUID) {
        System.err.println("LLFloaterBulkUpload: doBulkUpload not yet implemented")
    }

    companion object {
        private const val MAX_HEIGHT = 211
    }
}
