package com.firestorm.newview

import com.firestorm.llcharacter.GestureStepAnimation
import com.firestorm.llcharacter.GestureStepChat
import com.firestorm.llcharacter.GestureStepSound
import com.firestorm.llcharacter.GestureStepWait
import com.firestorm.llcharacter.MultiGesture
import com.firestorm.llcharacter.StepType
import com.firestorm.llcharacter.WaitFlags
import com.firestorm.llcharacter.ANIM_FLAG_STOP
import com.firestorm.llcommon.LLUUID
import com.firestorm.llui.Button
import com.firestorm.llui.CheckBoxCtrl
import com.firestorm.llui.ComboBox
import com.firestorm.llui.FloaterReg
import com.firestorm.llui.LLSD
import com.firestorm.llui.LineEditor
import com.firestorm.llui.Preview
import com.firestorm.llui.RadioGroup
import com.firestorm.llui.ScrollListCtrl
import com.firestorm.llui.ScrollListItem
import com.firestorm.llui.TextBox

open class PreviewGesture(key: LLSD) : Preview(key) {

    private var triggerEditor: LineEditor? = null
    private var replaceText: TextBox? = null
    private var replaceEditor: LineEditor? = null
    private var modifierCombo: ComboBox? = null
    private var keyCombo: ComboBox? = null

    private var libraryList: ScrollListCtrl? = null
    private var addBtn: Button? = null
    private var upBtn: Button? = null
    private var downBtn: Button? = null
    private var deleteBtn: Button? = null
    private var stepList: ScrollListCtrl? = null

    private var optionsText: TextBox? = null
    private var animationRadio: RadioGroup? = null
    private var animationCombo: ComboBox? = null
    private var soundCombo: ComboBox? = null
    private var chatEditor: LineEditor? = null
    private var waitKeyReleaseCheck: CheckBoxCtrl? = null
    private var waitAnimCheck: CheckBoxCtrl? = null
    private var waitTimeCheck: CheckBoxCtrl? = null
    private var waitTimeEditor: LineEditor? = null

    private var activeCheck: CheckBoxCtrl? = null
    private var saveBtn: Button? = null
    private var previewBtn: Button? = null

    private var previewGesture: MultiGesture? = null
    private var dirty: Boolean = false

    override fun draw() {
        System.err.println("PreviewGesture: draw not yet implemented")
    }

