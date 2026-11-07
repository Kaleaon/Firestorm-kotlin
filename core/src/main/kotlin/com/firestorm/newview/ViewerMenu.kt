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

    fun hideMenus(): Boolean {
        System.err.println("ViewerMenuHolder: menu-hide sweep not yet implemented")
        return false
    }

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
    fun initMenus(): Unit {
        System.err.println("ViewerMenu: load XML menu definitions and populate menu references not yet implemented")
    }

    fun initEditMenu(): Unit {
        System.err.println("ViewerMenu: initialize edit-menu callbacks not yet implemented")
    }

    fun initSpellcheckMenu(): Unit {
        System.err.println("ViewerMenu: initialize spell-check sub-menu not yet implemented")
    }

    fun initVolumeControlsCallbacks(): Unit {
        System.err.println("ViewerMenu: register volume-panel callbacks not yet implemented")
    }

    fun cleanupMenus() {
        menuBarView   = null
        menuHolder    = null
        editMenu      = null
        popupMenuView = null
        handlers.clear()
    }

    // ── Built-in action stubs ─────────────────────────────────────────────────

    fun handleObjectEdit(): Unit {
        System.err.println("ViewerMenu: enter build/edit mode for selected object not yet implemented")
    }

    fun handleObjectTouch(): Unit {
        System.err.println("ViewerMenu: send touch message to selected in-world object not yet implemented")
    }

    fun handleObjectOpen(): Unit {
        System.err.println("ViewerMenu: open selected object's inventory not yet implemented")
    }

    fun handleObjectDelete(): Unit {
        System.err.println("ViewerMenu: request deletion of selected object from server not yet implemented")
    }

    fun handleObjectReturn(): Unit {
        System.err.println("ViewerMenu: return selected object to owner's inventory not yet implemented")
    }

    fun handleBuy(): Unit {
        System.err.println("ViewerMenu: initiate buy flow for selected object or its contents not yet implemented")
    }

    fun handleTake(takeSeparate: Boolean = false): Unit {
        System.err.println("ViewerMenu: take selected object into inventory not yet implemented")
    }

    fun handleTakeCopy(): Unit {
        System.err.println("ViewerMenu: copy selected object into inventory not yet implemented")
    }

    fun handleBuyLand(): Unit {
        System.err.println("ViewerMenu: open land-purchase dialog not yet implemented")
    }

    fun handleGoTo(): Boolean {
        System.err.println("ViewerMenu: teleport agent to clicked location not yet implemented")
        return false
    }

    fun handleObjectSitOrStand(): Unit {
        System.err.println("ViewerMenu: sit on or stand up from selected object not yet implemented")
    }

    fun handleGiveMoney(): Unit {
        System.err.println("ViewerMenu: open pay-resident dialog not yet implemented")
    }

    fun handleAttachmentEdit(invItemId: LLUUID): Unit {
        System.err.println("ViewerMenu: edit attachment by inventory item id not yet implemented")
    }

    fun handleAttachmentTouch(invItemId: LLUUID): Unit {
        System.err.println("ViewerMenu: touch attachment by inventory item id not yet implemented")
    }

    fun handleAvatarFreeze(avatarId: LLUUID): Unit {
        System.err.println("ViewerMenu: send freeze request to region for avatarId not yet implemented")
    }

    fun handleAvatarEject(avatarId: LLUUID): Unit {
        System.err.println("ViewerMenu: send eject request to region for avatarId not yet implemented")
    }

    // ── Enable / visibility predicates ───────────────────────────────────────

    fun enableObjectEdit(): Boolean {
        System.err.println("ViewerMenu: enableObjectEdit not yet implemented")
        return false
    }

    fun enableObjectDelete(): Boolean {
        System.err.println("ViewerMenu: enableObjectDelete not yet implemented")
        return false
    }

    fun enableObjectTakeCopy(): Boolean {
        System.err.println("ViewerMenu: enableObjectTakeCopy not yet implemented")
        return false
    }

    fun enablePayObject(): Boolean {
        System.err.println("ViewerMenu: enablePayObject not yet implemented")
        return false
    }

    fun enableBuyObject(): Boolean {
        System.err.println("ViewerMenu: enableBuyObject not yet implemented")
        return false
    }

    fun isAgentMappable(agentId: LLUUID): Boolean {
        System.err.println("ViewerMenu: isAgentMappable not yet implemented")
        return false
    }

    fun enableGodFull(): Boolean {
        System.err.println("ViewerMenu: enableGodFull not yet implemented")
        return false
    }

    fun enableGodBasic(): Boolean {
        System.err.println("ViewerMenu: enableGodBasic not yet implemented")
        return false
    }
}
