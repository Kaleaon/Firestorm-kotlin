package com.firestorm.llui

import kotlin.math.min
import kotlin.math.max
import kotlin.math.roundToInt

private const val VERTICAL_MULTIPLE = 16
private const val AUTO_SCROLL_RATE_ACCEL = 120f

enum class ScrollOrientation { VERTICAL, HORIZONTAL }

open class Scrollbar(
    val orientation: ScrollOrientation,
    var docSize: Int = 0,
    var docPos: Int = 0,
    var pageSize: Int = 0,
    var stepSize: Int = VERTICAL_MULTIPLE,
    var visible: Boolean = false,
    var enabled: Boolean = true,
    var thickness: Int = 0
) {
    val docPosMax: Int get() = max(0, docSize - pageSize)

    fun isAtBeginning(): Boolean = docPos <= 0
    fun isAtEnd(): Boolean = docPos >= docPosMax

    fun setDocPos(pos: Int) {
        docPos = pos.coerceIn(0, docPosMax)
    }

    fun setDocSize(size: Int) { docSize = size }
    fun setPageSize(size: Int) { pageSize = size }
    fun setThickness(t: Int) { thickness = t }

    fun pageUp(overlap: Int = 0) {
        setDocPos(docPos - (pageSize - overlap))
    }

    fun pageDown(overlap: Int = 0) {
        setDocPos(docPos + (pageSize - overlap))
    }

    fun getDocPos(): Int = docPos
    fun getDocSize(): Int = docSize
    fun getVisible(): Boolean = visible
    fun getEnabled(): Boolean = enabled
    fun hasMouseCapture(): Boolean = false
    fun reshape(width: Int, height: Int, fromParent: Boolean = false) {}
    fun getRect(): Rect = Rect()
    fun setRect(r: Rect) {}

    fun handleKeyHere(key: Key, mask: Int): Boolean = false
    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (!visible || !enabled) return false
        setDocPos(docPos + clicks * stepSize)
        return true
    }
    fun handleScrollHWheel(x: Int, y: Int, clicks: Int): Boolean {
        if (!visible || !enabled) return false
        setDocPos(docPos + clicks * stepSize)
        return true
    }
}

open class ViewBorder(var visible: Boolean = false) {
    fun getVisible(): Boolean = visible
    fun setVisible(v: Boolean) { visible = v }
    fun getBorderWidth(): Int = if (visible) 1 else 0
    fun setKeyboardFocusHighlight(v: Boolean) {}
}

