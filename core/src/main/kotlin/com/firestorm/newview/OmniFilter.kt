package com.firestorm.newview

import java.util.UUID

class OmniFilter(val key: Any) {

    private companion object {
        const val NEEDLE_CHECK_COLUMN = 0
        const val NEEDLE_NAME_COLUMN = 1
        const val LOG_DATE_COLUMN = 0
        const val LOG_CONTENT_COLUMN = 1
    }

    // UI control stubs — replaced by actual UI framework bindings at runtime.
    var needleListCtrl: ScrollListCtrl? = null
    var addNeedleBtn: Button? = null
    var removeNeedleBtn: Button? = null
    var filterLogCtrl: ScrollListCtrl? = null
    var panelDetails: Panel? = null
    var needleNameCtrl: LineEditor? = null
    var senderNameCtrl: LineEditor? = null
    var senderCaseSensitiveCheck: CheckBoxCtrl? = null
    var senderMatchTypeCombo: ComboBox? = null
    var contentCtrl: TextEditor? = null
    var contentCaseSensitiveCheck: CheckBoxCtrl? = null
    var contentMatchTypeCombo: ComboBox? = null
    var regionNameCtrl: LineEditor? = null
    var ownerCtrl: LineEditor? = null

    var typeNearbyBtn: Button? = null
    var typeImBtn: Button? = null
    var typeGroupImBtn: Button? = null
    var typeObjectChatBtn: Button? = null
    var typeObjectImBtn: Button? = null
    var typeScriptErrorBtn: Button? = null
    var typeDialogBtn: Button? = null
    var typeOfferBtn: Button? = null
    var typeInviteBtn: Button? = null
    var typeLureBtn: Button? = null
    var typeLoadUrlBtn: Button? = null
    var typeFriendshipOfferBtn: Button? = null
    var typeTeleportRequestBtn: Button? = null
    var typeGroupNoticeBtn: Button? = null

    var chatReplaceCtrl: LineEditor? = null
    var buttonReplyCtrl: LineEditor? = null
    var textBoxReplyCtrl: TextEditor? = null

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    fun postBuild(): Boolean {
        System.err.println("OmniFilter: postBuild not yet implemented")

        // Populate needle list from engine state.
        needleListCtrl?.deleteAllItems()
        for ((needleName, needle) in OmniFilterEngine.getNeedleList()) {
            addNeedle(needleName, needle)
        }

        // Replay previously buffered log lines into the log control.
        for ((logTime, logMessage) in OmniFilterEngine.log) {
            onLogLine(logTime, logMessage)
        }

        // Subscribe to future log events.
        OmniFilterEngine.logSignal.add { time, line -> onLogLine(time, line) }

        if ((needleListCtrl?.itemCount ?: 0) > 0) {
            needleListCtrl?.selectFirstItem()
        }

        onSelectNeedle()
        return true
    }

    fun addNeedle(name: String, needle: OmniFilterEngine.Needle): ScrollListItem? {
        val item = needleListCtrl?.addRow(
            mapOf(
                "enabled" to needle.enabled,
                "needle_name" to name
            )
        )
        // The checkbox in column 0 reflects enabled state; changes feed back via onNeedleCheckboxChanged.
        item?.setCheckboxCallback(NEEDLE_CHECK_COLUMN) { checked ->
            onNeedleCheckboxChanged(name, checked)
        }
        needleListCtrl?.refreshLineHeight()
        return item
    }

    // -----------------------------------------------------------------------
    // Selection & detail panel
    // -----------------------------------------------------------------------

    protected fun getSelectedNeedle(): OmniFilterEngine.Needle? {
        val selectedItem = needleListCtrl?.getFirstSelected() ?: run {
            hideDetails()
            return null
        }
        val needleName = selectedItem.getColumnValue(NEEDLE_NAME_COLUMN)?.takeIf { it.isNotEmpty() } ?: run {
            hideDetails()
            return null
        }
        return OmniFilterEngine.getNeedleList()[needleName]
    }

    private fun hideDetails() {
        panelDetails?.setVisible(false)
        removeNeedleBtn?.setEnabled(false)
    }

