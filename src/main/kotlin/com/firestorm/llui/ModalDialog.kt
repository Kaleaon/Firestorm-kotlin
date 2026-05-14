package com.firestorm.llui

open class ModalDialog(
    key: String,
    val isModal: Boolean = true
) : Floater(key) {

    private var visibleTimeStart: Long = 0L

    val closeSignals: MutableList<() -> Unit> = mutableListOf()

    init {
        if (isModal) {
            minimizable = false
            closeable = false
        }
        visible = false
        closeSignals.add { stopModal() }
        centerOnScreen()
    }

    open fun postBuild(): Boolean = true

    open fun openFloater(key: Any? = null) {
        val savedHost = currentFloaterHost
        currentFloaterHost = null
        super.open_()
        currentFloaterHost = savedHost
    }

    override fun open_() = openFloater()

    open fun onOpen(key: Any?) {
        if (isModal) {
            val front = sModalStack.firstOrNull()
            if (front != null && front !== this) {
                front.visible = false
            }
            // APR: FocusMgr.setMouseCapture(this); LLUI.addPopup(this); FocusMgr.setFocus(this)
            sModalStack.remove(this)
            sModalStack.addFirst(this)
        }
        visibleTimeStart = System.currentTimeMillis()
    }

    fun stopModal() {
        // APR: FocusMgr.unlockFocus(); FocusMgr.releaseFocusIfNeeded(this)
        if (isModal) {
            if (!sModalStack.remove(this)) {
                println("ModalDialog::stopModal not in list!")
            }
        }
        sModalStack.firstOrNull()?.visible = true
    }

    open fun reshape(width: Int, height: Int, fromParent: Boolean = true) {
        // GPU: super.reshape(width, height, fromParent)
        centerOnScreen()
    }

    open fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        // APR: if visible popup menu and click is outside, hideMenus()
        // APR: if modal, delegate to super; if unhandled play UISndInvalidOp
        // APR: if non-modal, delegate to super
        return true
    }

    open fun handleMouseUp(x: Int, y: Int, mask: UInt): Boolean {
        // APR: childrenHandleMouseUp(x, y, mask)
        return true
    }

    open fun handleHover(x: Int, y: Int, mask: UInt): Boolean {
        // GPU: if no child handles hover, set arrow cursor
        // APR: if popup menu is under mouse, route hover into it and release mouse capture
        return true
    }

    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        // APR: childrenHandleScrollWheel(x, y, clicks)
        return true
    }

    open fun handleDoubleClick(x: Int, y: Int, mask: UInt): Boolean {
        // APR: if super.handleDoubleClick returns false, play UISndInvalidOp
        return true
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        // APR: hideMenus(); childrenHandleRightMouseDown(x, y, mask)
        return true
    }

    open fun handleKeyHere(key: Int, mask: UInt): Boolean {
        // APR: super.handleKeyHere(key, mask)
        return if (isModal) {
            val isQuit = key == KEY_Q && mask == MASK_CONTROL
            !isQuit
        } else {
            val elapsedMs = System.currentTimeMillis() - visibleTimeStart
            if (elapsedMs > 1000L && key == KEY_ESCAPE) {
                close()
                true
            } else {
                false
            }
        }
    }

    open fun setVisible(vis: Boolean) {
        if (isModal) {
            if (vis) {
                // APR: MenuGL.sMenuContainer.hideMenus(); EmojiHelper.hideHelper(); FocusMgr.setMouseCapture(this); LLUI.addPopup(this); FocusMgr.setFocus(this)
            } else {
                // APR: FocusMgr.releaseFocusIfNeeded(this)
            }
        }
        visible = vis
    }

    open fun draw() {
        // GPU: gl_drop_shadow(0, rect.height, rect.width, 0, ColorDropShadow, DROP_SHADOW_FLOATER)
        // GPU: super.draw()
    }

    fun centerOnScreen() {
        // GPU: centerWithin(Rect(0, 0, round(windowSize.x), round(windowSize.y)))
    }

    companion object {
        val sModalStack: ArrayDeque<ModalDialog> = ArrayDeque()

        var currentFloaterHost: Any? = null

        fun onAppFocusLost() {
            val instance = sModalStack.firstOrNull() ?: return
            System.err.println("ModalDialog: onAppFocusLost not yet implemented")
        }

        fun onAppFocusGained() {
            val instance = sModalStack.firstOrNull() ?: return
            System.err.println("ModalDialog: onAppFocusGained not yet implemented")
        }

        fun activeCount(): Int = sModalStack.size

        fun shutdownModals() { sModalStack.clear() }

        private const val KEY_ESCAPE: Int = 0x1B
        private const val KEY_Q: Int = 'Q'.code
        private const val MASK_CONTROL: UInt = 0x01u
    }
}
