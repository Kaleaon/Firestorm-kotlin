package com.firestorm.newview

import java.util.UUID

// Stub UI types – replace with real Android/Swing/JavaFX equivalents when porting the UI layer
class UiButton(val id: String) { var enabled: Boolean = true }
class UiComboBox(val id: String) {
    var enabled: Boolean = true
    private val items: MutableList<Pair<String, Any?>> = mutableListOf()
    var currentIndex: Int = -1
    fun addItem(label: String, data: Any? = null) { items.add(label to data) }
    fun removeAll() { items.clear(); currentIndex = -1 }
    fun clear() { currentIndex = -1 }
    fun selectNthItem(n: Int) { currentIndex = n }
    fun selectedLabel(): String = items.getOrNull(currentIndex)?.first ?: ""
    fun currentUserdata(): Any? = items.getOrNull(currentIndex)?.second
    fun getAllItems(): List<Pair<String, Any?>> = items.toList()
    fun size(): Int = items.size
}
class UiCheckBox(val id: String) {
    var enabled: Boolean = true
    var checked: Boolean = false
}
class UiScrollList(val id: String) {
    var enabled: Boolean = true
    private val rows: MutableList<ScrollListRow> = mutableListOf()
    var currentBoldRow: ScrollListRow? = null
    fun addRow(label: String): ScrollListRow { val r = ScrollListRow(label); rows.add(r); return r }
    fun deleteAllItems() { rows.clear(); currentBoldRow = null }
    fun getAllData(): List<ScrollListRow> = rows.toList()
    fun getAllSelected(): List<ScrollListRow> = rows.filter { it.selected }
    fun getFirstSelectedIndex(): Int = rows.indexOfFirst { it.selected }
    fun getItemIndex(row: ScrollListRow): Int = rows.indexOf(row)
    fun swapWithPrevious(index: Int) { if (index > 0) { val tmp = rows[index]; rows[index] = rows[index - 1]; rows[index - 1] = tmp } }
    fun swapWithNext(index: Int) { if (index < rows.size - 1) { val tmp = rows[index]; rows[index] = rows[index + 1]; rows[index + 1] = tmp } }
    fun deleteSelected() { rows.removeAll { it.selected } }
    fun deselectAll() { rows.forEach { it.selected = false } }
    fun itemCount(): Int = rows.size
}
data class ScrollListRow(val label: String, var selected: Boolean = false, var bold: Boolean = false, var iconValue: String = "", var userData: Any? = null)
class UiSpinner(val id: String) { var enabled: Boolean = true; var value: Float = 0f }
class UiTextLabel(val id: String) { var enabled: Boolean = true }
class UiPanel(val id: String) { var visible: Boolean = true }
typealias LlsdMap = Map<String, Any>

// ─── FloaterAO ───────────────────────────────────────────────────────────────

class FloaterAO private constructor(private val key: Any) {

