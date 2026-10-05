package com.firestorm.newview

import java.util.UUID

class FloaterAO(private val key: Map<String, Any>) {

    private var mSetList: MutableList<AOSet> = mutableListOf()
    private var mSelectedSet: AOSet? = null
    private var mSelectedState: AOSet.AOState? = null
    private var mCanDragAndDrop: Boolean = false
    private var mImportRunning: Boolean = false
    private var mCurrentBoldItem: ScrollListItem? = null
    private var mMore: Boolean = true

    private var mReloadCoverPanel: Panel? = null
    private var mMainInterfacePanel: Panel? = null
    private var mSmallInterfacePanel: Panel? = null

    private var mSetSelector: ComboBox? = null
    private var mActivateSetButton: Button? = null
    private var mAddButton: Button? = null
    private var mRemoveButton: Button? = null
    private var mDefaultCheckBox: CheckBox? = null
    private var mOverrideSitsCheckBox: CheckBox? = null
    private var mSmartCheckBox: CheckBox? = null
    private var mDisableMouselookCheckBox: CheckBox? = null

    private var mStateSelector: ComboBox? = null
    private var mAnimationList: ScrollListCtrl? = null
    private var mMoveUpButton: Button? = null
    private var mMoveDownButton: Button? = null
    private var mTrashButton: Button? = null
    private var mCycleCheckBox: CheckBox? = null
    private var mRandomizeCheckBox: CheckBox? = null
    private var mCycleTimeTextLabel: TextBox? = null
    private var mCycleTimeSpinner: SpinCtrl? = null

    private var mReloadButton: Button? = null
    private var mPreviousButton: Button? = null
    private var mNextButton: Button? = null
    private var mLessButton: Button? = null

    private var mSetSelectorSmall: ComboBox? = null
    private var mMoreButton: Button? = null
    private var mPreviousButtonSmall: Button? = null
    private var mNextButtonSmall: Button? = null
    private var mOverrideSitsCheckBoxSmall: CheckBox? = null

    open fun postBuild(): Boolean {
        System.err.println("FloaterAO: postBuild not yet implemented")
        updateSmart()

        AOEngine.instance.setReloadCallback { updateList() }
        AOEngine.instance.setAnimationChangedCallback { anim -> onAnimationChanged(anim) }

        onChangeAnimationSelection()
        mMainInterfacePanel?.setVisible(true)
        mSmallInterfacePanel?.setVisible(false)
        reloading(true)
        updateList()
        return true
    }

    fun onOpen(key: Map<String, Any>) {
        // no-op
    }

    fun onClose(appQuitting: Boolean) {
        if (!appQuitting) {
            // no-op
        }
    }

    fun updateList() {
        mReloadButton?.setEnabled(true)
        mImportRunning = false

        mSetList = AOEngine.instance.getSetList()
        mSetList.sortBy { it.name }

        val currentSetName = mSetSelector?.getSelectedItemLabel() ?: ""

        mSetSelector?.removeAll()
        mSetSelectorSmall?.removeAll()
        mAnimationList?.deleteAllItems()
        mCurrentBoldItem = null
        reloading(false)

        val noSetsLabel = "No sets loaded"
        if (mSetList.isEmpty()) {
            mSetSelector?.add(noSetsLabel)
            mSetSelectorSmall?.add(noSetsLabel)
            mSetSelector?.selectNthItem(0)
            mSetSelectorSmall?.selectNthItem(0)
            enableSetControls(false)
            return
        }

        val resolvedSetName = when {
            currentSetName.isEmpty() || currentSetName == noSetsLabel -> {
                AOEngine.instance.getCurrentSetName().ifEmpty { mSetList[0].name }
            }
            else -> currentSetName
        }

        var selectedIndex = 0
        for ((index, set) in mSetList.withIndex()) {
            mSetSelector?.add(set.name)
            mSetSelectorSmall?.add(set.name)
            if (set.name == resolvedSetName) {
                selectedIndex = index
                mSelectedSet = AOEngine.instance.selectSetByName(resolvedSetName)
                updateSetParameters()
                updateAnimationList()
            }
        }

        mSetSelector?.selectNthItem(selectedIndex)
        mSetSelectorSmall?.selectNthItem(selectedIndex)
        enableSetControls(true)

        if (mSetSelector?.getSelectedItemLabel().isNullOrEmpty()) {
            onClickReload()
        }
    }

