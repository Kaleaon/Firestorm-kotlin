package com.firestorm.newview

import java.io.File

private const val PRIMARY_FLOATER: Int = 1
private const val SECONDARY_FLOATER: Int = 2

private fun getXuiDir(skin: String = "default"): String {
    val delim = gDirUtilp.getDirDelimiter()
    return gDirUtilp.getSkinBaseDir() + delim + skin + delim + "xui" + delim
}

class LLOverlapPanel(params: Params = Params()) : LLPanel(params) {

    class Params : LLPanel.Params()

    val overlapMap: MutableMap<LLView, MutableList<LLView>> = mutableMapOf()
    var lastClickedElement: LLView? = null

    val originalWidth: Int = rect.getWidth()
    val originalHeight: Int = rect.getHeight()
    val spacing: Int = 10

    override fun draw() {
        val currentSelectionText = "Current selection: "
        val overlapperText = "Overlapper: "
        val textColor = LLColor4.grey
        TODO("GPU: gGL.color4fv(textColor.mV)")

        val clickedElement = LLView.sPreviewClickedElement
        if (clickedElement == null) {
            TODO("GPU: LLUI.translate(5f, rect.getHeight() - 20f)")
            LLView.sDrawPreviewHighlights = false
            TODO("GPU: render '$currentSelectionText' with LLFontGL")
            return
        }

        val iterExists = overlapMap[clickedElement] ?: return

        val overlappers = overlapMap[clickedElement] ?: mutableListOf()
        if (overlappers.isEmpty()) {
            TODO("GPU: LLUI.translate and render '$currentSelectionText${clickedElement.name} (no elements overlap)'")
            LLView.sDrawPreviewHighlights = false
            return
        }

        val needToRecalcBounds = lastClickedElement == null
        if (lastClickedElement == null) {
            lastClickedElement = clickedElement
        }

        if (needToRecalcBounds || clickedElement.name != lastClickedElement?.name) {
            TODO("GPU: recalculate panel bounds to fit selected element and all overlappers")
        }

        TODO("GPU: LLUI.translate and render selected element name + each overlapper name and widget miniature")
        LLView.sDrawPreviewHighlights = false

        lastClickedElement = clickedElement
    }
}

class LLLocalizationResetForcer(private val floater: LLFloaterUIPreview, id: Int) {

    private val savedLocalization: String =
        LLUI.instance.mSettingGroups["config"]!!.getString("Language")

    init {
        LLUI.instance.mSettingGroups["config"]!!.setString("Language", floater.getLocStr(id))
        gDirUtilp.setSkinFolder(
            gDirUtilp.getSkinFolder(),
            gDirUtilp.getSkinThemeFolder(),
            floater.getLocStr(id)
        )
    }

    fun reset() {
        LLUI.instance.mSettingGroups["config"]!!.setString("Language", savedLocalization)
        gDirUtilp.setSkinFolder(
            gDirUtilp.getSkinFolder(),
            gDirUtilp.getSkinThemeFolder(),
            savedLocalization
        )
    }
}

class LLGUIPreviewLiveFile(
    path: String,
    val fileName: String,
    val parent: LLFloaterUIPreview
) : LLLiveFile(path, 1.0) {

    var fadeTimer: LLFadeEventTimer? = null
    var firstFade: Boolean = true

    override fun loadFile(): Boolean {
        parent.displayFloater(click = false, id = 1)
        if (firstFade) {
            firstFade = false
        } else {
            fadeTimer?.mParent = null
            fadeTimer = LLFadeEventTimer(0.05f, this)
        }
        return true
    }

    fun onDestroy() {
        parent.mLiveFile = null
        fadeTimer?.mParent = null
    }
}

class LLFadeEventTimer(refresh: Float, parent: LLGUIPreviewLiveFile) : LLEventTimer(refresh) {

    var mParent: LLGUIPreviewLiveFile? = parent
    private var fadingOut: Boolean = true
    private val originalColor: LLColor4 = parent.parent.mDisplayedFloater!!.getBackgroundColor()

