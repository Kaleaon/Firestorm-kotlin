package com.firestorm.newview

class LLFlyoutComboBtnCtrl(
    private val mParent: LLPanel,
    private val mActionButton: String,
    private val mFlyoutButton: String,
    menuFile: String,
    private val mApplyImmediately: Boolean = true
) {
    private val mFlyoutMenu: LLToggleableMenu
    private var mSelectedName: String = ""

    private val mActionSignal: MutableList<(LLUICtrl?, LLSD) -> Unit> = mutableListOf()

    init {
        val saveRegistrar = LLUICtrl.CommitCallbackRegistry.ScopedRegistrar()
        saveRegistrar.add("FlyoutCombo.Button.Action") { ctrl, data -> onFlyoutItemSelected(ctrl, data) }

        val enabledRegistrar = LLUICtrl.EnableCallbackRegistry.ScopedRegistrar()
        enabledRegistrar.add("FlyoutCombo.Button.Check") { ctrl, data -> onFlyoutItemCheck(ctrl, data) }

        mParent.childSetAction(mFlyoutButton) { ctrl, data -> onFlyoutButton(ctrl, data) }
        mParent.childSetAction(mActionButton) { ctrl, data -> onFlyoutAction(ctrl, data) }

        mFlyoutMenu = LLUICtrlFactory.getInstance().createFromFile<LLToggleableMenu>(
            menuFile, gMenuHolder, LLViewerMenuHolderGL.childRegistryInstance()
        )

        setSelectedItem(0)
    }

    fun setAction(cb: (LLUICtrl?, LLSD) -> Unit) {
        mActionSignal.add(cb)
    }

    fun getItemCount(): UInt = mFlyoutMenu.getItemCount()

    fun setSelectedItem(itemno: Int) {
        val pitem = mFlyoutMenu.getItem(itemno)
        setSelectedItem(pitem)
    }

    fun setSelectedItem(item: String) {
        val pitem = mFlyoutMenu.getChild<LLMenuItemGL>(item, false)
        setSelectedItem(pitem)
    }

    fun setMenuItemEnabled(item: String, enabled: Boolean) {
        mFlyoutMenu.setItemEnabled(item, enabled)
        if (item == mSelectedName) {
            mParent.getChildView(mActionButton).setEnabled(enabled)
        }
    }

    fun setShownBtnEnabled(enabled: Boolean) {
        mParent.getChildView(mActionButton).setEnabled(enabled)
    }

    fun setMenuItemVisible(item: String, visible: Boolean) {
        mFlyoutMenu.setItemVisible(item, visible)
    }

    fun setMenuItemLabel(item: String, label: String) {
        mFlyoutMenu.setItemLabel(item, label)
    }

    protected fun onFlyoutButton(ctrl: LLUICtrl?, data: LLSD) {
        val (x, y) = LLUIInstance.getMousePositionLocal(mParent)
        mFlyoutMenu.updateParent(LLMenuGL.sMenuContainer)
        LLMenuGL.showPopup(mParent, mFlyoutMenu, x, y)
    }

    protected fun onFlyoutItemSelected(ctrl: LLUICtrl?, data: LLSD) {
        val pmenuitem = ctrl as? LLMenuItemGL ?: return
        setSelectedItem(pmenuitem)
        if (mApplyImmediately) {
            onFlyoutAction(pmenuitem, data)
        }
    }

    protected fun onFlyoutItemCheck(ctrl: LLUICtrl?, data: LLSD): Boolean {
        if (mApplyImmediately) return false
        val pmenuitem = ctrl as? LLMenuItemGL ?: return false
        return pmenuitem.getName() == mSelectedName
    }

    protected fun onFlyoutAction(ctrl: LLUICtrl?, data: LLSD) {
        val pmenuitem = mFlyoutMenu.getChild<LLMenuItemGL>(mSelectedName)
        if (mActionSignal.isNotEmpty()) {
            for (cb in mActionSignal) {
                cb(pmenuitem, data)
            }
        }
    }

    private fun setSelectedItem(pitem: LLMenuItemGL?) {
        if (pitem == null) return
        mSelectedName = pitem.getName()
        val actionButton = mParent.getChild<LLButton>(mActionButton)
        actionButton.setEnabled(pitem.getEnabled())
        actionButton.setLabel(pitem.getLabel())
    }
}
