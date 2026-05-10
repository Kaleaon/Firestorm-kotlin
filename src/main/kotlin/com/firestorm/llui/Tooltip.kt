package com.firestorm.llui

var gToolTipView: ToolTipView? = null

class ToolTipView {

    private var lastX: Int = 0
    private var lastY: Int = 0

    fun draw() {
        ToolTipMgr.updateToolTipVisibility()
        drawChildren()
    }

    fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        val mgr = ToolTipMgr
        if (x != lastX && y != lastY && !mgr.getMouseNearRect().pointInRect(x, y)) {
            mgr.unblockToolTips()
        }
        lastX = x
        lastY = y
        return false
    }

    fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        ToolTipMgr.blockToolTips()
        TODO("Platform: hide menus via menu container")
    }

    fun handleMiddleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        ToolTipMgr.blockToolTips()
        return false
    }

    fun handleRightMouseDown(x: Int, y: Int, mask: Int): Boolean {
        ToolTipMgr.blockToolTips()
        return false
    }

    fun handleScrollWheel(x: Int, y: Int, clicks: Int): Boolean {
        ToolTipMgr.blockToolTips()
        return false
    }

    fun drawStickyRect() {
        TODO("GPU: draw rect outline for ToolTipMgr.getMouseNearRect()")
    }

    private fun drawChildren() {
        TODO("GPU: render child views")
    }
}

data class StyledText(val text: String, val style: Any? = null)

data class ToolTipParams(
    val message: String = "",
    val styledMessage: List<StyledText> = emptyList(),
    val posX: Int = 0,
    val posY: Int = 0,
    val delayTime: Float = 0.35f,
    val visibleTimeOver: Float = 1f,
    val visibleTimeNear: Float = 0.5f,
    val visibleTimeFar: Float = 0.1f,
    val stickyRect: SimpleRect? = null,
    val image: Any? = null,
    val textColor: Color4? = null,
    val timeBasedMedia: Boolean = false,
    val webBasedMedia: Boolean = false,
    val mediaPlaying: Boolean = false,
    val createCallback: ((ToolTipParams) -> ToolTip?)? = null,
    val clickCallback: (() -> Unit)? = null,
    val clickPlayMediaCallback: (() -> Unit)? = null,
    val clickHomepageCallback: (() -> Unit)? = null,
    val maxWidth: Int = 200,
    val padding: Int = 4,
    val wrap: Boolean = true,
    val allowPasteTooltip: Boolean = false
)

data class SimpleRect(
    val left: Int = 0, val top: Int = 0,
    val right: Int = 0, val bottom: Int = 0
) {
    val width: Int get() = right - left
    val height: Int get() = top - bottom
    fun pointInRect(x: Int, y: Int) = x in left..right && y in bottom..top
    fun unionWith(other: SimpleRect) = SimpleRect(
        minOf(left, other.left), maxOf(top, other.top),
        maxOf(right, other.right), minOf(bottom, other.bottom)
    )
    fun centerAndSize(cx: Int, cy: Int, w: Int, h: Int) = SimpleRect(
        cx - w / 2, cy + h / 2, cx + w / 2, cy - h / 2
    )
}

open class ToolTip(protected val params: ToolTipParams) {

    protected var textBox: Any? = null
    protected var infoButton: Any? = null
    protected var playMediaButton: Any? = null
    protected var homePageButton: Any? = null

    private var fadeStarted: Boolean = false
    private var fadeElapsed: Float = 0f
    private var visibleStarted: Boolean = false
    private var visibleElapsed: Float = 0f

    protected val hasClickCallback: Boolean = params.clickCallback != null
    protected val padding: Int = params.padding
    protected val maxWidth: Int = params.maxWidth
    protected var isTooltipPastable: Boolean = params.allowPasteTooltip

    var visible: Boolean = false
        protected set

    var rect: SimpleRect = SimpleRect()

    init {
        buildTextBox()
        if (params.image != null) buildInfoButton()
        if (params.timeBasedMedia) buildPlayMediaButton()
        if (params.webBasedMedia) buildHomePageButton()
        if (params.clickCallback != null) { /* register mouse-up callback */ }
    }

    private fun buildTextBox() {
        TODO("GPU: create text box with padding=${params.padding}, maxWidth=${params.maxWidth}, wrap=${params.wrap}, color=${params.textColor}")
    }

    private fun buildInfoButton() {
        TODO("GPU: create info icon button with image=${params.image}, size derived from image width")
    }

    private fun buildPlayMediaButton() {
        TODO("GPU: create play/pause toggle button for time-based media")
    }

    private fun buildHomePageButton() {
        TODO("GPU: create home-page button for web-based media")
    }

    open fun initFromParams(p: ToolTipParams) {
        TODO("GPU: reshape text box, set styled or plain text, call updateTextBox and snapToChildren")
    }

    protected fun updateTextBox() {
        TODO("GPU: reshape text box to fit actual pixel dimensions of text")
    }

    protected fun snapToChildren() {
        TODO("GPU: resize this panel to wrap the text box plus padding on all sides")
    }

    open fun setVisible(vis: Boolean) {
        if (vis) {
            visibleStarted = true
            visibleElapsed = 0f
            fadeStarted = false
            visible = true
        } else {
            visibleStarted = false
            if (!fadeStarted) {
                fadeStarted = true
                fadeElapsed = 0f
            }
        }
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        TODO("GPU: highlight info button; set hand cursor if hasClickCallback")
    }