    override fun postBuild(): Boolean {
        setVisibleCallback { newVisibility -> if (newVisibility.asBoolean()) refresh() }

        triggerEditor = getChild<LineEditor>("trigger_editor").also { ed ->
            ed.setKeystrokeCallback { onKeystrokeCommit(ed) }
            ed.setCommitCallback { onCommitSetDirty() }
            ed.setCommitOnFocusLost(true)
            ed.setIgnoreTab(true)
        }

        replaceText = getChild<TextBox>("replace_text").also { it.setEnabled(false) }

        replaceEditor = getChild<LineEditor>("replace_editor").also { ed ->
            ed.setEnabled(false)
            ed.setKeystrokeCallback { onKeystrokeCommit(ed) }
            ed.setCommitCallback { onCommitSetDirty() }
            ed.setCommitOnFocusLost(true)
            ed.setIgnoreTab(true)
        }

        modifierCombo = getChild<ComboBox>("modifier_combo").also {
            it.setCommitCallback { onCommitKeyOrModifier() }
        }

        keyCombo = getChild<ComboBox>("key_combo").also {
            it.setCommitCallback { onCommitKeyOrModifier() }
        }

        libraryList = getChild<ScrollListCtrl>("library_list").also { list ->
            list.setCommitCallback { onCommitLibrary() }
            list.setDoubleClickCallback { onClickAdd() }
        }

        addBtn = getChild<Button>("add_btn").also { btn ->
            btn.setClickedCallback { onClickAdd() }
            btn.setEnabled(false)
        }

        upBtn = getChild<Button>("up_btn").also { btn ->
            btn.setClickedCallback { onClickUp() }
            btn.setEnabled(false)
        }

        downBtn = getChild<Button>("down_btn").also { btn ->
            btn.setClickedCallback { onClickDown() }
            btn.setEnabled(false)
        }

        deleteBtn = getChild<Button>("delete_btn").also { btn ->
            btn.setClickedCallback { onClickDelete() }
            btn.setEnabled(false)
        }

        stepList = getChild<ScrollListCtrl>("step_list").also {
            it.setCommitCallback { onCommitStep() }
        }

        optionsText = getChild<TextBox>("options_text")

        animationCombo = getChild<ComboBox>("animation_list").also {
            it.setVisible(false)
            it.setCommitCallback { onCommitAnimation() }
        }

        animationRadio = getChild<RadioGroup>("animation_trigger_type").also {
            it.setVisible(false)
            it.setCommitCallback { onCommitAnimationTrigger() }
        }

        soundCombo = getChild<ComboBox>("sound_list").also {
            it.setVisible(false)
            it.setCommitCallback { onCommitSound() }
        }

        chatEditor = getChild<LineEditor>("chat_editor").also { ed ->
            ed.setVisible(false)
            ed.setCommitCallback { onCommitChat() }
            ed.setCommitOnFocusLost(true)
            ed.setIgnoreTab(true)
        }

        waitKeyReleaseCheck = getChild<CheckBoxCtrl>("wait_key_release_check").also {
            it.setVisible(false)
            it.setCommitCallback { onCommitWait() }
        }

        waitAnimCheck = getChild<CheckBoxCtrl>("wait_anim_check").also {
            it.setVisible(false)
            it.setCommitCallback { onCommitWait() }
        }

        waitTimeCheck = getChild<CheckBoxCtrl>("wait_time_check").also {
            it.setVisible(false)
            it.setCommitCallback { onCommitWait() }
        }

        waitTimeEditor = getChild<LineEditor>("wait_time_editor").also { ed ->
            ed.setEnabled(false)
            ed.setVisible(false)
            ed.setCommitOnFocusLost(true)
            ed.setCommitCallback { onCommitWaitTime() }
            ed.setIgnoreTab(true)
        }

        activeCheck = getChild<CheckBoxCtrl>("active_check").also {
            it.setCommitCallback { onCommitActive() }
        }

        saveBtn = getChild<Button>("save_btn").also {
            it.setClickedCallback { onClickSave() }
        }

        previewBtn = getChild<Button>("preview_btn").also {
            it.setClickedCallback { onClickPreview() }
        }

        addModifiers()
        addKeys()
        addAnimations()
        addSounds()

        val item = getItem()
        if (item != null) {
            getChild<LineEditor>("desc").setValue(item.description)
        }

        return super.postBuild()
    }

    open fun canClose(): Boolean {
        if (!dirty || forceClose) return true
        if (!saveDialogShown) {
            saveDialogShown = true
            System.err.println("PreviewGesture: show SaveChanges notification dialog not yet implemented")
        }
        return false
    }

    open fun onClose(appQuitting: Boolean) {
        GestureMgr.stopGesture(previewGesture)
    }

    open fun onUpdateSucceeded() {
        refresh()
    }

