package com.firestorm.llui

open class ModalDialog(
    key: String,
    val isModal: Boolean = true
) : Floater(key) {

    private var visibleTimeStart: Long = 0L

    init {
        if (isModal) {
            minimizable = false
            closeable = false
        }
        visible = false
        closeCallbacks.add { stopModal() }
        centerOnScreen()
    }

    open fun postBuild(): Boolean = true

    open fun openFloater(key: Any? = null) {
        val savedHost = floaterHost
        floaterHost = null
        super.open_()
        floaterHost = savedHost
    }

    override fun open_() = openFloater()

    open fun onOpen(key: Any?) {
        if (isModal) {
            val front = sModalStack.firstOrNull()
            if (front != null && front !== this) {
                front.visible = false
            }
            TODO("APR: setMouseCapture(this)")
            TODO("APR: addPopup(this)")
            setFocusModal(true)

            sModalStack.remove(this)
            sModalStack.addFirst(this)
        }
        visibleTimeStart = System.currentTimeMillis()
    }

    fun stopModal() {
        TODO("APR: unlockFocus(); releaseFocusIfNeeded(this)")
        if (isModal) {
            if (!sModalStack.remove(this)) {
                println("ModalDialog::stopModal not in list!")
            }
        }
        sModalStack.firstOrNull()?.visible = true
    }

    open fun reshape(width: Int, height: Int, fromParent: Boolean = true) {
        TODO("GPU: reshape modal dialog to ($width, $height)")
        centerOnScreen()
    }

    open fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        val popupMenu = MenuGL.sMenuContainer?.getVisibleMenu()
        if (popupMenu != null) {
            TODO("APR: check if click is inside popup menu; hide if outside")
        }
        if (isModal) {
            val handled = super.handleMouseDownBase(x, y, mask)
            if (!handled) {
                TODO("APR: play UISndInvalidOp sound")
            }
        } else {
            super.handleMouseDownBase(x, y, mask)
        }
        return true
    }

    open fun handleMouseUp(x: Int, y: Int, mask: UInt): Boolean {
        TODO("APR: childrenHandleMouseUp($x, $y, $mask)")
        return true
    }

    open fun handleHover(x: Int, y: Int, mask: UInt): Boolean {
        TODO("GPU: set arrow cursor; delegate hover to children and visible popup menu")
        return true
    }

    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        TODO("APR: childrenHandleScrollWheel($x, $y, $clicks)")
        return true
    }

    open fun handleDoubleClick(x: Int, y: Int, mask: UInt): Boolean {
        TODO("APR: childrenHandleDoubleClick($x, $y, $mask); play UISndInvalidOp if unhandled")
        return true
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        MenuGL.sMenuContainer?.hideMenus()
        TODO("APR: childrenHandleRightMouseDown($x, $y, $mask)")
        return true
    }

    open fun handleKeyHere(key: Int, mask: UInt): Boolean {
        TODO("APR: delegate to super.handleKeyHere($key, $mask)")
        if (isModal) {
            val isQuit = key == KEY_Q && mask == MASK_CONTROL
            return !isQuit
        } else {
            val elapsedMs = System.currentTimeMillis() - visibleTimeStart
            if (elapsedMs > 1000L && key == KEY_ESCAPE) {
                close()
                return true
            }
            return false
        }
    }

    open fun setVisible(vis: Boolean) {
        if (isModal) {
            if (vis) {
                MenuGL.sMenuContainer?.hideMenus()
                TODO("APR: hideEmojiHelper(); setMouseCapture(this); addPopup(this)")
                setFocusModal(true)
            } else {
                TODO("APR: releaseFocusIfNeeded(this)")
            }
        }
        visible = vis
    }

    open fun draw() {
        TODO("GPU: gl_drop_shadow(); super.draw()")
    }

    fun centerOnScreen() {
        TODO("GPU: center this dialog within the current window bounds")
    }

    private fun setFocusModal(focus: Boolean) {
        TODO("APR: set keyboard focus on this modal dialog")
    }

    companion object {
        val sModalStack: ArrayDeque<ModalDialog> = ArrayDeque()

        fun onAppFocusLost() {
            val instance = sModalStack.firstOrNull() ?: return
            TODO("APR: release mouse capture from instance; instance.setFocus(false)")
        }

        fun onAppFocusGained() {
            val instance = sModalStack.firstOrNull() ?: return
            TODO("APR: setMouseCapture(instance); instance.setFocus(true); addPopup(instance); centerOnScreen")
        }

        fun activeCount(): Int = sModalStack.size

        fun shutdownModals() { sModalStack.clear() }

        private const val KEY_ESCAPE: Int = 0x1B
        private const val KEY_Q: Int = 'Q'.code
        private const val MASK_CONTROL: UInt = 0x01u
    }
}

private var floaterHost: Any? = null

private fun Floater.handleMouseDownBase(x: Int, y: Int, mask: UInt): Boolean {
    TODO("APR: delegate mouse down to floater base implementation")
}

val Floater.closeCallbacks: MutableList<() -> Unit>
    get() = TODO("APR: get close callbacks list for floater")
