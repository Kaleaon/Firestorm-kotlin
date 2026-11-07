package com.firestorm.llui

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MIN_FRACTIONAL_SIZE = 0.00001f
private const val MAX_FRACTIONAL_SIZE = 1f

class ResizeBar(val side: Side) {
    enum class Side { RIGHT, BOTTOM }
    private var rect: Rect = Rect()
    private var minSize: Int = 0
    var visible: Boolean = true
    fun getRect(): Rect = rect
    fun setShape(r: Rect) { rect = r }
    fun getVisible(): Boolean = visible
    fun setVisible(v: Boolean) { visible = v }
}

class LayoutPanel(
    val name: String = "",
    expandedMinDim: Int = 0,
    minDim: Int = -1,
    val autoResize: Boolean = true,
    val userResize: Boolean = false
) {
    var expandedMinDim: Int = if (expandedMinDim > 0) expandedMinDim else minDim
    var minDim: Int = minDim
    var collapsed: Boolean = false
    var visibleAmt: Float = 1f
    var collapseAmt: Float = 0f
    var fractionalSize: Float = 0f
    var targetDim: Int = 0
    var ignoreReshape: Boolean = false
    var orientation: Orientation = Orientation.HORIZONTAL
    var resizeBar: ResizeBar? = null
    var visible: Boolean = true
    private var rect: Rect = Rect()

    val reshapeCallbacks: MutableList<(LayoutPanel, Rect) -> Unit> = mutableListOf()

    fun getRect(): Rect = rect
    fun getVisible(): Boolean = visible
    fun isCollapsed(): Boolean = collapsed

    fun getMinDim(): Int = max(0, minDim)

    fun getExpandedMinDim(): Int = if (expandedMinDim >= 0) expandedMinDim else getMinDim()

    fun getRelevantMinDim(): Int = if (!collapsed) getExpandedMinDim() else minDim

    fun getAutoResizeFactor(): Float = visibleAmt * (1f - collapseAmt)

    fun getVisibleAmount(): Float = visibleAmt

    fun getLayoutDim(): Int = (if (orientation == Orientation.HORIZONTAL) rect.getWidth() else rect.getHeight().toFloat().roundToInt()).let { it }

    fun getTargetDim(): Int = targetDim

    fun setTargetDim(value: Int) {
        val newRect = rect.copy()
        if (orientation == Orientation.HORIZONTAL) {
            val updated = newRect.copy(right = newRect.left + value)
            setShape(updated)
        } else {
            val updated = newRect.copy(top = newRect.bottom + value)
            setShape(updated)
        }
    }

    fun getVisibleDim(): Int {
        val minD = getRelevantMinDim().toFloat()
        return (visibleAmt * (minD + ((targetDim - minD) * (1f - collapseAmt)))).roundToInt()
    }

    fun setOrientation(o: Orientation) {
        orientation = o
        val layoutDim = if (o == Orientation.HORIZONTAL) rect.getWidth() else rect.getHeight()
        if (!autoResize && userResize && minDim == -1) {
            minDim = layoutDim
        }
        targetDim = max(layoutDim, getMinDim())
    }

    fun setVisible(v: Boolean) {
        visible = v
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        if (width == rect.getWidth() && height == rect.getHeight()) return
        if (!ignoreReshape && !autoResize) {
            targetDim = if (orientation == Orientation.HORIZONTAL) width else height
        }
        val newRect = rect.copy(right = rect.left + width, top = rect.bottom + height)
        rect = newRect
    }

    fun handleReshape(newRect: Rect, byUser: Boolean) {
        rect = newRect
        reshapeCallbacks.forEach { it(this, newRect) }
    }

    fun setShape(newRect: Rect) {
        rect = newRect
    }

    fun setIgnoreReshape(ignore: Boolean) { ignoreReshape = ignore }

    fun addReshapeCallback(cb: (LayoutPanel, Rect) -> Unit) { reshapeCallbacks.add(cb) }
}

