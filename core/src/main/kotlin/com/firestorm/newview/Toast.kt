package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import com.firestorm.llcommon.LLSD
import java.util.concurrent.TimeUnit

private const val MOUSE_LEAVE = false
private const val MOUSE_ENTER = true

class ToastLifeTimer(private val toast: Toast, var period: Float) {

    private var startTimeMs: Long = -1L
    private var started: Boolean = false

    fun tick(): Boolean {
        if (hasExpired()) {
            toast.expire()
        }
        return false
    }

    fun stop() { started = false }

    fun start() {
        startTimeMs = System.currentTimeMillis()
        started = true
    }

    fun restart() {
        startTimeMs = System.currentTimeMillis()
    }

    fun getStarted(): Boolean = started

    fun getRemainingTimeF32(): Float {
        val elapsed = getElapsedTimeF32()
        if (!started || elapsed > period) return 0f
        return period - elapsed
    }

    private fun getElapsedTimeF32(): Float =
        if (startTimeMs < 0) 0f
        else (System.currentTimeMillis() - startTimeMs) / 1000f

    private fun hasExpired(): Boolean = started && getElapsedTimeF32() >= period
}

data class ToastParams(
    val panel: ToastPanel,
    val notifId: LLUUID = LLUUID(),
    val sessionId: LLUUID = LLUUID(),
    val notification: Notification? = null,
    val lifetimeSecs: Float = 10f,
    val fadingTimeSecs: Float = 2f,
    val onDeleteToast: ((Toast) -> Unit)? = null,
    val canFade: Boolean = true,
    val canBeStored: Boolean = true,
    val enableHideBtn: Boolean = true,
    val isModal: Boolean = false,
    val isTip: Boolean = false,
    val forceShow: Boolean = false,
    val forceStore: Boolean = false,
)

class Toast(p: ToastParams) {

    val notificationId: LLUUID = p.notifId
    val sessionId: LLUUID = p.sessionId
    val notification: Notification? = p.notification

    var rect: Rect = Rect()
    var visible: Boolean = false
    var isHidden: Boolean = false

    private var panel: ToastPanel? = p.panel
    private var wrapperPanel: Panel? = Panel()
    private val timer: ToastLifeTimer
    private var toastLifetime: Float = p.lifetimeSecs
    private var toastFadingTime: Float = p.fadingTimeSecs
    private val canFade: Boolean = p.canFade
    private var canBeStored: Boolean = p.canBeStored
    private val hideBtnEnabled: Boolean = p.enableHideBtn
    private var hideBtnPressed: Boolean = false
    private val isModal: Boolean = p.isModal
    private val isTip: Boolean = p.isTip
    private var isFading: Boolean = false
    private var isHovered: Boolean = false

    private val onFadeSignal: MutableList<(Toast) -> Unit> = mutableListOf()
    private val onDeleteToastSignal: MutableList<(Toast) -> Unit> = mutableListOf()
    private val onToastDestroyedSignal: MutableList<(Toast) -> Unit> = mutableListOf()
    private val onToastHoverSignal: MutableList<(Toast, Boolean) -> Unit> = mutableListOf()
    private val toastMouseEnterSignal: MutableList<(Toast) -> Unit> = mutableListOf()
    private val toastMouseLeaveSignal: MutableList<(Toast) -> Unit> = mutableListOf()

    init {
        timer = ToastLifeTimer(this, p.lifetimeSecs)
        p.onDeleteToast?.let { onDeleteToastSignal.add(it) }
        if (isModal) {
            sModalToastsList.add(0, this)
        }
        if (!canFade) {
            timer.stop()
        }
        panel?.let { insertPanel(it) }
    }

    fun getPanel(): ToastPanel? = panel

    fun getTimer(): ToastLifeTimer = timer

    fun isHovered(): Boolean = isHovered

    fun isNotificationValid(): Boolean = notification?.let { !it.isCancelled() } ?: false

    fun getCanBeStored(): Boolean = canBeStored
    fun setCanBeStored(value: Boolean) { canBeStored = value }
    fun setIsHidden(value: Boolean) { isHidden = value }

    fun getTopPad(): Int {
        val wp = wrapperPanel ?: return 0
        return rect.height - wp.rect.height
    }

    fun getRightPad(): Int {
        val wp = wrapperPanel ?: return 0
        return rect.width - wp.rect.width
    }

