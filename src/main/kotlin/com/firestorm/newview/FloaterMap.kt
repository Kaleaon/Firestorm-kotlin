package com.firestorm.newview

import com.firestorm.llui.Floater
import com.firestorm.llui.TextBox
import com.firestorm.llui.NetMap
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val MAP_MINOR_DIR_THRESHOLD: Float = 0.07f
private const val MAP_PADDING_LEFT: Int = 0
private const val MAP_PADDING_TOP: Int = 2
private const val MAP_PADDING_RIGHT: Int = 2
private const val MAP_PADDING_BOTTOM: Int = 0

class FloaterMap(key: Any) : Floater(key) {

    private var textBoxEast: TextBox? = null
    private var textBoxNorth: TextBox? = null
    private var textBoxWest: TextBox? = null
    private var textBoxSouth: TextBox? = null
    private var textBoxSouthEast: TextBox? = null
    private var textBoxNorthEast: TextBox? = null
    private var textBoxNorthWest: TextBox? = null
    private var textBoxSouthWest: TextBox? = null
    private var map: NetMap? = null

    companion object {
        fun getInstance(): FloaterMap? = TODO("APR: use JVM equivalent - FloaterReg::getTypedInstance")
    }

    override fun postBuild(): Boolean {
        map = getChild<NetMap>("Net Map")
        map?.setToolTipMsg(getString("ToolTipMsg"))
        map?.setParcelNameMsg(getString("ParcelNameMsg"))
        map?.setParcelSalePriceMsg(getString("ParcelSalePriceMsg"))
        map?.setParcelSaleAreaMsg(getString("ParcelSaleAreaMsg"))
        map?.setParcelOwnerMsg(getString("ParcelOwnerMsg"))
        map?.setRegionNameMsg(getString("RegionNameMsg"))
        map?.setToolTipHintMsg(getString("ToolTipHintMsg"))
        map?.setAltToolTipHintMsg(getString("AltToolTipHintMsg"))
        sendChildToBack(map)

        textBoxNorth     = getChild<TextBox>("floater_map_north")
        textBoxEast      = getChild<TextBox>("floater_map_east")
        textBoxWest      = getChild<TextBox>("floater_map_west")
        textBoxSouth     = getChild<TextBox>("floater_map_south")
        textBoxSouthEast = getChild<TextBox>("floater_map_southeast")
        textBoxNorthEast = getChild<TextBox>("floater_map_northeast")
        textBoxSouthWest = getChild<TextBox>("floater_map_southwest")
        textBoxNorthWest = getChild<TextBox>("floater_map_northwest")

        textBoxNorth?.reshapeToFitText()
        textBoxEast?.reshapeToFitText()
        textBoxWest?.reshapeToFitText()
        textBoxSouth?.reshapeToFitText()
        textBoxSouthEast?.reshapeToFitText()
        textBoxNorthEast?.reshapeToFitText()
        textBoxSouthWest?.reshapeToFitText()
        textBoxNorthWest?.reshapeToFitText()

        stretchMiniMap(
            getRect().width - MAP_PADDING_LEFT - MAP_PADDING_RIGHT,
            getRect().height - MAP_PADDING_TOP - MAP_PADDING_BOTTOM
        )

        updateMinorDirections()
        sendChildToBack(getDragHandle())

        setIsChrome(true)
        getDragHandle()?.setTitleVisible(true)

        TODO("APR: use JVM equivalent - gFloaterView->adjustToFitScreen")
        return true
    }

    override fun handleDoubleClick(x: Int, y: Int, mask: Int): Boolean {
        if (isMinimized()) {
            setMinimized(false)
            return true
        }
        val posGlobal = map?.viewPosToGlobal(x, y)
        map?.performDoubleClickAction(posGlobal)
        return true
    }

    override fun reshape(width: Int, height: Int, calledFromParent: Boolean) {
        super.reshape(width, height, calledFromParent)
        stretchMiniMap(
            width - MAP_PADDING_LEFT - MAP_PADDING_RIGHT,
            height - MAP_PADDING_TOP - MAP_PADDING_BOTTOM
        )
        updateMinorDirections()
    }