open class ScrollContainer(
    isOpaque: Boolean = false,
    backgroundColor: Int = 0,
    borderVisible: Boolean = false,
    hideScrollbar: Boolean = false,
    ignoreArrowKeys: Boolean = false,
    minAutoScrollRate: Float = 100f,
    maxAutoScrollRate: Float = 1000f,
    maxAutoScrollZone: UInt = 16u,
    reserveScrollCorner: Boolean = false,
    scrollbarSize: Int = 16,
    scrollCallback: ((Int) -> Unit)? = null
) : UiCtrl() {

    protected var scrolledView: View? = null

    private val scrollbar = arrayOf(
        Scrollbar(ScrollOrientation.VERTICAL, stepSize = VERTICAL_MULTIPLE),
        Scrollbar(ScrollOrientation.HORIZONTAL, stepSize = VERTICAL_MULTIPLE)
    )

    private var size: Int = scrollbarSize
    private val isOpaque: Boolean = isOpaque
    private val backgroundColor: Int = backgroundColor
    private var innerRect: Rect = Rect()
    private val border: ViewBorder = ViewBorder(borderVisible)
    private var reserveScrollCorner: Boolean = reserveScrollCorner
    private var autoScrolling: Boolean = false
    private var autoScrollRate: Float = 0f
    private val minAutoScrollRate: Float = minAutoScrollRate
    private val maxAutoScrollRate: Float = maxAutoScrollRate
    private val maxAutoScrollZone: UInt = maxAutoScrollZone
    private val hideScrollbar: Boolean = hideScrollbar
    private val ignoreArrowKeys: Boolean = ignoreArrowKeys

    fun setBorderVisible(b: Boolean) {
        border.setVisible(b)
        innerRect = getLocalRect().apply { stretch(-getBorderWidth()) }
    }

    fun scrollToShowRect(rect: Rect, constraint: Rect) {
        val scrolledView = scrolledView ?: return
        val contentWindow = getContentWindowRect()
        val scrolledRect = scrolledView.getRect()

        val constrained = rect.copy().also { r ->
            r.bottom = max(r.bottom, r.top - constraint.height)
            r.right = min(r.right, r.left + constraint.width)
        }

        val allowable = Rect(
            left = constrained.right - constraint.right,
            bottom = constrained.bottom - constraint.bottom,
            right = constrained.left - constraint.left,
            top = constrained.top - constraint.top
        ).also { it.translate(0, contentWindow.height) }

        val vertPos = scrollbar[ScrollOrientation.VERTICAL.ordinal].getDocPos()
            .coerceIn(scrollbar[ScrollOrientation.VERTICAL.ordinal].getDocSize() - allowable.top,
                       scrollbar[ScrollOrientation.VERTICAL.ordinal].getDocSize() - allowable.bottom)

        scrollbar[ScrollOrientation.VERTICAL.ordinal].setDocSize(scrolledRect.height)
        scrollbar[ScrollOrientation.VERTICAL.ordinal].setPageSize(contentWindow.height)
        scrollbar[ScrollOrientation.VERTICAL.ordinal].setDocPos(vertPos)

        val horizPos = scrollbar[ScrollOrientation.HORIZONTAL.ordinal].getDocPos()
            .coerceIn(allowable.left, allowable.right)

        scrollbar[ScrollOrientation.HORIZONTAL.ordinal].setDocSize(scrolledRect.width)
        scrollbar[ScrollOrientation.HORIZONTAL.ordinal].setPageSize(contentWindow.width)
        scrollbar[ScrollOrientation.HORIZONTAL.ordinal].setDocPos(horizPos)

        updateScroll()
    }

    fun scrollToShowRect(rect: Rect) {
        scrollToShowRect(rect, Rect(0, innerRect.height, innerRect.width, 0))
    }

    fun setReserveScrollCorner(b: Boolean) { reserveScrollCorner = b }

    fun getVisibleContentRect(): Rect {
        updateScroll()
        val visible = getContentWindowRect()
        val contents = scrolledView?.getRect() ?: return visible
        visible.translate(-contents.left, -contents.bottom)
        return visible
    }

    fun getContentWindowRect(): Rect {
        updateScroll()
        val (visW, visH, showH, showV) = calcVisibleSize()
        val bw = getBorderWidth()
        val bottom = if (showH) scrollbar[ScrollOrientation.HORIZONTAL.ordinal].getRect().top else bw
        return Rect(left = bw, top = bottom + visH, right = bw + visW, bottom = bottom)
    }

    open fun getScrolledViewRect(): Rect = scrolledView?.getRect() ?: Rect()

    fun pageUp(overlap: Int = 0) {
        scrollbar[ScrollOrientation.VERTICAL.ordinal].pageUp(overlap)
        updateScroll()
    }

    fun pageDown(overlap: Int = 0) {
        scrollbar[ScrollOrientation.VERTICAL.ordinal].pageDown(overlap)
        updateScroll()
    }

    fun goToTop() {
        scrollbar[ScrollOrientation.VERTICAL.ordinal].setDocPos(0)
        updateScroll()
    }

    fun goToBottom() {
        scrollbar[ScrollOrientation.VERTICAL.ordinal].setDocPos(scrollbar[ScrollOrientation.VERTICAL.ordinal].getDocSize())
        updateScroll()
    }

    fun isAtTop(): Boolean = scrollbar[ScrollOrientation.VERTICAL.ordinal].isAtBeginning()
    fun isAtBottom(): Boolean = scrollbar[ScrollOrientation.VERTICAL.ordinal].isAtEnd()

    fun getDocPosVertical(): Int = scrollbar[ScrollOrientation.VERTICAL.ordinal].getDocPos()
    fun getDocPosHorizontal(): Int = scrollbar[ScrollOrientation.HORIZONTAL.ordinal].getDocPos()

    fun getBorderWidth(): Int = border.getBorderWidth()

    fun getScrollbar(orientation: ScrollOrientation): Scrollbar = scrollbar[orientation.ordinal]

    open fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        innerRect = getLocalRect().apply { stretch(-getBorderWidth()) }
        val scrolledView = scrolledView ?: return
        val scrolledRect = scrolledView.getRect()
        val (visW, visH, _, _) = calcVisibleSize()
        scrollbar[ScrollOrientation.VERTICAL.ordinal].setDocSize(scrolledRect.height)
        scrollbar[ScrollOrientation.VERTICAL.ordinal].setPageSize(visH)
        scrollbar[ScrollOrientation.HORIZONTAL.ordinal].setDocSize(scrolledRect.width)
        scrollbar[ScrollOrientation.HORIZONTAL.ordinal].setPageSize(visW)
        updateScroll()
    }

    open fun handleKeyHere(key: Key, mask: Int): Boolean {
        if (ignoreArrowKeys) {
            when (key) {
                Key.LEFT, Key.RIGHT, Key.UP, Key.DOWN,
                Key.PAGE_UP, Key.PAGE_DOWN, Key.HOME, Key.END -> return false
                else -> {}
            }
        }
        if (scrolledView?.handleKeyHere(key, mask) == true) return true
        for (sb in scrollbar) {
            if (sb.handleKeyHere(key, mask)) {
                updateScroll()
                return true
            }
        }
        return false
    }

    open fun handleUnicodeCharHere(char: Char): Boolean =
        scrolledView?.handleUnicodeCharHere(char) ?: false

    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        val vertical = scrollbar[ScrollOrientation.VERTICAL.ordinal]
        if (vertical.getVisible() && vertical.getEnabled()) {
            vertical.handleScrollWheel(0, 0, clicks)
            updateScroll()
            return true
        }
        val horizontal = scrollbar[ScrollOrientation.HORIZONTAL.ordinal]
        if (horizontal.getVisible() && horizontal.getEnabled() && horizontal.handleScrollWheel(0, 0, clicks)) {
            updateScroll()
            return true
        }
        return false
    }

    open fun handleScrollHWheel(x: Int, y: Int, clicks: Int): Boolean {
        val horizontal = scrollbar[ScrollOrientation.HORIZONTAL.ordinal]
        if (horizontal.getVisible() && horizontal.getEnabled() && horizontal.handleScrollHWheel(0, 0, clicks)) {
            updateScroll()
            return true
        }
        return false
    }

    open fun handleDragAndDrop(x: Int, y: Int, mask: Int, drop: Boolean, cargoType: Int, cargoData: Any?): Boolean {
        autoScroll(x, y)
        return true
    }

    open fun draw() {
        if (autoScrolling) {
            autoScrollRate = min(autoScrollRate + frameDeltaTime() * AUTO_SCROLL_RATE_ACCEL, maxAutoScrollRate)
        } else {
            autoScrollRate = minAutoScrollRate
        }
        autoScrolling = false

        if (!hasFocus() && (scrollbar[0].hasMouseCapture() || scrollbar[1].hasMouseCapture())) {
            focusFirstItem()
        }

        if (isOpaque) {
            TODO("GPU: draw background rect with backgroundColor")
        }

        scrolledView?.let {
            updateScroll()
            val (visW, visH, showH, showV) = calcVisibleSize()
            TODO("GPU: clip and draw scrolled view within visible region")
        }

        if (border.getVisible()) {
            border.setKeyboardFocusHighlight(childHasKeyboardFocus())
        }

        TODO("GPU: draw all children except scrolledView")
    }

    open fun addChild(view: View, tabGroup: Int = 0): Boolean {
        if (scrolledView == null) {
            scrolledView = view
        }
        return true
    }

    fun canAutoScroll(x: Int, y: Int): Boolean {
        if (autoScrolling) return true
        return autoScrollInternal(x, y, doScroll = false)
    }

    fun autoScroll(x: Int, y: Int): Boolean = autoScrollInternal(x, y, doScroll = true)

    private fun autoScrollInternal(x: Int, y: Int, doScroll: Boolean): Boolean {
        var scrolling = false
        val hBar = scrollbar[ScrollOrientation.HORIZONTAL.ordinal]
        val vBar = scrollbar[ScrollOrientation.VERTICAL.ordinal]

        if (!hBar.getVisible() && !vBar.getVisible()) return false

        val speed = (autoScrollRate * frameDeltaTime()).roundToInt()
        val innerW = innerRect.width
        val innerH = innerRect.height
        val scrollZoneW = min(innerW / 3, maxAutoScrollZone.toInt())
        val scrollZoneH = min(innerH / 3, maxAutoScrollZone.toInt())

        if (hBar.getVisible()) {
            if (x < innerRect.left + scrollZoneW && hBar.getDocPos() > 0) {
                if (doScroll) { hBar.setDocPos(hBar.getDocPos() - speed); autoScrolling = true }
                scrolling = true
            }
            if (x > innerRect.right - scrollZoneW && hBar.getDocPos() < hBar.docPosMax) {
                if (doScroll) { hBar.setDocPos(hBar.getDocPos() + speed); autoScrolling = true }
                scrolling = true
            }
        }
        if (vBar.getVisible()) {
            if (y < innerRect.bottom + scrollZoneH && vBar.getDocPos() < vBar.docPosMax) {
                if (doScroll) { vBar.setDocPos(vBar.getDocPos() + speed); autoScrolling = true }
                scrolling = true
            }
            if (y > innerRect.top - scrollZoneH && vBar.getDocPos() > 0) {
                if (doScroll) { vBar.setDocPos(vBar.getDocPos() - speed); autoScrolling = true }
                scrolling = true
            }
        }
        return scrolling
    }

    fun getSize(): Int = size

    fun setSize(thickness: Int) {
        size = thickness
        scrollbar[ScrollOrientation.VERTICAL.ordinal].setThickness(thickness)
        scrollbar[ScrollOrientation.HORIZONTAL.ordinal].setThickness(thickness)
    }

    private fun scrollHorizontal(newPos: Int) {
        val sv = scrolledView ?: return
        val oldPos = -(sv.getRect().left - innerRect.left)
        sv.translate(-(newPos - oldPos), 0)
    }

    private fun scrollVertical(newPos: Int) {
        val sv = scrolledView ?: return
        val oldPos = sv.getRect().top - innerRect.top
        sv.translate(0, newPos - oldPos)
    }

    private data class VisibleSize(val visW: Int, val visH: Int, val showH: Boolean, val showV: Boolean)

    private fun calcVisibleSize(): VisibleSize {
        val docRect = getScrolledViewRect()
        val docW = docRect.width
        val docH = docRect.height
        val bw = getBorderWidth()
        var visW = getRect().width - 2 * bw
        var visH = getRect().height - 2 * bw
        var showV = false
        var showH = false

        if (!hideScrollbar) {
            if ((docH - visH) > 1) {
                showV = true
                visW -= size
            }
            if ((docW - visW) > 1) {
                showH = true
                visH -= size
                if (!showV && (docH - visH) > 1) {
                    showV = true
                    visW -= size
                }
            }
        }
        return VisibleSize(visW, visH, showH, showV)
    }

    private fun updateScroll() {
        if (!isVisible() || scrolledView == null) return
        val sv = scrolledView!!
        val docRect = sv.getRect()
        val docW = docRect.width
        val docH = docRect.height
        val (visW, visH, showH, showV) = calcVisibleSize()
        val bw = getBorderWidth()
        val vBar = scrollbar[ScrollOrientation.VERTICAL.ordinal]
        val hBar = scrollbar[ScrollOrientation.HORIZONTAL.ordinal]

        if (showV) {
            if (docRect.top < getRect().height - bw) {
                sv.translate(0, getRect().height - bw - docRect.top)
            }
            scrollVertical(vBar.getDocPos())
            vBar.visible = true
            val vScrollbarH = if (!showH && reserveScrollCorner) visH - size else visH
            vBar.reshape(size, vScrollbarH, true)
            val vScrollbarOffset = if (showH || reserveScrollCorner) size else 0
            val r = vBar.getRect()
            vBar.setRect(r.copy(bottom = innerRect.bottom + vScrollbarOffset,
                                top = innerRect.bottom + vScrollbarOffset + vScrollbarH))
        } else {
            sv.translate(0, getRect().height - bw - docRect.top)
            vBar.visible = false
            vBar.setDocPos(0)
        }

        if (showH) {
            if (docRect.left > bw) {
                sv.translate(bw - docRect.left, 0)
                hBar.setDocPos(0)
            } else {
                scrollHorizontal(hBar.getDocPos())
            }
            hBar.visible = true
            val hScrollbarW = if (!showV && reserveScrollCorner) visW - size else visW
            hBar.reshape(hScrollbarW, size, true)
        } else {
            sv.translate(bw - docRect.left, 0)
            hBar.visible = false
            hBar.setDocPos(0)
        }

        hBar.setDocSize(docW)
        hBar.setPageSize(visW)
        vBar.setDocSize(docH)
        vBar.setPageSize(visH)
    }

    open fun isVisible(): Boolean = true
    open fun hasFocus(): Boolean = false
    open fun childHasKeyboardFocus(): Boolean = false
    open fun focusFirstItem() {}
    open fun getLocalRect(): Rect = Rect()
    open fun getRect(): Rect = Rect()
    open fun frameDeltaTime(): Float = 0f
}

private fun Rect.stretch(amount: Int) {
    left -= amount; right += amount; top += amount; bottom -= amount
}

private fun View.handleKeyHere(key: Key, mask: Int): Boolean = false
private fun View.handleUnicodeCharHere(char: Char): Boolean = false
