package com.firestorm.llui

// ============================================================================
// RelPos — spatial anchor enum for badge placement
// ============================================================================

enum class RelPos(val bits: Int) {
    CENTER(0),
    LEFT(1 shl 0),
    RIGHT(1 shl 1),
    TOP(1 shl 2),
    BOTTOM(1 shl 3),
    BOTTOM_LEFT((1 shl 3) or (1 shl 0)),
    BOTTOM_RIGHT((1 shl 3) or (1 shl 1)),
    TOP_LEFT((1 shl 2) or (1 shl 0)),
    TOP_RIGHT((1 shl 2) or (1 shl 1));

    fun isBottom(): Boolean = (bits and BOTTOM.bits) == BOTTOM.bits
    fun isCenter(): Boolean = bits == CENTER.bits
    fun isLeft(): Boolean   = (bits and LEFT.bits) == LEFT.bits
    fun isRight(): Boolean  = (bits and RIGHT.bits) == RIGHT.bits
    fun isTop(): Boolean    = (bits and TOP.bits) == TOP.bits
}

// ============================================================================
// Badge
// ============================================================================

// BADGE_OFFSET_NOT_SPECIFIED signals that percentage-based positioning should
// be used instead of a fixed pixel offset.
private const val BADGE_OFFSET_NOT_SPECIFIED = Int.MAX_VALUE

class Badge(params: Params) : UICtrl(params) {

    data class Params(
        val owner: View? = null,
        val borderImage: UIImage? = null,
        val borderColor: UIColor = UIColor.TRANSPARENT,
        val image: UIImage? = null,
        val imageColor: UIColor = UIColor.WHITE,
        val label: String = "",
        val labelColor: UIColor = UIColor.WHITE,
        val labelOffsetHoriz: Int = 0,
        val labelOffsetVert: Int = 0,
        val location: RelPos = RelPos.TOP_LEFT,
        val locationOffsetHCenter: Int? = null,
        val locationOffsetVCenter: Int? = null,
        val locationPercentHCenter: UInt = 0u,
        val locationPercentVCenter: UInt = 0u,
        val paddingHoriz: Float = 0f,
        val paddingVert: Float = 0f,
        // Base UICtrl params forwarded
        val font: Font? = null,
        val name: String = ""
    ) {
        fun equalsIgnoringOwner(other: Params): Boolean =
            borderImage == other.borderImage &&
            borderColor == other.borderColor &&
            image == other.image &&
            imageColor == other.imageColor &&
            label == other.label &&
            labelColor == other.labelColor &&
            labelOffsetHoriz == other.labelOffsetHoriz &&
            labelOffsetVert == other.labelOffsetVert &&
            location == other.location &&
            locationOffsetHCenter == other.locationOffsetHCenter &&
            locationOffsetVCenter == other.locationOffsetVCenter &&
            locationPercentHCenter == other.locationPercentHCenter &&
            locationPercentVCenter == other.locationPercentVCenter &&
            paddingHoriz == other.paddingHoriz &&
            paddingVert == other.paddingVert
    }

    private val borderImage: UIImage?  = params.borderImage
    private val borderColor: UIColor   = params.borderColor
    private val glFont: Font?          = params.font
    private val image: UIImage?        = params.image
    private val imageColor: UIColor    = params.imageColor
    private var label: String          = params.label
    private val labelColor: UIColor    = params.labelColor
    private val labelOffsetHoriz: Int  = params.labelOffsetHoriz
    private val labelOffsetVert: Int   = params.labelOffsetVert
    private val location: RelPos       = params.location
    private val paddingHoriz: Float    = params.paddingHoriz
    private val paddingVert: Float     = params.paddingVert

    private val locationOffsetHCenter: Int =
        params.locationOffsetHCenter ?: BADGE_OFFSET_NOT_SPECIFIED
    private val locationOffsetVCenter: Int =
        params.locationOffsetVCenter ?: BADGE_OFFSET_NOT_SPECIFIED

    // Normalized [0,1] center fractions derived from location + percent params
    private val locationPercentHCenter: Float
    private val locationPercentVCenter: Float

    private val owner: View?           = params.owner
    private var parentScroller: ScrollContainer? = null
    private var drawAtParentTop: Boolean = false

    init {
        var hCenter = 0.5f
        var vCenter = 0.5f

        if (!location.isCenter()) {
            val h = params.locationPercentHCenter * 0.01f
            val v = params.locationPercentVCenter * 0.01f
            if (location.isRight())  hCenter = 0.5f * (1.0f + h)
            else if (location.isLeft()) hCenter = 0.5f * (1.0f - h)
            if (location.isTop())    vCenter = 0.5f * (1.0f + v)
            else if (location.isBottom()) vCenter = 0.5f * (1.0f - v)
        }

        locationPercentHCenter = hCenter
        locationPercentVCenter = vCenter
    }

    fun getLabel(): String = label

    fun setLabel(newLabel: String) {
        label = newLabel
    }

