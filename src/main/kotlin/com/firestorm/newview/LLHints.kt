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
            TODO("GPU: cancel notification via LLNotifications")
        }
    }

    fun hide() {
        if (!hidden) {
            hidden = true
            TODO("GPU: reset fade timer")
        }
    }

    fun draw() {
        TODO("GPU: render hint popup panel with fade alpha, arrow image, and target-relative positioning")
    }

    fun postBuild() {
        TODO("GPU: bind hint_text, close button, hint_title from panel XML; reshape for text bounds")
    }
}

object LLHints {

    private val targetRegistry: MutableMap<String, Any> = mutableMapOf()
    private val hints: MutableMap<LLNotificationPtr, LLHintPopup> = mutableMapOf()
    private var controlConnection: (() -> Unit)? = null

    init {
        TODO("GPU: subscribe to EnableUIHints control variable signal; set hint holder visibility")
    }

    fun show(hint: LLNotificationPtr) {
        val params = LLHintPopupParams(notification = hint)
        val popup = LLHintPopup(params)
        hints[hint] = popup
        TODO("GPU: add popup as child of gViewerWindow->getHintHolder() and center it")
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
        TODO("GPU: set gViewerWindow->getHintHolder() visibility to show")
    }
}