    override fun tick(): Boolean {
        var diff = 0.04f
        if (fadingOut) diff = -diff

        val p = mParent ?: return true

        val bgColor = p.parent.mDisplayedFloater?.getBackgroundColor() ?: return true
        val colors = bgColor.getValue().toMutableList()
        val colorsOld = colors.toList()

        colors[0] = (colors[0] as Double - diff).coerceAtLeast(originalColor.getValue()[0] as Double)
        colors[1] = (colors[1] as Double - diff).coerceAtLeast(originalColor.getValue()[1] as Double)
        colors[2] = (colors[2] as Double + diff).coerceAtMost(originalColor.getValue()[2] as Double)

        bgColor.setValue(colors)
        bgColor.clamp()
        p.parent.mDisplayedFloater?.setBackgroundColor(bgColor)

        if (bgColor.b <= 0.0f) {
            fadingOut = false
        }
        return false
    }
}

class LLPreviewedFloater(
    val floaterUIPreview: LLFloaterUIPreview,
    params: Params
) : LLFloater(LLSD(), params) {

    override fun draw() {
        if (floaterUIPreview.mHighlightingOverlaps) {
            LLView.sDrawPreviewHighlights = true
        }

        val oldDebugRects = LLView.sDebugRects
        val oldShowNames = LLView.sDebugRectsShowNames
        if (sShowRectangles) {
            LLView.sDebugRects = true
            LLView.sDebugRectsShowNames = false
        }

        super.draw()

        LLView.sDebugRects = oldDebugRects
        LLView.sDebugRectsShowNames = oldShowNames

        if (floaterUIPreview.mHighlightingOverlaps) {
            LLView.sDrawPreviewHighlights = false
        }
    }

    fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        selectElement(this, x, y, 0)
        return true
    }

    fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        if (!sShowRectangles) {
            return super.handleToolTip(x, y, mask)
        }
        TODO("GPU: DFS over visible children; build tooltip string with name + position + size; show via LLToolTipMgr")
    }

    fun selectElement(parent: LLView, x: Int, y: Int, depth: Int): Boolean {
        if (!parent.isVisible()) return false

        if (LLFloaterUIPreview.containerType(parent)) {
            for (child in parent.getChildList()) {
                val localX = x - child.rect.left
                val localY = y - child.rect.bottom
                if (child.pointInView(localX, localY) && child.isVisible() &&
                    selectElement(child, x, y, depth + 1)
                ) {
                    return true
                }
            }
        }

        LLView.sPreviewClickedElement = parent
        return true
    }

    companion object {
        var sShowRectangles: Boolean = false
    }
}

class LLFloaterUIPreview(key: LLSD) : LLFloater(key) {

    var mDisplayedFloater: LLPreviewedFloater? = null
    var mDisplayedFloater_2: LLPreviewedFloater? = null
    var mLiveFile: LLGUIPreviewLiveFile? = null
    var mOverlapPanel: LLOverlapPanel? = null
    var mHighlightingOverlaps: Boolean = false

    val mDiffsMap: MutableMap<String, Pair<MutableList<String>, MutableList<String>>> = mutableMapOf()

    private var mFileList: LLScrollListCtrl? = null
    private var mEditorPathTextBox: LLLineEditor? = null
    private var mEditorArgsTextBox: LLLineEditor? = null
    private var mDiffPathTextBox: LLLineEditor? = null
    private var mDisplayFloaterBtn: LLButton? = null
    private var mDisplayFloaterBtn_2: LLButton? = null
    private var mEditFloaterBtn: LLButton? = null
    private var mExecutableBrowseButton: LLButton? = null
    private var mCloseOtherButton: LLButton? = null
    private var mCloseOtherButton_2: LLButton? = null
    private var mDiffBrowseButton: LLButton? = null
    private var mToggleHighlightButton: LLButton? = null
    private var mToggleOverlapButton: LLButton? = null
    private var mLanguageSelection: LLComboBox? = null
    private var mLanguageSelection_2: LLComboBox? = null
    private var mLastDisplayedX: Int = 0
    private var mLastDisplayedY: Int = 0
    private var mDelim: String = ""

