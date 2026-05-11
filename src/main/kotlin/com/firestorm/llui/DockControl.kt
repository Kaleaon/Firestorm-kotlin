package com.firestorm.llui

import com.firestorm.llmath.Rect

class DockControl(
    dockWidget: View?,
    private val dockableFloater: Floater,
    private val dockTongue: DockTongueImage,
    private val dockAt: DocAt,
    getAllowedRectCallback: ((Rect) -> Unit)? = null
) {
    enum class DocAt { TOP, LEFT, RIGHT, BOTTOM }

    private val getAllowedRect: (Rect) -> Unit
    private var enabled: Boolean = false
    private var recalculateDockablePosition: Boolean = false
    private var dockWidgetVisible: Boolean = false

    private var dockWidgetHandle: ViewHandle? = dockWidget?.getHandle()
    private var nonToolbarPanelHandle: ViewHandle? = null

    private var prevDockRect: Rect = Rect()
    private var rootRect: Rect = Rect()
    private var floaterRect: Rect = Rect()

    private var dockTongueX: Int = 0
    private var dockTongueY: Int = 0

    init {
        nonToolbarPanelHandle = dockableFloater.getRootView()
            .findChild("non_toolbar_panel")
            ?.getHandle()

        if (dockableFloater.isDocked()) on() else off()

        getAllowedRect = getAllowedRectCallback ?: { rect -> getAllowedRectDefault(rect) }

        if (dockWidget != null) repositionDockable()

        dockWidgetVisible = if (getDock() != null) isDockVisible() else false
    }

    fun on() {
        if (isDockVisible()) {
            enabled = true
            recalculateDockablePosition = true
        }
    }

    fun off() {
        enabled = false
    }

    fun forceRecalculatePosition() {
        recalculateDockablePosition = true
    }

    fun setDock(dockWidget: View?) {
        if (dockWidget != null) {
            dockWidgetHandle = dockWidget.getHandle()
            repositionDockable()
            dockWidgetVisible = isDockVisible()
        } else {
            dockWidgetHandle = null
            dockWidgetVisible = false
        }
    }

    fun getDock(): View? = dockWidgetHandle?.get()

    fun getTongueWidth(): Int = dockTongue.getWidth()
    fun getTongueHeight(): Int = dockTongue.getHeight()

    fun getAllowedRect(rect: Rect) = getAllowedRect.invoke(rect)

    fun repositionDockable() {
        val dock = getDock() ?: return
        val dockRect = dock.calcScreenRect()
        val currentRootRect = Rect()
        val floaterRect = dockableFloater.calcScreenRect()
        getAllowedRect(currentRootRect)

        val needsRecalc = prevDockRect != dockRect
            || dockWidgetVisible != isDockVisible()
            || this.rootRect != currentRootRect
            || this.floaterRect != floaterRect
            || recalculateDockablePosition

        if (!needsRecalc) return

        if (!isDockVisible()) {
            dockableFloater.setDocked(false)
            off()
            (dockableFloater as? DockableFloater)?.onDockHidden()
        } else {
            if (enabled) moveDockable()
            (dockableFloater as? DockableFloater)?.onDockShown()
        }

        prevDockRect = dockRect
        this.rootRect = currentRootRect
        this.floaterRect = floaterRect
        recalculateDockablePosition = false
        dockWidgetVisible = isDockVisible()
    }

    fun isDockVisible(): Boolean {
        val dock = getDock() ?: return true
        if (!dock.isInVisibleChain()) return false

        val dockRect = dock.calcScreenRect()
        return when (dockAt) {
            DocAt.TOP, DocAt.BOTTOM -> {
                val parentRect = dock.getRootView().calcScreenRect()
                !(dockRect.right <= parentRect.left || dockRect.left >= parentRect.right)
            }
            else -> true
        }
    }

    fun drawToungue() {
        val useTongue = (dockableFloater as? DockableFloater)?.getUseTongue() ?: false
        if (enabled && useTongue) {
            TODO("GPU: dockTongue.draw($dockTongueX, $dockTongueY)")
        }
    }

    private fun moveDockable() {
        val dock = getDock() ?: return
        val dockRect = dock.calcScreenRect()
        val currentRootRect = Rect()
        getAllowedRect(currentRootRect)

        val useTongue = (dockableFloater as? DockableFloater)?.getUseTongue() ?: false
        val dockableRect = dockableFloater.calcScreenRect()

        var x: Int
        var y: Int

        when (dockAt) {
            DocAt.LEFT -> {
                x = dockRect.left - dockableRect.width
                y = dockRect.centerY + dockableRect.height / 2
                if (useTongue) x -= dockTongue.getWidth()
                dockTongueX = dockableRect.right
                dockTongueY = dockableRect.centerY - dockTongue.getHeight() / 2
            }
            DocAt.RIGHT -> {
                x = dockRect.right
                y = dockRect.centerY + dockableRect.height / 2
                if (useTongue) x += dockTongue.getWidth()
                dockTongueX = dockRect.right
                dockTongueY = dockableRect.centerY - dockTongue.getHeight() / 2
            }
            DocAt.TOP -> {
                x = dockRect.centerX - dockableRect.width / 2
                y = dockRect.top + dockableRect.height
                if (useTongue) {
                    y += dockTongue.getHeight()
                    if (y > currentRootRect.top) y = currentRootRect.top
                }
                x = x.coerceIn(currentRootRect.left, currentRootRect.right - dockableRect.width)

                val dockParentRect = dock.parent!!.calcScreenRect()
                dockTongueX = when {
                    dockRect.centerX < dockParentRect.left  -> dockParentRect.left  - dockTongue.getWidth() / 2
                    dockRect.centerX > dockParentRect.right -> dockParentRect.right - dockTongue.getWidth() / 2
                    else                                     -> dockRect.centerX     - dockTongue.getWidth() / 2
                }
                dockTongueY = dockRect.top
            }
            DocAt.BOTTOM -> {
                x = dockRect.centerX - dockableRect.width / 2
                y = dockRect.bottom
                if (useTongue) y -= dockTongue.getHeight()

                x = x.coerceIn(currentRootRect.left, currentRootRect.right - dockableRect.width)

                val dockParentRect = dock.parent!!.calcScreenRect()
                dockTongueX = when {
                    dockRect.centerX < dockParentRect.left  -> dockParentRect.left  - dockTongue.getWidth() / 2
                    dockRect.centerX > dockParentRect.right -> dockParentRect.right - dockTongue.getWidth() / 2
                    else                                     -> dockRect.centerX     - dockTongue.getWidth() / 2
                }
                dockTongueY = dockRect.bottom - dockTongue.getHeight()
            }
        }

        if (useTongue) {
            val maxAvailableHeight =
                currentRootRect.height - (currentRootRect.bottom - dockTongueY) - dockTongue.getHeight()
            if (dockableRect.height >= maxAvailableHeight) {
                TODO("GPU: dockableFloater.reshape(dockableRect.width, maxAvailableHeight); then position to ($x, $y)")
            }
        }

        TODO("GPU: convert screen ($x, $y, dockableRect.width, dockableRect.height) to parent-local coords; dockableFloater.setRect(localRect); convert tongue ($dockTongueX, $dockTongueY) to floater-local coords")
    }

    private fun getAllowedRectDefault(rect: Rect) {
        val panel = nonToolbarPanelHandle?.get() ?: return
        TODO("APR: copy panel.rect into rect")
    }
}