    fun onSelectNeedle() {
        val needle = getSelectedNeedle() ?: run { hideDetails(); return }

        if ((needleListCtrl?.itemCount ?: 0) == 0) {
            hideDetails()
            return
        }

        panelDetails?.setVisible(true)
        removeNeedleBtn?.setEnabled(true)

        needleNameCtrl?.setText(needleListCtrl?.getSelectedItemLabel(NEEDLE_NAME_COLUMN) ?: "")
        senderNameCtrl?.setText(needle.senderName)
        senderCaseSensitiveCheck?.setValue(!needle.senderNameCaseInsensitive)
        senderMatchTypeCombo?.selectByValue(needle.senderNameMatchType.ordinal)
        contentCtrl?.setText(needle.content)
        contentCaseSensitiveCheck?.setValue(!needle.contentCaseInsensitive)
        contentMatchTypeCombo?.selectByValue(needle.contentMatchType.ordinal)
        regionNameCtrl?.setText(needle.regionName)
        ownerCtrl?.clear()
        needle.ownerID?.let { ownerCtrl?.setText(it.toString()) }
        onOwnerChanged()

        typeNearbyBtn?.setToggleState(needle.types.contains(OmniFilterEngine.EType.NearbyChat))
        typeImBtn?.setToggleState(needle.types.contains(OmniFilterEngine.EType.InstantMessage))
        typeGroupImBtn?.setToggleState(needle.types.contains(OmniFilterEngine.EType.GroupChat))
        typeObjectChatBtn?.setToggleState(needle.types.contains(OmniFilterEngine.EType.ObjectChat))
        typeObjectImBtn?.setToggleState(needle.types.contains(OmniFilterEngine.EType.ObjectInstantMessage))
        typeScriptErrorBtn?.setToggleState(needle.types.contains(OmniFilterEngine.EType.ScriptError))
        typeDialogBtn?.setToggleState(needle.types.contains(OmniFilterEngine.EType.ScriptDialog))
        typeOfferBtn?.setToggleState(needle.types.contains(OmniFilterEngine.EType.InventoryOffer))
        typeInviteBtn?.setToggleState(needle.types.contains(OmniFilterEngine.EType.GroupInvite))
        typeLureBtn?.setToggleState(needle.types.contains(OmniFilterEngine.EType.Lure))
        typeLoadUrlBtn?.setToggleState(needle.types.contains(OmniFilterEngine.EType.URLRequest))
        typeFriendshipOfferBtn?.setToggleState(needle.types.contains(OmniFilterEngine.EType.FriendshipOffer))
        typeTeleportRequestBtn?.setToggleState(needle.types.contains(OmniFilterEngine.EType.TeleportRequest))
        typeGroupNoticeBtn?.setToggleState(needle.types.contains(OmniFilterEngine.EType.GroupNotice))

        chatReplaceCtrl?.setText(needle.chatReplace)
        buttonReplyCtrl?.setText(needle.buttonReply)
        textBoxReplyCtrl?.setText(needle.textBoxReply)

        contentCtrl?.requestFocus()
    }

    // -----------------------------------------------------------------------
    // Mutations
    // -----------------------------------------------------------------------

