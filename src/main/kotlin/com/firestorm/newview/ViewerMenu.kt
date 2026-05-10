package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llmath.*
import com.firestorm.llmessage.*

// ── Type aliases ─────────────────────────────────────────────────────────────

/** A menu action handler returns true if it consumed the event. */
typealias MenuHandler = () -> Boolean

// ── Stub UI types (thin stand-ins for the C++ GL widget hierarchy) ────────────

/** Stub for LLMenuBarGL – top-level menu bar. */
class MenuBarView(val name: String)

/** Stub for LLMenuGL – a single pull-down or context menu. */
class MenuView(val name: String)

/** Stub for LLContextMenu – right-click / pie-slice context menu. */
class ContextMenuView(val name: String)

/**
 * Kotlin equivalent of LLViewerMenuHolderGL.
 *
 * In C++ this is a widget that owns all floating context menus and provides
 * a virtual getMenuRect() so menus know where to clamp themselves on screen.
 * Here it is a plain object; real layout logic would be added later.
 */
class ViewerMenuHolder {
    private var parcelSelection: Any? = null
    private var objectSelection: Any? = null

    fun hideMenus(): Boolean = TODO("Implement menu-hide sweep")

    fun setParcelSelection(sel: Any?) { parcelSelection = sel }
    fun setObjectSelection(sel: Any?) { objectSelection = sel }

    /** Returns the usable menu rectangle (stub). */
    fun getMenuRect(): IntArray = intArrayOf(0, 0, 1920, 1080)
}

// ── Global menu references (mirror of C++ extern globals) ────────────────────

object ViewerMenu {

    // Primary bar / holder
    var menuHolder: ViewerMenuHolder? = null
    var menuBarView: MenuBarView?     = null
    var editMenu: MenuView?           = null
    var popupMenuView: MenuView?      = null
    var loginMenuBarView: MenuBarView? = null

    // 3-D scene context menus
    var menuAvatarSelf: ContextMenuView?       = null
    var menuAvatarOther: ContextMenuView?      = null
    var menuObject: ContextMenuView?           = null
    var menuAttachmentSelf: ContextMenuView?   = null
    var menuAttachmentOther: ContextMenuView?  = null
    var menuLand: ContextMenuView?             = null
    var menuMuteParticle: ContextMenuView?     = null

    // Attachment sub-menus
    var attachSubMenu: MenuView?               = null
    var detachSubMenu: MenuView?               = null
    val attachBodyPartMenus: Array<ContextMenuView?> = arrayOfNulls(9)
    val detachBodyPartMenus: Array<ContextMenuView?> = arrayOfNulls(9)

    // ── Handler registry ─────────────────────────────────────────────────────

    /**
     * Named action handlers.  In C++ these are registered through the
     * LLMenuGL listener / functor pattern.  Here we use a simple map so
     * callers can register and dispatch by string key.
     */
    val handlers: MutableMap<String, MenuHandler> = mutableMapOf()

    fun register(name: String, handler: MenuHandler) {
        handlers[name] = handler
    }

    /**
     * Dispatch a named action.
     * @return true if a handler was found and returned true, false otherwise.
     */
    fun dispatch(name: String): Boolean = handlers[name]?.invoke() ?: false

    // ── Initialisation ───────────────────────────────────────────────────────

    /**
     * Top-level menu initialisation.
     * Mirrors C++ `init_menus()` – loads XML menu definitions and wires up
     * all context menus to [menuHolder].
     */
    fun initMenus(): Unit = TODO("Load XML menu definitions and populate menu references")

    fun initEditMenu(): Unit = TODO("Initialize edit-menu callbacks")

    fun initSpellcheckMenu(): Unit = TODO("Initialize spell-check sub-menu")

    fun initVolumeControlsCallbacks(): Unit = TODO("Register volume-panel callbacks")

    fun cleanupMenus() {
        menuBarView   = null
        menuHolder    = null
        editMenu      = null
        popupMenuView = null
        handlers.clear()
    }

    // ── Built-in action stubs ─────────────────────────────────────────────────

    fun handleObjectEdit(): Unit = TODO("Enter build/edit mode for selected object")

    fun handleObjectTouch(): Unit = TODO("Send touch message to selected in-world object")

    fun handleObjectOpen(): Unit = TODO("Open selected object's inventory")

    fun handleObjectDelete(): Unit = TODO("Request deletion of selected object from server")

    fun handleObjectReturn(): Unit = TODO("Return selected object to owner's inventory")

    fun handleBuy(): Unit = TODO("Initiate buy flow for selected object or its contents")

    fun handleTake(takeSeparate: Boolean = false): Unit = TODO("Take selected object into inventory")

    fun handleTakeCopy(): Unit = TODO("Copy selected object into inventory")

    fun handleBuyLand(): Unit = TODO("Open land-purchase dialog")

    fun handleGoTo(): Boolean = TODO("Teleport agent to clicked location")

    fun handleObjectSitOrStand(): Unit = TODO("Sit on or stand up from selected object")

    fun handleGiveMoney(): Unit = TODO("Open pay-resident dialog")

    fun handleAttachmentEdit(invItemId: LLUUID): Unit = TODO("Edit attachment by inventory item id")

    fun handleAttachmentTouch(invItemId: LLUUID): Unit = TODO("Touch attachment by inventory item id")

    fun handleAvatarFreeze(avatarId: LLUUID): Unit = TODO("Send freeze request to region for avatarId")

    fun handleAvatarEject(avatarId: LLUUID): Unit = TODO("Send eject request to region for avatarId")

    // ── Enable / visibility predicates ───────────────────────────────────────

    fun enableObjectEdit(): Boolean = TODO("Return true when selected object can be edited")

    fun enableObjectDelete(): Boolean = TODO("Return true when selected object can be deleted")

    fun enableObjectTakeCopy(): Boolean = TODO("Return true when a copy can be taken")

    fun enablePayObject(): Boolean = TODO("Return true when selected object accepts payment")

    fun enableBuyObject(): Boolean = TODO("Return true when selected object is for sale")

    fun isAgentMappable(agentId: LLUUID): Boolean = TODO("Return true when agent location can be shown on map")

    fun enableGodFull(): Boolean = TODO("Return true when logged in as full god")

    fun enableGodBasic(): Boolean = TODO("Return true when logged in with any god level")
}