    private var mSavedEditorPath: String = ""
    private var mSavedEditorArgs: String = ""
    private var mSavedDiffPath: String = ""

    private val mExternalEditor: LLExternalEditor = LLExternalEditor()

    override fun postBuild(): Boolean {
        val mainPanel = getChild<LLPanel>("main_panel")
        mFileList = mainPanel.getChild<LLScrollListCtrl>("name_list")
        mFileList!!.setDoubleClickCallback { onClickDisplayFloater(PRIMARY_FLOATER) }

        setDefaultBtn("display_floater")

        mLanguageSelection = mainPanel.getChild<LLComboBox>("language_select_combo")
        mLanguageSelection!!.setCommitCallback { onLanguageComboSelect(mLanguageSelection!!) }
        mLanguageSelection_2 = mainPanel.getChild<LLComboBox>("language_select_combo_2")
        mLanguageSelection_2!!.setCommitCallback { onLanguageComboSelect(mLanguageSelection!!) }

        val editorPanel = mainPanel.getChild<LLPanel>("editor_panel")
        mDisplayFloaterBtn = mainPanel.getChild<LLButton>("display_floater")
        mDisplayFloaterBtn!!.setClickedCallback { onClickDisplayFloater(PRIMARY_FLOATER) }
        mDisplayFloaterBtn_2 = mainPanel.getChild<LLButton>("display_floater_2")
        mDisplayFloaterBtn_2!!.setClickedCallback { onClickDisplayFloater(SECONDARY_FLOATER) }
        mToggleOverlapButton = mainPanel.getChild<LLButton>("toggle_overlap_panel")
        mToggleOverlapButton!!.setClickedCallback { onClickToggleOverlapping() }
        mCloseOtherButton = mainPanel.getChild<LLButton>("close_displayed_floater")
        mCloseOtherButton!!.setClickedCallback { onClickCloseDisplayedFloater(PRIMARY_FLOATER) }
        mCloseOtherButton_2 = mainPanel.getChild<LLButton>("close_displayed_floater_2")
        mCloseOtherButton_2!!.setClickedCallback { onClickCloseDisplayedFloater(SECONDARY_FLOATER) }
        mEditFloaterBtn = mainPanel.getChild<LLButton>("edit_floater")
        mEditFloaterBtn!!.setClickedCallback { onClickEditFloater() }
        mExecutableBrowseButton = editorPanel.getChild<LLButton>("browse_for_executable")
        mExecutableBrowseButton!!.setClickedCallback { onClickBrowseForEditor() }

        val vltPanel = mainPanel.getChild<LLPanel>("vlt_panel")
        mDiffBrowseButton = vltPanel.getChild<LLButton>("browse_for_vlt_diffs")
        mDiffBrowseButton!!.setClickedCallback { onClickBrowseForDiffs() }
        mToggleHighlightButton = vltPanel.getChild<LLButton>("toggle_vlt_diff_highlight")
        mToggleHighlightButton!!.setClickedCallback { onClickToggleDiffHighlighting() }

        mainPanel.getChild<LLButton>("save_floater")
            .setClickedCallback { onClickSaveFloater(PRIMARY_FLOATER) }
        mainPanel.getChild<LLButton>("save_all_floaters")
            .setClickedCallback { onClickSaveAll(PRIMARY_FLOATER) }

        getChild<LLButton>("refresh_btn").setClickedCallback { refreshList() }
        getChild<LLButton>("export_schema").setClickedCallback { onClickExportSchema() }
        getChild<LLUICtrl>("show_rectangles")
            .setCommitCallback { data -> onClickShowRectangles(data) }

        mEditorPathTextBox = editorPanel.getChild<LLLineEditor>("executable_path_field")
        mEditorArgsTextBox = editorPanel.getChild<LLLineEditor>("executable_args_field")
        mDiffPathTextBox = vltPanel.getChild<LLLineEditor>("vlt_diff_path_field")

        mEditorPathTextBox!!.setText(mSavedEditorPath)
        mEditorArgsTextBox!!.setText(mSavedEditorArgs)
        mDiffPathTextBox!!.setText(mSavedDiffPath)

        mOverlapPanel = getChild<LLOverlapPanel>("overlap_panel")
        getChildView("overlap_scroll").setVisible(mHighlightingOverlaps)

        mDelim = gDirUtilp.getDirDelimiter()

        var foundEnUs = false
        val xuiDir = getXuiDir()
        mLanguageSelection!!.removeall()

        val iter = LLDirIterator(xuiDir, "*")
        var languageDirectory: String? = iter.next()
        while (languageDirectory != null) {
            val fullPath = gDirUtilp.add(xuiDir, languageDirectory)
            if (!File(fullPath).isDirectory) {
                languageDirectory = iter.next()
                continue
            }
            if (!languageDirectory.startsWith("template") && !languageDirectory.contains(".")) {
                if (languageDirectory.startsWith("en")) {
                    foundEnUs = true
                } else {
                    mLanguageSelection!!.add(languageDirectory)
                    mLanguageSelection_2!!.add(languageDirectory)
                }
            }
            languageDirectory = iter.next()
        }

        if (foundEnUs) {
            mLanguageSelection!!.add("en", ADD_TOP)
            mLanguageSelection_2!!.add("en", ADD_TOP)
        } else {
            popupAndPrintWarning("No EN localization found; check your XUI directories!")
        }

        mLanguageSelection!!.selectFirstItem()
        mLanguageSelection_2!!.selectFirstItem()

        refreshList()
        return true
    }

