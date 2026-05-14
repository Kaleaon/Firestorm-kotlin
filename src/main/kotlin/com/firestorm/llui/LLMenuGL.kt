package com.firestorm.llui

import com.firestorm.llmath.Rect
import com.firestorm.llcommon.*

open class MenuItemGL(
    name: String,
    var label: String = name
) {
    var enabled: Boolean = true
    var visible: Boolean = true
    var highlighted: Boolean = false
    var jumpKey: Int = 0

    var onClick: (() -> Unit)? = null
    var onEnable: (() -> Boolean)? = null

    open fun buildDrawLabel() {}

    open fun onCommit() {
        if (enabled) onClick?.invoke()
    }

    fun updateEnabled() {
        onEnable?.let { enabled = it() }
    }
}

class MenuItemSeparatorGL : MenuItemGL("separator", "") {
    override fun buildDrawLabel() {}
}

open class MenuGL(val name: String) {

    data class MenuItem(
        val label: String,
        val callback: (() -> Unit)? = null,
        var enabled: Boolean = true,
        var visible: Boolean = true
    )

    private val items: MutableList<MenuItem> = mutableListOf()

    var backgroundVisible: Boolean = true
    var dropShadowed: Boolean = true
    var scrollable: Boolean = false
    var maxScrollableItems: UInt = UInt.MAX_VALUE
    var horizontalLayout: Boolean = false

    var visible: Boolean = false
        private set

    var posX: Int = 0
        private set
    var posY: Int = 0
        private set

    companion object {
        const val BOOLEAN_TRUE_PREFIX: String = "✓ "
        const val BRANCH_SUFFIX: String = " >"
        const val ARROW_UP: String = "^"
        const val ARROW_DOWN: String = "v"

        var keyboardMode: Boolean = false
    }

    fun addItem(label: String, callback: (() -> Unit)? = null): Int {
        items.add(MenuItem(label, callback))
        return items.size - 1
    }

    fun addSeparator() {
        items.add(MenuItem("", null))
    }

    fun removeItem(index: Int): Boolean {
        if (index < 0 || index >= items.size) return false
        items.removeAt(index)
        return true
    }

    fun removeItem(label: String): Boolean {
        val idx = items.indexOfFirst { it.label == label }
        return if (idx >= 0) removeItem(idx) else false
    }

    fun empty() {
        items.clear()
    }

    fun getItemCount(): Int = items.size

    fun getItem(index: Int): MenuItem? =
        if (index in items.indices) items[index] else null

    fun getItem(label: String): MenuItem? =
        items.firstOrNull { it.label == label }

    fun setItemEnabled(label: String, enable: Boolean) {
        items.firstOrNull { it.label == label }?.let {
            val idx = items.indexOf(it)
            items[idx] = it.copy(enabled = enable)
        }
    }

    fun setItemVisible(label: String, isVisible: Boolean) {
        items.firstOrNull { it.label == label }?.let {
            val idx = items.indexOf(it)
            items[idx] = it.copy(visible = isVisible)
        }
    }

    fun show(x: Int, y: Int) {
        posX = x
        posY = y
        visible = true
        // no-op
    }

    fun hide() {
        visible = false
    }

    fun draw() {
        if (!visible) return
        TODO("GL: draw menu background and ${items.size} items at ($posX,$posY)")
    }
}

open class ContextMenu(name: String) : MenuGL(name) {
    fun show(x: Int, y: Int, spawningView: View? = null) {
        super.show(x, y)
    }
}

open class MenuBarGL(name: String) : MenuGL(name) {
    init {
        horizontalLayout = true
    }

    fun draw(availableWidth: Int) {
        if (!visible) return
        TODO("GL: draw horizontal menu bar items up to width $availableWidth")
    }
}

open class MenuHolderGL(
    name: String,
    rect: Rect = Rect()
) : Panel(name, rect) {

    fun hideMenus(): Boolean {
        for (child in children) {
            (child as? View)?.setVisible(false)
        }
        return true
    }

    fun hasVisibleMenu(): Boolean = children.any { it.visible }

    override fun draw() {
        if (!visible) return
        TODO("GL: draw all visible child menus")
    }
}