    fun getTimeLeftToLive(): Float {
        var timeToLive = timer.getRemainingTimeF32()
        if (!isFading) timeToLive += toastFadingTime
        return timeToLive
    }

    fun setLifetime(seconds: Int) { toastLifetime = seconds.toFloat() }
    fun setFadingTime(seconds: Int) { toastFadingTime = seconds.toFloat() }

    fun setCanFade(canFade: Boolean) {
        if (!canFade) timer.stop()
    }

    fun setHideButtonEnabled(enabled: Boolean) {
        // no-op
    }

    fun setVisible(show: Boolean) {
        if (isHidden) return
        if (show && visible) return

        if (show) {
            if (!timer.getStarted() && canFade) {
                timer.start()
            }
        } else {
            // no-op
        }
        visible = show
        panel?.takeIf { !it.isDead() && it.parent == wrapperPanel }?.let {
            // no-op
        }
    }

    fun hide() {
        if (!isHidden) {
            visible = false
            setFading(false)
            timer.stop()
            isHidden = true
            onFadeSignal.forEach { it(this) }
        }
    }

    fun closeToast() {
        onDeleteToastSignal.forEach { it(this) }
        // no-op
    }

    fun stopTimer() {
        if (canFade) {
            setFading(false)
            timer.stop()
        }
    }

    fun startTimer() {
        if (canFade) {
            setFading(false)
            timer.start()
        }
    }

    fun reshape(width: Int, height: Int) {
        rect = rect.copy(width = width, height = height)
    }

    fun reshapeToPanel() {
        val p = panel ?: return
        val panelRect = p.rect
        val newWidth = panelRect.width + getRightPad()
        val newHeight = panelRect.height + getTopPad()
        rect = rect.copy(width = newWidth, height = newHeight)
    }

    fun insertPanel(p: ToastPanel) {
        panel = p
        wrapperPanel?.addChild(p)
        reshapeToPanel()
    }

    fun draw() {
        // no-op
    }

    fun setBackgroundOpaque(opaque: Boolean) {
        // no-op
    }

    fun setFocus(hasFocus: Boolean) {
        // no-op
    }

    fun onFocusLost() {
        // no-op
    }

    fun onFocusReceived() {
        // no-op
    }

    fun notifyParent(info: LLSD): Int {
        if (info.has("action") && info["action"].asString() == "hide_toast") {
            hide()
            return 1
        }
        return 0
    }

    fun handleMouseDown(x: Int, y: Int): Boolean {
        // no-op
        return false
    }

    fun setOnFadeCallback(cb: (Toast) -> Unit) = onFadeSignal.add(cb).let { { onFadeSignal.remove(cb) } }
    fun setOnToastDestroyedCallback(cb: (Toast) -> Unit) = onToastDestroyedSignal.add(cb).let { { onToastDestroyedSignal.remove(cb) } }
    fun setOnToastHoverCallback(cb: (Toast, Boolean) -> Unit) = onToastHoverSignal.add(cb).let { { onToastHoverSignal.remove(cb) } }
    fun setMouseEnterCallback(cb: (Toast) -> Unit) = toastMouseEnterSignal.add(cb).let { { toastMouseEnterSignal.remove(cb) } }
    fun setMouseLeaveCallback(cb: (Toast) -> Unit) = toastMouseLeaveSignal.add(cb).let { { toastMouseLeaveSignal.remove(cb) } }

    internal fun expire() {
        if (!canFade) return
        if (isFading) {
            hide()
        } else {
            setFading(true)
            timer.restart()
        }
    }

    private fun setFading(fading: Boolean) {
        isFading = fading
        updateTransparency()
        timer.period = if (fading) toastFadingTime else toastLifetime
    }

    private fun updateTransparency() {
        // no-op
    }

    fun updateHoveredState() {
        // no-op
    }

    fun isDead(): Boolean = false

    companion object {
        private val sModalToastsList: MutableList<Toast> = mutableListOf()
        private val allToasts: MutableList<Toast> = mutableListOf()

        fun isAlertToastShown(): Boolean = sModalToastsList.isNotEmpty()

        fun updateClass() {
            allToasts.forEach { it.updateHoveredState() }
        }

        fun cleanupToasts() {
            allToasts.toList().forEach { it.closeToast() }
            allToasts.clear()
        }
    }
}

private fun ToastPanel.isDead(): Boolean = false
private val ToastPanel.parent: Panel? get() = null
