package com.firestorm.llui

import com.firestorm.llmath.Color4
import com.firestorm.llmath.Rect

open class ContainerView(
    name: String,
    rect: Rect = Rect(),
    private var label: String = "",
    var showLabel: Boolean = false,
    displayChildrenInitial: Boolean = true,
    var backgroundVisible: Boolean = true,
    var backgroundColor: Color4 = Color4(0f, 0f, 0f, 0.25f)
) : View(name, rect) {

    var displayChildren: Boolean = displayChildrenInitial
        set(value) {
            field = value
            for (child in children) child.visible = value
        }

    var scrollContainer: ScrollContainer? = null

    fun postBuild(): Boolean {
        displayChildren = displayChildren
        reshape(rect.width, rect.height, false)
        return true
    }

    fun addChildToBack(child: View) {
        addChild(child)
        sendChildToBack(child)
    }

    fun handleDoubleClick(x: Int, y: Int, mask: UInt): Boolean = handleMouseDown(x, y, mask)

    override fun handleMouseDown(x: Int, y: Int, mask: UInt): Boolean {
        var handled = false
        if (displayChildren) {
            handled = childrenHandleMouseDown(x, y, mask)
        }
        if (!handled && showLabel && y >= rect.height - 10) {
            displayChildren = !displayChildren
            reshape(rect.width, rect.height, false)
            handled = true
        }
        return handled
    }

    override fun handleMouseUp(x: Int, y: Int, mask: UInt): Boolean {
        if (displayChildren) {
            return childrenHandleMouseUp(x, y, mask)
        }
        return false
    }

    override fun draw() {
        if (!visible) return
        if (backgroundVisible) {
            // no-op
        }
        if (showLabel) {
            // no-op
        }
        super.draw()
    }

    override fun reshape(width: Int, height: Int, called: Boolean) {
        val sc = scrollContainer
        val scrollerRect = if (sc != null) sc.getContentWindowRect()
                           else Rect(0, 0, width, 0)

        arrange(scrollerRect.width, scrollerRect.height, called)

        if (sc != null) {
            val newRect = sc.getContentWindowRect()
            if (newRect.width != scrollerRect.width || newRect.height != scrollerRect.height) {
                arrange(newRect.width, newRect.height, called)
            }
        }
    }

    fun getRequiredRect(): Rect {
        var totalHeight = if (showLabel) 20 else 0
        if (displayChildren) {
            for (child in children) totalHeight += child.rect.height + 2
        }
        return Rect(0, totalHeight, 0, 0)
    }

    fun setLabel(text: String) { label = text }
    fun getDisplayChildren(): Boolean = displayChildren

    private fun arrange(width: Int, height: Int, calledFromParent: Boolean) {
        var totalHeight = if (showLabel) 20 else 0
        if (displayChildren) {
            for (child in children) totalHeight += child.rect.height + 2
        }
        if (totalHeight < height) totalHeight = height

        val anchorTop = followsTop()
        rect = if (anchorTop) {
            Rect(rect.left, rect.top, rect.left + width, rect.top - totalHeight)
        } else {
            Rect(rect.left, rect.bottom + totalHeight, rect.left + width, rect.bottom)
        }

        val left = 10
        val right = width - 2
        var top = totalHeight - if (showLabel) 20 else 0
        var bottom = top

        if (displayChildren) {
            for (child in children) {
                bottom -= child.rect.height
                child.rect = Rect(left, bottom + child.rect.height, right, bottom)
                child.reshape(right - left, child.rect.height)
                top = bottom - 2
                bottom = top
            }
        }

        if (!calledFromParent) {
            parent?.reshape(parent!!.rect.width, parent!!.rect.height, false)
        }
    }

    private fun followsTop(): Boolean = true

    private fun sendChildToBack(child: View) {
        children.remove(child)
        children.add(0, child)
    }

    private fun childrenHandleMouseDown(x: Int, y: Int, mask: UInt): Boolean =
        children.any { it.handleMouseDown(x, y, mask) }

    private fun childrenHandleMouseUp(x: Int, y: Int, mask: UInt): Boolean =
        children.any { it.handleMouseUp(x, y, mask) }
}
