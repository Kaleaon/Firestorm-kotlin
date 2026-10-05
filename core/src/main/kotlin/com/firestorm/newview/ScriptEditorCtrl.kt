package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llui.Button
import com.firestorm.llui.CheckBoxCtrl
import com.firestorm.llui.ComboBox
import com.firestorm.llui.Floater
import com.firestorm.llui.FloaterReg
import com.firestorm.llui.FrameTimer
import com.firestorm.llui.KeywordToken
import com.firestorm.llui.LLSD
import com.firestorm.llui.Panel
import com.firestorm.llui.Preview
import com.firestorm.llui.ScrollListCtrl
import com.firestorm.llui.TextBox
import java.io.File

private const val MAX_HISTORY_COUNT: Int = 10
private const val LIVE_HELP_REFRESH_TIME: Float = 1f

private fun haveScriptUploadCap(objectId: LLUUID): Boolean {
    return false
}

class LiveLSLFile(
    private val filePath: String,
    private val onChangeCallback: (String) -> Boolean
) {
    private var ignoreNextUpdate: Boolean = false

    fun ignoreNextUpdate() {
        ignoreNextUpdate = true
    }

    fun loadFile(): Boolean {
        if (ignoreNextUpdate) {
            ignoreNextUpdate = false
            return true
        }
        return onChangeCallback(filePath)
    }

    fun filename(): String = filePath

    fun destroy() {
        File(filePath).delete()
    }
}

