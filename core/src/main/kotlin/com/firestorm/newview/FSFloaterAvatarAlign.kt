package com.firestorm.newview

import java.util.UUID
import kotlin.math.*

abstract class FSAvatarAlignBase(key: LLSD) : LLFloater(key) {

    protected var targetAvatar: LLVOAvatar? = null
    protected val labelMini: String = LLTrans.getString("FSAvatarAlignToggleMini")
    protected val labelFull: String = LLTrans.getString("FSAvatarAlignToggleFull")

    private var compassCX: Int = 0
    private var compassCY: Int = 0
    private var compassR: Int = 0
    private var hoverOctant: Int = -1
    private var hoverToggle: Boolean = false
    private var toggleBtnRect: LLRect = LLRect()

    companion object {
        const val MAX_FACE_DISTANCE: Float = 20f

        fun getActive(): FSAvatarAlignBase? {
            return if (gSavedSettings.getBool("AvatarAlignMini")) {
                LLFloaterReg.getTypedInstance<FSFloaterAvatarAlignMini>("avatar_align_mini")
            } else {
                LLFloaterReg.getTypedInstance<FSFloaterAvatarAlign>("avatar_align")
            }
        }
    }

    protected open fun isMiniMode(): Boolean = false
    protected open fun getToolbarHeight(): Int = 0
    protected open fun getBottomReserve(): Int = 104
    protected abstract fun onToggleMode()

    data class CompassLayout(
        val R: Int,
        val cx: Float,
        val cy: Float,
        val miniCompact: Boolean,
        val showLabels: Boolean,
        val bearingY: Int,
        val sqHalf: Int,
        val toggleLabel: String
    )

    private fun buildCompassLayout(): CompassLayout {
        val local = getLocalRect()
        val headerH = getHeaderHeight()
        val areaTop = local.top - headerH - getToolbarHeight()
        val areaBottom = local.bottom + getBottomReserve()
        val availH = areaTop - areaBottom
        val availW = local.width

        val mini = isMiniMode()
        val topClear = if (mini) 7 else 27
        val overhead = if (mini) 29 else 80
        val hMargin = if (mini) 8 else 28
        val R = maxOf(minOf(availW / 2 - hMargin, (availH - overhead) / 2), 30)

        val cx = local.centerX.toFloat()
        val cy = (areaTop - topClear - R).toFloat()
        return CompassLayout(
            R = R,
            cx = cx,
            cy = cy,
            miniCompact = mini && R < 50,
            showLabels = !mini && R >= 50,
            bearingY = if (mini) (cy.toInt() - R - 10) else (cy.toInt() - R - 25),
            sqHalf = if (mini) R else (R + 20),
            toggleLabel = if (mini) labelMini else labelFull
        )
    }

    override fun draw() {
        super.draw()
        drawCompass()
    }

    private fun drawCompass() {
        val lo = buildCompassLayout()
        compassCX = lo.cx.toInt()
        compassCY = lo.cy.toInt()
        compassR = lo.R
        val cx = lo.cx
        val cy = lo.cy
        val fR = lo.R.toFloat()

        // no-op: GPU: unbind texture unit 0
        // no-op: GPU: draw filled circle (background) at (cx,cy) r=fR color(0.08,0.08,0.10,0.90)
        // no-op: GPU: draw circle outline (outer ring) at (cx,cy) r=fR color(0.45,0.45,0.45,1.0)
        // no-op: GPU: draw circle outline (inner ring) at (cx,cy) r=fR*0.5 color(0.25,0.25,0.25,1.0)

        // Tick marks at 45° intervals
        // no-op: GPU: draw 8 tick line segments at 45° intervals on the compass ring

        // Hover highlight
        when {
            hoverOctant == -2 -> { /* no-op: GPU: draw filled circle hover highlight at center zone */ }
            hoverOctant >= 0 -> { /* no-op: GPU: draw pie-slice highlight for octant */ }
        }

        // Cardinal arms as kite/diamond shapes: North=red, South=white, East/West=grey
        // no-op: GPU: draw 4 cardinal kite arms (N=red, S=white, E/W=grey)

        // Intercardinal arms (NE, SE, SW, NW) — skipped when miniCompact
        if (!lo.miniCompact) {
            // no-op: GPU: draw 4 intercardinal kite arms at 45/135/225/315°
        }

        // no-op: GPU: draw center dot filled+outlined

        // Heading needle: yellow triangle pointing in current agent facing direction
        val at = gAgent.getAtAxis().let { v -> FloatArray(3).also { it[0] = v.x; it[1] = v.y; it[2] = 0f } }
        val len = sqrt(at[0] * at[0] + at[1] * at[1])
        if (len > 0.01f) {
            // no-op: GPU: draw yellow heading needle triangle pointing toward agent at-axis projected to XY plane
        }

        // Cardinal labels: N/S/E/W in full mode when compass is large enough
        if (lo.showLabels) {
            // no-op: GPU: render N/S/E/W text labels around compass at distance R+10
        }

        // Bearing text below the compass ring
        if (!lo.miniCompact) {
            val hat = gAgent.getAtAxis()
            val bearing = ((atan2(hat.x, hat.y) * (180f / PI.toFloat())) + 360f) % 360f
            // no-op: GPU: render bearing string at bearingY
        }

        // Toggle-mode button in top-right corner of compass bounding square
        // no-op: GPU: draw toggle button rect and label at top-right of compass square; update toggleBtnRect
    }