    override fun draw() {
        var rotation = 0f
        val rotateMap = TODO("APR: use JVM equivalent - gSavedSettings.getBOOL(MiniMapRotate)") as? Boolean ?: true
        if (rotateMap) {
            val atAxis = TODO("APR: use JVM equivalent - LLViewerCamera::getInstance()->getAtAxis()") as? FloatArray
            rotation = atan2(atAxis?.get(0) ?: 0f, atAxis?.get(1) ?: 0f)
        }

        val pi = Math.PI.toFloat()
        val halfPi = pi / 2f

        setDirectionPos(textBoxEast,      rotation)
        setDirectionPos(textBoxNorth,     rotation + halfPi)
        setDirectionPos(textBoxWest,      rotation + pi)
        setDirectionPos(textBoxSouth,     rotation + pi + halfPi)
        setDirectionPos(textBoxNorthEast, rotation + halfPi / 2f)
        setDirectionPos(textBoxNorthWest, rotation + halfPi + halfPi / 2f)
        setDirectionPos(textBoxSouthWest, rotation + pi + halfPi / 2f)
        setDirectionPos(textBoxSouthEast, rotation + pi + halfPi + halfPi / 2f)

        val cameraMouselook = TODO("APR: use JVM equivalent - gAgentCamera.cameraMouselook()") as? Boolean ?: false
        if (cameraMouselook) {
            setMouseOpaque(false)
            getDragHandle()?.setMouseOpaque(false)
        } else {
            setMouseOpaque(true)
            getDragHandle()?.setMouseOpaque(true)
        }
        super.draw()
    }

    fun getCurrentTransparency(): Float {
        TODO("APR: use JVM equivalent - gSavedSettings.getF32(FSMiniMapOpacity)")
    }

    override fun setMinimized(minimized: Boolean) {
        super.setMinimized(minimized)
        if (minimized) {
            setTitle(getString("mini_map_caption"))
        } else {
            setTitle("")
        }
    }

    private fun setDirectionPos(textBox: TextBox?, rotation: Float) {
        if (textBox == null) return

        val mapHalfHeight  = (getRect().height / 2).toFloat() - (getHeaderHeight() / 2).toFloat()
        val mapHalfWidth   = (getRect().width / 2).toFloat()
        val textHalfHeight = (textBox.getRect().height / 2).toFloat()
        val textHalfWidth  = (textBox.getRect().width / 2).toFloat()
        val extraPadding   = (textBoxNorth?.getRect()?.width ?: 0) / 2f
        val posHalfHeight  = mapHalfHeight - textHalfHeight - extraPadding
        val posHalfWidth   = mapHalfWidth - textHalfWidth - extraPadding

        val pi = Math.PI.toFloat()
        val halfPi = pi / 2f
        val cornerAngle = atan2(posHalfHeight, posHalfWidth)
        val rotationMirroredIntoTop = abs(rotation % pi)
        val adjustedMirror = if (rotation < 0) pi - rotationMirroredIntoTop else rotationMirroredIntoTop
        val rotationMirroredIntoTopRight = halfPi - abs(adjustedMirror - halfPi)
        val atLeftRightEdge = rotationMirroredIntoTopRight < cornerAngle

        val partX = cos(rotation)
        val partY = sin(rotation)
        val x: Float
        val y: Float
        if (atLeftRightEdge) {
            x = if (partX >= 0) posHalfWidth else -posHalfWidth
            y = x * partY / partX
        } else {
            y = if (partY >= 0) posHalfHeight else -posHalfHeight
            x = y * partX / partY
        }

        textBox.setOrigin(
            (mapHalfWidth + x - textHalfWidth).toInt(),
            (mapHalfHeight + y - textHalfHeight).toInt()
        )
    }

    private fun updateMinorDirections() {
        val ne = textBoxNorthEast ?: return
        val showMinors = ne.getRect().height < MAP_MINOR_DIR_THRESHOLD *
            minOf(getRect().width, getRect().height)
        textBoxNorthEast?.setVisible(showMinors)
        textBoxNorthWest?.setVisible(showMinors)
        textBoxSouthWest?.setVisible(showMinors)
        textBoxSouthEast?.setVisible(showMinors)
    }

    private fun stretchMiniMap(width: Int, height: Int) {
        val m = map ?: return
        val mapRect = TODO("APR: use JVM equivalent - LLRect.setLeftTopAndSize") as? Any
        m.reshape(width, height, true)
        m.setRect(mapRect)
    }
}