    open fun onMouseLeave(x: Int, y: Int, mask: Int) {
        TODO("GPU: clear info button highlight")
    }

    open fun draw() {
        val alpha = if (fadeStarted) {
            val fadeTime = 0.2f
            val a = 1f - (fadeElapsed / fadeTime)
            if (a <= 0f) {
                fadeStarted = false
                visible = false
                return
            }
            a
        } else 1f

        TODO("GPU: draw tooltip panel with alpha=$alpha")
    }

    fun isFading(): Boolean = fadeStarted

    fun getVisibleTime(): Float = if (visibleStarted) visibleElapsed else 0f

    fun hasClickCallback(): Boolean = hasClickCallback

    fun isInVisibleChain(): Boolean = visible

    fun parentPointInView(x: Int, y: Int): Boolean = rect.pointInRect(x, y)

    fun getToolTipMessage(): String {
        TODO("GPU: retrieve text from text box")
    }

    fun isTooltipPastable(): Boolean = isTooltipPastable
}

class Inspector(params: ToolTipParams) : ToolTip(params)

object ToolTipMgr {

    private var toolTipsBlocked: Boolean = false
    private var toolTip: ToolTip? = null
    private var lastToolTipParams: ToolTipParams = ToolTipParams()
    private var nextToolTipParams: ToolTipParams = ToolTipParams()
    private var needsToolTip: Boolean = false
    private var mouseNearRect: SimpleRect = SimpleRect()

    fun show(params: ToolTipParams) {
        val hasContent = params.styledMessage.isNotEmpty() ||
                params.message.isNotEmpty() ||
                params.image != null ||
                params.createCallback != null
        if (!hasContent) return

        val mouseIdleTime: Float = TODO("Platform: get mouse idle time")

        if (!toolTipsBlocked && mouseIdleTime > params.delayTime) {
            val tooltipChanged = lastToolTipParams.message != params.message ||
                    lastToolTipParams.posX != params.posX ||
                    lastToolTipParams.posY != params.posY ||
                    lastToolTipParams.timeBasedMedia != params.timeBasedMedia ||
                    lastToolTipParams.webBasedMedia != params.webBasedMedia

            val tooltipShown = toolTip?.let { it.visible && !it.isFading() } ?: false
            needsToolTip = tooltipChanged || !tooltipShown
            nextToolTipParams = params
        }
    }

    fun show(message: String, allowPasteTooltip: Boolean = false) {
        show(ToolTipParams(message = message, allowPasteTooltip = allowPasteTooltip))
    }

    fun unblockToolTips() {
        toolTipsBlocked = false
    }

    fun blockToolTips() {
        hideToolTips()
        toolTipsBlocked = true
    }

    fun hideToolTips() {
        toolTip?.setVisible(false)
    }

    fun toolTipVisible(): Boolean = toolTip?.isInVisibleChain() ?: false

    fun getToolTipRect(): SimpleRect = if (toolTip != null && toolTip!!.visible) toolTip!!.rect else SimpleRect()

    fun getMouseNearRect(): SimpleRect = if (toolTipVisible()) mouseNearRect else SimpleRect()

    fun updateToolTipVisibility() {
        if (needsToolTip) {
            needsToolTip = false
            createToolTip(nextToolTipParams)
            lastToolTipParams = nextToolTipParams
            return
        }

        val cursorHidden: Boolean = TODO("Platform: check if mouse cursor is hidden")

        if (cursorHidden) {
            blockToolTips()
            return
        }

        if (toolTipVisible()) {
            val mouseX: Int = TODO("Platform: get local mouse X relative to gToolTipView")
            val mouseY: Int = TODO("Platform: get local mouse Y relative to gToolTipView")

            val tooltipTimeout = when {
                toolTip!!.parentPointInView(mouseX, mouseY) -> lastToolTipParams.visibleTimeOver
                mouseNearRect.pointInRect(mouseX, mouseY)   -> lastToolTipParams.visibleTimeNear
                else                                         -> lastToolTipParams.visibleTimeFar
            }

            if (toolTip!!.getVisibleTime() > tooltipTimeout) {
                hideToolTips()
                unblockToolTips()
            }
        }
    }

    fun getToolTipMessage(): String = if (toolTipVisible()) toolTip!!.getToolTipMessage() else ""

    fun isTooltipPastable(): Boolean = if (toolTipVisible()) toolTip!!.isTooltipPastable() else false

    private fun createToolTip(params: ToolTipParams) {
        blockToolTips()

        val effectiveParams = if (params.clickCallback != null) params else params

        val tip = params.createCallback?.invoke(effectiveParams)
            ?: ToolTip(effectiveParams)

        toolTip = tip

        gToolTipView?.let { view ->
            TODO("GPU: add tooltip as child of gToolTipView")
        }

        TODO("Platform: position tooltip near mouse at posX=${params.posX}, posY=${params.posY}")

        mouseNearRect = if (params.stickyRect != null) {
            params.stickyRect
        } else {
            val mx: Int = TODO("Platform: get mouse X local to gToolTipView parent")
            val my: Int = TODO("Platform: get mouse Y local to gToolTipView parent")
            SimpleRect().centerAndSize(mx, my, 3, 3)
        }

        if (tip.hasClickCallback()) {
            mouseNearRect = mouseNearRect.unionWith(tip.rect)
        }
    }
}