open class ScriptEdCore(
    private val container: ScriptEdContainer,
    private val sampleText: String,
    private val liveHelpHandle: Floater?,
    private val loadCallback: (Any?) -> Unit,
    private val saveCallback: (Any?, Boolean, Boolean) -> Unit,
    private val searchReplaceCallback: (Any?) -> Unit,
    private val userdata: Any?,
    val live: Boolean,
    private val bottomPad: Int = 0
) : Panel() {

    private var scriptName: String = ""
    private var editor: ScriptEditor? = null
    private var forceClose: Boolean = false
    private var errorList: ScrollListCtrl? = null
    private var functions: ComboBox? = null
    private var lastHelpToken: KeywordToken? = null
    private val liveHelpTimer: FrameTimer = FrameTimer()
    private var liveHelpHistorySize: Int = 0
    private var enableSave: Boolean = false
    private var hasScriptData: Boolean = false
    private var liveFile: LiveLSLFile? = null
    private var associatedExperience: LLUUID = LLUUID.NULL
    private var scriptRemoved: Boolean = false
    private var saveDialogShown: Boolean = false
    private var assetID: LLUUID = LLUUID.NULL
    private var lineCol: TextBox? = null
    private var saveBtn: Button? = null
    private var saveBtn2: Button? = null
    private var cutBtn: Button? = null
    private var copyBtn: Button? = null
    private var pasteBtn: Button? = null
    private var undoBtn: Button? = null
    private var redoBtn: Button? = null
    private var saveToDiskBtn: Button? = null
    private var loadFromDiskBtn: Button? = null
    private var searchBtn: Button? = null
    private var compiling: Boolean = false
    private var postScript: String = ""
    private var currentEditor: ScriptEditor? = null
    var syntaxIDConnectionIndex: Int = -1

    open fun postBuild(): Boolean {
        System.err.println("ScriptEdCore: postBuild not yet implemented")
        return false
    }

    fun initMenu() {
        System.err.println("ScriptEdCore: initMenu not yet implemented")
    }

    fun processKeywords() {
        functions?.clearRows()
        editor?.clearSegments()
        editor?.initKeywords()
        editor?.loadKeywords()

        val primaryKeywords = mutableListOf<String>()
        val secondaryKeywords = mutableListOf<String>()

        val tokenIt = editor?.keywordsBegin() ?: return
        val tokenEnd = editor?.keywordsEnd() ?: return
        System.err.println("ScriptEdCore: processKeywords keyword iteration not yet implemented")
    }

    open fun draw() {
        updateButtonBar()
        if (editor?.hasFocus() == true) {
            System.err.println("ScriptEdCore: draw line/col update not yet implemented")
        }
        updateDynamicHelp()
        super.draw()
    }

    fun setEnableEditing(enable: Boolean) {
        editor?.setEnabled(enable)
    }

    fun canLoadOrSaveToFile(): Boolean {
        return false
    }

    fun setScriptText(text: String, isValid: Boolean) {
        editor?.let {
            it.setText(text)
            hasScriptData = isValid
        }
    }

    fun getScriptText(): String = editor?.getText() ?: ""

    fun doSaveComplete(userdata: Any?, closeAfterSave: Boolean, sync: Boolean) {
        saveCallback(userdata, closeAfterSave, sync)
    }

    fun makeEditorPristine() {
        editor?.makePristine()
    }

    fun loadScriptText(filename: String): Boolean {
        if (filename.isEmpty()) return false
        val file = File(filename)
        if (!file.exists()) return false
        val text = file.readText()
        editor?.setText(text)
        return true
    }

    fun writeToFile(filename: String, unprocessed: Boolean): Boolean {
        val text = if (unprocessed) editor?.getText() ?: "" else getScriptText()
        val content = if (text.isEmpty()) " " else text
        return try {
            File(filename).writeText(content)
            true
        } catch (e: Exception) {
            errorList?.addElement(LLSD().apply {
                put("columns[0].value", "Error writing to local file. Is your hard drive full?")
                put("columns[0].font", "SANSSERIF_SMALL")
            })
            false
        }
    }

    fun sync() {
        val lf = liveFile ?: return
        val tmp = lf.filename()
        if (File(tmp).exists()) {
            lf.ignoreNextUpdate()
            writeToFile(tmp, false)
        }
    }

    fun doSave(closeAfterSave: Boolean, sync: Boolean = true) {
        if (saveDialogShown) return
        if (hasChanged()) {
            saveCallback(userdata, closeAfterSave, sync)
        }
    }

    fun canClose(): Boolean {
        if (mForceClose || !hasChanged()) return true
        if (!saveDialogShown) {
            saveDialogShown = true
            System.err.println("ScriptEdCore: canClose save-changes dialog not yet implemented")
        }
        return false
    }

    fun handleSaveChangesDialog(notification: LLSD, response: LLSD): Boolean {
        return false
    }

    fun handleReloadFromServerDialog(notification: LLSD, response: LLSD): Boolean {
        return false
    }

    fun openInExternalEditor() {
        System.err.println("ScriptEdCore: openInExternalEditor not yet implemented")
    }

    fun selectAll() {
        editor?.selectAll()
    }

    fun getAssociatedExperience(): LLUUID = associatedExperience

    fun setAssociatedExperience(experienceId: LLUUID) {
        associatedExperience = experienceId
    }

    fun setScriptName(name: String) {
        scriptName = name
    }

    fun setItemRemoved(scriptRemoved: Boolean) {
        this.scriptRemoved = scriptRemoved
    }

    fun setAssetID(assetId: LLUUID) {
        assetID = assetId
    }

    fun getAssetID(): LLUUID = assetID

    open fun hasAccelerators(): Boolean = true

    open fun handleKeyHere(key: Int, mask: Int): Boolean {
        return false
    }

    fun enableSave(enabled: Boolean) {
        enableSave = enabled
    }

    fun onBtnDynamicHelp() {
        System.err.println("ScriptEdCore: onBtnDynamicHelp not yet implemented")
    }

    fun updateIndicators(compiling: Boolean, success: Boolean) {
        this.compiling = compiling
        System.err.println("ScriptEdCore: updateIndicators not yet implemented")
    }

    private fun hasChanged(): Boolean {
        val ed = editor ?: return false
        return (!ed.isPristine() || enableSave) && hasScriptData
    }

    private fun updateButtonBar() {
        saveBtn?.setEnabled(hasChanged() && !scriptRemoved)
        cutBtn?.setEnabled(currentEditor?.canCut() ?: false)
        copyBtn?.setEnabled(currentEditor?.canCopy() ?: false)
        pasteBtn?.setEnabled(currentEditor?.canPaste() ?: false)
        undoBtn?.setEnabled(currentEditor?.canUndo() ?: false)
        redoBtn?.setEnabled(currentEditor?.canRedo() ?: false)
        saveToDiskBtn?.setEnabled(editor?.canLoadOrSaveToFile() ?: false)
        loadFromDiskBtn?.setEnabled(editor?.canLoadOrSaveToFile() ?: false)
    }

    private fun onBtnUndoChanges() {
        System.err.println("ScriptEdCore: onBtnUndoChanges not yet implemented")
    }

    private fun performAction(action: String) {
        val ed = currentEditor ?: return
        when (action) {
            "Revert All Changes" -> onBtnUndoChanges()
            "Undo"       -> ed.undo()
            "Redo"       -> ed.redo()
            "Cut"        -> ed.cut()
            "Copy"       -> ed.copy()
            "Paste"      -> ed.paste()
            "Select All" -> ed.selectAll()
            "Deselect"   -> ed.deselect()
            "Search"     -> System.err.println("ScriptEdCore: performAction Search not yet implemented")
        }
    }

    private fun enableAction(action: String): Boolean = when (action) {
        "Revert All Changes" -> currentEditor == editor && hasChanged()
        "Undo"       -> currentEditor?.canUndo() ?: false
        "Redo"       -> currentEditor?.canRedo() ?: false
        "Cut"        -> currentEditor?.canCut() ?: false
        "Copy"       -> currentEditor?.canCopy() ?: false
        "Paste"      -> currentEditor?.canPaste() ?: false
        "Select All" -> currentEditor?.canSelectAll() ?: false
        "Deselect"   -> currentEditor?.canDeselect() ?: false
        else         -> false
    }

    private fun updateDynamicHelp(immediate: Boolean = false) {
        System.err.println("ScriptEdCore: updateDynamicHelp not yet implemented")
    }

    private fun setHelpPage(helpString: String) {
        System.err.println("ScriptEdCore: setHelpPage not yet implemented")
    }

    private fun isKeyword(token: KeywordToken): Boolean = when (token.getType()) {
        KeywordToken.TT_CONSTANT,
        KeywordToken.TT_CONTROL,
        KeywordToken.TT_EVENT,
        KeywordToken.TT_FUNCTION -> true
        else -> false
    }

    private fun addHelpItemToHistory(helpString: String) {
        System.err.println("ScriptEdCore: addHelpItemToHistory not yet implemented")
    }

    protected fun deleteBridges() {
        System.err.println("ScriptEdCore: deleteBridges not yet implemented")
    }

    private fun selectFirstError() {
        errorList?.selectFirstItem()
    }

    private fun onBtnHelp() {
        System.err.println("ScriptEdCore: onBtnHelp not yet implemented")
    }

    private fun onFontChanged() {
        System.err.println("ScriptEdCore: onFontChanged not yet implemented")
    }

    private val mForceClose: Boolean get() = forceClose

    companion object {
        fun onBtnLoadFromFile(userdata: Any?) {
            System.err.println("ScriptEdCore: onBtnLoadFromFile not yet implemented")
        }

        fun onBtnSaveToFile(userdata: Any?) {
            System.err.println("ScriptEdCore: onBtnSaveToFile not yet implemented")
        }

        fun enableSaveToFileMenu(userdata: Any?): Boolean {
            return false
        }

        fun enableLoadFromFileMenu(userdata: Any?): Boolean {
            return false
        }
    }
}