    fun onNeedleChanged() {
        val needle = getSelectedNeedle() ?: return

        needle.senderName = senderNameCtrl?.getValue() ?: ""
        needle.senderNameCaseInsensitive = !(senderCaseSensitiveCheck?.getValue() ?: true)
        needle.senderNameMatchType = OmniFilterEngine.EMatchType.values()[
            senderMatchTypeCombo?.getValue()?.toIntOrNull() ?: 0
        ]
        needle.content = contentCtrl?.getValue() ?: ""
        needle.contentCaseInsensitive = !(contentCaseSensitiveCheck?.getValue() ?: true)
        needle.contentMatchType = OmniFilterEngine.EMatchType.values()[
            contentMatchTypeCombo?.getValue()?.toIntOrNull() ?: 0
        ]
        needle.regionName = regionNameCtrl?.getValue() ?: ""
        val ownerStr = ownerCtrl?.getValue() ?: ""
        needle.ownerID = runCatching { UUID.fromString(ownerStr) }.getOrNull()

        needle.types.clear()
        if (typeNearbyBtn?.getValue() == true)         needle.types.add(OmniFilterEngine.EType.NearbyChat)
        if (typeImBtn?.getValue() == true)             needle.types.add(OmniFilterEngine.EType.InstantMessage)
        if (typeGroupImBtn?.getValue() == true)        needle.types.add(OmniFilterEngine.EType.GroupChat)
        if (typeObjectChatBtn?.getValue() == true)     needle.types.add(OmniFilterEngine.EType.ObjectChat)
        if (typeObjectImBtn?.getValue() == true)       needle.types.add(OmniFilterEngine.EType.ObjectInstantMessage)
        if (typeScriptErrorBtn?.getValue() == true)    needle.types.add(OmniFilterEngine.EType.ScriptError)
        if (typeDialogBtn?.getValue() == true)         needle.types.add(OmniFilterEngine.EType.ScriptDialog)
        if (typeOfferBtn?.getValue() == true)          needle.types.add(OmniFilterEngine.EType.InventoryOffer)
        if (typeInviteBtn?.getValue() == true)         needle.types.add(OmniFilterEngine.EType.GroupInvite)
        if (typeLureBtn?.getToggleState() == true)     needle.types.add(OmniFilterEngine.EType.Lure)
        if (typeLoadUrlBtn?.getValue() == true)        needle.types.add(OmniFilterEngine.EType.URLRequest)
        if (typeFriendshipOfferBtn?.getValue() == true) needle.types.add(OmniFilterEngine.EType.FriendshipOffer)
        if (typeTeleportRequestBtn?.getValue() == true) needle.types.add(OmniFilterEngine.EType.TeleportRequest)
        if (typeGroupNoticeBtn?.getValue() == true)    needle.types.add(OmniFilterEngine.EType.GroupNotice)

        needle.chatReplace = chatReplaceCtrl?.getValue() ?: ""
        needle.buttonReply = buttonReplyCtrl?.getValue() ?: ""
        needle.textBoxReply = textBoxReplyCtrl?.getValue() ?: ""

        OmniFilterEngine.setDirty(true)
    }

    fun onAddNeedleClicked() {
        val newNeedleName = getString("OmnifilterNewNeedle")
        if (needleListCtrl?.selectItemByLabel(newNeedleName, NEEDLE_NAME_COLUMN) != true) {
            val newNeedle = OmniFilterEngine.newNeedle(newNeedleName)
            addNeedle(newNeedleName, newNeedle)?.setSelected(true)
        }
        onSelectNeedle()
        needleNameCtrl?.requestFocus()
    }

    fun onRemoveNeedleClicked() {
        val index = needleListCtrl?.getItemIndex(needleListCtrl?.getFirstSelected()) ?: return
        OmniFilterEngine.deleteNeedle(needleListCtrl?.getSelectedItemLabel(NEEDLE_NAME_COLUMN) ?: return)

        needleListCtrl?.selectPrevItem()
        needleListCtrl?.deleteSingleItem(index)

        if ((needleListCtrl?.numSelected ?: 0) == 0) {
            needleListCtrl?.selectFirstItem()
        }

        onSelectNeedle()
    }

    fun onNeedleNameChanged() {
        val oldName = needleListCtrl?.getSelectedItemLabel(NEEDLE_NAME_COLUMN) ?: return
        val newName = needleNameCtrl?.getValue() ?: return

        if (oldName == newName) return

        val selectedItem = needleListCtrl?.getFirstSelected()
        val nameCheckItem = needleListCtrl?.getItemByLabel(newName, NEEDLE_NAME_COLUMN)
        if (nameCheckItem != null && nameCheckItem != selectedItem) {
            needleNameCtrl?.setValue(oldName)
            return
        }

        OmniFilterEngine.renameNeedle(oldName, newName)
        selectedItem?.setColumnValue(NEEDLE_NAME_COLUMN, newName)
        onSelectNeedle()
    }

