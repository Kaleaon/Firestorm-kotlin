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
        TODO("GPU: Floater.reshape($width, $height, $fromParent); then centerOnScreen()")
    }

    open fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        TODO("APR: check visible popup menu; hide if click is outside; delegate to super if modal, play UISndInvalidOp if unhandled")
        return true
    }

    open fun handleMouseUp(x: Int, y: Int, mask: UInt): Boolean {
        TODO("APR: childrenHandleMouseUp($x, $y, $mask)")
        return true
    }

    open fun handleHover(x: Int, y: Int, mask: UInt): Boolean {
        TODO("GPU: set arrow cursor; delegate hover to children; route hover into visible popup menu when mouse is over it, releasing capture")
        return true
    }

    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        TODO("APR: childrenHandleScrollWheel($x, $y, $clicks)")
        return true
    }

    open fun handleDoubleClick(x: Int, y: Int, mask: UInt): Boolean {
        TODO("APR: super.handleDoubleClick; play UISndInvalidOp if unhandled")
        return true
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        TODO("APR: hideMenus(); childrenHandleRightMouseDown($x, $y, $mask)")
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
        TODO("GPU: gl_drop_shadow(0, rect.height, rect.width, 0, shadowColor, DROP_SHADOW_FLOATER); super.draw()")
    }

    fun centerOnScreen() {
        TODO("GPU: center this dialog within the current window bounds using LLUI.getWindowSize()")
    }

    companion object {
        val sModalStack: ArrayDeque<ModalDialog> = ArrayDeque()

        var currentFloaterHost: Any? = null

        fun onAppFocusLost() {
            val instance = sModalStack.firstOrNull() ?: return
            TODO("APR: if instance has mouse capture, release it; instance.setFocus(false)")
        }

        fun onAppFocusGained() {
            val instance = sModalStack.firstOrNull() ?: return
            TODO("APR: setMouseCapture(instance); instance.setFocus(true); addPopup(instance); instance.centerOnScreen()")
        }

        fun activeCount(): Int = sModalStack.size

        fun shutdownModals() { sModalStack.clear() }

        private const val KEY_ESCAPE: Int = 0x1B
        private const val KEY_Q: Int = 'Q'.code
        private const val MASK_CONTROL: UInt = 0x01u
    }
}