abstract class ScriptEdContainer(key: LLSD) : Preview(key) {

    protected var scriptEd: ScriptEdCore? = null
    private var backupFilename: String = ""
    private var backupTimer: Any? = null

    open fun refreshFromItem() {
        System.err.println("ScriptEdContainer: refreshFromItem not yet implemented")
    }

    fun updateStyle() {
        System.err.println("ScriptEdContainer: updateStyle not yet implemented")
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        return false
    }

    protected fun getTmpFileName(scriptName: String): String {
        return ""
    }

    protected open fun getBackupFileName(): String {
        return ""
    }

    protected fun onBackupTimer(): Boolean {
        return false
    }

    protected fun onExternalChange(filename: String): Boolean {
        return false
    }

    protected abstract fun saveIfNeeded(sync: Boolean = true)
}

open class PreviewLSL(key: LLSD) : ScriptEdContainer(key) {

    private var pendingUploads: Int = 0
    private var itemObserver: Any? = null
    private var dirty: Boolean = false

    fun getScriptID(): LLUUID = LLUUID.NULL

    fun setDirty() {
        dirty = true
    }

    open fun callbackLSLCompileSucceeded() {
        System.err.println("PreviewLSL: callbackLSLCompileSucceeded not yet implemented")
    }

    open fun callbackLSLCompileFailed(compileErrors: LLSD) {
        System.err.println("PreviewLSL: callbackLSLCompileFailed not yet implemented")
    }