    fun updateScrollListData() {
        val animationListData = mAnimationList?.getAllData() ?: return
        val anims = mSelectedState?.mAnimations ?: return
        for (index in anims.indices) {
            animationListData.getOrNull(index)?.setUserdata(anims[index].mInventoryUUID)
        }
    }

    fun updateSetParameters() {
        val set = mSelectedSet ?: return
        mOverrideSitsCheckBox?.setValue(set.getSitOverride())
        mOverrideSitsCheckBoxSmall?.setValue(set.getSitOverride())
        mSmartCheckBox?.setValue(set.getSmart())
        mDisableMouselookCheckBox?.setValue(set.getMouselookStandDisable())
        val isDefault = set == AOEngine.instance.getDefaultSet()
        mDefaultCheckBox?.setValue(isDefault)
        updateSmart()
    }

    fun updateAnimationList() {
        val currentStateSelected = mStateSelector?.getCurrentIndex() ?: -1

        mStateSelector?.removeAll()
        onChangeAnimationSelection()

        val set = mSelectedSet
        if (set == null) {
            mStateSelector?.setEnabled(false)
            mStateSelector?.add("No animations loaded")
            return
        }

        for (stateName in set.mStateNames) {
            val state = set.getStateByName(stateName)
            mStateSelector?.add(stateName, state)
        }

        enableStateControls(true)

        if (currentStateSelected == -1) {
            mStateSelector?.selectFirstItem()
        } else {
            mStateSelector?.selectNthItem(currentStateSelected)
        }

        onSelectState()
    }

    private fun reloading(reload: Boolean) {
        mReloadCoverPanel?.setVisible(reload)
        enableSetControls(!reload)
        enableStateControls(!reload)
    }

    fun tick(): Boolean {
        updateList()
        return false
    }

    fun enableSetControls(enable: Boolean) {
        mSetSelector?.setEnabled(enable)
        mSetSelectorSmall?.setEnabled(enable)
        mActivateSetButton?.setEnabled(enable)
        mRemoveButton?.setEnabled(enable)
        mDefaultCheckBox?.setEnabled(enable)
        mOverrideSitsCheckBox?.setEnabled(enable)
        mOverrideSitsCheckBoxSmall?.setEnabled(enable)
        mDisableMouselookCheckBox?.setEnabled(enable)
        if (!enable) enableStateControls(false)
    }

    fun enableStateControls(enable: Boolean) {
        mStateSelector?.setEnabled(enable)
        mAnimationList?.setEnabled(enable)
        mCycleCheckBox?.setEnabled(enable)
        if (enable) {
            updateCycleParameters()
        } else {
            mRandomizeCheckBox?.setEnabled(false)
            mCycleTimeTextLabel?.setEnabled(false)
            mCycleTimeSpinner?.setEnabled(false)
        }
        mPreviousButton?.setEnabled(enable)
        mPreviousButtonSmall?.setEnabled(enable)
        mNextButton?.setEnabled(enable)
        mNextButtonSmall?.setEnabled(enable)
        mCanDragAndDrop = enable
    }

    fun onSelectSet() {
        val label = mSetSelector?.getSelectedItemLabel() ?: return
        val set = AOEngine.instance.getSetByName(label)
        if (set == null) {
            onRenameSet()
            return
        }
        if (mSelectedSet != set) {
            mSelectedSet = set
            updateSetParameters()
            updateAnimationList()
        }
    }

    fun onSelectSetSmall() {
        val idx = mSetSelectorSmall?.getCurrentIndex() ?: return
        mSetSelector?.selectNthItem(idx)
        mSelectedSet = AOEngine.instance.getSetByName(mSetSelectorSmall?.getSelectedItemLabel() ?: "")
        if (mSelectedSet != null) {
            updateSetParameters()
            updateAnimationList()
            onClickActivate()
        }
    }

    fun onRenameSet() {
        val set = mSelectedSet ?: return
        val name = mSetSelector?.getSimple()?.trim() ?: return
        if (name.isNotEmpty()) {
            val validChars = name.all { it.code in 32..126 }
            val noForbidden = !name.contains(':') && !name.contains('|')
            if (validChars && noForbidden) {
                if (AOEngine.instance.renameSet(set, name)) {
                    reloading(true)
                    return
                }
            }
        }
        mSetSelector?.setSimple(set.name)
    }

