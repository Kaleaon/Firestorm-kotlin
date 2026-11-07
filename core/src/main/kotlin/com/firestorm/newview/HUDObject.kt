package com.firestorm.newview

import com.firestorm.llmath.Vector3
import com.firestorm.llmath.Vector3d

abstract class HUDObject(val type: UByte) {

    var mDead: Boolean = false
        protected set
    var mVisible: Boolean = true
        protected set
    var isHighlighted: Boolean = false
    protected var mPositionGlobal: Vector3d = Vector3d(0.0, 0.0, 0.0)
    protected var mSourceObject: ViewerObject? = null
    protected var mTargetObject: ViewerObject? = null

    open fun markDead() {
        mVisible = false
        mDead = true
        mSourceObject = null
        mTargetObject = null
    }

    open fun getDistance(): Float = 0f

    open fun setSourceObject(objectp: ViewerObject?) {
        if (objectp === mSourceObject) return
        mSourceObject = objectp
    }

    open fun setTargetObject(objectp: ViewerObject?) {
        if (objectp === mTargetObject) return
        mTargetObject = objectp
    }

    open fun getSourceObject(): ViewerObject? = mSourceObject
    open fun getTargetObject(): ViewerObject? = mTargetObject

    fun setPositionGlobal(positionGlobal: Vector3d) {
        mPositionGlobal = positionGlobal
    }

    fun setPositionAgent(positionAgent: Vector3) {
        System.err.println("HUDObject: setPositionAgent not yet implemented")
    }

    fun isVisible(): Boolean = mVisible
    fun getPositionGlobal(): Vector3d = mPositionGlobal

    protected abstract fun render()
    protected open fun renderForTimer() {}

