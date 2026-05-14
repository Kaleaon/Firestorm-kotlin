package com.firestorm.newview

import java.util.UUID

enum class EPopupDirection {
    LEFT, TOP, RIGHT, BOTTOM, TOP_RIGHT
}

data class LLHintTargetParams(
    val target: String,
    val direction: EPopupDirection
)

data class LLHintPopupParams(
    val notification: LLNotificationPtr,
    val targetParams: LLHintTargetParams? = null,
    val distance: Int = 0,
    val fadeInTime: Float = 0f,
    val fadeOutTime: Float = 0f
)

typealias LLNotificationPtr = Map<String, Any>

class LLHintPopup(private val params: LLHintPopupParams) {

    private val notification: LLNotificationPtr = params.notification
    private val target: String = params.targetParams?.target ?: ""
    private val direction: EPopupDirection = params.targetParams?.direction ?: EPopupDirection.TOP
    private val distance: Int = params.distance
    private val fadeInTime: Float = params.fadeInTime
    private val fadeOutTime: Float = params.fadeOutTime
    private var hidden = false

    fun onClickClose() {
        if (!hidden) {
            hide()
            // no-op
        }
    }

    fun hide() {
        if (!hidden) {
            hidden = true
            // no-op
        }
    }

    fun draw() {
        // no-op
    }

    fun postBuild() {
        // no-op
    }
}

object LLHints {

    private val targetRegistry: MutableMap<String, Any> = mutableMapOf()
    private val hints: MutableMap<LLNotificationPtr, LLHintPopup> = mutableMapOf()
    private var controlConnection: (() -> Unit)? = null

    init {
        System.err.println("LLHints: init not yet implemented")
    }

    fun show(hint: LLNotificationPtr) {
        val params = LLHintPopupParams(notification = hint)
        val popup = LLHintPopup(params)
        hints[hint] = popup
        System.err.println("LLHints: show not yet implemented")
    }

    fun hide(hint: LLNotificationPtr) {
        val popup = hints.remove(hint)
        popup?.hide()
    }

    fun registerHintTarget(name: String, target: Any) {
        targetRegistry[name] = target
    }

    fun getHintTarget(name: String): Any? = targetRegistry[name]

    private fun showHints(show: Boolean) {
        System.err.println("LLHints: showHints not yet implemented")
    }
}