    open fun refresh() {
        super.refresh()
        val item = getItem()
        val isComplete = item?.isFinished() ?: false

        if (previewGesture != null || !isComplete) {
            setAllControlsEnabled(false)
            previewBtn?.setEnabled(areGesturesEnabled())
            return
        }

        val modifiable = item!!.permissions.allowModifyBy(agentId())

        triggerEditor?.setEnabled(true)
        libraryList?.setEnabled(modifiable)
        stepList?.setEnabled(modifiable)
        optionsText?.setEnabled(modifiable)
        animationCombo?.setEnabled(modifiable)
        animationRadio?.setEnabled(modifiable)
        soundCombo?.setEnabled(modifiable)
        chatEditor?.setEnabled(modifiable)
        waitKeyReleaseCheck?.setEnabled(modifiable)
        waitAnimCheck?.setEnabled(modifiable)
        waitTimeCheck?.setEnabled(modifiable)
        waitTimeEditor?.setEnabled(modifiable)
        activeCheck?.setEnabled(true)

        val trigger = triggerEditor?.getText() ?: ""
        val replace = replaceEditor?.getText() ?: ""
        val haveTrigger = trigger.isNotEmpty()
        val haveReplace = replace.isNotEmpty()
        replaceText?.setEnabled(haveTrigger || haveReplace)
        replaceEditor?.setEnabled(haveTrigger || haveReplace)

        modifierCombo?.setEnabled(true)
        keyCombo?.setEnabled(true)

        val libraryItem = libraryList?.getFirstSelected()
        val stepItem = stepList?.getFirstSelected()
        val stepIndex = stepList?.getFirstSelectedIndex() ?: -1
        val stepCount = stepList?.getItemCount() ?: 0

        addBtn?.setEnabled(modifiable && libraryItem != null)
        upBtn?.setEnabled(modifiable && stepItem != null && stepIndex > 0)
        downBtn?.setEnabled(modifiable && stepItem != null && stepIndex < stepCount - 1)
        deleteBtn?.setEnabled(modifiable && stepItem != null)

        animationCombo?.setVisible(false)
        animationRadio?.setVisible(false)
        soundCombo?.setVisible(false)
        chatEditor?.setVisible(false)
        waitKeyReleaseCheck?.setVisible(false)
        waitAnimCheck?.setVisible(false)
        waitTimeCheck?.setVisible(false)
        waitTimeEditor?.setVisible(false)

        var optionsText = ""

        if (stepItem != null) {
            val step = stepItem.getUserdata() as? com.firestorm.llcharacter.GestureStep
            if (step != null) {
                when (step.type) {
                    StepType.ANIMATION -> {
                        val animStep = step as GestureStepAnimation
                        optionsText = getString("step_anim")
                        animationCombo?.setVisible(true)
                        animationRadio?.setVisible(true)
                        animationRadio?.setSelectedIndex(if (animStep.flags and ANIM_FLAG_STOP != 0u) 1 else 0)
                        animationCombo?.setCurrentByID(animStep.animAssetId)
                    }
                    StepType.SOUND -> {
                        val soundStep = step as GestureStepSound
                        optionsText = getString("step_sound")
                        soundCombo?.setVisible(true)
                        soundCombo?.setCurrentByID(soundStep.soundAssetId)
                    }
                    StepType.CHAT -> {
                        val chatStep = step as GestureStepChat
                        optionsText = getString("step_chat")
                        chatEditor?.setVisible(true)
                        chatEditor?.setText(chatStep.chatText)
                    }
                    StepType.WAIT -> {
                        val waitStep = step as GestureStepWait
                        optionsText = getString("step_wait")
                        waitKeyReleaseCheck?.setVisible(true)
                        waitKeyReleaseCheck?.set((waitStep.flags and WaitFlags.KEY_RELEASE) != 0u)
                        waitAnimCheck?.setVisible(true)
                        waitAnimCheck?.set((waitStep.flags and WaitFlags.ALL_ANIM) != 0u)
                        waitTimeCheck?.setVisible(true)
                        waitTimeCheck?.set((waitStep.flags and WaitFlags.TIME) != 0u)
                        waitTimeEditor?.setVisible(true)
                        waitTimeEditor?.setText("%.1f".format(waitStep.waitSeconds))
                    }
                    else -> {}
                }
            }
        }

        this.optionsText?.setText(optionsText)

        val active = GestureMgr.isGestureActive(itemUuid)
        activeCheck?.set(active)

        previewBtn?.setEnabled(stepCount > 0 && areGesturesEnabled())
        saveBtn?.setEnabled(dirty)
        addAnimations()
        addSounds()
    }

    private fun addModifiers() {
        val combo = modifierCombo ?: return
        combo.add(noneLabel(), ADD_BOTTOM)
        combo.add(shiftLabel(), ADD_BOTTOM)
        combo.add(ctrlLabel(), ADD_BOTTOM)
        combo.setCurrentByIndex(0)
    }

