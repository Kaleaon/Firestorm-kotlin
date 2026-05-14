package com.firestorm.newview

import java.util.UUID

private const val WIDGET_SPACING = 3
private const val FAVORITE_IMAGE_SIZE = 14
private const val FAVORITE_IMAGE_PAD = 3

enum class EItemState {
    IS_DEFAULT,
    IS_WORN,
    IS_MISMATCH
}

data class LLRect(
    var left: Int = 0,
    var top: Int = 0,
    var right: Int = 0,
    var bottom: Int = 0
) {
    fun getWidth(): Int = right - left
    fun getHeight(): Int = top - bottom
    fun pointInRect(x: Int, y: Int): Boolean = x in left..right && y in bottom..top
}

interface LLUICtrl {
    fun setVisible(visible: Boolean)
    fun getVisible(): Boolean
    fun setEnabled(enabled: Boolean)
    fun getEnabled(): Boolean
    fun getRect(): LLRect
    fun setShape(rect: LLRect)
    fun getText(): String
    fun getTextPixelWidth(): Int
    fun setToolTip(tip: String)
}

interface LLIconCtrl : LLUICtrl {
    fun setImage(image: Any?)
}

interface LLTextBox : LLUICtrl

abstract class LLPanelInventoryListItemBase(
    item: LLViewerInventoryItemStub?,
    hoverImage: Any? = null,
    selectedImage: Any? = null,
    separatorImage: Any? = null
) {

    val inventoryItemUUID: UUID = item?.uuid ?: UUID.fromString("00000000-0000-0000-0000-000000000000")
    var mHovered: Boolean = false
        protected set

    protected var iconCtrl: LLIconCtrl? = null
    protected var titleCtrl: LLTextBox? = null

    private var iconImage: Any? = null
    private var mHoverImage: Any? = hoverImage
    private var mSelectedImage: Any? = selectedImage
    private var mSeparatorImage: Any? = separatorImage

    private var mSelected: Boolean = false
    private var mSeparatorVisible: Boolean = false
    private var mIsFavorite: Boolean = false

    private var highlightedText: String = ""

    private val leftSideWidgets: MutableList<LLUICtrl> = mutableListOf()
    private val rightSideWidgets: MutableList<LLUICtrl> = mutableListOf()
    private var widgetSpacing: Int = WIDGET_SPACING

    private var leftWidgetsWidth: Int = 0
    private var rightWidgetsWidth: Int = 0
    private var mNeedsRefresh: Boolean = false

    companion object {
        fun create(item: LLViewerInventoryItemStub?): LLPanelInventoryListItemBase? {
            if (item == null) return null
            TODO("GPU: construct default-params panel and call postBuild()")
        }
    }

    open fun draw() {
        if (getNeedsRefresh()) {
            val invItem = getItem()
            if (invItem != null) {
                updateItem(invItem.name, getIsItemFavorite(invItem))
            }
            setNeedsRefresh(false)
        }
        TODO("GPU: draw hover/selected/separator images and call panel draw")
    }

    protected open fun updateItem(name: String, favorite: Boolean, itemState: EItemState = EItemState.IS_DEFAULT) {
        setIconImage(iconImage)
        if (mIsFavorite != favorite) {
            mIsFavorite = favorite
            reshapeMiddleWidgets()
        }
        setTitle(name, highlightedText, itemState)
    }

    fun addWidgetToLeftSide(ctrl: LLUICtrl, showWidget: Boolean = true) {
        leftSideWidgets.add(ctrl)
        setShowWidget(ctrl, showWidget)
    }

    fun addWidgetToRightSide(ctrl: LLUICtrl, showWidget: Boolean = true) {
        rightSideWidgets.add(ctrl)
        setShowWidget(ctrl, showWidget)
    }

    fun setShowWidget(ctrl: LLUICtrl, show: Boolean) {
        ctrl.setEnabled(show)
    }

    fun setWidgetSpacing(spacing: Int) { widgetSpacing = spacing }
    fun getWidgetSpacing(): Int = widgetSpacing

    open fun postBuild(): Boolean {
        val invItem = getItem()
        if (invItem != null) {
            TODO("GPU: load iconImage from LLInventoryIcon.getIcon for invItem type/flags")
            updateItem(invItem.name, getIsItemFavorite(invItem))
        }
        setNeedsRefresh(true)
        setWidgetsVisible(false)
        reshapeWidgets()
        return true
    }

    open fun setValue(value: Map<String, Any>) {
        if (value.containsKey("selected")) {
            mSelected = value["selected"] as? Boolean ?: false
        }
    }

    open fun notify(info: Map<String, Any>): Int {
        if (info.containsKey("match_filter")) {
            highlightedText = info["match_filter"] as? String ?: ""
            val test = titleCtrl?.getText()?.toUpperCase() ?: ""
            setNeedsRefresh(true)
            return if (highlightedText.isEmpty() || test.contains(highlightedText)) 0 else -1
        }
        return 0
    }

    open fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        mHovered = true
        return true
    }

    open fun onMouseLeave(x: Int, y: Int, mask: Int) {
        mHovered = false
    }

    fun getItemName(): String = getItem()?.name ?: ""

    fun getType(): String = getItem()?.type ?: "AT_NONE"

    fun getWearableType(): String = getItem()?.wearableType ?: "WT_NONE"

    fun getDescription(): String = getItem()?.actualDescription ?: ""

    fun getCreationDate(): Long = getItem()?.creationDate ?: 0L

    fun getItem(): LLViewerInventoryItemStub? {
        TODO("APR: use JVM equivalent - return gInventory.getItem(inventoryItemUUID)")
    }

    fun setSeparatorVisible(visible: Boolean) { mSeparatorVisible = visible }
    fun resetHighlight() { mHovered = false }

    fun setNeedsRefresh(needsRefresh: Boolean) { mNeedsRefresh = needsRefresh }
    fun getNeedsRefresh(): Boolean = mNeedsRefresh

    protected fun setLeftWidgetsWidth(width: Int) { leftWidgetsWidth = width }
    protected fun setRightWidgetsWidth(width: Int) { rightWidgetsWidth = width }

    protected open fun setWidgetsVisible(visible: Boolean) {
        for (widget in leftSideWidgets) {
            widget.setVisible(visible && widget.getEnabled())
        }
        for (widget in rightSideWidgets) {
            widget.setVisible(visible && widget.getEnabled())
        }
    }

    protected open fun reshapeWidgets() {
        reshapeRightWidgets()
        reshapeMiddleWidgets()
    }

    protected fun setIconImage(image: Any?) {
        if (image != null) {
            iconImage = image
            iconCtrl?.setImage(iconImage)
        }
    }

    protected fun setTitle(title: String, highlitText: String, itemState: EItemState = EItemState.IS_DEFAULT) {
        titleCtrl?.setToolTip(title)
        TODO("GPU: apply style params for itemState, apply favorite color if mIsFavorite, then textboxSetHighlightedVal")
    }

    protected open fun handleToolTip(x: Int, y: Int, mask: Int): Boolean {
        val textBoxRect = titleCtrl?.getRect() ?: return false
        TODO("GPU: show thumbnail tooltip if FSShowInventoryThumbnailTooltips enabled; fall back to panel tooltip if text is clipped")
    }

    protected open fun getDefaultParams(): Any {
        TODO("GPU: return LLUICtrlFactory.getDefaultParams<LLPanelInventoryListItemBase>()")
    }

    @Suppress("UNUSED")
    private fun reshapeLeftWidgets() {
        var widgetLeft = 0
        leftWidgetsWidth = 0
        for (widget in leftSideWidgets) {
            if (!widget.getVisible()) continue
            val rect = widget.getRect()
            widget.setShape(LLRect(widgetLeft, rect.top, widgetLeft + rect.getWidth(), rect.bottom))
            widgetLeft += rect.getWidth() + widgetSpacing
            leftWidgetsWidth = widgetLeft
        }
    }

    private fun reshapeRightWidgets() {
        var widgetRight = getLocalWidth() - widgetSpacing
        var widgetLeft = widgetRight
        for (widget in rightSideWidgets.reversed()) {
            if (!widget.getVisible()) continue
            val rect = widget.getRect()
            widgetLeft = widgetRight - rect.getWidth()
            widget.setShape(LLRect(widgetLeft, rect.top, widgetRight, rect.bottom))
            widgetRight = widgetLeft - widgetSpacing
        }
        rightWidgetsWidth = getLocalWidth() - widgetLeft
    }

    private fun reshapeMiddleWidgets() {
        val iconRect = iconCtrl?.getRect() ?: return
        val iconLeft = leftWidgetsWidth + widgetSpacing
        iconCtrl?.setShape(LLRect(iconLeft, iconRect.top, iconLeft + iconRect.getWidth(), iconRect.bottom))

        val nameLeft = iconLeft + iconRect.getWidth() + widgetSpacing
        var nameRight = getLocalWidth() - rightWidgetsWidth - widgetSpacing
        if (mIsFavorite) {
            nameRight -= FAVORITE_IMAGE_SIZE + FAVORITE_IMAGE_PAD
        }
        val nameRect = titleCtrl?.getRect() ?: return
        titleCtrl?.setShape(LLRect(nameLeft, nameRect.top, nameRight, nameRect.bottom))
    }

    protected open fun getLocalWidth(): Int {
        TODO("GPU: return panel local rect width")
    }

    private fun getIsItemFavorite(inv: LLViewerInventoryItemStub): Boolean {
        TODO("APR: use JVM equivalent - if item is link resolve linked object via gInventory and check getIsFavorite(), else check item.getIsFavorite()")
    }
}

data class LLViewerInventoryItemStub(
    val uuid: UUID,
    val name: String,
    val type: String,
    val wearableType: String,
    val actualDescription: String,
    val creationDate: Long
)