    fun onClose(appQuitting: Boolean) {
        if (!appQuitting && mDisplayedFloater != null) {
            onClickCloseDisplayedFloater(PRIMARY_FLOATER)
            onClickCloseDisplayedFloater(SECONDARY_FLOATER)
            mDisplayedFloater = null
            mDisplayedFloater_2 = null
        }
        mSavedEditorPath = mEditorPathTextBox?.getText() ?: ""
        mSavedEditorArgs = mEditorArgsTextBox?.getText() ?: ""
        mSavedDiffPath = mDiffPathTextBox?.getText() ?: ""
        mLiveFile?.onDestroy()
        mLiveFile = null
    }

    fun getLocStr(id: Int): String =
        if (id == 1) mLanguageSelection!!.getSelectedItemLabel(0)
        else mLanguageSelection_2!!.getSelectedItemLabel(0)

    private fun getLocalizedDirectory(skin: String = "default"): String =
        getXuiDir(skin) + getLocStr(1) + mDelim

    fun refreshList() {
        mFileList!!.clearRows()

        listFilesWithPattern("exo_*.xml")
        listFilesWithPattern("floater_*.xml")
        listFilesWithPattern("fs_*.xml")
        listFilesWithPattern("inspect_*.xml")
        listFilesWithPattern("menu_*.xml")
        listFilesWithPattern("panel_*.xml")
        listFilesWithPattern("sidepanel_*.xml")

        if (!mFileList!!.isEmpty()) {
            mFileList!!.selectFirstItem()
        }
    }

    private fun listFilesWithPattern(pattern: String) {
        val dir = getLocalizedDirectory()
        val iter = LLDirIterator(dir, pattern)
        var name: String? = iter.next()
        while (name != null) {
            addFloaterEntry(name)
            name = iter.next()
        }
    }