    open fun postBuild(): Boolean {
        System.err.println("PreviewLSL: postBuild not yet implemented")
        return false
    }

    fun getEditor(): ScriptEditor? = null

    override fun draw() {
        super.draw()
    }

    open fun canClose(): Boolean {
        return scriptEd?.canClose() ?: true
    }

    private fun closeIfNeeded() {
        if (pendingUploads <= 0) {
            System.err.println("PreviewLSL: closeIfNeeded not yet implemented")
        }
    }

    open fun loadAsset() {
        System.err.println("PreviewLSL: loadAsset not yet implemented")
    }

    override fun saveIfNeeded(sync: Boolean) {
        System.err.println("PreviewLSL: saveIfNeeded not yet implemented")
    }

    companion object {
        fun onSearchReplace(userdata: Any?) {
            System.err.println("PreviewLSL: onSearchReplace not yet implemented")
        }

        fun onLoad(userdata: Any?) {
            (userdata as? PreviewLSL)?.loadAsset()
        }

        fun onSave(userdata: Any?, closeAfterSave: Boolean, sync: Boolean) {
            (userdata as? PreviewLSL)?.saveIfNeeded(sync)
        }

        fun onLoadComplete(uuid: LLUUID, type: Int, userData: Any?, status: Int, extStatus: Int) {
            System.err.println("PreviewLSL: onLoadComplete not yet implemented")
        }

        fun finishedLSLUpload(itemId: LLUUID, response: LLSD) {
            System.err.println("PreviewLSL: finishedLSLUpload not yet implemented")
        }

        fun failedLSLUpload(itemId: LLUUID, taskId: LLUUID, response: LLSD, reason: String): Boolean {
            return false
        }

        fun createScriptEdPanel(userdata: Any?): Any? {
            return null
        }
    }
}

open class LiveLSLEditor(key: LLSD) : ScriptEdContainer(key) {

    private var isNew: Boolean = false
    private var askedForRunningInfo: Boolean = false
    private var haveRunningInfo: Boolean = false
    private var item: Any? = null
    private var closeAfterSave: Boolean = false
    private var pendingUploads: Int = 0
    private var isSaving: Boolean = false
    private var isModifiable: Boolean = false
    private var monoCheckbox: CheckBoxCtrl? = null
    private var experiences: ComboBox? = null
    private var experienceEnabled: CheckBoxCtrl? = null
    private var experienceIds: LLSD = LLSD()
    private var experienceProfile: Floater? = null
    private var objectName: String = ""

    fun setIsNew() {
        isNew = true
    }

    fun getEditor(): ScriptEditor? = null

    fun setObjectName(name: String) {
        objectName = name
    }

    fun experienceChanged() {
        val current = scriptEd?.getAssociatedExperience() ?: return
        val selected: LLUUID = experiences?.getSelectedValue()?.asUUID() ?: LLUUID.NULL
        if (current != selected) {
            scriptEd?.enableSave(isModifiable)
            scriptEd?.setAssociatedExperience(selected)
            updateExperiencePanel()
        }
    }

    fun setExperienceIds(experienceIds: LLSD) {
        this.experienceIds = experienceIds
        buildExperienceList()
    }

    fun buildExperienceList() {
        System.err.println("LiveLSLEditor: buildExperienceList not yet implemented")
    }

    fun updateExperiencePanel() {
        System.err.println("LiveLSLEditor: updateExperiencePanel not yet implemented")
    }

    fun requestExperiences() {
        System.err.println("LiveLSLEditor: requestExperiences not yet implemented")
    }

    open fun postBuild(): Boolean {
        System.err.println("LiveLSLEditor: postBuild not yet implemented")
        return false
    }