    fun onNeedleCheckboxChanged(needleName: String, checked: Boolean) {
        val needle = OmniFilterEngine.getNeedleList()[needleName] ?: return
        needle.enabled = checked
        OmniFilterEngine.setDirty(true)
    }

    fun onOwnerChanged() {
        val ownerStr = ownerCtrl?.getValue() ?: ""
        val isValid = runCatching { UUID.fromString(ownerStr); true }.getOrDefault(ownerStr.isEmpty())
        // Signal invalid UUID to the UI so it can highlight the field.
        ownerCtrl?.setValidState(isValid)
    }

    fun onLogLine(time: Long, logLine: String) {
        val timeStr = formatSlt(time)
        val scrollAtEnd = filterLogCtrl?.isScrollAtEnd() ?: false

        val item = filterLogCtrl?.addRow(
            mapOf(
                "timestamp" to timeStr,
                "log_entry" to logLine
            )
        )

        if (scrollAtEnd) {
            filterLogCtrl?.scrollToEnd()
        }

        item?.setColumnTooltip(LOG_DATE_COLUMN, formatSltLong(time))
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun getString(key: String): String {
        System.err.println("OmniFilter: getString not yet implemented")
        return ""
    }

    private fun formatSlt(epochSeconds: Long): String {
        System.err.println("OmniFilter: formatSlt not yet implemented")
        return ""
    }

    private fun formatSltLong(epochSeconds: Long): String {
        System.err.println("OmniFilter: formatSltLong not yet implemented")
        return ""
    }

    // -----------------------------------------------------------------------
    // Minimal UI-stub interfaces (replaced by real UI framework)
    // -----------------------------------------------------------------------

    interface ScrollListItem {
        fun getColumnValue(column: Int): String?
        fun setColumnValue(column: Int, value: String)
        fun setSelected(selected: Boolean)
        fun setCheckboxCallback(column: Int, callback: (Boolean) -> Unit)
        fun setColumnTooltip(column: Int, tip: String)
    }

    interface ScrollListCtrl {
        val itemCount: Int
        val numSelected: Int
        fun addRow(data: Map<String, Any?>): ScrollListItem?
        fun deleteAllItems()
        fun deleteSingleItem(index: Int)
        fun getFirstSelected(): ScrollListItem?
        fun getSelectedItemLabel(column: Int): String?
        fun getItemByLabel(label: String, column: Int): ScrollListItem?
        fun getItemIndex(item: ScrollListItem?): Int?
        fun selectFirstItem()
        fun selectPrevItem()
        fun selectItemByLabel(label: String, column: Int): Boolean
        fun refreshLineHeight()
        fun isScrollAtEnd(): Boolean
        fun scrollToEnd()
        fun setCommitCallback(callback: () -> Unit)
        fun setSearchColumn(column: Int)
        fun setCommitOnSelectionChange(value: Boolean)
    }

    interface Button {
        fun getValue(): Boolean
        fun setEnabled(enabled: Boolean)
        fun setToggleState(state: Boolean)
        fun getToggleState(): Boolean
        fun setCommitCallback(callback: () -> Unit)
    }

    interface CheckBoxCtrl {
        fun getValue(): Boolean
        fun setValue(value: Boolean)
        fun setCommitCallback(callback: () -> Unit)
    }

    interface ComboBox {
        fun getValue(): String?
        fun selectByValue(value: Int)
        fun setCommitCallback(callback: () -> Unit)
    }

    interface LineEditor {
        fun getText(): String
        fun getText(column: Int): String
        fun getValue(): String?
        fun setValue(value: String)
        fun setText(text: String)
        fun clear()
        fun requestFocus()
        fun setValidState(valid: Boolean)
        fun setCommitCallback(callback: () -> Unit)
        fun setKeystrokeCallback(callback: () -> Unit)
    }

    interface TextEditor {
        fun getText(): String
        fun getValue(): String?
        fun setValue(value: String)
        fun setText(text: String)
        fun requestFocus()
        fun setCommitOnFocusLost(value: Boolean)
        fun setCommitCallback(callback: () -> Unit)
    }

    interface Panel {
        fun setVisible(visible: Boolean)
    }
}