    private fun addKeys() {
        val combo = keyCombo ?: return
        combo.add(noneLabel())
        for (key in ' '.code..'~'.code) {
            val keyStr = stringFromKey(key.toByte())
            combo.add(keyStr, ADD_BOTTOM)
        }
        System.err.println("PreviewGesture: add non-printable key names not yet implemented")
        combo.setCurrentByIndex(0)
    }

    private fun addAnimations() {
        val combo = animationCombo ?: return
        val oldValue = combo.getCurrentID()
        combo.removeAll()
        combo.add(getString("none_text"), LLUUID.NULL)
        System.err.println("PreviewGesture: addAnimations not yet implemented")
        combo.setCurrentByID(oldValue)
    }

    private fun addSounds() {
        val combo = soundCombo ?: return
        combo.removeAll()
        combo.add(getString("none_text"), LLUUID.NULL)
        System.err.println("PreviewGesture: addSounds not yet implemented")
    }

    private fun initDefaultGesture() {
        var item = addStep(StepType.ANIMATION)
        (item?.getUserdata() as? GestureStepAnimation)?.let { anim ->
            anim.animAssetId = LLUUID.NULL
            System.err.println("PreviewGesture: initDefaultGesture anim setup not yet implemented")
            updateLabel(item)
        }

        item = addStep(StepType.WAIT)
        (item?.getUserdata() as? GestureStepWait)?.let { wait ->
            wait.flags = WaitFlags.ALL_ANIM
            updateLabel(item)
        }

        item = addStep(StepType.CHAT)
        (item?.getUserdata() as? GestureStepChat)?.let { chat ->
            System.err.println("PreviewGesture: initDefaultGesture chat setup not yet implemented")
            updateLabel(item)
        }

        stepList?.selectFirstItem()
        dirty = true
    }

    private fun loadAsset() {
        val item = getItem() ?: return

        val assetId = item.assetUUID
        if (assetId == LLUUID.NULL) {
            initDefaultGesture()
            refresh()
            setAssetStatus(PREVIEW_ASSET_LOADED)
            return
        }

        System.err.println("PreviewGesture: loadAsset not yet implemented")
    }

    private fun loadUIFromGesture(gesture: MultiGesture) {
        triggerEditor?.setText(gesture.trigger)
        replaceEditor?.setText(gesture.replaceText)

        System.err.println("PreviewGesture: loadUIFromGesture modifierCombo not yet implemented")
        System.err.println("PreviewGesture: loadUIFromGesture keyCombo not yet implemented")

        val stepListCtrl = stepList ?: return
        for (step in gesture.steps) {
            val newStep: com.firestorm.llcharacter.GestureStep = when (step.type) {
                StepType.ANIMATION -> GestureStepAnimation(
                    (step as GestureStepAnimation).animName,
                    step.animAssetId,
                    step.flags
                )
                StepType.SOUND -> GestureStepSound(
                    (step as GestureStepSound).soundName,
                    step.soundAssetId,
                    step.flags
                )
                StepType.CHAT -> GestureStepChat(
                    (step as GestureStepChat).chatText,
                    step.flags
                )
                StepType.WAIT -> GestureStepWait(
                    (step as GestureStepWait).waitSeconds,
                    step.flags
                )
                else -> continue
            }

            val row = LLSD()
            row["columns"][0]["value"] = getStepLabel(newStep.getLabel())
            row["columns"][0]["font"] = "SANSSERIF_SMALL"
            val listItem = stepListCtrl.addElement(row)
            listItem?.setUserdata(newStep)
        }
    }

    private fun saveIfNeeded() {
        if (!dirty) return

        val gesture = createGesture()
        val maxSize = gesture.getMaxSerialSize()

        System.err.println("PreviewGesture: saveIfNeeded not yet implemented")

        dirty = false
    }

