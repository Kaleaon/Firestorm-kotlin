package com.firestorm.newview

// ---------------------------------------------------------------------------
// Stub types for UI / command infrastructure not available on JVM
// ---------------------------------------------------------------------------

interface LLView {
    fun getName(): String
    fun getRect(): Rect
}

data class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int)

abstract class LLPanel : LLView {
    open fun onOpen(key: Map<String, Any?>): Unit = Unit
}

abstract class LLToolBar : LLView {
    abstract fun setStartDragCallback(cb: (Any, Any, Any) -> Unit)
    abstract fun setHandleDragCallback(cb: (Any, Any, Any, Any) -> Unit)
    abstract fun setHandleDropCallback(cb: (Any, Any, Any, Any, Any) -> Unit)
    abstract fun setButtonEnterCallback(cb: (LLView) -> Unit)
    abstract fun addCommand(id: CommandId)
    abstract fun getCommandsList(): List<CommandId>
    abstract fun enableCommand(id: CommandId, enabled: Boolean)
    abstract fun setTooltipButtonSuffix(suffix: String)
}

data class CommandId(val name: String)

abstract class LLCommand {
    abstract fun labelRef(): String
    abstract fun availableInToybox(): Boolean
    abstract fun id(): CommandId
}

object LLCommandManager {
    fun instance(): LLCommandManager = this
    fun commandCount(): UInt = TODO("APR: use JVM equivalent")
    fun getCommand(index: UInt): LLCommand = TODO("APR: use JVM equivalent")
    fun getCommand(id: CommandId): LLCommand? = TODO("APR: use JVM equivalent")
}

object LLTrans {
    fun getString(key: String): String = TODO("APR: use JVM equivalent")
}

object LLToolBarView {
    fun startDragTool(a: Any, b: Any, c: Any): Unit = TODO("GPU: toolbar drag")
    fun handleDragTool(a: Any, b: Any, c: Any, d: Any): Unit = TODO("GPU: toolbar drag")
    fun handleDropTool(a: Any, b: Any, c: Any, d: Any, e: Any): Unit = TODO("GPU: toolbar drop")
    fun loadDefaultToolbars(): Unit = TODO("APR: use JVM equivalent")
    fun clearAllToolbars(): Unit = TODO("APR: use JVM equivalent")
    fun hasCommand(id: CommandId): ToolbarLocation = TODO("APR: use JVM equivalent")
}

enum class ToolbarLocation { NONE, BOTTOM, LEFT, RIGHT }

object LLNotificationsUtilStub {
    fun add(notification: String): Unit = TODO("APR: use JVM equivalent")
    fun getSelectedOption(notification: Map<String, Any?>, response: Map<String, Any?>): Int =
        TODO("APR: use JVM equivalent")
}

// Drag-and-drop
enum class DragAndDropType
enum class Acceptance

abstract class LLFloaterBase(val key: Any) {
    open fun draw(): Unit = Unit
    open fun postBuild(): Boolean = true
    abstract fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?,
        accept: Acceptance?, tooltipMsg: StringBuilder
    ): Boolean
}

// ---------------------------------------------------------------------------
// FloaterToybox
// ---------------------------------------------------------------------------

/**
 * A palette floater showing all toolbar-eligible commands.
 * Commands are sorted by their localised labels so the list is alphabetised
 * in every locale without extra instrumentation.
 *
 * C++ heritage: LLFloaterToybox : LLFloater
 */
open class FloaterToybox(key: Any) : LLFloaterBase(key) {

    var toolBar: LLToolBar? = null
        private set

    init {
        // C++ registered commit callbacks via mCommitCallbackRegistrar;
        // In Kotlin these are wired directly in postBuild().
    }

    override fun postBuild(): Boolean {
        toolBar = getChildToolBar("toybox_toolbar")

        toolBar?.setStartDragCallback { a, b, c -> LLToolBarView.startDragTool(a, b, c) }
        toolBar?.setHandleDragCallback { a, b, c, d -> LLToolBarView.handleDragTool(a, b, c, d) }
        toolBar?.setHandleDropCallback { a, b, c, d, e -> LLToolBarView.handleDropTool(a, b, c, d, e) }
        toolBar?.setButtonEnterCallback { btn -> onToolBarButtonEnter(btn) }

        val cmdMgr = LLCommandManager.instance()
        val alphabetized = (0u until cmdMgr.commandCount())
            .map { cmdMgr.getCommand(it) }
            .filter { it.availableInToybox() }
            .sortedWith(Comparator { a, b ->
                LLTrans.getString(a.labelRef()).compareTo(LLTrans.getString(b.labelRef()))
            })

        for (cmd in alphabetized) {
            toolBar?.addCommand(cmd.id())
        }
        return true
    }

    override fun draw() {
        val tb = toolBar ?: return

        for (id in tb.getCommandsList()) {
            val commandNotPresent = LLToolBarView.hasCommand(id) == ToolbarLocation.NONE
            tb.enableCommand(id, commandNotPresent)
        }

        super.draw()
    }

    override fun handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?,
        accept: Acceptance?, tooltipMsg: StringBuilder
    ): Boolean {
        val tb = toolBar ?: return false
        val rect = tb.getRect()
        val localX = x - rect.left
        val localY = y - rect.bottom
        return tb.handleDragAndDrop(localX, localY, mask, drop, cargoType, cargoData, accept, tooltipMsg)
    }

    protected fun onBtnClearAll() {
        LLNotificationsUtilStub.add("ConfirmClearAllToybox")
    }

    protected fun onBtnRestoreDefaults() {
        LLNotificationsUtilStub.add("ConfirmRestoreToybox")
    }

    protected fun onToolBarButtonEnter(button: LLView) {
        val commandId = CommandId(button.getName())
        val command = LLCommandManager.instance().getCommand(commandId)
        val suffix = if (command != null) {
            when (LLToolBarView.hasCommand(commandId)) {
                ToolbarLocation.BOTTOM -> LLTrans.getString("Toolbar_Bottom_Tooltip")
                ToolbarLocation.LEFT   -> LLTrans.getString("Toolbar_Left_Tooltip")
                ToolbarLocation.RIGHT  -> LLTrans.getString("Toolbar_Right_Tooltip")
                else -> ""
            }
        } else ""
        toolBar?.setTooltipButtonSuffix(suffix)
    }

    // Stub: in the real viewer this is resolved by the UI framework
    private fun getChildToolBar(name: String): LLToolBar = TODO("GPU: child widget lookup '$name'")

    private fun LLToolBar.handleDragAndDrop(
        x: Int, y: Int, mask: Int, drop: Boolean,
        cargoType: DragAndDropType, cargoData: Any?,
        accept: Acceptance?, tooltipMsg: StringBuilder
    ): Boolean = TODO("GPU: toolbar drag-and-drop dispatch")

    companion object {
        fun finishRestoreToybox(notification: Map<String, Any?>, response: Map<String, Any?>): Boolean {
            if (LLNotificationsUtilStub.getSelectedOption(notification, response) == 0) {
                LLToolBarView.loadDefaultToolbars()
            }
            return false
        }

        fun finishClearAllToybox(notification: Map<String, Any?>, response: Map<String, Any?>): Boolean {
            if (LLNotificationsUtilStub.getSelectedOption(notification, response) == 0) {
                LLToolBarView.clearAllToolbars()
            }
            return false
        }
    }
}
