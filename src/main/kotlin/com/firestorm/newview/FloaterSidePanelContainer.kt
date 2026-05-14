package com.firestorm.newview

// ---------------------------------------------------------------------------
// Stub types for SL UI infrastructure not available on JVM
// ---------------------------------------------------------------------------

open class LLPanelBase : LLView {
    override fun getName(): String = ""
    override fun getRect(): Rect = Rect()
    open fun onOpen(key: Map<String, Any?>): Unit {
        System.err.println("LLPanelBase: onOpen not yet implemented")
    }
}

class LLSideTrayPanelContainer : LLView {
    override fun getName(): String = ""
    override fun getRect(): Rect = Rect()
    fun openPanel(panelName: String, params: Map<String, Any?>): Unit {
        System.err.println("LLSideTrayPanelContainer: openPanel not yet implemented")
    }
    fun getCurrentPanel(): LLPanelBase? = null
}

class LLFloaterViewStub {
    fun getParentFloater(view: LLView): FloaterSidePanelContainer? = null
    fun getZOrder(floater: FloaterSidePanelContainer): Int = 0
}

val gFloaterView = LLFloaterViewStub()

object LLFloaterRegStub {
    fun getTypedSidePanelInstance(name: String): FloaterSidePanelContainer? = null
    fun findTypedSidePanelInstance(name: String): FloaterSidePanelContainer? = null
    fun getFloaterList(name: String): List<FloaterSidePanelContainer> = emptyList()
    fun canShowInstance(floaterName: String, key: Map<String, Any?>): Boolean = false
}

object TransientFloaterMgr {
    fun addControlView(scope: String, view: Any): Unit {
        System.err.println("TransientFloaterMgr: addControlView not yet implemented")
    }
    fun removeControlView(scope: String, view: Any): Unit {
        System.err.println("TransientFloaterMgr: removeControlView not yet implemented")
    }
}

object LLNotificationsUtilFS {
    fun add(name: String, substitutions: Map<String, Any?> = emptyMap(),
            payload: Map<String, Any?> = emptyMap(),
            cb: ((Map<String, Any?>, Map<String, Any?>) -> Unit)? = null): Unit {
        System.err.println("LLNotificationsUtilFS: add not yet implemented")
    }
    fun getSelectedOption(notification: Map<String, Any?>, response: Map<String, Any?>): Int = 0
}

// Appearance-panel stubs
class LLPanelOutfitEdit : LLPanelBase()
class LLPanelEditWearable {
    fun onClose(): Unit {
        System.err.println("LLPanelEditWearable: onClose not yet implemented")
    }
    fun getVisible(): Boolean = false
    fun isDirty(): Boolean = false
}
class LLSidepanelAppearance : LLPanelBase() {
    fun getWearable(): LLPanelEditWearable? = null
    fun showOutfitsInventoryPanel(): Unit {
        System.err.println("LLSidepanelAppearance: showOutfitsInventoryPanel not yet implemented")
    }
}

// ---------------------------------------------------------------------------
// FloaterSidePanelContainer
// ---------------------------------------------------------------------------

/**
 * Wraps a single "main panel" child, formerly hosted in the Side Tray.
 * Clicking this floater does not dismiss transient floaters (e.g. IM windows)
 * so the user can drag inventory items from My Inventory into a docked IM.
 *
 * The RLVa validate-signal mechanism is preserved as a nullable callback list
 * because Kotlin has no boost::signals2 equivalent on JVM.
 *
 * C++ heritage: LLFloaterSidePanelContainer : LLFloater
 */
open class FloaterSidePanelContainer(key: Any, val instanceName: String = "") : LLFloaterBase(key) {

    protected var mainPanel: LLPanelBase? = null

    init {
        TransientFloaterMgr.addControlView("GLOBAL", this)
    }

    fun destroy() {
        TransientFloaterMgr.removeControlView("GLOBAL", this)
    }

    open fun postBuild(): Boolean {
        mainPanel = findChildPanel(MAIN_PANEL_NAME)
        return true
    }

    open fun onOpen(key: Map<String, Any?>) {
        mainPanel?.onOpen(key)
    }

    open fun closeFloater(appQuitting: Boolean = false) {
        if (instanceName == "appearance") {
            val panelOutfitEdit = findChildTyped<LLPanelOutfitEdit>("panel_outfit_edit")
            if (panelOutfitEdit != null) {
                val parent = gFloaterView.getParentFloater(panelOutfitEdit)
                if (parent == this) {
                    val panelAppearance = mainPanel as? LLSidepanelAppearance
                    if (panelAppearance != null) {
                        panelAppearance.getWearable()?.onClose()
                        if (!appQuitting) panelAppearance.showOutfitsInventoryPanel()
                    }
                }
            }
        }

        doCloseFloater(appQuitting)

        // Secondary inventory floaters self-destruct on close so the registry
        // does not keep stale instances alive.
        if ((instanceName == "inventory" && key != null) || instanceName == "secondary_inventory") {
            destroy()
        }
    }

