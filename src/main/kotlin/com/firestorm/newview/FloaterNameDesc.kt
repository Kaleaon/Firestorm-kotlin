package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.LineEditor
import com.firestorm.llui.TextBox
import com.firestorm.llui.UICtrl
import java.util.UUID

private const val PREVIEW_LINE_HEIGHT = 19
private const val PREVIEW_BORDER_WIDTH = 2

open class FloaterNameDesc(args: Any) : Floater(args) {

    protected var isAudio: Boolean = false
    protected var isText: Boolean = false
    protected val filenameAndPath: String
    protected val filename: String
    protected var destinationFolderId: UUID = UUID(0, 0)

    init {
        val argsMap = args as? Map<*, *>
        if (argsMap != null) {
            filenameAndPath = argsMap["filename"]?.toString() ?: ""
            destinationFolderId = argsMap["dest"]?.let { UUID.fromString(it.toString()) } ?: UUID(0, 0)
        } else {
            filenameAndPath = args.toString()
        }
        filename = filenameAndPath.substringAfterLast('/').substringAfterLast('\\')
    }

    override fun postBuild(): Boolean {
        var assetName = filename
        assetName = assetName.map { if (it.code < 32 || it.code > 126) '?' else it }.joinToString("")
        assetName = assetName.replace('|', '?')
        assetName = assetName.trim()
        assetName = assetName.substringBeforeLast('.')

        setTitle(filename)

        TODO("APR: use JVM equivalent - center floater within root view")

        val nameForm = getChild<UICtrl>("name_form")
        nameForm.setCommitCallback { doCommit() }
        nameForm.setValue(assetName)

        val nameEditor = getChild<LineEditor>("name_form")
        nameEditor.setMaxTextLength(63)
        nameEditor.setPrevalidate("ASCII_PRINTABLE_NO_PIPE")

        val descEditor = getChild<LineEditor>("description_form")
        descEditor.setCommitCallback { doCommit() }
        descEditor.setMaxTextLength(127)
        descEditor.setPrevalidate("ASCII_PRINTABLE_NO_PIPE")

        getChild<UICtrl>("cancel_btn").setCommitCallback { onBtnCancel() }

        val expectedUploadCost = getExpectedUploadCost()
        getChild<UICtrl>("ok_btn").setLabelArg("[AMOUNT]", expectedUploadCost.toString())

        val infoText = findChild<TextBox>("info_text")
        infoText?.setValue(TODO("APR: use JVM equivalent - translate UploadFeeInfo string"))

        setDefaultBtn("ok_btn")
        return true
    }

    open fun getExpectedUploadCost(): Int {
        TODO("APR: use JVM equivalent - look up upload cost for file extension via agent benefits")
    }

    override fun onDestroy() {
        TODO("APR: use JVM equivalent - release focus before destroying")
    }

    protected open fun onCommit() {
        // Subclasses override to react to name/description edits.
    }

    fun doCommit() {
        onCommit()
    }

    fun onBtnOK() {
        getChildView("ok_btn").setEnabled(false)

        val expectedUploadCost = getExpectedUploadCost()
        if (canAffordTransaction(expectedUploadCost)) {
            val name        = getChild<UICtrl>("name_form").getValue().toString()
            val description = getChild<UICtrl>("description_form").getValue().toString()
            TODO("APR: use JVM equivalent - upload_new_resource with name, description, upload cost, and destination folder")
        } else {
            val args = mapOf("COST" to expectedUploadCost.toString())
            TODO("APR: use JVM equivalent - show ErrorCannotAffordUpload notification with COST arg")
        }
        closeFloater(false)
    }

    fun onBtnCancel() {
        closeFloater(false)
    }

    private fun canAffordTransaction(cost: Int): Boolean {
        TODO("APR: use JVM equivalent - check agent balance >= cost")
    }
}

class FloaterSoundPreview(args: Any) : FloaterNameDesc(args) {
    init {
        isAudio = true
    }

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        getChild<UICtrl>("ok_btn").setCommitCallback { onBtnOK() }
        return true
    }
}

class FloaterAnimPreview(args: Any) : FloaterNameDesc(args) {
    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        getChild<UICtrl>("ok_btn").setCommitCallback { onBtnOK() }
        return true
    }
}

class FloaterScriptPreview(args: Any) : FloaterNameDesc(args) {
    init {
        isText = true
    }

    override fun postBuild(): Boolean {
        if (!super.postBuild()) return false
        getChild<UICtrl>("ok_btn").setCommitCallback { onBtnOK() }
        return true
    }
}
