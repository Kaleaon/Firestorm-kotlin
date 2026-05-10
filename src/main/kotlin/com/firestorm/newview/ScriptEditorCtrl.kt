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
    TODO("APR: use JVM equivalent - look up object region capability 'UpdateScriptTask'")
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
        TODO("APR: use JVM equivalent - wire up child views by name")
    }

    fun initMenu() {
        TODO("APR: use JVM equivalent - bind menu item callbacks to performAction/doSave etc.")
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
        TODO("APR: use JVM equivalent - iterate keyword tokens, separate TT_FUNCTION from others, add to combobox")
    }

    open fun draw() {
        updateButtonBar()
        if (editor?.hasFocus() == true) {
            TODO("APR: use JVM equivalent - get current line/col from editor and set lineCol value")
        }
        updateDynamicHelp()
        super.draw()
    }

    fun setEnableEditing(enable: Boolean) {
        editor?.setEnabled(enable)
    }

    fun canLoadOrSaveToFile(): Boolean {
        TODO("APR: use JVM equivalent - check editor canLoadOrSaveToFile")
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
            TODO("APR: use JVM equivalent - show save-changes notification dialog")
        }
        return false
    }

    fun handleSaveChangesDialog(notification: LLSD, response: LLSD): Boolean {
        TODO("APR: use JVM equivalent - handle save/discard/cancel response from notification")
    }

    fun handleReloadFromServerDialog(notification: LLSD, response: LLSD): Boolean {
        TODO("APR: use JVM equivalent - handle reload/cancel response from notification")
    }

    fun openInExternalEditor() {
        TODO("APR: use JVM equivalent - launch external editor process with script file")
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
        TODO("APR: use JVM equivalent - handle keyboard shortcuts for editor panel")
    }

    fun enableSave(enabled: Boolean) {
        enableSave = enabled
    }

    fun onBtnDynamicHelp() {
        TODO("APR: use JVM equivalent - open LSL keyword help floater for current token")
    }

    fun updateIndicators(compiling: Boolean, success: Boolean) {
        this.compiling = compiling
        TODO("APR: use JVM equivalent - show/hide progress_indicator and status_indicator child views")
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
        TODO("APR: use JVM equivalent - prompt reload from server")
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
            "Search"     -> TODO("APR: use JVM equivalent - show FloaterSearchReplace for currentEditor")
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
        TODO("APR: use JVM equivalent - update live help floater with current keyword token")
    }

    private fun setHelpPage(helpString: String) {
        TODO("APR: use JVM equivalent - navigate lsl_guide_html media control to helpString URL")
    }

    private fun isKeyword(token: KeywordToken): Boolean = when (token.getType()) {
        KeywordToken.TT_CONSTANT,
        KeywordToken.TT_CONTROL,
        KeywordToken.TT_EVENT,
        KeywordToken.TT_FUNCTION -> true
        else -> false
    }

    private fun addHelpItemToHistory(helpString: String) {
        TODO("APR: use JVM equivalent - add help string to history list, cap at MAX_HISTORY_COUNT")
    }

    protected fun deleteBridges() {
        TODO("APR: use JVM equivalent - destroy LLEntryAndEdCore bridge objects")
    }

    private fun selectFirstError() {
        errorList?.selectFirstItem()
    }

    private fun onBtnHelp() {
        TODO("APR: use JVM equivalent - open LSL portal help URL")
    }

    private fun onFontChanged() {
        TODO("APR: use JVM equivalent - re-apply font settings FSScriptingFontName / FSScriptingFontSize")
    }

    private val mForceClose: Boolean get() = forceClose

    companion object {
        fun onBtnLoadFromFile(userdata: Any?) {
            TODO("APR: use JVM equivalent - open file picker, load selected file into editor")
        }

        fun onBtnSaveToFile(userdata: Any?) {
            TODO("APR: use JVM equivalent - open file picker, save editor text to selected file")
        }

        fun enableSaveToFileMenu(userdata: Any?): Boolean {
            TODO("APR: use JVM equivalent - return true when editor has content and can write files")
        }

        fun enableLoadFromFileMenu(userdata: Any?): Boolean {
            TODO("APR: use JVM equivalent - return true when editor can load from file")
        }
    }
}

abstract class ScriptEdContainer(key: LLSD) : Preview(key) {

    protected var scriptEd: ScriptEdCore? = null
    private var backupFilename: String = ""
    private var backupTimer: Any? = null

    open fun refreshFromItem() {
        TODO("APR: use JVM equivalent - reload item data from inventory")
    }

    fun updateStyle() {
        TODO("APR: use JVM equivalent - re-apply color syntax highlighting style to script editor")
    }

    fun handleKeyHere(key: Int, mask: Int): Boolean {
        TODO("APR: use JVM equivalent - delegate key events to scriptEd")
    }

    protected fun getTmpFileName(scriptName: String): String {
        TODO("APR: use JVM equivalent - build temp file path for external editor")
    }

    protected open fun getBackupFileName(): String {
        TODO("APR: use JVM equivalent - build backup file path for script recovery")
    }

    protected fun onBackupTimer(): Boolean {
        TODO("APR: use JVM equivalent - write current editor content to backupFilename")
    }

    protected fun onExternalChange(filename: String): Boolean {
        TODO("APR: use JVM equivalent - reload editor text from filename after external editor save")
    }

    protected abstract fun saveIfNeeded(sync: Boolean = true)
}

open class PreviewLSL(key: LLSD) : ScriptEdContainer(key) {

    private var pendingUploads: Int = 0
    private var itemObserver: Any? = null
    private var dirty: Boolean = false

    fun getScriptID(): LLUUID = TODO("APR: use JVM equivalent - return mItemUUID")