    private fun createGesture(): MultiGesture {
        val gesture = MultiGesture()
        gesture.trigger = triggerEditor?.getText() ?: ""
        gesture.replaceText = replaceEditor?.getText() ?: ""

        System.err.println("PreviewGesture: createGesture mask not yet implemented")
        System.err.println("PreviewGesture: createGesture key not yet implemented")

        val dataList = stepList?.getAllData() ?: emptyList<ScrollListItem>()
        for (item in dataList) {
            val step = item.getUserdata() as? com.firestorm.llcharacter.GestureStep ?: continue
            when (step.type) {
                StepType.ANIMATION -> gesture.steps.add(
                    GestureStepAnimation(
                        (step as GestureStepAnimation).animName,
                        step.animAssetId,
                        step.flags
                    )
                )
                StepType.SOUND -> gesture.steps.add(
                    GestureStepSound(
                        (step as GestureStepSound).soundName,
                        step.soundAssetId,
                        step.flags
                    )
                )
                StepType.CHAT -> gesture.steps.add(
                    GestureStepChat(
                        (step as GestureStepChat).chatText,
                        step.flags
                    )
                )
                StepType.WAIT -> gesture.steps.add(
                    GestureStepWait(
                        (step as GestureStepWait).waitSeconds,
                        step.flags
                    )
                )
                else -> {}
            }
        }

        return gesture
    }

    private fun addStep(stepType: StepType): ScrollListItem? {
        val step: com.firestorm.llcharacter.GestureStep = when (stepType) {
            StepType.ANIMATION -> GestureStepAnimation()
            StepType.SOUND -> GestureStepSound()
            StepType.CHAT -> GestureStepChat()
            StepType.WAIT -> GestureStepWait()
            else -> return null
        }

        val row = LLSD()
        row["columns"][0]["value"] = getStepLabel(step.getLabel())
        row["columns"][0]["font"] = "SANSSERIF_SMALL"
        val stepItem = stepList?.addElement(row) ?: return null
        stepItem.setUserdata(step)

        libraryList?.deselectAllItems()
        stepList?.deselectAllItems()
        stepItem.setSelected(true)

        return stepItem
    }

    private fun onKeystrokeCommit(caller: LineEditor) {
        onCommitSetDirty()
    }

    private fun onCommitSetDirty() {
        dirty = true
        refresh()
    }

    private fun onCommitLibrary() {
        if (libraryList?.getFirstSelected() != null) {
            stepList?.deselectAllItems()
            refresh()
        }
    }

    private fun onCommitStep() {
        if (stepList?.getFirstSelected() == null) return
        libraryList?.deselectAllItems()
        refresh()
    }

    private fun onCommitAnimation() {
        val stepItem = stepList?.getFirstSelected() ?: return
        val step = stepItem.getUserdata() as? GestureStepAnimation ?: return

        if ((animationCombo?.getCurrentIndex() ?: 0) <= 0) {
            step.animName = ""
            step.animAssetId = LLUUID.NULL
        } else {
            step.animName = animationCombo?.getSimple() ?: ""
            step.animAssetId = animationCombo?.getCurrentID() ?: LLUUID.NULL
        }

        updateLabel(stepItem)
        dirty = true
        refresh()
    }

    private fun onCommitAnimationTrigger() {
        val stepItem = stepList?.getFirstSelected() ?: return
        val step = stepItem.getUserdata() as? GestureStepAnimation ?: return

        if ((animationRadio?.getSelectedIndex() ?: 0) == 0) {
            step.flags = step.flags and ANIM_FLAG_STOP.inv()
        } else {
            step.flags = step.flags or ANIM_FLAG_STOP
        }

        updateLabel(stepItem)
        dirty = true
        refresh()
    }

    private fun onCommitSound() {
        val stepItem = stepList?.getFirstSelected() ?: return
        val step = stepItem.getUserdata() as? GestureStepSound ?: return

        step.soundName = soundCombo?.getSimple() ?: ""
        step.soundAssetId = soundCombo?.getCurrentID() ?: LLUUID.NULL
        step.flags = 0u

        updateLabel(stepItem)
        dirty = true
        refresh()
    }

    private fun onCommitChat() {
        val stepItem = stepList?.getFirstSelected() ?: return
        val step = stepItem.getUserdata() as? GestureStepChat ?: return

        step.chatText = chatEditor?.getText() ?: ""
        step.flags = 0u

        updateLabel(stepItem)
        dirty = true
        refresh()
    }

