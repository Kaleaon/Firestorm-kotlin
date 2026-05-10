package com.firestorm.newview

import java.util.UUID
import java.io.File

private const val PREVIEW_LINE_HEIGHT = 19
private const val PREVIEW_BORDER_WIDTH = 2
private const val PREVIEW_RESIZE_HANDLE_SIZE = (RESIZE_HANDLE_WIDTH * OO_SQRT2).toInt() + PREVIEW_BORDER_WIDTH
private const val PREVIEW_HPAD = PREVIEW_RESIZE_HANDLE_SIZE

open class FloaterNameDesc(args: LLSD) : Floater(args) {

    protected var isAudio: Boolean = false
    protected var isText: Boolean = false

    protected val filenameAndPath: String
    protected val filename: String
    protected var destinationFolderId: UUID = UUID(0, 0)

    init {
        if (args.isString()) {
            filenameAndPath = args.asString()
        } else {
            filenameAndPath = args["filename"].asString()
            destinationFolderId = args["dest"].asUUID()
        }
        filename = DirUtils.getBaseFileName(filenameAndPath, stripExtension = false)
    }

    override fun postBuild(): Boolean {
        var assetName = filename
        assetName = StringUtils.replaceNonstandardAscii(assetName, '?')
        assetName = assetName.replace('|', '?')
        assetName = StringUtils.stripNonprintable(assetName)
        assetName = assetName.trim()
        assetName = DirUtils.getBaseFileName(assetName, stripExtension = true)

        setTitle(filename)
        centerWithin(ViewerWindow.instance.rootView.rect)

        getChild<UiCtrl>("name_form")?.apply {
            setCommitCallback { doCommit() }
            setValue(LLSD(assetName))
        }
        getChild<LineEditor>("name_form")?.apply {
            maxTextLength = DB_INV_ITEM_NAME_STR_LEN
            setPrevalidate(TextValidate::validateAsciiPrintableNoPipe)
        }

        getChild<UiCtrl>("description_form")?.apply {
            setCommitCallback { doCommit() }
        }
        getChild<LineEditor>("description_form")?.apply {
            maxTextLength = DB_INV_ITEM_DESC_STR_LEN
            setPrevalidate(TextValidate::validateAsciiPrintableNoPipe)
        }

        getChild<UiCtrl>("cancel_btn")?.setCommitCallback { onBtnCancel() }

        val expectedUploadCost = getExpectedUploadCost()
        getChild<UiCtrl>("ok_btn")?.setLabelArg("[AMOUNT]", "$expectedUploadCost")

        getChild<TextBox>("info_text")?.setValue(Trans.getString("UploadFeeInfo"))

        setDefaultBtn("ok_btn")
        return true
    }

    open fun getExpectedUploadCost(): Int {
        val extension = DirUtils.getExtension(filename)
        val assetType = ResourceUploadInfo.findAssetTypeOfExtension(extension) ?: run {
            logWarn("Unable to find upload cost for $filename")
            return -1
        }
        val cost = AgentBenefitsMgr.current().findUploadCost(assetType)
        if (cost == null) {
            logWarn("Unable to find upload cost for asset type $assetType")
            return -1
        }
        return cost
    }

    fun onBtnOK() {
        getChildView("ok_btn")?.isEnabled = false

        val expectedUploadCost = getExpectedUploadCost()
        if (canAffordTransaction(expectedUploadCost)) {
            val name = getChild<UiCtrl>("name_form")?.getValue()?.asString() ?: ""
            val description = getChild<UiCtrl>("description_form")?.getValue()?.asString() ?: ""
            val uploadInfo = NewFileResourceUploadInfo(
                filenameAndPath = filenameAndPath,
                name = name,
                description = description,
                nextOwnerPerms = FloaterPerms.getNextOwnerPerms("Uploads"),
                groupPerms = FloaterPerms.getGroupPerms("Uploads"),
                everyonePerms = FloaterPerms.getEveryonePerms("Uploads"),
                expectedUploadCost = expectedUploadCost,
                destinationFolderId = destinationFolderId
            )
            uploadNewResource(uploadInfo)
        } else {
            val args = LLSD().apply { put("COST", "$expectedUploadCost") }
            NotificationsUtil.add("ErrorCannotAffordUpload", args)
        }
        closeFloater(quitting = false)
    }

    fun onBtnCancel() {
        closeFloater(quitting = false)
    }

    fun doCommit() {
        onCommit()
    }

    protected open fun onCommit() {}

    override fun onDestroy() {
        FocusMgr.instance.releaseFocusIfNeeded(this)
    }
}

class FloaterSoundPreview(args: LLSD) : FloaterNameDesc(args) {

    init {
        isAudio = true
    }

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        getChild<UiCtrl>("ok_btn")?.setCommitCallback { onBtnOK() }
        return true
    }
}

class FloaterAnimPreview(args: LLSD) : FloaterNameDesc(args) {

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        getChild<UiCtrl>("ok_btn")?.setCommitCallback { onBtnOK() }
        return true
    }
}

class FloaterScriptPreview(args: LLSD) : FloaterNameDesc(args) {

    init {
        isText = true
    }

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        getChild<UiCtrl>("ok_btn")?.setCommitCallback { onBtnOK() }
        return true
    }
}