class LayoutStack(
    val orientation: Orientation,
    borderSize: Int = 4,
    val animate: Boolean = true,
    val clip: Boolean = true,
    val openTimeConstant: Float = 0.02f,
    val closeTimeConstant: Float = 0.03f,
    val resizeBarOverlap: Int = 1,
    val showDragHandle: Boolean = false,
    val dragHandleFirstIndent: Int = 0,
    val dragHandleSecondIndent: Int = 0,
    val dragHandleThickness: Int = 5,
    val dragHandleShift: Int = 2,
    val dragHandleColor: FloatArray = floatArrayOf(0.5f, 0.5f, 0.5f, 1f),
    val saveSizes: Boolean = false,
    val name: String = ""
) {
    var panelSpacing: Int = borderSize
        set(value) { if (field != value) { field = value; needsLayout = true } }

    val panels: MutableList<LayoutPanel> = mutableListOf()
    private var animatedThisFrame: Boolean = false
    var needsLayout: Boolean = true
    private var savedSizes: MutableList<Int> = mutableListOf()
    private var rect: Rect = Rect()

    companion object {
        private val instances: MutableList<LayoutStack> = mutableListOf()

        fun updateClass() {
            for (layout in instances) {
                layout.updateLayout()
                layout.animatedThisFrame = false
            }
        }
    }

    init {
        instances.add(this)
        if (saveSizes) {
            System.err.println("LayoutStack: saveSizes not yet implemented")
        }
    }

    fun getNumPanels(): Int = panels.size

    fun addPanel(panel: LayoutPanel, animateIn: Boolean = false) {
        addChild(panel)
        if (animateIn) {
            panel.visibleAmt = 0f
            panel.setVisible(true)
        }
    }

    fun collapsePanel(panel: LayoutPanel, collapsed: Boolean = true) {
        panel.collapsed = collapsed
        needsLayout = true
    }

    fun addChild(child: LayoutPanel, tabGroup: Int = 0) {
        if (saveSizes && savedSizes.isNotEmpty()) {
            val dim = savedSizes.getOrNull(panels.size) ?: 0
            if (dim > 0) {
                if (orientation == Orientation.HORIZONTAL) {
                    child.reshape(dim, child.getRect().getHeight(), true)
                } else {
                    child.reshape(child.getRect().getWidth(), dim, true)
                }
            }
        }
        child.setOrientation(orientation)
        panels.add(child)
        createResizeBar(child)
        needsLayout = true
        updateFractionalSizes()
    }

    fun removeChild(view: LayoutPanel) {
        panels.remove(view)
        view.resizeBar = null
        updateFractionalSizes()
        needsLayout = true
    }

    fun deleteAllChildren() {
        panels.forEach { it.resizeBar = null }
        panels.clear()
        updateFractionalSizes()
        needsLayout = true
    }

    fun postBuild(): Boolean {
        updateLayout()
        return true
    }

    fun draw() {
        updateLayout()
        if (clip) { /* no-op */ }
        for (panelp in panels) {
            val essentiallyInvisible = (!panelp.getVisible() || panelp.collapsed) && (panelp.visibleAmt < 0.001f || !animate)
            if (essentiallyInvisible) continue
            // no-op
            panelp.resizeBar?.let { bar ->
                if (bar.getVisible()) { /* no-op */ }
            }
        }
    }

    fun reshape(width: Int, height: Int, calledFromParent: Boolean = true) {
        rect = rect.copy(right = rect.left + width, top = rect.bottom + height)
        needsLayout = true
    }

    fun updateLayout() {
        if (!needsLayout) return

        val continueAnimating = animatePanels()
        var totalVisibleFraction = 0f
        var spaceToDistribute = if (orientation == Orientation.HORIZONTAL) rect.getWidth() else rect.getHeight()

        for (panelp in panels) {
            if (panelp.autoResize) {
                panelp.targetDim = panelp.getRelevantMinDim()
            }
            spaceToDistribute -= panelp.getVisibleDim() + (panelSpacing * panelp.getVisibleAmount()).roundToInt()
            totalVisibleFraction += panelp.fractionalSize * panelp.getAutoResizeFactor()
        }

        if (panels.isNotEmpty()) {
            spaceToDistribute += (panelSpacing.toFloat() * panels.last().getVisibleAmount()).roundToInt()
        }

        var remainingSpace = spaceToDistribute
        if (spaceToDistribute > 0 && totalVisibleFraction > 0f) {
            for (panelp in panels) {
                if (panelp.autoResize) {
                    val fractionToDistribute = (panelp.fractionalSize * panelp.getAutoResizeFactor()) / totalVisibleFraction
                    val delta = (spaceToDistribute * fractionToDistribute).roundToInt()
                    panelp.targetDim += delta
                    remainingSpace -= delta
                }
            }
        }

        for (panelp in panels) {
            if (remainingSpace == 0) break
            if (panelp.autoResize && !panelp.collapsed && panelp.getVisible()) {
                val spaceForPanel = if (remainingSpace > 0) 1 else -1
                panelp.targetDim += spaceForPanel
                remainingSpace -= spaceForPanel
            }
        }

        var curPos = if (orientation == Orientation.HORIZONTAL) 0f else rect.getHeight().toFloat()

        if (saveSizes) savedSizes.clear()

        for (panelp in panels) {
            val panelDim = max(panelp.getExpandedMinDim(), panelp.targetDim).toFloat()

            val panelRect: Rect = if (orientation == Orientation.HORIZONTAL) {
                Rect(
                    left = curPos.roundToInt(),
                    top = rect.getHeight(),
                    right = (curPos + panelDim).roundToInt(),
                    bottom = 0
                )
            } else {
                Rect(
                    left = 0,
                    top = curPos.roundToInt(),
                    right = rect.getWidth(),
                    bottom = (curPos - panelDim).roundToInt()
                )
            }

            val panelSpacingF = panelSpacing * panelp.getVisibleAmount()
            val panelVisibleDim = panelp.getVisibleDim().toFloat()
            val panelSpacingRound = panelSpacingF.roundToInt()

            val resizeBarRect: Rect = if (orientation == Orientation.HORIZONTAL) {
                if (showDragHandle && panelSpacingRound > dragHandleThickness) {
                    val left = panelRect.right + dragHandleShift
                    Rect(left, panelRect.top - dragHandleFirstIndent, left + dragHandleThickness, panelRect.bottom + dragHandleSecondIndent)
                } else {
                    Rect(panelRect.right - resizeBarOverlap, panelRect.top, panelRect.right + panelSpacingRound + resizeBarOverlap, panelRect.bottom)
                }
            } else {
                if (showDragHandle && panelSpacingRound > dragHandleThickness) {
                    val top = panelRect.bottom - dragHandleShift
                    Rect(panelRect.left + dragHandleFirstIndent, top, panelRect.right - dragHandleSecondIndent, top - dragHandleThickness)
                } else {
                    Rect(panelRect.left, panelRect.bottom + resizeBarOverlap, panelRect.right, panelRect.bottom - panelSpacingRound - resizeBarOverlap)
                }
            }

            panelp.setIgnoreReshape(true)
            panelp.setShape(panelRect)
            panelp.setIgnoreReshape(false)
            panelp.resizeBar?.setShape(resizeBarRect)

            if (saveSizes) savedSizes.add(panelp.targetDim)

            curPos += if (orientation == Orientation.HORIZONTAL) panelVisibleDim + panelSpacingF else -(panelVisibleDim + panelSpacingF)
        }

        updateResizeBarLimits()
        needsLayout = continueAnimating
    }

    fun setPanelSpacing(value: Int) {
        panelSpacing = value
    }

    fun findEmbeddedPanel(panelp: LayoutPanel): LayoutPanel? = panels.firstOrNull { it === panelp }

    fun findEmbeddedPanelByName(name: String): LayoutPanel? = panels.firstOrNull { it.name == name }

    fun refreshFromSettings() {
        System.err.println("LayoutStack: refreshFromSettings not yet implemented")
    }

    private fun updateFractionalSizes() {
        var totalResizableDim = 0f
        for (panelp in panels) {
            if (panelp.autoResize) {
                totalResizableDim += max(MIN_FRACTIONAL_SIZE, (panelp.getLayoutDim() - panelp.getRelevantMinDim()).toFloat())
            }
        }
        for (panelp in panels) {
            if (panelp.autoResize) {
                val panelResizableDim = max(MIN_FRACTIONAL_SIZE, (panelp.getLayoutDim() - panelp.getRelevantMinDim()).toFloat())
                panelp.fractionalSize = if (panelResizableDim > 0f)
                    (panelResizableDim / totalResizableDim).coerceIn(MIN_FRACTIONAL_SIZE, MAX_FRACTIONAL_SIZE)
                else
                    MIN_FRACTIONAL_SIZE
            }
        }
        normalizeFractionalSizes()
    }

    private fun normalizeFractionalSizes() {
        val autoResizePanels = panels.filter { it.autoResize }
        val total = autoResizePanels.sumOf { it.fractionalSize.toDouble() }.toFloat()

        if (total == 0f) {
            val equalShare = MAX_FRACTIONAL_SIZE / autoResizePanels.size.coerceAtLeast(1)
            autoResizePanels.forEach { it.fractionalSize = equalShare }
        } else {
            autoResizePanels.forEach { it.fractionalSize /= total }
        }
    }

    private fun animatePanels(): Boolean {
        var continueAnimating = false

        for (panelp in panels) {
            if (panelp.getVisible()) {
                if (animate && panelp.visibleAmt < 1f) {
                    if (!animatedThisFrame) {
                        panelp.visibleAmt = lerp(panelp.visibleAmt, 1f, openTimeConstant)
                        if (panelp.visibleAmt > 0.99f) panelp.visibleAmt = 1f
                    }
                    animatedThisFrame = true
                    continueAnimating = true
                } else {
                    if (panelp.visibleAmt != 1f) {
                        panelp.visibleAmt = 1f
                        animatedThisFrame = true
                    }
                }
            } else {
                if (animate && panelp.visibleAmt > 0f) {
                    if (!animatedThisFrame) {
                        panelp.visibleAmt = lerp(panelp.visibleAmt, 0f, closeTimeConstant)
                        if (panelp.visibleAmt < 0.001f) panelp.visibleAmt = 0f
                    }
                    continueAnimating = true
                    animatedThisFrame = true
                } else {
                    if (panelp.visibleAmt != 0f) {
                        panelp.visibleAmt = 0f
                        animatedThisFrame = true
                    }
                }
            }

            val collapseState = if (panelp.collapsed) 1f else 0f
            if (panelp.collapseAmt != collapseState) {
                if (animate) {
                    if (!animatedThisFrame) {
                        panelp.collapseAmt = lerp(panelp.collapseAmt, collapseState, closeTimeConstant)
                    }
                    if (abs(panelp.collapseAmt - collapseState) < 0.001f) {
                        panelp.collapseAmt = collapseState
                    }
                    animatedThisFrame = true
                    continueAnimating = true
                } else {
                    panelp.collapseAmt = collapseState
                    animatedThisFrame = true
                }
            }
        }

        if (animatedThisFrame) needsLayout = true
        return continueAnimating
    }

    private fun updatePanelRect(resizedPanel: LayoutPanel, newRect: Rect) {
        val newDim = if (orientation == Orientation.HORIZONTAL) newRect.getWidth() else newRect.getHeight()
        val deltaPanelDim = newDim - resizedPanel.getVisibleDim()
        if (deltaPanelDim == 0) return

        var totalVisibleFraction = 0f
        var deltaAutoResizeHeadroom = 0f
        var oldAutoResizeHeadroom = 0f
        var otherResizePanel: LayoutPanel? = null
        var followingPanel: LayoutPanel? = null

        for (panelp in panels.reversed()) {
            if (panelp.autoResize) {
                oldAutoResizeHeadroom += (panelp.targetDim - panelp.getRelevantMinDim()).toFloat()
                if (panelp.getVisible() && !panelp.collapsed) {
                    totalVisibleFraction += panelp.fractionalSize
                }
            }
            if (panelp === resizedPanel) otherResizePanel = followingPanel
            if (panelp.getVisible() && !panelp.collapsed) followingPanel = panelp
        }

        if (resizedPanel.autoResize) {
            if (otherResizePanel == null || !otherResizePanel.autoResize) {
                deltaAutoResizeHeadroom += deltaPanelDim
            }
        }

        needsLayout = true
    }

    private fun createResizeBar(panelp: LayoutPanel) {
        for (lp in panels) {
            if (lp.resizeBar == null) {
                val side = if (orientation == Orientation.HORIZONTAL) ResizeBar.Side.RIGHT else ResizeBar.Side.BOTTOM
                val bar = ResizeBar(side)
                lp.resizeBar = bar

                if (showDragHandle) {
                    // no-op
                }
            }
        }
    }

    private fun updateResizeBarLimits() {
        var headroomBefore = 0
        var headroomAfter = panels.filter { it.autoResize && it.getVisible() && !it.collapsed }
            .sumOf { it.targetDim - it.getRelevantMinDim() }

        for (panelp in panels) {
            val resizeBar = panelp.resizeBar ?: continue
            val isLast = panelp === panels.last()
            val nextVisible = panels.dropWhile { it !== panelp }.drop(1).firstOrNull { it.getVisible() && !it.collapsed }

            resizeBar.setVisible(
                !isLast && panelp.getVisible() && !panelp.collapsed && nextVisible != null
            )
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