    fun addFloaterEntry(path: String) {
        val entryId = LLUUID().apply { generate(path) }

        val xmlTree = LLXmlTree()
        val fullPath = getLocalizedDirectory() + path
        val success = xmlTree.parseFile(fullPath, true)

        var entryName = ""
        var entryTitle = ""

        if (success) {
            val rootFloater = xmlTree.getRoot()
            if (rootFloater == null) {
                popupAndPrintWarning("No root node found in XUI file: $path")
                return
            }
            entryName = rootFloater.getAttributeString("name")
                .takeIf { it.isNotEmpty() } ?: "Error: unable to load $path"
            entryTitle = rootFloater.getAttributeString("title")
        } else {
            popupAndPrintWarning("Unable to parse XUI file: $path")
            mLiveFile?.onDestroy()
            mLiveFile = null
            return
        }

        val row = LLSD.map(
            "id" to LLSD(entryId),
            "columns" to LLSD.array(
                LLSD.map("column" to "title_column", "type" to "text", "value" to entryTitle),
                LLSD.map("column" to "file_column", "type" to "text", "value" to path),
                LLSD.map("column" to "top_level_node_column", "type" to "text", "value" to entryName)
            )
        )
        mFileList!!.addElement(row)
    }

    fun displayFloater(click: Boolean, id: Int) {
        val resetForcer = LLLocalizationResetForcer(this, id)
        try {
            val floaterRef: LLPreviewedFloater?

            if (id == 1) {
                if (mDisplayedFloater != null) {
                    mLastDisplayedX = mDisplayedFloater!!.calcScreenRect().left
                    mLastDisplayedY = mDisplayedFloater!!.calcScreenRect().bottom
                    mDisplayedFloater = null
                }
            } else {
                mDisplayedFloater_2 = null
            }

            val path = mFileList!!.getSelectedItemLabel(1)
            if (path.isEmpty()) return

            val p = LLFloater.getDefaultParams().apply {
                minHeight = headerHeight
                minWidth = 10
            }

            val newFloater = LLPreviewedFloater(this, p)

            when {
                path.startsWith("floater_") || path.startsWith("inspect_") -> {
                    newFloater.buildFromFile(path)
                    newFloater.openFloater(newFloater.getKey())
                    newFloater.setCanResize(newFloater.isResizable())
                }
                path.startsWith("menu_") -> {
                    // save/menu processing removed
                }
                else -> {
                    newFloater.setCanResize(true)
                    val floaterHeaderSize = LLFloater.getDefaultParams().headerHeight
                    val panel = LLUICtrlFactory.create<LLPanel>(LLPanel.Params())
                    panel.buildFromFile(path)
                    panel.setOrigin(2, 2)
                    newFloater.setTitle(path)
                    panel.setUseBoundingRect(true)
                    panel.updateBoundingRect()
                    val boundingRect = panel.getBoundingRect()
                    val newRect = panel.getRect().unionWith(boundingRect)
                    val floaterRect = newRect.stretch(4, 4)
                    newFloater.reshape(floaterRect.getWidth(), floaterRect.getHeight() + floaterHeaderSize)
                    panel.reshape(newRect.getWidth(), newRect.getHeight())
                    newFloater.addChild(panel)
                    newFloater.openFloater()
                }
            }

            if (id == 1) newFloater.setOrigin(mLastDisplayedX, mLastDisplayedY)

            // Prevent double-free: the floater must not be closeable via its own close button
            newFloater.setCanClose(false)

            if (id == 1) {
                mDisplayedFloater = newFloater
                mCloseOtherButton?.setEnabled(true)
            } else {
                mDisplayedFloater_2 = newFloater
                mCloseOtherButton_2?.setEnabled(true)
            }

            val fullPath = getLocalizedDirectory() + path
            val floaterLang = if (File(fullPath).exists()) getLocStr(id) else "EN"
            val suffix = if (id == 1) " - Primary" else " - Secondary"
            newFloater.setTitle("${newFloater.getTitle()} [$floaterLang$suffix]")

            newFloater.center()
            addDependentFloater(newFloater)

            if (click && id == 1) {
                mLiveFile?.onDestroy()
                mLiveFile = LLGUIPreviewLiveFile(fullPath, path, this)
                mLiveFile!!.checkAndReload()
                mLiveFile!!.addToEventTimer()
            }

            if (id == 1) mToggleOverlapButton?.setEnabled(true)

            if (LLView.sHighlightingDiffs && click && id == 1) {
                highlightChangedElements()
            }

            if (id == 1) {
                mOverlapPanel!!.overlapMap.clear()
                LLView.sPreviewClickedElement = null
                mOverlapPanel!!.lastClickedElement = null
                findOverlapsInChildren(mDisplayedFloater!!)

                if (mHighlightingOverlaps) {
                    for (view in mOverlapPanel!!.overlapMap.keys) {
                        LLView.sPreviewHighlightedElements.add(view)
                    }
                } else if (LLView.sHighlightingDiffs) {
                    highlightChangedElements()
                }
            }
        } finally {
            resetForcer.reset()
        }
    }

