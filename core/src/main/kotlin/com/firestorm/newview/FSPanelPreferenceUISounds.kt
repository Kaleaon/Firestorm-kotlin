package com.firestorm.newview

import java.util.UUID

class FSPanelPreferenceUISounds : LLPanelPreference() {

    data class UISoundEntry(
        val soundSetting: String,
        val playmodeSetting: String,
        val labelControl: String,
        var displayLabel: String = "",
        val usesCombo: Boolean = false,
        val invertedBool: Boolean = false
    )

    private companion object {
        const val COL_LABEL  = 0
        const val COL_CHECK  = 1
        const val COL_STATUS = 2

        fun makeEntry(
            sound: String,
            labelOverride: String? = null,
            combo: Boolean = false,
            inverted: Boolean = false
        ): UISoundEntry {
            val playmode = "PlayMode$sound"
            // Label key: strip "UISnd" prefix and prepend "textFS", or use explicit override.
            val labelControl = labelOverride ?: ("textFS" + sound.removePrefix("UISnd"))
            return UISoundEntry(
                soundSetting    = sound,
                playmodeSetting = playmode,
                labelControl    = labelControl,
                usesCombo       = combo,
                invertedBool    = inverted
            )
        }
    }

    private var soundsList: LLScrollListCtrl? = null
    private var filter: LLFilterEditor? = null
    private var uuidEditor: LLLineEditor? = null
    private var playCheck: LLCheckBoxCtrl? = null
    private var playCombo: LLComboBox? = null
    private var selectedLabel: LLTextBox? = null
    private var settingName: LLTextBox? = null
    private val entries: MutableList<UISoundEntry> = mutableListOf()
    private var contextMenuHandle: LLHandle<LLContextMenu>? = null

    init {
        registerCommitCallback("Pref.SelectUISound")          { onSelectSound() }
        registerCommitCallback("Pref.UpdateUISoundFilter")    { onUpdateFilter() }
        registerCommitCallback("Pref.CommitUISoundUUID")      { onCommitUUID() }
        registerCommitCallback("Pref.CommitUISoundPlayCheck") { onCommitPlayCheck() }
        registerCommitCallback("Pref.CommitUISoundPlayCombo") { onCommitPlayCombo() }
        registerCommitCallback("Pref.PreviewSelectedUISound") { onPreviewSound() }
        registerCommitCallback("Pref.ResetSelectedUISound")   { onResetSound() }
    }

    override fun postBuild(): Boolean {
        soundsList    = getChild("ui_sounds_list")
        filter        = getChild("ui_sound_filter")
        uuidEditor    = getChild("ui_sound_uuid")
        playCheck     = getChild("ui_sound_play_checkbox")
        playCombo     = getChild("ui_sound_play_combo")
        selectedLabel = getChild("ui_sound_selected_label")
        settingName   = getChild("ui_sound_setting_name")

        soundsList?.setFilterColumn(COL_LABEL)
        soundsList?.setDoubleClickCallback { onDoubleClick() }
        soundsList?.setRightMouseDownCallback { ctrl, x, y, mask -> onRightClick(ctrl, x, y, mask) }

        val registrar = ScopedCommitRegistrar()
        registrar.add("UISounds.CopyUUID") { onCopyUUID() }

        val menu = LLContextMenu.create("ui_sounds_context_menu", visible = false).apply {
            addItem(label = "Copy sound UUID", name = "copy_uuid", onClick = "UISounds.CopyUUID")
        }
        contextMenuHandle = menu.getHandle()
        gMenuHolder.addChild(menu)

        applyLocalizedLabels()
        buildList()
        refreshEditor()

        return super.postBuild()
    }

    fun refreshList() {
        applyLocalizedLabels()
        buildList()
        refreshEditor()
    }