    override fun handleMouseDown(x: Int, y: Int, mask: Int): Boolean {
        if (toggleBtnRect.isNotEmpty() && toggleBtnRect.pointInRect(x, y)) {
            onToggleMode()
            return true
        }

        if (compassR > 0) {
            val dx = x - compassCX
            val dy = y - compassCY
            val dist = sqrt((dx * dx + dy * dy).toFloat())
            if (dist <= compassR.toFloat()) {
                if (dist < compassR.toFloat() * 0.25f) {
                    onClickFaceNearestAvatar()
                } else {
                    var deg = atan2(dx.toFloat(), dy.toFloat()) * (180f / PI.toFloat())
                    deg = (deg + 360f) % 360f
                    val octant = (((deg / 45f).roundToInt() * 45f) % 360f)
                    onClickCardinal(octant)
                }
                return true
            }
        }
        return super.handleMouseDown(x, y, mask)
    }

    override fun handleHover(x: Int, y: Int, mask: Int): Boolean {
        hoverToggle = toggleBtnRect.isNotEmpty() && toggleBtnRect.pointInRect(x, y)
        hoverOctant = -1

        if (!hoverToggle && compassR > 0) {
            val dx = x - compassCX
            val dy = y - compassCY
            val dist = sqrt((dx * dx + dy * dy).toFloat())
            if (dist <= compassR.toFloat()) {
                hoverOctant = if (dist < compassR.toFloat() * 0.25f) {
                    -2
                } else {
                    val deg = (atan2(dx.toFloat(), dy.toFloat()) * (180f / PI.toFloat()) + 360f) % 360f
                    ((deg / 45f).roundToInt()) % 8
                }
            }
        }
        return super.handleHover(x, y, mask)
    }

    private fun snapAvatarBody(targetAt: LLVector3) {
        if (!isAgentAvatarValid() || gAgentAvatarp.mRoot == null) return
        var at = targetAt.copy().apply { z = 0f }
        if (at.normalize() < 0.001f) return
        val up = LLVector3(0f, 0f, 1f)
        val left = (up cross at).normalized()
        val cleanAt = left cross up
        // no-op: GPU: set world rotation of gAgentAvatarp.mRoot from (cleanAt, left, up) and set world position from gAgent.getPositionAgent()
    }

    private fun snapRemoteAvatarBody(avatar: LLVOAvatar?) {
        if (avatar == null || avatar.isDead() || avatar.mRoot == null) return
        // no-op: GPU: reset avatar.mRoot world rotation and position to avatar's server rotation/position
    }

    private fun applyRotation(direction: LLVector3) {
        gAgent.resetAxes(direction)
        System.err.println("FSFloaterAvatarAlign: applyRotation not yet implemented") // APR: use JVM equivalent for send_agent_update(true, false)
        snapAvatarBody(direction)
        snapRemoteAvatarBody(targetAvatar)
        targetAvatar = null
    }

    private fun rotateAgentTo(targetDeg: Float) {
        val yawRad = targetDeg * (PI.toFloat() / 180f)
        val lookAt = LLVector3(sin(yawRad), cos(yawRad), 0f)
        applyRotation(lookAt)
    }

    fun onClickCardinal(targetDeg: Float) {
        rotateAgentTo(targetDeg)
    }

    fun onClickRotate(deltaDeg: Float) {
        val at = gAgent.getFrameAgent().getAtAxis().apply { z = 0f }.normalized()
        val yawDeg = atan2(at.x, at.y) * (180f / PI.toFloat())
        rotateAgentTo(((yawDeg + deltaDeg + 360f) % 360f))
    }