    private fun onCommitWait() {
        val stepItem = stepList?.getFirstSelected() ?: return
        val step = stepItem.getUserdata() as? GestureStepWait ?: return

        var flags: UInt = 0u
        if (waitKeyReleaseCheck?.get() == true) flags = flags or WaitFlags.KEY_RELEASE
        if (waitAnimCheck?.get() == true) flags = flags or WaitFlags.ALL_ANIM
        if (waitTimeCheck?.get() == true) flags = flags or WaitFlags.TIME
        step.flags = flags

        val waitSecondsText = waitTimeEditor?.getText() ?: "0"
        var waitSeconds = waitSecondsText.toFloatOrNull() ?: 0f
        waitSeconds = waitSeconds.coerceIn(0f, 3600f)
        step.waitSeconds = waitSeconds

        waitTimeEditor?.setEnabled(waitTimeCheck?.get() ?: false)
        updateLabel(stepItem)
        dirty = true
        refresh()
    }

    private fun onCommitWaitTime() {
        waitTimeCheck?.set(true)
        onCommitWait()
    }

    private fun onCommitKeyOrModifier() {
        dirty = true
        refresh()
    }

    private fun onCommitActive() {
        if (!GestureMgr.isGestureActive(itemUuid)) {
            GestureMgr.activateGesture(itemUuid)
        } else {
            GestureMgr.deactivateGesture(itemUuid)
        }
        System.err.println("PreviewGesture: onCommitActive inventory update not yet implemented")
        refresh()
    }

    private fun onClickSave() {
        saveIfNeeded()
    }

    private fun onClickPreview() {
        if (!areGesturesEnabled()) return

        if (previewGesture == null) {
            previewGesture = createGesture()
            previewGesture!!.doneCallback = { onDonePreview() }
            previewBtn?.setLabel(getString("stop_txt"))
            GestureMgr.playGesture(previewGesture!!)
            refresh()
        } else {
            GestureMgr.stopGesture(previewGesture)
            refresh()
        }
    }

    private fun onDonePreview() {
        previewBtn?.setLabel(getString("preview_txt"))
        previewGesture = null
        refresh()
    }

    private fun onClickAdd() {
        val libraryItem = libraryList?.getFirstSelected() ?: return
        val index = libraryList?.getFirstSelectedIndex() ?: return

        if (index >= StepType.entries.size - 1) return

        addStep(StepType.entries[index])
        dirty = true
        refresh()
    }

    private fun onClickUp() {
        val selectedIndex = stepList?.getFirstSelectedIndex() ?: return
        if (selectedIndex > 0) {
            stepList?.swapWithPrevious(selectedIndex)
            dirty = true
            refresh()
        }
    }

    private fun onClickDown() {
        val selectedIndex = stepList?.getFirstSelectedIndex() ?: return
        val count = stepList?.getItemCount() ?: return
        if (selectedIndex < count - 1) {
            stepList?.swapWithNext(selectedIndex)
            dirty = true
            refresh()
        }
    }

    private fun onClickDelete() {
        val item = stepList?.getFirstSelected() ?: return
        val selectedIndex = stepList?.getFirstSelectedIndex() ?: return

        stepList?.deleteSingleItem(selectedIndex)
        dirty = true
        refresh()
    }

    private fun handleSaveChangesDialog(option: Int): Boolean {
        saveDialogShown = false
        return when (option) {
            0 -> {
                GestureMgr.stopGesture(previewGesture)
                closeAfterSave = true
                onClickSave()
                false
            }
            1 -> {
                GestureMgr.stopGesture(previewGesture)
                dirty = false
                closeFloater()
                false
            }
            else -> {
                System.err.println("PreviewGesture: abortQuit not yet implemented")
                false
            }
        }
    }