    private fun buildList() {
        val list = soundsList ?: return
        val prevSel = getSelectedIndex()

        if (entries.isEmpty()) {
            entries.addAll(listOf(
                makeEntry("UISndAlert"),
                makeEntry("UISndBadKeystroke"),
                makeEntry("UISndClick"),
                makeEntry("UISndClickRelease"),
                makeEntry("UISndHealthReductionF"),
                makeEntry("UISndHealthReductionM"),
                makeEntry("UISndMoneyChangeDown"),
                makeEntry("UISndMoneyChangeUp"),
                makeEntry("UISndNearbyChat"),
                makeEntry("UISndNewIncomingIMSession",       combo = true),
                makeEntry("UISndNewIncomingGroupIMSession",  combo = true),
                makeEntry("UISndNewIncomingConfIMSession",   combo = true),
                makeEntry("UISndStartIM"),
                makeEntry("UISndChatMention"),
                makeEntry("UISndObjectCreate"),
                makeEntry("UISndObjectDelete"),
                makeEntry("UISndObjectRezIn"),
                makeEntry("UISndObjectRezOut"),
                makeEntry("UISndSnapshot",                  inverted = true),
                makeEntry("UISndTeleportOut"),
                makeEntry("UISndPieMenuAppear"),
                makeEntry("UISndPieMenuHide"),
                makeEntry("UISndPieMenuSliceHighlight0"),
                makeEntry("UISndPieMenuSliceHighlight1"),
                makeEntry("UISndPieMenuSliceHighlight2"),
                makeEntry("UISndPieMenuSliceHighlight3"),
                makeEntry("UISndPieMenuSliceHighlight4"),
                makeEntry("UISndPieMenuSliceHighlight5"),
                makeEntry("UISndPieMenuSliceHighlight6"),
                makeEntry("UISndPieMenuSliceHighlight7"),
                makeEntry("UISndTyping"),
                makeEntry("UISndWindowClose"),
                makeEntry("UISndWindowOpen"),
                makeEntry("UISndScriptFloaterOpen"),
                makeEntry("UISndScriptFloaterClose"),
                makeEntry("UISndFriendOnline"),
                makeEntry("UISndFriendOffline"),
                makeEntry("UISndFriendshipOffer"),
                makeEntry("UISndTeleportOffer"),
                makeEntry("UISndInventoryOffer"),
                makeEntry("UISndIncomingVoiceCall"),
                makeEntry("UISndGroupInvitation"),
                makeEntry("UISndGroupNotice"),
                makeEntry("UISndQuestionExperience"),
                makeEntry("UISndInvalidOp"),
                makeEntry("UISndMovelockToggle"),
                makeEntry("UISndFootsteps"),
                makeEntry("UISndTrackerBeacon"),
                makeEntry("UISndMicToggle"),
                makeEntry("UISndRestart"),
                makeEntry("UISndRestartOpenSim"),
            ))
        }

        list.deleteAllItems()

        for ((i, entry) in entries.withIndex()) {
            // UISndRestartOpenSim is only relevant on OpenSim grids.
            if (entry.soundSetting == "UISndRestartOpenSim" && !LLGridManager.instance().isInOpenSim()) {
                continue
            }

            entry.displayLabel = resolveEntryLabel(entry)

            val row = LLSD.map().apply {
                put("id", i)
                val cols = LLSD.array()
                cols.add(LLSD.map().apply {
                    put("column", "ui_sound_label")
                    put("value",  entry.displayLabel)
                })
                if (entry.usesCombo) {
                    cols.add(LLSD.map().apply { put("column", "ui_sound_check");  put("value", "") })
                    cols.add(LLSD.map().apply { put("column", "ui_sound_status"); put("value", getModeLabel(entry)) })
                } else {
                    val raw = gSavedSettings.getBool(entry.playmodeSetting)
                    val isEnabled = if (entry.invertedBool) !raw else raw
                    cols.add(LLSD.map().apply {
                        put("type",   "checkbox")
                        put("column", "ui_sound_check")
                        put("value",  isEnabled)
                    })
                    cols.add(LLSD.map().apply { put("column", "ui_sound_status"); put("value", getModeLabel(entry)) })
                }
                put("columns", cols)
            }
            list.addElement(row)
        }

        if (prevSel >= 0) {
            for (row in list.getAllData()) {
                if (row?.getValue()?.asInteger() == prevSel) {
                    list.selectNthItem(list.getItemIndex(row))
                    break
                }
            }
        }
        if (list.getItemCount() > 0 && list.getFirstSelected() == null) {
            list.selectFirstItem()
        }
    }