    private fun onClickDisplayFloater(id: Int) {
        displayFloater(click = true, id = id)
    }

    private fun onClickSaveFloater(id: Int) {
        displayFloater(click = true, id = id)
        popupAndPrintWarning("Save-floater functionality removed, use XML schema to clean up XUI files")
    }

    private fun onClickSaveAll(id: Int) {
        val listSize = mFileList!!.getItemCount()
        for (index in 0 until listSize) {
            mFileList!!.selectNthItem(index)
            displayFloater(click = true, id = id)
        }
        popupAndPrintWarning("Save-floater functionality removed, use XML schema to clean up XUI files")
    }

    private fun onLanguageComboSelect(ctrl: LLUICtrl) {
        val caller = ctrl as? LLComboBox ?: return
        if (caller.name == "language_select_combo") {
            if (mDisplayedFloater != null) {
                onClickCloseDisplayedFloater(PRIMARY_FLOATER)
                displayFloater(click = true, id = 1)
            }
        } else {
            if (mDisplayedFloater_2 != null) {
                onClickCloseDisplayedFloater(PRIMARY_FLOATER)
                displayFloater(click = true, id = 2)
            }
        }
    }

    private fun onClickExportSchema() {
        // Schema generation intentionally unimplemented — see C++ source for context
    }

    private fun onClickShowRectangles(data: LLSD) {
        LLPreviewedFloater.sShowRectangles = data.asBoolean()
    }

    private fun onClickEditFloater() {
        val fileName = mFileList!!.getSelectedItemLabel(1)
        if (fileName.isEmpty()) return

        var filePath = getLocalizedDirectory(gDirUtilp.getSkinFolder()) + fileName
        if (!File(filePath).exists()) {
            filePath = getLocalizedDirectory() + fileName
        }
        if (!File(filePath).exists()) {
            popupAndPrintWarning("No file for this floater exists in the selected localization.  Opening the EN version instead.")
            filePath = getXuiDir() + mDelim + "en" + mDelim + fileName
        }

        var cmdOverride = ""
        val bin = mEditorPathTextBox!!.getText()
        if (bin.isNotEmpty()) {
            val quotedBin = if (!bin.contains('"')) "\"$bin\"" else bin
            cmdOverride = "$quotedBin ${mEditorArgsTextBox!!.getText()}"
        }

        val status = mExternalEditor.setCommand("LL_XUI_EDITOR", cmdOverride)
        if (status != LLExternalEditor.EC_SUCCESS) {
            val warning = if (status == LLExternalEditor.EC_NOT_SPECIFIED)
                getString("ExternalEditorNotSet")
            else
                LLExternalEditor.getErrorMessage(status)
            popupAndPrintWarning(warning)
            return
        }

        if (mExternalEditor.run(filePath) != LLExternalEditor.EC_SUCCESS) {
            popupAndPrintWarning(LLExternalEditor.getErrorMessage(status))
        }
    }

    private fun onClickBrowseForEditor() {
        LLFilePickerReplyThread.startPicker(
            { filenames -> getExecutablePath(filenames) },
            LLFilePicker.FFLOAD_EXE,
            false
        )
    }

    private fun getExecutablePath(filenames: List<String>) {
        var executablePath = filenames[0]
        TODO("APR: use JVM equivalent — on macOS, inspect app bundle Info.plist to find CFBundleExecutable")
        mEditorPathTextBox!!.setText(executablePath)
    }

