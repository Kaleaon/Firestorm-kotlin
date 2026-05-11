package com.firestorm.newview

import com.firestorm.llcommon.LLUUID
import kotlin.math.max
import kotlin.math.min

val ALERT_CHANNEL_UUID = LLUUID("F3E07BC8-A973-476D-8C7F-F3B7293975D1")
val NOTIFICATION_CHANNEL_UUID = LLUUID("AEED3193-8709-4693-8558-7452CCA97AE5")
val NEARBY_CHAT_CHANNEL_UUID = LLUUID("E1158BD6-661C-4981-9DAD-4DCBFF062502")
val STARTUP_CHANNEL_UUID = LLUUID("B56AF90D-6684-48E4-B1E4-722D3DEB2CB6")

const val NOTIFY_BOX_WIDTH = 305

enum class ToastAlignment { TOP, CENTRE, BOTTOM }
enum class ChannelAlignment { LEFT, CENTRE, RIGHT }

open class ScreenChannelBase(
    val channelId: LLUUID,
    displayToastsAlways: Boolean = false,
    protected var toastAlignment: ToastAlignment = ToastAlignment.BOTTOM,
    protected var channelAlignment: ChannelAlignment = ChannelAlignment.LEFT,
) {
    protected var controlHovering: Boolean = false
    protected var hoveredToast: Toast? = null
    protected var canStoreToasts: Boolean = true
    protected var displayToastsAlways: Boolean = displayToastsAlways
    protected var showToasts: Boolean = true
    protected var hiddenToastsNum: Int = 0

    var rect: Rect = Rect()

    protected fun getChannelRect(): Rect {
        TODO("APR: use JVM equivalent — query floater snap region and chiclet container screen rects from viewer window")
    }

    open fun updatePositionAndSize(newRect: Rect) {
        var r = rect.copy(y = newRect.y + newRect.height)
        when (channelAlignment) {
            ChannelAlignment.LEFT -> Unit
            ChannelAlignment.CENTRE -> {
                val cx = newRect.width / 2
                r = r.copy(x = cx - r.width / 2)
            }
            ChannelAlignment.RIGHT -> {
                r = r.copy(x = newRect.x + newRect.width - r.width)
            }
        }
        rect = r
        redrawToasts()
    }

    open fun init(channelLeft: Int, channelRight: Int) {
        rect = Rect(x = channelLeft, y = 0, width = channelRight - channelLeft, height = 0)
        updateRect()
    }

    protected fun updateRect() {
        val channelRect = getChannelRect()
        val channelTop = channelRect.y + channelRect.height
        val channelBottom = channelRect.y + CHANNEL_BOTTOM_PANEL_MARGIN
        rect = rect.copy(y = channelBottom, height = channelTop - channelBottom)
    }

    open fun killToastByNotificationId(id: LLUUID) {}
    open fun modifyToastNotificationById(id: LLUUID, data: Any) {}
    open fun removeToastByNotificationId(id: LLUUID) {}
    open fun hideToastsFromScreen() {}
    open fun removeToastsFromChannel() {}
    open fun redrawToasts() {}

    open fun setControlHovering(control: Boolean) { controlHovering = control }

    fun isHovering(): Boolean = hoveredToast?.isHovered() ?: false

    fun setCanStoreToasts(store: Boolean) { canStoreToasts = store }
    fun getDisplayToastsAlways(): Boolean = displayToastsAlways
    fun getNumberOfHiddenToasts(): Int = hiddenToastsNum
    fun setShowToasts(show: Boolean) { showToasts = show }
    fun getShowToasts(): Boolean = showToasts
    fun getToastAlignment(): ToastAlignment = toastAlignment

    companion object {
        private const val CHANNEL_BOTTOM_PANEL_MARGIN = 35
    }
}

abstract class ToastMatcher {
    abstract fun matches(notification: Notification?): Boolean
}