    private fun refreshEditor() {
        val selLabel = selectedLabel ?: return
        val setName  = settingName   ?: return
        val uuidEd   = uuidEditor    ?: return
        val check    = playCheck     ?: return
        val combo    = playCombo     ?: return

        val entry   = getSelectedEntry()
        val hasSel  = entry != null

        uuidEd.setEnabled(hasSel)

        if (!hasSel) {
            selLabel.setValue(getPanelString("ui_sound_select_prompt"))
            setName.setValue("")
            uuidEd.setValue(LLSD())
            check.setVisible(false)
            combo.setVisible(false)
            return
        }

        selLabel.setValue(entry!!.displayLabel.ifEmpty { entry.soundSetting })
        setName.setValue(entry.soundSetting)
        uuidEd.setValue(gSavedSettings.getString(entry.soundSetting))
        gSavedSettings.getControl(entry.soundSetting)?.let { ctrl ->
            uuidEd.setToolTip(ctrl.getDefault().asString())
        }

        if (entry.usesCombo) {
            check.setVisible(false)
            combo.setVisible(true)
            combo.setValue(gSavedSettings.getUInt(entry.playmodeSetting).toInt())
        } else {
            combo.setVisible(false)
            check.setVisible(true)
            val raw = gSavedSettings.getBool(entry.playmodeSetting)
            check.setValue(if (entry.invertedBool) !raw else raw)
        }
    }

    private fun onSelectSound() {
        val list = soundsList ?: return
        val selected = list.getFirstSelected() ?: return
        val idx = selected.getValue().asInteger()
        if (idx >= 0 && idx < entries.size) {
            val entry = entries[idx]
            if (!entry.usesCombo) {
                val checked    = selected.getColumn(COL_CHECK).getValue().asBoolean()
                val raw        = gSavedSettings.getBool(entry.playmodeSetting)
                val wasEnabled = if (entry.invertedBool) !raw else raw
                if (checked != wasEnabled) {
                    gSavedSettings.setBool(entry.playmodeSetting, if (entry.invertedBool) !checked else checked)
                    selected.getColumn(COL_STATUS).setValue(getModeLabel(entry))
                }
            }
        }
        refreshEditor()
    }

    private fun onUpdateFilter() {
        val list = soundsList ?: return
        val f    = filter     ?: return
        list.setFilterString(f.getValue().asString())
    }

    private fun onCommitUUID() {
        val entry = getSelectedEntry() ?: return
        val ed    = uuidEditor         ?: return
        gSavedSettings.setString(entry.soundSetting, ed.getValue().asString())
    }

    private fun onCommitPlayCheck() {
        val entry = getSelectedEntry() ?: return
        if (entry.usesCombo) return
        val check = playCheck ?: return

        val checked = check.getValue().asBoolean()
        gSavedSettings.setBool(entry.playmodeSetting, if (entry.invertedBool) !checked else checked)

        val selected = soundsList?.getFirstSelected()
        selected?.getColumn(COL_CHECK)?.setValue(checked)
        selected?.getColumn(COL_STATUS)?.setValue(getModeLabel(entry))
    }

    private fun onCommitPlayCombo() {
        val entry = getSelectedEntry() ?: return
        if (!entry.usesCombo) return
        val combo = playCombo ?: return

        val mode = combo.getValue().asInteger().toUInt()
        gSavedSettings.setUInt(entry.playmodeSetting, mode)

        soundsList?.getFirstSelected()?.getColumn(COL_STATUS)?.setValue(getModeLabel(entry))
    }

    private fun onPreviewSound() {
        val entry = getSelectedEntry() ?: return
        makeUiSound(entry.soundSetting, forcePlay = true)
    }

