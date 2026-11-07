package com.firestorm.llui

// DockableFloater: a floater that can attach ("dock") to a host widget.
// uniqueDocking means at most one docked instance is visible at a time.
// useTongue controls whether a visual pointer image is drawn toward the dock.

private const val UNDOCK_LEAP_HEIGHT: UInt = 12u

open class DockableFloater(
    dockControl: DockControl?,
    val uniqueDocking: Boolean = true,
    var useTongue: Boolean = true,
    key: LLSD,
    params: Params = defaultParams()
) : Floater(key, params) {

    var dockControl: DockControl? = dockControl
        private set

    var overlapsScreenChannel: Boolean = false
        open get() = field && isVisible && isDocked
        open set(value) { field = value }

    var isDockedStateForcedCallback: (() -> Boolean)? = null

    private var dockTongue: UIImage? = null
    private var drawAtParentTop: Boolean = false
    private var forceDocking: Boolean = false

    init {
        initialize()
    }

    private fun initialize() {
        setDocked(this.dockControl?.isDockVisible() == true)
        resetInstance()
        setCanClose(true)
        setCanDock(true)
        setCanMinimize(true)
        overlapsScreenChannel = false
        forceDocking = false
    }

    open fun postBuild(): Boolean {
        if (isDockedStateForcedCallback?.invoke() == true) {
            forceDocking = true
        }
        dockTongue = LLUI.getUIImage("Flyout_Pointer")
        super.setDocked(true)
        return super.postBuild()
    }

    open fun setDocked(docked: Boolean, popOnUndock: Boolean = true) {
        val ctrl = dockControl
        if (ctrl != null && ctrl.isDockVisible()) {
            if (docked) {
                resetInstance()
                ctrl.on()
            } else {
                ctrl.off()
            }
            if (!docked && popOnUndock) {
                translate(0, UNDOCK_LEAP_HEIGHT.toInt())
            }
        }
        super.setDocked(docked, popOnUndock)
    }

    open fun setVisible(visible: Boolean) {
        if (visible && forceDocking) {
            setCanDock(true)
            setDocked(true)
            forceDocking = false
        }
        if (visible && isDocked) {
            resetInstance()
        }
        if (visible) {
            dockControl?.repositionDockable()
        }
        if (visible && !isMinimized) {
            super.setFrontmost(autoFocus)
        }
        super.setVisible(visible)
    }

    open fun setMinimized(minimize: Boolean) {
        if (minimize && isDocked) {
            // minimizing a docked floater just hides it
            setVisible(false)
        } else {
            super.setMinimized(minimize)
        }
    }

    open fun draw() {
        val ctrl = dockControl
        if (ctrl != null) {
            ctrl.repositionDockable()
            if (isDocked) {
                ctrl.drawTongue()
            }
        }
        super.draw()
    }

    fun getDockWidget(): View? = dockControl?.getDock()

    open fun onDockHidden() {
        setCanDock(false)
    }

    open fun onDockShown() {
        if (!isMinimized) {
            setCanDock(true)
        }
    }

    fun setDockControl(newDockControl: DockControl?) {
        dockControl = newDockControl
        setDocked(isDocked)
    }

    fun getDockTongue(dockSide: DockControl.DocAt = DockControl.DocAt.TOP): UIImage? {
        dockTongue = when (dockSide) {
            DockControl.DocAt.LEFT  -> LLUI.getUIImage("Flyout_Left")
            DockControl.DocAt.RIGHT -> LLUI.getUIImage("Flyout_Right")
            else                    -> LLUI.getUIImage("Flyout_Pointer")
        }
        return dockTongue
    }

    private fun resetInstance() {
        if (uniqueDocking && instanceHandle?.get() !== this) {
            val prev = instanceHandle?.get()
            if (prev != null && prev.isDocked) {
                prev.setVisible(false)
            }
            instanceHandle = getHandle()
        }
    }

    companion object {
        var instanceHandle: FloaterHandle? = null
            private set

        fun getInstanceHandle(): FloaterHandle? = instanceHandle

        fun toggleInstance(sdname: LLSD) {
            val name = sdname.asString()
            val instance = FloaterReg.findInstance(name) as? DockableFloater
            if (instance == null || instance.isDocked) {
                FloaterReg.toggleInstance(name, LLSD())
                instance?.storeVisibilityControl()
            } else {
                instance.setMinimized(false)
                if (instance.isVisible) {
                    instance.setVisible(false)
                } else {
                    instance.setVisible(true)
                    gFloaterView?.bringToFront(instance)
                }
            }
        }
    }
}