    open fun onClickCloseBtn(appQuitting: Boolean = false) {
        if (!appQuitting && instanceName == "appearance") {
            val panelOutfitEdit = findChildTyped<LLPanelOutfitEdit>("panel_outfit_edit")
            if (panelOutfitEdit != null) {
                val parent = gFloaterView.getParentFloater(panelOutfitEdit)
                if (parent == this) {
                    val panelAppearance = getPanel("appearance") as? LLSidepanelAppearance
                    val editWearable = panelAppearance?.getWearable()
                    if (editWearable != null && editWearable.getVisible() && editWearable.isDirty()) {
                        LLNotificationsUtilFS.add("UsavedWearableChanges", cb = { n, r ->
                            onCloseMsgCallback(n, r)
                        })
                        return
                    }
                }
            }
        }
        closeFloater()
    }

    fun cleanup() = destroy()

    fun openChildPanel(panelName: String, params: Map<String, Any?>): LLPanelBase? {
        val view = findChildView(panelName) ?: return null

        if (!isVisible()) {
            openFloater()
        } else if (!hasFocus()) {
            setFocus(true)
        }

        val container = (view as? LLView)?.let { findSideTrayParent(it) }
        return if (container != null) {
            container.openPanel(panelName, params)
            container.getCurrentPanel()
        } else {
            val panel = view as? LLPanelBase
            panel?.onOpen(params)
            panel
        }
    }

    override fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?,
        accept: Acceptance?, tooltipMsg: StringBuilder
    ): Boolean {
        // no-op
        return false
    }

    protected fun onCloseMsgCallback(notification: Map<String, Any?>, response: Map<String, Any?>) {
        if (LLNotificationsUtilFS.getSelectedOption(notification, response) == 0) {
            closeFloater()
        }
    }

    // Stubs for UI-framework operations
    private fun findChildPanel(name: String): LLPanelBase? = null
    private fun findChildView(name: String): LLView? = null
    private fun findSideTrayParent(view: LLView): LLSideTrayPanelContainer? = null
    private fun isVisible(): Boolean = false
    private fun openFloater(): Unit {
        System.err.println("FloaterSidePanelContainer: openFloater not yet implemented")
    }
    private fun hasFocus(): Boolean = false
    private fun setFocus(v: Boolean): Unit {
        System.err.println("FloaterSidePanelContainer: setFocus not yet implemented")
    }
    private fun doCloseFloater(appQuitting: Boolean): Unit {
        System.err.println("FloaterSidePanelContainer: doCloseFloater not yet implemented")
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> findChildTyped(name: String): T? =
        findChildView(name) as? T

    companion object {
        const val MAIN_PANEL_NAME = "main_panel"

        // RLVa validate-signal: callers register predicates; all must return true for the panel to show.
        private val validateCallbacks: MutableList<(String, String, Map<String, Any?>) -> Boolean> =
            mutableListOf()

        fun setValidateCallback(cb: (String, String, Map<String, Any?>) -> Boolean) {
            validateCallbacks.add(cb)
        }

        fun canShowPanel(floaterName: String, key: Map<String, Any?>): Boolean =
            canShowPanel(floaterName, MAIN_PANEL_NAME, key)

        fun canShowPanel(floaterName: String, panelName: String, key: Map<String, Any?>): Boolean =
            validateCallbacks.isEmpty() || validateCallbacks.all { it(floaterName, panelName, key) }

        fun showPanel(floaterName: String, key: Map<String, Any?>) {
            val floater = LLFloaterRegStub.getTypedSidePanelInstance(floaterName) ?: return
            if ((floater.isVisible() || LLFloaterRegStub.canShowInstance(floaterName, key))
                && canShowPanel(floaterName, key)
            ) {
                floater.openChildPanel(MAIN_PANEL_NAME, key)
            }
        }

        fun showPanel(floaterName: String, panelName: String, key: Map<String, Any?>) {
            val floater = LLFloaterRegStub.getTypedSidePanelInstance(floaterName) ?: return
            if ((floater.isVisible() || LLFloaterRegStub.canShowInstance(floaterName, key))
                && canShowPanel(floaterName, panelName, key)
            ) {
                floater.openChildPanel(panelName, key)
            }
        }

        fun getPanel(floaterName: String, panelName: String = MAIN_PANEL_NAME): LLPanelBase? {
            val floater = LLFloaterRegStub.getTypedSidePanelInstance(floaterName) ?: return null
            return if (panelName == MAIN_PANEL_NAME) floater.mainPanel
            else floater.findChildView(panelName) as? LLPanelBase
        }

        fun findPanel(floaterName: String, panelName: String = MAIN_PANEL_NAME): LLPanelBase? {
            val floater = LLFloaterRegStub.findTypedSidePanelInstance(floaterName) ?: return null
            return if (panelName == MAIN_PANEL_NAME) floater.mainPanel
            else floater.findChildView(panelName) as? LLPanelBase
        }

        fun getTopmostInventoryFloater(): FloaterSidePanelContainer? {
            val primary = LLFloaterRegStub.getFloaterList("inventory")
            val secondary = LLFloaterRegStub.getFloaterList("secondary_inventory")
            val combined = primary + secondary

            var topmost: FloaterSidePanelContainer? = null
            var zMin = Int.MAX_VALUE

            for (floater in combined) {
                if (floater.isVisible()) {
                    val z = gFloaterView.getZOrder(floater)
                    if (z < zMin) {
                        zMin = z
                        topmost = floater
                    }
                }
            }
            return topmost
        }

        // Extension helpers that need access to isVisible() on a specific instance
        private fun FloaterSidePanelContainer.isVisible(): Boolean = false
        private fun FloaterSidePanelContainer.findChildView(name: String): LLView? = null
    }
}