    fun onClickNearest() {
        val at = gAgent.getFrameAgent().getAtAxis().apply { z = 0f }.normalized()
        val yawDeg = (atan2(at.x, at.y) * (180f / PI.toFloat()) + 360f) % 360f
        val nearestDeg = ((yawDeg / 45f).roundToInt() * 45f) % 360f
        rotateAgentTo(nearestDeg)
    }

    fun isAvatarInRange(avatar: LLVOAvatar?): Boolean {
        if (avatar == null || avatar.isDead()) return false
        return distVec(avatar.getPositionAgent(), gAgent.getPositionAgent()) <= MAX_FACE_DISTANCE
    }

    fun faceAvatar(avatar: LLVOAvatar?) {
        if (avatar == null || !isAgentAvatarValid()) return
        targetAvatar = avatar
        val direction = (avatar.getPositionAgent() - gAgentAvatarp.getPositionAgent()).apply { z = 0f }.normalized()
        applyRotation(direction)
    }

    fun onClickFaceNearestAvatar() {
        if (!isAgentAvatarValid()) return
        val myPos = gAgent.getPositionAgent()
        var nearest: LLVOAvatar? = null
        var nearestDistSq = Float.MAX_VALUE

        for (character in LLCharacter.sInstances) {
            val avatar = character as LLVOAvatar
            if (avatar.isDead() || avatar.isControlAvatar() || avatar.isSelf()) continue
            val distSq = distVecSquared(avatar.getPositionAgent(), myPos)
            if (distSq > MAX_FACE_DISTANCE * MAX_FACE_DISTANCE) continue
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq
                nearest = avatar
            }
        }

        if (nearest == null) return
        faceAvatar(nearest)
    }

    protected fun repositionOnToggle(next: LLFloater, oldRect: LLRect) {
        val view = gFloaterView.getRect()
        val newW = next.getRect().width
        val newH = next.getRect().height
        val onLeft = oldRect.centerX < view.centerX
        val onBot = oldRect.centerY < view.centerY
        val newLeft = if (onLeft) oldRect.left else (oldRect.right - newW)
        val newBot = if (onBot) oldRect.bottom else (oldRect.top - newH)
        next.setOrigin(newLeft, newBot)
    }
}

class FSFloaterAvatarAlign(key: LLSD) : FSAvatarAlignBase(key) {

    override fun postBuild(): Boolean {
        childSetAction("btn_rotate_left_90")  { onClickRotate(-90f) }
        childSetAction("btn_rotate_left_45")  { onClickRotate(-45f) }
        childSetAction("btn_rotate_right_45") { onClickRotate(45f) }
        childSetAction("btn_rotate_right_90") { onClickRotate(90f) }
        childSetAction("btn_rotate_left_10")  { onClickRotate(-10f) }
        childSetAction("btn_rotate_left_1")   { onClickRotate(-1f) }
        childSetAction("btn_rotate_right_1")  { onClickRotate(1f) }
        childSetAction("btn_rotate_right_10") { onClickRotate(10f) }
        childSetAction("btn_nearest")         { onClickNearest() }
        childSetAction("btn_avatar")          { onClickFaceNearestAvatar() }
        return true
    }

    override fun onOpen(key: LLSD) {}

    override fun onToggleMode() {
        gSavedSettings.setBool("AvatarAlignMini", true)
        val rect = getRect()
        closeFloater(false)
        val mini = LLFloaterReg.showTypedInstance<FSFloaterAvatarAlignMini>("avatar_align_mini")
        if (mini != null) repositionOnToggle(mini, rect)
    }
}

class FSFloaterAvatarAlignMini(key: LLSD) : FSAvatarAlignBase(key) {

    override fun isMiniMode(): Boolean = true
    override fun getToolbarHeight(): Int = 22
    override fun getBottomReserve(): Int = 3

    override fun postBuild(): Boolean {
        childSetAction("btn_rotate_left_1")  { onClickRotate(-1f) }
        childSetAction("btn_rotate_right_1") { onClickRotate(1f) }
        childSetAction("btn_avatar")         { onClickFaceNearestAvatar() }
        return true
    }

    override fun onOpen(key: LLSD) {}

    override fun onToggleMode() {
        gSavedSettings.setBool("AvatarAlignMini", false)
        val rect = getRect()
        closeFloater(false)
        val full = LLFloaterReg.showTypedInstance<FSFloaterAvatarAlign>("avatar_align")
        if (full != null) repositionOnToggle(full, rect)
    }
}