    fun setDirty() {
        dirty = true
    }

    open fun callbackLSLCompileSucceeded() {
        TODO("APR: use JVM equivalent - mark compile succeeded, update UI")
    }

    open fun callbackLSLCompileFailed(compileErrors: LLSD) {
        TODO("APR: use JVM equivalent - populate error list with compile errors")
    }

    open fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent - wire up child views")
    }

    fun getEditor(): ScriptEditor? = scriptEd?.let { TODO("return scriptEd.editor") }

    override fun draw() {
        super.draw()
    }

    open fun canClose(): Boolean {
        return scriptEd?.canClose() ?: true
    }

    private fun closeIfNeeded() {
        if (pendingUploads <= 0) {
            TODO("APR: use JVM equivalent - close floater if no pending uploads")
        }
    }

    open fun loadAsset() {
        TODO("APR: use JVM equivalent - fetch script asset from asset storage")
    }

    override fun saveIfNeeded(sync: Boolean) {
        TODO("APR: use JVM equivalent - upload script text as asset, handle mono checkbox")
    }

    companion object {
        fun onSearchReplace(userdata: Any?) {
            TODO("APR: use JVM equivalent - show search/replace floater")
        }

        fun onLoad(userdata: Any?) {
            (userdata as? PreviewLSL)?.loadAsset()
        }

        fun onSave(userdata: Any?, closeAfterSave: Boolean, sync: Boolean) {
            (userdata as? PreviewLSL)?.saveIfNeeded(sync)
        }

        fun onLoadComplete(uuid: LLUUID, type: Int, userData: Any?, status: Int, extStatus: Int) {
            TODO("APR: use JVM equivalent - handle asset load response, set script text")
        }

        fun finishedLSLUpload(itemId: LLUUID, response: LLSD) {
            TODO("APR: use JVM equivalent - handle successful script asset upload")
        }

        fun failedLSLUpload(itemId: LLUUID, taskId: LLUUID, response: LLSD, reason: String): Boolean {
            TODO("APR: use JVM equivalent - handle failed script asset upload")
        }

        fun createScriptEdPanel(userdata: Any?): Any? {
            TODO("APR: use JVM equivalent - factory function for LLScriptEdCore panel")
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

    fun getEditor(): ScriptEditor? = scriptEd?.let { TODO("return scriptEd.editor") }

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
        TODO("APR: use JVM equivalent - populate experiences combobox from experienceIds LLSD array")
    }

    fun updateExperiencePanel() {
        TODO("APR: use JVM equivalent - show/hide experience panel based on experienceEnabled checkbox")
    }

    fun requestExperiences() {
        TODO("APR: use JVM equivalent - request experience IDs from capability server")
    }

    open fun postBuild(): Boolean {
        TODO("APR: use JVM equivalent - wire up child views including mono checkbox and experience panel")
    }

    open fun callbackLSLCompileSucceeded(taskId: LLUUID, itemId: LLUUID, isScriptRunning: Boolean) {
        TODO("APR: use JVM equivalent - mark compile succeeded, update running state")
    }

    open fun callbackLSLCompileFailed(compileErrors: LLSD) {
        TODO("APR: use JVM equivalent - populate error list with compile errors")
    }

    open fun canClose(): Boolean = scriptEd?.canClose() ?: true

    private fun closeIfNeeded() {
        if (pendingUploads <= 0 && closeAfterSave) {
            TODO("APR: use JVM equivalent - close floater after upload completes")
        }
    }

    override fun draw() {
        super.draw()
    }

    open fun loadAsset() {
        TODO("APR: use JVM equivalent - fetch script asset from object task inventory")
    }

    override fun saveIfNeeded(sync: Boolean) {
        TODO("APR: use JVM equivalent - compile and upload script to object task inventory")
    }

    private fun monoChecked(): Boolean = monoCheckbox?.get() ?: false

    companion object {
        fun processScriptRunningReply(msg: Any?, unused: Any?) {
            TODO("APR: use JVM equivalent - handle ScriptRunningReply message from simulator")
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
            TODO("APR: use JVM equivalent - show search/replace floater for live editor")
        }

        fun onLoad(userdata: Any?) {
            (userdata as? LiveLSLEditor)?.loadAsset()
        }

        fun onSave(userdata: Any?, closeAfterSave: Boolean, sync: Boolean) {
            (userdata as? LiveLSLEditor)?.saveIfNeeded(sync)
        }

        fun onLoadComplete(assetUuid: LLUUID, type: Int, userData: Any?, status: Int, extStatus: Int) {
            TODO("APR: use JVM equivalent - handle asset load response for task inventory script")
        }

        fun onRunningCheckboxClicked(ui: Any?, userdata: Any?) {
            TODO("APR: use JVM equivalent - send SetScriptRunning message to simulator")
        }

        fun onReset(userdata: Any?) {
            TODO("APR: use JVM equivalent - send ScriptReset message to simulator")
        }

        fun onMonoCheckboxClicked(ui: Any?, userdata: Any?) {
            TODO("APR: use JVM equivalent - re-enable save after mono checkbox change")
        }

        fun finishLSLUpload(itemId: LLUUID, taskId: LLUUID, newAssetId: LLUUID, response: LLSD, isRunning: Boolean) {
            TODO("APR: use JVM equivalent - handle completed script upload to task inventory")
        }

        fun receiveExperienceIds(result: LLSD, parent: LiveLSLEditor?) {
            parent?.setExperienceIds(result)
        }

        fun createScriptEdPanel(userdata: Any?): Any? {
            TODO("APR: use JVM equivalent - factory function for LLScriptEdCore panel in live editor")
        }
    }
}