    private fun setAllControlsEnabled(enabled: Boolean) {
        triggerEditor?.setEnabled(enabled)
        replaceText?.setEnabled(enabled)
        replaceEditor?.setEnabled(enabled)
        modifierCombo?.setEnabled(enabled)
        keyCombo?.setEnabled(enabled)
        libraryList?.setEnabled(enabled)
        addBtn?.setEnabled(enabled)
        upBtn?.setEnabled(enabled)
        downBtn?.setEnabled(enabled)
        deleteBtn?.setEnabled(enabled)
        stepList?.setEnabled(enabled)
        optionsText?.setEnabled(enabled)
        animationCombo?.setEnabled(enabled)
        animationRadio?.setEnabled(enabled)
        soundCombo?.setEnabled(enabled)
        chatEditor?.setEnabled(enabled)
        waitKeyReleaseCheck?.setEnabled(enabled)
        waitAnimCheck?.setEnabled(enabled)
        waitTimeCheck?.setEnabled(enabled)
        waitTimeEditor?.setEnabled(enabled)
        activeCheck?.setEnabled(enabled)
        saveBtn?.setEnabled(enabled)
    }

    private fun areGesturesEnabled(): Boolean {
        System.err.println("PreviewGesture: areGesturesEnabled not yet implemented")
        return true
    }

    private fun agentId(): LLUUID {
        System.err.println("PreviewGesture: agentId not yet implemented")
        return LLUUID.NULL
    }

    private fun noneLabel(): String {
        System.err.println("PreviewGesture: noneLabel not yet implemented")
        return "---"
    }

    private fun shiftLabel(): String {
        System.err.println("PreviewGesture: shiftLabel not yet implemented")
        return "Shift"
    }

    private fun ctrlLabel(): String {
        System.err.println("PreviewGesture: ctrlLabel not yet implemented")
        return "Ctrl"
    }

    private fun stringFromKey(key: Byte): String {
        System.err.println("PreviewGesture: stringFromKey not yet implemented")
        return key.toChar().toString()
    }

    private fun getString(key: String): String {
        System.err.println("PreviewGesture: getString not yet implemented")
        return key
    }

    private fun getItem(): InventoryItemRef? {
        System.err.println("PreviewGesture: getItem not yet implemented")
        return null
    }

    private fun setAssetStatus(status: Int) {
        System.err.println("PreviewGesture: setAssetStatus not yet implemented")
    }

    private fun closeFloater() {
        System.err.println("PreviewGesture: closeFloater not yet implemented")
    }

    companion object {
        private const val ADD_BOTTOM = 1
        private const val PREVIEW_ASSET_LOADED = 1
        private const val PREVIEW_ASSET_LOADING = 2
        private const val PREVIEW_ASSET_ERROR = 3

        fun show(itemId: LLUUID, objectId: LLUUID): PreviewGesture {
            System.err.println("PreviewGesture: show not yet implemented")
            return PreviewGesture(LLSD())
        }

        fun finishInventoryUpload(itemId: LLUUID, newAssetId: LLUUID) {
            if (GestureMgr.isGestureActive(itemId)) {
                GestureMgr.replaceGesture(itemId, newAssetId)
                System.err.println("PreviewGesture: finishInventoryUpload notifyObservers not yet implemented")
            }
            System.err.println("PreviewGesture: finishInventoryUpload onUpdateSucceeded not yet implemented")
        }

        private fun updateLabel(item: ScrollListItem?) {
            val step = item?.getUserdata() as? com.firestorm.llcharacter.GestureStep ?: return
            val cell = item.getColumn(0) ?: return
            cell.setValue(getStepLabel(step.getLabel()))
        }

        private fun getStepLabel(labels: List<String>): String {
            if (labels.size != 2) return ""
            val action = labels[1]
            val localizedAction = when (action) {
                "None" -> ""
                "until animations are done" -> ""
                else -> action
            }
            return when (labels[0]) {
                "Chat" -> "Chat $localizedAction"
                "Sound" -> "Sound $localizedAction"
                "Wait" -> "Wait $localizedAction"
                "AnimFlagStop" -> "AnimFlagStop $localizedAction"
                "AnimFlagStart" -> "AnimFlagStart $localizedAction"
                else -> ""
            }
        }
    }
}

interface InventoryItemRef {
    val uuid: LLUUID
    val assetUUID: LLUUID
    val description: String
    val permissions: PermissionsRef
    val isFinished: () -> Boolean
}

interface PermissionsRef {
    fun allowModifyBy(agentId: LLUUID): Boolean
}