class ScreenChannel(
    channelId: LLUUID,
    displayToastsAlways: Boolean = false,
    toastAlignment: ToastAlignment = ToastAlignment.BOTTOM,
    channelAlignment: ChannelAlignment = ChannelAlignment.LEFT,
) : ScreenChannelBase(channelId, displayToastsAlways, toastAlignment, channelAlignment) {

    private val toastList: MutableList<ToastElem> = mutableListOf()
    private val storedToastList: MutableList<ToastElem> = mutableListOf()
    private var startUpToastPanel: Toast? = null
    private val onStoreToastListeners: MutableList<(ToastPanel?, LLUUID) -> Unit> = mutableListOf()

    inner class ToastElem(private val toast: Toast) {
        fun getToast(): Toast? = if (toast.isDead()) null else toast
        fun getId(): LLUUID = if (toast.isDead()) LLUUID() else toast.notificationId
        override fun equals(other: Any?): Boolean = when (other) {
            is LLUUID -> getId() == other
            is Toast -> getToast() == other
            else -> false
        }
        override fun hashCode(): Int = getId().hashCode()
    }

    override fun init(channelLeft: Int, channelRight: Int) {
        super.init(channelLeft, channelRight)
        val channelRect = getChannelRect()
        updatePositionAndSize(channelRect)
    }

    override fun updatePositionAndSize(newRect: Rect) {
        when (channelAlignment) {
            ChannelAlignment.LEFT -> {
                rect = rect.copy(height = (newRect.height * getHeightRatio()).toInt())
            }
            ChannelAlignment.CENTRE -> {
                super.updatePositionAndSize(newRect)
                return
            }
            ChannelAlignment.RIGHT -> {
                val newTop = (newRect.height * getHeightRatio()).toInt()
                rect = rect.copy(
                    x = newRect.x + newRect.width - rect.width,
                    height = newTop,
                )
            }
        }
        redrawToasts()
    }

    fun findToasts(matcher: ToastMatcher): List<Toast> {
        val result = mutableListOf<Toast>()
        for (elem in storedToastList) {
            val toast = elem.getToast()
            if (toast != null && matcher.matches(toast.notification)) result.add(toast)
        }
        for (elem in toastList) {
            val toast = elem.getToast()
            if (toast != null && matcher.matches(toast.notification)) result.add(toast)
        }
        return result
    }

    fun addToast(p: ToastParams) {
        val showToastNow = if (displayToastsAlways) true
            else wasStartUpToastShown && (showToasts || p.forceShow)
        val storeToast = !showToastNow && p.canBeStored && canStoreToasts

        if (!showToastNow && !storeToast) {
            val notification = p.notification ?: Notifications.find(p.notifId)
            if (notification != null &&
                (!notification.canLogToIM() || !notification.hasFormElements())
            ) {
                Notifications.cancel(notification.id)
            }
            TODO("GPU: dispose panel p.panel to prevent leak")
        }

        val toast = Toast(p)
        val newElem = ToastElem(toast)

        toast.setOnFadeCallback { onToastFade(it) }
        toast.setOnToastDestroyedCallback { onToastDestroyed(it) }
        if (controlHovering) {
            toast.setOnToastHoverCallback { t, enter -> onToastHover(t, enter) }
            toast.setMouseEnterCallback { stopToastTimer(it) }
            toast.setMouseLeaveCallback { startToastTimer(it) }
        }

        if (showToastNow) {
            toastList.add(newElem)
            if (p.canBeStored) {
                storeToast(newElem)
            }
            updateShowToastsState()
            redrawToasts()
        } else {
            hiddenToastsNum++
            storeToast(newElem)
        }
    }

    override fun killToastByNotificationId(id: LLUUID) {
        val notification = Notifications.find(id) ?: return
        val inActive = toastList.indexOfFirst { it == id }
        if (inActive >= 0) {
            val toast = toastList[inActive].getToast()
            if (toast != null && toast.isNotificationValid()) {
                if (!notification.canLogToIM() || !notification.hasFormElements()) {
                    Notifications.cancel(id)
                }
            } else {
                removeToastByNotificationId(id)
            }
        } else {
            val inStored = storedToastList.indexOfFirst { it == id }
            if (inStored >= 0) {
                val toast = storedToastList[inStored].getToast()
                if (toast != null) {
                    if (!notification.canLogToIM() || !notification.hasFormElements()) {
                        Notifications.cancel(id)
                    }
                    deleteToast(toast)
                }
                storedToastList.removeAll { it == id }
            }
        }
    }

    override fun removeToastByNotificationId(id: LLUUID) {
        var i = toastList.indexOfFirst { it == id }
        while (i >= 0) {
            toastList[i].getToast()?.let { deleteToast(it) }
            toastList.removeAt(i)
            redrawToasts()
            i = toastList.indexOfFirst { it == id }
        }
        val stored = storedToastList.indexOfFirst { it == id }
        if (stored >= 0) {
            storedToastList[stored].getToast()?.let { deleteToast(it) }
            storedToastList.removeAt(stored)
        }
    }

    fun killMatchedToasts(matcher: ToastMatcher) {
        findToasts(matcher).forEach { killToastByNotificationId(it.notificationId) }
    }

    fun modifyToastByNotificationId(id: LLUUID, panel: ToastPanel) {
        val i = toastList.indexOfFirst { it == id }
        if (i >= 0) {
            toastList[i].getToast()?.let { toast ->
                TODO("GPU: swap toast panel — remove old panel, insertPanel(panel), startTimer")
            }
            redrawToasts()
        }
    }

    override fun hideToastsFromScreen() {
        for (elem in toastList) {
            elem.getToast()?.setVisible(false)
                ?: TODO("APR: log warning — attempt to hide a deleted toast")
        }
    }

    fun hideToast(notificationId: LLUUID) {
        toastList.firstOrNull { it == notificationId }?.getToast()?.hide()
            ?: TODO("APR: log warning — attempt to hide a deleted toast")
    }

    fun closeHiddenToasts(matcher: ToastMatcher) {
        val toClose = toastList.mapNotNull { elem ->
            val toast = elem.getToast()
            if (toast != null && !toast.isDead() && toast.notification != null &&
                !toast.visible && matcher.matches(toast.notification)
            ) toast else null
        }
        toClose.forEach { it.closeToast() }
    }

    override fun removeToastsFromChannel() {
        hideToastsFromScreen()
        toastList.forEach { it.getToast()?.let { t -> deleteToast(t) } }
        toastList.clear()
    }

    override fun redrawToasts() {
        if (toastList.isEmpty()) return
        when (toastAlignment) {
            ToastAlignment.TOP -> showToastsTop()
            ToastAlignment.CENTRE -> showToastsCentre()
            ToastAlignment.BOTTOM -> showToastsBottom()
        }
    }

    fun loadStoredToastsToChannel() {
        if (storedToastList.isEmpty()) return
        for (elem in storedToastList) {
            val toast = elem.getToast() ?: continue
            toast.setIsHidden(false)
            toast.startTimer()
            toastList.add(elem)
        }
        storedToastList.clear()
        redrawToasts()
    }

    fun loadStoredToastByNotificationIdToChannel(id: LLUUID) {
        val i = storedToastList.indexOfFirst { it == id }
        if (i < 0) return
        val toast = storedToastList[i].getToast() ?: return
        if (toast.visible) return
        toast.setIsHidden(false)
        toast.startTimer()
        toastList.add(storedToastList[i])
        redrawToasts()
    }

    fun removeToastsBySessionId(id: LLUUID) {
        if (toastList.isEmpty()) return
        hideToastsFromScreen()
        val iter = toastList.iterator()
        while (iter.hasNext()) {
            val toast = iter.next().getToast()
            if (toast != null && toast.sessionId == id) {
                deleteToast(toast)
                iter.remove()
            }
        }
        redrawToasts()
    }

    fun removeAndStoreAllStorableToasts() {
        if (toastList.isEmpty()) return
        hideToastsFromScreen()
        val iter = toastList.iterator()
        while (iter.hasNext()) {
            val elem = iter.next()
            val toast = elem.getToast()
            if (toast != null && toast.getCanBeStored()) {
                storeToast(elem)
                iter.remove()
            }
        }
        redrawToasts()
    }

    fun getToastByNotificationId(id: LLUUID): Toast? {
        storedToastList.firstOrNull { it == id }?.getToast()?.let { return it }
        return toastList.firstOrNull { it == id }?.getToast()
    }

    open fun stopToastTimer(toast: Toast) {
        if (toast != hoveredToast) return
        toast.stopTimer()
    }

    open fun startToastTimer(toast: Toast) {
        if (toast == hoveredToast) return
        toast.startTimer()
    }

    fun updateShowToastsState() {
        TODO("APR: query dockable floater state; if none docked, setShowToasts(true); else updateRect()")
    }

    fun updateStartUpString(num: Int) {}

    fun closeStartUpToast() {
        startUpToastPanel?.setVisible(false)
        startUpToastPanel = null
    }

    fun addOnStoreToastCallback(cb: (ToastPanel?, LLUUID) -> Unit): () -> Unit {
        onStoreToastListeners.add(cb)
        return { onStoreToastListeners.remove(cb) }
    }

    private fun storeToast(elem: ToastElem) {
        if (storedToastList.any { it == elem.getId() }) return
        val toast = elem.getToast() ?: return
        storedToastList.add(elem)
        onStoreToastListeners.forEach { it(toast.getPanel(), toast.notificationId) }
    }

    private fun deleteToast(toast: Toast) {
        if (toast.isDead()) return
        toast.closeToast()
        if (hoveredToast == toast) hoveredToast = null
    }

    private fun onToastDestroyed(toast: Toast) {
        toastList.removeAll { it == toast }
        storedToastList.removeAll { it == toast }
        if (hoveredToast == toast) hoveredToast = null
    }

    private fun onToastFade(toast: Toast) {
        val i = toastList.indexOfFirst { it == toast }
        if (i < 0) return
        val deleteNow = !canStoreToasts || !toast.getCanBeStored()
        if (deleteNow) {
            toastList.removeAt(i)
            deleteToast(toast)
        } else {
            storeToast(toastList[i])
            toastList.removeAt(i)
        }
        redrawToasts()
    }

    private fun onToastHover(toast: Toast, mouseEnter: Boolean) {
        if (mouseEnter) {
            if (toast.isHovered()) hoveredToast = toast
        } else if (hoveredToast != null) {
            if (!hoveredToast!!.isHovered()) hoveredToast = null
        }
        redrawToasts()
    }

    private fun onStartUpToastHide() {
        TODO("APR: fire onCommit to notify channel manager that startup toast closed")
    }

    private fun createStartUpToast(notifNum: Int, timer: Float) {
        updateRect()
        val p = ToastParams(
            panel = PanelGenericTip(Notification(name = "StartUp", type = NotificationType.NOTIFY)),
            lifetimeSecs = timer,
            enableHideBtn = false,
        )
        val toast = Toast(p)
        startUpToastPanel = toast
        toast.setOnFadeCallback { onStartUpToastHide() }
        TODO("GPU: configure startup toast text box with LLTrans.getString('StartUpNotifications'), reshape and position")
    }

    private fun showToastsBottom() {
        val channelRect = getChannelRect()
        var bottom = rect.y - TODO_FLOATER_VIEW_BOTTOM
        var toastMargin = 0
        val snapshot = toastList.toList()
        updateRect()

        val dockedFloaterShift = getDockableFloaterShift()

        for (i in snapshot.indices.reversed()) {
            if (i < snapshot.lastIndex) {
                val prev = snapshot[i + 1].getToast() ?: run {
                    TODO("APR: log warning — attempt to display a deleted toast"); return
                }
                bottom = prev.rect.y + prev.rect.height
                toastMargin = TOAST_GAP
            }
            val toast = snapshot[i].getToast() ?: run {
                TODO("APR: log warning — attempt to display a deleted toast"); return
            }
            var tr = toast.rect
            tr = tr.copy(x = channelRect.x + channelRect.width - tr.width, y = bottom + toastMargin)
            toast.rect = tr

            if (dockedFloaterShift != 0 && i == snapshot.lastIndex) {
                toast.rect = toast.rect.copy(y = toast.rect.y + dockedFloaterShift)
            }

            val stopShowing = toast.rect.y + toast.rect.height > channelRect.y + channelRect.height && i != snapshot.lastIndex

            if (stopShowing) {
                hiddenToastsNum = 0
                for (j in 0..i) {
                    snapshot[j].getToast()?.hide()
                }
                return
            }

            if (!toast.visible) toast.setVisible(true)
            TODO("GPU: send toast to back in z-order if it doesn't have focus and FSShowToastsInFront is false")
        }
    }

    private fun showToastsCentre() {
        val first = toastList.firstOrNull()?.getToast() ?: run {
            TODO("APR: log warning — attempt to display a deleted toast"); return
        }
        val centreY = (rect.height) / 2 + first.rect.height / 2
        for (elem in toastList.asReversed()) {
            val toast = elem.getToast() ?: run {
                TODO("APR: log warning — attempt to display a deleted toast"); return
            }
            val tr = toast.rect
            toast.rect = tr.copy(
                x = rect.x - tr.width / 2,
                y = centreY + tr.height / 2 + TOAST_GAP,
            )
            toast.setVisible(true)
        }
    }

    private fun showToastsTop() {
        val channelRect = getChannelRect()
        var top = channelRect.y + channelRect.height
        var toastMargin = 0
        val snapshot = toastList.toList()
        updateRect()

        val dockedFloaterShift = getDockableFloaterShift(forTop = true)

        for (i in snapshot.indices.reversed()) {
            if (i < snapshot.lastIndex) {
                val prev = snapshot[i + 1].getToast() ?: run {
                    TODO("APR: log warning — attempt to display a deleted toast"); return
                }
                top = prev.rect.y
                toastMargin = TOAST_GAP
            }
            val toast = snapshot[i].getToast() ?: run {
                TODO("APR: log warning — attempt to display a deleted toast"); return
            }
            var tr = toast.rect
            tr = tr.copy(
                x = channelRect.x + channelRect.width - tr.width,
                y = top - toastMargin - tr.height,
            )
            toast.rect = tr

            if (dockedFloaterShift != 0 && i == snapshot.lastIndex) {
                toast.rect = toast.rect.copy(y = toast.rect.y + dockedFloaterShift)
            }

            val stopShowing = toast.rect.y < channelRect.y && i != snapshot.lastIndex
            if (stopShowing) {
                hiddenToastsNum = 0
                val toastsToHide = mutableListOf<Toast>()
                for (j in 0..i) {
                    snapshot[j].getToast()?.let { toastsToHide.add(it) }
                }
                toastsToHide.forEach { it.hide() }
                return
            }

            if (!toast.visible) toast.setVisible(true)
            TODO("GPU: send toast to back in z-order if it doesn't have focus and FSShowToastsInFront is false")
        }
    }

    private fun getDockableFloaterShift(forTop: Boolean = false): Int {
        TODO("APR: query LLDockableFloater instance handle; return ±(floater.height + tongueHeight) if overlapsScreenChannel, else 0")
    }

    companion object {
        var wasStartUpToastShown: Boolean = false
            private set

        fun setStartUpToastShown() { wasStartUpToastShown = true }
        fun getStartUpToastShown(): Boolean = wasStartUpToastShown

        private fun getHeightRatio(): Float {
            TODO("APR: read NotificationChannelHeightRatio from saved settings, clamp to [0,1]")
        }

        private const val TOAST_GAP = 3
        private val TODO_FLOATER_VIEW_BOTTOM: Int get() = TODO("GPU: gFloaterView->getRect().mBottom")
    }
}
