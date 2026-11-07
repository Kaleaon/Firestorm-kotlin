package com.firestorm.llui

import com.firestorm.llmath.Rect

open class View(
    var name: String,
    var rect: Rect = Rect()
) {
    var visible: Boolean = true
    var enabled: Boolean = true
    var parent: View? = null
    val children: MutableList<View> = mutableListOf()

    enum class ESnapType { SNAP_NONE, SNAP_PARENT, SNAP_SIBLINGS }

    companion object {
        const val FOLLOWS_NONE: UInt = 0x00u
        const val FOLLOWS_LEFT: UInt = 0x01u
        const val FOLLOWS_RIGHT: UInt = 0x02u
        const val FOLLOWS_TOP: UInt = 0x10u
        const val FOLLOWS_BOTTOM: UInt = 0x20u
        const val FOLLOWS_ALL: UInt = 0x33u
    }

    fun addChild(child: View) {
        child.parent = this
        children.add(child)
    }

    fun removeChild(child: View) {
        if (children.remove(child)) {
            child.parent = null
        }
    }

    fun getChildByName(name: String, recurse: Boolean = false): View? {
        for (child in children) {
            if (child.name == name) return child
            if (recurse) {
                val found = child.getChildByName(name, recurse = true)
                if (found != null) return found
            }
        }
        return null
    }

    open fun draw() {
        if (!visible) return
        for (child in children) {
            child.draw()
        }
    }

    open fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean = false
    open fun handleMouseUp(x: Int, y: Int, mask: UInt): Boolean = false
    open fun handleHover(x: Int, y: Int, mask: UInt): Boolean = false
    open fun handleKey(key: Int, mask: UInt, called: Boolean): Boolean = false

    fun localToScreen(local: Rect): Rect {
        var offsetX = 0
        var offsetY = 0
        var current: View? = parent
        while (current != null) {
            offsetX += current.rect.left
            offsetY += current.rect.bottom
            current = current.parent
        }
        return Rect(
            local.left + offsetX,
            local.top + offsetY,
            local.right + offsetX,
            local.bottom + offsetY
        )
    }

    fun screenToLocal(screen: Rect): Rect {
        var offsetX = 0
        var offsetY = 0
        var current: View? = parent
        while (current != null) {
            offsetX += current.rect.left
            offsetY += current.rect.bottom
            current = current.parent
        }
        return Rect(
            screen.left - offsetX,
            screen.top - offsetY,
            screen.right - offsetX,
            screen.bottom - offsetY
        )
    }

    fun setVisible(visible: Boolean) {
        this.visible = visible
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    open fun reshape(width: Int, height: Int, called: Boolean = true) {
        rect = Rect(rect.left, rect.bottom + height, rect.left + width, rect.bottom)
    }
}
