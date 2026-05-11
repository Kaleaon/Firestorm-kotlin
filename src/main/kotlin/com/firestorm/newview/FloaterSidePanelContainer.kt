package com.firestorm.newview

// ---------------------------------------------------------------------------
// Stub types for SL UI infrastructure not available on JVM
// ---------------------------------------------------------------------------

open class LLPanelBase : LLView {
    override fun getName(): String = TODO("GPU: panel name")
    override fun getRect(): Rect = TODO("GPU: panel rect")
    open fun onOpen(key: Map<String, Any?>): Unit = TODO("GPU: panel open")
}

class LLSideTrayPanelContainer : LLView {
    override fun getName(): String = TODO("GPU: side tray panel container name")
    override fun getRect(): Rect = TODO("GPU: side tray panel container rect")
    fun openPanel(panelName: String, params: Map<String, Any?>): Unit =
        TODO("GPU: side tray open panel")
    fun getCurrentPanel(): LLPanelBase? = TODO("GPU: side tray current panel")
}

class LLFloaterViewStub {
    fun getParentFloater(view: LLView): FloaterSidePanelContainer? = TODO("GPU: parent floater query")
    fun getZOrder(floater: FloaterSidePanelContainer): Int = TODO("GPU: floater z-order")
}

val gFloaterView = LLFloaterViewStub()

object LLFloaterRegStub {
    fun getTypedSidePanelInstance(name: String): FloaterSidePanelContainer? = TODO("APR: use JVM equivalent")
    fun findTypedSidePanelInstance(name: String): FloaterSidePanelContainer? = TODO("APR: use JVM equivalent")
    fun getFloaterList(name: String): List<FloaterSidePanelContainer> = TODO("APR: use JVM equivalent")
    fun canShowInstance(floaterName: String, key: Map<String, Any?>): Boolean = TODO("APR: use JVM equivalent")
}

object TransientFloaterMgr {
    fun addControlView(scope: String, view: Any): Unit = TODO("APR: use JVM equivalent")
    fun removeControlView(scope: String, view: Any): Unit = TODO("APR: use JVM equivalent")
}

object LLNotificationsUtilFS {
    fun add(name: String, substitutions: Map<String, Any?> = emptyMap(),
            payload: Map<String, Any?> = emptyMap(),
            cb: ((Map<String, Any?>, Map<String, Any?>) -> Unit)? = null): Unit =
        TODO("APR: use JVM equivalent")
    fun getSelectedOption(notification: Map<String, Any?>, response: Map<String, Any?>): Int =
        TODO("APR: use JVM equivalent")
}

// Appearance-panel stubs
class LLPanelOutfitEdit : LLPanelBase()
class LLPanelEditWearable {
    fun onClose(): Unit = TODO("GPU: wearable editor close")
    fun getVisible(): Boolean = TODO("GPU: visibility query")
    fun isDirty(): Boolean = TODO("GPU: wearable dirty flag")
}
class LLSidepanelAppearance : LLPanelBase() {
    fun getWearable(): LLPanelEditWearable? = TODO("GPU: wearable panel query")
    fun showOutfitsInventoryPanel(): Unit = TODO("GPU: panel navigation")
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
    ): Boolean = TODO("GPU: side panel container drag-and-drop")

    protected fun onCloseMsgCallback(notification: Map<String, Any?>, response: Map<String, Any?>) {
        if (LLNotificationsUtilFS.getSelectedOption(notification, response) == 0) {
            closeFloater()
        }
    }

    // Stubs for UI-framework operations
    private fun findChildPanel(name: String): LLPanelBase? = TODO("GPU: child panel lookup '$name'")
    private fun findChildView(name: String): LLView? = TODO("GPU: child view lookup '$name'")
    private fun findSideTrayParent(view: LLView): LLSideTrayPanelContainer? = TODO("GPU: parent container query")
    private fun isVisible(): Boolean = TODO("GPU: visibility query")
    private fun openFloater(): Unit = TODO("GPU: floater open")
    private fun hasFocus(): Boolean = TODO("GPU: focus query")
    private fun setFocus(v: Boolean): Unit = TODO("GPU: focus mutation")
    private fun doCloseFloater(appQuitting: Boolean): Unit = TODO("GPU: floater close")

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
        private fun FloaterSidePanelContainer.isVisible(): Boolean = TODO("GPU: visibility query")
        private fun FloaterSidePanelContainer.findChildView(name: String): LLView? =
            TODO("GPU: child view lookup '$name'")
    }
}