    private fun onClickBrowseForDiffs() {
        LLFilePickerReplyThread.startPicker(
            { filenames -> getDiffsFilePath(filenames) },
            LLFilePicker.FFLOAD_XML,
            false
        )
    }

    private fun getDiffsFilePath(filenames: List<String>) {
        mDiffPathTextBox!!.setText(filenames[0])
        if (LLView.sHighlightingDiffs) {
            onClickToggleDiffHighlighting()
            onClickToggleDiffHighlighting()
        }
    }

    private fun onClickToggleDiffHighlighting() {
        if (mHighlightingOverlaps) {
            onClickToggleOverlapping()
            mToggleOverlapButton?.toggleState()
        }

        LLView.sPreviewHighlightedElements.clear()
        mDiffsMap.clear()
        mFileList!!.clearHighlightedItems()

        if (LLView.sHighlightingDiffs) {
            LLView.sHighlightingDiffs = false
            return
        }

        val pathInTextField = mDiffPathTextBox!!.getText()
        var error = false

        if (pathInTextField.isEmpty()) {
            popupAndPrintWarning("Unable to highlight differences because no file was provided; fill in the relevant text field")
            error = true
        }

        if (!error && !File(pathInTextField).exists()) {
            popupAndPrintWarning("Unable to highlight differences because an invalid path to a difference file was provided:\"$pathInTextField\"")
            error = true
        }

        if (!error) {
            val xmlTree = LLXmlTree()
            if (xmlTree.parseFile(pathInTextField, true)) {
                val root = xmlTree.getRoot()
                if (root != null && root.name.startsWith("XuiDelta")) {
                    var child = root.getFirstChild()
                    while (child != null) {
                        when {
                            child.name.startsWith("file") -> scanDiffFile(child)
                            child.name.startsWith("error") -> {
                                val errorFile = child.getAttributeString("filename")
                                val errorMessage = child.getAttributeString("message")
                                mDiffsMap.getOrPut(errorFile) { Pair(mutableListOf(), mutableListOf()) }
                                    .second.add(errorMessage)
                            }
                            else -> {
                                popupAndPrintWarning("Child was neither a file or an error, but rather: \"${child.name}\"")
                                error = true
                            }
                        }
                        child = root.getNextChild()
                    }
                } else {
                    popupAndPrintWarning("Root node not named XuiDelta:\"$pathInTextField\"")
                    error = true
                }
            } else {
                popupAndPrintWarning("Unable to create tree from XML:\"$pathInTextField\"")
                error = true
            }
        }

        if (error) {
            mToggleHighlightButton?.setToggleState(false)
        } else {
            LLView.sHighlightingDiffs = true
            highlightChangedElements()
            highlightChangedFiles()
        }
    }

    private fun scanDiffFile(fileNode: LLXmlTreeNode) {
        val fileName = fileNode.getAttributeString("name")
        if (fileName.isEmpty()) {
            popupAndPrintWarning("Empty file name encountered in differences:\"$fileName\"")
            return
        }

        var child = fileNode.getFirstChild()
        while (child != null) {
            if (child.name.startsWith("delta")) {
                val id = child.getAttributeString("id")
                mDiffsMap.getOrPut(fileName) { Pair(mutableListOf(), mutableListOf()) }
                    .first.add(id)
            } else {
                popupAndPrintWarning("Child of file was not a delta, but rather: \"${child.name}\"")
                return
            }
            child = fileNode.getNextChild()
        }
    }

    private fun highlightChangedElements() {
        val liveFile = mLiveFile ?: return

        val (changedPaths, errors) = mDiffsMap[liveFile.fileName] ?: Pair(mutableListOf(), mutableListOf())

        for (path in changedPaths) {
            if (path.startsWith(".")) continue

            var element: LLView? = mDisplayedFloater
            val tokens = path.split(".")
            var failed = false
            for (token in tokens) {
                element = element?.findChild<LLView>(token, recurse = false)
                if (element == null) {
                    failed = true
                    break
                }
            }
            if (!failed && element != null) {
                LLView.sPreviewHighlightedElements.add(element)
            }
        }

        for (errorMsg in errors) {
            popupAndPrintWarning("Error listed among differences.  Filename: \"${liveFile.fileName}\".  Message: \"$errorMsg\"")
        }
    }

