package com.firestorm.llui

import com.firestorm.llmath.Rect
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
    var thickness: Int = 0
) : View("scrollbar_${orientation.name.lowercase()}") {

    val docPosMax: Int get() = max(0, docSize - pageSize)

    fun isAtBeginning(): Boolean = docPos <= 0
    fun isAtEnd(): Boolean = docPos >= docPosMax

    fun getDocPos(): Int = docPos
    fun getDocSize(): Int = docSize

    fun setDocPos(pos: Int) { docPos = pos.coerceIn(0, docPosMax) }
    fun setDocSize(size: Int) { docSize = size }
    fun setPageSize(size: Int) { pageSize = size }
    fun setThickness(t: Int) { thickness = t }

    fun pageUp(overlap: Int = 0) { setDocPos(docPos - (pageSize - overlap)) }
    fun pageDown(overlap: Int = 0) { setDocPos(docPos + (pageSize - overlap)) }

    fun handleScrollWheel(clicks: Int): Boolean {
        if (!visible || !enabled) return false
        setDocPos(docPos + clicks * stepSize)
        return true
    }
}

open class ScrollContainer(
    name: String,
    rect: Rect = Rect(),
    var isOpaque: Boolean = false,
    var backgroundColor: Any? = null,
    borderVisible: Boolean = false,
    var hideScrollbar: Boolean = false,
    var ignoreArrowKeys: Boolean = false,
    var minAutoScrollRate: Float = 100f,
    var maxAutoScrollRate: Float = 1000f,
    var maxAutoScrollZone: UInt = 16u,
    var reserveScrollCorner: Boolean = false,
    scrollbarThickness: Int = 16
) : View(name, rect) {

    protected var scrolledView: View? = null

    val scrollbar = arrayOf(
        Scrollbar(ScrollOrientation.VERTICAL, stepSize = VERTICAL_MULTIPLE),
        Scrollbar(ScrollOrientation.HORIZONTAL, stepSize = VERTICAL_MULTIPLE)
    )

    private var size: Int = scrollbarThickness
    private val border: ViewBorder = ViewBorder()
    private var innerRect: Rect = Rect()
    private var autoScrolling: Boolean = false
    private var autoScrollRate: Float = 0f

    init {
        border.visible = borderVisible
        innerRect = localRect().stretch(-getBorderWidth())
        addChild(scrollbar[ScrollOrientation.VERTICAL.ordinal])
        addChild(scrollbar[ScrollOrientation.HORIZONTAL.ordinal])
    }

    open fun setValue(value: Any?) {
        val r = value as? Rect ?: return
        innerRect = r
    }

    fun setBorderVisible(b: Boolean) {
        border.visible = b
        innerRect = localRect().stretch(-getBorderWidth())
    }

    fun scrollToShowRect(targetRect: Rect, constraint: Rect) {
        val sv = scrolledView ?: return
        val contentWindow = getContentWindowRect()
        val scrolledRect = sv.rect

        val constrained = targetRect.copy(
            bottom = max(targetRect.bottom, targetRect.top - constraint.height),
            right = min(targetRect.right, targetRect.left + constraint.width)
        )

        val allowable = Rect(
            left = constrained.right - constraint.right,
            bottom = constrained.bottom - constraint.bottom,
            right = constrained.left - constraint.left,
            top = constrained.top - constraint.top
        ).translate(0, contentWindow.height)

        val vBar = scrollbar[ScrollOrientation.VERTICAL.ordinal]
        val hBar = scrollbar[ScrollOrientation.HORIZONTAL.ordinal]

        val vertPos = vBar.docPos.coerceIn(
            vBar.docSize - allowable.top,
            vBar.docSize - allowable.bottom
        )
        vBar.setDocSize(scrolledRect.height); vBar.setPageSize(contentWindow.height); vBar.setDocPos(vertPos)

        val horizPos = hBar.docPos.coerceIn(allowable.left, allowable.right)
        hBar.setDocSize(scrolledRect.width); hBar.setPageSize(contentWindow.width); hBar.setDocPos(horizPos)

        updateScroll()
    }

    fun scrollToShowRect(targetRect: Rect) {
        scrollToShowRect(targetRect, Rect(0, innerRect.height, innerRect.width, 0))
    }

    fun setReserveScrollCorner(b: Boolean) { reserveScrollCorner = b }

    fun getVisibleContentRect(): Rect {
        updateScroll()
        val visible = getContentWindowRect()
        val contents = scrolledView?.rect ?: return visible
        return visible.translate(-contents.left, -contents.bottom)
    }

    fun getContentWindowRect(): Rect {
        updateScroll()
        val (visW, visH, showH, _) = calcVisibleSize()
        val bw = getBorderWidth()
        val hTop = if (showH) scrollbar[ScrollOrientation.HORIZONTAL.ordinal].rect.top else bw
        return Rect.NULL.setOriginAndSize(bw, hTop, visW, visH)
    }

    open fun getScrolledViewRect(): Rect = scrolledView?.rect ?: Rect.NULL

    fun pageUp(overlap: Int = 0) { scrollbar[ScrollOrientation.VERTICAL.ordinal].pageUp(overlap); updateScroll() }
    fun pageDown(overlap: Int = 0) { scrollbar[ScrollOrientation.VERTICAL.ordinal].pageDown(overlap); updateScroll() }
    fun goToTop() { scrollbar[ScrollOrientation.VERTICAL.ordinal].setDocPos(0); updateScroll() }
    fun goToBottom() { scrollbar[ScrollOrientation.VERTICAL.ordinal].setDocPos(scrollbar[ScrollOrientation.VERTICAL.ordinal].docSize); updateScroll() }

    fun isAtTop(): Boolean = scrollbar[ScrollOrientation.VERTICAL.ordinal].isAtBeginning()
    fun isAtBottom(): Boolean = scrollbar[ScrollOrientation.VERTICAL.ordinal].isAtEnd()
    fun getDocPosVertical(): Int = scrollbar[ScrollOrientation.VERTICAL.ordinal].docPos
    fun getDocPosHorizontal(): Int = scrollbar[ScrollOrientation.HORIZONTAL.ordinal].docPos
    fun getBorderWidth(): Int = if (border.visible) border.borderWidth else 0
    fun getScrollbar(orientation: ScrollOrientation): Scrollbar = scrollbar[orientation.ordinal]

    override fun reshape(width: Int, height: Int, called: Boolean) {
        super.reshape(width, height, called)
        innerRect = localRect().stretch(-getBorderWidth())
        val sv = scrolledView ?: return
        val (visW, visH, _, _) = calcVisibleSize()
        val vBar = scrollbar[ScrollOrientation.VERTICAL.ordinal]
        val hBar = scrollbar[ScrollOrientation.HORIZONTAL.ordinal]
        vBar.setDocSize(sv.rect.height); vBar.setPageSize(visH)
        hBar.setDocSize(sv.rect.width); hBar.setPageSize(visW)
        updateScroll()
    }

    open fun handleKeyHere(key: Int, mask: UInt): Boolean {
        val KEY_LEFT = 0x83; val KEY_RIGHT = 0x84; val KEY_UP = 0x81; val KEY_DOWN = 0x82
        val KEY_PAGE_UP = 0x90; val KEY_PAGE_DOWN = 0x91; val KEY_HOME = 0x92; val KEY_END = 0x93

        if (ignoreArrowKeys && key in intArrayOf(KEY_LEFT, KEY_RIGHT, KEY_UP, KEY_DOWN,
                KEY_PAGE_UP, KEY_PAGE_DOWN, KEY_HOME, KEY_END)) {
            return false
        }
        if (scrolledView?.handleKeyHere(key, mask) == true) return true
        for (sb in scrollbar) {
            if (sb.handleKeyHere(key, mask)) { updateScroll(); return true }
        }
        return false
    }

    open fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        val vBar = scrollbar[ScrollOrientation.VERTICAL.ordinal]
        if (vBar.visible && vBar.enabled) {
            vBar.handleScrollWheel(clicks)
            updateScroll()
            return true
        }
        val hBar = scrollbar[ScrollOrientation.HORIZONTAL.ordinal]
        if (hBar.visible && hBar.enabled && hBar.handleScrollWheel(clicks)) {
            updateScroll()
            return true
        }
        return false
    }

    open fun handleScrollHWheel(x: Int, y: Int, clicks: Int): Boolean {
        val hBar = scrollbar[ScrollOrientation.HORIZONTAL.ordinal]
        if (hBar.visible && hBar.enabled && hBar.handleScrollWheel(clicks)) {
            updateScroll()
            return true
        }
        return false
    }

    open fun handleDragAndDrop(x: Int, y: Int, mask: UInt, drop: Boolean, cargoType: Int, cargoData: Any?): Boolean {
        autoScroll(x, y)
        return true
    }

    override fun draw() {
        if (autoScrolling) {
            autoScrollRate = min(autoScrollRate + frameDeltaTime() * AUTO_SCROLL_RATE_ACCEL, maxAutoScrollRate)
        } else {
            autoScrollRate = minAutoScrollRate
        }
        autoScrolling = false

        if (!hasFocus() && scrollbar.any { it.hasMouseCapture() }) focusFirstItem()

        if (rect.isValid()) {
            if (isOpaque) {
                // no-op
            }

            scrolledView?.let {
                updateScroll()
                val (_, _, showH, showV) = calcVisibleSize()
                // no-op
            }

            if (border.visible) {
                border.hasKeyboardFocus = childHasKeyboardFocus()
            }

            for (child in children.asReversed()) {
                if (child !== scrolledView && child.visible) {
                    // no-op
                }
            }
        }
    }

    fun addScrollChild(view: View, tabGroup: Int = 0): Boolean {
        if (scrolledView == null) scrolledView = view
        addChild(view)
        sendChildToFront(scrollbar[ScrollOrientation.HORIZONTAL.ordinal])
        sendChildToFront(scrollbar[ScrollOrientation.VERTICAL.ordinal])
        return true
    }

    fun canAutoScroll(x: Int, y: Int): Boolean {
        if (autoScrolling) return true
        return autoScrollImpl(x, y, doScroll = false)
    }

    fun autoScroll(x: Int, y: Int): Boolean = autoScrollImpl(x, y, doScroll = true)

    private fun autoScrollImpl(x: Int, y: Int, doScroll: Boolean): Boolean {
        val hBar = scrollbar[ScrollOrientation.HORIZONTAL.ordinal]
        val vBar = scrollbar[ScrollOrientation.VERTICAL.ordinal]
        if (!hBar.visible && !vBar.visible) return false

        val speed = (autoScrollRate * frameDeltaTime()).roundToInt()
        val zoneW = min(innerRect.width / 3, maxAutoScrollZone.toInt())
        val zoneH = min(innerRect.height / 3, maxAutoScrollZone.toInt())
        var scrolling = false

        if (hBar.visible) {
            if (x < innerRect.left + zoneW && hBar.docPos > 0) {
                if (doScroll) { hBar.setDocPos(hBar.docPos - speed); autoScrolling = true }
                scrolling = true
            }
            if (x > innerRect.right - zoneW && hBar.docPos < hBar.docPosMax) {
                if (doScroll) { hBar.setDocPos(hBar.docPos + speed); autoScrolling = true }
                scrolling = true
            }
        }
        if (vBar.visible) {
            if (y < innerRect.bottom + zoneH && vBar.docPos < vBar.docPosMax) {
                if (doScroll) { vBar.setDocPos(vBar.docPos + speed); autoScrolling = true }
                scrolling = true
            }
            if (y > innerRect.top - zoneH && vBar.docPos > 0) {
                if (doScroll) { vBar.setDocPos(vBar.docPos - speed); autoScrolling = true }
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
        val oldPos = -(sv.rect.left - innerRect.left)
        sv.rect = sv.rect.translate(-(newPos - oldPos), 0)
    }

    private fun scrollVertical(newPos: Int) {
        val sv = scrolledView ?: return
        val oldPos = sv.rect.top - innerRect.top
        sv.rect = sv.rect.translate(0, newPos - oldPos)
    }

    private data class VisSize(val visW: Int, val visH: Int, val showH: Boolean, val showV: Boolean)

    private fun calcVisibleSize(): VisSize {
        val docRect = getScrolledViewRect()
        val bw = getBorderWidth()
        var visW = rect.width - 2 * bw
        var visH = rect.height - 2 * bw
        var showV = false
        var showH = false

        if (!hideScrollbar) {
            if ((docRect.height - visH) > 1) { showV = true; visW -= size }
            if ((docRect.width - visW) > 1) {
                showH = true; visH -= size
                if (!showV && (docRect.height - visH) > 1) { showV = true; visW -= size }
            }
        }
        return VisSize(visW, visH, showH, showV)
    }

    private fun updateScroll() {
        if (!visible || scrolledView == null) return
        val sv = scrolledView!!
        val (visW, visH, showH, showV) = calcVisibleSize()
        val bw = getBorderWidth()
        val vBar = scrollbar[ScrollOrientation.VERTICAL.ordinal]
        val hBar = scrollbar[ScrollOrientation.HORIZONTAL.ordinal]

        if (showV) {
            if (sv.rect.top < rect.height - bw) {
                sv.rect = sv.rect.translate(0, rect.height - bw - sv.rect.top)
            }
            scrollVertical(vBar.docPos)
            vBar.visible = true
            val vH = if (!showH && reserveScrollCorner) visH - size else visH
            vBar.reshape(size, vH)
            val vOffset = if (showH || reserveScrollCorner) size else 0
            vBar.rect = vBar.rect.setOriginAndSize(vBar.rect.left, innerRect.bottom + vOffset, size, vH)
        } else {
            sv.rect = sv.rect.translate(0, rect.height - bw - sv.rect.top)
            vBar.visible = false; vBar.setDocPos(0)
        }

        if (showH) {
            if (sv.rect.left > bw) {
                sv.rect = sv.rect.translate(bw - sv.rect.left, 0)
                hBar.setDocPos(0)
            } else {
                scrollHorizontal(hBar.docPos)
            }
            hBar.visible = true
            val hW = if (!showV && reserveScrollCorner) visW - size else visW
            hBar.reshape(hW, size)
        } else {
            sv.rect = sv.rect.translate(bw - sv.rect.left, 0)
            hBar.visible = false; hBar.setDocPos(0)
        }

        hBar.setDocSize(sv.rect.width); hBar.setPageSize(visW)
        vBar.setDocSize(sv.rect.height); vBar.setPageSize(visH)
    }

    open fun hasFocus(): Boolean = false
    open fun childHasKeyboardFocus(): Boolean = false
    open fun focusFirstItem() {}
    open fun frameDeltaTime(): Float = 0f
    open fun sendChildToFront(child: View) {}

    private fun localRect(): Rect = Rect(0, rect.height, rect.width, 0)
}

private fun Rect.setOriginAndSize(l: Int, b: Int, w: Int, h: Int): Rect =
    Rect(left = l, bottom = b, right = l + w, top = b + h)

private fun View.handleKeyHere(key: Int, mask: UInt): Boolean = false

private fun Scrollbar.handleKeyHere(key: Int, mask: UInt): Boolean = false
private fun Scrollbar.hasMouseCapture(): Boolean = false