    open fun callbackLSLCompileSucceeded(taskId: LLUUID, itemId: LLUUID, isScriptRunning: Boolean) {
        System.err.println("LiveLSLEditor: callbackLSLCompileSucceeded not yet implemented")
    }

    open fun callbackLSLCompileFailed(compileErrors: LLSD) {
        System.err.println("LiveLSLEditor: callbackLSLCompileFailed not yet implemented")
    }

    open fun canClose(): Boolean = scriptEd?.canClose() ?: true

    private fun closeIfNeeded() {
        if (pendingUploads <= 0 && closeAfterSave) {
            System.err.println("LiveLSLEditor: closeIfNeeded not yet implemented")
        }
    }

    override fun draw() {
        super.draw()
    }

    open fun loadAsset() {
        System.err.println("LiveLSLEditor: loadAsset not yet implemented")
    }

    override fun saveIfNeeded(sync: Boolean) {
        System.err.println("LiveLSLEditor: saveIfNeeded not yet implemented")
    }

    private fun monoChecked(): Boolean = monoCheckbox?.get() ?: false

    companion object {
        fun processScriptRunningReply(msg: Any?, unused: Any?) {
            System.err.println("LiveLSLEditor: processScriptRunningReply not yet implemented")
        }

        fun setAssociatedExperience(editor: LiveLSLEditor?, experience: LLSD) {
            editor?.scriptEd?.setAssociatedExperience(experience.asUUID())
            editor?.updateExperiencePanel()
        }

        fun onToggleExperience(ui: Any?, userdata: Any?) {
            val self = userdata as? LiveLSLEditor ?: return
            val id = if ((ui as? CheckBoxCtrl)?.get() == true) {
                val cur = self.scriptEd?.getAssociatedExperience() ?: LLUUID.NULL
                if (cur.isNull()) self.experienceIds.beginArray()?.asUUID() ?: LLUUID.NULL
                else cur
            } else {
                LLUUID.NULL
            }
            if (id != self.scriptEd?.getAssociatedExperience()) {
                self.scriptEd?.enableSave(self.isModifiable)
            }
            self.scriptEd?.setAssociatedExperience(id)
            self.updateExperiencePanel()
        }

        fun onViewProfile(ui: Any?, userdata: Any?) {
            val self = userdata as? LiveLSLEditor ?: return
            if (self.experienceEnabled?.get() == true) {
                val id = self.scriptEd?.getAssociatedExperience() ?: LLUUID.NULL
                if (id.isNotNull()) {
                    FloaterReg.showInstance("experience_profile", LLSD(id), focus = true)
                }
            }
        }

        fun onSearchReplace(userdata: Any?) {
            System.err.println("LiveLSLEditor: onSearchReplace not yet implemented")
        }

        fun onLoad(userdata: Any?) {
            (userdata as? LiveLSLEditor)?.loadAsset()
        }

        fun onSave(userdata: Any?, closeAfterSave: Boolean, sync: Boolean) {
            (userdata as? LiveLSLEditor)?.saveIfNeeded(sync)
        }

        fun onLoadComplete(assetUuid: LLUUID, type: Int, userData: Any?, status: Int, extStatus: Int) {
            System.err.println("LiveLSLEditor: onLoadComplete not yet implemented")
        }

        fun onRunningCheckboxClicked(ui: Any?, userdata: Any?) {
            System.err.println("LiveLSLEditor: onRunningCheckboxClicked not yet implemented")
        }

        fun onReset(userdata: Any?) {
            System.err.println("LiveLSLEditor: onReset not yet implemented")
        }

        fun onMonoCheckboxClicked(ui: Any?, userdata: Any?) {
            System.err.println("LiveLSLEditor: onMonoCheckboxClicked not yet implemented")
        }

        fun finishLSLUpload(itemId: LLUUID, taskId: LLUUID, newAssetId: LLUUID, response: LLSD, isRunning: Boolean) {
            System.err.println("LiveLSLEditor: finishLSLUpload not yet implemented")
        }

        fun receiveExperienceIds(result: LLSD, parent: LiveLSLEditor?) {
            parent?.setExperienceIds(result)
        }

        fun createScriptEdPanel(userdata: Any?): Any? {
            return null
        }
    }
}
