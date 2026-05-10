package com.firestorm.newview

import java.util.UUID

private const val PREVIEW_LINE_HEIGHT = 19
private const val PREVIEW_BORDER_WIDTH = 2

open class LLFloaterNameDesc(args: LLSD) : LLFloater(args) {

    protected var mIsAudio: Boolean = false
    protected var mIsText: Boolean = false
    protected var mFilenameAndPath: String = ""
    protected var mFilename: String = ""
    protected var mDestinationFolderId: UUID? = null

    init {
        if (args.isString()) {
            mFilenameAndPath = args.asString()
        } else {
            mFilenameAndPath = args["filename"].asString()
            mDestinationFolderId = args["dest"].asUUID()
        }
        mFilename = gDirUtilp.getBaseFileName(mFilenameAndPath, false)
    }

    override fun postBuild(): Boolean {
        var assetName = mFilename
        assetName = LLStringUtil.replaceNonstandardASCII(assetName, '?')
        assetName = assetName.replace('|', '?')
        assetName = LLStringUtil.stripNonprintable(assetName)
        assetName = assetName.trim()
        assetName = gDirUtilp.getBaseFileName(assetName, true)

        setTitle(mFilename)
        centerWithin(gViewerWindow.getRootView().getRect())

        getChild<LLUICtrl>("name_form")?.setCommitCallback { doCommit() }
        getChild<LLUICtrl>("name_form")?.setValue(LLSD(assetName))

        val nameEditor = getChild<LLLineEditor>("name_form")
        nameEditor?.setMaxTextLength(DB_INV_ITEM_NAME_STR_LEN)
        nameEditor?.setPrevalidate(LLTextValidate::validateASCIIPrintableNoPipe)

        getChild<LLUICtrl>("description_form")?.setCommitCallback { doCommit() }
        val descEditor = getChild<LLLineEditor>("description_form")
        descEditor?.setMaxTextLength(DB_INV_ITEM_DESC_STR_LEN)
        descEditor?.setPrevalidate(LLTextValidate::validateASCIIPrintableNoPipe)

        getChild<LLUICtrl>("cancel_btn")?.setCommitCallback { onBtnCancel() }

        val expectedCost = getExpectedUploadCost()
        getChild<LLUICtrl>("ok_btn")?.setLabelArg("[AMOUNT]", expectedCost.toString())

        getChild<LLTextBox>("info_text")?.setValue(LLTrans.getString("UploadFeeInfo"))

        setDefaultBtn("ok_btn")
        return true
    }

    open fun getExpectedUploadCost(): Int {
        val extension = gDirUtilp.getExtension(mFilename)
        val assetType = LLResourceUploadInfo.findAssetTypeOfExtension(extension) ?: return -1
        return LLAgentBenefitsMgr.current().findUploadCost(assetType) ?: -1
    }

    override fun onDestroy() {
        gFocusMgr.releaseFocusIfNeeded(this)
    }

    protected open fun onCommit() {}

    fun doCommit() {
        onCommit()
    }

    fun onBtnOK() {
        getChildView("ok_btn")?.setEnabled(false)

        val expectedCost = getExpectedUploadCost()
        if (canAffordTransaction(expectedCost)) {
            val uploadInfo = LLNewFileResourceUploadInfo(
                mFilenameAndPath,
                getChild<LLUICtrl>("name_form")?.getValue()?.asString() ?: "",
                getChild<LLUICtrl>("description_form")?.getValue()?.asString() ?: "",
                permissions = 0,
                folderType = LLFolderType.FT_NONE,
                inventoryType = LLInventoryType.IT_NONE,
                nextOwnerPerms = LLFloaterPerms.getNextOwnerPerms("Uploads"),
                groupPerms = LLFloaterPerms.getGroupPerms("Uploads"),
                everyonePerms = LLFloaterPerms.getEveryonePerms("Uploads"),
                expectedCost = expectedCost,
                destinationFolderId = mDestinationFolderId
            )
            uploadNewResource(uploadInfo)
        } else {
            LLNotificationsUtil.add("ErrorCannotAffordUpload", mapOf("COST" to expectedCost.toString()))
        }

        closeFloater(false)
    }

    fun onBtnCancel() {
        closeFloater(false)
    }
}

class LLFloaterSoundPreview(filename: LLSD) : LLFloaterNameDesc(filename) {

    init {
        mIsAudio = true
    }

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        getChild<LLUICtrl>("ok_btn")?.setCommitCallback { onBtnOK() }
        return true
    }
}

class LLFloaterAnimPreview(filename: LLSD) : LLFloaterNameDesc(filename) {

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        getChild<LLUICtrl>("ok_btn")?.setCommitCallback { onBtnOK() }
        return true
    }
}

class LLFloaterScriptPreview(filename: LLSD) : LLFloaterNameDesc(filename) {

    init {
        mIsText = true
    }

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        getChild<LLUICtrl>("ok_btn")?.setCommitCallback { onBtnOK() }
        return true
    }
}