    fun onClickActivate() {
        val idx = mSetSelector?.getCurrentIndex() ?: return
        mSetSelectorSmall?.selectNthItem(idx)
        AOEngine.instance.selectSet(mSelectedSet)
    }

    fun addAnimation(name: String): ScrollListItem? {
        return mAnimationList?.addElement(mapOf(
            "icon" to "FSAO_Animation_Stopped",
            "animation_name" to name
        ))
    }

    fun onSelectState() {
        mAnimationList?.deleteAllItems()
        mCurrentBoldItem = null
        mAnimationList?.setCommentText("No animations loaded")
        mAnimationList?.setEnabled(false)
        onChangeAnimationSelection()

        val set = mSelectedSet ?: return
        val stateName = mStateSelector?.getSelectedItemLabel() ?: return
        mSelectedState = set.getStateByName(stateName) ?: return

        val state = mSelectedState!!
        if (state.mAnimations.isNotEmpty()) {
            for (anim in state.mAnimations) {
                val item = addAnimation(anim.mName)
                if (item != null) {
                    item.setUserdata(anim.mInventoryUUID)
                    if (set.getMotion() == state.mRemapID && state.mCurrentAnimationID == anim.mAssetUUID) {
                        mCurrentBoldItem = item
                        item.setIcon("FSAO_Animation_Playing")
                        item.setBold(true)
                    }
                }
            }
            mAnimationList?.setCommentText("")
            mAnimationList?.setEnabled(true)
        }

        mCycleCheckBox?.setValue(state.mCycle)
        mRandomizeCheckBox?.setValue(state.mRandom)
        mCycleTimeSpinner?.setValue(state.mCycleTime)
        updateCycleParameters()
    }

    fun onClickReload() {
        reloading(true)
        mSelectedSet = null
        mSelectedState = null
        AOEngine.instance.reload(false)
        updateList()
    }

    fun onClickAdd() {
        // no-op
    }

    fun newSetCallback(newSetName: String, confirmed: Boolean): Boolean {
        val name = newSetName.trim()
        if (name.isEmpty()) return false
        val validChars = name.all { it.code in 32..126 }
        val noForbidden = !name.contains(':') && !name.contains('|')
        if (!validChars || !noForbidden) {
            System.err.println("FloaterAO: newSetCallback not yet implemented")
        }
        if (confirmed) {
            if (AOEngine.instance.getSetByName(name) != null) {
                System.err.println("FloaterAO: newSetCallback not yet implemented")
            }
            AOEngine.instance.addSet(name) { reloading(true) }
        }
        return false
    }

    fun onClickRemove() {
        val set = mSelectedSet ?: return
        // no-op
    }

    fun removeSetCallback(confirmed: Boolean): Boolean {
        if (confirmed) {
            val set = mSelectedSet ?: return false
            if (AOEngine.instance.removeSet(set)) {
                reloading(true)
                mSetSelector?.removeAll()
                mSetSelectorSmall?.removeAll()
                mAnimationList?.deleteAllItems()
                mCurrentBoldItem = null
                return true
            }
        }
        return false
    }

    fun onCheckDefault() {
        val set = mSelectedSet ?: return
        val selectedSet = if (mDefaultCheckBox?.getValue() == true) set else null
        AOEngine.instance.setDefaultSet(selectedSet)
    }

    fun onCheckOverrideSits() {
        mOverrideSitsCheckBoxSmall?.setValue(mOverrideSitsCheckBox?.getValue())
        val set = mSelectedSet ?: return
        AOEngine.instance.setOverrideSits(set, mOverrideSitsCheckBox?.getValue() == true)
        updateSmart()
    }

    fun onCheckOverrideSitsSmall() {
        mOverrideSitsCheckBox?.setValue(mOverrideSitsCheckBoxSmall?.getValue())
        onCheckOverrideSits()
    }

    fun updateSmart() {
        mSmartCheckBox?.setEnabled(mOverrideSitsCheckBox?.getValue() == true)
    }

    fun onCheckSmart() {
        val set = mSelectedSet ?: return
        AOEngine.instance.setSmart(set, mSmartCheckBox?.getValue() == true)
    }

    fun onCheckDisableStands() {
        val set = mSelectedSet ?: return
        AOEngine.instance.setDisableMouselookStands(set, mDisableMouselookCheckBox?.getValue() == true)
    }