    private fun highlightChangedFiles() {
        for ((fileName, _) in mDiffsMap) {
            val item = mFileList!!.getItemByLabel(fileName, false, 1)
            item?.setHighlighted(true)
        }
    }

    private fun onClickCloseDisplayedFloater(callerId: Int) {
        if (callerId == PRIMARY_FLOATER) {
            mCloseOtherButton?.setEnabled(false)
            mToggleOverlapButton?.setEnabled(false)

            if (mDisplayedFloater != null) {
                mLastDisplayedX = mDisplayedFloater!!.calcScreenRect().left
                mLastDisplayedY = mDisplayedFloater!!.calcScreenRect().bottom
                mDisplayedFloater = null
            }

            mLiveFile?.onDestroy()
            mLiveFile = null

            if (mToggleOverlapButton?.getToggleState() == true) {
                mToggleOverlapButton?.toggleState()
                onClickToggleOverlapping()
            }

            LLView.sPreviewClickedElement = null
            mOverlapPanel?.lastClickedElement = null
        } else {
            mCloseOtherButton_2?.setEnabled(false)
            mDisplayedFloater_2 = null
        }
    }

    private fun onClickToggleOverlapping() {
        if (LLView.sHighlightingDiffs) {
            onClickToggleDiffHighlighting()
            mToggleHighlightButton?.toggleState()
        }
        LLView.sPreviewHighlightedElements.clear()

        val (width, height) = getResizeLimits()
        if (mHighlightingOverlaps) {
            mHighlightingOverlaps = false
            val panelWidth = mOverlapPanel!!.rect.getWidth()
            setRect(LLRect(rect.left, rect.top, rect.right - panelWidth, rect.bottom))
            setResizeLimits(width - panelWidth, height)
        } else {
            mHighlightingOverlaps = true
            displayFloater(click = false, id = 1)
            val panelWidth = mOverlapPanel!!.rect.getWidth()
            setRect(LLRect(rect.left, rect.top, rect.right + panelWidth, rect.bottom))
            setResizeLimits(width + panelWidth, height)
        }
        getChildView("overlap_scroll").setVisible(mHighlightingOverlaps)
    }

    private fun findOverlapsInChildren(parent: LLView) {
        if (parent.getChildCount() == 0 || !containerType(parent)) return

        val children = parent.getChildList()
        for (child in children) {
            if (overlapIgnorable(child)) continue
            for (sibling in children) {
                if (overlapIgnorable(sibling)) continue
                if (sibling !== child && elementOverlap(child, sibling)) {
                    mOverlapPanel!!.overlapMap.getOrPut(child) { mutableListOf() }.add(sibling)
                }
            }
            findOverlapsInChildren(child)
        }
    }

    private fun overlapIgnorable(viewp: LLView): Boolean =
        viewp is LLDragHandle || viewp is LLViewBorder || viewp is LLResizeBar

    private fun elementOverlap(view1: LLView, view2: LLView): Boolean {
        val r1 = view1.rect
        val r2 = view2.rect
        val tolerance = 2
        return r1.left <= r2.right - tolerance &&
            r2.left <= r1.right - tolerance &&
            r1.top >= r2.bottom + tolerance &&
            r2.top >= r1.bottom + tolerance
    }

    companion object {
        fun containerType(viewp: LLView): Boolean =
            viewp is LLPanel || viewp is LLLayoutStack

        private fun popupAndPrintWarning(warning: String) {
            LLNotificationsUtil.add("GenericAlert", LLSD.map("MESSAGE" to warning))
        }
    }
}

object LLFloaterUIPreviewUtil {
    fun registerFloater() {
        LLFloaterReg.add(
            "ui_preview",
            "floater_ui_preview.xml"
        ) { key -> LLFloaterUIPreview(key) }
    }
}