    fun setDrawAtParentTop(drawAtTop: Boolean) {
        drawAtParentTop = drawAtTop
    }

    fun addToView(view: View): Boolean {
        val added = view.addChild(this)
        if (added) {
            setShape(view.getLocalRect())
            var parent: View? = owner
            while (parent != null) {
                val scroller = parent as? ScrollContainer
                if (scroller != null) {
                    parentScroller = scroller
                    break
                }
                parent = parent.getParent()
            }
        }
        return added
    }

    override fun draw() {
        if (label.isEmpty()) return
        val ownerView = owner ?: return
        if (!ownerView.isInVisibleChain()) return

        val font = glFont ?: return

        val badgeWidth  = 2.0f * paddingHoriz + font.getWidthF32(label)
        val badgeHeight = 2.0f * paddingVert   + font.lineHeight

        val ownerRect = ownerView.localRectToOtherView(ownerView.getLocalRect(), this)

        var locationOffsetHoriz = locationOffsetHCenter
        var locationOffsetVert  = locationOffsetVCenter

        val scroller = parentScroller
        if (scroller != null) {
            val visibleRect = scroller.getVisibleContentRect()

            if (locationOffsetHCenter != BADGE_OFFSET_NOT_SPECIFIED) {
                locationOffsetHoriz += when {
                    location.isRight()  -> visibleRect.right
                    location.isLeft()   -> visibleRect.left
                    else                -> (visibleRect.left + visibleRect.right) / 2
                }
            }

            if (locationOffsetVCenter != BADGE_OFFSET_NOT_SPECIFIED) {
                locationOffsetVert += when {
                    location.isTop()    -> visibleRect.top
                    location.isBottom() -> visibleRect.bottom
                    else                -> (visibleRect.bottom + visibleRect.top) / 2
                }
            }
        }

        val badgeCenterX: Float = if (locationOffsetHCenter == BADGE_OFFSET_NOT_SPECIFIED)
            ownerRect.left + ownerRect.width * locationPercentHCenter
        else
            locationOffsetHoriz.toFloat()

        val badgeCenterY: Float = if (locationOffsetVCenter == BADGE_OFFSET_NOT_SPECIFIED) {
            if (drawAtParentTop)
                ownerRect.top - badgeHeight * 0.5f - 1f
            else
                ownerRect.bottom + ownerRect.height * locationPercentVCenter
        } else {
            locationOffsetVert.toFloat()
        }

        val alpha = drawContext.alpha

        if (image != null) {
            val badgeX = (badgeCenterX - badgeWidth  * 0.5f).toInt()
            val badgeY = (badgeCenterY - badgeHeight * 0.5f).toInt()
            image.drawSolid(badgeX, badgeY, badgeWidth.toInt(), badgeHeight.toInt(), imageColor.withAlpha(alpha))
            borderImage?.drawSolid(badgeX, badgeY, badgeWidth.toInt(), badgeHeight.toInt(), borderColor.withAlpha(alpha))
        } else {
            TODO("GPU: renderBadgeBackground($badgeCenterX, $badgeCenterY, $badgeWidth, $badgeHeight, imageColor)")
        }

        TODO("GPU: render label '${label}' centered at ($badgeCenterX + $labelOffsetHoriz, $badgeCenterY + $labelOffsetVert) with DROP_SHADOW_SOFT")
    }
}

// ============================================================================
// BadgeHolder — a UI container that can accept and display a Badge child
// ============================================================================

open class BadgeHolder(acceptsBadge: Boolean) {

    var acceptsBadge: Boolean = acceptsBadge
        private set

    fun setAcceptsBadge(accepts: Boolean) {
        acceptsBadge = accepts
    }

    open fun addBadge(badge: Badge): Boolean = false
}

// ============================================================================
// BadgeOwner — attached to a View to manage a single Badge lifecycle
// ============================================================================

class BadgeOwner(private val ownerView: View) {

    var hasBadgeHolderParent: Boolean = false
        private set

    private var badge: Badge? = null

    fun initBadgeParams(params: Badge.Params) {
        badge = createBadge(params)
    }

    fun addBadgeToParentHolder() {
        val b = badge ?: return
        var parent: View? = ownerView.getParent()
        while (parent != null) {
            val holder = parent as? BadgeHolder
            if (holder != null && holder.acceptsBadge) {
                hasBadgeHolderParent = holder.addBadge(b)
                return
            }
            parent = parent.getParent()
        }
    }

    fun setBadgeLabel(label: String) {
        badge?.setLabel(label)
    }

    fun setBadgeVisibility(visible: Boolean) {
        badge?.setVisible(visible)
    }

    fun setDrawBadgeAtTop(drawAtTop: Boolean) {
        badge?.setDrawAtParentTop(drawAtTop)
    }

    fun reshapeBadge(newRect: Rect) {
        badge?.setShape(newRect)
    }

    private fun createBadge(params: Badge.Params): Badge = Badge(params)
}