    companion object {
        fun create(key: Any): FloaterAO = FloaterAO(key)
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private var setList: MutableList<AOSet> = mutableListOf()
    private var selectedSet: AOSet? = null
    private var selectedState: AOSet.AOState? = null

    private var canDragAndDrop: Boolean = false
    private var importRunning: Boolean = false
    private var more: Boolean = true

    // ── Widget references (populated in postBuild) ────────────────────────────

    private lateinit var reloadCoverPanel: UiPanel
    private lateinit var mainInterfacePanel: UiPanel
    private lateinit var smallInterfacePanel: UiPanel

    private lateinit var setSelector: UiComboBox
    private lateinit var setSelectorSmall: UiComboBox
    private lateinit var activateSetButton: UiButton
    private lateinit var addButton: UiButton
    private lateinit var removeButton: UiButton
    private lateinit var defaultCheckBox: UiCheckBox
    private lateinit var overrideSitsCheckBox: UiCheckBox
    private lateinit var overrideSitsCheckBoxSmall: UiCheckBox
    private lateinit var smartCheckBox: UiCheckBox
    private lateinit var disableMouselookCheckBox: UiCheckBox

    private lateinit var stateSelector: UiComboBox
    private lateinit var animationList: UiScrollList
    private lateinit var moveUpButton: UiButton
    private lateinit var moveDownButton: UiButton
    private lateinit var trashButton: UiButton
    private lateinit var cycleCheckBox: UiCheckBox
    private lateinit var randomizeCheckBox: UiCheckBox
    private lateinit var cycleTimeTextLabel: UiTextLabel
    private lateinit var cycleTimeSpinner: UiSpinner

    private lateinit var reloadButton: UiButton
    private lateinit var previousButton: UiButton
    private lateinit var nextButton: UiButton
    private lateinit var lessButton: UiButton

    private lateinit var moreButton: UiButton
    private lateinit var previousButtonSmall: UiButton
    private lateinit var nextButtonSmall: UiButton

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun postBuild(): Boolean {
        reloadCoverPanel = UiPanel("ao_reload_cover")
        mainInterfacePanel = UiPanel("animation_overrider_panel")
        smallInterfacePanel = UiPanel("animation_overrider_panel_small")

        setSelector = UiComboBox("ao_set_selection_combo")
        setSelectorSmall = UiComboBox("ao_set_selection_combo_small")
        activateSetButton = UiButton("ao_activate")
        addButton = UiButton("ao_add")
        removeButton = UiButton("ao_remove")
        defaultCheckBox = UiCheckBox("ao_default")
        overrideSitsCheckBox = UiCheckBox("ao_sit_override")
        overrideSitsCheckBoxSmall = UiCheckBox("ao_sit_override_small")
        smartCheckBox = UiCheckBox("ao_smart")
        disableMouselookCheckBox = UiCheckBox("ao_disable_stands_in_mouselook")

        stateSelector = UiComboBox("ao_state_selection_combo")
        animationList = UiScrollList("ao_state_animation_list")
        moveUpButton = UiButton("ao_move_up")
        moveDownButton = UiButton("ao_move_down")
        trashButton = UiButton("ao_trash")
        cycleCheckBox = UiCheckBox("ao_cycle")
        randomizeCheckBox = UiCheckBox("ao_randomize")
        cycleTimeTextLabel = UiTextLabel("ao_cycle_time_seconds_label")
        cycleTimeSpinner = UiSpinner("ao_cycle_time")

        reloadButton = UiButton("ao_reload")
        previousButton = UiButton("ao_previous")
        nextButton = UiButton("ao_next")
        lessButton = UiButton("ao_less")

        moreButton = UiButton("ao_more")
        previousButtonSmall = UiButton("ao_previous_small")
        nextButtonSmall = UiButton("ao_next_small")

        AOEngine.setReloadCallback { updateList() }
        AOEngine.setAnimationChangedCallback { id -> onAnimationChanged(id) }

        onChangeAnimationSelection()
        mainInterfacePanel.visible = true
        smallInterfacePanel.visible = false
        reloading(true)
        updateList()

        if (getSavedBool("UseFullAOInterface")) onClickMore() else onClickLess()

        return true
    }

    fun onOpen(key: Any) {
        System.err.println("FloaterAO: onOpen not yet implemented")
    }

    fun onClose(appQuitting: Boolean) {
        if (!appQuitting) {
            System.err.println("FloaterAO: onClose not yet implemented")
        }
    }

    // ── Reload cover ──────────────────────────────────────────────────────────

    fun reloading(reload: Boolean) {
        reloadCoverPanel.visible = reload
        enableSetControls(!reload)
        enableStateControls(!reload)
        // timer start/stop for reload watchdog is platform-specific
    }

    fun tick(): Boolean {
        updateList()
        return false
    }

    // ── List / set population ─────────────────────────────────────────────────

    fun updateList() {
        reloadButton.enabled = true
        importRunning = false

        val sorted = AOEngine.getSetList().sortedBy { it.name }
        setList = sorted.toMutableList()

        val currentSetName = if (setSelector.selectedLabel().isEmpty()) AOEngine.getCurrentSetName()
        else setSelector.selectedLabel()

        setSelector.removeAll(); setSelector.clear()
        setSelectorSmall.removeAll(); setSelectorSmall.clear()
        animationList.deleteAllItems()
        reloading(false)

        if (setList.isEmpty()) {
            setSelector.addItem("(no sets loaded)")
            setSelectorSmall.addItem("(no sets loaded)")
            setSelector.selectNthItem(0)
            setSelectorSmall.selectNthItem(0)
            enableSetControls(false)
            return
        }

        val nameToUse = currentSetName.ifEmpty { setList[0].name }
        var selectedIndex = 0

        for ((index, set) in setList.withIndex()) {
            setSelector.addItem(set.name, set)
            setSelectorSmall.addItem(set.name, set)
            if (set.name == nameToUse) {
                selectedIndex = index
                selectedSet = AOEngine.selectSetByName(nameToUse)
                updateSetParameters()
                updateAnimationList()
            }
        }

        setSelector.selectNthItem(selectedIndex)
        setSelectorSmall.selectNthItem(selectedIndex)
        enableSetControls(true)

        if (setSelector.selectedLabel().isEmpty()) onClickReload()
    }

    fun updateSetParameters() {
        val set = selectedSet ?: return
        overrideSitsCheckBox.checked = set.sitOverride
        overrideSitsCheckBoxSmall.checked = set.sitOverride
        smartCheckBox.checked = set.smart
        disableMouselookCheckBox.checked = set.mouselookStandDisable
        defaultCheckBox.checked = (set === AOEngine.getDefaultSet())
        updateSmart()
    }

    fun updateAnimationList() {
        val currentStateSelected = stateSelector.currentIndex
        stateSelector.removeAll()
        onChangeAnimationSelection()

        val set = selectedSet
        if (set == null) {
            stateSelector.enabled = false
            stateSelector.addItem("(no animations loaded)")
            return
        }

        for (stateName in set.stateNames) {
            val state = set.getStateByName(stateName) ?: continue
            stateSelector.addItem(stateName, state)
        }

        enableStateControls(true)

        if (currentStateSelected == -1) stateSelector.selectNthItem(0)
        else stateSelector.selectNthItem(currentStateSelected)

        onSelectState()
    }

    fun updateScrollListData() {
        val state = selectedState ?: return
        val rows = animationList.getAllData()
        for ((index, anim) in state.animations.withIndex()) {
            rows.getOrNull(index)?.userData = anim.inventoryUUID
        }
    }

    // ── Set selector callbacks ────────────────────────────────────────────────

    fun onSelectSet() {
        val set = AOEngine.getSetByName(setSelector.selectedLabel())
        if (set == null) { onRenameSet(); return }
        if (selectedSet !== set) {
            selectedSet = set
            updateSetParameters()
            updateAnimationList()
        }
    }

    fun onSelectSetSmall() {
        setSelector.selectNthItem(setSelectorSmall.currentIndex)
        selectedSet = AOEngine.getSetByName(setSelectorSmall.selectedLabel())
        if (selectedSet != null) {
            updateSetParameters()
            updateAnimationList()
            onClickActivate()
        }
    }

    fun onRenameSet() {
        val set = selectedSet ?: return
        val name = setSelector.selectedLabel().trim()
        if (name.isNotEmpty() && !name.contains(':') && !name.contains('|') && name.all { it.code in 32..126 }) {
            if (AOEngine.renameSet(set, name)) { reloading(true); return }
        }
        setSelector.addItem(set.name)
    }

    fun onClickActivate() {
        setSelectorSmall.selectNthItem(setSelector.currentIndex)
        val set = selectedSet ?: return
        AOEngine.selectSet(set)
    }

    // ── State / animation callbacks ───────────────────────────────────────────

    fun onSelectState() {
        animationList.deleteAllItems()
        animationList.enabled = false
        onChangeAnimationSelection()

        val set = selectedSet ?: return
        val state = (stateSelector.currentUserdata() as? AOSet.AOState)
            ?: set.getStateByName(stateSelector.selectedLabel())
            ?: return
        selectedState = state

        if (state.animations.isNotEmpty()) {
            for (anim in state.animations) {
                val row = addAnimation(anim.name)
                row.userData = anim.inventoryUUID
                if (set.currentMotion == state.remapId && state.currentAnimationID == anim.assetUUID) {
                    animationList.currentBoldRow = row
                    row.bold = true
                    row.iconValue = "FSAO_Animation_Playing"
                }
            }
            animationList.enabled = true
        }

        cycleCheckBox.checked = state.cycle
        randomizeCheckBox.checked = state.random
        cycleTimeSpinner.value = state.cycleTime
        updateCycleParameters()
    }

    fun onChangeAnimationSelection() {
        val list = animationList.getAllSelected()
        val resortEnable = list.size == 1 && canDragAndDrop
        val trashEnable = list.isNotEmpty() && canDragAndDrop
        if (!canDragAndDrop) animationList.deselectAll()
        moveDownButton.enabled = resortEnable
        moveUpButton.enabled = resortEnable
        trashButton.enabled = trashEnable
    }

    private fun addAnimation(name: String): ScrollListRow = animationList.addRow(name)

    // ── Button handlers ───────────────────────────────────────────────────────

    fun onClickReload() {
        reloading(true)
        selectedSet = null
        selectedState = null
        AOEngine.reload(fromTimer = false)
        updateList()
    }

    fun onClickAdd() {
        showNewSetDialog { newSetName ->
            val name = newSetName.trim()
            if (name.isEmpty() || !name.all { it.code in 32..126 } || name.contains(':') || name.contains('|')) return@showNewSetDialog
            if (AOEngine.getSetByName(name) != null) return@showNewSetDialog
            AOEngine.addSet(name, { _ -> reloading(true) })
        }
    }

    fun onClickRemove() {
        val set = selectedSet ?: return
        showRemoveSetDialog(set.name) {
            if (AOEngine.removeSet(set)) {
                reloading(true)
                setSelector.removeAll(); setSelectorSmall.removeAll()
                setSelector.clear(); setSelectorSmall.clear()
                animationList.deleteAllItems()
            }
        }
    }

    fun onCheckDefault() {
        val set = if (defaultCheckBox.checked) selectedSet else null
        AOEngine.setDefaultSet(set)
    }

    fun onCheckOverrideSits() {
        overrideSitsCheckBoxSmall.checked = overrideSitsCheckBox.checked
        selectedSet?.let { AOEngine.setOverrideSits(it, overrideSitsCheckBox.checked) }
        updateSmart()
    }

    fun onCheckOverrideSitsSmall() {
        overrideSitsCheckBox.checked = overrideSitsCheckBoxSmall.checked
        onCheckOverrideSits()
    }

    fun updateSmart() {
        smartCheckBox.enabled = overrideSitsCheckBox.checked
    }

    fun onCheckSmart() {
        selectedSet?.let { AOEngine.setSmart(it, smartCheckBox.checked) }
    }

    fun onCheckDisableStands() {
        selectedSet?.let { AOEngine.setDisableMouselookStands(it, disableMouselookCheckBox.checked) }
    }

    fun onClickMoveUp() {
        val state = selectedState ?: return
        val index = animationList.getFirstSelectedIndex().takeIf { it >= 0 } ?: return
        if (animationList.getAllSelected().size != 1) return
        if (AOEngine.swapWithPrevious(state, index)) {
            animationList.swapWithPrevious(index)
            updateScrollListData()
        }
    }

    fun onClickMoveDown() {
        val state = selectedState ?: return
        if (animationList.getAllSelected().size != 1) return
        val index = animationList.getFirstSelectedIndex().takeIf { it >= 0 } ?: return
        if (index >= animationList.itemCount() - 1) return
        if (AOEngine.swapWithNext(state, index)) {
            animationList.swapWithNext(index)
            updateScrollListData()
        }
    }

    fun onClickTrash() {
        val state = selectedState ?: return
        val list = animationList.getAllSelected()
        if (list.isEmpty()) return
        for (row in list.reversed()) {
            AOEngine.removeAnimation(selectedSet ?: return, state, animationList.getItemIndex(row))
        }
        animationList.deleteSelected()
        animationList.currentBoldRow = null
    }

    fun updateCycleParameters() {
        val enabled = cycleCheckBox.checked
        randomizeCheckBox.enabled = enabled
        cycleTimeTextLabel.enabled = enabled
        cycleTimeSpinner.enabled = enabled
    }

    fun onCheckCycle() {
        val state = selectedState ?: return
        AOEngine.setCycle(state, cycleCheckBox.checked)
        updateCycleParameters()
    }

    fun onCheckRandomize() {
        val state = selectedState ?: return
        AOEngine.setRandomize(state, randomizeCheckBox.checked)
    }

    fun onChangeCycleTime() {
        val state = selectedState ?: return
        AOEngine.setCycleTime(state, cycleTimeSpinner.value)
    }

    fun onClickPrevious() = AOEngine.cycle(AOEngine.CycleMode.CyclePrevious, resetTimer = true)
    fun onClickNext() = AOEngine.cycle(AOEngine.CycleMode.CycleNext, resetTimer = true)

    fun onDoubleClick() {
        val row = animationList.getAllSelected().firstOrNull() ?: return
        val animUUID = row.userData as? UUID ?: return
        if (selectedState !== AOEngine.getCurrentState()) return
        if (AOEngine.getCurrentSet() !== selectedSet) {
            setSelectorSmall.selectNthItem(setSelector.currentIndex)
            AOEngine.selectSet(selectedSet ?: return)
        }
        AOEngine.playAnimation(animUUID)
    }

    fun onClickMore() {
        more = true
        smallInterfacePanel.visible = false
        mainInterfacePanel.visible = true
        setSavedBool("UseFullAOInterface", true)
        System.err.println("FloaterAO: onClickMore not yet implemented")
    }

    fun onClickLess() {
        more = false
        smallInterfacePanel.visible = true
        mainInterfacePanel.visible = false
        setSavedBool("UseFullAOInterface", false)
        System.err.println("FloaterAO: onClickLess not yet implemented")
    }

    // ── Animation-changed signal handler ──────────────────────────────────────

    fun onAnimationChanged(animation: UUID) {
        animationList.currentBoldRow?.let { old ->
            old.bold = false
            old.iconValue = "FSAO_Animation_Stopped"
        }
        animationList.currentBoldRow = null

        if (animation == UUID(0, 0)) return

        for (row in animationList.getAllData()) {
            val id = row.userData as? UUID ?: continue
            if (id == animation) {
                animationList.currentBoldRow = row
                row.bold = true
                row.iconValue = "FSAO_Animation_Playing"
                return
            }
        }
    }

    // ── Enable helpers ────────────────────────────────────────────────────────

    private fun enableSetControls(enable: Boolean) {
        setSelector.enabled = enable
        setSelectorSmall.enabled = enable
        activateSetButton.enabled = enable
        removeButton.enabled = enable
        defaultCheckBox.enabled = enable
        overrideSitsCheckBox.enabled = enable
        overrideSitsCheckBoxSmall.enabled = enable
        disableMouselookCheckBox.enabled = enable
        if (!enable) enableStateControls(false)
    }

    private fun enableStateControls(enable: Boolean) {
        stateSelector.enabled = enable
        animationList.enabled = enable
        cycleCheckBox.enabled = enable
        if (enable) updateCycleParameters()
        else {
            randomizeCheckBox.enabled = false
            cycleTimeTextLabel.enabled = false
            cycleTimeSpinner.enabled = false
        }
        listOf(previousButton, nextButton, previousButtonSmall, nextButtonSmall).forEach { it.enabled = enable }
        canDragAndDrop = enable
    }

    // ── Drag and drop ─────────────────────────────────────────────────────────

    fun handleDragAndDrop(x: Int, y: Int, drop: Boolean, cargoType: DragDropType, cargoData: Any?): DragAcceptance {
        if (!more) return DragAcceptance.NO
        return when (cargoType) {
            DragDropType.NOTECARD -> {
                if (importRunning) return DragAcceptance.NO
                if (cargoData != null && drop) {
                    if (AOEngine.importNotecard(cargoData)) {
                        reloading(true)
                        reloadButton.enabled = false
                        importRunning = true
                    }
                }
                DragAcceptance.YES_SINGLE
            }
            DragDropType.ANIMATION -> {
                if (!drop && (selectedSet == null || selectedState == null || !canDragAndDrop)) {
                    return DragAcceptance.NO
                }
                if (cargoData != null && drop) {
                    AOEngine.addAnimation(selectedSet ?: return DragAcceptance.NO, selectedState ?: return DragAcceptance.NO, cargoData)
                    addAnimation(cargoData.toString())
                    reloading(true)
                }
                DragAcceptance.YES_MULTI
            }
            else -> DragAcceptance.NO
        }
    }

    // ── Dialog stubs ──────────────────────────────────────────────────────────

    private fun showNewSetDialog(onConfirm: (String) -> Unit) {
        System.err.println("FloaterAO: showNewSetDialog not yet implemented")
    }

    private fun showRemoveSetDialog(setName: String, onConfirm: () -> Unit) {
        System.err.println("FloaterAO: showRemoveSetDialog not yet implemented")
    }

    private fun getSavedBool(key: String): Boolean {
        System.err.println("FloaterAO: getSavedBool not yet implemented")
        return false
    }

    private fun setSavedBool(key: String, value: Boolean) {
        System.err.println("FloaterAO: setSavedBool not yet implemented")
    }

    // ── Drag-and-drop support types ───────────────────────────────────────────

    enum class DragDropType { NOTECARD, ANIMATION, OTHER }
    enum class DragAcceptance { YES_SINGLE, YES_MULTI, NO }
}
