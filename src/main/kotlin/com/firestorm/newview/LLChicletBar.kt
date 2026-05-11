package com.firestorm.newview

import java.util.UUID

// LLChicletBar is a singleton panel that owns the chiclet panel and well buttons.
// LLSingleton<T> → Kotlin object; session observer interface is implemented inline.
object LLChicletBar {

    var mChicletPanel: LLChicletPanel? = null
    private var mToolbarStack: Any? = null

    init {
        TODO("APR: LLIMMgr.getInstance().addSessionObserver(this); buildFromFile(\"panel_chiclet_bar.xml\")")
    }

    fun destroy() {
        TODO("APR: if LLIMMgr.instanceExists() then LLIMMgr.getInstance().removeSessionObserver(this)")
    }

    // ---- LLIMSessionObserver ------------------------------------------------

    fun sessionAdded(sessionId: UUID, name: String, otherParticipantId: UUID, hasOfflineMsg: Boolean) {
        val panel = mChicletPanel ?: return
        TODO("APR: find im session; if no chiclet exists yet, createIMChiclet(sessionId); set name and other-participant id on it")
    }

    fun sessionActivated(sessionId: UUID, name: String, otherParticipantId: UUID) {}

    fun sessionVoiceOrIMStarted(sessionId: UUID) {}

    fun sessionRemoved(sessionId: UUID) {
        val panel = mChicletPanel ?: return
        TODO("GPU: find and close FSFloaterIM for sessionId; panel.removeChiclet(sessionId)")
    }

    fun sessionIDUpdated(oldSessionId: UUID, newSessionId: UUID) {
        val panel = mChicletPanel ?: return
        panel.findChiclet<LLChiclet>(oldSessionId)?.sessionId = newSessionId
    }

    // ---- public API --------------------------------------------------------

    fun getTotalUnreadIMCount(): Int =
        mChicletPanel?.getTotalUnreadIMCount() ?: 0

    fun createIMChiclet(sessionId: UUID): LLIMChiclet? {
        val type = LLIMChiclet.getIMSessionType(sessionId)
        return when (type) {
            LLIMChiclet.EType.TYPE_IM    -> mChicletPanel?.createChiclet<LLIMP2PChiclet>(sessionId)
            LLIMChiclet.EType.TYPE_GROUP -> mChicletPanel?.createChiclet<LLIMGroupChiclet>(sessionId)
            LLIMChiclet.EType.TYPE_AD_HOC -> mChicletPanel?.createChiclet<LLAdHocChiclet>(sessionId)
            else -> null
        }
    }

    fun postBuild(): Boolean {
        TODO("GPU: bind mToolbarStack and mChicletPanel from XML children; showWellButton for im_well and notification_well; connect FSDisableIMChiclets setting signal to updateVisibility()")
    }

    fun getChicletPanel(): LLChicletPanel? = mChicletPanel

    fun reshape(width: Int, height: Int, calledFromParent: Boolean) {
        TODO("GPU: track extra_shrink_width; call processWidthDecreased if narrowing; delegate to LLPanel.reshape() when appropriate")
    }

    fun showWellButton(wellName: String, visible: Boolean) {
        TODO("GPU: find child panel named '${wellName}_panel' and set visible=$visible")
    }

    fun updateVisibility(data: Any?) {
        TODO("GPU: set mChicletPanel visible = !(data.asBoolean())")
    }

    // ---- private helpers ---------------------------------------------------

    private fun processWidthDecreased(deltaWidth: Int): Int {
        val shrinkHeadroom = getChicletPanelShrinkHeadroom()
        if (shrinkHeadroom > 0) {
            val shrinkBy = minOf(-deltaWidth, shrinkHeadroom)
            TODO("GPU: reshape mChicletPanel parent by -shrinkBy; return remaining excess or 0")
        }
        return -deltaWidth
    }

    private fun getChicletPanelShrinkHeadroom(): Int {
        val panel = mChicletPanel ?: return 0
        TODO("GPU: return (mChicletPanel parent current width) - mChicletPanel.mMinWidth; must be >= 0")
    }

    private fun fitWithTopInfoBar() {
        // Not used in Firestorm (mini location panel is absent).
    }

    private fun log(panel: Any?, descr: String) {
        TODO("APR: emit debug log: descr, panel name/rect, parent name/rect")
    }
}