    fun onChangeAnimationSelection() {
        val list = mAnimationList?.getAllSelected() ?: emptyList()
        var resortEnable = false
        var trashEnable = false

        if (!mCanDragAndDrop) {
            mAnimationList?.deselectAllItems()
        } else if (list.isNotEmpty()) {
            if (list.size == 1) resortEnable = true
            trashEnable = true
        }

        mMoveDownButton?.setEnabled(resortEnable)
        mMoveUpButton?.setEnabled(resortEnable)
        mTrashButton?.setEnabled(trashEnable)
    }

    fun onClickMoveUp() {
        val state = mSelectedState ?: return
        val list = mAnimationList?.getAllSelected() ?: return
        if (list.size != 1) return
        val currentIndex = mAnimationList?.getFirstSelectedIndex() ?: -1
        if (currentIndex == -1) return
        if (AOEngine.instance.swapWithPrevious(state, currentIndex)) {
            mAnimationList?.swapWithPrevious(currentIndex)
            updateScrollListData()
        }
    }

    fun onClickMoveDown() {
        val state = mSelectedState ?: return
        val list = mAnimationList?.getAllSelected() ?: return
        if (list.size != 1) return
        val currentIndex = mAnimationList?.getFirstSelectedIndex() ?: -1
        val itemCount = mAnimationList?.getItemCount() ?: 0
        if (currentIndex >= itemCount - 1) return
        if (AOEngine.instance.swapWithNext(state, currentIndex)) {
            mAnimationList?.swapWithNext(currentIndex)
            updateScrollListData()
        }
    }

    fun onClickTrash() {
        val state = mSelectedState ?: return
        val list = mAnimationList?.getAllSelected() ?: return
        if (list.isEmpty()) return
        for (index in list.indices.reversed()) {
            val itemIndex = mAnimationList?.getItemIndex(list[index]) ?: continue
            AOEngine.instance.removeAnimation(mSelectedSet, state, itemIndex)
        }
        mAnimationList?.deleteSelectedItems()
        mCurrentBoldItem = null
    }

    fun updateCycleParameters() {
        val enabled = mCycleCheckBox?.getValue() == true
        mRandomizeCheckBox?.setEnabled(enabled)
        mCycleTimeTextLabel?.setEnabled(enabled)
        mCycleTimeSpinner?.setEnabled(enabled)
    }

    fun onCheckCycle() {
        val state = mSelectedState ?: return
        val cycle = mCycleCheckBox?.getValue() == true
        AOEngine.instance.setCycle(state, cycle)
        updateCycleParameters()
    }

    fun onCheckRandomize() {
        val state = mSelectedState ?: return
        AOEngine.instance.setRandomize(state, mRandomizeCheckBox?.getValue() == true)
    }

    fun onChangeCycleTime() {
        val state = mSelectedState ?: return
        AOEngine.instance.setCycleTime(state, mCycleTimeSpinner?.getValueF32() ?: 0f)
    }

    fun onClickPrevious() = AOEngine.instance.cycle(AOEngine.CycleDirection.PREVIOUS, true)
    fun onClickNext() = AOEngine.instance.cycle(AOEngine.CycleDirection.NEXT, true)

    fun onDoubleClick() {
        val item = mAnimationList?.getFirstSelected() ?: return
        val animUUID = item.getUserdata() as? UUID ?: return
        if (mSelectedState != AOEngine.instance.getCurrentState()) return
        if (AOEngine.instance.getCurrentSet() != mSelectedSet) {
            val idx = mSetSelector?.getCurrentIndex() ?: return
            mSetSelectorSmall?.selectNthItem(idx)
            AOEngine.instance.selectSet(mSelectedSet)
        }
        AOEngine.instance.playAnimation(animUUID)
    }

    fun onClickMore() {
        mMore = true
        mSmallInterfacePanel?.setVisible(false)
        mMainInterfacePanel?.setVisible(true)
        // no-op
    }

    fun onClickLess() {
        mMore = false
        mMainInterfacePanel?.setVisible(false)
        mSmallInterfacePanel?.setVisible(true)
        // no-op
    }

