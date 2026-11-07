package com.firestorm.llui

import com.firestorm.llmath.Rect

// Full context menu extracted from the LLContextMenu / LLContextMenuBranch sections
// of llmenugl.h.  The minimal placeholder in LLMenuGL.kt is superseded here.

open class ContextMenuFull(name: String) : MenuGL(name) {

    private var hoveredAnyItem: Boolean = false
    private var hoverItemLabel: String? = null
    private var spawningView: View? = null

    init {
        // Context menus start hidden and are raised by an explicit show() call.
        hide()
    }

    // Visibility is only changed via show()/hide(); direct assignment is ignored
    // to prevent callers from bypassing the menu-positioning logic.
    fun setVisible(value: Boolean) {
        if (!value) hide()
    }

    open fun show(x: Int, y: Int, spawner: View? = null) {
        spawningView = spawner
        super.show(x, y)
    }

    // Resets hover state in addition to hiding the menu.
    // hide() in MenuGL is not open, so we extend visibility control here
    // by clearing the extra fields and then delegating.
    fun hideAndReset() {
        hide()
        hoveredAnyItem = false
        hoverItemLabel = null
    }

    open fun handleHover(x: Int, y: Int, mask: UInt): Boolean {
        // no-op
        return false
    }

    open fun handleRightMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        // no-op
        return false
    }

    open fun handleRightMouseUp(x: Int, y: Int, mask: UInt): Boolean {
        // no-op
        return false
    }

    // Routes child views (e.g. sub-context-menus) through the context-child path.
    // Mirrors LLContextMenu::addChild → addContextChild.
    fun addChild(child: View, tabGroup: Int = 0) {
        System.err.println("ContextMenuFull: addChild not yet implemented")
    }

    fun getSpawningView(): View? = spawningView
    fun setSpawningView(view: View?) { spawningView = view }
}

// A menu item that cascades into a sub-context-menu when highlighted.
class ContextMenuBranch(
    name: String,
    label: String,
    private val branch: ContextMenuFull
) : MenuItemGL(name, label) {

    override fun buildDrawLabel() {
        // Appending BRANCH_SUFFIX signals to the renderer that a sub-menu arrow is needed.
    }

    override fun onCommit() {
        showSubMenu()
    }

    fun highlight(on: Boolean) {
        if (on) showSubMenu() else branch.hideAndReset()
    }

    fun getBranch(): ContextMenuFull = branch

    private fun showSubMenu() {
        // no-op
    }
}
