package com.firestorm.newview

import java.util.UUID

const val MIN_REQUIRED_PIXEL_AREA_EMOTE: Float = 2000.0f
const val EMOTE_MORPH_FADEIN_TIME: Float = 0.3f
const val EMOTE_MORPH_IN_TIME: Float = 1.1f
const val EMOTE_MORPH_FADEOUT_TIME: Float = 1.4f

open class Emote(id: UUID) : Motion(id) {

    protected var character: Character? = null
    protected var param: VisualParam? = null

    override fun getLoop(): Boolean = false

    override fun getDuration(): Float =
        EMOTE_MORPH_FADEIN_TIME + EMOTE_MORPH_IN_TIME + EMOTE_MORPH_FADEOUT_TIME

    override fun getEaseInDuration(): Float = EMOTE_MORPH_FADEIN_TIME

    override fun getEaseOutDuration(): Float = EMOTE_MORPH_FADEOUT_TIME

    override fun getMinPixelArea(): Float = MIN_REQUIRED_PIXEL_AREA_EMOTE

    override fun getPriority(): JointPriority = JointPriority.MEDIUM_PRIORITY

    override fun getBlendType(): MotionBlendType = MotionBlendType.NORMAL_BLEND

    override fun canDeprecate(): Boolean = false

    override fun onInitialize(character: Character): MotionInitStatus {
        this.character = character
        TODO("GPU: set face joint signature bytes to 0xff for all priority tracks")
        @Suppress("UNREACHABLE_CODE")
        return MotionInitStatus.STATUS_SUCCESS
    }

    override fun onActivate(): Boolean {
        val ch = character ?: return true

        val defaultParam = ch.getVisualParam("Express_Closed_Mouth")
        defaultParam?.setWeight(defaultParam.maxWeight, false)

        param = ch.getVisualParam(name)
        param?.let {
            it.setWeight(0.0f, false)
            ch.updateVisualParams()
        }

        return true
    }

    override fun onUpdate(time: Float, jointMask: UByteArray): Boolean {
        val p = param ?: return true

        val weight = p.minWeight + pose.getWeight() * (p.maxWeight - p.minWeight)
        p.setWeight(weight, false)

        val ch = character ?: return true

        val defaultParam = ch.getVisualParam("Express_Closed_Mouth")
        defaultParam?.let {
            val defaultWeight = it.minWeight + (1.0f - pose.getWeight()) * (it.maxWeight - it.minWeight)
            it.setWeight(defaultWeight, false)
        }

        ch.updateVisualParams()
        return true
    }

    override fun onDeactivate() {
        param?.setWeight(param!!.defaultWeight, false)

        val ch = character ?: return
        val defaultParam = ch.getVisualParam("Express_Closed_Mouth")
        defaultParam?.setWeight(defaultParam.maxWeight, false)
        ch.updateVisualParams()
    }

    companion object {
        fun create(id: UUID): Motion = Emote(id)
    }
}
