package com.firestorm.llcharacter

import com.firestorm.llcommon.LLUUID

// Step-type enum matching EStepType in llmultigesture.h
enum class StepType {
    ANIMATION,
    SOUND,
    CHAT,
    WAIT,
    EOF_MARKER
}

// Wait-flag bit constants
object WaitFlags {
    const val TIME: UInt        = 0x01u
    const val ALL_ANIM: UInt    = 0x02u
    const val KEY_RELEASE: UInt = 0x04u
}

const val ANIM_FLAG_STOP: UInt = 0x01u

// ── Step types ────────────────────────────────────────────────────────────────

abstract class GestureStep {
    abstract val type: StepType
    abstract fun getLabel(): List<String>
    abstract fun getMaxSerialSize(): Int
    open fun dump(): String = type.name
}

class GestureStepAnimation(
    var animName: String = "None",
    var animAssetId: LLUUID = LLUUID.NULL,
    var flags: UInt = 0u
) : GestureStep() {
    override val type get() = StepType.ANIMATION
    override fun getMaxSerialSize() = 256 + 64 + 64

    override fun getLabel(): List<String> {
        val verb = if (flags and ANIM_FLAG_STOP != 0u) "AnimFlagStop" else "AnimFlagStart"
        return listOf(verb, animName)
    }

    override fun dump() = "step animation $animName id $animAssetId flags $flags"
}

class GestureStepSound(
    var soundName: String = "None",
    var soundAssetId: LLUUID = LLUUID.NULL,
    var flags: UInt = 0u
) : GestureStep() {
    override val type get() = StepType.SOUND
    override fun getMaxSerialSize() = 256 + 64 + 64
    override fun getLabel() = listOf("Sound", soundName)
    override fun dump() = "step sound $soundName id $soundAssetId flags $flags"
}

class GestureStepChat(
    var chatText: String = "",
    var flags: UInt = 0u
) : GestureStep() {
    override val type get() = StepType.CHAT
    override fun getMaxSerialSize() = 256 + 64
    override fun getLabel() = listOf("Chat", chatText)
    override fun dump() = "step chat $chatText flags $flags"
}

class GestureStepWait(
    var waitSeconds: Float = 0f,
    var flags: UInt = 0u
) : GestureStep() {
    override val type get() = StepType.WAIT
    override fun getMaxSerialSize() = 64 + 64

    override fun getLabel(): List<String> {
        val detail = when {
            flags and WaitFlags.TIME != 0u     -> "%.1f seconds".format(waitSeconds)
            flags and WaitFlags.ALL_ANIM != 0u -> "until animations are done"
            else                               -> ""
        }
        return listOf("Wait", detail)
    }

    override fun dump() = "step wait $waitSeconds flags $flags"
}

// ── MultiGesture ──────────────────────────────────────────────────────────────

/**
 * LLMultiGesture — an asset-based gesture composed of ordered [GestureStep]s
 * with playback state tracking.
 *
 * Translated from llmultigesture.h / llmultigesture.cpp.
 */
class MultiGesture {
    var key: Key = 0
    var mask: Mask = 0u
    var name: String = ""
    var trigger: String = ""
    var replaceText: String = ""

    val steps: MutableList<GestureStep> = mutableListOf()

    // Playback state
    var isPlaying: Boolean = false
    var currentStep: Int = 0
    var waitingAnimations: Boolean = false
    var waitingKeyRelease: Boolean = false
    var waitingTimer: Boolean = false
    var triggeredByKey: Boolean = false
    var keyReleased: Boolean = false
    var waitingAtEnd: Boolean = false

    // Elapsed time accumulated while in a WAIT step
    var waitElapsed: Float = 0f

    val requestedAnimIds: MutableSet<LLUUID> = mutableSetOf()
    val playingAnimIds: MutableSet<LLUUID> = mutableSetOf()

    var doneCallback: ((MultiGesture) -> Unit)? = null

    fun reset() {
        isPlaying = false
        currentStep = 0
        waitElapsed = 0f
        waitingAnimations = false
        waitingKeyRelease = false
        waitingTimer = false
        triggeredByKey = false
        keyReleased = false
        waitingAtEnd = false
        requestedAnimIds.clear()
        playingAnimIds.clear()
    }

    fun start() {
        reset()
        isPlaying = true
    }

    fun stop() {
        isPlaying = false
        doneCallback?.invoke(this)
    }

    /**
     * Advance playback by [dt] seconds.
     * Concrete gesture manager logic (network calls, audio) is stubbed.
     */
    fun step(dt: Float) {
        if (!isPlaying) return

        if (waitingTimer) {
            waitElapsed += dt
            val waitStep = steps.getOrNull(currentStep) as? GestureStepWait
            if (waitStep != null && waitElapsed >= waitStep.waitSeconds) {
                waitingTimer = false
                waitElapsed = 0f
                currentStep++
            }
            return
        }

        if (currentStep >= steps.size) {
            stop()
            return
        }

        when (val s = steps[currentStep]) {
            is GestureStepAnimation -> {
                TODO("GPU: trigger animation ${s.animName}")
            }
            is GestureStepSound -> {
                TODO("GPU: play sound ${s.soundName}")
            }
            is GestureStepChat -> {
                TODO("GPU: send chat ${s.chatText}")
            }
            is GestureStepWait -> {
                if (s.flags and WaitFlags.TIME != 0u) {
                    waitingTimer = true
                    waitElapsed = 0f
                    return   // advance on next step() call when time elapsed
                }
            }
        }
        currentStep++
    }

    fun getMaxSerialSize(): Int {
        var size = 64 + 64 + 64 + 256 + 256 + 64   // version, key, mask, trigger, replace, count
        for (step in steps) size += 64 + step.getMaxSerialSize()
        return size
    }

    fun dump(): String = buildString {
        appendLine("key=$key mask=$mask trigger=$trigger replace=$replaceText")
        steps.forEach { appendLine("  ${it.dump()}") }
    }
}