    fun onAnimationChanged(animation: UUID) {
        val boldItem = mCurrentBoldItem
        if (boldItem != null) {
            boldItem.setIcon("FSAO_Animation_Stopped")
            boldItem.setBold(false)
            mCurrentBoldItem = null
        }

        if (animation == UUID.fromString("00000000-0000-0000-0000-000000000000")) return

        val allData = mAnimationList?.getAllData() ?: return
        for (item in allData) {
            val id = item.getUserdata() as? UUID
            if (id == animation) {
                mCurrentBoldItem = item
                item.setIcon("FSAO_Animation_Playing")
                item.setBold(true)
                return
            }
        }
    }

    fun handleDragAndDrop(
        x: Int, y: Int, drop: Boolean,
        cargoType: DragAndDropType,
        cargoData: Any?,
        accept: AcceptanceHolder,
        tooltipMsg: StringBuilder
    ): Boolean {
        if (!mMore) {
            tooltipMsg.clear()
            tooltipMsg.append("Drag and drop only available on full interface")
            accept.value = Acceptance.ACCEPT_NO
            return true
        }

        val item = cargoData as? InventoryItem2

        return when (cargoType) {
            DragAndDropType.DAD_NOTECARD -> {
                if (mImportRunning) {
                    accept.value = Acceptance.ACCEPT_NO
                } else {
                    accept.value = Acceptance.ACCEPT_YES_SINGLE
                    if (item != null && drop) {
                        if (AOEngine.instance.importNotecard(item)) {
                            reloading(true)
                            mReloadButton?.setEnabled(false)
                            mImportRunning = true
                        }
                    }
                }
                true
            }
            DragAndDropType.DAD_ANIMATION -> {
                if (!drop && (mSelectedSet == null || mSelectedState == null || !mCanDragAndDrop)) {
                    accept.value = Acceptance.ACCEPT_NO
                    return true
                }
                accept.value = Acceptance.ACCEPT_YES_MULTI
                if (item != null && drop) {
                    AOEngine.instance.addAnimation(mSelectedSet, mSelectedState, item)
                    addAnimation(item.name)
                    reloading(true)
                }
                true
            }
            else -> {
                accept.value = Acceptance.ACCEPT_NO
                true
            }
        }
    }

    enum class DragAndDropType { DAD_NOTECARD, DAD_ANIMATION, OTHER }
    enum class Acceptance { ACCEPT_NO, ACCEPT_YES_SINGLE, ACCEPT_YES_MULTI }
    class AcceptanceHolder(var value: Acceptance = Acceptance.ACCEPT_NO)
    class InventoryItem2(val name: String)

    private class Panel {
        fun setVisible(v: Boolean) {}
    }
    private class Button {
        fun setEnabled(v: Boolean) {}
    }
    private class ComboBox {
        fun add(label: String, data: Any? = null) {}
        fun removeAll() {}
        fun selectNthItem(idx: Int) {}
        fun selectFirstItem() {}
        fun getCurrentIndex(): Int = -1
        fun getSelectedItemLabel(): String = ""
        fun getSimple(): String = ""
        fun setSimple(s: String) {}
        fun setEnabled(v: Boolean) {}
    }
    private class CheckBox {
        fun getValue(): Boolean = false
        fun setValue(v: Any?) {}
        fun setEnabled(v: Boolean) {}
    }
    private class TextBox {
        fun setEnabled(v: Boolean) {}
    }
    private class SpinCtrl {
        fun setValue(v: Any?) {}
        fun getValueF32(): Float = 0f
        fun setEnabled(v: Boolean) {}
    }
    private class ScrollListItem {
        private var userdata: Any? = null
        fun setUserdata(d: Any?) { userdata = d }
        fun getUserdata(): Any? = userdata
        fun setIcon(icon: String) {}
        fun setBold(bold: Boolean) {}
    }
    private class ScrollListCtrl {
        fun deleteAllItems() {}
        fun deleteSelectedItems() {}
        fun setCommentText(t: String) {}
        fun setEnabled(v: Boolean) {}
        fun addElement(data: Map<String, Any>): ScrollListItem = ScrollListItem()
        fun getAllData(): List<ScrollListItem> = emptyList()
        fun getAllSelected(): List<ScrollListItem> = emptyList()
        fun getFirstSelected(): ScrollListItem? = null
        fun getFirstSelectedIndex(): Int = -1
        fun getItemCount(): Int = 0
        fun getItemIndex(item: ScrollListItem): Int = -1
        fun deselectAllItems() {}
        fun swapWithPrevious(idx: Int) {}
        fun swapWithNext(idx: Int) {}
    }
}