class ViewHandle(private val view: View) {
    fun get(): View? = view
}

class DockTongueImage {
    fun getWidth(): Int = TODO("GPU: return tongue image pixel width")
    fun getHeight(): Int = TODO("GPU: return tongue image pixel height")
}

abstract class DockableFloater : Floater("") {
    abstract fun isDocked(): Boolean
    abstract fun setDocked(docked: Boolean)
    abstract fun getUseTongue(): Boolean
    open fun onDockHidden() {}
    open fun onDockShown() {}
}

fun View.getHandle(): ViewHandle = ViewHandle(this)

fun View.calcScreenRect(): Rect = TODO("GPU: compute screen-space bounding rect for this view")

fun View.getRootView(): View = TODO("APR: traverse parent chain to find root view")

fun View.isInVisibleChain(): Boolean = TODO("APR: walk parent chain checking visibility at each level")

fun View.findChild(name: String): View? = getChildByName(name, recurse = true)

fun Floater.getRootView(): View = TODO("APR: return the root view for this floater")

fun Floater.calcScreenRect(): Rect = TODO("GPU: return screen-space rect for this floater")

fun Floater.setDocked(docked: Boolean) { TODO("APR: set docked state on this floater") }

fun Floater.isDocked(): Boolean = TODO("APR: return whether this floater is currently docked")