    private fun onResetSound() {
        val entry = getSelectedEntry() ?: return
        gSavedSettings.getControl(entry.soundSetting)?.resetToDefault()
        gSavedSettings.getControl(entry.playmodeSetting)?.resetToDefault()

        val selected = soundsList?.getFirstSelected()
        if (selected != null) {
            if (entry.usesCombo) {
                selected.getColumn(COL_STATUS).setValue(getModeLabel(entry))
            } else {
                val raw = gSavedSettings.getBool(entry.playmodeSetting)
                val isEnabled = if (entry.invertedBool) !raw else raw
                selected.getColumn(COL_CHECK).setValue(isEnabled)
                selected.getColumn(COL_STATUS).setValue(getModeLabel(entry))
            }
        }
        refreshEditor()
    }

    private fun onDoubleClick() {
        onPreviewSound()
    }

    private fun onRightClick(ctrl: LLUICtrl, x: Int, y: Int, mask: Int) {
        soundsList?.selectItemAt(x, y, mask)
        refreshEditor()
        val entry = getSelectedEntry() ?: return
        val menu  = contextMenuHandle?.get() ?: return
        val (screenX, screenY) = soundsList!!.localPointToScreen(x, y)
        menu.show(screenX, screenY, soundsList)
    }

    private fun onCopyUUID() {
        val entry = getSelectedEntry() ?: return
        val uuid  = gSavedSettings.getString(entry.soundSetting)
        LLClipboard.instance().copyToClipboard(uuid)
    }

    private fun applyLocalizedLabels() {
        playCheck?.let { check ->
            val lbl = getPanelString("ui_sound_play_this")
            if (lbl.isNotEmpty()) check.setLabel(lbl)
        }

        contextMenuHandle?.get()?.let { menu ->
            val lbl = getPanelString("ui_sound_copy_uuid")
            if (lbl.isNotEmpty()) {
                menu.findChild<LLMenuItemGL>("copy_uuid")?.setLabel(lbl)
            }
        }

        playCombo?.let { combo ->
            val rows = listOf(
                "1" to "ui_sound_playmode_new_session",
                "2" to "ui_sound_playmode_every_message",
                "3" to "ui_sound_playmode_not_focus",
                "0" to "ui_sound_playmode_mute"
            )
            for ((value, key) in rows) {
                val item = combo.getItemByValue(value) ?: continue
                val s    = getPanelString(key)
                if (s.isNotEmpty()) item.getColumn(0).setValue(s)
            }
            combo.updateLabel()
        }
    }

    private fun resolveEntryLabel(entry: UISoundEntry): String {
        var label = getPanelString(entry.labelControl)
        if (label.isEmpty()) label = entry.soundSetting
        label = label.trim()
        if (label.endsWith(':')) label = label.dropLast(1)
        return label
    }

    private fun getModeLabel(entry: UISoundEntry): String {
        return if (entry.usesCombo) {
            when (gSavedSettings.getUInt(entry.playmodeSetting).toInt()) {
                1    -> getPanelString("ui_sound_playmode_new_session")
                2    -> getPanelString("ui_sound_playmode_every_message")
                3    -> getPanelString("ui_sound_playmode_not_focus")
                else -> getPanelString("ui_sound_playmode_mute")
            }
        } else {
            val enabled = gSavedSettings.getBool(entry.playmodeSetting)
            if (entry.invertedBool) {
                if (enabled) getPanelString("ui_sound_playmode_mute") else getPanelString("ui_sound_play_this")
            } else {
                if (enabled) getPanelString("ui_sound_play_this") else getPanelString("ui_sound_playmode_mute")
            }
        }
    }

    private fun getSelectedIndex(): Int {
        return soundsList?.getFirstSelected()?.getValue()?.asInteger() ?: -1
    }

    private fun getSelectedEntry(): UISoundEntry? {
        val idx = getSelectedIndex()
        if (idx < 0 || idx >= entries.size) return null
        return entries[idx]
    }

    // Walk up the panel hierarchy to find a localized string, mirroring the C++ parent-walk.
    private fun getPanelString(name: String): String {
        if (hasString(name)) return getString(name)
        var ancestor = getParent()
        while (ancestor != null) {
            if (ancestor is LLPanel && ancestor.hasString(name)) return ancestor.getString(name)
            ancestor = ancestor.getParent()
        }
        return ""
    }
}