    companion object {
        private val sHUDObjects: MutableList<HUDObject> = mutableListOf()

        fun addHUDObject(type: UByte): HUDObject? {
            val obj: HUDObject? = when (type.toInt()) {
                LL_HUD_TEXT        -> HUDText(type)
                LL_HUD_ICON        -> HUDIcon(type)
                LL_HUD_NAME_TAG    -> null
                else               -> { System.err.println("WARN: Unknown HUD object type: $type"); null }
            }
            if (obj != null) sHUDObjects.add(obj)
            return obj
        }

        fun addHUDEffect(type: UByte): HUDEffect? {
            val obj: HUDEffect? = when (type.toInt()) {
                LL_HUD_EFFECT_BEAM  -> {
                    val s = HUDEffectSpiral(type)
                    s.setDuration(0.7f); s.setVMag(0f); s.setVOffset(0f)
                    s.setInitialRadius(0.1f); s.setFinalRadius(0.2f)
                    s.setSpinRate(10f); s.setFlickerRate(0f)
                    s.setScaleBase(0.05f); s.setScaleVar(0.02f)
                    s
                }
                LL_HUD_EFFECT_GLOW  -> null  // deprecated
                LL_HUD_EFFECT_POINT -> {
                    val s = HUDEffectSpiral(type)
                    s.setDuration(0.5f); s.setVMag(1f); s.setVOffset(0f)
                    s.setInitialRadius(0.5f); s.setFinalRadius(1f)
                    s.setSpinRate(10f); s.setFlickerRate(0f)
                    s.setScaleBase(0.1f); s.setScaleVar(0.1f)
                    s
                }
                LL_HUD_EFFECT_SPHERE -> {
                    val s = HUDEffectSpiral(type)
                    s.setDuration(0.5f); s.setVMag(1f); s.setVOffset(0f)
                    s.setInitialRadius(0.5f); s.setFinalRadius(0.5f)
                    s.setSpinRate(20f); s.setFlickerRate(0f)
                    s.setScaleBase(0.1f); s.setScaleVar(0.1f)
                    s
                }
                LL_HUD_EFFECT_SPIRAL -> {
                    val s = HUDEffectSpiral(type)
                    s.setDuration(2f); s.setVMag(-2f); s.setVOffset(0.5f)
                    s.setInitialRadius(1f); s.setFinalRadius(0.5f)
                    s.setSpinRate(10f); s.setFlickerRate(20f)
                    s.setScaleBase(0.02f); s.setScaleVar(0.02f)
                    s
                }
                LL_HUD_EFFECT_EDIT  -> {
                    val s = HUDEffectSpiral(type)
                    s.setDuration(2f); s.setVMag(2f); s.setVOffset(-1f)
                    s.setInitialRadius(1.5f); s.setFinalRadius(1f)
                    s.setSpinRate(4f); s.setFlickerRate(200f)
                    s.setScaleBase(0.1f); s.setScaleVar(0.1f)
                    s
                }
                LL_HUD_EFFECT_LOOKAT          -> HUDEffectLookAt(type)
                LL_HUD_EFFECT_POINTAT         -> HUDEffectPointAt(type)
                LL_HUD_EFFECT_VOICE_VISUALIZER -> null
                LL_HUD_EFFECT_BLOB            -> null
                LL_HUD_EFFECT_RESET_SKELETON  -> null
                else -> { System.err.println("WARN: Unknown HUD effect type: $type"); null }
            }
            if (obj != null) sHUDObjects.add(obj)
            return obj
        }

        fun updateAll() {
            HUDText.updateAll()
            HUDIcon.updateAll()
            // no-op: HUDNameTag.updateAll() not yet implemented
            sortObjects()
        }

        fun renderAll() {
            // no-op
        }

        fun renderAllForTimer() {
            val iter = sHUDObjects.iterator()
            while (iter.hasNext()) {
                val obj = iter.next()
                val effect = obj as? HUDEffect
                if (effect != null || obj.isVisible()) {
                    obj.renderForTimer()
                }
            }
        }

        fun reshapeAll() {
            HUDText.reshape()
            // no-op: HUDNameTag.reshape() not yet implemented
        }

        fun cleanupHUDObjects() {
            HUDIcon.cleanupDeadIcons()
            sHUDObjects.forEach { it.markDead() }
            sHUDObjects.clear()
        }

        private fun sortObjects() {
            sHUDObjects.sortWith(Comparator { lhs, rhs ->
                if (lhs.isHighlighted) return@Comparator -1
                if (rhs.isHighlighted) return@Comparator 1
                rhs.getDistance().compareTo(lhs.getDistance())
            })
        }

        const val LL_HUD_TEXT                    = 0
        const val LL_HUD_ICON                    = 1
        const val LL_HUD_CONNECTOR               = 2
        const val LL_HUD_FLEXIBLE_OBJECT          = 3
        const val LL_HUD_ANIMAL_CONTROLS          = 4
        const val LL_HUD_LOCAL_ANIMATION_OBJECT   = 5
        const val LL_HUD_CLOTH                   = 6
        const val LL_HUD_EFFECT_BEAM             = 7
        const val LL_HUD_EFFECT_GLOW             = 8
        const val LL_HUD_EFFECT_POINT            = 9
        const val LL_HUD_EFFECT_TRAIL            = 10
        const val LL_HUD_EFFECT_SPHERE           = 11
        const val LL_HUD_EFFECT_SPIRAL           = 12
        const val LL_HUD_EFFECT_EDIT             = 13
        const val LL_HUD_EFFECT_LOOKAT           = 14
        const val LL_HUD_EFFECT_POINTAT          = 15
        const val LL_HUD_EFFECT_VOICE_VISUALIZER = 16
        const val LL_HUD_NAME_TAG                = 17
        const val LL_HUD_EFFECT_BLOB             = 18
        const val LL_HUD_EFFECT_RESET_SKELETON   = 19
    }
}

class HUDEffectSpiral(type: UByte) : HUDEffect(type) {
    private var vMag: Float = 0f
    private var vOffset: Float = 0f
    private var initialRadius: Float = 0f
    private var finalRadius: Float = 0f
    private var spinRate: Float = 0f
    private var flickerRate: Float = 0f
    private var scaleBase: Float = 0f
    private var scaleVar: Float = 0f

    fun setVMag(v: Float)          { vMag = v }
    fun setVOffset(v: Float)       { vOffset = v }
    fun setInitialRadius(r: Float) { initialRadius = r }
    fun setFinalRadius(r: Float)   { finalRadius = r }
    fun setSpinRate(r: Float)      { spinRate = r }
    fun setFlickerRate(r: Float)   { flickerRate = r }
    fun setScaleBase(s: Float)     { scaleBase = s }
    fun setScaleVar(s: Float)      { scaleVar = s }

    override fun render() {
        // no-op
    }
}
