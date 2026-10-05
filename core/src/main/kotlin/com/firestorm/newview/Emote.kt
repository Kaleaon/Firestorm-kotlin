package com.firestorm.newview

import com.firestorm.llcharacter.Character
import com.firestorm.llcharacter.JointPriority
import com.firestorm.llcharacter.LLMotion
import com.firestorm.llcharacter.MotionBlendType
import com.firestorm.llcharacter.MotionInitStatus
import com.firestorm.llcharacter.MotionPoseWeight
import com.firestorm.llcharacter.VisualParam
import com.firestorm.llcommon.LLUUID

private const val MIN_REQUIRED_PIXEL_AREA_EMOTE: Float = 2000.0f
private const val EMOTE_MORPH_FADEIN_TIME: Float = 0.3f
private const val EMOTE_MORPH_IN_TIME: Float = 1.1f
private const val EMOTE_MORPH_FADEOUT_TIME: Float = 1.4f

open class Emote(id: LLUUID) : LLMotion(id) {

    private var character: Character? = null
    private var param: VisualParam? = null

    private val pose = MotionPoseWeight()

    override fun getLoop(): Boolean = false

    override fun getDuration(): Float =
        EMOTE_MORPH_FADEIN_TIME + EMOTE_MORPH_IN_TIME + EMOTE_MORPH_FADEOUT_TIME

    override fun getEaseInDuration(): Float = EMOTE_MORPH_FADEIN_TIME

    override fun getEaseOutDuration(): Float = EMOTE_MORPH_FADEOUT_TIME

    override fun getMinPixelArea(): Float = MIN_REQUIRED_PIXEL_AREA_EMOTE

    override fun getPriority(): JointPriority = JointPriority.MEDIUM

    override fun getBlendType(): MotionBlendType = MotionBlendType.NORMAL_BLEND

    override fun canDeprecate(): Boolean = false

    override fun onInitialize(character: Character): MotionInitStatus {
        this.character = character
        // no-op
        return MotionInitStatus.SUCCESS
    }

    override fun onActivate(): Boolean {
        val ch = character ?: return true

        val defaultParam = ch.getVisualParam("Express_Closed_Mouth")
        defaultParam?.setWeight(defaultParam.maxWeight, uploadBake = false)

        param = ch.getVisualParam(name)
        param?.let {
            it.setWeight(0.0f, uploadBake = false)
            ch.updateVisualParams()
        }

        return true
    }

    override fun onUpdate(activeTime: Float): Boolean {
        val p = param ?: return true

        val blendWeight = pose.weight
        val weight = p.minWeight + blendWeight * (p.maxWeight - p.minWeight)
        p.setWeight(weight, uploadBake = false)

        val ch = character ?: return true

        val defaultParam = ch.getVisualParam("Express_Closed_Mouth")
        defaultParam?.let {
            val defaultWeight = it.minWeight + (1.0f - blendWeight) * (it.maxWeight - it.minWeight)
            it.setWeight(defaultWeight, uploadBake = false)
        }

        ch.updateVisualParams()
        return true
    }

    override fun onDeactivate() {
        param?.setWeight(param!!.defaultWeight, uploadBake = false)

        val ch = character ?: return
        val defaultParam = ch.getVisualParam("Express_Closed_Mouth")
        defaultParam?.setWeight(defaultParam.maxWeight, uploadBake = false)
        ch.updateVisualParams()
    }

    override fun getPose(): MotionPoseWeight = pose

    companion object {
        fun create(id: LLUUID): LLMotion = Emote(id)
    }
}
